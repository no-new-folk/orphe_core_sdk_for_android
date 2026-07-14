package io.orphe.orphecoresdk;

/** SDK内部で使用する[w, x, y, z]順のクオータニオン。 */
final class OrpheQuaternion {
    final double w;
    final double x;
    final double y;
    final double z;

    OrpheQuaternion(final double w, final double x, final double y, final double z) {
        this.w = w;
        this.x = x;
        this.y = y;
        this.z = z;
    }
}
