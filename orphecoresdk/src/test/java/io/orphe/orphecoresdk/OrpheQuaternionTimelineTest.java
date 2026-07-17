package io.orphe.orphecoresdk;

import androidx.annotation.NonNull;

import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class OrpheQuaternionTimelineTest {
    @Test
    public void insoleRecoveredGapRecalculatesTailToChronologicalResult() {
        OrpheInsoleValueAccumulator ordered = new OrpheInsoleValueAccumulator();
        OrpheQuaternionTimeline<OrpheInsoleValue> orderedTimeline =
                OrpheQuaternionTimelines.forInsole();
        add(ordered, orderedTimeline, insolePacket(1));
        add(ordered, orderedTimeline, insolePacket(2));
        add(ordered, orderedTimeline, insolePacket(3));

        OrpheInsoleValueAccumulator recovered = new OrpheInsoleValueAccumulator();
        OrpheQuaternionTimeline<OrpheInsoleValue> recoveredTimeline =
                OrpheQuaternionTimelines.forInsole();
        add(recovered, recoveredTimeline, insolePacket(1));
        OrpheInsoleValueUpdate beforeRecovery =
                add(recovered, recoveredTimeline, insolePacket(3));
        OrpheInsoleValue[] oldSnapshot = beforeRecovery.getAllValues();
        add(recovered, recoveredTimeline, insolePacket(2));

        assertQuaternionsEqual(ordered.snapshot(), recovered.snapshot());
        assertEquals(8, oldSnapshot.length);
        assertEquals(12, recovered.snapshot().length);
    }

    @Test
    public void coreRecoveredGapRecalculatesTailToChronologicalResult() {
        OrpheSensorValueAccumulator ordered = new OrpheSensorValueAccumulator();
        OrpheQuaternionTimeline<OrpheSensorValue> orderedTimeline =
                OrpheQuaternionTimelines.forCore();
        add(ordered, orderedTimeline, corePacket(65535));
        add(ordered, orderedTimeline, corePacket(0));
        add(ordered, orderedTimeline, corePacket(1));

        OrpheSensorValueAccumulator recovered = new OrpheSensorValueAccumulator();
        OrpheQuaternionTimeline<OrpheSensorValue> recoveredTimeline =
                OrpheQuaternionTimelines.forCore();
        add(recovered, recoveredTimeline, corePacket(65535));
        add(recovered, recoveredTimeline, corePacket(1));
        OrpheSensorValueUpdate update = add(recovered, recoveredTimeline, corePacket(0));

        assertNotNull(update);
        assertEquals(24, update.getAllValues().length);
        assertQuaternionsEqual(ordered.snapshot(), recovered.snapshot());
    }

    @Test
    public void insole100HzOutputUsesAllIntermediate200HzSamples() {
        OrpheQuaternionTimeline<OrpheInsoleValue> timeline =
                OrpheQuaternionTimelines.forInsole();

        OrpheQuaternionTimeline.Result<OrpheInsoleValue> result =
                timeline.add(insolePacket(10));
        OrpheInsoleValue[] output = OrpheInsoleValue.forOutputSamplingRate(
                result.receivedValues,
                OrpheInsoleSamplingRate.hz100
        );

        OrpheMadgwickFilter expectedFilter = new OrpheMadgwickFilter();
        expectedFilter.updateDps(0.0, 0.0, 1.0, 30.0, 20.0, 10.0);
        expectedFilter.updateDps(0.0, 0.0, 1.0, 30.0, 20.0, 10.0);
        OrpheQuaternion expectedAtTenMillis =
                expectedFilter.updateDps(0.0, 0.0, 1.0, 30.0, 20.0, 10.0);

        assertEquals(2, output.length);
        assertEquals(1, output[0].dataPosition);
        assertEquals(0, output[1].dataPosition);
        assertEquals(expectedAtTenMillis.w, output[1].quatW, 1.0E-12);
        assertEquals(expectedAtTenMillis.x, output[1].quatX, 1.0E-12);
        assertEquals(expectedAtTenMillis.y, output[1].quatY, 1.0E-12);
        assertEquals(expectedAtTenMillis.z, output[1].quatZ, 1.0E-12);
    }

    @Test
    public void coreCalculatedValueHasNormalizedQuaternionAndGravity() {
        OrpheQuaternionTimeline.Result<OrpheSensorValue> result =
                OrpheQuaternionTimelines.forCore().add(corePacket(20));
        OrpheSensorValue value = result.receivedValues[7];

        final double quaternionNorm = Math.sqrt(
                value.quatW * value.quatW
                        + value.quatX * value.quatX
                        + value.quatY * value.quatY
                        + value.quatZ * value.quatZ
        );
        final double gravityNorm = Math.sqrt(
                value.accOfGravityX * value.accOfGravityX
                        + value.accOfGravityY * value.accOfGravityY
                        + value.accOfGravityZ * value.accOfGravityZ
        );
        assertEquals(1.0, quaternionNorm, 1.0E-12);
        assertEquals(1.0, gravityNorm, 1.0E-12);
    }

    @Test
    public void coreAndInsoleUseSameQuaternionCalculation() {
        OrpheSensorValue core =
                OrpheQuaternionTimelines.forCore().add(corePacket(20)).receivedValues[0];
        OrpheInsoleValue insole =
                OrpheQuaternionTimelines.forInsole().add(insolePacket(20)).receivedValues[0];

        assertEquals(core.quatW, insole.quatW, 1.0E-12);
        assertEquals(core.quatX, insole.quatX, 1.0E-12);
        assertEquals(core.quatY, insole.quatY, 1.0E-12);
        assertEquals(core.quatZ, insole.quatZ, 1.0E-12);
    }

    @Test
    public void decodedInsoleRequestPacketReceivesCalculatedQuaternion() throws Exception {
        byte[] packet = requestPacket(54, 30);
        OrpheInsoleValue[] decoded = OrpheInsoleValue.fromBytes(
                packet,
                OrpheSidePosition.leftPlantar,
                OrpheAccRange.range16,
                OrpheGyroRange.range2000,
                OrpheInsolePressureCalibration.DEFAULT,
                0L,
                OrpheInsoleSamplingRate.hz200
        );

        OrpheInsoleValue[] calculated =
                OrpheQuaternionTimelines.forInsole().add(decoded).receivedValues;

        assertEquals(0.0, decoded[3].quatZ, 0.0);
        assertTrue(Math.abs(calculated[3].quatZ) > 0.0);
        assertUnitQuaternion(
                calculated[3].quatW,
                calculated[3].quatX,
                calculated[3].quatY,
                calculated[3].quatZ
        );
    }

    @Test
    public void decodedCoreRequestPacketReceivesCalculatedQuaternionAndUnsignedSerial()
            throws Exception {
        byte[] packet = requestPacket(54, 30);
        packet[1] = (byte) 0xFF;
        packet[2] = (byte) 0xFF;
        OrpheSensorValue[] decoded = OrpheSensorValue.fromBytes(
                packet,
                OrpheSidePosition.leftInstep,
                OrpheAccRange.range16,
                OrpheGyroRange.range2000,
                0L
        );

        OrpheSensorValue[] calculated =
                OrpheQuaternionTimelines.forCore().add(decoded).receivedValues;

        assertEquals(65535, decoded[0].serialNumber);
        assertEquals(0.0, decoded[7].quatZ, 0.0);
        assertTrue(Math.abs(calculated[7].quatZ) > 0.0);
        assertUnitQuaternion(
                calculated[7].quatW,
                calculated[7].quatX,
                calculated[7].quatY,
                calculated[7].quatZ
        );
    }

    @Test
    public void coreUpdateCallbackDelegatesOnlyDeltaToExistingOverride() {
        OrpheSensorValueAccumulator accumulator = new OrpheSensorValueAccumulator();
        OrpheQuaternionTimeline<OrpheSensorValue> timeline =
                OrpheQuaternionTimelines.forCore();
        OrpheSensorValueUpdate update = add(accumulator, timeline, corePacket(42));
        final int[] receivedSerials = new int[8];
        OrpheCoreCallback callback = new OrpheCoreCallback() {
            @Override
            public void gotSensorValues(OrpheSensorValue[] sensorValues) {
                for (int i = 0; i < sensorValues.length; i++) {
                    receivedSerials[i] = sensorValues[i].serialNumber;
                }
            }
        };

        callback.gotSensorValues(update);

        assertArrayEquals(
                new int[]{42, 42, 42, 42, 42, 42, 42, 42},
                receivedSerials
        );
    }

    private static OrpheInsoleValueUpdate add(
            OrpheInsoleValueAccumulator accumulator,
            OrpheQuaternionTimeline<OrpheInsoleValue> timeline,
            OrpheInsoleValue[] values
    ) {
        OrpheQuaternionTimeline.Result<OrpheInsoleValue> result = timeline.add(values);
        return accumulator.add(result.receivedValues, result.recalculatedPackets);
    }

    private static OrpheSensorValueUpdate add(
            OrpheSensorValueAccumulator accumulator,
            OrpheQuaternionTimeline<OrpheSensorValue> timeline,
            OrpheSensorValue[] values
    ) {
        OrpheQuaternionTimeline.Result<OrpheSensorValue> result = timeline.add(values);
        return accumulator.add(result.receivedValues, result.recalculatedPackets);
    }

    @NonNull
    private static OrpheInsoleValue[] insolePacket(final int serialNumber) {
        OrpheInsoleValue[] values = new OrpheInsoleValue[4];
        for (int i = 0; i < values.length; i++) {
            values[i] = new OrpheInsoleValue(
                    OrpheSidePosition.leftPlantar,
                    serialNumber,
                    3 - i,
                    1_000L + i * 5L,
                    1_000L + i * 5L,
                    0.0,
                    0.0,
                    0.0,
                    0.0,
                    0.0,
                    0.0,
                    0.0,
                    0.0,
                    1.0,
                    30.0,
                    20.0,
                    10.0,
                    2_000L
            );
        }
        return values;
    }

    @NonNull
    private static OrpheSensorValue[] corePacket(final int serialNumber) {
        OrpheSensorValue[] values = new OrpheSensorValue[8];
        for (int i = 0; i < values.length; i++) {
            values[i] = new OrpheSensorValue(
                    OrpheSidePosition.leftInstep,
                    serialNumber,
                    7 - i,
                    1_000L + i * 5L,
                    1_000L + i * 5L,
                    0.0, 0.0, 0.0, 0.0, // quaternion
                    0.0, 0.0, 1.0,      // acceleration
                    30.0, 20.0, 10.0,   // gyroscope
                    0.0, 0.0, 0.0,      // gravity
                    0.0, 0.0, 0.0,      // normalized acceleration
                    0.0, 0.0, 0.0,      // normalized gyroscope
                    0.0,                 // normalized magnetic field
                    0.0, 0.0, 0.0,      // world-coordinate acceleration
                    0.0,                 // magnetic field
                    0,
                    0.0,
                    false,
                    0,
                    false,
                    2_000L
            );
        }
        return values;
    }

    private static void assertQuaternionsEqual(
            OrpheInsoleValue[] expected,
            OrpheInsoleValue[] actual
    ) {
        assertEquals(expected.length, actual.length);
        for (int i = 0; i < expected.length; i++) {
            assertEquals(expected[i].serialNumber, actual[i].serialNumber);
            assertEquals(expected[i].quatW, actual[i].quatW, 1.0E-12);
            assertEquals(expected[i].quatX, actual[i].quatX, 1.0E-12);
            assertEquals(expected[i].quatY, actual[i].quatY, 1.0E-12);
            assertEquals(expected[i].quatZ, actual[i].quatZ, 1.0E-12);
        }
    }

    private static void assertQuaternionsEqual(
            OrpheSensorValue[] expected,
            OrpheSensorValue[] actual
    ) {
        assertEquals(expected.length, actual.length);
        for (int i = 0; i < expected.length; i++) {
            assertEquals(expected[i].serialNumber, actual[i].serialNumber);
            assertEquals(expected[i].quatW, actual[i].quatW, 1.0E-12);
            assertEquals(expected[i].quatX, actual[i].quatX, 1.0E-12);
            assertEquals(expected[i].quatY, actual[i].quatY, 1.0E-12);
            assertEquals(expected[i].quatZ, actual[i].quatZ, 1.0E-12);
        }
    }

    private static byte[] requestPacket(final int type, final int gyroZDegreesPerSecond) {
        byte[] packet = new byte[OrpheInsoleValue.PACKET_LENGTH_200_HZ];
        packet[0] = (byte) type;
        packet[1] = 0;
        packet[2] = 1;
        packet[3] = 10;
        packet[4] = 20;
        packet[5] = 30;
        for (int sample = 0; sample < 8; sample++) {
            final int index = sample * 12 + 8;
            putInt16(
                    packet,
                    index + 4,
                    (int) Math.round(
                            gyroZDegreesPerSecond
                                    / (double) OrpheGyroRange.range2000.value
                                    * (1 << 15)
                    )
            );
            putInt16(packet, index + 10, 1 << 11);
        }
        return packet;
    }

    private static void putInt16(byte[] packet, int index, int value) {
        packet[index] = (byte) ((value >> 8) & 0xFF);
        packet[index + 1] = (byte) (value & 0xFF);
    }

    private static void assertUnitQuaternion(
            double w,
            double x,
            double y,
            double z
    ) {
        assertEquals(1.0, Math.sqrt(w * w + x * x + y * y + z * z), 1.0E-12);
    }
}
