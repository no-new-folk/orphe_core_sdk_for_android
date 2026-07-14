package io.orphe.orphecoresdk;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

public class OrpheInsoleValueCalibrationTest {
    private static final double DELTA = 1.0E-9;

    @Test
    public void coefficientOptionsContainAllEditablePressureValues() {
        assertArrayEquals(
                new OrpheInsoleCoefficient[]{
                        OrpheInsoleCoefficient.coefficient1,
                        OrpheInsoleCoefficient.coefficient2,
                        OrpheInsoleCoefficient.coefficient3,
                        OrpheInsoleCoefficient.threshold
                },
                OrpheInsoleCoefficient.values()
        );
    }

    @Test
    public void pressureIsZeroAtOrBelowDefaultThreshold() {
        assertEquals(0.0, OrpheInsoleValue.milliVoltToNewton(239.0, null, null), DELTA);
        assertEquals(0.0, OrpheInsoleValue.milliVoltToNewton(240.0, null, null), DELTA);
    }

    @Test
    public void customThresholdIsUsed() {
        assertEquals(
                0.0,
                OrpheInsoleValue.milliVoltToNewton(300.0, 1.0, 0.0, 0.0, 300.0),
                DELTA
        );
        assertEquals(
                1.0,
                OrpheInsoleValue.milliVoltToNewton(300.0, 1.0, 0.0, 0.0, 299.0),
                DELTA
        );
    }

    @Test
    public void defaultCalibrationMatchesDemo010() {
        final double milliVolt = 300.0;
        final double expected = 2.77942 * Math.exp(0.00235 * milliVolt) + 4.14411;

        assertEquals(expected, OrpheInsoleValue.milliVoltToNewton(milliVolt, null, null), DELTA);
        assertCoefficient(
                2.77942,
                0.00235,
                4.14411,
                240.0,
                OrpheInsolePressureCoefficient.DEFAULT
        );
    }

    @Test
    public void legacyTwoValueApiUsesDefaultCoefficient2AndThreshold() {
        final double milliVolt = 300.0;
        final double expected = 1.5 * Math.exp(0.00235 * milliVolt) + 2.5;

        assertEquals(expected, OrpheInsoleValue.milliVoltToNewton(milliVolt, 1.5, 2.5), DELTA);
        assertCoefficient(
                1.5,
                0.00235,
                2.5,
                240.0,
                new OrpheInsolePressureCoefficient(1.5, 2.5)
        );
    }

    @Test
    public void allCustomPressureValuesAreUsed() {
        final double milliVolt = 300.0;
        final double expected = 1.5 * Math.exp(0.003 * milliVolt) + 2.5;

        assertEquals(
                expected,
                OrpheInsoleValue.milliVoltToNewton(300.0, 1.5, 0.003, 2.5, 200.0),
                DELTA
        );
    }

    @Test
    public void explicitlyConfiguredZeroCoefficientIsNotReplacedWithDefault() {
        assertEquals(7.0, OrpheInsoleValue.milliVoltToNewton(300.0, 0.0, 7.0), DELTA);
    }

    @Test
    public void negativeCalibrationResultIsClampedToZero() {
        assertEquals(0.0, OrpheInsoleValue.milliVoltToNewton(300.0, 1.0, -100.0), DELTA);
    }

    @Test
    public void invalidMilliVoltValuesAreZero() {
        assertEquals(0.0, OrpheInsoleValue.milliVoltToNewton(-1.0, null, null), DELTA);
        assertEquals(0.0, OrpheInsoleValue.milliVoltToNewton(10000.0, null, null), DELTA);
        assertEquals(0.0, OrpheInsoleValue.milliVoltToNewton(Double.NaN, null, null), DELTA);
        assertEquals(0.0, OrpheInsoleValue.milliVoltToNewton(Double.POSITIVE_INFINITY,
                null, null), DELTA);
    }

    @Test
    public void nonFiniteCalculationResultIsZero() {
        assertEquals(
                0.0,
                OrpheInsoleValue.milliVoltToNewton(
                        300.0, Double.POSITIVE_INFINITY, 0.00235, 4.14411, 240.0),
                DELTA
        );
    }

    @Test
    public void pressureCalibrationStoresDifferentCoefficientsForAllSensorPositions() {
        final OrpheInsolePressureCalibration calibration = distinctCalibration();

        assertCoefficient(1.0, 0.001, 10.0, 100.0, calibration.toeInside);
        assertCoefficient(2.0, 0.002, 20.0, 110.0, calibration.midInside);
        assertCoefficient(3.0, 0.003, 30.0, 120.0, calibration.toeOutside);
        assertCoefficient(4.0, 0.004, 40.0, 130.0, calibration.center);
        assertCoefficient(5.0, 0.005, 50.0, 140.0, calibration.midOutside);
        assertCoefficient(6.0, 0.006, 60.0, 150.0, calibration.heel);
    }

    @Test
    public void legacySingleCoefficientUpdateKeepsOtherValuesAndOriginalConfig() {
        final OrpheInsolePressureCalibration original = distinctCalibration();
        final OrpheInsolePressureCalibration updatedCoefficient1 = original.withCoefficient(
                OrpheInsoleSensorPosition.toeInside,
                OrpheInsoleCoefficient.coefficient1,
                0.0
        );
        final OrpheInsolePressureCalibration updatedCoefficient2 = original.withCoefficient(
                OrpheInsoleSensorPosition.toeInside,
                OrpheInsoleCoefficient.coefficient2,
                0.01
        );
        final OrpheInsolePressureCalibration updatedCoefficient3 = original.withCoefficient(
                OrpheInsoleSensorPosition.toeInside,
                OrpheInsoleCoefficient.coefficient3,
                30.0
        );
        final OrpheInsolePressureCalibration updatedThreshold = original.withCoefficient(
                OrpheInsoleSensorPosition.toeInside,
                OrpheInsoleCoefficient.threshold,
                350.0
        );

        assertCoefficient(1.0, 0.001, 10.0, 100.0, original.toeInside);
        assertCoefficient(0.0, 0.001, 10.0, 100.0, updatedCoefficient1.toeInside);
        assertCoefficient(1.0, 0.01, 10.0, 100.0, updatedCoefficient2.toeInside);
        assertCoefficient(1.0, 0.001, 30.0, 100.0, updatedCoefficient3.toeInside);
        assertCoefficient(1.0, 0.001, 10.0, 350.0, updatedThreshold.toeInside);
        assertSame(original.heel, updatedThreshold.heel);
    }

    @Test
    public void bothPacketFormatsUseDifferentCalibrationForAllSixSensorPositions() throws Exception {
        final OrpheInsolePressureCalibration calibration = distinctCalibration();

        assertPacketUsesAllSensorCoefficients(
                pressurePacket(54),
                calibration,
                OrpheInsoleValue.PACKET_LENGTH_200_HZ
        );
        assertPacketUsesAllSensorCoefficients(
                pressurePacket(56),
                calibration,
                OrpheInsoleValue.PACKET_LENGTH_100_HZ
        );
    }

    @Test
    public void packetConversionUsesCoefficientsForEachSensorPosition() throws Exception {
        final byte[] packet = new byte[OrpheInsoleValue.PACKET_LENGTH_100_HZ];
        packet[0] = 56;
        putUint16(packet, 28, 300);
        putUint16(packet, 60, 300);

        final Map<OrpheInsoleCoefficient, Double> toeInsideCoefficients = new HashMap<>();
        toeInsideCoefficients.put(OrpheInsoleCoefficient.coefficient1, 1.5);
        toeInsideCoefficients.put(OrpheInsoleCoefficient.coefficient2, 0.003);
        toeInsideCoefficients.put(OrpheInsoleCoefficient.coefficient3, 2.5);
        toeInsideCoefficients.put(OrpheInsoleCoefficient.threshold, 250.0);
        final Map<OrpheInsoleSensorPosition, Map<OrpheInsoleCoefficient, Double>> coefficientMap =
                new HashMap<>();
        coefficientMap.put(OrpheInsoleSensorPosition.toeInside, toeInsideCoefficients);

        final OrpheInsoleValue[] values = OrpheInsoleValue.fromBytes(
                packet,
                OrpheSidePosition.leftPlantar,
                OrpheAccRange.range16,
                OrpheGyroRange.range2000,
                coefficientMap,
                0L
        );

        final double expected = 1.5 * Math.exp(0.003 * 300.0) + 2.5;
        assertEquals(expected, values[0].pressureToeInside, DELTA);
        assertEquals(expected, values[1].pressureToeInside, DELTA);
        assertEquals(0.0, values[0].pressureHeel, DELTA);
    }

    @Test
    public void mapWithoutNewValuesUsesDefaultsForBackwardCompatibility() {
        final Map<OrpheInsoleCoefficient, Double> values = new HashMap<>();
        values.put(OrpheInsoleCoefficient.coefficient1, 1.5);
        values.put(OrpheInsoleCoefficient.coefficient3, 2.5);
        final Map<OrpheInsoleSensorPosition, Map<OrpheInsoleCoefficient, Double>> coefficientMap =
                new HashMap<>();
        coefficientMap.put(OrpheInsoleSensorPosition.toeInside, values);

        final OrpheInsolePressureCalibration calibration =
                OrpheInsolePressureCalibration.fromMap(coefficientMap);

        assertCoefficient(1.5, 0.00235, 2.5, 240.0, calibration.toeInside);
    }

    private static OrpheInsolePressureCalibration distinctCalibration() {
        return new OrpheInsolePressureCalibration(
                new OrpheInsolePressureCoefficient(1.0, 0.001, 10.0, 100.0),
                new OrpheInsolePressureCoefficient(2.0, 0.002, 20.0, 110.0),
                new OrpheInsolePressureCoefficient(3.0, 0.003, 30.0, 120.0),
                new OrpheInsolePressureCoefficient(4.0, 0.004, 40.0, 130.0),
                new OrpheInsolePressureCoefficient(5.0, 0.005, 50.0, 140.0),
                new OrpheInsolePressureCoefficient(6.0, 0.006, 60.0, 150.0)
        );
    }

    private static byte[] pressurePacket(final int packetType) {
        final int packetLength = packetType == 56
                ? OrpheInsoleValue.PACKET_LENGTH_100_HZ
                : OrpheInsoleValue.PACKET_LENGTH_200_HZ;
        final byte[] packet = new byte[packetLength];
        packet[0] = (byte) packetType;
        final int valueCount = packetType == 56 ? 2 : 4;
        final int valueLength = packetType == 56 ? 32 : 24;
        final int pressureOffset = packetType == 56 ? 20 : 12;
        for (int valueIndex = 0; valueIndex < valueCount; valueIndex++) {
            final int index = 8 + valueIndex * valueLength + pressureOffset;
            for (int sensorIndex = 0; sensorIndex < 6; sensorIndex++) {
                putUint16(packet, index + sensorIndex * 2, 300);
            }
        }
        return packet;
    }

    private static void assertPacketUsesAllSensorCoefficients(
            final byte[] packet,
            final OrpheInsolePressureCalibration calibration,
            final int expectedPacketLength
    ) throws Exception {
        assertEquals(expectedPacketLength, packet.length);
        final OrpheInsoleValue[] values = OrpheInsoleValue.fromBytes(
                packet,
                OrpheSidePosition.leftPlantar,
                OrpheAccRange.range16,
                OrpheGyroRange.range2000,
                calibration,
                0L
        );
        assertEquals(
                expectedPacketLength == OrpheInsoleValue.PACKET_LENGTH_100_HZ ? 2 : 4,
                values.length
        );

        for (final OrpheInsoleValue value : values) {
            assertEquals(expectedPressure(calibration.toeInside), value.pressureToeInside, DELTA);
            assertEquals(expectedPressure(calibration.midInside), value.pressureMidInside, DELTA);
            assertEquals(expectedPressure(calibration.toeOutside), value.pressureToeOutside, DELTA);
            assertEquals(expectedPressure(calibration.center), value.pressureCenter, DELTA);
            assertEquals(expectedPressure(calibration.midOutside), value.pressureMidOutside, DELTA);
            assertEquals(expectedPressure(calibration.heel), value.pressureHeel, DELTA);
        }
    }

    private static double expectedPressure(final OrpheInsolePressureCoefficient coefficient) {
        return coefficient.coefficient1
                * Math.exp(coefficient.coefficient2 * 300.0)
                + coefficient.coefficient3;
    }

    private static void assertCoefficient(
            final double coefficient1,
            final double coefficient2,
            final double coefficient3,
            final double threshold,
            final OrpheInsolePressureCoefficient actual
    ) {
        assertEquals(coefficient1, actual.coefficient1, DELTA);
        assertEquals(coefficient2, actual.coefficient2, DELTA);
        assertEquals(coefficient3, actual.coefficient3, DELTA);
        assertEquals(threshold, actual.threshold, DELTA);
    }

    private static void putUint16(byte[] data, int index, int value) {
        data[index] = (byte) ((value >> 8) & 0xFF);
        data[index + 1] = (byte) (value & 0xFF);
    }
}
