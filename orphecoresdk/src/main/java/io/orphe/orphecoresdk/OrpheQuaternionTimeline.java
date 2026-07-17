package io.orphe.orphecoresdk;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/** 16bitシリアル順に姿勢を計算し、過去値の挿入時は後続を再計算するSDK内部ストア。 */
final class OrpheQuaternionTimeline<T> {
    private static final int HALF_SERIAL_NUMBER_MODULUS =
            OrpheFifoRequester.SERIAL_NUMBER_MODULUS / 2;
    private static final int DEFAULT_MAX_PACKETS =
            OrpheFifoConfig.DEFAULT.ringBufferCapacity + 1;

    interface Adapter<T> {
        int serialNumber(@NonNull T value);

        double accX(@NonNull T value);

        double accY(@NonNull T value);

        double accZ(@NonNull T value);

        double gyroX(@NonNull T value);

        double gyroY(@NonNull T value);

        double gyroZ(@NonNull T value);

        @NonNull T withQuaternion(@NonNull T value, @NonNull OrpheQuaternion quaternion);

        @NonNull T[] newArray(int length);
    }

    @NonNull private final Adapter<T> adapter;
    private final TreeMap<Long, Packet<T>> packets = new TreeMap<>();
    private final OrpheMadgwickFilter filter = new OrpheMadgwickFilter();
    private final int maxPackets;
    private long baseSequence = Long.MIN_VALUE;
    private OrpheMadgwickFilter.State baseState;
    private boolean initialized;
    private int latestSerialNumber;
    private long latestSequence;

    OrpheQuaternionTimeline(@NonNull final Adapter<T> adapter) {
        this(adapter, DEFAULT_MAX_PACKETS);
    }

    OrpheQuaternionTimeline(@NonNull final Adapter<T> adapter, final int maxPackets) {
        if (maxPackets < 1) {
            throw new IllegalArgumentException("maxPackets must be positive.");
        }
        this.adapter = adapter;
        this.maxPackets = maxPackets;
    }

    @NonNull
    synchronized Result<T> add(@NonNull final T[] values) {
        if (values.length == 0) {
            return new Result<>(adapter.newArray(0), new ArrayList<>(), false);
        }
        final int serialNumber = OrpheFifoRequester.normalizeSerialNumber(
                adapter.serialNumber(values[0])
        );
        final long sequence = resolveSequence(serialNumber);
        final Packet<T> existing = packets.get(sequence);
        if (existing != null) {
            return new Result<>(copy(existing.calculatedValues), new ArrayList<>(), false);
        }

        final Packet<T> inserted = new Packet<>(copy(values));
        packets.put(sequence, inserted);
        if (!initialized || sequence > latestSequence) {
            initialized = true;
            latestSerialNumber = serialNumber;
            latestSequence = sequence;
        }

        final ArrayList<T[]> recalculated = recalculateFrom(sequence);
        trimToCapacity();
        return new Result<>(copy(inserted.calculatedValues), recalculated, true);
    }

    synchronized void clear() {
        packets.clear();
        initialized = false;
        latestSerialNumber = 0;
        latestSequence = 0L;
        baseSequence = Long.MIN_VALUE;
        baseState = null;
        filter.reset();
    }

    @NonNull
    private ArrayList<T[]> recalculateFrom(final long sequence) {
        final Map.Entry<Long, Packet<T>> previous = packets.lowerEntry(sequence);
        if (previous != null) {
            filter.restore(previous.getValue().stateAfter);
        } else if (baseState != null && sequence > baseSequence) {
            filter.restore(baseState);
        } else {
            filter.reset();
        }

        final ArrayList<T[]> recalculated = new ArrayList<>();
        for (Map.Entry<Long, Packet<T>> entry : packets.tailMap(sequence, true).entrySet()) {
            final Packet<T> packet = entry.getValue();
            final T[] calculated = adapter.newArray(packet.rawValues.length);
            for (int i = 0; i < packet.rawValues.length; i++) {
                final T rawValue = packet.rawValues[i];
                final OrpheQuaternion quaternion = filter.updateDps(
                        adapter.accX(rawValue),
                        adapter.accY(rawValue),
                        adapter.accZ(rawValue),
                        adapter.gyroX(rawValue),
                        adapter.gyroY(rawValue),
                        adapter.gyroZ(rawValue)
                );
                calculated[i] = adapter.withQuaternion(rawValue, quaternion);
            }
            packet.calculatedValues = calculated;
            packet.stateAfter = filter.snapshot();
            recalculated.add(copy(calculated));
        }
        return recalculated;
    }

    private void trimToCapacity() {
        while (packets.size() > maxPackets) {
            final Map.Entry<Long, Packet<T>> oldest = packets.pollFirstEntry();
            if (oldest == null) {
                return;
            }
            baseSequence = oldest.getKey();
            baseState = oldest.getValue().stateAfter;
        }
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

    @NonNull
    private T[] copy(@NonNull final T[] values) {
        return Arrays.copyOf(values, values.length);
    }

    static final class Result<T> {
        @NonNull final T[] receivedValues;
        @NonNull final List<T[]> recalculatedPackets;
        final boolean added;

        Result(
                @NonNull final T[] receivedValues,
                @NonNull final List<T[]> recalculatedPackets,
                final boolean added
        ) {
            this.receivedValues = receivedValues;
            this.recalculatedPackets = recalculatedPackets;
            this.added = added;
        }
    }

    private static final class Packet<T> {
        @NonNull final T[] rawValues;
        T[] calculatedValues;
        OrpheMadgwickFilter.State stateAfter;

        Packet(@NonNull final T[] rawValues) {
            this.rawValues = rawValues;
        }
    }
}
