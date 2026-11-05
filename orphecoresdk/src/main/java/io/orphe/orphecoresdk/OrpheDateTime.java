package io.orphe.orphecoresdk;

import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import java.time.LocalDateTime;

/**
 * DATE_TIMEキャラクタリスティックの値を変換します。
 *
 * <p>ワイヤ形式は[YY(西暦-2000), MM, DD, hh, mm, ss, 10ms単位]の7バイトです（CORE / INSOLE共通）。
 * ミリ秒は10ms単位で送受信されるため、1桁目は切り捨てられます。</p>
 */
@RequiresApi(api = Build.VERSION_CODES.O)
public final class OrpheDateTime {
    /** DATE_TIMEのバイト長。 */
    public static final int LENGTH = 7;

    private OrpheDateTime() {
    }

    /**
     * 日時をDATE_TIMEの7バイトへ変換します。
     *
     * @param dateTime 変換する日時
     * @return DATE_TIMEへ書き込む7バイト
     */
    @NonNull
    public static byte[] toBytes(@NonNull final LocalDateTime dateTime) {
        return new byte[]{
                (byte) (dateTime.getYear() - 2000),
                (byte) dateTime.getMonthValue(),
                (byte) dateTime.getDayOfMonth(),
                (byte) dateTime.getHour(),
                (byte) dateTime.getMinute(),
                (byte) dateTime.getSecond(),
                (byte) (dateTime.getNano() / 10_000_000),
        };
    }

    /**
     * DATE_TIMEの7バイトを日時へ変換します。
     *
     * @param bytes DATE_TIMEから読み出した値
     * @return 変換した日時
     * @throws IllegalArgumentException バイト長が不足している場合
     */
    @NonNull
    public static LocalDateTime fromBytes(@NonNull final byte[] bytes) {
        if (bytes.length < LENGTH) {
            throw new IllegalArgumentException("DATE_TIME requires " + LENGTH + " bytes: " + bytes.length);
        }
        return LocalDateTime.of(
                (bytes[0] & 0xFF) + 2000,
                bytes[1] & 0xFF,
                bytes[2] & 0xFF,
                bytes[3] & 0xFF,
                bytes[4] & 0xFF,
                bytes[5] & 0xFF,
                (bytes[6] & 0xFF) * 10_000_000
        );
    }
}
