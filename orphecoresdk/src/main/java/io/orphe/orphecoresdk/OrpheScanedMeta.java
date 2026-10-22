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
        this(deviceId, chargeStatus, null);
    }

    /**
     * ORPHEのスキャンデータを保存するためのデータ
     *
     * @param deviceId デバイスID
     * @param chargeStatus インソールの充電ステータス
     * @param side アドバタイズから判別できた左右。判別できなかった場合はnull
     */
    public OrpheScanedMeta(
            String deviceId,
            OrpheInsoleChargeStatus chargeStatus,
            OrpheSide side
    ) {
        this(deviceId, chargeStatus, side, false);
    }

    /**
     * ORPHEのスキャンデータを保存するためのデータ
     *
     * @param deviceId デバイスID
     * @param chargeStatus インソールの充電ステータス
     * @param side アドバタイズから判別できた左右。判別できなかった場合はnull
     * @param isCore ORPHE COREとして判別された場合はtrue
     */
    public OrpheScanedMeta(
            String deviceId,
            OrpheInsoleChargeStatus chargeStatus,
            OrpheSide side,
            boolean isCore
    ) {
        this.deviceId = deviceId;
        this.chargeStatus = chargeStatus;
        this.side = side;
        this.isCore = isCore;
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

    /**
     * アドバタイズから判別できた左右。判別できなかった場合はnull。
     */
    public final OrpheSide side;

    /**
     * ORPHE COREとして判別されたかどうか。
     * trueの場合は[OrpheInsole]ではなく[Orphe]で接続してください。
     */
    public final boolean isCore;

    /**
     * アドバタイズから左右が判別できなかったかどうか。
     *
     * @return 左右が判別できなかった場合はtrue
     */
    public boolean sideIsUnknown() {
        return side == null;
    }
}
