package io.orphe.orphecoresdk;

/**
 * fifo受信方式のリクエスト設定。
 */
public class OrpheFifoConfig {
    /** {@link #drainTimeoutMillis}の既定値。 */
    public static final long DEFAULT_DRAIN_TIMEOUT_MILLIS = 3000L;

    public static final OrpheFifoConfig DEFAULT =
            new OrpheFifoConfig(200, 200L, 5000L, 100, 1500);

    /**
     * @deprecated FIFOは固定回数で打ち切らず、未受信値を次回へ持ち越します。
     * {@link #OrpheFifoConfig(int, long, long, int, int)}を使用してください。
     */
    @Deprecated
    public OrpheFifoConfig(
            final int requestLength,
            final long requestIntervalMillis,
            final long requestTimeoutMillis,
            final int maxRetryCount
    ) {
        this(
                requestLength,
                requestIntervalMillis,
                requestTimeoutMillis,
                legacyCarryOverLimit(maxRetryCount),
                1500,
                DEFAULT_DRAIN_TIMEOUT_MILLIS,
                maxRetryCount
        );
    }

    public OrpheFifoConfig(
            final int requestLength,
            final long requestIntervalMillis,
            final long requestTimeoutMillis,
            final int maxCarryOverSerials,
            final int ringBufferCapacity
    ) {
        this(
                requestLength,
                requestIntervalMillis,
                requestTimeoutMillis,
                maxCarryOverSerials,
                ringBufferCapacity,
                DEFAULT_DRAIN_TIMEOUT_MILLIS,
                0
        );
    }

    /**
     * @param drainTimeoutMillis FIFO停止時の回収フェーズ（drain/catch-up）の上限時間[ms]。
     *                           0で無効（従来どおり即時停止。取り残しは missing として通知される）。
     */
    public OrpheFifoConfig(
            final int requestLength,
            final long requestIntervalMillis,
            final long requestTimeoutMillis,
            final int maxCarryOverSerials,
            final int ringBufferCapacity,
            final long drainTimeoutMillis
    ) {
        this(
                requestLength,
                requestIntervalMillis,
                requestTimeoutMillis,
                maxCarryOverSerials,
                ringBufferCapacity,
                drainTimeoutMillis,
                0
        );
    }

    private OrpheFifoConfig(
            final int requestLength,
            final long requestIntervalMillis,
            final long requestTimeoutMillis,
            final int maxCarryOverSerials,
            final int ringBufferCapacity,
            final long drainTimeoutMillis,
            final int legacyMaxRetryCount
    ) {
        if (requestLength < 1 || requestLength > 200) {
            throw new IllegalArgumentException("requestLength must be between 1 and 200.");
        }
        if (requestIntervalMillis < 50L) {
            throw new IllegalArgumentException("requestIntervalMillis must be at least 50ms.");
        }
        if (requestTimeoutMillis < requestIntervalMillis) {
            throw new IllegalArgumentException(
                    "requestTimeoutMillis must be greater than or equal to requestIntervalMillis.");
        }
        if (maxCarryOverSerials < 1 || maxCarryOverSerials >= 65536) {
            throw new IllegalArgumentException(
                    "maxCarryOverSerials must be between 1 and 65535.");
        }
        if (ringBufferCapacity < 1 || ringBufferCapacity >= 65536) {
            throw new IllegalArgumentException(
                    "ringBufferCapacity must be between 1 and 65535.");
        }
        if (drainTimeoutMillis < 0L) {
            throw new IllegalArgumentException("drainTimeoutMillis must not be negative.");
        }
        this.requestLength = requestLength;
        this.requestIntervalMillis = requestIntervalMillis;
        this.requestTimeoutMillis = requestTimeoutMillis;
        this.maxCarryOverSerials = maxCarryOverSerials;
        this.ringBufferCapacity = ringBufferCapacity;
        this.drainTimeoutMillis = drainTimeoutMillis;
        this.maxRetryCount = legacyMaxRetryCount;
    }

    /** 1回のBLE要求で取得する最大シリアル数。 */
    public final int requestLength;

    /** 新しいデータを確認する間隔。 */
    public final long requestIntervalMillis;

    /** 要求への応答を待つ時間。 */
    public final long requestTimeoutMillis;

    /**
     * @deprecated 固定リトライ回数は使用されません。未受信値はcarry-overされます。
     */
    @Deprecated
    public final int maxRetryCount;

    /** 次回以降へ持ち越す未受信シリアル数の上限。 */
    public final int maxCarryOverSerials;

    /** FWリングバッファから安全に取得できる最大シリアル数。 */
    public final int ringBufferCapacity;

    /**
     * FIFO停止時の回収フェーズ（drain/catch-up）の上限時間[ms]。
     *
     * <p>停止時点でFWが既に生成済みのシリアルを固定ターゲットとして、未要求分（catch-up）と
     * 要求済み未受信分（carry-over）の回収を続ける。0で無効（従来どおり即時停止）。
     * 取りこぼしが無い正常系では1往復で完了するため、実質的な遅延はほぼ無い。
     */
    public final long drainTimeoutMillis;

    private static int legacyCarryOverLimit(final int maxRetryCount) {
        if (maxRetryCount < 0) {
            throw new IllegalArgumentException("maxRetryCount must not be negative.");
        }
        return 100;
    }
}
