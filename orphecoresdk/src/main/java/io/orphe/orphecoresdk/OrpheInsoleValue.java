package io.orphe.orphecoresdk;

import static android.content.ContentValues.TAG;

import android.os.Build;
import android.util.Log;

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
                          @NonNull final double pressureToeOutside,
                          @NonNull final double pressureMidOutside,
                          @NonNull final double pressureToeInside,
                          @NonNull final double pressureCenter,
                          @NonNull final double pressureMidInside,
                          @NonNull final double pressureHeel,

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
      this.pressureToeOutside = pressureToeOutside;
      this.pressureMidOutside = pressureMidOutside;
      this.pressureToeInside = pressureToeInside;
      this.pressureCenter = pressureCenter;
      this.pressureMidInside = pressureMidInside;
      this.pressureHeel = pressureHeel;
      this.accX = accX;
      this.accY = accY;
      this.accZ = accZ;
      this.gyroX = gyroX;
      this.gyroY = gyroY;
      this.gyroZ = gyroZ;
      this.quatW = 0.0;
      this.quatX = 0.0;
      this.quatY = 0.0;
      this.quatZ = 0.0;
    }


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
                          @NonNull final double pressureToeOutside,
                          @NonNull final double pressureMidOutside,
                          @NonNull final double pressureToeInside,
                          @NonNull final double pressureCenter,
                          @NonNull final double pressureMidInside,
                          @NonNull final double pressureHeel,

                          /// 加速度
                          @NonNull final double accX,
                          @NonNull final double accY,
                          @NonNull final double accZ,

                          /// ジャイロによる角度の範囲
                          @NonNull final double gyroX,
                          @NonNull final double gyroY,
                          @NonNull final double gyroZ,

                          /// クオータニオン
                          @NonNull final double quatW,
                          @NonNull final double quatX,
                          @NonNull final double quatY,
                          @NonNull final double quatZ

                     ){
      this.sidePosition = sidePosition;
      this.serialNumber = serialNumber;
      this.dataPosition = dataPosition;
      this.startTime = startTime;
      this.endTime = endTime;
      this.pressureToeOutside = pressureToeOutside;
      this.pressureMidOutside = pressureMidOutside;
      this.pressureToeInside = pressureToeInside;
      this.pressureCenter = pressureCenter;
      this.pressureMidInside = pressureMidInside;
      this.pressureHeel = pressureHeel;
      this.accX = accX;
      this.accY = accY;
      this.accZ = accZ;
      this.gyroX = gyroX;
      this.gyroY = gyroY;
      this.gyroZ = gyroZ;
      this.quatW = quatW;
      this.quatX = quatX;
      this.quatY = quatY;
      this.quatZ = quatZ;
    }

    /**
     * 文字列に変換します。
     *
     * @return 文字列
     */
    public String toString(){
      final StringBuilder builder = new StringBuilder();
        builder.append("#");
        builder.append(String.format("%d", serialNumber));
        builder.append(" @");
        builder.append(String.format("%d", startTime));
        builder.append("\n");
        builder.append("pressure:(");
        builder.append(String.format("%.2f", pressureToeOutside));
        builder.append(",");
        builder.append(String.format("%.2f", pressureMidOutside));
        builder.append(",");
        builder.append(String.format("%.2f", pressureToeInside));
        builder.append(",");
        builder.append(String.format("%.2f", pressureCenter));
        builder.append(",");
        builder.append(String.format("%.2f", pressureMidInside));
        builder.append(",");
        builder.append(String.format("%.2f", pressureHeel));
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
            case 53: {
                return new OrpheInsoleValue[0];
            }
            case 54:
            case 55: {
                index = 2;

                final int serialNumber = getUint16(bytes, 1);
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
                    final double gyroX = parseInt(bytes, index) / (double) (1 << 15) * gyroRange.value;
                    final double gyroY = parseInt(bytes, index + 2) / (double) (1 << 15) * gyroRange.value;
                    final double gyroZ = parseInt(bytes, index + 4) / (double) (1 << 15) * gyroRange.value;
                    final double accX = parseInt(bytes, index + 6) / (double) (1 << 15) * accRange.value;
                    final double accY = parseInt(bytes, index + 8) / (double) (1 << 15) * accRange.value;
                    final double accZ = parseInt(bytes, index + 10) / (double) (1 << 15) * accRange.value;
                    // Log.d(TAG, "ToeOutside: " + parseInt(bytes, 16) + "ToeInside: " + parseInt(bytes, 12) +"MidOutside: " +  parseInt(bytes, 20) + "Center: " + parseInt(bytes, 18) + "MidInside: " + parseInt(bytes, 14) + "Heel" +  parseInt(bytes, 22));
                    final double pressureToeInside = milliVoltToNewton((double) parseInt(bytes, index + 12));
                    final double pressureMidInside = milliVoltToNewton((double) parseInt(bytes, index + 14));
                    final double pressureToeOutside = milliVoltToNewton((double) parseInt(bytes, index + 16));
                    final double pressureCenter = milliVoltToNewton((double) parseInt(bytes, index + 18));
                    final double pressureMidOutside = milliVoltToNewton((double) parseInt(bytes, index + 20));
                    final double pressureHeel = milliVoltToNewton((double) parseInt(bytes, index + 22));
                    res.add(
                            new OrpheInsoleValue(
                                    sidePosition,
                                    serialNumber,
                                    0,
                                    timestamp.toInstant(ZoneOffset.UTC).toEpochMilli(),
                                    timestamp.toInstant(ZoneOffset.UTC).toEpochMilli(),
                                    pressureToeOutside,
                                    pressureMidOutside,
                                    pressureToeInside,
                                    pressureCenter,
                                    pressureMidInside,
                                    pressureHeel,
                                    accX,
                                    accY,
                                    accZ,
                                    gyroX,
                                    gyroY,
                                    gyroZ

                            )
                    );
                }
            }
            break;
            case 56: {
                index = 2;

                final int serialNumber = getUint16(bytes, 1);
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
                for (int s = 1; s >= 0; s--) {
                    index = s * 32 + 8;
                    final long duration = s == 0
                            ? 0
                            : 5 * 1000;
                    final LocalDateTime timestamp = baseTimestamp.minusNanos(duration);
                    // TODO: 計算で出力
                    final double quatW = parseInt(bytes, index) / 16384.0;
                    final double quatX = parseInt(bytes, index + 2) / 16384.0;
                    final double quatY = parseInt(bytes, index + 4) / 16384.0;
                    final double quatZ = parseInt(bytes, index + 6) / 16384.0;
                    final double gyroX = parseInt(bytes, index + 8) / (double) (1 << 15) * gyroRange.value;
                    final double gyroY = parseInt(bytes, index + 10) / (double) (1 << 15) * gyroRange.value;
                    final double gyroZ = parseInt(bytes, index + 12) / (double) (1 << 15) * gyroRange.value;
                    final double accX = parseInt(bytes, index + 14) / (double) (1 << 15) * accRange.value;
                    final double accY = parseInt(bytes, index + 16) / (double) (1 << 15) * accRange.value;
                    final double accZ = parseInt(bytes, index + 18) / (double) (1 << 15) * accRange.value;
                    // Log.d(TAG, "ToeOutside: " + parseInt(bytes, 20) + "ToeInside: " + parseInt(bytes, 24) +"MidOutside: " +  parseInt(bytes, 22) + "pressureCenter: " + parseInt(bytes, 26) + "pressureMidInside: " + parseInt(bytes, 28) + "Heel" +  parseInt(bytes, 30));
                    final double pressureToeInside = milliVoltToNewton((double) parseInt(bytes, index + 20));
                    final double pressureMidInside = milliVoltToNewton((double) parseInt(bytes, index + 22));
                    final double pressureToeOutside = milliVoltToNewton((double) parseInt(bytes, index + 24));
                    final double pressureCenter = milliVoltToNewton((double) parseInt(bytes, index + 26));
                    final double pressureMidOutside = milliVoltToNewton((double) parseInt(bytes, index + 28));
                    final double pressureHeel = milliVoltToNewton((double) parseInt(bytes, index + 30));
                    res.add(
                            new OrpheInsoleValue(
                                    sidePosition,
                                    serialNumber,
                                    0,
                                    timestamp.toInstant(ZoneOffset.UTC).toEpochMilli(),
                                    timestamp.toInstant(ZoneOffset.UTC).toEpochMilli(),
                                    pressureToeOutside,
                                    pressureMidOutside,
                                    pressureToeInside,
                                    pressureCenter,
                                    pressureMidInside,
                                    pressureHeel,
                                    accX,
                                    accY,
                                    accZ,
                                    gyroX,
                                    gyroY,
                                    gyroZ,
                                    quatW,
                                    quatX,
                                    quatY,
                                    quatZ
                                    )
                    );
                }
            }
            break;
        }
        final OrpheInsoleValue[] array = new OrpheInsoleValue[res.size()];
        return res.toArray(array);
    }

    public static double milliVoltToNewton(double milliVolt) {
        return Math.pow(Math.E, ((milliVolt * 3.3 / 4096 * 1000) + 360.02) / 300.03);
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
    @NonNull public final double pressureToeOutside;
    /**
     * 圧力２
     */
    @NonNull public final double pressureMidOutside;
    /**
     * 圧力３
     */
    @NonNull public final double pressureToeInside;
    /**
     * 圧力４
     */
    @NonNull public final double pressureCenter;
    /**
     * 圧力５
     */
    @NonNull public final double pressureMidInside;
    /**
     * 圧力６
     */
    @NonNull public final double pressureHeel;


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

    /**
     * クオータニオンW
     */
    @NonNull public final double quatW;
    /**
     * クオータニオンX
     */
    @NonNull public final double quatX;
    /**
     * クオータニオンY
     */
    @NonNull public final double quatY;
    /**
     * クオータニオンZ
     */
    @NonNull public final double quatZ;


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
