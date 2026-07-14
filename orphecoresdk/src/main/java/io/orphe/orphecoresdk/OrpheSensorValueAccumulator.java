package io.orphe.orphecoresdk;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/** COREのbestEffort値と再計算版をシリアル順に保持するSDK内部ストア。 */
final class OrpheSensorValueAccumulator {
    private static final int HALF_SERIAL_NUMBER_MODULUS =
            OrpheBestEffortRequester.SERIAL_NUMBER_MODULUS / 2;

    private final TreeMap<Long, Packet> packets = new TreeMap<>();
    private boolean initialized;
    private int latestSerialNumber;
    private long latestSequence;
    private long version;

    @Nullable
    synchronized OrpheSensorValueUpdate add(@NonNull final OrpheSensorValue[] values) {
        return add(values, Collections.singletonList(values));
    }

    @Nullable
    synchronized OrpheSensorValueUpdate add(
            @NonNull final OrpheSensorValue[] values,
            @NonNull final List<OrpheSensorValue[]> recalculatedPackets
    ) {
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
        final OrpheSensorValue[] copiedValues = values.clone();
        packets.put(sequence, new Packet(copiedValues, version));
        if (!initialized || sequence > latestSequence) {
            initialized = true;
            latestSerialNumber = serialNumber;
            latestSequence = sequence;
        }

        for (OrpheSensorValue[] recalculated : recalculatedPackets) {
            if (recalculated == null || recalculated.length == 0) {
                continue;
            }
            final int recalculatedSerial = OrpheBestEffortRequester.normalizeSerialNumber(
                    recalculated[0].serialNumber
            );
            final Packet packet = packets.get(resolveSequence(recalculatedSerial));
            if (packet != null) {
                packet.revisions.put(version, recalculated.clone());
            }
        }
        return new OrpheSensorValueUpdate(copiedValues, this, version);
    }

    @NonNull
    synchronized OrpheSensorValue[] snapshot() {
        return snapshot(version);
    }

    @NonNull
    synchronized OrpheSensorValue[] snapshot(final long maximumVersion) {
        int valueCount = 0;
        for (Packet packet : packets.values()) {
            final OrpheSensorValue[] values = packet.valuesAt(maximumVersion);
            if (values != null) {
                valueCount += values.length;
            }
        }

        final OrpheSensorValue[] result = new OrpheSensorValue[valueCount];
        int offset = 0;
        for (Packet packet : packets.values()) {
            final OrpheSensorValue[] values = packet.valuesAt(maximumVersion);
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
        @NonNull final TreeMap<Long, OrpheSensorValue[]> revisions = new TreeMap<>();
        final long insertedAtVersion;

        Packet(@NonNull final OrpheSensorValue[] values, final long version) {
            insertedAtVersion = version;
            revisions.put(version, values);
        }

        @Nullable
        OrpheSensorValue[] valuesAt(final long maximumVersion) {
            if (insertedAtVersion > maximumVersion) {
                return null;
            }
            final Map.Entry<Long, OrpheSensorValue[]> revision =
                    revisions.floorEntry(maximumVersion);
            return revision == null ? null : revision.getValue();
        }
    }
}
