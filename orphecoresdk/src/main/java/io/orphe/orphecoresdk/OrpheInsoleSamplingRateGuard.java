package io.orphe.orphecoresdk;

import androidx.annotation.NonNull;

/** 選択中のサンプリングレートと受信パケット形式の整合を管理します。 */
final class OrpheInsoleSamplingRateGuard {
    private final int maxRetryCount;
    private int retryCount;
    private boolean retryScheduled;
    private boolean confirmed;

    OrpheInsoleSamplingRateGuard(final int maxRetryCount) {
        this.maxRetryCount = Math.max(0, maxRetryCount);
    }

    boolean accepts(
            @NonNull final OrpheInsoleSamplingRate samplingRate,
            final int packetHeader
    ) {
        if (!samplingRate.matchesSensorValueHeader(packetHeader)) {
            return false;
        }
        confirmed = true;
        retryCount = 0;
        retryScheduled = false;
        return true;
    }

    boolean scheduleRetry() {
        if (confirmed || retryScheduled || retryCount >= maxRetryCount) {
            return false;
        }
        retryScheduled = true;
        retryCount++;
        return true;
    }

    void retryStarted() {
        retryScheduled = false;
    }

    void reset() {
        retryCount = 0;
        retryScheduled = false;
        confirmed = false;
    }

    int retryCount() {
        return retryCount;
    }
}
