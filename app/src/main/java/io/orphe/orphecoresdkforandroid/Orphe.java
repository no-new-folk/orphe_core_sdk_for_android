package io.orphe.orphecoresdkforandroid;

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
import android.os.ParcelUuid;
import android.util.Log;

import androidx.annotation.NonNull;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class Orphe {
    private static final String TAG = Orphe.class.getSimpleName();
    private static final long SCAN_PERIOD = 20000; // スキャンの期間（ミリ秒）
    private final Context mContext;
    private final OrpheCallback mOrpheCallback;
    private BluetoothLeScanner mBluetoothLeScanner;
    private BluetoothDevice mBluetoothDevice;
    private BluetoothGatt mBluetoothGatt;
    private final Handler mHandler = new Handler();
    private boolean mIsScanning = false;
    private int mStepsNumber = 0;
    public final OrpheSidePosition sidePosition;

    /**
     * Orphe constructor
     *
     * @param context       Context.
     * @param sidePosition Side and Position.
     * @param orpheCallback Callback to register.
     */
    public Orphe(@NonNull final Context context, @NonNull final OrpheSidePosition sidePosition, @NonNull final OrpheCallback orpheCallback) {
        mContext = context;
        mOrpheCallback = orpheCallback;
        this.sidePosition = sidePosition;
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
     * Begin BLE connection.
     */
    @SuppressLint("MissingPermission")
    public void begin() {
        if (mIsScanning) {
            mBluetoothLeScanner.stopScan(scanCallback);
            mIsScanning = false;
        }
        if (mBluetoothDevice != null) {
            mOrpheCallback.onScan(mBluetoothDevice);
            connectGatt(mBluetoothDevice);
            return;
        }
        mHandler.postDelayed(() -> {
            if (mIsScanning) {
                mBluetoothLeScanner.stopScan(scanCallback);
                mIsScanning = false;
                mOrpheCallback.onScan(null);
            }
        }, SCAN_PERIOD);

        Log.d(TAG, "begin startScan");
        mIsScanning = true;
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
     * Stop and disconnect GATT connection.
     */
    @SuppressLint("MissingPermission")
    public void stop() {
        if (mBluetoothGatt != null) {
            mBluetoothGatt.disconnect();
        }
        mBluetoothDevice = null;
        if (mIsScanning) {
            mBluetoothLeScanner.stopScan(scanCallback);
            mIsScanning = false;
        }
    }

    @SuppressLint("MissingPermission")
    private void connectGatt(BluetoothDevice device) {
        if (mBluetoothGatt != null && mBluetoothGatt.getDevice().equals(device)) {
            Log.d(TAG, "BluetoothGatt already exists, try to connect");
            mBluetoothGatt.connect();
        } else {
            try {
                // connect to the GATT server on the device
                Log.d(TAG, "connect try to connect:" + mBluetoothDevice.getAddress());
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
                mBluetoothLeScanner.stopScan(scanCallback);
                mIsScanning = false;
                mOrpheCallback.onScan(device);
                connectGatt(device);
            }
        }
    };

    private final BluetoothGattCallback mBluetoothGattCallback = new BluetoothGattCallback() {
        @SuppressLint("MissingPermission")
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                mOrpheCallback.onConnect(gatt.getDevice());
                gatt.discoverServices();
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                mOrpheCallback.onDisconnect(gatt.getDevice());
                mStepsNumber = 0;
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                outputGattServicesToLog(gatt.getServices());
                setCharacteristicNotification(gatt, GattUUIDDefine.UUID_SERVICE_ORPHE_OTHER_SERVICE,
                        GattUUIDDefine.UUID_CHAR_ORPHE_STEP_ANALYSIS, true);
            } else {
                Log.w(TAG, "onServicesDiscovered received: " + status);
            }
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            UUID characteristicUuid = characteristic.getUuid();
            if (characteristicUuid == null) {
                Log.d(TAG, "onCharacteristicWrite UUID is null");
                return;
            }
            Log.d(TAG, "onCharacteristicWrite UUID:" + characteristicUuid
                    + ", status:" + status);
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            super.onDescriptorWrite(gatt, descriptor, status);
            Log.d(TAG, "onDescriptorWrite Desc UUID:" + descriptor.getUuid().toString()
                    + ", status:" + status);
            Log.d(TAG, "                  Char UUID:" + descriptor.getCharacteristic().getUuid().toString());
        }

        @Override
        public void onCharacteristicRead(@NonNull BluetoothGatt gatt, @NonNull BluetoothGattCharacteristic characteristic, @NonNull byte[] value, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                onRead(gatt, characteristic, value);
            }
        }

        @Override
        public void onCharacteristicRead(@NonNull BluetoothGatt gatt, @NonNull BluetoothGattCharacteristic characteristic, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                onRead(gatt, characteristic, characteristic.getValue());
            }
        }

        @Override
        public void onCharacteristicChanged(@NonNull BluetoothGatt gatt, @NonNull BluetoothGattCharacteristic characteristic, @NonNull byte[] value) {
            onRead(gatt, characteristic, value);
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            onRead(gatt, characteristic, characteristic.getValue());
        }

        private void outputGattServicesToLog(final List<BluetoothGattService> gattServices) {
            if (gattServices == null) return;
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

        private void onRead(@NonNull BluetoothGatt gatt, @NonNull BluetoothGattCharacteristic characteristic, @NonNull byte[] value) {
            // 歩容解析
            // if (GattUUIDDefine.UUID_CHAR_ORPHE_STEP_ANALYSIS.equals(characteristic.getUuid())) {
            //    mOrpheCallback.gotData(value);
            // }
            // 生データ
            if (GattUUIDDefine.UUID_CHAR_ORPHE_SENSOR_VALUES.equals(characteristic.getUuid())) {
                // Data
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    try {
                        mOrpheCallback.gotSensorValues(OrpheSensorValue.fromBytes(value, sidePosition));
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }
    };
}
