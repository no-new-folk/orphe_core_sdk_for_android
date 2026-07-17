package io.orphe.orphecoresdk;

import androidx.annotation.NonNull;

/** CORE / INSOLE共通のクオータニオンから算出したオイラー角（ラジアン）。 */
final class OrpheEulerAngles {
    final double yaw;
    final double pitch;
    final double roll;

    private OrpheEulerAngles(final double yaw, final double pitch, final double roll) {
        this.yaw = yaw;
        this.pitch = pitch;
        this.roll = roll;
    }

    /** ORPHE-INSOLE.jsが利用するQuaternion.toEuler()と同じ式で算出します。 */
    @NonNull
    static OrpheEulerAngles fromQuaternion(
            final double quatW,
            final double quatX,
            final double quatY,
            final double quatZ
    ) {
        final double pitchTerm = 2.0 * (quatW * quatY - quatZ * quatX);
        final double roll = Math.atan2(
                2.0 * (quatW * quatX + quatY * quatZ),
                1.0 - 2.0 * (quatX * quatX + quatY * quatY)
        );
        final double pitch = pitchTerm >= 1.0
                ? Math.PI / 2.0
                : (pitchTerm <= -1.0 ? -Math.PI / 2.0 : Math.asin(pitchTerm));
        final double yaw = Math.atan2(
                2.0 * (quatW * quatZ + quatX * quatY),
                1.0 - 2.0 * (quatY * quatY + quatZ * quatZ)
        );

        return new OrpheEulerAngles(yaw, pitch, roll);
    }
}
