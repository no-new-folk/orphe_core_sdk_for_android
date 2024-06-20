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
    @NonNull final double eulerX,
    @NonNull final double eulerY,
    @NonNull final double eulerZ,

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

    /// 正規化されたオイラー角
    @NonNull final double normalizedEulerX,
    @NonNull final double normalizedEulerY,
    @NonNull final double normalizedEulerZ,

    /// 正規化された加速度
    @NonNull final double normalizedAccX,
    @NonNull final double normalizedAccY,
    @NonNull final double normalizedAccZ,

    /// 正規化されたジャイロによる角度の範囲
    @NonNull  final double normalizedGyroX,
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
    @NonNull final boolean isStoredData
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
      this.eulerX = eulerX;
      this.eulerY = eulerY;
      this.eulerZ = eulerZ;
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
      this.normalizedEulerX = normalizedEulerX;
      this.normalizedEulerY = normalizedEulerY;
      this.normalizedEulerZ = normalizedEulerZ;
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
        builder.append("euler:(");
        builder.append(String.format("%.2f", eulerX));
        builder.append(",");
        builder.append(String.format("%.2f", eulerY));
        builder.append(",");
        builder.append(String.format("%.2f", eulerZ));
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
     * @return OrpheSensorValueの配列
     */
    @RequiresApi(api = Build.VERSION_CODES.O)
    public static OrpheSensorValue[] fromBytes(
            byte[] bytes, OrpheSidePosition sidePosition, OrpheAccRange accRange, OrpheGyroRange gyroRange) throws Exception {
        if (bytes.length == 92) {
            final ArrayList<OrpheSensorValue> res = new ArrayList();
            int index = 0;
            boolean isResend = false;
            switch (getUint8(bytes,0)) {
                case 51:
                    if (getUint8(bytes, 1) == 3) {
                        System.out.println("MotionValue: 蓄積開始！");
                    } else if (getUint8(bytes,1) == 4) {
                        throw new Exception("再送エラー");
                    }
                    return new OrpheSensorValue[0];
                case 52:
                    isResend = true;
                    break;
            }
            index = 2;

            final int serialNumber = parseInt(bytes,1);
            final LocalDateTime now =  LocalDateTime.now();
            final LocalDateTime baseTimestamp = LocalDateTime.of(
                    now.getYear(),
                    now.getMonth(),
                    now.getDayOfMonth(),
                    getUint8(bytes,3),
                    getUint8(bytes,4),
                    getUint8(bytes,5),
                    getUint8(bytes,6) * 1000
                    );
            for (int s = 3; s >= 0; s--) {
                index = s * 21 + 8;
                final long duration = s == 0
                        ? 0
                        : getUint8(bytes, index - 1) * 1000;
                final LocalDateTime timestamp = baseTimestamp.minusNanos (duration);
                final double quatW = parseInt(bytes,index) / 16384.0;
                final double quatX =  parseInt(bytes,index + 2) / 16384.0;
                final double quatY = parseInt(bytes,index + 4) / 16384.0;
                final double quatZ =  parseInt(bytes,index + 6) / 16384.0;
                final double gyroX = parseInt(bytes,index + 8) / (double)(1 << 15) * gyroRange.value;
                final double gyroY = parseInt(bytes,index + 10) / (double)(1 << 15) * gyroRange.value;
                final double gyroZ =  parseInt(bytes,index + 12) / (double)(1 << 15) * gyroRange.value;
                final double accX = parseInt(bytes,index + 14) / (double)(1 << 15) * accRange.value;
                final double accY =  parseInt(bytes,index + 16) / (double)(1 << 15) * accRange.value;
                final double accZ =  parseInt(bytes,index + 18) / (double)(1 << 15) * accRange.value;
                final double eulerX = toEulerX(quatW, quatX, quatY, quatZ);
                final double eulerY = toEulerY(quatW, quatX, quatY, quatZ);
                final double eulerZ = toEulerZ(quatW, quatX, quatY, quatZ);
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
                                eulerX,
                                eulerY,
                                eulerZ,
                                accX,
                                accY,
                                accZ,
                                gyroX,
                                gyroY,
                                gyroZ,
                                gravityX,
                                gravityY,
                                gravityZ,
                                eulerX / 180.0,
                                eulerY / 180.0,
                                eulerZ / 180.0,
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
                                false
                            )
                );
            }
            final OrpheSensorValue[] array = new OrpheSensorValue[res.size()];
            return res.toArray(array);
        } else if (bytes.length == 40) {}
        return new OrpheSensorValue[0];
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
    @NonNull final double quatX;
    /**
     * クオータニオンY
     */
    @NonNull final double quatY;
    /**
     * クオータニオンZ
     */
    @NonNull final double quatZ;
    /**
     * クオータニオンW
     */
    @NonNull final double quatW;

    /**
     * オイラー角X
     */
    @NonNull final double eulerX;
    /**
     * オイラー角Y
     */
    @NonNull final double eulerY;
    /**
     * オイラー角Z
     */
    @NonNull final double eulerZ;

    /**
     * 加速度X
     */
    @NonNull final double accX;
    /**
     * 加速度Y
     */
    @NonNull final double accY;
    /**
     * 加速度Z
     */
    @NonNull final double accZ;

    /**
     * ジャイロによる角度X
     */
    @NonNull final double gyroX;
    /**
     * ジャイロによる角度Y
     */
    @NonNull final double gyroY;
    /**
     * ジャイロによる角度Z
     */
    @NonNull final double gyroZ;

    /**
     * 重力加速度X
     */
    ///
    @NonNull final double accOfGravityX;
    /**
     * 重力加速度Y
     */
    @NonNull final double accOfGravityY;
    /**
     * 重力加速度Z
     */
    @NonNull final double accOfGravityZ;


    /**
     * 正規化されたオイラー角X
     */
    @NonNull final double normalizedEulerX;
    /**
     * 正規化されたオイラー角Y
     */
    @NonNull final double normalizedEulerY;
    /**
     * 正規化されたオイラー角Z
     */
    @NonNull final double normalizedEulerZ;

    /**
     * 正規化された加速度X
     */
    @NonNull final double normalizedAccX;
    /**
     * 正規化された加速度Y
     */
    @NonNull final double normalizedAccY;
    /**
     * 正規化された加速度Z
     */
    @NonNull final double normalizedAccZ;

    /**
     * 正規化されたジャイロによる角度X
     */
    @NonNull  final double normalizedGyroX;
    /**
     * 正規化されたジャイロによる角度Y
     */
    @NonNull final double normalizedGyroY;
    /**
     * 正規化されたジャイロによる角度Z
     */
    @NonNull final double normalizedGyroZ;

    /**
     * 正規化された磁力
     */
    @NonNull final double normalizedMag;

    /**
     * 正規化された世界座標系の加速度X
     */
    @NonNull final double normalizedWorldCoordinateAccX;
    /**
     * 正規化された世界座標系の加速度Y
     */
    @NonNull  final double normalizedWorldCoordinateAccY;
    /**
     * 正規化された世界座標系の加速度Z
     */
    @NonNull final double normalizedWorldCoordinateAccZ;

    /**
     * 磁力
     */
    ///
    @NonNull final double mag;

    /**
     * 衝撃値
     */
    @NonNull final int shock;

    /**
     * 正規化された衝撃値
     */
    @NonNull final double normalizedShock;

    /**
     * 30秒保持の再送処理を行ったかどうか
     */
    @NonNull final boolean isResendData;

    /**
     * ドロップしたフレーム数
     */
    @NonNull final int dropNum;

    /**
     * 30秒保持でデータ取得を行ったかどうか
     */
    @NonNull final boolean isStoredData;


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

    private static double toEulerX(
            double quatW, double quatX, double quatY, double quatZ) {
        final double w = quatY;
        final double x = quatZ;
        final double y = quatW;
        final double z = quatX;

        double phi = 0.0;
        double theta = 0.0;
        double psi = 0.0;
        theta = Math.asin(-2.0 * (x * z - w * y)) / (2.0 * Math.PI)* 360.0;
        phi = Math.atan2(2.0 * (y * z + w * x), w * w - x * x - y * y + z * z) /
                (2.0 * Math.PI)*
                360.0;
        psi = Math.atan2(2.0 * (x * y + w * z), w * w + x * x - y * y - z * z) /
                (2.0 * Math.PI) *
                360.0;

        if (phi > 0) {
            phi = 180.0 - phi;
        } else {
            phi = -180.0 - phi;
        }
        theta = -theta;
        psi = -psi;

        return theta;
    }


    private static double toEulerY(
            double quatW, double quatX, double quatY, double quatZ) {
        final double w = quatY;
        final double x = quatZ;
        final double y = quatW;
        final double z = quatX;

        double phi = 0.0;
        double theta = 0.0;
        double psi = 0.0;
        theta = Math.asin(-2.0 * (x * z - w * y)) / (2.0 * Math.PI)* 360.0;
        phi = Math.atan2(2.0 * (y * z + w * x), w * w - x * x - y * y + z * z) /
                (2.0 * Math.PI)*
                360.0;
        psi = Math.atan2(2.0 * (x * y + w * z), w * w + x * x - y * y - z * z) /
                (2.0 * Math.PI) *
                360.0;

        if (phi > 0) {
            phi = 180.0 - phi;
        } else {
            phi = -180.0 - phi;
        }
        theta = -theta;
        psi = -psi;

        return phi;
    }


    private static double toEulerZ(
            double quatW, double quatX, double quatY, double quatZ) {
        final double w = quatY;
        final double x = quatZ;
        final double y = quatW;
        final double z = quatX;

        double phi = 0.0;
        double theta = 0.0;
        double psi = 0.0;
        theta = Math.asin(-2.0 * (x * z - w * y)) / (2.0 * Math.PI)* 360.0;
        phi = Math.atan2(2.0 * (y * z + w * x), w * w - x * x - y * y + z * z) /
                (2.0 * Math.PI)*
                360.0;
        psi = Math.atan2(2.0 * (x * y + w * z), w * w + x * x - y * y - z * z) /
                (2.0 * Math.PI) *
                360.0;

        if (phi > 0) {
            phi = 180.0 - phi;
        } else {
            phi = -180.0 - phi;
        }
        theta = -theta;
        psi = -psi;

        return psi;
    }


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
