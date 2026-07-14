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
    private static final int SYNC_DATE_TIME_TRY_COUNT = 3;
    private static final long SYNC_DATE_TIME_WAIT_MS = 100;
    private static final long SENSOR_COMMAND_INTERVAL_MS = 100L;
    private static final int SAMPLING_RATE_MAX_RETRY_COUNT = 3;
    private static final long SAMPLING_RATE_RETRY_DELAY_MS = 500L;
    private final Context mContext;
    private final OrpheInsoleCallback mOrpheCallback;
    private BluetoothLeScanner mBluetoothLeScanner;
    private BluetoothDevice mBluetoothDevice;
    private BluetoothGatt mBluetoothGatt;
    private final Handler mHandler = new Handler();
    public final OrpheSidePosition sidePosition;

    private OrpheInsoleValue mLatestValue;

    private int mLatestSerialNumber;
    private LocalDateTime mLatestSerialNumberTime;
    private long mLatestSerialNumberReceivedAtMillis = -1L;
    private boolean mSyncDateTimeInProgress;
    private int mSyncDateTimeReadCount;
    private long mSyncDateTimeTotalRttMillis;
    private long mSyncDateTimeReadStartMillis;
    private BluetoothGattCharacteristic mSyncDateTimeCharacteristic;

    private boolean mDebugMode;
    private OrpheInsoleSensorConfig mSensorConfig;
    private boolean mRequestLoopStarted;
    private OrpheBestEffortRequester<OrpheInsoleValue[]> mBestEffortRequester;
    private OrpheBestEffortInitializer mBestEffortInitializer;
    private OrpheInsoleValueAccumulator mBestEffortValues =
            new OrpheInsoleValueAccumulator();
    private final OrpheQuaternionTimeline<OrpheInsoleValue> mQuaternionTimeline =
            OrpheQuaternionTimelines.forInsole();
    private final OrpheInsoleSamplingRateGuard mSamplingRateGuard =
            new OrpheInsoleSamplingRateGuard(SAMPLING_RATE_MAX_RETRY_COUNT);
    private boolean mSamplingRateValidationActive;
    private boolean mClosed;
    private final Runnable mRequestLoopRunnable = () -> {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestLatestInsoleValueForBestEffortMode();
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
    private final Runnable mSamplingRateRetryRunnable = () -> {
        mSamplingRateGuard.retryStarted();
        if (mClosed
                || status() != OrpheCoreStatus.connected
                || mSensorConfig.receiveMode != OrpheSensorReceiveMode.realtime
                || Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return;
        }
        Log.w(TAG, "Retrying sensor sampling rate: " + mSensorConfig.samplingRate);
        mSamplingRateValidationActive = false;
        stopRequestLoop();
        resetBestEffortAnchorCandidate();
        cancelRequestingSensorDataInternal();
        applySensorConfigAfterNotificationStarted(false);
    };

    private volatile OrpheInsolePressureCalibration mPressureCalibration =
            OrpheInsolePressureCalibration.DEFAULT;

    /**
     * 6点すべての圧力補正値を一括設定します。
     *
     * @param pressureCalibration 6点の圧力補正設定
     */
    public synchronized void setPressureCalibration(
            @NonNull final OrpheInsolePressureCalibration pressureCalibration
    ) {
        if (pressureCalibration == null) {
            throw new IllegalArgumentException("Pressure calibration must not be null.");
        }
        mPressureCalibration = pressureCalibration;
    }

    /**
     * 圧力係数の設定。coefficient1、coefficient2、coefficient3、thresholdを設定できます。
     * @param sensorPosition センサーの取り付け位置
     * @param coefficient 圧力係数
     * @param value 圧力係数の値
     */
    public synchronized void setCoefficient(
            @NonNull final OrpheInsoleSensorPosition sensorPosition,
            @NonNull final OrpheInsoleCoefficient coefficient,
            final double value
    ) {
        mPressureCalibration = mPressureCalibration.withCoefficient(sensorPosition, coefficient, value);
    }


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
     * 現在のbestEffortセッションで取得できた全値をシリアル順で返します。
     *
     * @return SDK内でマージ済みの全値
     */
    @NonNull
    public OrpheInsoleValue[] getBestEffortValues() {
        return mBestEffortValues.snapshot();
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
     * @param debugMode     デバッグモード
     */
    public OrpheInsole(@NonNull final Context context, @NonNull final OrpheInsoleCallback orpheCallback, @NonNull final OrpheSidePosition sidePosition, @NonNull final OrpheAccRange accRange, @NonNull final OrpheGyroRange gyroRange, boolean debugMode) {
        this(context, orpheCallback, sidePosition, accRange, gyroRange, debugMode, OrpheInsoleSensorConfig.DEFAULT);
    }

    /**
     * ORPHE INSOLEを管理します。
     * インスタンス化したあと[startScan]で対応しているORPHE INSOLEを探し、[connect]で接続します。
     * [disconnect]で切断します。
     *
     * @param context       コンテキスト
     * @param orpheCallback コールバック引数
     * @param sidePosition  この[Orphe]に対応する取り付け位置
     * @param accRange      加速度レンジの設定
     * @param gyroRange     ジャイロレンジの設定
     * @param debugMode     デバッグモード
     * @param sensorConfig  センサー値取得初期設定
     */
    public OrpheInsole(@NonNull final Context context, @NonNull final OrpheInsoleCallback orpheCallback, @NonNull final OrpheSidePosition sidePosition, @NonNull final OrpheAccRange accRange, @NonNull final OrpheGyroRange gyroRange, boolean debugMode, @NonNull final OrpheInsoleSensorConfig sensorConfig) {
        mContext = context;
        mOrpheCallback = orpheCallback;
        this.sidePosition = sidePosition;
        this.accRange = accRange;
        this.gyroRange = gyroRange;
        this.mDebugMode = debugMode;
        this.mSensorConfig = sensorConfig;
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
        this(context, orpheCallback, sidePosition, accRange, gyroRange, false);
    }

    /**
     * ORPHE INSOLEを管理します。
     *
     * @param context       コンテキスト
     * @param orpheCallback コールバック引数
     * @param sidePosition  この[Orphe]に対応する取り付け位置
     * @param accRange      加速度レンジの設定
     * @param gyroRange     ジャイロレンジの設定
     * @param sensorConfig  センサー値取得初期設定
     */
    public OrpheInsole(@NonNull final Context context, @NonNull final OrpheInsoleCallback orpheCallback, @NonNull final OrpheSidePosition sidePosition, @NonNull final OrpheAccRange accRange, @NonNull final OrpheGyroRange gyroRange, @NonNull final OrpheInsoleSensorConfig sensorConfig) {
        this(context, orpheCallback, sidePosition, accRange, gyroRange, false, sensorConfig);
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
     * センサー値取得設定を変更します。接続中の場合は次回接続時に反映されます。
     *
     * @param sensorConfig センサー値取得設定
     */
    public void setSensorConfig(@NonNull final OrpheInsoleSensorConfig sensorConfig) {
        final OrpheSensorReceiveMode previousReceiveMode = mSensorConfig.receiveMode;
        resetSamplingRateGuard();
        stopRequestLoop();
        resetBestEffortAnchorCandidate();
        mQuaternionTimeline.clear();
        mSensorConfig = sensorConfig;
        createBestEffortRequester();
        if (mStatus == OrpheCoreStatus.connected && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            cancelRequestingSensorDataInternal();
            if (previousReceiveMode != OrpheSensorReceiveMode.realtime
                    && sensorConfig.receiveMode == OrpheSensorReceiveMode.realtime) {
                stopAccumulation();
            }
            applySensorConfigAfterNotificationStarted();
        }
    }


    /**
     * ORPHE INSOLEのスキャンを開始します。
     * 見つかった場合は[OrpheInsoleCallback.onScan]に対応する[BluetoothDevice]が渡されます。
     */
    @SuppressLint("MissingPermission")
    public void startScan() {
        if (mClosed || mBluetoothLeScanner == null) {
            return;
        }
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
                mStatus = OrpheCoreStatus.none;
                mOrpheCallback.onScan(null, null);
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && mSensorConfig.receiveMode != OrpheSensorReceiveMode.realtime) {
            stopAccumulation();
            stopRequestLoop();
        }
        mQuaternionTimeline.clear();
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
        if (mClosed || device == null || mBluetoothLeScanner == null) {
            return;
        }
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            mBluetoothGatt.writeCharacteristic(
                    characteristic, value, BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);
        } else {
            characteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);
            characteristic.setValue(value);
            mBluetoothGatt.writeCharacteristic(characteristic);
        }
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

    /**
     * 保留中の処理を破棄し、BLEリソースを解放します。このインスタンスは再利用できません。
     */
    @SuppressLint("MissingPermission")
    public void close() {
        mClosed = true;
        resetSamplingRateGuard();
        mHandler.removeCallbacksAndMessages(null);
        stopRequestLoop();
        mQuaternionTimeline.clear();
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
        final byte[] value = new byte[]{
                (byte)(now.getYear() - 2000),
                (byte)(now.getMonthValue()),
                (byte)(now.getDayOfMonth()),
                (byte)(now.getHour()),
                (byte)(now.getMinute()),
                (byte)(now.getSecond()),
                (byte)(now.getNano() / 1_000_000 / 10),
        };
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            mBluetoothGatt.writeCharacteristic(
                    characteristic, value, BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);
        } else {
            characteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);
            characteristic.setValue(value);
            mBluetoothGatt.writeCharacteristic(characteristic);
        }
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

    /**
     * 設定済みのINSOLEサンプリングレートをデバイスに適用します。
     */
    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    public void applyInsoleSamplingRate() {
        setSensorRequestMode(mSensorConfig.samplingRate.toSensorRequestMode());
    }

    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    private void applySensorConfigAfterNotificationStarted() {
        applySensorConfigAfterNotificationStarted(true);
    }

    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    private void applySensorConfigAfterNotificationStarted(final boolean resetBestEffortValues) {
        if (resetBestEffortValues
                && mSensorConfig.receiveMode == OrpheSensorReceiveMode.bestEffort) {
            // 過去の更新オブジェクトが保持するスナップショット時点を壊さないよう、
            // 新しいセッションではストア自体を入れ替える。
            mBestEffortValues = new OrpheInsoleValueAccumulator();
        }
        if (mSensorConfig.receiveMode != OrpheSensorReceiveMode.realtime) {
            mQuaternionTimeline.clear();
        }
        Handler handler = new Handler(Looper.getMainLooper());
        handler.postDelayed(() -> {
            if (mClosed || mStatus != OrpheCoreStatus.connected) {
                return;
            }
            if (mSensorConfig.receiveMode == OrpheSensorReceiveMode.realtime) {
                applyInsoleSamplingRate();
                mSamplingRateValidationActive = true;
                return;
            }

            // FWのrequest / bestEffort蓄積データは0x36 (200Hz) 固定。
            // Realtimeの100Hz/200Hzモードを経由せず、直接requestへ切り替える。
            mSamplingRateValidationActive = false;
            setSensorRequestMode(OrpheSensorRequestMode.request);
            handler.postDelayed(() -> {
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

    private boolean acceptsSensorPacketHeader(final int packetHeader) {
        if (mSensorConfig.receiveMode != OrpheSensorReceiveMode.realtime) {
            return packetHeader == 54;
        }
        if (mSamplingRateGuard.accepts(mSensorConfig.samplingRate, packetHeader)) {
            mHandler.removeCallbacks(mSamplingRateRetryRunnable);
            return true;
        }
        if (!mSamplingRateValidationActive) {
            return false;
        }
        if (mSamplingRateGuard.scheduleRetry()) {
            Log.w(
                    TAG,
                    "Unexpected sensor packet header. expected="
                            + mSensorConfig.samplingRate.sensorValueHeader
                            + ", actual=" + packetHeader
                            + ", retry=" + mSamplingRateGuard.retryCount()
                            + "/" + SAMPLING_RATE_MAX_RETRY_COUNT
            );
            mHandler.postDelayed(
                    mSamplingRateRetryRunnable,
                    SAMPLING_RATE_RETRY_DELAY_MS
            );
        }
        return false;
    }

    private void resetSamplingRateGuard() {
        mHandler.removeCallbacks(mSamplingRateRetryRunnable);
        mSamplingRateGuard.reset();
        mSamplingRateValidationActive = false;
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
        requestLatestInsoleValueForBestEffortMode();
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
    private void requestLatestInsoleValueForBestEffortMode() {
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

    private void resetBestEffortAnchorCandidate() {
        mLatestSerialNumberReceivedAtMillis = -1L;
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
                20L,
                mSensorConfig.bestEffortConfig,
                new OrpheBestEffortRequester.Listener<OrpheInsoleValue[]>() {
                    @Override
                    public void onCurrentStateRequest() {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            setDeviceInfo(new byte[]{11, 1});
                        }
                    }

                    @Override
                    public void onRequest(@NonNull OrpheValueRequest[] requests) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            writeInsoleValueRequest(requests);
                        }
                    }

                    @Override
                    public void onValue(@NonNull OrpheInsoleValue[] values) {
                        // 表示通知は受信直後に行う。要求器は欠損回収の進行だけを担当する。
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
     * 最新の[OrpheInsoleValue]の取得をリクエストします。
     *
     * @param length 取得する数（あくまで目安）
     */
    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    public void requestLatestInsoleValue(int length) {
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
            final long startTime = now - length * 20;
            serialNumber = serialNumber + (int) Math.ceil((startTime - prev) / 20);
            serialNumber = OrpheBestEffortRequester.normalizeSerialNumber(serialNumber);
            Log.d(TAG, "Request: " + serialNumber + ", " + length);
            requestInsoleValue(new OrpheValueRequest[]{
                    new OrpheValueRequest(serialNumber, length)
            });
        } else {
            if(now > prev){
                final int l = (int) Math.ceil((now - prev) / 20);
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
        if (!canSendManualRequest() || !validateRequests(requests)) {
            return;
        }
        mQuaternionTimeline.clear();
        writeInsoleValueRequest(requests);
    }

    /** 手動requestモードで1つのシリアル範囲を要求します。 */
    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    public void requestInsoleValue(int startSerialNumber, int length) {
        requestInsoleValue(new OrpheValueRequest[]{
                new OrpheValueRequest(startSerialNumber, length)
        });
    }

    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    private void writeInsoleValueRequest(OrpheValueRequest[] requests) {
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
            if (record == null) {
                return;
            }
            final byte[] manufacturerData = record.getManufacturerSpecificData(0);
            if (device == null || manufacturerData == null) {
                return;
            }
            String deviceName = device.getName();
            // TODO: 暫定的にManufacturerDataから探す
            if (manufacturerData.length >= 15 && manufacturerData[0] == 1 && manufacturerData[5] == 1) {
                // 左右情報が一致しない場合は排除
                if(sidePosition.side == OrpheSide.left && manufacturerData[6] > 0){
                    return;
                } else if(sidePosition.side == OrpheSide.right && manufacturerData[6] != 1){
                    return;
                }
                mBluetoothDevice = device;
                deviceName = formatInsoleId(manufacturerData);
                OrpheInsoleChargeStatus chargeStatus = OrpheInsoleChargeStatus.fromValue(manufacturerData[14]);
                mOrpheCallback.onScan(device, new OrpheScanedMeta(deviceName, chargeStatus));
                return;
            }
            // 左右情報が一致しない場合は排除
            if (manufacturerData.length < 7) {
                return;
            }
            if(sidePosition.side == OrpheSide.left && manufacturerData[6] > 0){
                return;
            } else if(sidePosition.side == OrpheSide.right && manufacturerData[6] != 1){
                return;
            }
            if (deviceName == null) {
                return;
            }
            if (deviceName.contains(DeviceNameDefine.ORPHE_CORE)) {
                mBluetoothDevice = device;
                mOrpheCallback.onScan(device, new OrpheScanedMeta(deviceName));
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
                            resetSamplingRateGuard();
                            resetBestEffortAnchorCandidate();
                            mStatus = OrpheCoreStatus.connected;
                            mOrpheCallback.onConnect(gatt.getDevice());
                        }
                );
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.d(TAG, "disconnected");
                Log.d(TAG, status().toString());
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    stopRequestLoop();
                }
                mainHandler.post(
                        () -> {
                            if (mClosed) {
                                gatt.close();
                                return;
                            }
                            resetSamplingRateGuard();
                            resetBestEffortAnchorCandidate();
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
            if(mDebugMode){
                Log.d(TAG, "onRead:" + characteristic.getUuid().toString() + " " + bytesToHex(value));
            }
            // DeviceInfo
            if (GattUUIDDefine.UUID_CHAR_ORPHE_DEVICE_INFORMATION.equals(characteristic.getUuid())) {
                // Data
                mainHandler.post(
                        () -> {
                            if (mClosed) {
                                return;
                            }
                            try {
                                final DeviceInfoValue deviceInfo = DeviceInfoValue.fromBytes(value);
                                mDeviceInfo = deviceInfo;
                                mOrpheCallback.gotDeviceInfo(deviceInfo);
                            } catch (Exception e) {
                                Log.e(TAG, "Failed to parse device info. length=" + value.length, e);
                            }
                        }
                );
            }
            // DateTime
            if (GattUUIDDefine.UUID_CHAR_ORPHE_DATE_TIME.equals(characteristic.getUuid())) {
                // Data
                mainHandler.post(
                        () -> {
                            if (mClosed) {
                                return;
                            }
                            try {
                                Log.d(TAG, "DateTime: " + value[0] + ", " + value[1] + ", " + value[2] + ", " + value[3] + ", " + value[4] + ", " + value[5] + ", " + value[6]);
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    handleSyncDateTimeRead();
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "Failed to parse date/time. length=" + value.length, e);
                            }
                        }
                );
            }
        }

        @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
        private void onNotified(@NonNull BluetoothGatt gatt, @NonNull BluetoothGattCharacteristic characteristic, @NonNull byte[] value) {
            final long receivedAt = System.currentTimeMillis();
            final Handler mainHandler = new Handler(Looper.getMainLooper());
            if (mDebugMode) {
                Log.d(TAG, "onNotified:" + characteristic.getUuid().toString() + " " + bytesToHex(value));
            }
            // 歩容解析
            // if (GattUUIDDefine.UUID_CHAR_ORPHE_STEP_ANALYSIS.equals(characteristic.getUuid())) {
            //    mOrpheCallback.gotData(value);
            // }
            // 生データ
            if (GattUUIDDefine.UUID_CHAR_ORPHE_SENSOR_VALUES.equals(characteristic.getUuid())) {
                // Data
                mainHandler.post(
                        () -> {
                            if (mClosed) {
                                return;
                            }
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
                                                mLatestSerialNumberReceivedAtMillis = receivedAt;
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
                                    case 54:
                                    case 55:
                                    case 56:
                                        if (!acceptsSensorPacketHeader(value[0] & 0xFF)) {
                                            break;
                                        }
                                        final OrpheInsoleValue[] values;
                                        OrpheQuaternionTimeline.Result<OrpheInsoleValue>
                                                quaternionResult = null;
                                        if (mSensorConfig.receiveMode == OrpheSensorReceiveMode.realtime) {
                                            values = OrpheInsoleValue.fromBytes(
                                                    value,
                                                    sidePosition,
                                                    accRange,
                                                    gyroRange,
                                                    mPressureCalibration,
                                                    receivedAt
                                            );
                                        } else {
                                            final OrpheInsoleValue[] fullRateValues =
                                                    OrpheInsoleValue.fromBytes(
                                                    value,
                                                    sidePosition,
                                                    accRange,
                                                    gyroRange,
                                                    mPressureCalibration,
                                                    receivedAt,
                                                    OrpheInsoleSamplingRate.hz200
                                            );
                                            quaternionResult = mQuaternionTimeline.add(fullRateValues);
                                            values = OrpheInsoleValue.forOutputSamplingRate(
                                                    quaternionResult.receivedValues,
                                                    mSensorConfig.samplingRate
                                            );
                                        }
                                        if (values.length > 0) {
                                            mLatestValue = values[values.length - 1];
                                            mLatestSerialNumber = mLatestValue.serialNumber;
                                            mLatestSerialNumberTime = LocalDateTime.now();
                                            mLatestSerialNumberReceivedAtMillis = receivedAt;
                                            if (mSensorConfig.receiveMode == OrpheSensorReceiveMode.bestEffort) {
                                                final ArrayList<OrpheInsoleValue[]>
                                                        recalculatedPackets = new ArrayList<>();
                                                if (quaternionResult != null) {
                                                    for (OrpheInsoleValue[] recalculated
                                                            : quaternionResult.recalculatedPackets) {
                                                        recalculatedPackets.add(
                                                                OrpheInsoleValue.forOutputSamplingRate(
                                                                        recalculated,
                                                                        mSensorConfig.samplingRate
                                                                )
                                                        );
                                                    }
                                                }
                                                final OrpheInsoleValueUpdate update =
                                                        mBestEffortValues.add(
                                                                values,
                                                                recalculatedPackets
                                                        );
                                                if (update != null) {
                                                    mOrpheCallback.gotInsoleValues(update);
                                                }
                                                mBestEffortRequester.onValue(
                                                        mLatestSerialNumber,
                                                        values,
                                                        receivedAt
                                                );
                                            } else {
                                                mOrpheCallback.gotInsoleValues(values);
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

    static int forwardSerialDistance(int previousSerialNumber, int currentSerialNumber) {
        return OrpheBestEffortRequester.forwardSerialDistance(
                previousSerialNumber,
                currentSerialNumber
        );
    }

    static boolean isRecoverableGap(int forwardDistance, int requestLength) {
        return forwardDistance > 1 && forwardDistance - 1 <= Math.max(1, requestLength);
    }

    private static String formatInsoleId(@NonNull byte[] data) {
        long number = getUint32(data, 1);
        char side = (data[6] == 0) ? 'L' : 'R';
        return String.format("IN%08X%c", number, side);
    }

    private static long getUint32(@NonNull byte[] data, int index) {
        return (long) (((data[index] & 0xFF) << 24) | ((data[index + 1] & 0xFF) << 16) | ((data[index + 2] & 0xFF) << 8) | (data[index + 3] & 0xFF));
    }
}
