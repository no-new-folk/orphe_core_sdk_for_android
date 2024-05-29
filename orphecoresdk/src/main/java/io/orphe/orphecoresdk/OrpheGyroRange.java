package io.orphe.orphecoresdk;

import androidx.annotation.NonNull;

/**
 * ORPHE COREのジャイロレンジ設定
 */
public enum OrpheGyroRange {
    /// ±250dps
    range250(250),

    /// ±500dps
    range500(500),

    /// ±1000dps
    range1000(1000),

    /// ±2000dps
    range2000(2000);

    /**
     * ORPHE COREのジャイロレンジ設定
     */
    OrpheGyroRange(
            @NonNull final int value
    ){
        this.value = value;
    }

    /**
     * 数字の値。
     */
    /// 左右
    final int value;

    /**
     * 数字からOrpheGyroRangeに変換します。
     *
     * @param value 数字
     * @return 対応するOrpheGyroRange。
     */
    static OrpheGyroRange fromValue(int value) {
        OrpheGyroRange[] values = OrpheGyroRange.values();
        return values[value];
    }
}
