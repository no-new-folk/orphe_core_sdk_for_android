package io.orphe.orphecoresdk;

import androidx.annotation.NonNull;

public enum OrpheAccRange {
    /// 2
    range2(2),

    /// 4
    range4(4),

    /// 8
    range8(8),

    /// 16
    range16(16);

    OrpheAccRange(
            @NonNull final int value
    ){
        this.value = value;
    }

    /// 左右
    final int value;

    /// 10進数を元に[OrpheLedBrightness]を返します。
    static OrpheAccRange fromValue(int value) {
        OrpheAccRange[] values = OrpheAccRange.values();
        return values[value];
    }
}
