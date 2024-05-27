package io.orphe.orphecoresdkforandroid;

import androidx.annotation.NonNull;

public enum OrpheGyroRange {
    /// ±250dps
    range250(250),

    /// ±500dps
    range500(500),

    /// ±1000dps
    range1000(1000),

    /// ±2000dps
    range2000(2000);

    OrpheGyroRange(
            @NonNull final int value
    ){
        this.value = value;
    }

    /// 左右
    final int value;

    /// 10進数を元に[OrpheLedBrightness]を返します。
    static OrpheGyroRange fromValue(int value) {
        OrpheGyroRange[] values = OrpheGyroRange.values();
        return values[value];
    }
}
