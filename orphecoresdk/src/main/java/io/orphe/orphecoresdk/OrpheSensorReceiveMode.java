package io.orphe.orphecoresdk;

/**
 * センサー値の受信方式。
 */
public enum OrpheSensorReceiveMode {
    /** デバイスから送信されるリアルタイム値を受信します。 */
    realtime,

    /** アプリケーションが明示的にリクエストした範囲だけを受信します。 */
    request,

    /** SDKが継続取得と欠損回収を自動的に行います。 */
    fifo
}
