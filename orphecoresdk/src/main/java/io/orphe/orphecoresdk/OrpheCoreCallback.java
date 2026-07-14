package io.orphe.orphecoresdk;

import android.bluetooth.BluetoothDevice;

import androidx.annotation.NonNull;

import java.util.UUID;

/**
 * ORPHE COREに対するコールバックを指定します。これを定義してOrpheオブジェクトを作成するときに渡します。
 */
public abstract class OrpheCoreCallback implements OrpheCallback {

    /**
     * センサーの値がNotifyで取得されたときのコールバック。
     *
     * @param sensorValues １回のNotifyで送られたセンサー値が入ります。（１度に複数送信されることもあります）
      */
    public void gotSensorValues(OrpheSensorValue[] sensorValues) {

    }

    /**
     * bestEffort受信中に値が更新された直後のコールバック。
     * 既存実装との互換性のため、既定では今回差分を配列版へ転送します。
     *
     * @param update 今回差分とSDK内で再計算・マージ済みの全値
     */
    public void gotSensorValues(@NonNull OrpheSensorValueUpdate update) {
        gotSensorValues(update.getDeltaValues());
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
     * @param meta スキャンされたメタデータが渡されます。
     */
    public void onScan(BluetoothDevice bluetoothDevice, OrpheScanedMeta meta) {
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
