package io.orphe.orphecoresdk;

import androidx.annotation.NonNull;

import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class OrpheInsoleValueAccumulatorTest {
    @Test
    public void notifiesOutOfOrderValueImmediatelyAndMergesRecoveredGap() {
        OrpheInsoleValueAccumulator accumulator = new OrpheInsoleValueAccumulator();

        OrpheInsoleValueUpdate first = accumulator.add(packet(10));
        OrpheInsoleValueUpdate afterGap = accumulator.add(packet(12));

        assertNotNull(first);
        assertNotNull(afterGap);
        assertSerials(new int[]{12}, afterGap.getDeltaValues());
        assertSerials(new int[]{10, 12}, afterGap.getAllValues());

        OrpheInsoleValueUpdate recovered = accumulator.add(packet(11));

        assertNotNull(recovered);
        assertSerials(new int[]{11}, recovered.getDeltaValues());
        assertSerials(new int[]{10, 11, 12}, recovered.getAllValues());
    }

    @Test
    public void duplicateRequestResponseDoesNotNotifyTwice() {
        OrpheInsoleValueAccumulator accumulator = new OrpheInsoleValueAccumulator();

        assertNotNull(accumulator.add(packet(20)));
        assertNull(accumulator.add(packet(20)));
        assertSerials(new int[]{20}, accumulator.snapshot());
    }

    @Test
    public void updateKeepsSnapshotFromItsOwnCallbackTime() {
        OrpheInsoleValueAccumulator accumulator = new OrpheInsoleValueAccumulator();
        accumulator.add(packet(30));
        OrpheInsoleValueUpdate beforeRecovery = accumulator.add(packet(32));
        accumulator.add(packet(31));

        assertNotNull(beforeRecovery);
        assertSerials(new int[]{30, 32}, beforeRecovery.getAllValues());
        assertSerials(new int[]{30, 31, 32}, accumulator.snapshot());
    }

    @Test
    public void serialNumberRolloverAndOlderRecoveryStayOrdered() {
        OrpheInsoleValueAccumulator accumulator = new OrpheInsoleValueAccumulator();
        accumulator.add(packet(65535));
        accumulator.add(packet(0));
        accumulator.add(packet(65534));

        assertSerials(new int[]{65534, 65535, 0}, accumulator.snapshot());
    }

    @Test
    public void allValuesPreserveEveryValueWithinOneSerial() {
        OrpheInsoleValueAccumulator accumulator = new OrpheInsoleValueAccumulator();
        OrpheInsoleValueUpdate update = accumulator.add(packet(40, 4));

        assertNotNull(update);
        assertEquals(4, update.getDeltaValues().length);
        assertEquals(4, update.getAllValues().length);
        assertArrayEquals(new int[]{0, 1, 2, 3}, dataPositions(update.getAllValues()));
    }

    @Test
    public void updateCallbackDelegatesDeltaToExistingCallbackOverride() {
        OrpheInsoleValueAccumulator accumulator = new OrpheInsoleValueAccumulator();
        OrpheInsoleValueUpdate update = accumulator.add(packet(50, 2));
        final int[] receivedSerials = new int[2];
        OrpheInsoleCallback callback = new OrpheInsoleCallback() {
            @Override
            public void gotInsoleValues(OrpheInsoleValue[] insoleValues) {
                for (int i = 0; i < insoleValues.length; i++) {
                    receivedSerials[i] = insoleValues[i].serialNumber;
                }
            }
        };

        assertNotNull(update);
        callback.gotInsoleValues(update);

        assertArrayEquals(new int[]{50, 50}, receivedSerials);
    }

    @NonNull
    private static OrpheInsoleValue[] packet(final int serialNumber) {
        return packet(serialNumber, 1);
    }

    @NonNull
    private static OrpheInsoleValue[] packet(final int serialNumber, final int length) {
        OrpheInsoleValue[] values = new OrpheInsoleValue[length];
        for (int i = 0; i < length; i++) {
            values[i] = new OrpheInsoleValue(
                    OrpheSidePosition.leftPlantar,
                    serialNumber,
                    i,
                    1_000L + i,
                    1_001L + i,
                    0.0,
                    0.0,
                    0.0,
                    0.0,
                    0.0,
                    0.0,
                    0.0,
                    0.0,
                    0.0,
                    0.0,
                    0.0,
                    0.0,
                    2_000L
            );
        }
        return values;
    }

    private static void assertSerials(
            @NonNull final int[] expected,
            @NonNull final OrpheInsoleValue[] actual
    ) {
        int[] serials = new int[actual.length];
        for (int i = 0; i < actual.length; i++) {
            serials[i] = actual[i].serialNumber;
        }
        assertArrayEquals(expected, serials);
    }

    @NonNull
    private static int[] dataPositions(@NonNull final OrpheInsoleValue[] values) {
        int[] positions = new int[values.length];
        for (int i = 0; i < values.length; i++) {
            positions[i] = values[i].dataPosition;
        }
        return positions;
    }
}
