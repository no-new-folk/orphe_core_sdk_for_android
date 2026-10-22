package io.orphe.orphecoresdk;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

public class OrpheSensorValueTest {
    private static final double DELTA = 1.0E-9;

    /** ORPHE CORE（CR-3）の実機から受信したRealtimeパケット（0x32）。 */
    private static final String CORE_REALTIME_PACKET =
            "32002f0e231a0255cd40174b17afeb9800610010ffe706cc0325036005cd3d174c17a8eb98"
                    + "0074001cfff306d0031c037a0acd3a174d17a0eb9800820028fffc06cd030b03970ecd"
                    + "36174f1798eb9a008a0034000206c602ff03aecf000000000000000000000000";

    @Test
    public void coreRealtimePacketIsDecodedIntoFourSamples() throws Exception {
        final OrpheSensorValue[] values = OrpheSensorValue.fromBytes(
                hexToBytes(CORE_REALTIME_PACKET),
                OrpheSidePosition.leftPlantar,
                OrpheAccRange.range16,
                OrpheGyroRange.range2000,
                0L
        );

        assertEquals(4, values.length);
        for (OrpheSensorValue value : values) {
            assertEquals(47, value.serialNumber);
        }
    }

    @Test
    public void coreRealtimePacketUsesMillisecondTimestamps() throws Exception {
        final OrpheSensorValue[] values = OrpheSensorValue.fromBytes(
                hexToBytes(CORE_REALTIME_PACKET),
                OrpheSidePosition.leftPlantar,
                OrpheAccRange.range16,
                OrpheGyroRange.range2000,
                0L
        );

        // 時点は3→0の順に格納される。
        final OrpheSensorValue first = findByPosition(values, 0);
        final LocalDateTime firstTime = LocalDateTime.ofInstant(
                Instant.ofEpochMilli(first.startTime),
                ZoneOffset.UTC
        );
        assertEquals(14, firstTime.getHour());
        assertEquals(35, firstTime.getMinute());
        assertEquals(26, firstTime.getSecond());
        assertEquals(597, firstTime.getNano() / 1_000_000);

        assertEquals(5L, findByPosition(values, 1).startTime - first.startTime);
        assertEquals(10L, findByPosition(values, 2).startTime - first.startTime);
        assertEquals(14L, findByPosition(values, 3).startTime - first.startTime);
    }

    @Test
    public void coreRealtimePacketContainsDeviceQuaternionAndImu() throws Exception {
        final OrpheSensorValue[] values = OrpheSensorValue.fromBytes(
                hexToBytes(CORE_REALTIME_PACKET),
                OrpheSidePosition.leftPlantar,
                OrpheAccRange.range16,
                OrpheGyroRange.range2000,
                0L
        );

        final OrpheSensorValue first = findByPosition(values, 0);
        assertEquals((short) 0xcd40 / 16384.0, first.quatW, DELTA);
        assertEquals(0x174b / 16384.0, first.quatX, DELTA);
        assertEquals(0x0061 / 32768.0 * 2000, first.gyroX, DELTA);
        assertEquals(0x06cc / 32768.0 * 16, first.accX, DELTA);
    }

    private static OrpheSensorValue findByPosition(OrpheSensorValue[] values, int position) {
        for (OrpheSensorValue value : values) {
            if (value.dataPosition == position) {
                return value;
            }
        }
        throw new AssertionError("dataPosition " + position + " is not found.");
    }

    private static byte[] hexToBytes(String hex) {
        final byte[] bytes = new byte[hex.length() / 2];
        for (int i = 0; i < bytes.length; i++) {
            bytes[i] = (byte) Integer.parseInt(hex.substring(i * 2, i * 2 + 2), 16);
        }
        return bytes;
    }
}
