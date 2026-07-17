package io.orphe.orphecoresdk;

import androidx.annotation.NonNull;

/**
 * ORPHE COREのセンサー値受信設定。
 */
public class OrpheCoreSensorConfig {
    public static final OrpheCoreSensorConfig DEFAULT = new OrpheCoreSensorConfig(
            OrpheSensorReceiveMode.realtime,
            OrpheFifoConfig.DEFAULT,
            500L
    );

    public OrpheCoreSensorConfig(@NonNull final OrpheSensorReceiveMode receiveMode) {
        this(receiveMode, OrpheFifoConfig.DEFAULT, 500L);
    }

    public OrpheCoreSensorConfig(
            @NonNull final OrpheSensorReceiveMode receiveMode,
            @NonNull final OrpheFifoConfig fifoConfig,
            final long modeChangeDelayMillis
    ) {
        this.receiveMode = receiveMode;
        this.fifoConfig = fifoConfig;
        this.modeChangeDelayMillis = Math.max(0L, modeChangeDelayMillis);
    }

    @NonNull public final OrpheSensorReceiveMode receiveMode;
    @NonNull public final OrpheFifoConfig fifoConfig;
    public final long modeChangeDelayMillis;
}
