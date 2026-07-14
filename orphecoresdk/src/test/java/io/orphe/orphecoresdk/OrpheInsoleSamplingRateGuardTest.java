package io.orphe.orphecoresdk;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class OrpheInsoleSamplingRateGuardTest {
    @Test
    public void acceptsOnlyQuaternionPacketFor100Hz() {
        OrpheInsoleSamplingRateGuard guard = new OrpheInsoleSamplingRateGuard(3);

        assertFalse(guard.accepts(OrpheInsoleSamplingRate.hz100, 55));
        assertTrue(guard.accepts(OrpheInsoleSamplingRate.hz100, 56));
    }

    @Test
    public void acceptsRealtimeAndStoredPacketHeadersFor200Hz() {
        OrpheInsoleSamplingRateGuard realtimeGuard = new OrpheInsoleSamplingRateGuard(3);
        OrpheInsoleSamplingRateGuard storedGuard = new OrpheInsoleSamplingRateGuard(3);

        assertTrue(realtimeGuard.accepts(OrpheInsoleSamplingRate.hz200, 55));
        assertTrue(storedGuard.accepts(OrpheInsoleSamplingRate.hz200, 54));
    }

    @Test
    public void schedulesAtMostThreeRetriesAndCoalescesPendingRetry() {
        OrpheInsoleSamplingRateGuard guard = new OrpheInsoleSamplingRateGuard(3);

        assertTrue(guard.scheduleRetry());
        assertFalse(guard.scheduleRetry());
        guard.retryStarted();
        assertTrue(guard.scheduleRetry());
        guard.retryStarted();
        assertTrue(guard.scheduleRetry());
        guard.retryStarted();
        assertFalse(guard.scheduleRetry());
        assertEquals(3, guard.retryCount());
    }

    @Test
    public void matchingPacketCancelsFurtherRetriesUntilReset() {
        OrpheInsoleSamplingRateGuard guard = new OrpheInsoleSamplingRateGuard(3);
        assertTrue(guard.scheduleRetry());

        assertTrue(guard.accepts(OrpheInsoleSamplingRate.hz100, 56));
        assertFalse(guard.scheduleRetry());

        guard.reset();
        assertTrue(guard.scheduleRetry());
    }
}
