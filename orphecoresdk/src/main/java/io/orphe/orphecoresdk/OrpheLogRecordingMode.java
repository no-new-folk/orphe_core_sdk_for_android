package io.orphe.orphecoresdk;

/**
 * ORPHE COREの計測モードの設定
 */
public enum OrpheLogRecordingMode {
    /// 停止
    stop,

    /// 記録中
    recording,

    /// 一時停止
    pause,

    /// 再開
    resume;

    /**
     * 数字からOrpheLogRecordingModeに変換します。
     *
     * @param value 数字
     * @return 対応するOrpheLogRecordingMode。
     */
    static OrpheLogRecordingMode fromValue(int value) {
        OrpheLogRecordingMode[] values = OrpheLogRecordingMode.values();
        return values[value];
    }
}
