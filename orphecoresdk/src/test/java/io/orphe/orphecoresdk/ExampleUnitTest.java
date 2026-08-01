package io.orphe.orphecoresdk;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class ExampleUnitTest {
    @Test
    public void addition_isCorrect() {
        assertEquals(4, 2 + 2);
    }

    @Test
    public void insole200HzPacketHasFourValues() throws Exception {
        byte[] packet = new byte[OrpheInsoleValue.PACKET_LENGTH_200_HZ];
        packet[0] = 55;
        packet[1] = 0x12;
        packet[2] = 0x34;
        packet[3] = 10;
        packet[4] = 20;
        packet[5] = 30;
        packet[6] = 0;
        packet[7] = 0;

        OrpheInsoleValue[] values = OrpheInsoleValue.fromBytes(
                packet,
                OrpheSidePosition.leftPlantar,
                OrpheAccRange.range16,
                OrpheGyroRange.range2000,
                new java.util.HashMap<>(),
                0L
        );

        assertEquals(4, values.length);
        assertEquals(0x1234, values[0].serialNumber);
        assertEquals(3, values[0].dataPosition);
        assertEquals(0, values[3].dataPosition);
        // 3フレーム × 4.808ms（実測ODR 208Hz） = 14.42ms → epochミリ秒への切り捨てで14
        assertEquals(14, values[3].startTime - values[0].startTime);
        for (OrpheInsoleValue value : values) {
            assertEquals(0.0, value.quatW, 0.0);
            assertEquals(0.0, value.quatX, 0.0);
            assertEquals(0.0, value.quatY, 0.0);
            assertEquals(0.0, value.quatZ, 0.0);
        }
    }

    @Test
    public void insoleRequestPacketCanBeDownsampledTo100Hz() throws Exception {
        byte[] packet = requestPacket();
        putInt16(packet, 3 * 24 + 8, 3000);
        putInt16(packet, 2 * 24 + 8, 2000);
        putInt16(packet, 1 * 24 + 8, 1000);
        putInt16(packet, 0 * 24 + 8, 0);

        OrpheInsoleValue[] values = OrpheInsoleValue.fromBytes(
                packet,
                OrpheSidePosition.leftPlantar,
                OrpheAccRange.range16,
                OrpheGyroRange.range2000,
                OrpheInsolePressureCalibration.DEFAULT,
                123L,
                OrpheInsoleSamplingRate.hz100
        );

        assertEquals(2, values.length);
        assertEquals(1, values[0].dataPosition);
        assertEquals(0, values[1].dataPosition);
        // 2フレーム × 4.808ms = 9.62ms → epochミリ秒への切り捨てで9
        assertEquals(9, values[1].startTime - values[0].startTime);
        assertEquals(3000.0 * OrpheGyroRange.range2000.sensitivity,
                values[0].gyroX, 1.0E-9);
        assertEquals(1000.0 * OrpheGyroRange.range2000.sensitivity,
                values[1].gyroX, 1.0E-9);
        assertEquals(123L, values[0].receivedAt);
    }

    @Test
    public void insoleRequestPacketKeepsFourValuesAt200Hz() throws Exception {
        OrpheInsoleValue[] values = OrpheInsoleValue.fromBytes(
                requestPacket(),
                OrpheSidePosition.leftPlantar,
                OrpheAccRange.range16,
                OrpheGyroRange.range2000,
                OrpheInsolePressureCalibration.DEFAULT,
                0L,
                OrpheInsoleSamplingRate.hz200
        );

        assertEquals(4, values.length);
        assertEquals(3, values[0].dataPosition);
        assertEquals(0, values[3].dataPosition);
        // 3フレーム × 4.808ms = 14.42ms → 14
        assertEquals(14, values[3].startTime - values[0].startTime);
    }

    @Test
    public void insole100HzPacketHasTwoValues() throws Exception {
        byte[] packet = new byte[OrpheInsoleValue.PACKET_LENGTH_100_HZ];
        packet[0] = 56;
        packet[1] = 0x12;
        packet[2] = 0x34;
        packet[3] = 10;
        packet[4] = 20;
        packet[5] = 30;
        packet[6] = 0;
        packet[7] = 0;
        putInt16(packet, 8, 0x4000);
        putInt16(packet, 10, 0x00FF);
        putInt16(packet, 12, -256);
        putInt16(packet, 14, 0x2000);
        putInt16(packet, 40, 0x4000);
        putInt16(packet, 42, 0x00FF);
        putInt16(packet, 44, -256);
        putInt16(packet, 46, 0x2000);

        OrpheInsoleValue[] values = OrpheInsoleValue.fromBytes(
                packet,
                OrpheSidePosition.leftPlantar,
                OrpheAccRange.range16,
                OrpheGyroRange.range2000,
                new java.util.HashMap<>(),
                0L
        );

        assertEquals(2, values.length);
        assertEquals(0x1234, values[0].serialNumber);
        assertEquals(1, values[0].dataPosition);
        assertEquals(0, values[1].dataPosition);
        // 100Hzは208Hzストリームの1/2間引き: 2フレーム × 4.808ms = 9.62ms → 9
        assertEquals(9, values[1].startTime - values[0].startTime);
        for (OrpheInsoleValue value : values) {
            assertEquals(1.0, value.quatW, 1.0E-9);
            assertEquals(255.0 / 16384.0, value.quatX, 1.0E-9);
            assertEquals(-256.0 / 16384.0, value.quatY, 1.0E-9);
            assertEquals(0.5, value.quatZ, 1.0E-9);
        }
    }

    @Test
    public void insoleSamplingRateMapsToDeviceModes() {
        assertEquals(4, OrpheInsoleSamplingRate.hz100.sensorRequestModeValue);
        assertEquals(56, OrpheInsoleSamplingRate.hz100.sensorValueHeader);
        assertEquals(2, OrpheInsoleSamplingRate.hz100.valuesPerSerialNumber);
        assertEquals(3, OrpheInsoleSamplingRate.hz200.sensorRequestModeValue);
        assertEquals(55, OrpheInsoleSamplingRate.hz200.sensorValueHeader);
        assertEquals(4, OrpheInsoleSamplingRate.hz200.valuesPerSerialNumber);
    }

    @Test
    public void short100HzPacketIsRejected() {
        byte[] packet = new byte[OrpheInsoleValue.PACKET_LENGTH_100_HZ - 1];
        packet[0] = 56;

        assertPacketIsRejected(packet);
    }

    @Test
    public void short200HzPacketIsRejected() {
        byte[] packet = new byte[OrpheInsoleValue.PACKET_LENGTH_200_HZ - 1];
        packet[0] = 55;

        assertPacketIsRejected(packet);
    }

    @Test
    public void invalidPacketTimeIsRejected() {
        byte[] packet = new byte[OrpheInsoleValue.PACKET_LENGTH_100_HZ];
        packet[0] = 56;
        packet[3] = 25;

        assertPacketIsRejected(packet);
    }

    @Test
    public void serialDistanceClassifiesForwardDuplicateBackwardAndWrap() {
        assertEquals(1, OrpheInsole.forwardSerialDistance(100, 101));
        assertEquals(0, OrpheInsole.forwardSerialDistance(100, 100));
        assertEquals(-1, OrpheInsole.forwardSerialDistance(101, 100));
        assertEquals(1, OrpheInsole.forwardSerialDistance(65535, 0));
    }

    @Test
    public void onlySmallForwardGapsAreRecoverable() {
        assertFalse(OrpheInsole.isRecoverableGap(0, 100));
        assertFalse(OrpheInsole.isRecoverableGap(1, 100));
        assertFalse(OrpheInsole.isRecoverableGap(-1, 100));
        assertTrue(OrpheInsole.isRecoverableGap(2, 100));
        assertTrue(OrpheInsole.isRecoverableGap(101, 100));
        assertFalse(OrpheInsole.isRecoverableGap(102, 100));
    }

    private void assertPacketIsRejected(byte[] packet) {
        try {
            OrpheInsoleValue.fromBytes(
                    packet,
                    OrpheSidePosition.leftPlantar,
                    OrpheAccRange.range16,
                    OrpheGyroRange.range2000,
                    new java.util.HashMap<>(),
                    0L
            );
            fail("Expected an invalid packet to be rejected.");
        } catch (Exception expected) {
            assertNotNull(expected.getMessage());
        }
    }

    @Test
    public void insoleGyroUsesDatasheetSensitivityForEveryRange() throws Exception {
        final OrpheGyroRange[] ranges = {
                OrpheGyroRange.range250,
                OrpheGyroRange.range500,
                OrpheGyroRange.range1000,
                OrpheGyroRange.range2000
        };
        // LSM6DSOXのデータシート感度[dps/LSB]。生値1000のときのdps値。
        final double[] expected = {8.75, 17.5, 35.0, 70.0};

        for (int i = 0; i < ranges.length; i++) {
            byte[] packet = requestPacket();
            putInt16(packet, 3 * 24 + 8, 1000);

            OrpheInsoleValue[] values = OrpheInsoleValue.fromBytes(
                    packet,
                    OrpheSidePosition.leftPlantar,
                    OrpheAccRange.range16,
                    ranges[i],
                    OrpheInsolePressureCalibration.DEFAULT,
                    0L,
                    OrpheInsoleSamplingRate.hz200
            );

            assertEquals(ranges[i].name(), expected[i], values[0].gyroX, 1.0E-9);
        }
    }

    private static void putInt16(byte[] packet, int index, int value) {
        packet[index] = (byte) ((value >> 8) & 0xFF);
        packet[index + 1] = (byte) (value & 0xFF);
    }

    private static byte[] requestPacket() {
        byte[] packet = new byte[OrpheInsoleValue.PACKET_LENGTH_200_HZ];
        packet[0] = 54;
        packet[1] = 0x12;
        packet[2] = 0x34;
        packet[3] = 10;
        packet[4] = 20;
        packet[5] = 30;
        packet[6] = 0;
        packet[7] = 0;
        return packet;
    }
}
