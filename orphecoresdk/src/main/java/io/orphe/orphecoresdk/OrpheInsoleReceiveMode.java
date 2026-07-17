package io.orphe.orphecoresdk;

/**
 * ORPHE INSOLEのセンサー値受信方式。
 */
@Deprecated
public enum OrpheInsoleReceiveMode {
    /**
     * デバイスが送信したセンサー値をそのままNotifyで受け取ります。
     */
    realtime,

    /**
     * アプリケーションが明示的にリクエストした範囲だけを受け取ります。
     */
    request,

    /** SDKが継続取得と欠損回収を自動的に行います。 */
    fifo;

    OrpheSensorReceiveMode toSensorReceiveMode() {
        switch (this) {
            case request:
                return OrpheSensorReceiveMode.request;
            case fifo:
                return OrpheSensorReceiveMode.fifo;
            case realtime:
            default:
                return OrpheSensorReceiveMode.realtime;
        }
    }
}
