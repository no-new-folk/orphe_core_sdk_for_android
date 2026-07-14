package io.orphe.orphecoresdk;

/**
 * ORPHE INSOLEのセンサー値サンプリングレート。
 */
public enum OrpheInsoleSamplingRate {
    /**
     * 100Hz出力。realtimeではクオータニオン付き、request / bestEffortでは
     * 200Hz蓄積データを間引いたクオータニオンなしの値を返します。
     */
    hz100(4, 56, 2),

    /**
     * 200Hz出力。標準のINSOLEデータを受信します。
     */
    hz200(3, 55, 4);

    OrpheInsoleSamplingRate(int sensorRequestModeValue, int sensorValueHeader, int valuesPerSerialNumber) {
        this.sensorRequestModeValue = sensorRequestModeValue;
        this.sensorValueHeader = sensorValueHeader;
        this.valuesPerSerialNumber = valuesPerSerialNumber;
    }

    /**
     * DeviceInfoに書き込むセンサー取得モード値。
     */
    public final int sensorRequestModeValue;

    /**
     * Notifyされるセンサー値の先頭バイト。
     */
    public final int sensorValueHeader;

    /**
     * 1シリアル番号に含まれるセンサー値件数。
     */
    public final int valuesPerSerialNumber;

    OrpheSensorRequestMode toSensorRequestMode() {
        return OrpheSensorRequestMode.fromValue(sensorRequestModeValue);
    }

    /**
     * 指定されたセンサーパケットがこのサンプリングレートの形式かを返します。
     * 200Hzの54は蓄積値の応答、55はリアルタイム値です。
     */
    boolean matchesSensorValueHeader(final int header) {
        if (this == hz100) {
            return header == sensorValueHeader;
        }
        return header == 54 || header == sensorValueHeader;
    }
}
