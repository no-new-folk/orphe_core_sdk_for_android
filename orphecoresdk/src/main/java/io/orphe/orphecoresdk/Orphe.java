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

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * ORPHE COREを管理します。
 * インスタンス化したあと[startScan]で対応しているORPHE COREを探し、[connect]で接続します。
 * [disconnect]で切断します。
 */
public class Orphe {
    private static final String TAG = Orphe.class.getSimpleName();
    private static final long SCAN_PERIOD = 20000; // スキャンの期間（ミリ秒）
    private static final int SYNC_DATE_TIME_TRY_COUNT = 3;
    private static final long SYNC_DATE_TIME_WAIT_MS = 100;
    private static final long SENSOR_COMMAND_INTERVAL_MS = 100L;
    private final Context mContext;
    private final OrpheCoreCallback mOrpheCallback;
    private BluetoothLeScanner mBluetoothLeScanner;
    private BluetoothDevice mBluetoothDevice;
    private BluetoothGatt mBluetoothGatt;
    private final Handler mHandler = new Handler();
    public final OrpheSidePosition sidePosition;

    private OrpheSensorValue mLatestValue;

    private int mLatestSerialNumber;
    private LocalDateTime mLatestSerialNumberTime;
    private boolean mSyncDateTimeInProgress;
    private int mSyncDateTimeReadCount;
    private long mSyncDateTimeTotalRttMillis;
    private long mSyncDateTimeReadStartMillis;
    private BluetoothGattCharacteristic mSyncDateTimeCharacteristic;

    private boolean mDebugMode;
    private OrpheCoreSensorConfig mSensorConfig;
    private OrpheBestEffortRequester<OrpheSensorValue[]> mBestEffortRequester;
    private OrpheBestEffortInitializer mBestEffortInitializer;
    private boolean mRequestLoopStarted;
    private boolean mClosed;
    private final Runnable mRequestLoopRunnable = () -> {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestLatestSensorValueForBestEffortMode();
        }
    };
    private final Runnable mBestEffortInitializationRunnable = new Runnable() {
        @Override
        public void run() {
            if (mBestEffortInitializer == null || !mBestEffortInitializer.isRunning()) {
                return;
            }
            mBestEffortInitializer.tick(System.currentTimeMillis());
            mHandler.postDelayed(this, SENSOR_COMMAND_INTERVAL_MS);
        }
    };

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
     * 最新の[OrpheSensorValue]を返します。
     *
     * @return 最新の[OrpheSensorValue]
     */
    public OrpheSensorValue getLatestValue() {
        return mLatestValue;
    }

    /**
     * 最新のシリアルナンバーを取得します。
     *
     * @return 最新のシリアルナンバー
     */
    public int getLatestSerialNumber() { return mLatestSerialNumber; }

    /**
     * 最新のシリアルナンバーを取得した日時を取得します。
     *
     * @return 最新のシリアルナンバーを取得した日時
     */
    public LocalDateTime getLatestSerialNumberTime() { return mLatestSerialNumberTime; }

    /**
     * 現在のDeviceInfoを返します。
     *
     * @return 現在のDeviceInfo。
     */
    public DeviceInfoValue deviceInfo() { return mDeviceInfo; }

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
     * ORPHE COREを管理します。
     * インスタンス化したあと[startScan]で対応しているORPHE COREを探し、[connect]で接続します。
     * [disconnect]で切断します。
     *
     * @param context コンテキスト
     * @param orpheCallback コールバック引数
     * @param sidePosition この[Orphe]に対応する取り付け位置
     * @param accRange 加速度レンジの設定
     * @param gyroRange ジャイロレンジの設定
     * @param debugMode デバッグモード
     */
    public Orphe(@NonNull final Context context, @NonNull final OrpheCoreCallback orpheCallback, @NonNull final OrpheSidePosition sidePosition, @NonNull final OrpheAccRange accRange, @NonNull final OrpheGyroRange gyroRange, boolean debugMode) {
        this(context, orpheCallback, sidePosition, accRange, gyroRange, debugMode, OrpheCoreSensorConfig.DEFAULT);
    }

    public Orphe(
            @NonNull final Context context,
            @NonNull final OrpheCoreCallback orpheCallback,
            @NonNull final OrpheSidePosition sidePosition,
            @NonNull final OrpheAccRange accRange,
            @NonNull final OrpheGyroRange gyroRange,
            boolean debugMode,
            @NonNull final OrpheCoreSensorConfig sensorConfig
    ) {
        mContext = context;
        mOrpheCallback = orpheCallback;
        this.sidePosition = sidePosition;
        this.accRange = accRange;
        this.gyroRange = gyroRange;
        mDebugMode = debugMode;
        mSensorConfig = sensorConfig;
        createBestEffortInitializer();
        createBestEffortRequester();
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

    public Orphe(
            @NonNull final Context context,
            @NonNull final OrpheCoreCallback orpheCallback,
            @NonNull final OrpheSidePosition sidePosition,
            @NonNull final OrpheCoreSensorConfig sensorConfig
    ) {
        this(
                context,
                orpheCallback,
                sidePosition,
                OrpheAccRange.range16,
                OrpheGyroRange.range2000,
                false,
                sensorConfig
        );
    }

    /** センサー値受信設定を変更します。接続中の場合は直ちに反映します。 */
    public void setSensorConfig(@NonNull final OrpheCoreSensorConfig sensorConfig) {
        final OrpheSensorReceiveMode previousMode = mSensorConfig.receiveMode;
        stopRequestLoop();
        mSensorConfig = sensorConfig;
        createBestEffortRequester();
        if (mStatus == OrpheCoreStatus.connected && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            cancelRequestingSensorDataInternal();
            if (previousMode != OrpheSensorReceiveMode.realtime
                    && sensorConfig.receiveMode == OrpheSensorReceiveMode.realtime) {
                stopAccumulation();
            }
            applySensorConfigAfterNotificationStarted();
        }
    }

    /**
     * ORPHE COREを管理します。
     * インスタンス化したあと[startScan]で対応しているORPHE COREを探し、[connect]で接続します。
     * [disconnect]で切断します。
     *
     * @param context コンテキスト
     * @param orpheCallback コールバック引数
     * @param sidePosition この[Orphe]に対応する取り付け位置
     * @param accRange 加速度レンジの設定
     * @param gyroRange ジャイロレンジの設定
     */
    public Orphe(@NonNull final Context context, @NonNull final OrpheCoreCallback orpheCallback, @NonNull final OrpheSidePosition sidePosition, @NonNull final OrpheAccRange accRange, @NonNull final OrpheGyroRange gyroRange) {
        this(context, orpheCallback, sidePosition, accRange, gyroRange, false);
    }

    /**
     * ORPHE COREを管理します。
     * インスタンス化したあと[startScan]で対応しているORPHE COREを探し、[connect]で接続します。
     * [disconnect]で切断します。
     *
     * @param context コンテキスト
     * @param orpheCallback コールバック引数
     * @param sidePosition この[Orphe]に対応する取り付け位置
     */
    public Orphe(@NonNull final Context context, @NonNull final OrpheCoreCallback orpheCallback, @NonNull final OrpheSidePosition sidePosition) {
        this(context, orpheCallback, sidePosition, OrpheAccRange.range16, OrpheGyroRange.range2000);
    }

    /**
     * ORPHE COREのスキャンを開始します。
     * 見つかった場合は[OrpheCallback.onScan]に対応する[BluetoothDevice]が渡されます。
     */
    @SuppressLint("MissingPermission")
    public void startScan() {
        if (mClosed || mBluetoothLeScanner == null) {
            return;
        }
        if(mStatus == OrpheCoreStatus.disconnecting || mStatus == OrpheCoreStatus.connected || mStatus == OrpheCoreStatus.connecting){
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
                mStatus = OrpheCoreStatus.none;
                mOrpheCallback.onScan(null, null);
            }
        }, SCAN_PERIOD);

        Log.d(TAG, "begin startScan");
        mStatus = OrpheCoreStatus.scanned;
        final List<ScanFilter> scanFilters = Arrays.asList(
                new ScanFilter.Builder()
                        .setServiceUuid(ParcelUuid.fromString(GattUUIDDefine.UUID_SERVICE_ORPHE_OTHER_SERVICE.toString()))
                        .build(),
                new ScanFilter.Builder()
                        .setServiceUuid(ParcelUuid.fromString(GattUUIDDefine.UUID_SERVICE_ORPHE_INFORMATION.toString()))
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && mSensorConfig.receiveMode != OrpheSensorReceiveMode.realtime) {
            stopAccumulation();
        }
        stopRequestLoop();
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
        if (mClosed || device == null || mBluetoothLeScanner == null) {
            return;
        }
        if(mStatus == OrpheCoreStatus.connected || mStatus == OrpheCoreStatus.connecting || mStatus == OrpheCoreStatus.disconnecting){
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

    /** BLEリソースとSDK内の遅延処理を解放します。このインスタンスは再利用できません。 */
    @SuppressLint("MissingPermission")
    public void close() {
        mClosed = true;
        mHandler.removeCallbacksAndMessages(null);
        stopRequestLoop();
        if (mBluetoothLeScanner != null && mStatus == OrpheCoreStatus.scanned) {
            mBluetoothLeScanner.stopScan(scanCallback);
        }
        if (mBluetoothGatt != null) {
            mBluetoothGatt.disconnect();
            mBluetoothGatt.close();
            mBluetoothGatt = null;
        }
        mBluetoothDevice = null;
        mStatus = OrpheCoreStatus.none;
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
        mSyncDateTimeInProgress = true;
        mSyncDateTimeReadCount = 0;
        mSyncDateTimeTotalRttMillis = 0;
        mSyncDateTimeCharacteristic = characteristic;
        readDateTimeForSync();
    }

    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    @SuppressLint("MissingPermission")
    private void readDateTimeForSync() {
        if (!mSyncDateTimeInProgress || mStatus != OrpheCoreStatus.connected || mBluetoothGatt == null || mSyncDateTimeCharacteristic == null) {
            finishSyncDateTime();
            return;
        }
        mSyncDateTimeReadStartMillis = System.currentTimeMillis();
        if (!mBluetoothGatt.readCharacteristic(mSyncDateTimeCharacteristic)) {
            Log.d(TAG, "Could not read characteristic: " + GattUUIDDefine.UUID_CHAR_ORPHE_DATE_TIME.toString());
            finishSyncDateTime();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    private void handleSyncDateTimeRead() {
        if (!mSyncDateTimeInProgress) {
            return;
        }
        mSyncDateTimeTotalRttMillis += Math.max(0, System.currentTimeMillis() - mSyncDateTimeReadStartMillis);
        mSyncDateTimeReadCount++;
        if (mSyncDateTimeReadCount >= SYNC_DATE_TIME_TRY_COUNT) {
            finishSyncDateTime();
            return;
        }
        new Handler(Looper.getMainLooper()).postDelayed(this::readDateTimeForSync, SYNC_DATE_TIME_WAIT_MS);
    }

    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    @SuppressLint("MissingPermission")
    private void finishSyncDateTime() {
        final BluetoothGattCharacteristic characteristic = mSyncDateTimeCharacteristic;
        final long offsetMillis = mSyncDateTimeReadCount > 0
                ? mSyncDateTimeTotalRttMillis / (mSyncDateTimeReadCount * 2L)
                : 0;
        mSyncDateTimeInProgress = false;
        mSyncDateTimeReadCount = 0;
        mSyncDateTimeTotalRttMillis = 0;
        mSyncDateTimeReadStartMillis = 0;
        mSyncDateTimeCharacteristic = null;
        if (mStatus != OrpheCoreStatus.connected || mBluetoothGatt == null || characteristic == null) {
            return;
        }
        final LocalDateTime now = LocalDateTime.now().plusNanos(offsetMillis * 1_000_000L);
        mBluetoothGatt.writeCharacteristic(characteristic, new byte[]{
                (byte)(now.getYear() - 2000),
                (byte)(now.getMonthValue()),
                (byte)(now.getDayOfMonth()),
                (byte)(now.getHour()),
                (byte)(now.getMinute()),
                (byte)(now.getSecond()),
                (byte)(now.getNano() / 1_000_000 / 10),
        }, BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
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
     * センサーの取得モードを設定します。
     *
     * @param mode センサーの取得モード
     */
    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    public void setSensorRequestMode(OrpheSensorRequestMode mode) {
        if (mode == null) {
            return;
        }
        setDeviceInfo(new byte[]{13, (byte) mode.value});
    }

    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    private void applySensorConfigAfterNotificationStarted() {
        mHandler.postDelayed(() -> {
            if (mClosed || mStatus != OrpheCoreStatus.connected) {
                return;
            }
            if (mSensorConfig.receiveMode == OrpheSensorReceiveMode.realtime) {
                setSensorRequestMode(OrpheSensorRequestMode.realtime);
                stopRequestLoop();
                return;
            }
            setSensorRequestMode(OrpheSensorRequestMode.request);
            mHandler.postDelayed(() -> {
                if (mClosed
                        || mStatus != OrpheCoreStatus.connected
                        || mSensorConfig.receiveMode == OrpheSensorReceiveMode.realtime) {
                    return;
                }
                if (mSensorConfig.receiveMode == OrpheSensorReceiveMode.bestEffort) {
                    startBestEffortInitialization();
                } else {
                    startAccumulation();
                }
            }, SENSOR_COMMAND_INTERVAL_MS);
        }, mSensorConfig.modeChangeDelayMillis);
    }

    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    private void startRequestLoop() {
        if (mRequestLoopStarted
                || mClosed
                || mStatus != OrpheCoreStatus.connected
                || mSensorConfig.receiveMode != OrpheSensorReceiveMode.bestEffort) {
            return;
        }
        mRequestLoopStarted = true;
        mBestEffortRequester.start();
        requestLatestSensorValueForBestEffortMode();
    }

    private void stopRequestLoop() {
        mRequestLoopStarted = false;
        mHandler.removeCallbacks(mRequestLoopRunnable);
        mHandler.removeCallbacks(mBestEffortInitializationRunnable);
        if (mBestEffortInitializer != null) {
            mBestEffortInitializer.stop();
        }
        if (mBestEffortRequester != null) {
            mBestEffortRequester.stop();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    private void requestLatestSensorValueForBestEffortMode() {
        if (!mRequestLoopStarted
                || mStatus != OrpheCoreStatus.connected
                || mSensorConfig.receiveMode != OrpheSensorReceiveMode.bestEffort) {
            return;
        }
        final long nowMillis = System.currentTimeMillis();
        mBestEffortRequester.tick(nowMillis);
        mHandler.postDelayed(
                mRequestLoopRunnable,
                mBestEffortRequester.nextTickDelayMillis(
                        nowMillis,
                        mSensorConfig.bestEffortConfig.requestIntervalMillis
                )
        );
    }

    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    private void startBestEffortInitialization() {
        mHandler.removeCallbacks(mBestEffortInitializationRunnable);
        mBestEffortInitializer.start(System.currentTimeMillis());
        mHandler.postDelayed(
                mBestEffortInitializationRunnable,
                SENSOR_COMMAND_INTERVAL_MS
        );
    }

    private void createBestEffortInitializer() {
        mBestEffortInitializer = new OrpheBestEffortInitializer(
                new OrpheBestEffortInitializer.Listener() {
                    @Override
                    public void onCommand(int command) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            setDeviceInfo(new byte[]{11, (byte) command});
                        }
                    }

                    @Override
                    public void onComplete() {
                        mHandler.removeCallbacks(mBestEffortInitializationRunnable);
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            startRequestLoop();
                        }
                    }

                    @Override
                    public void onFailure(int command) {
                        mHandler.removeCallbacks(mBestEffortInitializationRunnable);
                        Log.e(TAG, "Best Effort initialization failed. command=" + command);
                    }
                }
        );
    }

    private void createBestEffortRequester() {
        mBestEffortRequester = new OrpheBestEffortRequester<>(
                40L,
                mSensorConfig.bestEffortConfig,
                new OrpheBestEffortRequester.Listener<OrpheSensorValue[]>() {
                    @Override
                    public void onCurrentStateRequest() {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            setDeviceInfo(new byte[]{11, 1});
                        }
                    }

                    @Override
                    public void onRequest(@NonNull OrpheValueRequest[] requests) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            writeSensorValueRequest(requests);
                        }
                    }

                    @Override
                    public void onValue(@NonNull OrpheSensorValue[] values) {
                        mOrpheCallback.gotSensorValues(values);
                    }

                    @Override
                    public void onMissing(int serialNumber) {
                        mOrpheCallback.sensorValueIsNotFound(serialNumber);
                    }
                }
        );
    }
    
    /**
     * 現在の生データのシリアルナンバーを取得します。
     */
    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    public void getCurrentSerialNumber() {
        setDeviceInfo(new byte[]{11, 1});
    }

    /**
     * 最新の[OrpheSensorValue]の取得をリクエストします。
     *
     * @param length 取得する数（あくまで目安）
     */
    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    public void requestLatestSensorValue(int length) {
        if (!canSendManualRequest()) {
            return;
        }
        if (mLatestSerialNumberTime == null) {
            getCurrentSerialNumber();
            return;
        }
        final long now = LocalDateTime.now().toInstant(ZoneOffset.UTC).toEpochMilli();
        final long prev = mLatestSerialNumberTime.toInstant(ZoneOffset.UTC).toEpochMilli();
        int serialNumber = mLatestSerialNumber;
        if (serialNumber >= 256 * 256 - 1) {
            serialNumber = 0;
        } else {
            serialNumber++;
        }
        if (length > 0) {
            final long startTime = now - length * 40;
            serialNumber = serialNumber + (int) Math.ceil((startTime - prev) / 40);
            serialNumber = OrpheBestEffortRequester.normalizeSerialNumber(serialNumber);
            requestSensorValue(new OrpheValueRequest[]{
                    new OrpheValueRequest(serialNumber, length)
            });
        } else {
            if(now > prev){
                final int l = (int) Math.ceil((now - prev) / 40);
                requestSensorValue(new OrpheValueRequest[]{
                        new OrpheValueRequest(serialNumber, l)
                });
            } else {
                requestSensorValue(new OrpheValueRequest[]{
                        new OrpheValueRequest(serialNumber, 40)
                });
            }
        }
    }

    /**
     * 最新の[OrpheSensorValue]の取得をリクエストします。
     */
    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    public void requestLatestSensorValue() {
        requestLatestSensorValue(100);
    }

    /**
     * [OrpheSensorValue]の取得をリクエストします。
     *
     * @param requests リクエスト情報を渡します。
     */
    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    public void requestSensorValue(OrpheValueRequest[] requests) {
        if (!canSendManualRequest() || !validateRequests(requests)) {
            return;
        }
        writeSensorValueRequest(requests);
    }

    /** 手動requestモードで1つのシリアル範囲を要求します。 */
    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    public void requestSensorValue(int startSerialNumber, int length) {
        requestSensorValue(new OrpheValueRequest[]{
                new OrpheValueRequest(startSerialNumber, length)
        });
    }

    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    private void writeSensorValueRequest(OrpheValueRequest[] requests) {
        int i = 0;
        final byte[] byteList = new byte[122];
        for(int j = 0; j < 122; j++) {
            byteList[j] = 0;
        }
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
        if (mSensorConfig.receiveMode != OrpheSensorReceiveMode.request) {
            Log.w(TAG, "cancelRequestingSensorData is available only in request mode.");
            return;
        }
        cancelRequestingSensorDataInternal();
    }

    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    private void cancelRequestingSensorDataInternal() {
        setDeviceInfo(new byte[]{11, 7});
    }

    private boolean canSendManualRequest() {
        if (mStatus != OrpheCoreStatus.connected) {
            Log.w(TAG, "A sensor request requires a connected device.");
            return false;
        }
        if (mSensorConfig.receiveMode != OrpheSensorReceiveMode.request) {
            Log.w(TAG, "Manual sensor requests are available only in request mode.");
            return false;
        }
        return true;
    }

    private boolean validateRequests(OrpheValueRequest[] requests) {
        if (requests == null || requests.length < 1) {
            Log.e(TAG, "A minimum of one request is required.");
            return false;
        }
        if (requests.length > 30) {
            Log.e(TAG, "You cannot send more than 30 requests.");
            return false;
        }
        for (OrpheValueRequest request : requests) {
            if (request == null
                    || request.startSerialNumber < 0
                    || request.startSerialNumber >= OrpheBestEffortRequester.SERIAL_NUMBER_MODULUS
                    || request.length < 1
                    || request.length >= OrpheBestEffortRequester.SERIAL_NUMBER_MODULUS) {
                Log.e(TAG, "A request contains an invalid serial number or length.");
                return false;
            }
        }
        return true;
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
            if(device == null){
                return;
            }
            final String deviceName = device.getName();
            Log.d(TAG, "onScanResult:" + deviceName);
            if(deviceName == null){
                return;
            }
            if (deviceName.contains(DeviceNameDefine.ORPHE_CORE)) {
                mBluetoothDevice = device;
                mOrpheCallback.onScan(device, new OrpheScanedMeta(deviceName));
            }
        }
    };

    private final BluetoothGattCallback mBluetoothGattCallback = new BluetoothGattCallback() {
        @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
        @SuppressLint("MissingPermission")
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            Log.d(TAG, "onConnectionStateChange:" + status + " " + newState);
            if (mClosed) {
                gatt.close();
                return;
            }
            final Handler mainHandler = new Handler(Looper.getMainLooper());
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.d(TAG, "connected");
                gatt.discoverServices();
                mainHandler.post(
                    () -> {
                        if (mClosed) {
                            return;
                        }
                        mStatus = OrpheCoreStatus.connected;
                        mOrpheCallback.onConnect(gatt.getDevice());
                    }
                );
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                stopRequestLoop();
                mainHandler.post(
                        () -> {
                            if (mClosed) {
                                gatt.close();
                                return;
                            }
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
                        applySensorConfigAfterNotificationStarted();
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
            if(GattUUIDDefine.UUID_CHAR_ORPHE_DEVICE_INFORMATION.equals(characteristic.getUuid())) {
                // Data
                mainHandler.post(
                        () -> {
                            try {
                                final DeviceInfoValue deviceInfo =  DeviceInfoValue.fromBytes(value);
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
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    handleSyncDateTimeRead();
                                }
                            } catch (Exception e) {
                                throw new RuntimeException(e);
                            }
                        }
                );
            }
        }

        @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
        private void onNotified(@NonNull BluetoothGatt gatt, @NonNull BluetoothGattCharacteristic characteristic, @NonNull byte[] value) {
            final long receivedAt = System.currentTimeMillis();
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
                                if (value.length == 0) {
                                    Log.w(TAG, "Ignored empty sensor notification.");
                                    return;
                                }
                                switch ((byte) value[0]) {
                                    case 53:
                                        if (value.length < 2) {
                                            Log.w(TAG, "Ignored short request response. length=" + value.length);
                                            return;
                                        }
                                        switch ((byte) value[1]) {
                                            case 1:
                                                if (value.length < 4) {
                                                    Log.w(TAG, "Ignored short serial response. length=" + value.length);
                                                    return;
                                                }
                                                final int currentSerialNumber = (int) (((value[2] & 0xFF) << 8) | (value[3] & 0xFF));
                                                mLatestSerialNumber = currentSerialNumber;
                                                mLatestSerialNumberTime = LocalDateTime.now();
                                                mOrpheCallback.gotCurrentSerialNumber(currentSerialNumber);
                                                if (mSensorConfig.receiveMode == OrpheSensorReceiveMode.bestEffort) {
                                                    if (value.length < 7) {
                                                        Log.w(TAG, "Ignored short Best Effort current-state response. length="
                                                                + value.length);
                                                        return;
                                                    }
                                                    final int accumulatedCount = (int) (
                                                            ((value[5] & 0xFF) << 8)
                                                                    | (value[6] & 0xFF)
                                                    );
                                                    mBestEffortRequester.onCurrentState(
                                                            currentSerialNumber,
                                                            accumulatedCount,
                                                            System.currentTimeMillis()
                                                    );
                                                }
                                                break;
                                            case 2:
                                                if (value.length < 6) {
                                                    Log.w(TAG, "Ignored short missing-data response. length=" + value.length);
                                                    return;
                                                }
                                                final int serialNumber = (int) (((value[2] & 0xFF) << 8) | (value[3] & 0xFF));
                                                final int length = (int) (((value[4] & 0xFF) << 8) | (value[5] & 0xFF));
                                                if (mSensorConfig.receiveMode == OrpheSensorReceiveMode.bestEffort) {
                                                    mBestEffortRequester.onNotFound(
                                                            serialNumber,
                                                            length,
                                                            System.currentTimeMillis()
                                                    );
                                                } else {
                                                    for (int i = 0; i < length; i++) {
                                                        mOrpheCallback.sensorValueIsNotFound(
                                                                OrpheBestEffortRequester.normalizeSerialNumber(serialNumber + i)
                                                        );
                                                    }
                                                }
                                                break;
                                            case 3:
                                            case 4:
                                            case 6:
                                                if (mSensorConfig.receiveMode
                                                        == OrpheSensorReceiveMode.bestEffort) {
                                                    mBestEffortInitializer.onAcknowledged(
                                                            value[1] & 0xFF,
                                                            System.currentTimeMillis()
                                                    );
                                                }
                                                break;
                                        }
                                        break;
                                    case 50:
                                    case 54:
                                    case 55:
                                    case 56:
                                        final OrpheSensorValue[] values = OrpheSensorValue.fromBytes(value, sidePosition, accRange, gyroRange, receivedAt);
                                        if (values.length > 0) {
                                            mLatestValue = values[values.length - 1];
                                            mLatestSerialNumber = mLatestValue.serialNumber;
                                            mLatestSerialNumberTime = LocalDateTime.now();
                                            if (mSensorConfig.receiveMode == OrpheSensorReceiveMode.bestEffort) {
                                                mBestEffortRequester.onValue(
                                                        mLatestSerialNumber,
                                                        values,
                                                        System.currentTimeMillis()
                                                );
                                            } else {
                                                mOrpheCallback.gotSensorValues(values);
                                            }
                                        }
                                        break;
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "Ignored invalid sensor notification. type="
                                        + (value[0] & 0xFF) + ", length=" + value.length, e);
                            }
                        }
                );
            }
        }
    };
}
