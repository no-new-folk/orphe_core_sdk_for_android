package io.orphe.orphecoresdk;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/** fifoで受信した値をシリアル順にマージするSDK内部ストア。 */
final class OrpheInsoleValueAccumulator {
    private static final int HALF_SERIAL_NUMBER_MODULUS =
            OrpheFifoRequester.SERIAL_NUMBER_MODULUS / 2;

    private final TreeMap<Long, Packet> packets = new TreeMap<>();
    private boolean initialized;
    private int latestSerialNumber;
    private long latestSequence;
    private long version;

    /**
     * 未取得のシリアルだけを追加する。再要求で同じシリアルが重複しても更新通知しない。
     */
    @Nullable
    synchronized OrpheInsoleValueUpdate add(@NonNull final OrpheInsoleValue[] values) {
        return add(values, Collections.singletonList(values));
    }

    /**
     * 新規受信値を追加し、欠損回収によって再計算された既存パケットも同じ版で更新する。
     */
    @Nullable
    synchronized OrpheInsoleValueUpdate add(
            @NonNull final OrpheInsoleValue[] values,
            @NonNull final List<OrpheInsoleValue[]> recalculatedPackets
    ) {
        if (values.length == 0) {
            return null;
        }
        final int serialNumber = OrpheFifoRequester.normalizeSerialNumber(
                values[0].serialNumber
        );
        final long sequence = resolveSequence(serialNumber);
        if (packets.containsKey(sequence)) {
            return null;
        }

        version++;
        final OrpheInsoleValue[] copiedValues = values.clone();
        packets.put(sequence, new Packet(copiedValues, version));
        if (!initialized || sequence > latestSequence) {
            initialized = true;
            latestSerialNumber = serialNumber;
            latestSequence = sequence;
        }

        for (OrpheInsoleValue[] recalculated : recalculatedPackets) {
            if (recalculated == null || recalculated.length == 0) {
                continue;
            }
            final int recalculatedSerial = OrpheFifoRequester.normalizeSerialNumber(
                    recalculated[0].serialNumber
            );
            final long recalculatedSequence = resolveSequence(recalculatedSerial);
            final Packet packet = packets.get(recalculatedSequence);
            if (packet != null) {
                packet.revisions.put(version, recalculated.clone());
            }
        }
        return new OrpheInsoleValueUpdate(copiedValues, this, version);
    }

    @NonNull
    synchronized OrpheInsoleValue[] snapshot() {
        return snapshot(version);
    }

    /** 指定更新時点に存在していた値だけをシリアル順で返す。 */
    @NonNull
    synchronized OrpheInsoleValue[] snapshot(final long maximumVersion) {
        int valueCount = 0;
        for (Packet packet : packets.values()) {
            final OrpheInsoleValue[] values = packet.valuesAt(maximumVersion);
            if (values != null) {
                valueCount += values.length;
            }
        }

        final OrpheInsoleValue[] result = new OrpheInsoleValue[valueCount];
        int offset = 0;
        for (Packet packet : packets.values()) {
            final OrpheInsoleValue[] values = packet.valuesAt(maximumVersion);
            if (values == null) {
                continue;
            }
            System.arraycopy(values, 0, result, offset, values.length);
            offset += values.length;
        }
        return result;
    }

    synchronized void clear() {
        packets.clear();
        initialized = false;
        latestSerialNumber = 0;
        latestSequence = 0L;
        version = 0L;
    }

    private long resolveSequence(final int serialNumber) {
        if (!initialized) {
            return 0L;
        }
        final int forwardDistance = OrpheFifoRequester.normalizeSerialNumber(
                serialNumber - latestSerialNumber
        );
        if (forwardDistance <= HALF_SERIAL_NUMBER_MODULUS) {
            return latestSequence + forwardDistance;
        }
        return latestSequence
                - (OrpheFifoRequester.SERIAL_NUMBER_MODULUS - forwardDistance);
    }

    private static final class Packet {
        @NonNull final TreeMap<Long, OrpheInsoleValue[]> revisions = new TreeMap<>();
        final long insertedAtVersion;

        Packet(@NonNull final OrpheInsoleValue[] values, final long version) {
            insertedAtVersion = version;
            revisions.put(version, values);
        }

        @Nullable
        OrpheInsoleValue[] valuesAt(final long maximumVersion) {
            if (insertedAtVersion > maximumVersion) {
                return null;
            }
            final Map.Entry<Long, OrpheInsoleValue[]> revision =
                    revisions.floorEntry(maximumVersion);
            return revision == null ? null : revision.getValue();
        }
    }
}
