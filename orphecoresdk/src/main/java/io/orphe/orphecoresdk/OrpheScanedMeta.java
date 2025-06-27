package io.orphe.orphecoresdk;

/**
 * ORPHEのスキャンデータを保存するためのデータ
 */
public class OrpheScanedMeta {
    /**
     * ORPHEのスキャンデータを保存するためのデータ
     *
     * @param deviceId デバイスID
     * @param chargeStatus インソールの充電ステータス
     */
    public OrpheScanedMeta(
            String deviceId,
            OrpheInsoleChargeStatus chargeStatus
    ) {
        this.deviceId = deviceId;
        this.chargeStatus = chargeStatus;
    }

    /**
     * ORPHEのスキャンデータを保存するためのデータ
     *
     * @param deviceId デバイスID
     */
    public OrpheScanedMeta(
            String deviceId
            ) {
        this(deviceId, null);
    }

    /**
     * ORPHEのスキャンデータを保存するためのデータ
     */
    public OrpheScanedMeta(
    ) {
        this(null, null);
    }


    /**
     * デバイスID
     */
    public final String deviceId;

    /**
     * インソールの充電ステータス
     */
    public final OrpheInsoleChargeStatus chargeStatus;
}
