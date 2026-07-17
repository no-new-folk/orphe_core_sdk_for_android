package io.orphe.orphecoresdk;

import androidx.annotation.NonNull;

/**
 * fifo受信中にセンサー値が更新されたことを表します。
 *
 * <p>差分はNotifyを受信した時点で即時に取得できます。全体値は必要になった時だけ
 * SDK内の蓄積値からスナップショットを生成するため、グラフ表示で差分だけを使う場合に
 * 全体配列のコピーは発生しません。</p>
 */
public final class OrpheInsoleValueUpdate {
    @NonNull private final OrpheInsoleValue[] deltaValues;
    @NonNull private final OrpheInsoleValueAccumulator accumulator;
    private final long version;
    private OrpheInsoleValue[] allValues;

    OrpheInsoleValueUpdate(
            @NonNull final OrpheInsoleValue[] deltaValues,
            @NonNull final OrpheInsoleValueAccumulator accumulator,
            final long version
    ) {
        this.deltaValues = deltaValues.clone();
        this.accumulator = accumulator;
        this.version = version;
    }

    /**
     * 今回新たに受信した値を返します。
     *
     * @return 今回の差分値
     */
    @NonNull
    public OrpheInsoleValue[] getDeltaValues() {
        return deltaValues.clone();
    }

    /**
     * この更新時点までにfifoで取得できた全値をシリアル順で返します。
     * 欠損値が後から回収された場合は、そのシリアル位置へ挿入されます。
     *
     * @return この更新時点の全値
     */
    @NonNull
    public synchronized OrpheInsoleValue[] getAllValues() {
        if (allValues == null) {
            allValues = accumulator.snapshot(version);
        }
        return allValues.clone();
    }
}
