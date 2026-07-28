package io.orphe.orphecoresdk;

import androidx.annotation.NonNull;

/**
 * ORPHE COREのジャイロレンジ設定
 */
public enum OrpheGyroRange {
    /// ±250dps
    range250(250, 0.00875),

    /// ±500dps
    range500(500, 0.0175),

    /// ±1000dps
    range1000(1000, 0.035),

    /// ±2000dps
    range2000(2000, 0.07);

    /**
     * ORPHE COREのジャイロレンジ設定
     */
    OrpheGyroRange(
            @NonNull final int value,
            @NonNull final double sensitivity
    ){
        this.value = value;
        this.sensitivity = sensitivity;
    }

    /**
     * 数字の値。
     */
    /// 左右
    final int value;

    /**
     * 生値1LSBあたりの角速度[dps]。
     *
     * LSM6DSOXのデータシート感度（±2000dpsで70 mdps/LSB）に基づく値です。
     * フルスケールを{@code range / 32768}で割る換算は約12.8%小さい値になるため使用しません。
     */
    final double sensitivity;

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
