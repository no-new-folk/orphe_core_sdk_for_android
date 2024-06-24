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
                          @NonNull final double pressureOutside,
                          @NonNull final double pressureTop,
                          @NonNull final double pressureInside,
                          @NonNull final double pressureBottom
                     ){
      this.sidePosition = sidePosition;
      this.serialNumber = serialNumber;
      this.dataPosition = dataPosition;
      this.startTime = startTime;
      this.endTime = endTime;
      this.pressureOutside = pressureOutside;
      this.pressureTop = pressureTop;
      this.pressureInside = pressureInside;
      this.pressureBottom = pressureBottom;
    }

    /**
     * 文字列に変換します。
     *
     * @return 文字列
     */
    public String toString(){
      final StringBuilder builder = new StringBuilder();
        builder.append("pressure:(");
        builder.append(String.format("%.2f", pressureOutside));
        builder.append(",");
        builder.append(String.format("%.2f", pressureInside));
        builder.append(",");
        builder.append(String.format("%.2f", pressureTop));
        builder.append(",");
        builder.append(String.format("%.2f", pressureBottom));
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
        final double pressureOutside = parseInt(bytes, 8);
        final double pressureTop = parseInt(bytes, 10);
        final double pressureInside = parseInt(bytes, 12);
        final double pressureBottom = parseInt(bytes, 14);
        final LocalDateTime timestamp = baseTimestamp;
        return new OrpheInsoleValue(
                sidePosition,
                serialNumber,
                0,
                timestamp.toInstant(ZoneOffset.UTC).toEpochMilli(),
                timestamp.toInstant(ZoneOffset.UTC).toEpochMilli(),
                pressureOutside,
                pressureTop,
                pressureInside,
                pressureBottom
        );
    }

    /**
     *  取り付け位置。
     */
    @NonNull
    public final OrpheSidePosition sidePosition;

    /**
     * シリアルナンバー
     */
    @NonNull public final int serialNumber;

    /**
     * 同じデータ中の位置
     */
    @NonNull public final int dataPosition;

    /**
     * 開始日時のタイムスタンプ（ナノ秒）
     */
    @NonNull public final long startTime;

    /**
     * 終了日時のタイムスタンプ（ナノ秒）
     */
    public final long endTime;

    /**
     * クオータニオンX
     */
    @NonNull public final double pressureOutside;
    /**
     * クオータニオンY
     */
    @NonNull public final double pressureTop;
    /**
     * クオータニオンZ
     */
    @NonNull public final double pressureInside;
    /**
     * クオータニオンW
     */
    @NonNull public final double pressureBottom;

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
