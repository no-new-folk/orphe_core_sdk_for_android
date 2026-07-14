package io.orphe.orphecoresdk;

import androidx.annotation.NonNull;

/** BLEパケットの基本数値型を復号します。 */
final class OrpheByteParser {
    private OrpheByteParser() {
    }

    /** big-endianの2バイトをsigned 16bitとして返します。 */
    static int getInt16BigEndian(@NonNull final byte[] data, final int index) {
        return (short) (((data[index] & 0xFF) << 8) | (data[index + 1] & 0xFF));
    }
}
