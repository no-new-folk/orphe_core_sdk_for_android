package io.orphe.orphecoresdkforandroid;

import org.junit.Test;

import java.util.Collections;

import io.orphe.orphecoresdk.OrpheInsoleValue;
import io.orphe.orphecoresdk.OrpheSidePosition;

import static org.junit.Assert.assertEquals;

public class MeasurementStorageTest {
    @Test
    public void csvWritesEulerColumnsInYawPitchRollOrder() {
        OrpheInsoleValue value = new OrpheInsoleValue(
                OrpheSidePosition.leftPlantar,
                42,
                1,
                1_000L,
                1_000L,
                0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
                0.0, 0.0, 0.0,
                0.0, 0.0, 0.0,
                1.0, 255.0 / 16384.0, -256.0 / 16384.0, 0.5,
                2_000L
        );

        String[] lines = MeasurementStorage.toCsv(Collections.singletonList(value)).split("\\n");
        String[] header = lines[0].split(",", -1);
        String[] row = lines[1].split(",", -1);

        assertEquals(24, header.length);
        assertEquals(header.length, row.length);
        assertEquals("eulerYaw", header[11]);
        assertEquals("eulerPitch", header[12]);
        assertEquals("eulerRoll", header[13]);
        assertEquals(value.eulerYaw, Double.parseDouble(row[11]), 0.0);
        assertEquals(value.eulerPitch, Double.parseDouble(row[12]), 0.0);
        assertEquals(value.eulerRoll, Double.parseDouble(row[13]), 0.0);
    }
}
