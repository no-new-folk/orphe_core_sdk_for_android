package io.orphe.orphecoresdk;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class OrpheFifoInitializerTest {
    @Test
    public void acknowledgementsAdvanceStopClearStartInOrder() {
        Recorder recorder = new Recorder();
        OrpheFifoInitializer initializer = new OrpheFifoInitializer(recorder);

        initializer.start(0L);
        initializer.onAcknowledged(6, 1L);
        initializer.onAcknowledged(3, 2L);
        initializer.onAcknowledged(4, 3L);

        assertEquals(Arrays.asList(6, 3, 4), recorder.commands);
        assertEquals(1, recorder.completed);
        assertFalse(initializer.isRunning());
    }

    @Test
    public void missingAcknowledgementRetriesUpToTenAttempts() {
        Recorder recorder = new Recorder();
        OrpheFifoInitializer initializer = new OrpheFifoInitializer(recorder);
        initializer.start(0L);

        for (int attempt = 1; attempt < 10; attempt++) {
            initializer.tick(attempt * 1000L);
        }
        assertTrue(initializer.isRunning());
        initializer.tick(10_000L);

        assertEquals(10, recorder.commands.size());
        assertEquals(Arrays.asList(6), recorder.failures);
        assertFalse(initializer.isRunning());
    }

    private static final class Recorder implements OrpheFifoInitializer.Listener {
        final List<Integer> commands = new ArrayList<>();
        final List<Integer> failures = new ArrayList<>();
        int completed;

        @Override
        public void onCommand(int command) {
            commands.add(command);
        }

        @Override
        public void onComplete() {
            completed++;
        }

        @Override
        public void onFailure(int command) {
            failures.add(command);
        }
    }
}
