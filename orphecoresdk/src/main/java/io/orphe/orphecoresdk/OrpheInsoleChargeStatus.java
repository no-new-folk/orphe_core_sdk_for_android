package io.orphe.orphecoresdk;

/**
 * ORPHE INSOLEの充電ステータス
 */
public enum OrpheInsoleChargeStatus {
    /// ワイヤレス充電
    wireless,

    /// 有線充電
    wired,

    /// 充電なし
    none;

    /**
     * OrpheInsoleChargeStatus。
     *
     * @param value 数字
     * @return 対応するOrpheInsoleChargeStatus。
     */
    static OrpheInsoleChargeStatus fromValue(int value) {
        if (16 <= value && value < 32) {
            return OrpheInsoleChargeStatus.wireless;
        } else if (32 <= value && value < 48) {
            return OrpheInsoleChargeStatus.wired;
        }
        return OrpheInsoleChargeStatus.none;
    }
}
