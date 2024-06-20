package io.orphe.orphecoresdk;

import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;

/**
 * ORPHEのセンサー値を格納するためのクラス。
 */
public class OrpheInsoleValue {
    /**
     * ORPHEのセンサー値を格納するためのクラス。
     */
  public OrpheInsoleValue(@NonNull
                     final OrpheSidePosition sidePosition,

                          /// シリアルナンバー
                          @NonNull final int serialNumber,

                          /// 同じデータ中の位置
                          @NonNull final int dataPosition,

                          /// タイムスタンプ
                          @NonNull final long startTime,

                          final long endTime,

                          /// 圧力の値
                          @NonNull final double pressure1,
                          @NonNull final double pressure2,
                          @NonNull final double pressure3,
                          @NonNull final double pressure4
                     ){
      this.sidePosition = sidePosition;
      this.serialNumber = serialNumber;
      this.dataPosition = dataPosition;
      this.startTime = startTime;
      this.endTime = endTime;
      this.pressure1 = pressure1;
      this.pressure2 = pressure2;
      this.pressure3 = pressure3;
      this.pressure4 = pressure4;
    }

    /**
     * 文字列に変換します。
     *
     * @return 文字列
     */
    public String toString(){
      final StringBuilder builder = new StringBuilder();
        builder.append("pressure:(");
        builder.append(String.format("%.2f", pressure1));
        builder.append(",");
        builder.append(String.format("%.2f", pressure2));
        builder.append(",");
        builder.append(String.format("%.2f", pressure3));
        builder.append(",");
        builder.append(String.format("%.2f", pressure4));
        builder.append(")\n");
        return builder.toString();
    }


    /**
     * バイト配列から[OrpheInsoleValue]を取得します。
     *
     * @param bytes ORPHE Insoleから送られたバイト配列
     * @param sidePosition 取り付け位置
     * @return OrpheInsoleValueの配列
     */
    @RequiresApi(api = Build.VERSION_CODES.O)
    public static OrpheInsoleValue fromBytes(
            byte[] bytes, OrpheSidePosition sidePosition) throws Exception {

        final int serialNumber = parseInt(bytes, 1);
        final LocalDateTime now = LocalDateTime.now();
        final LocalDateTime baseTimestamp = LocalDateTime.of(
                now.getYear(),
                now.getMonth(),
                now.getDayOfMonth(),
                getUint8(bytes, 3),
                getUint8(bytes, 4),
                getUint8(bytes, 5),
                getUint16(bytes, 6) * 1000
        );
        final double pressure1 = parseInt(bytes, 8);
        final double pressure2 = parseInt(bytes, 10);
        final double pressure3 = parseInt(bytes, 12);
        final double pressure4 = parseInt(bytes, 14);
        final LocalDateTime timestamp = baseTimestamp;
        return new OrpheInsoleValue(
                sidePosition,
                serialNumber,
                0,
                timestamp.toInstant(ZoneOffset.UTC).toEpochMilli(),
                timestamp.toInstant(ZoneOffset.UTC).toEpochMilli(),
                pressure1,
                pressure2,
                pressure3,
                pressure4
        );
    }

    /**
     *  取り付け位置。
     */
    @NonNull
    final OrpheSidePosition sidePosition;

    /**
     * シリアルナンバー
     */
    @NonNull final int serialNumber;

    /**
     * 同じデータ中の位置
     */
    @NonNull final int dataPosition;

    /**
     * 開始日時のタイムスタンプ（ナノ秒）
     */
    @NonNull final long startTime;

    /**
     * 終了日時のタイムスタンプ（ナノ秒）
     */
    final long endTime;

    /**
     * クオータニオンX
     */
    @NonNull final double pressure1;
    /**
     * クオータニオンY
     */
    @NonNull final double pressure2;
    /**
     * クオータニオンZ
     */
    @NonNull final double pressure3;
    /**
     * クオータニオンW
     */
    @NonNull final double pressure4;

    private static int parseInt(@NonNull byte[] bytes,  int index) {
        return (int)((getInt8(bytes, index) << 8) + getInt8(bytes, index + 1));
    }

    private static short getUint16(@NonNull byte[] data, int index) {
        return (short) (((data[index] & 0xFF) << 8) | (data[index + 1] & 0xFF));
    }
    private static byte getUint8(@NonNull byte[] data, int index) {
        return (byte) (data[index] & 0xFF);
    }
    private static byte getInt8(@NonNull byte[] data, int index) {
        return (byte) data[index];
    }
}
