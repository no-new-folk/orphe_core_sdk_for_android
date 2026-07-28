package io.orphe.orphecoresdk;

/**
 * ORPHE INSOLEのスキャン判定の設定。
 */
public class OrpheInsoleScanConfig {
    /**
     * 既定設定。アドバタイズ名による判定と左右不明候補の通知を有効にします。
     */
    public static final OrpheInsoleScanConfig DEFAULT = new OrpheInsoleScanConfig(true, true);

    /**
     * ORPHE INSOLEのスキャン判定の設定。
     *
     * @param nameMatchEnabled           アドバタイズ名のプレフィックスによる判定を有効にするかどうか。
     * @param allowUnknownSideCandidate  アドバタイズから左右が判別できない候補を通知するかどうか。
     */
    public OrpheInsoleScanConfig(
            final boolean nameMatchEnabled,
            final boolean allowUnknownSideCandidate
    ) {
        this.nameMatchEnabled = nameMatchEnabled;
        this.allowUnknownSideCandidate = allowUnknownSideCandidate;
    }

    /**
     * アドバタイズ名のプレフィックス（{@link DeviceNameDefine#ORPHE_INSOLE}）による判定を有効にするかどうか。
     */
    public final boolean nameMatchEnabled;

    /**
     * アドバタイズから左右が判別できない候補を通知するかどうか。
     *
     * <p>{@code true}の場合、左右不明の機体は左右両方の{@link OrpheInsole}に通知されるため、
     * どちらへ接続するかは呼び出し側で選択する必要があります。</p>
     */
    public final boolean allowUnknownSideCandidate;
}
