package io.orphe.orphecoresdk;

import android.bluetooth.BluetoothDevice;

import java.util.UUID;


/**
 * ORPHE COREに対するコールバックを指定します。これを定義してOrpheオブジェクトを作成するときに渡します。
 */
public interface OrpheCallback {


    /**
     * ORPHE COREのDeviceInfoが取得されたときのコールバック。
     * DeviceInfo中にバッテリーの情報が含まれます。
     *
     * @param deviceInfo 取得されたDeviceInfoの値。
     */
    void gotDeviceInfo(DeviceInfoValue deviceInfo);

    /**
     * 現在のシリアルナンバーの値がNotifyで取得されたときのコールバック。
     *
     * @param currentSerialNumber 現在のシリアルナンバー。
     */
    void gotCurrentSerialNumber(int currentSerialNumber);

    /**
     * 特定のシリアルナンバーが見つからなかった場合のコールバック
     *
     * @param serialNumber 見つからなかったシリアルナンバー
     */
    void sensorValueIsNotFound(int serialNumber);

    /**
     * スキャンされたときのコールバック。
     *
     * @param bluetoothDevice スキャンされたBluetoothDeviceが渡されます。
     */
    void onScan(BluetoothDevice bluetoothDevice);

    /**
     * 接続されたときのコールバック。
     *
     * @param bluetoothDevice 接続されたBluetoothDeviceが渡されます。
     */
    void onConnect(BluetoothDevice bluetoothDevice);

    /**
     * 切断されたときのコールバック。
     *
     * @param bluetoothDevice 切断されたBluetoothDeviceが渡されます。
     */
    void onDisconnect(BluetoothDevice bluetoothDevice);

    /**
     * Notifyの取得が開始されたときのコールバック。
     *
     * @param characteristicUuid 対応するNotifyのキャラクタリスティックUUID。
     */
    void onStartNotify(UUID characteristicUuid);

    /**
     * Notifyの取得が終了したときのコールバック。
     *
     * @param characteristicUuid 対応するNotifyのキャラクタリスティックUUID。
     */
    void onStopNotify(UUID characteristicUuid);
}
