package io.orphe.orphecoresdk;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.TreeMap;

/** bestEffortで受信した値をシリアル順にマージするSDK内部ストア。 */
final class OrpheInsoleValueAccumulator {
    private static final int HALF_SERIAL_NUMBER_MODULUS =
            OrpheBestEffortRequester.SERIAL_NUMBER_MODULUS / 2;

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
        if (values.length == 0) {
            return null;
        }
        final int serialNumber = OrpheBestEffortRequester.normalizeSerialNumber(
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
            if (packet.version <= maximumVersion) {
                valueCount += packet.values.length;
            }
        }

        final OrpheInsoleValue[] result = new OrpheInsoleValue[valueCount];
        int offset = 0;
        for (Packet packet : packets.values()) {
            if (packet.version > maximumVersion) {
                continue;
            }
            System.arraycopy(packet.values, 0, result, offset, packet.values.length);
            offset += packet.values.length;
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
        final int forwardDistance = OrpheBestEffortRequester.normalizeSerialNumber(
                serialNumber - latestSerialNumber
        );
        if (forwardDistance <= HALF_SERIAL_NUMBER_MODULUS) {
            return latestSequence + forwardDistance;
        }
        return latestSequence
                - (OrpheBestEffortRequester.SERIAL_NUMBER_MODULUS - forwardDistance);
    }

    private static final class Packet {
        @NonNull final OrpheInsoleValue[] values;
        final long version;

        Packet(@NonNull final OrpheInsoleValue[] values, final long version) {
            this.values = values;
            this.version = version;
        }
    }
}
