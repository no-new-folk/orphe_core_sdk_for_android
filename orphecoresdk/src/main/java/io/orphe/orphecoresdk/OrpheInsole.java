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
import android.bluetooth.le.ScanRecord;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelUuid;
import android.util.Log;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.UUID;

/**
 * ORPHE INSOLEを管理します。
 * インスタンス化したあと[startScan]で対応しているORPHE INSOLEを探し、[connect]で接続します。
 * [disconnect]で切断します。
 */
public class OrpheInsole {
    private static final String TAG = OrpheInsole.class.getSimpleName();
    private static final long SCAN_PERIOD = 20000; // スキャンの期間（ミリ秒）
    private final Context mContext;
    private final OrpheInsoleCallback mOrpheCallback;
    private BluetoothLeScanner mBluetoothLeScanner;
    private BluetoothDevice mBluetoothDevice;
    private BluetoothGatt mBluetoothGatt;
    private final Handler mHandler = new Handler();
    public final OrpheSidePosition sidePosition;

    private OrpheInsoleValue mLatestValue;

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
    public OrpheCoreStatus status() {
        return mStatus;
    }

    @NonNull
    private OrpheCoreStatus mStatus = OrpheCoreStatus.none;

    /**
     * 対応する[BluetoothDevice]を返します。
     *
     * @return 対応する[BluetoothDevice]
     */
    public BluetoothDevice device() {
        return mBluetoothDevice;
    }

    /**
     * 最新の[OrpheInsoleValue]を返します。
     *
     * @return 最新の[OrpheInsoleValue]
     */
    public OrpheInsoleValue getLatestValue() {
        return mLatestValue;
    }

    /**
     * 現在のDeviceInfoを返します。
     *
     * @return 現在のDeviceInfo。
     */
    public DeviceInfoValue deviceInfo() {
        return mDeviceInfo;
    }

    private DeviceInfoValue mDeviceInfo = new DeviceInfoValue(
            OrpheBatteryStatus.unknown,
            OrpheSidePosition.both,
            OrpheLogRecordingMode.stop,
            false,
            OrpheLedBrightness.off,
            60,
            OrpheAccRange.range16,
            OrpheGyroRange.range2000
    );


    /**
     * ORPHE INSOLEを管理します。
     * インスタンス化したあと[startScan]で対応しているORPHE COREを探し、[connect]で接続します。
     * [disconnect]で切断します。
     *
     * @param context       コンテキスト
     * @param orpheCallback コールバック引数
     * @param sidePosition  この[Orphe]に対応する取り付け位置
     * @param accRange      加速度レンジの設定
     * @param gyroRange     ジャイロレンジの設定
     */
    public OrpheInsole(@NonNull final Context context, @NonNull final OrpheInsoleCallback orpheCallback, @NonNull final OrpheSidePosition sidePosition, @NonNull final OrpheAccRange accRange, @NonNull final OrpheGyroRange gyroRange) {
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
     * ORPHE INSOLEを管理します。
     * インスタンス化したあと[startScan]で対応しているORPHE COREを探し、[connect]で接続します。
     * [disconnect]で切断します。
     *
     * @param context       コンテキスト
     * @param orpheCallback コールバック引数
     * @param sidePosition  この[Orphe]に対応する取り付け位置
     */
    public OrpheInsole(@NonNull final Context context, @NonNull final OrpheInsoleCallback orpheCallback, @NonNull final OrpheSidePosition sidePosition) {
        this(context, orpheCallback, sidePosition, OrpheAccRange.range16, OrpheGyroRange.range2000);
    }


    /**
     * ORPHE INSOLEのスキャンを開始します。
     * 見つかった場合は[OrpheInsoleCallback.onScan]に対応する[BluetoothDevice]が渡されます。
     */
    @SuppressLint("MissingPermission")
    public void startScan() {
        if (mStatus == OrpheCoreStatus.disconnecting || mStatus == OrpheCoreStatus.connected || mStatus == OrpheCoreStatus.connecting) {
            return;
        }
        if (mStatus == OrpheCoreStatus.scanned) {
            mBluetoothLeScanner.stopScan(scanCallback);
            mStatus = OrpheCoreStatus.none;
        }
        // if (mBluetoothDevice != null) {
        //    mOrpheCallback.onScan(mBluetoothDevice);
        //    connect(mBluetoothDevice);
        //    return;
        // }
        mHandler.postDelayed(() -> {
            if (mStatus == OrpheCoreStatus.scanned) {
                mBluetoothLeScanner.stopScan(scanCallback);
                mOrpheCallback.onScan(null);
            }
        }, SCAN_PERIOD);

        Log.d(TAG, "begin startScan");
        mStatus = OrpheCoreStatus.scanned;
        // TODO: 暫定的にサービスUUIDによるフィルタはスキップ
        final List<ScanFilter> scanFilters = Arrays.asList(
                //new ScanFilter.Builder()
                //      .setServiceUuid(ParcelUuid.fromString(GattUUIDDefine.UUID_SERVICE_ORPHE_OTHER_SERVICE.toString()))
                //    .build(),
                //new ScanFilter.Builder()
                //      .setServiceUuid(ParcelUuid.fromString(GattUUIDDefine.UUID_SERVICE_ORPHE_INFORMATION.toString()))
                //    .build()
        );
        mBluetoothLeScanner.startScan(scanFilters, new ScanSettings.Builder().build(),
                scanCallback);
    }

    /**
     * ORPHE INSOLEのスキャンを停止します。
     */
    @SuppressLint("MissingPermission")
    public void stopScan() {
        if (mStatus == OrpheCoreStatus.none) {
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
     * ORPHE INSOLEを切断します。
     */
    @SuppressLint("MissingPermission")
    public void disconnect() {
        if (mStatus != OrpheCoreStatus.connected) {
            return;
        }
        mStatus = OrpheCoreStatus.disconnecting;
        if (mBluetoothGatt != null) {
            mBluetoothGatt.disconnect();
        }
        mBluetoothDevice = null;
    }

    /**
     * 接続する[BluetoothDevice]を渡してORPHE INSOLE接続します。
     *
     * @param device [OrpheInsoleCallback.onScan]で渡された[BluetoothDevice]
     */
    @SuppressLint("MissingPermission")
    public void connect(BluetoothDevice device) {
        if (mStatus == OrpheCoreStatus.connected || mStatus == OrpheCoreStatus.connecting || mStatus == OrpheCoreStatus.disconnecting) {
            return;
        }
        mBluetoothLeScanner.stopScan(scanCallback);
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


    /**
     * デバイスの設定情報を読み取ります。
     */
    @SuppressLint("MissingPermission")
    public void getDeviceInfo() {
        if (mStatus != OrpheCoreStatus.connected || mBluetoothGatt == null) {
            return;
        }
        BluetoothGattService service = mBluetoothGatt.getService(GattUUIDDefine.UUID_SERVICE_ORPHE_INFORMATION);
        if (service == null) {
            Log.d(TAG, "Could not get service: " + GattUUIDDefine.UUID_SERVICE_ORPHE_INFORMATION.toString());
            return;
        }
        BluetoothGattCharacteristic characteristic = service.getCharacteristic(GattUUIDDefine.UUID_CHAR_ORPHE_DEVICE_INFORMATION);
        if (characteristic == null) {
            Log.d(TAG, "Could not get characteristic: " + GattUUIDDefine.UUID_CHAR_ORPHE_DEVICE_INFORMATION.toString());
            return;
        }
        mBluetoothGatt.readCharacteristic(characteristic);
    }

    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    @SuppressLint("MissingPermission")
    private void setDeviceInfo(byte[] value) {
        if (mStatus != OrpheCoreStatus.connected || mBluetoothGatt == null) {
            return;
        }
        BluetoothGattService service = mBluetoothGatt.getService(GattUUIDDefine.UUID_SERVICE_ORPHE_INFORMATION);
        if (service == null) {
            Log.d(TAG, "Could not get service: " + GattUUIDDefine.UUID_SERVICE_ORPHE_INFORMATION.toString());
            return;
        }
        BluetoothGattCharacteristic characteristic = service.getCharacteristic(GattUUIDDefine.UUID_CHAR_ORPHE_DEVICE_INFORMATION);
        if (characteristic == null) {
            Log.d(TAG, "Could not get characteristic: " + GattUUIDDefine.UUID_CHAR_ORPHE_DEVICE_INFORMATION.toString());
            return;
        }
        mBluetoothGatt.writeCharacteristic(characteristic, value, BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
    }

    /**
     * デバイスの時間をアプリと同期します。
     */
    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    @SuppressLint("MissingPermission")
    public void syncDateTime(){
        if (mStatus != OrpheCoreStatus.connected || mBluetoothGatt == null) {
            return;
        }
        BluetoothGattService service = mBluetoothGatt.getService(GattUUIDDefine.UUID_SERVICE_ORPHE_INFORMATION);
        if (service == null) {
            Log.d(TAG, "Could not get service: " + GattUUIDDefine.UUID_SERVICE_ORPHE_INFORMATION.toString());
            return;
        }
        BluetoothGattCharacteristic characteristic = service.getCharacteristic(GattUUIDDefine.UUID_CHAR_ORPHE_DATE_TIME);
        if (characteristic == null) {
            Log.d(TAG, "Could not get characteristic: " + GattUUIDDefine.UUID_CHAR_ORPHE_DATE_TIME.toString());
            return;
        }
        final LocalDateTime now = LocalDateTime.now();
        final int res = mBluetoothGatt.writeCharacteristic(characteristic, new byte[]{
                (byte)(now.getYear() - 2000),
                (byte)(now.getMonthValue()),
                (byte)(now.getDayOfMonth()),
                (byte)(now.getHour()),
                (byte)(now.getMinute()),
                (byte)(now.getSecond()),
                0,
                //(byte)Math.round(now.getNano() / 1_000_000 / 10),
        }, BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);

    }


    /**
     * デバイスの時間を取得します。
     */
    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    @SuppressLint("MissingPermission")
    public void getCurrentDateTime(){
        if (mStatus != OrpheCoreStatus.connected || mBluetoothGatt == null) {
            return;
        }
        Log.d(TAG, "SyncTime: ");
        BluetoothGattService service = mBluetoothGatt.getService(GattUUIDDefine.UUID_SERVICE_ORPHE_INFORMATION);
        if (service == null) {
            Log.d(TAG, "Could not get service: " + GattUUIDDefine.UUID_SERVICE_ORPHE_INFORMATION.toString());
            return;
        }
        BluetoothGattCharacteristic characteristic = service.getCharacteristic(GattUUIDDefine.UUID_CHAR_ORPHE_DATE_TIME);
        if (characteristic == null) {
            Log.d(TAG, "Could not get characteristic: " + GattUUIDDefine.UUID_CHAR_ORPHE_DATE_TIME.toString());
            return;
        }
        mBluetoothGatt.readCharacteristic(characteristic);
    }


    /**
     * 現在の生データのシリアルナンバーを取得します。
     */
    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    public void getCurrentSerialNumber() {
        setDeviceInfo(new byte[]{11, 1});
    }

    /**
     * 最新の[OrpheInsoleValue]の取得をリクエストします。
     *
     * @param length 取得する数（あくまで目安）
     */
    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    public void requestLatestInsoleValue(int length) {
        if (mLatestValue == null) {
            getCurrentSerialNumber();
            return;
        }
        final long now = LocalDateTime.now().toInstant(ZoneOffset.UTC).toEpochMilli();
        int serialNumber = mLatestValue.serialNumber;
        if (serialNumber >= 256 * 256 - 1) {
            serialNumber = 0;
        } else {
            serialNumber++;
        }
        if (length > 0) {
            final long startTime = now - length * 20;
            serialNumber = serialNumber + (int) Math.ceil((startTime - mLatestValue.startTime) / 20);
            serialNumber = serialNumber % (256 * 256);
            Log.d(TAG, "Request: " + serialNumber + ", " + length);
            requestInsoleValue(new OrpheValueRequest[]{
                    new OrpheValueRequest(serialNumber, length)
            });
        } else {
            if(now > mLatestValue.startTime){
                final int l = (int) Math.ceil((now - mLatestValue.startTime) / 20);
                Log.d(TAG, "Request: " + serialNumber + ", " + l);
                requestInsoleValue(new OrpheValueRequest[]{
                        new OrpheValueRequest(serialNumber, l)
                });
            } else {
                Log.d(TAG, "Request: " + serialNumber + ", " + 10);
                requestInsoleValue(new OrpheValueRequest[]{
                        new OrpheValueRequest(serialNumber, 10)
                });
            }
        }
    }

    /**
     * 最新の[OrpheInsoleValue]の取得をリクエストします。
     */
    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    public void requestLatestInsoleValue() {
        requestLatestInsoleValue(0);
    }

    /**
     * [OrpheInsoleValue]の取得をリクエストします。
     *
     * @param requests リクエスト情報を渡します。
     */
    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    public void requestInsoleValue(OrpheValueRequest[] requests) {
        if (requests.length < 1) {
            Log.e(TAG, "A minimum of one request is required.");
        } else if (requests.length > 30) {
            Log.e(TAG, "You cannot send more than 30 requests.");
        }
        int i = 0;
        final byte[] byteList = new byte[requests.length * 4 + 2];
        byteList[i] = 11;
        i++;
        byteList[i] = 2;
        i++;
        for (OrpheValueRequest request : requests) {
            final int start = request.startSerialNumber;
            final int length = request.length;
            byteList[i] = (byte) (start >> 8);
            i++;
            byteList[i] = (byte) start;
            i++;
            byteList[i] = (byte) (length >> 8);
            i++;
            byteList[i] = (byte) length;
            i++;
        }
        setDeviceInfo(byteList);
    }

    /**
     * 現在のリクエストをキャンセルします。
     */
    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    public void cancelRequestingSensorData() {
        setDeviceInfo(new byte[]{11, 7});
    }

    /**
     * 生データの蓄積を開始します。
     */
    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    public void startAccumulation() {
        setDeviceInfo(new byte[]{11, 4});
    }

    /**
     * 生データの蓄積を停止します。
     */
    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    public void stopAccumulation() {
        setDeviceInfo(new byte[]{11, 6});
    }

    /**
     * 蓄積された生データをすべてクリアします。
     */
    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    public void clearAccumulation() {
        setDeviceInfo(new byte[]{11, 3});
    }

    private final ScanCallback scanCallback = new ScanCallback() {
        @SuppressLint("MissingPermission")
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            final BluetoothDevice device = result.getDevice();
            final ScanRecord record = result.getScanRecord();
            final byte[] manufacturerData = record.getManufacturerSpecificData(0);
            if (manufacturerData == null) {
                return;
            }
            final String deviceName = device.getName();
            Log.d(TAG, "onScanResult:" + deviceName + " " + bytesToHex(manufacturerData));
            // TODO: 暫定的にManufacturerDataから探す
            if (manufacturerData.length > 4 && manufacturerData[0] == 1 && manufacturerData[1] == 18) {
                mBluetoothDevice = device;
                mOrpheCallback.onScan(device);
                return;
            }
            if (deviceName == null) {
                return;
            }
            if (deviceName.contains(DeviceNameDefine.ORPHE_CORE)) {
                mBluetoothDevice = device;
                mOrpheCallback.onScan(device);
            }
        }
    };


    // TODO: 不要になったら消す
    private static String bytesToHex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : bytes) {
            hexString.append(String.format("%02x", b));
        }
        return hexString.toString();
    }

    private final BluetoothGattCallback mBluetoothGattCallback = new BluetoothGattCallback() {
        @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
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
                Log.d(TAG, "disconnected");
                Log.d(TAG, status().toString());
                mainHandler.post(
                        () -> {
                            mStatus = OrpheCoreStatus.none;
                            mBluetoothDevice = null;
                            mOrpheCallback.onDisconnect(gatt.getDevice());
                            startScan();
                        }
                );
            }
        }

        @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                outputGattServicesToLog(gatt.getServices());
                setCharacteristicNotification(gatt, GattUUIDDefine.UUID_SERVICE_ORPHE_OTHER_SERVICE,
                        GattUUIDDefine.UUID_CHAR_ORPHE_SENSOR_VALUES, true);
                getDeviceInfo();
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

        @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
        @Override
        public void onCharacteristicChanged(@NonNull BluetoothGatt gatt, @NonNull BluetoothGattCharacteristic characteristic, @NonNull byte[] value) {
            onNotified(gatt, characteristic, value);
        }

        @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            onNotified(gatt, characteristic, characteristic.getValue());
        }

        private void outputGattServicesToLog(final List<BluetoothGattService> gattServices) {
            if (gattServices == null) {
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

        @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
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
                Handler handler = new Handler(Looper.getMainLooper());
                handler.postDelayed(() -> {
                    syncDateTime();
                    handler.postDelayed(() -> {
                        getCurrentSerialNumber();
                    }, 500);
                }, 500);
            } else {
                mOrpheCallback.onStopNotify(characteristicUUID);
            }
        }

        @RequiresApi(api = Build.VERSION_CODES.O)
        private void onRead(@NonNull BluetoothGatt gatt, @NonNull BluetoothGattCharacteristic characteristic, @NonNull byte[] value) {
            final Handler mainHandler = new Handler(Looper.getMainLooper());
            // DeviceInfo
            if (GattUUIDDefine.UUID_CHAR_ORPHE_DEVICE_INFORMATION.equals(characteristic.getUuid())) {
                // Data
                mainHandler.post(
                        () -> {
                            try {
                                final DeviceInfoValue deviceInfo = DeviceInfoValue.fromBytes(value);
                                mDeviceInfo = deviceInfo;
                                mOrpheCallback.gotDeviceInfo(deviceInfo);
                            } catch (Exception e) {
                                throw new RuntimeException(e);
                            }
                        }
                );
            }
            // DateTime
            if (GattUUIDDefine.UUID_CHAR_ORPHE_DATE_TIME.equals(characteristic.getUuid())) {
                // Data
                mainHandler.post(
                        () -> {
                            try {
                                Log.d(TAG, "DateTime: " + value[0] + ", " + value[1] + ", " + value[2] + ", " + value[3] + ", " + value[4] + ", " + value[5] + ", " + value[6]);
                            } catch (Exception e) {
                                throw new RuntimeException(e);
                            }
                        }
                );
            }
        }

        @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
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
                                switch ((byte) value[0]) {
                                    case 53:
                                        if ((byte) value[1] == 1) {
                                            final int currentSerialNumber = (int) (((value[2] & 0xFF) << 8) | (value[3] & 0xFF));
                                            mOrpheCallback.gotCurrentSerialNumber(currentSerialNumber);
                                            requestInsoleValue(new OrpheValueRequest[]{new OrpheValueRequest(
                                                    currentSerialNumber, 10
                                            )});
                                        }
                                        break;
                                    case 54:
                                        final OrpheInsoleValue[] values = OrpheInsoleValue.fromBytes(value, sidePosition, accRange, gyroRange);
                                        mOrpheCallback.gotInsoleValues(values);
                                        if (values.length > 0) {
                                            mLatestValue = values[0];
                                        }
                                        break;
                                }
                            } catch (Exception e) {
                                throw new RuntimeException(e);
                            }
                        }
                );
            }
        }
    };
}
