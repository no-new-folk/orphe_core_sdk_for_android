package io.orphe.orphecoresdk;

/**
 * ORPHEのスキャンデータを保存するためのデータ
 */
public class OrpheScanedMeta {
    /**
     * ORPHEのスキャンデータを保存するためのデータ
     *
     * @param deviceId デバイスID
     */
    public OrpheScanedMeta(
            String deviceId
    ) {
        this.deviceId = deviceId;
    }

    /**
     * ORPHEのスキャンデータを保存するためのデータ
     */
    public OrpheScanedMeta(
    ) {
        this(null);
    }


    /**
     * デバイスID
     */
    public final String deviceId;
}
