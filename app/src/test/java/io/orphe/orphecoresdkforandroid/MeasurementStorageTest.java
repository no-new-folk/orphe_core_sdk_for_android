package io.orphe.orphecoresdkforandroid;

import org.junit.Test;

import java.util.Collections;

import io.orphe.orphecoresdk.OrpheInsoleSamplingRate;
import io.orphe.orphecoresdk.OrpheInsoleValue;
import io.orphe.orphecoresdk.OrpheSensorReceiveMode;
import io.orphe.orphecoresdk.OrpheSidePosition;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class MeasurementStorageTest {
    @Test
    public void realtime100HzCsvWritesQuaternionColumnsOnly() {
        OrpheInsoleValue value = createValue();

        String[] lines = MeasurementStorage.toCsv(
                Collections.singletonList(value),
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

    private static void assertQuaternionColumnsAreOmitted(
            OrpheSensorReceiveMode receiveMode,
            OrpheInsoleSamplingRate samplingRate
    ) {
        OrpheInsoleValue value = createValue();
        String[] lines = MeasurementStorage.toCsv(
                Collections.singletonList(value),
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
