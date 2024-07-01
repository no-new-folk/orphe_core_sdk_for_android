package io.orphe.orphecoresdk;

/**
 * ORPHE COREのバッテリーのステータス。
 */
public enum OrpheBatteryStatus {

    /// 不明
    unknown,

    /// 低
    low,

    /// 中
    normal,

    /// フル
    full;

    /**
     * 数字からOrpheBatteryStatusに変換します。
     *
     * @param value 数字
     * @return 対応するOrpheBatteryStatus。
     */
    static OrpheBatteryStatus fromValue(int value) {
        OrpheBatteryStatus[] values = OrpheBatteryStatus.values();
        return values[value + 1];
    }
}
