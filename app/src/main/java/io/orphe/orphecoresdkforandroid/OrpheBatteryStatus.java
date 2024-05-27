package io.orphe.orphecoresdkforandroid;

public enum OrpheBatteryStatus {

    /// 不明
    unknown,

    /// 低
    low,

    /// 中
    normal,

    /// フル
    full;

    /// 10進数を元に[OrpheBatteryStatus]を返します。
    static OrpheBatteryStatus fromValue(int value) {
        OrpheBatteryStatus[] values = OrpheBatteryStatus.values();
        return values[value];
    }
}
