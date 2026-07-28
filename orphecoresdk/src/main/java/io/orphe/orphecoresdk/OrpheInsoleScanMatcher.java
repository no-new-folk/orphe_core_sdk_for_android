package io.orphe.orphecoresdk;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * アドバタイズ情報からORPHE INSOLE / ORPHE COREを判定します。
 *
 * <p>アドバタイズ名のプレフィックスを最優先で判定し、一致しない場合にmanufacturer dataの
 * シグネチャで判定します。manufacturer dataが取得できない機体もアドバタイズ名だけで
 * 判定できるようにするため、この順序を崩してはいけません。</p>
 */
final class OrpheInsoleScanMatcher {
    /** manufacturer dataのシグネチャを判定するために必要な最小長。 */
    private static final int SIGNATURE_MIN_LENGTH = 7;
    /** 充電ステータスを読み取るために必要な最小長。 */
    private static final int CHARGE_STATUS_MIN_LENGTH = 15;
    private static final int SIGNATURE_INDEX = 0;
    private static final int INSOLE_MARK_INDEX = 5;
    private static final int SIDE_INDEX = 6;
    private static final int CHARGE_STATUS_INDEX = 14;

    private OrpheInsoleScanMatcher() {
    }

    /** 判定の根拠。 */
    enum Source {
        /** アドバタイズ名のプレフィックスで判定しました。 */
        deviceName,

        /** manufacturer dataのシグネチャで判定しました。 */
        manufacturerData
    }

    /** 判定結果。 */
    static final class Match {
        Match(
                @NonNull final Source source,
                @Nullable final String deviceId,
                @Nullable final OrpheSide side,
                @Nullable final OrpheInsoleChargeStatus chargeStatus,
                final boolean isCore
        ) {
            this.source = source;
            this.deviceId = deviceId;
            this.side = side;
            this.chargeStatus = chargeStatus;
            this.isCore = isCore;
        }

        /** 判定の根拠。 */
        @NonNull
        final Source source;

        /** デバイスID。決定できない場合はnull。 */
        @Nullable
        final String deviceId;

        /** 左右。アドバタイズから決定できない場合はnull。 */
        @Nullable
        final OrpheSide side;

        /** 充電ステータス。取得できない場合はnull。 */
        @Nullable
        final OrpheInsoleChargeStatus chargeStatus;

        /** ORPHE COREとして判定された場合はtrue。 */
        final boolean isCore;
    }

    /**
     * アドバタイズ情報がORPHEのデバイスかどうかを判定します。
     *
     * @param manufacturerData manufacturer data。取得できていない場合はnull。
     * @param advertisedName   アドバタイズ名。取得できていない場合はnull。
     * @param address          BLEのMACアドレス。取得できていない場合はnull。
     * @return 判定結果。ORPHEのデバイスではない場合はnull。
     */
    @Nullable
    static Match match(
            @Nullable final byte[] manufacturerData,
            @Nullable final String advertisedName,
            @Nullable final String address
    ) {
        return match(manufacturerData, advertisedName, address, true);
    }

    /**
     * アドバタイズ情報がORPHEのデバイスかどうかを判定します。
     *
     * @param manufacturerData        manufacturer data。取得できていない場合はnull。
     * @param advertisedName          アドバタイズ名。取得できていない場合はnull。
     * @param address                 BLEのMACアドレス。取得できていない場合はnull。
     * @param insoleNameMatchEnabled  ORPHE INSOLEをアドバタイズ名のプレフィックスで判定するかどうか。
     * @return 判定結果。ORPHEのデバイスではない場合はnull。
     */
    @Nullable
    static Match match(
            @Nullable final byte[] manufacturerData,
            @Nullable final String advertisedName,
            @Nullable final String address,
            final boolean insoleNameMatchEnabled
    ) {
        final boolean hasSignature = hasInsoleSignature(manufacturerData);
        if (insoleNameMatchEnabled
                && advertisedName != null
                && advertisedName.startsWith(DeviceNameDefine.ORPHE_INSOLE)) {
            // アドバタイズ名で判定できる場合はmanufacturer dataを必須としません。
            return new Match(
                    Source.deviceName,
                    hasSignature
                            ? formatInsoleId(manufacturerData)
                            : resolveNameDeviceId(advertisedName, address),
                    hasSignature
                            ? parseSide(manufacturerData)
                            : parseSideFromName(advertisedName),
                    parseChargeStatus(manufacturerData),
                    false
            );
        }
        if (hasSignature) {
            return new Match(
                    Source.manufacturerData,
                    formatInsoleId(manufacturerData),
                    parseSide(manufacturerData),
                    parseChargeStatus(manufacturerData),
                    false
            );
        }
        if (advertisedName != null && advertisedName.contains(DeviceNameDefine.ORPHE_CORE)) {
            return new Match(Source.deviceName, advertisedName, null, null, true);
        }
        return null;
    }

    /** manufacturer dataがORPHE INSOLEのシグネチャを持つかどうかを返します。 */
    private static boolean hasInsoleSignature(@Nullable final byte[] data) {
        return data != null
                && data.length >= SIGNATURE_MIN_LENGTH
                && data[SIGNATURE_INDEX] == 1
                && data[INSOLE_MARK_INDEX] == 1;
    }

    /** manufacturer dataから左右を返します。判別できない場合はnullを返します。 */
    @Nullable
    private static OrpheSide parseSide(@Nullable final byte[] data) {
        if (!hasInsoleSignature(data)) {
            return null;
        }
        switch (data[SIDE_INDEX]) {
            case 0:
                return OrpheSide.left;
            case 1:
                return OrpheSide.right;
            default:
                return null;
        }
    }

    /** アドバタイズ名の末尾から左右を返します。判別できない場合はnullを返します。 */
    @Nullable
    private static OrpheSide parseSideFromName(@NonNull final String advertisedName) {
        if (advertisedName.isEmpty()) {
            return null;
        }
        switch (advertisedName.charAt(advertisedName.length() - 1)) {
            case 'L':
                return OrpheSide.left;
            case 'R':
                return OrpheSide.right;
            default:
                return null;
        }
    }

    /** manufacturer dataから充電ステータスを返します。取得できない場合はnullを返します。 */
    @Nullable
    private static OrpheInsoleChargeStatus parseChargeStatus(@Nullable final byte[] data) {
        if (data == null || data.length < CHARGE_STATUS_MIN_LENGTH) {
            return null;
        }
        return OrpheInsoleChargeStatus.fromValue(data[CHARGE_STATUS_INDEX] & 0xFF);
    }

    /** manufacturer dataが無い場合のデバイスIDを返します。 */
    @Nullable
    private static String resolveNameDeviceId(
            @NonNull final String advertisedName,
            @Nullable final String address
    ) {
        if (!advertisedName.isEmpty()) {
            return advertisedName;
        }
        if (address == null || address.isEmpty()) {
            return null;
        }
        return "IN-" + address.replace(":", "");
    }

    private static String formatInsoleId(@NonNull final byte[] data) {
        final long number = getUint32(data, 1);
        final char side = (data[SIDE_INDEX] == 0) ? 'L' : 'R';
        return String.format("IN%08X%c", number, side);
    }

    private static long getUint32(@NonNull final byte[] data, final int index) {
        return (long) (((data[index] & 0xFF) << 24) | ((data[index + 1] & 0xFF) << 16) | ((data[index + 2] & 0xFF) << 8) | (data[index + 3] & 0xFF));
    }
}
