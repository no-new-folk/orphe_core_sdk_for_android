package io.orphe.orphecoresdk;

import static android.content.ContentValues.TAG;

import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Map;

/**
 * ORPHE INSOLEのセンサー値を格納するためのクラス。
 */
public class OrpheInsoleValue {

    static final int PACKET_LENGTH_200_HZ = 104;
    static final int PACKET_LENGTH_100_HZ = 72;

    /// 圧力係数のデフォルト値
    private static final double DEFAULT_COEFFICIENT1 = 2.77942;
    private static final double DEFAULT_COEFFICIENT2 = 0.00235;
    private static final double DEFAULT_COEFFICIENT3 = 4.14411;
    private static final double DEFAULT_THRESHOLD = 240.0;


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

                          /// SDKが値を受信した時刻（Android端末時刻・epochミリ秒）
                          @NonNull final long receivedAt

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
      this.eulerYaw = 0.0;
      this.eulerPitch = 0.0;
      this.eulerRoll = 0.0;
      this.receivedAt = receivedAt;
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
                          @NonNull final double quatZ,

                          /// SDKが値を受信した時刻（Android端末時刻・epochミリ秒）
                          @NonNull final long receivedAt

                     ){
      final OrpheEulerAngles euler =
              OrpheEulerAngles.fromQuaternion(quatW, quatX, quatY, quatZ);
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
      this.eulerYaw = euler.yaw;
      this.eulerPitch = euler.pitch;
      this.eulerRoll = euler.roll;
      this.receivedAt = receivedAt;
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
        builder.append("euler(yaw,pitch,roll):(");
        builder.append(String.format("%.2f", eulerYaw));
        builder.append(",");
        builder.append(String.format("%.2f", eulerPitch));
        builder.append(",");
        builder.append(String.format("%.2f", eulerRoll));
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
     * @param coefficientMap 圧力係数
     * @param receivedAt SDKが値を受信した時刻（Android端末時刻・epochミリ秒）
     * @return OrpheInsoleValue
     */
    @RequiresApi(api = Build.VERSION_CODES.O)
    public static OrpheInsoleValue[] fromBytes(
            byte[] bytes, OrpheSidePosition sidePosition, OrpheAccRange accRange, OrpheGyroRange gyroRange, Map<OrpheInsoleSensorPosition, Map<OrpheInsoleCoefficient, Double>> coefficientMap, long receivedAt) throws Exception {
        return fromBytes(
                bytes,
                sidePosition,
                accRange,
                gyroRange,
                OrpheInsolePressureCalibration.fromMap(coefficientMap),
                receivedAt
        );
    }

    /**
     * バイト配列からOrpheInsoleValueを生成します。
     *
     * @param bytes バイト配列
     * @param sidePosition 取り付け位置
     * @param accRange 加速度レンジ
     * @param gyroRange ジャイロレンジ
     * @param pressureCalibration 6点の圧力補正設定
     * @param receivedAt SDKが値を受信した時刻（Android端末時刻・epochミリ秒）
     * @return OrpheInsoleValue
     */
    @RequiresApi(api = Build.VERSION_CODES.O)
    static OrpheInsoleValue[] fromBytes(
            byte[] bytes,
            OrpheSidePosition sidePosition,
            OrpheAccRange accRange,
            OrpheGyroRange gyroRange,
            OrpheInsolePressureCalibration pressureCalibration,
            long receivedAt
    ) throws Exception {
        return fromBytes(
                bytes,
                sidePosition,
                accRange,
                gyroRange,
                pressureCalibration,
                receivedAt,
                OrpheInsoleSamplingRate.hz200
        );
    }

    /**
     * request / fifo の出力レートを指定してセンサーパケットを変換します。
     * FWの蓄積データ (0x36) は200Hz固定のため、100Hz指定時は0ms/10msの2点へ間引きます。
     */
    @RequiresApi(api = Build.VERSION_CODES.O)
    static OrpheInsoleValue[] fromBytes(
            byte[] bytes,
            OrpheSidePosition sidePosition,
            OrpheAccRange accRange,
            OrpheGyroRange gyroRange,
            OrpheInsolePressureCalibration pressureCalibration,
            long receivedAt,
            OrpheInsoleSamplingRate outputSamplingRate
    ) throws Exception {

        final OrpheInsolePressureCalibration calibration = pressureCalibration == null
                ? OrpheInsolePressureCalibration.DEFAULT
                : pressureCalibration;

        if (bytes == null || bytes.length == 0) {
            throw new IllegalArgumentException("Sensor packet is empty.");
        }

        final int packetType = getUint8(bytes, 0);
        if ((packetType == 54 || packetType == 55) && bytes.length < PACKET_LENGTH_200_HZ) {
            throw new IllegalArgumentException(
                    "Sensor packet is too short. type=" + packetType + ", length=" + bytes.length);
        }
        if (packetType == 56 && bytes.length < PACKET_LENGTH_100_HZ) {
            throw new IllegalArgumentException(
                    "Sensor packet is too short. type=" + packetType + ", length=" + bytes.length);
        }

        final ArrayList<OrpheInsoleValue> res = new ArrayList();
        int index = 0;
        boolean isResend = false;
        switch (packetType) {
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
                final int[] samplePositions = outputSamplingRate == OrpheInsoleSamplingRate.hz100
                        ? new int[]{3, 1}
                        : new int[]{3, 2, 1, 0};
                for (int outputIndex = 0; outputIndex < samplePositions.length; outputIndex++) {
                    final int s = samplePositions[outputIndex];
                    index = s * 24 + 8;
                    final long duration = (3 - s) * 5_000_000L;
                    final LocalDateTime timestamp = baseTimestamp.plusNanos(duration);
                    // request / fifoの受信処理で、復号後に時系列姿勢を計算する。
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
                    final double pressureToeInside = calibratedPressure(
                            getUint16(bytes, index + 12), calibration, OrpheInsoleSensorPosition.toeInside);
                    final double pressureMidInside = calibratedPressure(
                            getUint16(bytes, index + 14), calibration, OrpheInsoleSensorPosition.midInside);
                    final double pressureToeOutside = calibratedPressure(
                            getUint16(bytes, index + 16), calibration, OrpheInsoleSensorPosition.toeOutside);
                    final double pressureCenter = calibratedPressure(
                            getUint16(bytes, index + 18), calibration, OrpheInsoleSensorPosition.center);
                    final double pressureMidOutside = calibratedPressure(
                            getUint16(bytes, index + 20), calibration, OrpheInsoleSensorPosition.midOutside);
                    final double pressureHeel = calibratedPressure(
                            getUint16(bytes, index + 22), calibration, OrpheInsoleSensorPosition.heel);
                    res.add(
                            new OrpheInsoleValue(
                                    sidePosition,
                                    serialNumber,
                                    samplePositions.length - outputIndex - 1,
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
                                    receivedAt

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
                    final long duration = (1 - s) * 10_000_000L;
                    final LocalDateTime timestamp = baseTimestamp.plusNanos(duration);
                    // realtime 100Hzではデバイス算出のクオータニオンを使用する。
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
                    final double pressureToeInside = calibratedPressure(
                            getUint16(bytes, index + 20), calibration, OrpheInsoleSensorPosition.toeInside);
                    final double pressureMidInside = calibratedPressure(
                            getUint16(bytes, index + 22), calibration, OrpheInsoleSensorPosition.midInside);
                    final double pressureToeOutside = calibratedPressure(
                            getUint16(bytes, index + 24), calibration, OrpheInsoleSensorPosition.toeOutside);
                    final double pressureCenter = calibratedPressure(
                            getUint16(bytes, index + 26), calibration, OrpheInsoleSensorPosition.center);
                    final double pressureMidOutside = calibratedPressure(
                            getUint16(bytes, index + 28), calibration, OrpheInsoleSensorPosition.midOutside);
                    final double pressureHeel = calibratedPressure(
                            getUint16(bytes, index + 30), calibration, OrpheInsoleSensorPosition.heel);
                    res.add(
                            new OrpheInsoleValue(
                                    sidePosition,
                                    serialNumber,
                                    s,
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
                                    quatZ,
                                    receivedAt
                                    )
                    );
                }
            }
            break;
        }
        final OrpheInsoleValue[] array = new OrpheInsoleValue[res.size()];
        return res.toArray(array);
    }

    /** 既存のセンサー値を保ったまま、算出済み姿勢と出力内位置を設定します。 */
    @NonNull
    OrpheInsoleValue withQuaternion(
            @NonNull final OrpheQuaternion quaternion,
            final int outputDataPosition
    ) {
        return new OrpheInsoleValue(
                sidePosition,
                serialNumber,
                outputDataPosition,
                startTime,
                endTime,
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
                quaternion.w,
                quaternion.x,
                quaternion.y,
                quaternion.z,
                receivedAt
        );
    }

    /** request / fifoの200Hz計算結果を指定出力レートへ変換します。 */
    @NonNull
    static OrpheInsoleValue[] forOutputSamplingRate(
            @NonNull final OrpheInsoleValue[] values,
            @NonNull final OrpheInsoleSamplingRate outputSamplingRate
    ) {
        if (outputSamplingRate == OrpheInsoleSamplingRate.hz200 || values.length < 4) {
            return values.clone();
        }
        return new OrpheInsoleValue[]{
                values[0].withQuaternion(
                        new OrpheQuaternion(
                                values[0].quatW,
                                values[0].quatX,
                                values[0].quatY,
                                values[0].quatZ
                        ),
                        1
                ),
                values[2].withQuaternion(
                        new OrpheQuaternion(
                                values[2].quatW,
                                values[2].quatX,
                                values[2].quatY,
                                values[2].quatZ
                        ),
                        0
                )
        };
    }

    private static double calibratedPressure(
            final double milliVolt,
            @NonNull final OrpheInsolePressureCalibration pressureCalibration,
            @NonNull final OrpheInsoleSensorPosition sensorPosition
    ) {
        final OrpheInsolePressureCoefficient coefficient =
                pressureCalibration.coefficientFor(sensorPosition);
        return milliVoltToNewton(
                milliVolt,
                coefficient.coefficient1,
                coefficient.coefficient2,
                coefficient.coefficient3,
                coefficient.threshold
        );
    }

    /**
     * coefficient2とthresholdにdemo010と同じ既定値を使用して圧力へ変換します。
     */
    public static double milliVoltToNewton(double milliVolt, Double coefficient1, Double coefficient3) {
        return milliVoltToNewton(
                milliVolt,
                coefficient1,
                DEFAULT_COEFFICIENT2,
                coefficient3,
                DEFAULT_THRESHOLD
        );
    }

    /**
     * 指定された4つの補正値を使用してmVをNへ変換します。
     */
    public static double milliVoltToNewton(
            double milliVolt,
            Double coefficient1,
            Double coefficient2,
            Double coefficient3,
            Double threshold
    ) {
        if (!Double.isFinite(milliVolt) || milliVolt < 0.0 || milliVolt >= 10000.0) {
            return 0.0;
        }

        final double c1 = coefficient1 == null ? DEFAULT_COEFFICIENT1 : coefficient1;
        final double c2 = coefficient2 == null ? DEFAULT_COEFFICIENT2 : coefficient2;
        final double c3 = coefficient3 == null ? DEFAULT_COEFFICIENT3 : coefficient3;
        final double pressureThreshold = threshold == null ? DEFAULT_THRESHOLD : threshold;
        if (!Double.isFinite(pressureThreshold) || milliVolt <= pressureThreshold) {
            return 0.0;
        }

        final double result = c1 * Math.exp(c2 * milliVolt) + c3;
        return Double.isFinite(result) ? Math.max(0.0, result) : 0.0;
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
     * 開始日時のタイムスタンプ（epochミリ秒）
     */
    @NonNull public final long startTime;

    /**
     * 終了日時のタイムスタンプ（epochミリ秒）
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

    /**
     * ヨー角（ラジアン）
     */
    @NonNull public final double eulerYaw;
    /**
     * ピッチ角（ラジアン）
     */
    @NonNull public final double eulerPitch;
    /**
     * ロール角（ラジアン）
     */
    @NonNull public final double eulerRoll;

    /**
     * SDKが値を受信した時刻（Android端末時刻・epochミリ秒）
     */
    @NonNull public final long receivedAt;


    private static int parseInt(@NonNull byte[] bytes,  int index) {
        return OrpheByteParser.getInt16BigEndian(bytes, index);
    }

    private static int getUint16(@NonNull byte[] data, int index) {
        return (int) (((data[index] & 0xFF) << 8) | (data[index + 1] & 0xFF));
    }
    private static byte getUint8(@NonNull byte[] data, int index) {
        return (byte) (data[index] & 0xFF);
    }
}
