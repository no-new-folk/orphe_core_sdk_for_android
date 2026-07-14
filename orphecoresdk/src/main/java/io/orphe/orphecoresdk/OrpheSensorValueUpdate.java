package io.orphe.orphecoresdk;

import androidx.annotation.NonNull;

/** COREのbestEffort受信における今回差分と再計算済み全値。 */
public final class OrpheSensorValueUpdate {
    @NonNull private final OrpheSensorValue[] deltaValues;
    @NonNull private final OrpheSensorValueAccumulator accumulator;
    private final long version;
    private OrpheSensorValue[] allValues;

    OrpheSensorValueUpdate(
            @NonNull final OrpheSensorValue[] deltaValues,
            @NonNull final OrpheSensorValueAccumulator accumulator,
            final long version
    ) {
        this.deltaValues = deltaValues.clone();
        this.accumulator = accumulator;
        this.version = version;
    }

    /** 今回新たに受信した差分値を返します。 */
    @NonNull
    public OrpheSensorValue[] getDeltaValues() {
        return deltaValues.clone();
    }

    /** この更新時点までの、シリアル順かつ姿勢再計算済みの全値を返します。 */
    @NonNull
    public synchronized OrpheSensorValue[] getAllValues() {
        if (allValues == null) {
            allValues = accumulator.snapshot(version);
        }
        return allValues.clone();
    }
}
