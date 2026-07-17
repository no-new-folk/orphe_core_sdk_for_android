package io.orphe.orphecoresdk;

import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import java.time.ZoneOffset;
import java.util.ArrayList;
import java.time.LocalDateTime;

/**
 * ORPHEのセンサー値を格納するためのクラス。
 */
public class OrpheSensorValue {
    /**
     * ORPHEのセンサー値を格納するためのクラス。
     */
  public OrpheSensorValue(@NonNull
                     final OrpheSidePosition sidePosition,

    /// シリアルナンバー
    @NonNull final int serialNumber,

    /// 同じデータ中の位置
    @NonNull final int dataPosition,

    /// タイムスタンプ
    @NonNull final long startTime,

    final long endTime,

    /// クオータニオン
    @NonNull final double quatX,
    @NonNull final double quatY,
    @NonNull final double quatZ,
    @NonNull final double quatW,

    /// オイラー角
    @NonNull final double eulerYaw,
    @NonNull final double eulerPitch,
    @NonNull final double eulerRoll,

    /// 加速度
    @NonNull final double accX,
    @NonNull final double accY,
    @NonNull final double accZ,

    /// ジャイロによる角度の範囲
    @NonNull final double gyroX,
    @NonNull final double gyroY,
    @NonNull final double gyroZ,

    /// 重力加速度
    @NonNull final double accOfGravityX,
    @NonNull final double accOfGravityY,
    @NonNull final double accOfGravityZ,

    /// 正規化された加速度
    @NonNull final double normalizedAccX,
    @NonNull final double normalizedAccY,
    @NonNull final double normalizedAccZ,

    /// 正規化されたジャイロによる角度の範囲
    @NonNull final double normalizedGyroX,
    @NonNull final double normalizedGyroY,
    @NonNull final double normalizedGyroZ,

    /// 正規化された磁力
    @NonNull final double normalizedMag,

    /// 正規化された世界座標系の加速度
    @NonNull final double normalizedWorldCoordinateAccX,
    @NonNull  final double normalizedWorldCoordinateAccY,
    @NonNull final double normalizedWorldCoordinateAccZ,

    /// 磁力
    @NonNull final double mag,

    /// 衝撃値
    @NonNull final int shock,

    /// 正規化された衝撃値
    @NonNull final double normalizedShock,

    /// 30秒保持の再送処理を行ったかどうか
    @NonNull final boolean isResendData,

    /// ドロップしたフレーム数
    @NonNull final int dropNum,

    /// 30秒保持でデータ取得を行ったかどうか
    @NonNull final boolean isStoredData,

    /// SDKが値を受信した時刻（Android端末時刻・epochミリ秒）
    @NonNull final long receivedAt
                     ){
      this.sidePosition = sidePosition;
      this.serialNumber = serialNumber;
      this.dataPosition = dataPosition;
      this.startTime = startTime;
      this.endTime = endTime;
      this.quatX = quatX;
      this.quatY = quatY;
      this.quatZ = quatZ;
      this.quatW = quatW;
      this.eulerYaw = eulerYaw;
      this.eulerPitch = eulerPitch;
      this.eulerRoll = eulerRoll;
      this.accX = accX;
      this.accY = accY;
      this.accZ = accZ;
      this.gyroX = gyroX;
      this.gyroY = gyroY;
      this.gyroZ = gyroZ;
      this.accOfGravityX = accOfGravityX;
      this.accOfGravityY = accOfGravityY;
      this.accOfGravityZ = accOfGravityZ;
      this.normalizedAccX = normalizedAccX;
      this.normalizedAccY = normalizedAccY;
      this.normalizedAccZ = normalizedAccZ;
      this.normalizedGyroX = normalizedGyroX;
      this.normalizedGyroY = normalizedGyroY;
      this.normalizedGyroZ = normalizedGyroZ;
      this.normalizedMag = normalizedMag;
      this.normalizedWorldCoordinateAccX = normalizedWorldCoordinateAccX;
      this.normalizedWorldCoordinateAccY = normalizedWorldCoordinateAccY;
      this.normalizedWorldCoordinateAccZ = normalizedWorldCoordinateAccZ;
      this.mag = mag;
      this.shock = shock;
      this.normalizedShock = normalizedShock;
      this.isResendData = isResendData;
      this.dropNum = dropNum;
      this.isStoredData = isStoredData;
      this.receivedAt = receivedAt;
    }

    /**
     * 文字列に変換します。
     *
     * @return 文字列
     */
    public String toString(){
      final StringBuilder builder = new StringBuilder();
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
        builder.append("quat:(");
        builder.append(String.format("%.2f", quatW));
        builder.append(",");
        builder.append(String.format("%.2f", quatX));
        builder.append(",");
        builder.append(String.format("%.2f", quatY));
        builder.append(",");
        builder.append(String.format("%.2f", quatZ));
        builder.append(")\n");
        return builder.toString();
    }


    /**
     * バイト配列から[OrpheSensorValue]を取得します。
     *
     * @param bytes ORPHECOREから送られたバイト配列
     * @param sidePosition 取り付け位置
     * @param accRange 加速度レンジ
     * @param gyroRange ジャイロレンジ
     * @param receivedAt SDKが値を受信した時刻（Android端末時刻・epochミリ秒）
     * @return OrpheSensorValueの配列
     */
    @RequiresApi(api = Build.VERSION_CODES.O)
    public static OrpheSensorValue[] fromBytes(
            byte[] bytes, OrpheSidePosition sidePosition, OrpheAccRange accRange, OrpheGyroRange gyroRange, long receivedAt) throws Exception {
        final ArrayList<OrpheSensorValue> res = new ArrayList();
        int index = 0;
        boolean isResend = false;
        switch (getUint8(bytes, 0)) {
            case 51:
            case 52:
            case 53: {
                return new OrpheSensorValue[0];
            }
            case 50: {
                index = 2;

                final int serialNumber = parseInt(bytes, 1);
                final LocalDateTime now = LocalDateTime.now();
                final LocalDateTime baseTimestamp = LocalDateTime.of(
                        now.getYear(),
                        now.getMonth(),
                        now.getDayOfMonth(),
                        getUint8(bytes, 3),
                        getUint8(bytes, 4),
                        getUint8(bytes, 5),
                        getUint8(bytes, 6) * 1000
                );
                for (int s = 3; s >= 0; s--) {
                    index = s * 21 + 8;
                    final long duration = s == 0
                            ? 0
                            : getUint8(bytes, index - 1) * 1000;
                    final LocalDateTime timestamp = baseTimestamp.plusNanos(duration);
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
                    final OrpheEulerAngles euler =
                            OrpheEulerAngles.fromQuaternion(quatW, quatX, quatY, quatZ);
                    final double gravityX = toGravityX(quatW, quatX, quatY, quatZ);
                    final double gravityY = toGravityY(quatW, quatX, quatY, quatZ);
                    final double gravityZ = toGravityZ(quatW, quatX, quatY, quatZ);
                    res.add(
                            new OrpheSensorValue(
                                    sidePosition,
                                    serialNumber,
                                    s,
                                    timestamp.toInstant(ZoneOffset.UTC).toEpochMilli(),
                                    timestamp.toInstant(ZoneOffset.UTC).toEpochMilli(),
                                    quatX,
                                    quatY,
                                    quatZ,
                                    quatW,
                                    euler.yaw,
                                    euler.pitch,
                                    euler.roll,
                                    accX,
                                    accY,
                                    accZ,
                                    gyroX,
                                    gyroY,
                                    gyroZ,
                                    gravityX,
                                    gravityY,
                                    gravityZ,
                                    accX / accRange.value,
                                    accY / accRange.value,
                                    accZ / accRange.value,
                                    gyroX / gyroRange.value,
                                    gyroY / gyroRange.value,
                                    gyroZ / gyroRange.value,
                                    0,
                                    0,
                                    0,
                                    0,
                                    0,
                                    0,
                                    0,
                                    false,
                                    0,
                                    false,
                                    receivedAt
                            )
                    );
                }
            }
            break;
            case 54: {
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
                for (int s = 7; s >= 0; s--) {
                    index = s * 12 + 8;
                    final long duration = s == 0
                            ? 0
                            : 5 * 1000;
                    final LocalDateTime timestamp = baseTimestamp.minusNanos(duration);
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
                    final OrpheEulerAngles euler =
                            OrpheEulerAngles.fromQuaternion(quatW, quatX, quatY, quatZ);
                    final double gravityX = toGravityX(quatW, quatX, quatY, quatZ);
                    final double gravityY = toGravityY(quatW, quatX, quatY, quatZ);
                    final double gravityZ = toGravityZ(quatW, quatX, quatY, quatZ);
                    res.add(
                            new OrpheSensorValue(
                                    sidePosition,
                                    serialNumber,
                                    s,
                                    timestamp.toInstant(ZoneOffset.UTC).toEpochMilli(),
                                    timestamp.toInstant(ZoneOffset.UTC).toEpochMilli(),
                                    quatX,
                                    quatY,
                                    quatZ,
                                    quatW,
                                    euler.yaw,
                                    euler.pitch,
                                    euler.roll,
                                    accX,
                                    accY,
                                    accZ,
                                    gyroX,
                                    gyroY,
                                    gyroZ,
                                    gravityX,
                                    gravityY,
                                    gravityZ,
                                    accX / accRange.value,
                                    accY / accRange.value,
                                    accZ / accRange.value,
                                    gyroX / gyroRange.value,
                                    gyroY / gyroRange.value,
                                    gyroZ / gyroRange.value,
                                    0,
                                    0,
                                    0,
                                    0,
                                    0,
                                    0,
                                    0,
                                    false,
                                    0,
                                    false,
                                    receivedAt
                            )
                    );
                }
            }
            break;
        }
        final OrpheSensorValue[] array = new OrpheSensorValue[res.size()];
        return res.toArray(array);
    }

    /** 既存の生値とメタデータを保ち、算出済み姿勢と派生値を設定します。 */
    @NonNull
    OrpheSensorValue withQuaternion(@NonNull final OrpheQuaternion quaternion) {
        final OrpheEulerAngles euler = OrpheEulerAngles.fromQuaternion(
                quaternion.w, quaternion.x, quaternion.y, quaternion.z);
        final double gravityX = toGravityX(
                quaternion.w, quaternion.x, quaternion.y, quaternion.z);
        final double gravityY = toGravityY(
                quaternion.w, quaternion.x, quaternion.y, quaternion.z);
        final double gravityZ = toGravityZ(
                quaternion.w, quaternion.x, quaternion.y, quaternion.z);
        return new OrpheSensorValue(
                sidePosition,
                serialNumber,
                dataPosition,
                startTime,
                endTime,
                quaternion.x,
                quaternion.y,
                quaternion.z,
                quaternion.w,
                euler.yaw,
                euler.pitch,
                euler.roll,
                accX,
                accY,
                accZ,
                gyroX,
                gyroY,
                gyroZ,
                gravityX,
                gravityY,
                gravityZ,
                normalizedAccX,
                normalizedAccY,
                normalizedAccZ,
                normalizedGyroX,
                normalizedGyroY,
                normalizedGyroZ,
                normalizedMag,
                normalizedWorldCoordinateAccX,
                normalizedWorldCoordinateAccY,
                normalizedWorldCoordinateAccZ,
                mag,
                shock,
                normalizedShock,
                isResendData,
                dropNum,
                isStoredData,
                receivedAt
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
     * クオータニオンW
     */
    @NonNull public final double quatW;

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
     * 重力加速度X
     */
    ///
    @NonNull public final double accOfGravityX;
    /**
     * 重力加速度Y
     */
    @NonNull public final double accOfGravityY;
    /**
     * 重力加速度Z
     */
    @NonNull public final double accOfGravityZ;

    /**
     * 正規化された加速度X
     */
    @NonNull public final double normalizedAccX;
    /**
     * 正規化された加速度Y
     */
    @NonNull public final double normalizedAccY;
    /**
     * 正規化された加速度Z
     */
    @NonNull public final double normalizedAccZ;

    /**
     * 正規化されたジャイロによる角度X
     */
    @NonNull public final double normalizedGyroX;
    /**
     * 正規化されたジャイロによる角度Y
     */
    @NonNull public final double normalizedGyroY;
    /**
     * 正規化されたジャイロによる角度Z
     */
    @NonNull public final double normalizedGyroZ;

    /**
     * 正規化された磁力
     */
    @NonNull public final double normalizedMag;

    /**
     * 正規化された世界座標系の加速度X
     */
    @NonNull public final double normalizedWorldCoordinateAccX;
    /**
     * 正規化された世界座標系の加速度Y
     */
    @NonNull public final double normalizedWorldCoordinateAccY;
    /**
     * 正規化された世界座標系の加速度Z
     */
    @NonNull public final double normalizedWorldCoordinateAccZ;

    /**
     * 磁力
     */
    ///
    @NonNull public final double mag;

    /**
     * 衝撃値
     */
    @NonNull public final int shock;

    /**
     * 正規化された衝撃値
     */
    @NonNull public final double normalizedShock;

    /**
     * 30秒保持の再送処理を行ったかどうか
     */
    @NonNull public final boolean isResendData;

    /**
     * ドロップしたフレーム数
     */
    @NonNull public final int dropNum;

    /**
     * 30秒保持でデータ取得を行ったかどうか
     */
    @NonNull public final boolean isStoredData;

    /**
     * SDKが値を受信した時刻（Android端末時刻・epochミリ秒）
     */
    @NonNull public final long receivedAt;


    private static double toGravityX(
      double quatW, double quatX, double quatY, double quatZ) {
        return (quatW * quatX + quatY * quatZ) * (-2.0);
    }
    private static double toGravityY(
            double quatW, double quatX, double quatY, double quatZ) {
        return (quatX * quatZ - quatW * quatY) * 2.0;
    }

    private static double toGravityZ(
            double quatW, double quatX, double quatY, double quatZ) {
        return  quatW * quatW - quatX * quatX - quatY * quatY + quatZ * quatZ;
    }

    private static int parseInt(@NonNull byte[] bytes,  int index) {
        return OrpheByteParser.getInt16BigEndian(bytes, index);
    }

    private static int getUint16(@NonNull byte[] data, int index) {
        return ((data[index] & 0xFF) << 8) | (data[index + 1] & 0xFF);
    }
    private static byte getUint8(@NonNull byte[] data, int index) {
        return (byte) (data[index] & 0xFF);
    }
}
