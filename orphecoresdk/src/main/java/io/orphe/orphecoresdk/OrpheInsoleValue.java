package io.orphe.orphecoresdk;

import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;

/**
 * ORPHE INSOLEのセンサー値を格納するためのクラス。
 */
public class OrpheInsoleValue {
    /**
     * ORPHE INSOLEのセンサー値を格納するためのクラス。
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
                          @NonNull final double pressure4,
                          @NonNull final double pressure5,
                          @NonNull final double pressure6,

                          /// 加速度
                          @NonNull final double accX,
                          @NonNull final double accY,
                          @NonNull final double accZ,

                          /// ジャイロによる角度の範囲
                          @NonNull final double gyroX,
                          @NonNull final double gyroY,
                          @NonNull final double gyroZ

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
      this.pressure5 = pressure5;
      this.pressure6 = pressure6;
      this.accX = accX;
      this.accY = accY;
      this.accZ = accZ;
      this.gyroX = gyroX;
      this.gyroY = gyroY;
      this.gyroZ = gyroZ;
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
        builder.append(",");
        builder.append(String.format("%.2f", pressure5));
        builder.append(",");
        builder.append(String.format("%.2f", pressure6));
        builder.append(")\n");
        builder.append("acc:(");
        builder.append(String.format("%.2f", accX));
        builder.append(",");
        builder.append(String.format("%.2f", accY));
        builder.append(",");
        builder.append(String.format("%.2f", accZ));
        builder.append(")\n");
        builder.append("gyro:(");
        builder.append(String.format("%.2f", gyroX));
        builder.append(",");
        builder.append(String.format("%.2f", gyroY));
        builder.append(",");
        builder.append(String.format("%.2f", gyroZ));
        builder.append(")\n");
        return builder.toString();
    }


    /**
     * バイト配列から[OrpheInsoleValue]を取得します。
     *
     * @param bytes ORPHEINSOLEから送られたバイト配列
     * @param sidePosition 取り付け位置
     * @param accRange 加速度レンジ
     * @param gyroRange ジャイロレンジ
     * @return OrpheInsoleValue
     */
    @RequiresApi(api = Build.VERSION_CODES.O)
    public static OrpheInsoleValue[] fromBytes(
            byte[] bytes, OrpheSidePosition sidePosition, OrpheAccRange accRange, OrpheGyroRange gyroRange) throws Exception {

        final ArrayList<OrpheInsoleValue> res = new ArrayList();
        int index = 0;
        boolean isResend = false;
        switch (getUint8(bytes, 0)) {
            case 51:
            case 52:
            case 53:
                return new OrpheInsoleValue[0];
        }
        index = 2;

        final int serialNumber = getUint16(bytes, 1);
        final LocalDateTime now = LocalDateTime.now();
        final LocalDateTime baseTimestamp = LocalDateTime.of(
                now.getYear(),
                now.getMonth(),
                now.getDayOfMonth(),
                now.getHour(),
                getUint8(bytes, 4),
                getUint8(bytes, 5),
                getUint8(bytes, 6) * 1000
        );
        for (int s = 3; s >= 0; s--) {
            index = s * 24 + 8;
            final long duration = s == 0
                    ? 0
                    : 5 * 1000;
            final LocalDateTime timestamp = baseTimestamp.minusNanos(duration);
            // TODO: 計算で出力
            final double quatW = 0;
            final double quatX = 0;
            final double quatY = 0;
            final double quatZ = 0;
            final double gyroX = parseInt(bytes, index + 8) / (double) (1 << 15) * gyroRange.value;
            final double gyroY = parseInt(bytes, index + 10) / (double) (1 << 15) * gyroRange.value;
            final double gyroZ = parseInt(bytes, index + 12) / (double) (1 << 15) * gyroRange.value;
            final double accX = parseInt(bytes, index + 14) / (double) (1 << 15) * accRange.value;
            final double accY = parseInt(bytes, index + 16) / (double) (1 << 15) * accRange.value;
            final double accZ = parseInt(bytes, index + 18) / (double) (1 << 15) * accRange.value;
            final double pressure1 = parseInt(bytes, 20);
            final double pressure2 = parseInt(bytes, 22);
            final double pressure3 = parseInt(bytes, 24);
            final double pressure4 = parseInt(bytes, 26);
            final double pressure5 = parseInt(bytes, 28);
            final double pressure6 = parseInt(bytes, 30);
            res.add(
                    new OrpheInsoleValue(
                            sidePosition,
                            serialNumber,
                            0,
                            timestamp.toInstant(ZoneOffset.UTC).toEpochMilli(),
                            timestamp.toInstant(ZoneOffset.UTC).toEpochMilli(),
                            pressure1,
                            pressure2,
                            pressure3,
                            pressure4,
                            pressure5,
                            pressure6,
                            accX,
                            accY,
                            accZ,
                            gyroX,
                            gyroY,
                            gyroZ

                    )
            );
        }
        final OrpheInsoleValue[] array = new OrpheInsoleValue[res.size()];
        return res.toArray(array);
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
     * 圧力１
     */
    @NonNull public final double pressure1;
    /**
     * 圧力２
     */
    @NonNull public final double pressure2;
    /**
     * 圧力３
     */
    @NonNull public final double pressure3;
    /**
     * 圧力４
     */
    @NonNull public final double pressure4;
    /**
     * 圧力５
     */
    @NonNull public final double pressure5;
    /**
     * 圧力６
     */
    @NonNull public final double pressure6;


    /**
     * 加速度X
     */
    @NonNull public final double accX;
    /**
     * 加速度Y
     */
    @NonNull public final double accY;
    /**
     * 加速度Z
     */
    @NonNull public final double accZ;

    /**
     * ジャイロによる角度X
     */
    @NonNull public final double gyroX;
    /**
     * ジャイロによる角度Y
     */
    @NonNull public final double gyroY;
    /**
     * ジャイロによる角度Z
     */
    @NonNull public final double gyroZ;

    private static int parseInt(@NonNull byte[] bytes,  int index) {
        return (int)((getInt8(bytes, index) << 8) + getInt8(bytes, index + 1));
    }

    private static int getUint16(@NonNull byte[] data, int index) {
        return (int) (((data[index] & 0xFF) << 8) | (data[index + 1] & 0xFF));
    }
    private static byte getUint8(@NonNull byte[] data, int index) {
        return (byte) (data[index] & 0xFF);
    }
    private static byte getInt8(@NonNull byte[] data, int index) {
        return (byte) data[index];
    }
}
