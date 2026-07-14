package io.orphe.orphecoresdk;

import androidx.annotation.NonNull;

/**
 * ORPHE COREのセンサー値受信設定。
 */
public class OrpheCoreSensorConfig {
    public static final OrpheCoreSensorConfig DEFAULT = new OrpheCoreSensorConfig(
            OrpheSensorReceiveMode.realtime,
            OrpheBestEffortConfig.DEFAULT,
            500L
    );

    public OrpheCoreSensorConfig(@NonNull final OrpheSensorReceiveMode receiveMode) {
        this(receiveMode, OrpheBestEffortConfig.DEFAULT, 500L);
    }

    public OrpheCoreSensorConfig(
            @NonNull final OrpheSensorReceiveMode receiveMode,
            @NonNull final OrpheBestEffortConfig bestEffortConfig,
            final long modeChangeDelayMillis
    ) {
        this.receiveMode = receiveMode;
        this.bestEffortConfig = bestEffortConfig;
        this.modeChangeDelayMillis = Math.max(0L, modeChangeDelayMillis);
    }

    @NonNull public final OrpheSensorReceiveMode receiveMode;
    @NonNull public final OrpheBestEffortConfig bestEffortConfig;
    public final long modeChangeDelayMillis;
}
