package io.orphe.orphecoresdk;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class OrpheMadgwickFilterTest {
    @Test
    public void identityIsStableForStationaryUpwardAcceleration() {
        OrpheMadgwickFilter filter = new OrpheMadgwickFilter();

        OrpheQuaternion quaternion = filter.updateDps(0.0, 0.0, 1.0, 0.0, 0.0, 0.0);

        assertEquals(1.0, quaternion.w, 1.0E-12);
        assertEquals(0.0, quaternion.x, 1.0E-12);
        assertEquals(0.0, quaternion.y, 1.0E-12);
        assertEquals(0.0, quaternion.z, 1.0E-12);
    }

    @Test
    public void gyroDegreesPerSecondIsConvertedToRadiansLikeFirmware() {
        OrpheMadgwickFilter filter = new OrpheMadgwickFilter();
        final double halfStep = 0.5
                * Math.toRadians(90.0)
                * OrpheMadgwickFilter.SAMPLE_INTERVAL_SECONDS;
        final double expectedNorm = Math.sqrt(1.0 + halfStep * halfStep);

        OrpheQuaternion quaternion = filter.updateDps(
                0.0, 0.0, 0.0, 90.0, 0.0, 0.0);

        assertEquals(1.0 / expectedNorm, quaternion.w, 1.0E-12);
        assertEquals(halfStep / expectedNorm, quaternion.x, 1.0E-12);
        assertEquals(0.0, quaternion.y, 1.0E-12);
        assertEquals(0.0, quaternion.z, 1.0E-12);
    }

    @Test
    public void axesAreNotReorderedOrSignFlipped() {
        OrpheQuaternion x = new OrpheMadgwickFilter().updateDps(
                0.0, 0.0, 0.0, 90.0, 0.0, 0.0);
        OrpheQuaternion y = new OrpheMadgwickFilter().updateDps(
                0.0, 0.0, 0.0, 0.0, 90.0, 0.0);
        OrpheQuaternion z = new OrpheMadgwickFilter().updateDps(
                0.0, 0.0, 0.0, 0.0, 0.0, 90.0);

        assertTrue(x.x > 0.0);
        assertEquals(0.0, x.y, 0.0);
        assertEquals(0.0, x.z, 0.0);
        assertTrue(y.y > 0.0);
        assertEquals(0.0, y.x, 0.0);
        assertEquals(0.0, y.z, 0.0);
        assertTrue(z.z > 0.0);
        assertEquals(0.0, z.x, 0.0);
        assertEquals(0.0, z.y, 0.0);
    }

    @Test
    public void invalidInputDoesNotCorruptState() {
        OrpheMadgwickFilter filter = new OrpheMadgwickFilter();
        filter.updateDps(0.0, 0.0, 1.0, 10.0, 20.0, 30.0);

        OrpheQuaternion before = quaternion(filter.snapshot());
        OrpheQuaternion after = filter.update(
                Double.NaN, 0.0, 1.0, 0.0, 0.0, 0.0, 0.005);

        assertEquals(before.w, after.w, 0.0);
        assertEquals(before.x, after.x, 0.0);
        assertEquals(before.y, after.y, 0.0);
        assertEquals(before.z, after.z, 0.0);
    }

    private static OrpheQuaternion quaternion(OrpheMadgwickFilter.State state) {
        return new OrpheQuaternion(state.q0, state.q1, state.q2, state.q3);
    }
}
