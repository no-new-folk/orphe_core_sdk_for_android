package io.orphe.orphecoresdkforandroid;

public enum OrpheLogRecordingMode {
    /// 停止
    stop,

    /// 記録中
    recording,

    /// 一時停止
    pause,

    /// 再開
    resume;

    /// 10進数を元に[OrpheLogRecordingMode]を返します。
    static OrpheLogRecordingMode fromValue(int value) {
        OrpheLogRecordingMode[] values = OrpheLogRecordingMode.values();
        return values[value];
    }
}
