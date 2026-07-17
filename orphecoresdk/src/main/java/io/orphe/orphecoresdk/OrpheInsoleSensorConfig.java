package io.orphe.orphecoresdk;

import androidx.annotation.NonNull;

/**
 * ORPHE INSOLEのセンサー値取得初期設定。
 */
public class OrpheInsoleSensorConfig {
    /**
     * 既定設定。200HzのリアルタイムNotifyを使用します。
     */
    public static final OrpheInsoleSensorConfig DEFAULT = new OrpheInsoleSensorConfig(
            OrpheSensorReceiveMode.realtime,
            OrpheInsoleSamplingRate.hz200
    );

    public OrpheInsoleSensorConfig(
            @NonNull final OrpheSensorReceiveMode receiveMode,
            @NonNull final OrpheInsoleSamplingRate samplingRate
    ) {
        this(receiveMode, samplingRate, OrpheFifoConfig.DEFAULT, 500L);
    }

    /** @deprecated {@link OrpheSensorReceiveMode}を使用してください。 */
    @Deprecated
    public OrpheInsoleSensorConfig(
            @NonNull final OrpheInsoleReceiveMode receiveMode,
            @NonNull final OrpheInsoleSamplingRate samplingRate
    ) {
        this(receiveMode.toSensorReceiveMode(), samplingRate);
    }

    public OrpheInsoleSensorConfig(
            @NonNull final OrpheSensorReceiveMode receiveMode,
            @NonNull final OrpheInsoleSamplingRate samplingRate,
            @NonNull final OrpheFifoConfig fifoConfig,
            final long modeChangeDelayMillis
    ) {
        this.receiveMode = receiveMode;
        this.samplingRate = samplingRate;
        this.fifoConfig = fifoConfig;
        this.modeChangeDelayMillis = Math.max(0, modeChangeDelayMillis);
    }

    /** @deprecated {@link OrpheFifoConfig}を使用してください。 */
    @Deprecated
    public OrpheInsoleSensorConfig(
            @NonNull final OrpheInsoleReceiveMode receiveMode,
            @NonNull final OrpheInsoleSamplingRate samplingRate,
            final int requestLength,
            final long requestIntervalMillis,
            final long modeChangeDelayMillis
    ) {
        this(
                receiveMode.toSensorReceiveMode(),
                samplingRate,
                new OrpheFifoConfig(
                        requestLength,
                        requestIntervalMillis,
                        Math.max(1500L, requestIntervalMillis),
                        2
                ),
                modeChangeDelayMillis
        );
    }

    /**
     * センサー値の受信方式。
     */
    @NonNull
    public final OrpheSensorReceiveMode receiveMode;

    /**
     * サンプリングレート。
     */
    @NonNull
    public final OrpheInsoleSamplingRate samplingRate;

    /**
     * fifo方式の要求設定。
     */
    @NonNull
    public final OrpheFifoConfig fifoConfig;

    /**
     * Notify開始後にセンサー取得モードを書き込むまでの待ち時間。
     */
    public final long modeChangeDelayMillis;
}
