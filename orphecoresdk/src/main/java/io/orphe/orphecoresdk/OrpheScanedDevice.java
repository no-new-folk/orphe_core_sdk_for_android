package io.orphe.orphecoresdk;

import android.bluetooth.BluetoothDevice;

import androidx.annotation.NonNull;

/**
 * ORPHEのスキャンデータを保存するためのデータ
 */
public class OrpheScanedDevice {
    /**
     * ORPHEのスキャンデータを保存するためのデータ
     *
     * @param bluetoothDevice Bluetoothデバイス
     * @param deviceId デバイスID
     */
    public OrpheScanedDevice(
            BluetoothDevice bluetoothDevice,
            String deviceId
    ) {
        this.bluetoothDevice = bluetoothDevice;
        this.deviceId = deviceId;
    }

    /**
     * ORPHEのスキャンデータを保存するためのデータ
     *
     * @param bluetoothDevice Bluetoothデバイス
     */
    public OrpheScanedDevice(
            BluetoothDevice bluetoothDevice
    ) {
        this(bluetoothDevice, null);
    }

    /**
     * BlueToothデバイス
     */
    @NonNull
    public final BluetoothDevice bluetoothDevice;

    /**
     * デバイスID
     */
    public final String deviceId;
}
