package io.orphe.orphecoresdkforandroid;

import org.junit.Test;

import java.util.Collections;

import io.orphe.orphecoresdk.OrpheAccRange;
import io.orphe.orphecoresdk.OrpheGyroRange;
import io.orphe.orphecoresdk.OrpheInsoleSamplingRate;
import io.orphe.orphecoresdk.OrpheInsoleValue;
import io.orphe.orphecoresdk.OrpheSensorReceiveMode;
import io.orphe.orphecoresdk.OrpheSensorValue;
import io.orphe.orphecoresdk.OrpheSidePosition;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class MeasurementStorageTest {
    @Test
    public void realtime100HzCsvWritesQuaternionColumnsOnly() {
        OrpheInsoleValue value = createValue();

        String[] lines = MeasurementStorage.toCsv(
                Collections.singletonList(SensorSample.fromInsole(value)),
                OrpheSensorReceiveMode.realtime,
                OrpheInsoleSamplingRate.hz100
        ).split("\\n");
        String[] header = lines[0].split(",", -1);
        String[] row = lines[1].split(",", -1);

        assertEquals(21, header.length);
        assertEquals(header.length, row.length);
        assertEquals("quatW", header[7]);
        assertEquals("quatZ", header[10]);
        assertEquals(value.quatW, Double.parseDouble(row[7]), 0.0);
        assertEquals(value.quatZ, Double.parseDouble(row[10]), 0.0);
        assertFalse(lines[0].contains("euler"));
        assertEquals("pressureToeOutside", header[11]);
        assertEquals(value.pressureToeOutside, Double.parseDouble(row[11]), 0.0);
    }

    @Test
    public void realtime200HzCsvOmitsQuaternionColumns() {
        assertQuaternionColumnsAreOmitted(
                OrpheSensorReceiveMode.realtime,
                OrpheInsoleSamplingRate.hz200
        );
    }

    @Test
    public void requestCsvOmitsOrientationColumnsAtEverySamplingRate() {
        assertQuaternionColumnsAreOmitted(
                OrpheSensorReceiveMode.request,
                OrpheInsoleSamplingRate.hz100
        );
        assertQuaternionColumnsAreOmitted(
                OrpheSensorReceiveMode.request,
                OrpheInsoleSamplingRate.hz200
        );
    }

    @Test
    public void fifoCsvOmitsOrientationColumnsAtEverySamplingRate() {
        assertQuaternionColumnsAreOmitted(
                OrpheSensorReceiveMode.fifo,
                OrpheInsoleSamplingRate.hz100
        );
        assertQuaternionColumnsAreOmitted(
                OrpheSensorReceiveMode.fifo,
                OrpheInsoleSamplingRate.hz200
        );
    }

    @Test
    public void coreRealtimeCsvWritesQuaternionAndOmitsPressureColumns() {
        OrpheSensorValue value = createCoreValue();
        String[] lines = MeasurementStorage.toCsv(
                Collections.singletonList(SensorSample.fromCore(value)),
                OrpheSensorReceiveMode.realtime,
                OrpheInsoleSamplingRate.hz200
        ).split("\\n");
        String[] header = lines[0].split(",", -1);
        String[] row = lines[1].split(",", -1);

        assertEquals(15, header.length);
        assertEquals(header.length, row.length);
        assertFalse(lines[0].contains("pressure"));
        assertEquals("quatW", header[7]);
        assertEquals(value.quatW, Double.parseDouble(row[7]), 0.0);
        assertEquals("serialNumber", header[11]);
        assertEquals(Integer.toString(value.serialNumber), row[11]);
        assertEquals("leftPlantar", row[13]);
    }

    @Test
    public void coreFifoCsvOmitsQuaternionAndPressureColumns() {
        OrpheSensorValue value = createCoreValue();
        String[] lines = MeasurementStorage.toCsv(
                Collections.singletonList(SensorSample.fromCore(value)),
                OrpheSensorReceiveMode.fifo,
                OrpheInsoleSamplingRate.hz200
        ).split("\\n");
        String[] header = lines[0].split(",", -1);
        String[] row = lines[1].split(",", -1);

        assertEquals(11, header.length);
        assertEquals(header.length, row.length);
        assertFalse(lines[0].contains("pressure"));
        assertFalse(lines[0].contains("quat"));
        assertEquals("receivedAt", header[10]);
        assertEquals(Long.toString(value.receivedAt), row[10]);
    }

    private static OrpheSensorValue createCoreValue() throws RuntimeException {
        // ORPHE CORE（CR-3）の実機から受信したRealtimeパケット（0x32）。
        final String hex = "32002f0e231a0255cd40174b17afeb9800610010ffe706cc0325036005cd3d174c17a8eb98"
                + "0074001cfff306d0031c037a0acd3a174d17a0eb9800820028fffc06cd030b03970ecd"
                + "36174f1798eb9a008a0034000206c602ff03aecf000000000000000000000000";
        final byte[] bytes = new byte[hex.length() / 2];
        for (int i = 0; i < bytes.length; i++) {
            bytes[i] = (byte) Integer.parseInt(hex.substring(i * 2, i * 2 + 2), 16);
        }
        try {
            return OrpheSensorValue.fromBytes(
                    bytes,
                    OrpheSidePosition.leftPlantar,
                    OrpheAccRange.range16,
                    OrpheGyroRange.range2000,
                    3_000L
            )[0];
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static void assertQuaternionColumnsAreOmitted(
            OrpheSensorReceiveMode receiveMode,
            OrpheInsoleSamplingRate samplingRate
    ) {
        OrpheInsoleValue value = createValue();
        String[] lines = MeasurementStorage.toCsv(
                Collections.singletonList(SensorSample.fromInsole(value)),
                receiveMode,
                samplingRate
        ).split("\\n");
        String[] header = lines[0].split(",", -1);
        String[] row = lines[1].split(",", -1);

        assertEquals(17, header.length);
        assertEquals(header.length, row.length);
        assertFalse(lines[0].contains("quat"));
        assertFalse(lines[0].contains("euler"));
        assertEquals("pressureToeOutside", header[7]);
        assertEquals(value.pressureToeOutside, Double.parseDouble(row[7]), 0.0);
        assertEquals("serialNumber", header[13]);
        assertEquals(Integer.toString(value.serialNumber), row[13]);
        assertEquals("receivedAt", header[16]);
        assertEquals(Long.toString(value.receivedAt), row[16]);
    }

    private static OrpheInsoleValue createValue() {
        return new OrpheInsoleValue(
                OrpheSidePosition.leftPlantar,
                42,
                1,
                1_000L,
                1_000L,
                11.0, 12.0, 13.0, 14.0, 15.0, 16.0,
                21.0, 22.0, 23.0,
                31.0, 32.0, 33.0,
                1.0, 255.0 / 16384.0, -256.0 / 16384.0, 0.5,
                2_000L
        );
    }
}
