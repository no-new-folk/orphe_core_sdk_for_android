package io.orphe.orphecoresdk;

/**
 * ORPHECOREのデバイスの名前の定義
 */
@SuppressWarnings("unused")
public class DeviceNameDefine {
    /**
     * ORPHECOREのデバイスの名前のプレフィックス定義
     */
    public static final String ORPHE_CORE = "CR-";

    /**
     * ORPHE INSOLEのデバイスの名前のプレフィックス定義。
     * 世代が進んでも判定できるように"INS0"ではなく"INS"で前方一致します。
     */
    public static final String ORPHE_INSOLE = "INS";
}
