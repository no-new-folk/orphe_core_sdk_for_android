package io.orphe.orphecoresdk;

public enum OrpheLedBrightness {
    off,
    weak,
    strong;

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

    /// 10進数を元に[OrpheLedBrightness]を返します。
    static OrpheLedBrightness fromValue(int value) {
        OrpheLedBrightness[] values = OrpheLedBrightness.values();
        return values[value];
    }
}
