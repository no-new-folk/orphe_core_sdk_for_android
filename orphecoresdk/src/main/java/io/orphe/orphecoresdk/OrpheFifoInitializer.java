package io.orphe.orphecoresdk;

import androidx.annotation.NonNull;

/** FIFO開始前のstop/clear/start accumulationをACK付きで直列化する。 */
final class OrpheFifoInitializer {
    static final int COMMAND_CLEAR = 3;
    static final int COMMAND_START = 4;
    static final int COMMAND_STOP = 6;
    private static final int MAX_ATTEMPTS = 10;
    private static final long RETRY_INTERVAL_MILLIS = 1000L;

    interface Listener {
        void onCommand(int command);

        void onComplete();

        void onFailure(int command);
    }

    @NonNull private final Listener listener;
    private boolean running;
    private int command;
    private int attempts;
    private long deadlineMillis;

    OrpheFifoInitializer(@NonNull final Listener listener) {
        this.listener = listener;
    }

    void start(final long nowMillis) {
        running = true;
        beginCommand(COMMAND_STOP, nowMillis);
    }

    void stop() {
        running = false;
        command = 0;
        attempts = 0;
        deadlineMillis = 0L;
    }

    void tick(final long nowMillis) {
        if (!running || nowMillis < deadlineMillis) {
            return;
        }
        if (attempts >= MAX_ATTEMPTS) {
            final int failedCommand = command;
            stop();
            listener.onFailure(failedCommand);
            return;
        }
        send(nowMillis);
    }

    void onAcknowledged(final int acknowledgedCommand, final long nowMillis) {
        if (!running || acknowledgedCommand != command) {
            return;
        }
        switch (command) {
            case COMMAND_STOP:
                beginCommand(COMMAND_CLEAR, nowMillis);
                break;
            case COMMAND_CLEAR:
                beginCommand(COMMAND_START, nowMillis);
                break;
            case COMMAND_START:
                stop();
                listener.onComplete();
                break;
            default:
                break;
        }
    }

    boolean isRunning() {
        return running;
    }

    private void beginCommand(final int nextCommand, final long nowMillis) {
        command = nextCommand;
        attempts = 0;
        send(nowMillis);
    }

    private void send(final long nowMillis) {
        attempts++;
        deadlineMillis = nowMillis + RETRY_INTERVAL_MILLIS;
        listener.onCommand(command);
    }
}
