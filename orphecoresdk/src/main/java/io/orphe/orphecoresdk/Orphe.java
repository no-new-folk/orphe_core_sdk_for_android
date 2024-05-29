package io.orphe.orphecoresdk;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelUuid;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * ORPHE COREを管理します。
 *
 * インスタンス化したあと[startScan]で対応しているORPHE COREを探し、[connect]で接続します。
 *
 * [disconnect]で切断します。
 */
public class Orphe {
    private static final String TAG = Orphe.class.getSimpleName();
    private static final long SCAN_PERIOD = 20000; // スキャンの期間（ミリ秒）
    private final Context mContext;
    private final OrpheCallback mOrpheCallback;
    private BluetoothLeScanner mBluetoothLeScanner;
    private BluetoothDevice mBluetoothDevice;
    private BluetoothGatt mBluetoothGatt;
    private final Handler mHandler = new Handler();
    public final OrpheSidePosition sidePosition;

    /**
     * 加速度レンジ
     */
    public final OrpheAccRange accRange;

    /**
     * ジャイロレンジ
     */
    public final OrpheGyroRange gyroRange;

    /**
     * 現在の接続ステータスを返します。
     *
     * @return 現在の接続ステータス
     */
    public OrpheCoreStatus status(){
        return mStatus;
    }

    @NonNull
    private OrpheCoreStatus mStatus = OrpheCoreStatus.none;

    /**
     * 対応する[BluetoothDevice]を返します。
     *
     * @return 対応する[BluetoothDevice]
     */
    public BluetoothDevice device() { return mBluetoothDevice; }


    /**
     * ORPHE COREを管理します。
     *
     * インスタンス化したあと[startScan]で対応しているORPHE COREを探し、[connect]で接続します。
     *
     * [disconnect]で切断します。
     *
     * @param context コンテキスト
     * @param orpheCallback コールバック引数
     * @param sidePosition この[Orphe]に対応する取り付け位置
     * @param accRange 加速度レンジの設定
     * @param gyroRange ジャイロレンジの設定
     */
    public Orphe(@NonNull final Context context, @NonNull final OrpheCallback orpheCallback, @NonNull final OrpheSidePosition sidePosition, @NonNull final OrpheAccRange accRange, @NonNull final OrpheGyroRange gyroRange) {
        mContext = context;
        mOrpheCallback = orpheCallback;
        this.sidePosition = sidePosition;
        this.accRange = accRange;
        this.gyroRange = gyroRange;
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            Log.e(TAG, "Unable to obtain a BluetoothAdapter.");
        } else {
            mBluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();
            if (mBluetoothLeScanner == null) {
                Log.e(TAG, "Unable to obtain a BluetoothLeScanner.");
            }
        }
        mBluetoothDevice = null;
    }

    /**
     * ORPHE COREを管理します。
     *
     * インスタンス化したあと[startScan]で対応しているORPHE COREを探し、[connect]で接続します。
     *
     * [disconnect]で切断します。
     *
     * @param context コンテキスト
     * @param orpheCallback コールバック引数
     * @param sidePosition この[Orphe]に対応する取り付け位置
     */
    public Orphe(@NonNull final Context context, @NonNull final OrpheCallback orpheCallback, @NonNull final OrpheSidePosition sidePosition) {
        this(context, orpheCallback, sidePosition, OrpheAccRange.range16, OrpheGyroRange.range2000);
    }

    /**
     * ORPHE COREのスキャンを開始します。
     *
     * 見つかった場合は[OrpheCallback.onScan]に対応する[BluetoothDevice]が渡されます。
     */
    @SuppressLint("MissingPermission")
    public void startScan() {
        if(mStatus == OrpheCoreStatus.disconnecting || mStatus == OrpheCoreStatus.connected || mStatus == OrpheCoreStatus.connecting){
            return;
        }
        if (mStatus == OrpheCoreStatus.scanned) {
            mBluetoothLeScanner.stopScan(scanCallback);
            mStatus = OrpheCoreStatus.none;
        }
        if (mBluetoothDevice != null) {
            mOrpheCallback.onScan(mBluetoothDevice);
            connect(mBluetoothDevice);
            return;
        }
        mHandler.postDelayed(() -> {
            if (mStatus == OrpheCoreStatus.scanned) {
                mBluetoothLeScanner.stopScan(scanCallback);
                mOrpheCallback.onScan(null);
            }
        }, SCAN_PERIOD);

        Log.d(TAG, "begin startScan");
        mStatus = OrpheCoreStatus.scanned;
        List<ScanFilter> scanFilters = Arrays.asList(
                new ScanFilter.Builder()
                        .setServiceUuid(ParcelUuid.fromString(
                                "db1b7aca-cda5-4453-a49b-33a53d3f0833"))
                        .build(),
                new ScanFilter.Builder()
                        .setServiceUuid(ParcelUuid.fromString(
                                "01a9d6b5-ff6e-444a-b266-0be75e85c064"))
                        .build());
        mBluetoothLeScanner.startScan(scanFilters, new ScanSettings.Builder().build(),
                scanCallback);
    }

    /**
     * ORPHE COREのスキャンを停止します。
     */
    @SuppressLint("MissingPermission")
    public void stopScan() {
        if(mStatus == OrpheCoreStatus.none){
            return;
        }
        if (mBluetoothGatt != null) {
            mBluetoothGatt.disconnect();
        }
        mBluetoothDevice = null;
        if (mStatus == OrpheCoreStatus.scanned) {
            mBluetoothLeScanner.stopScan(scanCallback);
            mStatus = OrpheCoreStatus.none;
        }
    }


    /**
     * ORPHE COREを切断します。
     */
    @SuppressLint("MissingPermission")
    public void disconnect() {
        if(mStatus != OrpheCoreStatus.connected){
            return;
        }
        mStatus = OrpheCoreStatus.disconnecting;
        if (mBluetoothGatt != null) {
            mBluetoothGatt.disconnect();
        }
        mBluetoothDevice = null;
    }

    /**
     * 接続する[BluetoothDevice]を渡してORPHE CORE接続します。
     *
     * @param device [OrpheCallback.onScan]で渡された[BluetoothDevice]
     */
    @SuppressLint("MissingPermission")
    public void connect(BluetoothDevice device) {
        if(mStatus == OrpheCoreStatus.connected || mStatus == OrpheCoreStatus.connecting || mStatus == OrpheCoreStatus.disconnecting){
            return;
        }
        if(mStatus == OrpheCoreStatus.scanned) {
            mBluetoothLeScanner.stopScan(scanCallback);
        }
        if (mBluetoothGatt != null && mBluetoothGatt.getDevice().equals(device)) {
            Log.d(TAG, "BluetoothGatt already exists, try to connect");
            mStatus = OrpheCoreStatus.connecting;
            mBluetoothGatt.connect();
        } else {
            try {
                // connect to the GATT server on the device
                Log.d(TAG, "connect try to connect:" + mBluetoothDevice.getAddress());
                mStatus = OrpheCoreStatus.connecting;
                mBluetoothGatt = device.connectGatt(mContext, true, mBluetoothGattCallback);
            } catch (IllegalArgumentException e) {
                Log.w(TAG, "Device not found with provided address.");
            }
        }
    }

    private final ScanCallback scanCallback = new ScanCallback() {
        @SuppressLint("MissingPermission")
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            final BluetoothDevice device = result.getDevice();
            Log.d(TAG, "onScanResult:" + device.getName());
            if (device.getName().contains(DeviceNameDefine.ORPHE_CORE)) {
                mBluetoothDevice = device;
                mOrpheCallback.onScan(device);
            }
        }
    };

    private final BluetoothGattCallback mBluetoothGattCallback = new BluetoothGattCallback() {
        @SuppressLint("MissingPermission")
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            Log.d(TAG, "onConnectionStateChange:" + status + " " + newState);
            final Handler mainHandler = new Handler(Looper.getMainLooper());
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.d(TAG, "connected");
                gatt.discoverServices();
                mainHandler.post(
                    () -> {
                        mStatus = OrpheCoreStatus.connected;
                        mOrpheCallback.onConnect(gatt.getDevice());
                    }
                );
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                mainHandler.post(
                        () -> {
                            mStatus = OrpheCoreStatus.none;
                            mOrpheCallback.onDisconnect(gatt.getDevice());
                            mBluetoothLeScanner.startScan(scanCallback);
                        }
                );
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                outputGattServicesToLog(gatt.getServices());
                setCharacteristicNotification(gatt, GattUUIDDefine.UUID_SERVICE_ORPHE_OTHER_SERVICE,
                        GattUUIDDefine.UUID_CHAR_ORPHE_SENSOR_VALUES, true);
            } else {
                Log.w(TAG, "onServicesDiscovered received: " + status);
            }
        }

        // @Override
        // public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
        //     UUID characteristicUuid = characteristic.getUuid();
        //     if (characteristicUuid == GattUUIDDefine.UUID_CHAR_ORPHE_DEVICE_INFORMATION) {
        //         Log.d(TAG, "onCharacteristicWrite UUID:" + characteristicUuid
        //                 + ", status:" + status);
        //     } else {
        //         Log.d(TAG, "onCharacteristicWrite UUID is null");
        //         return;
        //     }
        // }

        // @Override
        // public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
        //     super.onDescriptorWrite(gatt, descriptor, status);
        //     Log.d(TAG, "onDescriptorWrite Desc UUID:" + descriptor.getUuid().toString()
        //             + ", status:" + status);
        //     Log.d(TAG, "                  Char UUID:" + descriptor.getCharacteristic().getUuid().toString());
        // }

        @RequiresApi(api = Build.VERSION_CODES.O)
        @Override
        public void onCharacteristicRead(@NonNull BluetoothGatt gatt, @NonNull BluetoothGattCharacteristic characteristic, @NonNull byte[] value, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                onRead(gatt, characteristic, value);
            }
        }

        @RequiresApi(api = Build.VERSION_CODES.O)
        @Override
        public void onCharacteristicRead(@NonNull BluetoothGatt gatt, @NonNull BluetoothGattCharacteristic characteristic, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                onRead(gatt, characteristic, characteristic.getValue());
            }
        }

        @RequiresApi(api = Build.VERSION_CODES.O)
        @Override
        public void onCharacteristicChanged(@NonNull BluetoothGatt gatt, @NonNull BluetoothGattCharacteristic characteristic, @NonNull byte[] value) {
            onNotified(gatt, characteristic, value);
        }

        @RequiresApi(api = Build.VERSION_CODES.O)
        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            onNotified(gatt, characteristic, characteristic.getValue());
        }

        private void outputGattServicesToLog(final List<BluetoothGattService> gattServices) {
            if (gattServices == null){
                return;
            }
            if (!gattServices.isEmpty()) {
                Log.d(TAG, "Services:");
                for (BluetoothGattService gattService : gattServices) {
                    Log.d(TAG, gattService.getUuid().toString());
                    List<BluetoothGattCharacteristic> gattCharacteristics
                            = gattService.getCharacteristics();
                    if (!gattCharacteristics.isEmpty()) {
                        Log.d(TAG, "    Characteristics:");
                        for (BluetoothGattCharacteristic characteristic : gattCharacteristics) {
                            Log.d(TAG, "    " + characteristic.getUuid().toString());
                            final byte[] data = characteristic.getValue();
                            List<BluetoothGattDescriptor> descriptors
                                    = characteristic.getDescriptors();
                            if (!descriptors.isEmpty()) {
                                Log.d(TAG, "        Descriptors:");
                                for (BluetoothGattDescriptor descriptor : descriptors) {
                                    Log.d(TAG, "        " + descriptor.getUuid().toString());
                                }
                            }
                        }
                    }
                }
            }
        }

        private void setCharacteristicNotification(@NonNull BluetoothGatt gatt,
                                                   UUID serviceUUID, UUID characteristicUUID, boolean enable) {
            Log.d(TAG, "setCharacteristicNotification");
            BluetoothGattService service = gatt.getService(serviceUUID);
            if (service == null) {
                Log.d(TAG, "Could not get service: " + serviceUUID.toString());
                return;
            }
            BluetoothGattCharacteristic characteristic = service.getCharacteristic(characteristicUUID);
            if (characteristic == null) {
                Log.d(TAG, "Could not get characteristic: " + characteristicUUID.toString());
                return;
            }
            int properties = characteristic.getProperties();
            if ((properties & BluetoothGattCharacteristic.PROPERTY_NOTIFY) == 0) {
                Log.d(TAG, "Characteristic does not support notification");
                return;
            }
            Log.d(TAG, "setCharacteristicNotification to " + enable);
            @SuppressLint("MissingPermission") boolean result = gatt.setCharacteristicNotification(characteristic, enable);
            Log.d(TAG, "setCharacteristicNotification " + (result ? "success" : "fail"));
            BluetoothGattDescriptor descriptor = characteristic.getDescriptor(GattUUIDDefine.UUID_DESC_CLIENT_CHAR_CONFIG);
            if (descriptor == null) {
                Log.d(TAG, "could not get descriptor:" + GattUUIDDefine.UUID_DESC_CLIENT_CHAR_CONFIG.toString());
                return;
            }
            boolean setResult = descriptor.setValue(enable ?
                    BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                    : BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);
            Log.d(TAG, "descriptor setresult:" + setResult);
            @SuppressLint("MissingPermission") boolean writeResult = gatt.writeDescriptor(descriptor);
            Log.d(TAG, "descriptor writeresult:" + writeResult);
            if (enable) {
                mOrpheCallback.onStartNotify(characteristicUUID);
            } else {
                mOrpheCallback.onStopNotify(characteristicUUID);
            }
        }
        @RequiresApi(api = Build.VERSION_CODES.O)
        private void onRead(@NonNull BluetoothGatt gatt, @NonNull BluetoothGattCharacteristic characteristic, @NonNull byte[] value) {

        }

        @RequiresApi(api = Build.VERSION_CODES.O)
        private void onNotified(@NonNull BluetoothGatt gatt, @NonNull BluetoothGattCharacteristic characteristic, @NonNull byte[] value) {
            final Handler mainHandler = new Handler(Looper.getMainLooper());
            // 歩容解析
            // if (GattUUIDDefine.UUID_CHAR_ORPHE_STEP_ANALYSIS.equals(characteristic.getUuid())) {
            //    mOrpheCallback.gotData(value);
            // }
            // 生データ
            if (GattUUIDDefine.UUID_CHAR_ORPHE_SENSOR_VALUES.equals(characteristic.getUuid())) {
                // Data
                mainHandler.post(
                        () -> {
                            try {
                                mOrpheCallback.gotSensorValues(OrpheSensorValue.fromBytes(value, sidePosition, accRange, gyroRange));
                            } catch (Exception e) {
                                throw new RuntimeException(e);
                            }
                        }
                );
            }
        }
    };
}
