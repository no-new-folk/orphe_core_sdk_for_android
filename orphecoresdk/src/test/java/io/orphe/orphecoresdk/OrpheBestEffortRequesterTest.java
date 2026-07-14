package io.orphe.orphecoresdk;

import androidx.annotation.NonNull;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class OrpheBestEffortRequesterTest {
    @Test
    public void defaultConfigMatchesTokorotenSafetyValues() {
        OrpheBestEffortConfig config = OrpheBestEffortConfig.DEFAULT;

        assertEquals(200, config.requestLength);
        assertEquals(200L, config.requestIntervalMillis);
        assertEquals(5000L, config.requestTimeoutMillis);
        assertEquals(250L, OrpheBestEffortRequester.RESPONSE_IDLE_TIMEOUT_MILLIS);
        assertEquals(100, config.maxCarryOverSerials);
        assertEquals(1500, config.ringBufferCapacity);
    }

    @Test
    public void partialResponseUsesIdleTimeoutAndCarriesUnresolvedSerial() {
        Recorder recorder = new Recorder();
        OrpheBestEffortRequester<Integer> requester = new OrpheBestEffortRequester<>(
                20L,
                new OrpheBestEffortConfig(10, 100L, 5000L, 10, 1500),
                recorder
        );
        requester.start();
        requester.onCurrentState(3, 3, 0L);
        requester.onValue(1, 1, 100L);
        requester.onValue(3, 3, 110L);

        requester.tick(359L);
        assertTrue(requester.hasActiveRequest());
        assertEquals(1L, requester.nextTickDelayMillis(359L, 200L));

        requester.tick(360L);

        assertFalse(requester.hasActiveRequest());
        assertEquals(1, requester.carryOverCount());
        assertEquals(1, recorder.currentStateRequests);
        assertTrue(recorder.missing.isEmpty());
    }

    @Test
    public void idleTimeoutDoesNotStartUntilFirstExpectedResponse() {
        Recorder recorder = new Recorder();
        OrpheBestEffortRequester<Integer> requester = new OrpheBestEffortRequester<>(
                20L,
                new OrpheBestEffortConfig(10, 100L, 5000L, 10, 1500),
                recorder
        );
        requester.start();
        requester.onCurrentState(3, 3, 0L);

        requester.tick(1000L);

        assertTrue(requester.hasActiveRequest());
        assertEquals(0, requester.carryOverCount());
        assertEquals(0, recorder.currentStateRequests);

        requester.tick(5000L);

        assertFalse(requester.hasActiveRequest());
        assertEquals(3, requester.carryOverCount());
        assertEquals(1, recorder.currentStateRequests);
    }

    @Test
    public void currentStateBuildsInitialRangeFromAccumulatedCount() {
        Recorder recorder = new Recorder();
        OrpheBestEffortRequester<Integer> requester = requester(recorder);
        requester.start();

        requester.tick(1_000L);
        assertEquals(1, recorder.currentStateRequests);

        requester.onCurrentState(105, 5, 1_001L);

        assertEquals(Arrays.asList("101:5"), recorder.requests.get(0));
    }

    @Test
    public void timeoutCarriesHoleIntoNextRequestWithoutMarkingMissing() {
        Recorder recorder = new Recorder();
        OrpheBestEffortRequester<Integer> requester = requester(recorder);
        requester.start();
        requester.onCurrentState(3, 3, 0L);
        requester.onValue(1, 1, 1L);
        requester.onValue(3, 3, 2L);

        requester.tick(201L);
        requester.onCurrentState(4, 4, 202L);

        assertEquals(Arrays.asList("4:1", "2:1"), recorder.requests.get(1));
        assertTrue(recorder.missing.isEmpty());
        assertEquals(1, requester.carryOverCount());
    }

    @Test
    public void carryOverCanBeRecoveredAfterMultipleTimeouts() {
        Recorder recorder = new Recorder();
        OrpheBestEffortRequester<Integer> requester = requester(recorder);
        requester.start();
        requester.onCurrentState(2, 2, 0L);
        requester.onValue(1, 1, 1L);

        requester.tick(201L);
        requester.onCurrentState(3, 3, 202L);
        requester.onValue(3, 3, 203L);
        requester.tick(403L);
        requester.onCurrentState(4, 4, 404L);
        requester.onValue(2, 2, 405L);
        requester.onValue(4, 4, 406L);

        assertEquals(Arrays.asList(1, 3, 2, 4), recorder.values);
        assertTrue(recorder.missing.isEmpty());
        assertEquals(0, requester.carryOverCount());
        assertFalse(requester.hasActiveRequest());
    }

    @Test
    public void firmwareNoDataIsReportedAndNotCarriedOver() {
        Recorder recorder = new Recorder();
        OrpheBestEffortRequester<Integer> requester = requester(recorder);
        requester.start();
        requester.onCurrentState(3, 3, 0L);
        requester.onValue(1, 1, 1L);
        requester.onValue(3, 3, 2L);

        requester.onNotFound(2, 1, 3L);

        assertEquals(Arrays.asList(2), recorder.missing);
        assertEquals(0, requester.carryOverCount());
        assertFalse(requester.hasActiveRequest());
    }

    @Test
    public void noDataInNewRangeForcesLatestRangeResync() {
        Recorder recorder = new Recorder();
        OrpheBestEffortRequester<Integer> requester = requester(recorder);
        requester.start();
        requester.onCurrentState(3, 3, 0L);
        requester.onNotFound(1, 3, 1L);

        requester.tick(2L);
        requester.onCurrentState(10, 4, 3L);

        assertEquals(Arrays.asList("7:4"), recorder.requests.get(1));
    }

    @Test
    public void carryOverOverflowDropsPendingAndResyncs() {
        Recorder recorder = new Recorder();
        OrpheBestEffortConfig config = new OrpheBestEffortConfig(10, 100L, 200L, 2, 1500);
        OrpheBestEffortRequester<Integer> requester = new OrpheBestEffortRequester<>(
                20L,
                config,
                recorder
        );
        requester.start();
        requester.onCurrentState(3, 3, 0L);

        requester.tick(200L);
        assertEquals(0, requester.carryOverCount());
        requester.onCurrentState(5, 5, 201L);

        assertEquals(Arrays.asList("1:5"), recorder.requests.get(1));
    }

    @Test
    public void ringBufferOverflowSkipsOldestSerials() {
        Recorder recorder = new Recorder();
        OrpheBestEffortConfig config = new OrpheBestEffortConfig(200, 100L, 200L, 100, 1500);
        OrpheBestEffortRequester<Integer> requester = new OrpheBestEffortRequester<>(
                40L,
                config,
                recorder
        );
        requester.start();
        requester.onCurrentState(0, 1, 0L);
        requester.onValue(0, 0, 1L);
        requester.tick(2L);

        requester.onCurrentState(2000, 1500, 3L);

        assertEquals(Arrays.asList("501:200"), recorder.requests.get(1));
    }

    @Test
    public void commandUsesAtMostThirtyRangesIncludingNewRange() {
        Recorder recorder = new Recorder();
        OrpheBestEffortConfig config = new OrpheBestEffortConfig(200, 100L, 200L, 100, 1500);
        OrpheBestEffortRequester<Integer> requester = new OrpheBestEffortRequester<>(
                20L,
                config,
                recorder
        );
        requester.start();
        requester.onCurrentState(60, 60, 0L);
        for (int serial = 2; serial <= 60; serial += 2) {
            requester.onValue(serial, serial, 1L);
        }

        requester.tick(200L);
        requester.onCurrentState(61, 61, 201L);

        assertEquals(30, recorder.requests.get(1).size());
        assertEquals("61:1", recorder.requests.get(1).get(0));
        assertEquals(30, recorder.requestedSerialCounts.get(1).intValue());
    }

    @Test
    public void serialNumberWrapIsHandled() {
        Recorder recorder = new Recorder();
        OrpheBestEffortRequester<Integer> requester = requester(recorder);
        requester.start();

        requester.onCurrentState(1, 4, 0L);

        assertEquals(Arrays.asList("65534:4"), recorder.requests.get(0));
    }

    @Test
    public void stopDropsOldStateAndReconnectStartsFresh() {
        Recorder recorder = new Recorder();
        OrpheBestEffortRequester<Integer> requester = requester(recorder);
        requester.start();
        requester.onCurrentState(10, 10, 0L);
        assertTrue(requester.hasActiveRequest());

        requester.stop();
        assertFalse(requester.hasActiveRequest());
        requester.onValue(1, 1, 30L);
        assertTrue(recorder.values.isEmpty());

        requester.start();
        requester.onCurrentState(200, 1, 100L);
        assertEquals(Arrays.asList("200:1"), recorder.requests.get(1));
    }

    private OrpheBestEffortRequester<Integer> requester(Recorder recorder) {
        return new OrpheBestEffortRequester<>(
                20L,
                new OrpheBestEffortConfig(10, 100L, 200L, 10, 1500),
                recorder
        );
    }

    private static final class Recorder
            implements OrpheBestEffortRequester.Listener<Integer> {
        int currentStateRequests;
        final List<List<String>> requests = new ArrayList<>();
        final List<Integer> requestedSerialCounts = new ArrayList<>();
        final List<Integer> values = new ArrayList<>();
        final List<Integer> missing = new ArrayList<>();

        @Override
        public void onCurrentStateRequest() {
            currentStateRequests++;
        }

        @Override
        public void onRequest(@NonNull OrpheValueRequest[] values) {
            final ArrayList<String> ranges = new ArrayList<>();
            int total = 0;
            for (OrpheValueRequest value : values) {
                ranges.add(value.startSerialNumber + ":" + value.length);
                total += value.length;
            }
            requests.add(ranges);
            requestedSerialCounts.add(total);
        }

        @Override
        public void onValue(@NonNull Integer value) {
            values.add(value);
        }

        @Override
        public void onMissing(int serialNumber) {
            missing.add(serialNumber);
        }
    }
}
