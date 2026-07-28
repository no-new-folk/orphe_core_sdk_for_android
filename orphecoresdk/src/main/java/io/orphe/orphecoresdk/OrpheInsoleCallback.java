package io.orphe.orphecoresdk;

import android.bluetooth.BluetoothDevice;

import androidx.annotation.NonNull;

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
     * fifo受信中に値が更新された直後のコールバック。
     *
     * <p>{@link OrpheInsoleValueUpdate#getDeltaValues()}で今回の差分、
     * {@link OrpheInsoleValueUpdate#getAllValues()}でこの更新時点の全値を取得できます。
     * 既存実装との互換性のため、既定では差分を{@link #gotInsoleValues(OrpheInsoleValue[])}へ
     * 転送します。</p>
     *
     * @param update 今回の差分とSDK内でマージ済みの全値
     */
    public void gotInsoleValues(@NonNull OrpheInsoleValueUpdate update) {
        gotInsoleValues(update.getDeltaValues());
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
     * スキャンの開始に失敗したときのコールバック。
     *
     * <p>{@link android.bluetooth.le.ScanCallback}のエラーコードがそのまま渡されます。
     * 主なものは1:ALREADY_STARTED、2:APPLICATION_REGISTRATION_FAILED、
     * 3:INTERNAL_ERROR、4:FEATURE_UNSUPPORTED、5:OUT_OF_HARDWARE_RESOURCES、
     * 6:SCANNING_TOO_FREQUENTLY（スキャンの頻度制限）です。</p>
     *
     * @param errorCode スキャン失敗のエラーコード。
     */
    public void onScanFailed(int errorCode) {
    }

    /**
     * 接続したデバイスの左右が、期待していた左右と一致しなかったときのコールバック。
     *
     * <p>アドバタイズから左右が判別できず、左右不明候補として接続した場合に発生しえます。
     * 接続自体は維持されるため、左右を入れ替えて接続し直すかどうかは呼び出し側で判断します。</p>
     *
     * @param bluetoothDevice 接続されたBluetoothDeviceが渡されます。
     * @param expected 期待していた左右。
     * @param actual 接続したデバイスから取得された実際の左右。
     */
    public void onSideMismatch(BluetoothDevice bluetoothDevice, OrpheSide expected, OrpheSide actual) {
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
