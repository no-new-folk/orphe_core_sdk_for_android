package io.orphe.orphecoresdk;

import androidx.annotation.NonNull;

/**
 * ORPHE COREの加速度レンジ設定
 */
public enum OrpheAccRange {
    /// 2
    range2(2),

    /// 4
    range4(4),

    /// 8
    range8(8),

    /// 16
    range16(16);

    /**
     * ORPHE COREの加速度レンジ設定
     */
    OrpheAccRange(
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
     * 数字からOrpheAccRangeに変換します。
     *
     * @param value 数字
     * @return 対応するOrpheAccRange。
     */
    static OrpheAccRange fromValue(int value) {
        OrpheAccRange[] values = OrpheAccRange.values();
        return values[value];
    }
}
