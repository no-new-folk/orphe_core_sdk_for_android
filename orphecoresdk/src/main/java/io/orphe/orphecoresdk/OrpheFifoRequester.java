package io.orphe.orphecoresdk;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Python版ところてんと同じcarry-over方式で継続取得するSDK内部状態管理。 */
final class OrpheFifoRequester<T> {
    static final int SERIAL_NUMBER_MODULUS = 65536;
    static final int MAX_REQUEST_RANGES = 30;
    static final long RESPONSE_IDLE_TIMEOUT_MILLIS = 250L;
    private static final int MAX_FORWARD_SERIAL_DISTANCE = SERIAL_NUMBER_MODULUS / 2;

    interface Listener<T> {
        /** 最新シリアル・蓄積件数の問い合わせを送信する。 */
        void onCurrentStateRequest();

        /** 新規範囲とcarry-over範囲をまとめた要求を送信する。 */
        void onRequest(@NonNull OrpheValueRequest[] requests);

        void onValue(@NonNull T value);

        /** FWがnoDataを返し、回復不能と確定したシリアルを通知する。 */
        void onMissing(int serialNumber);
    }

    private final long serialIntervalMillis;
    @NonNull private final OrpheFifoConfig config;
    @NonNull private final Listener<T> listener;
    private final LinkedHashSet<Integer> carryOverSerials = new LinkedHashSet<>();

    private boolean running;
    private Integer lastSerialNumber;
    private ActiveRequest activeRequest;

    OrpheFifoRequester(
            final long serialIntervalMillis,
            @NonNull final OrpheFifoConfig config,
            @NonNull final Listener<T> listener
    ) {
        if (serialIntervalMillis < 1L) {
            throw new IllegalArgumentException("serialIntervalMillis must be positive.");
        }
        this.serialIntervalMillis = serialIntervalMillis;
        this.config = config;
        this.listener = listener;
    }

    void start() {
        reset();
        running = true;
    }

    void stop() {
        running = false;
        reset();
    }

    /**
     * 応答待ち中なら期限を確認し、待機していなければ最新状態を問い合わせる。
     */
    void tick(final long nowMillis) {
        if (!running) {
            return;
        }
        if (activeRequest != null) {
            if (activeRequest.hasTimedOut(nowMillis)) {
                finishActiveRequest(true);
            } else {
                return;
            }
        }
        listener.onCurrentStateRequest();
    }

    /**
     * 通常のポーリング間隔と、応答受信後の無通信期限のうち早い方を返す。
     * 応答がまだ1件もない場合は総タイムアウトまで通常間隔で監視する。
     */
    long nextTickDelayMillis(final long nowMillis, final long normalIntervalMillis) {
        if (normalIntervalMillis < 1L) {
            throw new IllegalArgumentException("normalIntervalMillis must be positive.");
        }
        if (activeRequest == null) {
            return normalIntervalMillis;
        }

        long delayMillis = Math.min(
                normalIntervalMillis,
                remainingMillis(activeRequest.deadlineMillis, nowMillis)
        );
        if (activeRequest.hasProgress) {
            delayMillis = Math.min(
                    delayMillis,
                    remainingMillis(
                            activeRequest.lastProgressAtMillis + RESPONSE_IDLE_TIMEOUT_MILLIS,
                            nowMillis
                    )
            );
        }
        return delayMillis;
    }

    /** FWのcurrentSerial応答を受け、新規値とcarry-overを1コマンドへまとめる。 */
    void onCurrentState(
            final int currentSerialNumber,
            final int accumulatedCount,
            final long nowMillis
    ) {
        if (!running || activeRequest != null) {
            return;
        }

        final int current = normalizeSerialNumber(currentSerialNumber);
        final NewRange newRange = calculateNewRange(current, accumulatedCount);
        final int reservedRanges = newRange.count > 0 ? 1 : 0;
        final int maxCarryRanges = MAX_REQUEST_RANGES - reservedRanges;
        final List<OrpheValueRequest> carryRequests = buildCarryOverRequests(
                maxCarryRanges,
                config.requestLength
        );

        int carryCount = 0;
        for (OrpheValueRequest request : carryRequests) {
            carryCount += request.length;
        }
        final int remainingForNew = Math.max(0, config.requestLength - carryCount);
        final int newCount = Math.min(newRange.count, remainingForNew);

        final ArrayList<OrpheValueRequest> requests = new ArrayList<>();
        if (newCount > 0) {
            requests.add(new OrpheValueRequest(newRange.startSerialNumber, newCount));
        }
        requests.addAll(carryRequests);
        if (requests.isEmpty()) {
            return;
        }

        activeRequest = new ActiveRequest(
                requests,
                newRange.startSerialNumber,
                newCount,
                nowMillis + config.requestTimeoutMillis
        );
        listener.onRequest(requests.toArray(new OrpheValueRequest[0]));
    }

    void onValue(final int serialNumber, @NonNull final T value, final long nowMillis) {
        if (!running) {
            return;
        }
        final int normalized = normalizeSerialNumber(serialNumber);
        if (activeRequest != null && activeRequest.expectedSerials.contains(normalized)) {
            if (activeRequest.resolvedSerials.add(normalized)) {
                activeRequest.markProgress(nowMillis);
                carryOverSerials.remove(normalized);
                listener.onValue(value);
                completeRequestIfResolved();
            }
            return;
        }

        // タイムアウト後、次の要求を作る前に遅延パケットが届いた場合も回収する。
        if (carryOverSerials.remove(normalized)) {
            listener.onValue(value);
        }
    }

    void onNotFound(final int startSerialNumber, final int length, final long nowMillis) {
        if (!running || activeRequest == null || length <= 0) {
            return;
        }
        final int boundedLength = Math.min(length, SERIAL_NUMBER_MODULUS - 1);
        boolean progressed = false;
        for (int i = 0; i < boundedLength; i++) {
            final int serialNumber = normalizeSerialNumber(startSerialNumber + i);
            if (!activeRequest.expectedSerials.contains(serialNumber)
                    || !activeRequest.resolvedSerials.add(serialNumber)) {
                continue;
            }
            progressed = true;
            carryOverSerials.remove(serialNumber);
            if (activeRequest.newSerials.contains(serialNumber)) {
                activeRequest.resyncAfterCompletion = true;
            }
            listener.onMissing(serialNumber);
        }
        if (progressed) {
            activeRequest.markProgress(nowMillis);
        }
        completeRequestIfResolved();
    }

    boolean isRunning() {
        return running;
    }

    boolean hasActiveRequest() {
        return activeRequest != null;
    }

    boolean isAnchorKnown() {
        return lastSerialNumber != null;
    }

    int carryOverCount() {
        return carryOverSerials.size();
    }

    private NewRange calculateNewRange(final int currentSerialNumber, final int accumulatedCount) {
        if (lastSerialNumber == null) {
            final int count = Math.min(
                    Math.max(0, accumulatedCount),
                    config.requestLength
            );
            final int start = count > 0
                    ? normalizeSerialNumber(currentSerialNumber - (count - 1))
                    : 0;
            return new NewRange(start, count);
        }

        int needed = normalizeSerialNumber(currentSerialNumber - lastSerialNumber);
        if (needed > config.ringBufferCapacity) {
            final int skipped = needed - config.ringBufferCapacity;
            lastSerialNumber = normalizeSerialNumber(lastSerialNumber + skipped);
            carryOverSerials.clear();
            needed = config.ringBufferCapacity;
        }
        return new NewRange(nextSerialNumber(lastSerialNumber), needed);
    }

    @NonNull
    private List<OrpheValueRequest> buildCarryOverRequests(
            final int maxRanges,
            final int maxSerials
    ) {
        if (carryOverSerials.isEmpty() || maxRanges <= 0 || maxSerials <= 0) {
            return Collections.emptyList();
        }
        final ArrayList<Integer> serials = new ArrayList<>(carryOverSerials);
        Collections.sort(serials);
        final ArrayList<OrpheValueRequest> result = new ArrayList<>();
        int index = 0;
        int selected = 0;
        while (index < serials.size() && result.size() < maxRanges && selected < maxSerials) {
            final int start = serials.get(index);
            int length = 1;
            index++;
            while (index < serials.size()
                    && serials.get(index) == serials.get(index - 1) + 1
                    && selected + length < maxSerials) {
                length++;
                index++;
            }
            final int boundedLength = Math.min(length, maxSerials - selected);
            result.add(new OrpheValueRequest(start, boundedLength));
            selected += boundedLength;
        }
        return result;
    }

    private void completeRequestIfResolved() {
        if (activeRequest != null
                && activeRequest.resolvedSerials.size() >= activeRequest.expectedSerials.size()) {
            finishActiveRequest(false);
        }
    }

    private void finishActiveRequest(final boolean timedOut) {
        if (activeRequest == null) {
            return;
        }
        if (timedOut) {
            for (int serialNumber : activeRequest.expectedSerials) {
                if (!activeRequest.resolvedSerials.contains(serialNumber)) {
                    carryOverSerials.add(serialNumber);
                }
            }
        }

        if (activeRequest.resyncAfterCompletion) {
            lastSerialNumber = null;
            carryOverSerials.clear();
        } else if (activeRequest.newCount > 0) {
            lastSerialNumber = normalizeSerialNumber(
                    activeRequest.newStartSerialNumber + activeRequest.newCount - 1
            );
        }
        activeRequest = null;

        if (carryOverSerials.size() > config.maxCarryOverSerials) {
            carryOverSerials.clear();
            lastSerialNumber = null;
        }
    }

    private void reset() {
        lastSerialNumber = null;
        activeRequest = null;
        carryOverSerials.clear();
    }

    static int normalizeSerialNumber(final int serialNumber) {
        int normalized = serialNumber % SERIAL_NUMBER_MODULUS;
        if (normalized < 0) {
            normalized += SERIAL_NUMBER_MODULUS;
        }
        return normalized;
    }

    static int nextSerialNumber(final int serialNumber) {
        return normalizeSerialNumber(serialNumber + 1);
    }

    static int forwardSerialDistance(final int previous, final int current) {
        final int distance = normalizeSerialNumber(current - previous);
        if (distance == 0) {
            return 0;
        }
        return distance <= MAX_FORWARD_SERIAL_DISTANCE ? distance : -1;
    }

    private static long remainingMillis(final long deadlineMillis, final long nowMillis) {
        return Math.max(1L, deadlineMillis - nowMillis);
    }

    private static final class NewRange {
        final int startSerialNumber;
        final int count;

        NewRange(final int startSerialNumber, final int count) {
            this.startSerialNumber = startSerialNumber;
            this.count = count;
        }
    }

    private static final class ActiveRequest {
        @NonNull final Set<Integer> expectedSerials = new HashSet<>();
        @NonNull final Set<Integer> newSerials = new HashSet<>();
        @NonNull final Set<Integer> resolvedSerials = new HashSet<>();
        final int newStartSerialNumber;
        final int newCount;
        final long deadlineMillis;
        boolean hasProgress;
        long lastProgressAtMillis;
        boolean resyncAfterCompletion;

        ActiveRequest(
                @NonNull final List<OrpheValueRequest> requests,
                final int newStartSerialNumber,
                final int newCount,
                final long deadlineMillis
        ) {
            this.newStartSerialNumber = newStartSerialNumber;
            this.newCount = newCount;
            this.deadlineMillis = deadlineMillis;
            for (OrpheValueRequest request : requests) {
                for (int i = 0; i < request.length; i++) {
                    expectedSerials.add(normalizeSerialNumber(request.startSerialNumber + i));
                }
            }
            for (int i = 0; i < newCount; i++) {
                newSerials.add(normalizeSerialNumber(newStartSerialNumber + i));
            }
        }

        void markProgress(final long nowMillis) {
            hasProgress = true;
            lastProgressAtMillis = nowMillis;
        }

        boolean hasTimedOut(final long nowMillis) {
            return nowMillis >= deadlineMillis
                    || (hasProgress
                    && nowMillis - lastProgressAtMillis >= RESPONSE_IDLE_TIMEOUT_MILLIS);
        }
    }
}
