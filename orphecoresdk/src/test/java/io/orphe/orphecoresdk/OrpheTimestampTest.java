package io.orphe.orphecoresdk;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;

/** パケットのbyte6-7（ミリ秒）とDATE_TIMEのミリ秒が保持されることを検証します。 */
public class OrpheTimestampTest {

    @Test
    public void coreFifoPacketKeepsMillisecondsAndFiveMillisecondInterval() throws Exception {
        final OrpheSensorValue[] values = OrpheSensorValue.fromBytes(
                packet(0x36, 104),
                OrpheSidePosition.leftPlantar,
                OrpheAccRange.range16,
                OrpheGyroRange.range2000,
                0L
        );

        assertEquals(8, values.length);
        final long[] times = sortedStartTimes(values);
        assertBaseTime(times[0]);
        for (int i = 1; i < times.length; i++) {
            assertEquals(5L, times[i] - times[i - 1]);
        }
    }

    @Test
    public void insoleFifoPacketKeepsMilliseconds() throws Exception {
        final OrpheInsoleValue[] values = OrpheInsoleValue.fromBytes(
                packet(0x36, OrpheInsoleValue.PACKET_LENGTH_200_HZ),
                OrpheSidePosition.leftPlantar,
                OrpheAccRange.range16,
                OrpheGyroRange.range2000,
                OrpheInsolePressureCalibration.DEFAULT,
                0L
        );

        assertEquals(4, values.length);
        final long[] times = sortedStartTimes(values);
        assertBaseTime(times[0]);
        for (int i = 1; i < times.length; i++) {
            assertEquals(5L, times[i] - times[i - 1]);
        }
    }

    @Test
    public void insoleRealtime100HzPacketKeepsMilliseconds() throws Exception {
        final OrpheInsoleValue[] values = OrpheInsoleValue.fromBytes(
                packet(0x38, OrpheInsoleValue.PACKET_LENGTH_100_HZ),
                OrpheSidePosition.leftPlantar,
                OrpheAccRange.range16,
                OrpheGyroRange.range2000,
                OrpheInsolePressureCalibration.DEFAULT,
                0L
        );

        assertEquals(2, values.length);
        final long[] times = sortedStartTimes(values);
        assertBaseTime(times[0]);
        assertEquals(10L, times[1] - times[0]);
    }

    @Test
    public void dateTimeIsEncodedWithTenMillisecondResolution() {
        final LocalDateTime dateTime = LocalDateTime.of(2026, 11, 5, 14, 35, 26, 597_000_000);

        final byte[] bytes = OrpheDateTime.toBytes(dateTime);

        assertArrayEquals(new byte[]{26, 11, 5, 14, 35, 26, 59}, bytes);
        assertEquals(
                LocalDateTime.of(2026, 11, 5, 14, 35, 26, 590_000_000),
                OrpheDateTime.fromBytes(bytes)
        );
    }

    @Test
    public void dateTimeDecodesUnsignedSubSecond() {
        final LocalDateTime dateTime = OrpheDateTime.fromBytes(
                new byte[]{26, 12, 31, 23, 59, 59, 99});

        assertEquals(990, dateTime.getNano() / 1_000_000);
    }

    @Test(expected = IllegalArgumentException.class)
    public void dateTimeRejectsShortBytes() {
        OrpheDateTime.fromBytes(new byte[]{26, 11, 5, 14, 35, 26});
    }

    /** 14:35:26.597、シリアル番号47のパケットを作ります。 */
    private static byte[] packet(int type, int length) {
        final byte[] bytes = new byte[length];
        bytes[0] = (byte) type;
        bytes[1] = 0x00;
        bytes[2] = 0x2f;
        bytes[3] = 14;
        bytes[4] = 35;
        bytes[5] = 26;
        bytes[6] = 0x02;
        bytes[7] = 0x55;
        return bytes;
    }

    private static void assertBaseTime(long epochMillis) {
        final LocalDateTime time = LocalDateTime.ofInstant(
                Instant.ofEpochMilli(epochMillis), ZoneOffset.UTC);
        assertEquals(14, time.getHour());
        assertEquals(35, time.getMinute());
        assertEquals(26, time.getSecond());
        assertEquals(597, time.getNano() / 1_000_000);
    }

    private static long[] sortedStartTimes(OrpheSensorValue[] values) {
        final long[] times = new long[values.length];
        for (int i = 0; i < values.length; i++) {
            times[i] = values[i].startTime;
        }
        Arrays.sort(times);
        return times;
    }

    private static long[] sortedStartTimes(OrpheInsoleValue[] values) {
        final long[] times = new long[values.length];
        for (int i = 0; i < values.length; i++) {
            times[i] = values[i].startTime;
        }
        Arrays.sort(times);
        return times;
    }
}
