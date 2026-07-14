package io.orphe.orphecoresdk;

import androidx.annotation.NonNull;

/** CORE / INSOLEの値モデルに対応する姿勢推定タイムライン生成処理。 */
final class OrpheQuaternionTimelines {
    private OrpheQuaternionTimelines() {
    }

    @NonNull
    static OrpheQuaternionTimeline<OrpheInsoleValue> forInsole() {
        return new OrpheQuaternionTimeline<>(new OrpheQuaternionTimeline.Adapter<OrpheInsoleValue>() {
            @Override
            public int serialNumber(@NonNull final OrpheInsoleValue value) {
                return value.serialNumber;
            }

            @Override
            public double accX(@NonNull final OrpheInsoleValue value) {
                return value.accX;
            }

            @Override
            public double accY(@NonNull final OrpheInsoleValue value) {
                return value.accY;
            }

            @Override
            public double accZ(@NonNull final OrpheInsoleValue value) {
                return value.accZ;
            }

            @Override
            public double gyroX(@NonNull final OrpheInsoleValue value) {
                return value.gyroX;
            }

            @Override
            public double gyroY(@NonNull final OrpheInsoleValue value) {
                return value.gyroY;
            }

            @Override
            public double gyroZ(@NonNull final OrpheInsoleValue value) {
                return value.gyroZ;
            }

            @NonNull
            @Override
            public OrpheInsoleValue withQuaternion(
                    @NonNull final OrpheInsoleValue value,
                    @NonNull final OrpheQuaternion quaternion
            ) {
                return value.withQuaternion(quaternion, value.dataPosition);
            }

            @NonNull
            @Override
            public OrpheInsoleValue[] newArray(final int length) {
                return new OrpheInsoleValue[length];
            }
        });
    }

    @NonNull
    static OrpheQuaternionTimeline<OrpheSensorValue> forCore() {
        return new OrpheQuaternionTimeline<>(new OrpheQuaternionTimeline.Adapter<OrpheSensorValue>() {
            @Override
            public int serialNumber(@NonNull final OrpheSensorValue value) {
                return value.serialNumber;
            }

            @Override
            public double accX(@NonNull final OrpheSensorValue value) {
                return value.accX;
            }

            @Override
            public double accY(@NonNull final OrpheSensorValue value) {
                return value.accY;
            }

            @Override
            public double accZ(@NonNull final OrpheSensorValue value) {
                return value.accZ;
            }

            @Override
            public double gyroX(@NonNull final OrpheSensorValue value) {
                return value.gyroX;
            }

            @Override
            public double gyroY(@NonNull final OrpheSensorValue value) {
                return value.gyroY;
            }

            @Override
            public double gyroZ(@NonNull final OrpheSensorValue value) {
                return value.gyroZ;
            }

            @NonNull
            @Override
            public OrpheSensorValue withQuaternion(
                    @NonNull final OrpheSensorValue value,
                    @NonNull final OrpheQuaternion quaternion
            ) {
                return value.withQuaternion(quaternion);
            }

            @NonNull
            @Override
            public OrpheSensorValue[] newArray(final int length) {
                return new OrpheSensorValue[length];
            }
        });
    }
}
