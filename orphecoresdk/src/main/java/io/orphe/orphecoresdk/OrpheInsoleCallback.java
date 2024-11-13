package io.orphe.orphecoresdk;

import android.bluetooth.BluetoothDevice;

import java.util.UUID;

/**
 * ORPHE INSOLEに対するコールバックを指定します。これを定義してOrpheオブジェクトを作成するときに渡します。
 */
public abstract class OrpheInsoleCallback implements OrpheCallback {

    /**
     * インソールの値がNotifyで取得されたときのコールバック。
     *
     * @param insoleValues 1回のNotifyで送られたインソールの値が入ります。（１度に複数送信されることもあります）
      */
    public void gotInsoleValues(OrpheInsoleValue[] insoleValues) {

    }

    /**
     * 現在のシリアルナンバーの値がNotifyで取得されたときのコールバック。
     *
     * @param currentSerialNumber 現在のシリアルナンバー。
     */
    public void gotCurrentSerialNumber(int currentSerialNumber) {

    }

    /**
     * 特定のシリアルナンバーが見つからなかった場合のコールバック
     *
     * @param serialNumber 見つからなかったシリアルナンバー
     */
    public void sensorValueIsNotFound(int serialNumber){

    }

    /**
     * ORPHE COREのDeviceInfoが取得されたときのコールバック。
     * DeviceInfo中にバッテリーの情報が含まれます。
     *
     * @param deviceInfo 取得されたDeviceInfoの値。
     */
    public void gotDeviceInfo(DeviceInfoValue deviceInfo) {

    }

    /**
     * スキャンされたときのコールバック。
     *
     * @param bluetoothDevice スキャンされたBluetoothDeviceが渡されます。
     */
    public void onScan(BluetoothDevice bluetoothDevice) {
    }

    /**
     * 接続されたときのコールバック。
     *
     * @param bluetoothDevice 接続されたBluetoothDeviceが渡されます。
     */
    public void onConnect(BluetoothDevice bluetoothDevice) {
    }

    /**
     * 切断されたときのコールバック。
     *
     * @param bluetoothDevice 切断されたBluetoothDeviceが渡されます。
     */
    public void onDisconnect(BluetoothDevice bluetoothDevice) {

    }

    /**
     * Notifyの取得が開始されたときのコールバック。
     *
     * @param characteristicUuid 対応するNotifyのキャラクタリスティックUUID。
     */
    public void onStartNotify(UUID characteristicUuid) {

    }

    /**
     * Notifyの取得が終了したときのコールバック。
     *
     * @param characteristicUuid 対応するNotifyのキャラクタリスティックUUID。
     */
    public void onStopNotify(UUID characteristicUuid) {

    }
}