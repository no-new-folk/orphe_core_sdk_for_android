package io.orphe.orphecoresdk;

/**
 * ORPHE COREのLEDの明るさ設定
 */
public enum OrpheLedBrightness {
    off,
    weak,
    strong;

    /**
     * 明るさの値
     *
     * @return 明るさの値を数字にして返します
     */
    /// [OrpheLedBrightness]の16進数を返します。
    int value() {
        switch (this) {
            case off:
                return 0x00;
            case weak:
                return 0x14;
            case strong:
            default:
                return 0x80;
        }
    }

    /**
     * 数字からOrpheLedBrightnessに変換します。
     *
     * @param value 数字
     * @return 対応するOrpheLedBrightness。
     */
    /// 10進数を元に[OrpheLedBrightness]を返します。
    static OrpheLedBrightness fromValue(int value) {
        OrpheLedBrightness[] values = OrpheLedBrightness.values();
        return values[value];
    }
}
