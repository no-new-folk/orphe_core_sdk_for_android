package io.orphe.orphecoresdkforandroid;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import io.orphe.orphecoresdk.OrpheInsoleValue;
import io.orphe.orphecoresdk.OrpheSensorValue;
import io.orphe.orphecoresdk.OrpheSidePosition;

/**
 * INSOLE（OrpheInsoleValue）とCORE（OrpheSensorValue）の受信値を、
 * グラフ表示とCSV保存で同じように扱うためのサンプルアプリ内の値。
 */
public final class SensorSample {
    public enum DeviceType {
        insole("INSOLE"),
        core("CORE");

        public final String label;

        DeviceType(String label) {
            this.label = label;
        }

        @Nullable
        public static DeviceType fromLabel(@Nullable String label) {
            for (DeviceType type : values()) {
                if (type.label.equals(label)) {
                    return type;
                }
            }
            return null;
        }
    }

    public final DeviceType deviceType;
    public final OrpheSidePosition sidePosition;
    public final int serialNumber;
    public final int dataPosition;
    public final long startTime;
    public final long receivedAt;
    public final double accX;
    public final double accY;
    public final double accZ;
    public final double gyroX;
    public final double gyroY;
    public final double gyroZ;
    public final double quatW;
    public final double quatX;
    public final double quatY;
    public final double quatZ;
    /** TO, MO, TI, C, MI, Hの順の圧力値。圧力センサーのないCOREではnull。 */
    @Nullable public final double[] pressures;

    SensorSample(
            DeviceType deviceType,
            OrpheSidePosition sidePosition,
            int serialNumber,
            int dataPosition,
            long startTime,
            long receivedAt,
            double accX, double accY, double accZ,
            double gyroX, double gyroY, double gyroZ,
            double quatW, double quatX, double quatY, double quatZ,
            @Nullable double[] pressures
    ) {
        this.deviceType = deviceType;
        this.sidePosition = sidePosition;
        this.serialNumber = serialNumber;
        this.dataPosition = dataPosition;
        this.startTime = startTime;
        this.receivedAt = receivedAt;
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
        this.pressures = pressures;
    }

    public static SensorSample fromInsole(@NonNull OrpheInsoleValue value) {
        return new SensorSample(
                DeviceType.insole,
                value.sidePosition,
                value.serialNumber,
                value.dataPosition,
                value.startTime,
                value.receivedAt,
                value.accX, value.accY, value.accZ,
                value.gyroX, value.gyroY, value.gyroZ,
                value.quatW, value.quatX, value.quatY, value.quatZ,
                new double[]{
                        value.pressureToeOutside,
                        value.pressureMidOutside,
                        value.pressureToeInside,
                        value.pressureCenter,
                        value.pressureMidInside,
                        value.pressureHeel
                }
        );
    }

    public static SensorSample fromCore(@NonNull OrpheSensorValue value) {
        return new SensorSample(
                DeviceType.core,
                value.sidePosition,
                value.serialNumber,
                value.dataPosition,
                value.startTime,
                value.receivedAt,
                value.accX, value.accY, value.accZ,
                value.gyroX, value.gyroY, value.gyroZ,
                value.quatW, value.quatX, value.quatY, value.quatZ,
                null
        );
    }

    public static SensorSample[] fromInsole(@NonNull OrpheInsoleValue[] values) {
        final SensorSample[] samples = new SensorSample[values.length];
        for (int i = 0; i < values.length; i++) {
            samples[i] = fromInsole(values[i]);
        }
        return samples;
    }

    public static SensorSample[] fromCore(@NonNull OrpheSensorValue[] values) {
        final SensorSample[] samples = new SensorSample[values.length];
        for (int i = 0; i < values.length; i++) {
            samples[i] = fromCore(values[i]);
        }
        return samples;
    }
}
