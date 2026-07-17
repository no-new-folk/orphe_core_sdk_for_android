package io.orphe.orphecoresdkforandroid;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.text.SimpleDateFormat;

import io.orphe.orphecoresdk.DeviceInfoValue;
import io.orphe.orphecoresdk.OrpheAccRange;
import io.orphe.orphecoresdk.OrpheBatteryStatus;
import io.orphe.orphecoresdk.OrpheCoreStatus;
import io.orphe.orphecoresdk.OrpheGyroRange;
import io.orphe.orphecoresdk.OrpheInsole;
import io.orphe.orphecoresdk.OrpheInsoleCallback;
import io.orphe.orphecoresdk.OrpheInsolePressureCalibration;
import io.orphe.orphecoresdk.OrpheInsoleSamplingRate;
import io.orphe.orphecoresdk.OrpheInsoleSensorConfig;
import io.orphe.orphecoresdk.OrpheInsoleValue;
import io.orphe.orphecoresdk.OrpheScanedMeta;
import io.orphe.orphecoresdk.OrpheSensorReceiveMode;
import io.orphe.orphecoresdk.OrpheSidePosition;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = MainActivity.class.getSimpleName();
    private static final int REQUEST_BLE_PERMISSIONS = 0;
    private static final long CHART_UPDATE_INTERVAL_MILLIS = 50L;
    private boolean mPermissionGranted = false;

    private Button mScanButton;
    private TextView mScanStatusTextView;
    private Button mConnectButtonLeft;
    private Button mConnectButtonRight;
    private Button mCalibrationButtonLeft;
    private Button mCalibrationButtonRight;
    private TextView mConnectionStatusTextViewLeft;
    private TextView mConnectionStatusTextViewRight;
    private OrpheInsole mOrpheLeft;
    private OrpheInsole mOrpheRight;

    private TextView mBatteryStatusTextViewLeft;
    private TextView mBatteryStatusTextViewRight;
    private TextView mSensorSettingTextView;
    private Spinner mReceiveModeSpinner;
    private Spinner mSamplingRateSpinner;
    private SensorLineChartView mLeftAccChart;
    private SensorLineChartView mLeftGyroChart;
    private SensorLineChartView mLeftQuatChart;
    private SensorLineChartView mLeftPressureChart;
    private SensorLineChartView mRightAccChart;
    private SensorLineChartView mRightGyroChart;
    private SensorLineChartView mRightQuatChart;
    private SensorLineChartView mRightPressureChart;
    private TextView mMeasurementStatusTextView;
    private Button mStartMeasurementButton;
    private Button mStopMeasurementButton;
    private Button mClearMeasurementButton;
    private LinearLayout mMeasurementHistoryContainer;

    private OrpheSensorReceiveMode mReceiveMode = OrpheSensorReceiveMode.fifo;
    private OrpheInsoleSamplingRate mSamplingRate = OrpheInsoleSamplingRate.hz200;
    private boolean mUpdatingSensorSettingSpinners = false;

    private final LinkedHashMap<String, DeviceCandidate> mFoundDevicesLeft = new LinkedHashMap<>();
    private final LinkedHashMap<String, DeviceCandidate> mFoundDevicesRight = new LinkedHashMap<>();
    private String mDeviceIdLeft;
    private String mDeviceIdRight;
    private boolean mScanningLeft = false;
    private boolean mScanningRight = false;
    private boolean mHasScanned = false;
    private MeasurementStorage mMeasurementStorage;
    private PressureCalibrationStorage mPressureCalibrationStorage;
    private final Object mMeasurementLock = new Object();
    private final List<OrpheInsoleValue> mLeftMeasuredValues = new ArrayList<>();
    private final List<OrpheInsoleValue> mRightMeasuredValues = new ArrayList<>();
    private boolean mIsMeasuring = false;
    private long mMeasurementStartTimeMillis = 0L;
    private long mMeasurementEndTimeMillis = 0L;
    private long mLastLeftChartUpdateMillis = 0L;
    private long mLastRightChartUpdateMillis = 0L;
    private boolean mDestroyed = false;
    private final SimpleDateFormat mDateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.US);

    private static final class DeviceCandidate {
        final BluetoothDevice device;
        final String deviceId;
        final String chargeStatus;

        DeviceCandidate(BluetoothDevice device, String deviceId, String chargeStatus) {
            this.device = device;
            this.deviceId = deviceId;
            this.chargeStatus = chargeStatus;
        }

        String displayLabel() {
            return String.format("%s  充電(%s)", deviceId, chargeStatus);
        }
    }

    private final OrpheInsoleCallback mOrpheCallbackLeft = new OrpheInsoleCallback() {
        @Override
        public void gotInsoleValues(OrpheInsoleValue[] values) {
            if (values != null && values.length > 0) {
                appendMeasuredValues(values, true);
                runOnUiThread(() -> {
                    if (mDestroyed || !shouldUpdateChart(true)) {
                        return;
                    }
                    appendInsoleValues(values, mLeftAccChart, mLeftGyroChart, mLeftQuatChart, mLeftPressureChart);
                    updateMeasurementStatus();
                });
            }
        }
        @Override
        public void sensorValueIsNotFound(int serialNumber) {
            Log.d(TAG, "NotFound: " + serialNumber);
        }


        @Override
        public void gotDeviceInfo(DeviceInfoValue value){
            if(mBatteryStatusTextViewLeft != null) {
                mBatteryStatusTextViewLeft.setText(value.batteryStatus.name());
            }
        }

        @SuppressLint("MissingPermission")
        @Override
        public void onScan(BluetoothDevice bluetoothDevice, OrpheScanedMeta meta) {
            handleScanResult(true, bluetoothDevice, meta);
        }

        @SuppressLint("MissingPermission")
        @Override
        public void onConnect(BluetoothDevice bluetoothDevice) {
            handleConnected(true, bluetoothDevice);
        }

        @SuppressLint("MissingPermission")
        @Override
        public void onDisconnect(BluetoothDevice bluetoothDevice) {
            handleDisconnected(true, bluetoothDevice);
        }
    };


    private final OrpheInsoleCallback mOrpheCallbackRight = new OrpheInsoleCallback() {
        @Override
        public void gotInsoleValues(OrpheInsoleValue[] values) {
            if (values != null && values.length > 0) {
                appendMeasuredValues(values, false);
                runOnUiThread(() -> {
                    if (mDestroyed || !shouldUpdateChart(false)) {
                        return;
                    }
                    appendInsoleValues(values, mRightAccChart, mRightGyroChart, mRightQuatChart, mRightPressureChart);
                    updateMeasurementStatus();
                });
            }
        }

        public void gotDeviceInfo(DeviceInfoValue value){
            if(mBatteryStatusTextViewRight != null) {
                mBatteryStatusTextViewRight.setText(value.batteryStatus.name());
            }
        }

        @SuppressLint("MissingPermission")
        @Override
        public void onScan(BluetoothDevice bluetoothDevice, OrpheScanedMeta meta) {
            handleScanResult(false, bluetoothDevice, meta);
        }

        @SuppressLint("MissingPermission")
        @Override
        public void onConnect(BluetoothDevice bluetoothDevice) {
            handleConnected(false, bluetoothDevice);
        }

        @SuppressLint("MissingPermission")
        @Override
        public void onDisconnect(BluetoothDevice bluetoothDevice) {
            handleDisconnected(false, bluetoothDevice);
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate");
        setContentView(R.layout.activity_main);
        mScanButton = findViewById(R.id.button_scan);
        mScanStatusTextView = findViewById(R.id.text_scan_status);
        mConnectButtonLeft = findViewById(R.id.button_connect_left);
        mConnectButtonRight = findViewById(R.id.button_connect_right);
        mCalibrationButtonLeft = findViewById(R.id.button_calibration_left);
        mCalibrationButtonRight = findViewById(R.id.button_calibration_right);
        mConnectionStatusTextViewLeft = findViewById(R.id.text_connection_status_left);
        mConnectionStatusTextViewRight = findViewById(R.id.text_connection_status_right);
        mBatteryStatusTextViewLeft = findViewById(R.id.battery_status_left);
        mBatteryStatusTextViewRight = findViewById(R.id.battery_status_right);
        mSensorSettingTextView = findViewById(R.id.text_sensor_setting);
        mReceiveModeSpinner = findViewById(R.id.spinner_receive_mode);
        mSamplingRateSpinner = findViewById(R.id.spinner_sampling_rate);
        mLeftAccChart = findViewById(R.id.chart_left_acc);
        mLeftGyroChart = findViewById(R.id.chart_left_gyro);
        mLeftQuatChart = findViewById(R.id.chart_left_quat);
        mLeftPressureChart = findViewById(R.id.chart_left_pressure);
        mRightAccChart = findViewById(R.id.chart_right_acc);
        mRightGyroChart = findViewById(R.id.chart_right_gyro);
        mRightQuatChart = findViewById(R.id.chart_right_quat);
        mRightPressureChart = findViewById(R.id.chart_right_pressure);
        mMeasurementStatusTextView = findViewById(R.id.text_measurement_status);
        mStartMeasurementButton = findViewById(R.id.button_start_measurement);
        mStopMeasurementButton = findViewById(R.id.button_stop_measurement);
        mClearMeasurementButton = findViewById(R.id.button_clear_measurement);
        mMeasurementHistoryContainer = findViewById(R.id.layout_measurement_history);
        mMeasurementStorage = new MeasurementStorage(this);
        mPressureCalibrationStorage = new PressureCalibrationStorage(this);
        configureCharts();

        mConnectionStatusTextViewLeft.setText("NoConnection");
        mConnectionStatusTextViewRight.setText("NoConnection");
        updateMeasurementStatus();

        mBatteryStatusTextViewLeft.setText(OrpheBatteryStatus.unknown.name());
        mBatteryStatusTextViewRight.setText(OrpheBatteryStatus.unknown.name());

        mPermissionGranted = hasBlePermissions();
        if (!mPermissionGranted) {
            requestBlePermissions();
        }
        OrpheInsoleSensorConfig sensorConfig = createSensorConfig();
        mOrpheLeft = new OrpheInsole(this, mOrpheCallbackLeft, OrpheSidePosition.leftPlantar, OrpheAccRange.range16, OrpheGyroRange.range2000, true, sensorConfig);
        mOrpheRight = new OrpheInsole(this, mOrpheCallbackRight, OrpheSidePosition.rightPlantar, OrpheAccRange.range16, OrpheGyroRange.range2000, true, sensorConfig);
        setupSensorSettingSpinners();
        updateSensorSettingButtons();
        mScanButton.setOnClickListener(v -> startDeviceScan());
        mConnectButtonLeft.setOnClickListener(v -> handleConnectButton(true));
        mConnectButtonRight.setOnClickListener(v -> handleConnectButton(false));
        mCalibrationButtonLeft.setOnClickListener(v -> showPressureCalibrationDialog(true));
        mCalibrationButtonRight.setOnClickListener(v -> showPressureCalibrationDialog(false));
        mStartMeasurementButton.setOnClickListener(v -> startMeasurement());
        mStopMeasurementButton.setOnClickListener(v -> stopAndSaveMeasurement());
        mClearMeasurementButton.setOnClickListener(v -> clearCurrentMeasurement());
        updateConnectionControls();
        updateMeasurementButtonStates();
        loadMeasurementHistory();
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        Log.d(TAG, "onRequestPermissionsResult requestCode: " + requestCode);
        if (requestCode != REQUEST_BLE_PERMISSIONS) {
            return;
        }
        mPermissionGranted = grantResults.length > 0;
        for (int i : grantResults) {
            if (i != PackageManager.PERMISSION_GRANTED) {
                mPermissionGranted = false;
                break;
            }
        }
        updateConnectionControls();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume");
        loadMeasurementHistory();
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause");
    }

    @Override
    protected void onDestroy() {
        Log.d(TAG, "onDestroy");
        mDestroyed = true;
        mIsMeasuring = false;
        mOrpheLeft.close();
        mOrpheRight.close();
        super.onDestroy();
    }

    private void changeButtonState(Button button, OrpheCoreStatus status){
        if(status == OrpheCoreStatus.scanned) {
            button.setBackgroundColor(Color.rgb(83, 109, 254));
            button.setTextColor(Color.rgb(240, 240, 240));
        } else if(status == OrpheCoreStatus.connecting){
            button.setBackgroundColor(Color.rgb(255, 152, 0));
            button.setTextColor(Color.rgb(240, 240, 240));
        } else if(status == OrpheCoreStatus.connected){
            button.setBackgroundColor(Color.rgb(76, 175, 80));
            button.setTextColor(Color.rgb(240, 240, 240));
        } else {
            button.setBackgroundColor(Color.rgb(180, 180, 180));
            button.setTextColor(Color.rgb(24, 24, 24));
        }
    }

    private boolean hasBlePermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED
                    && checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED;
        }
        return checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestBlePermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            requestPermissions(new String[]{
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.BLUETOOTH_SCAN}, REQUEST_BLE_PERMISSIONS);
        } else {
            requestPermissions(new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.BLUETOOTH_ADMIN,
                    Manifest.permission.BLUETOOTH}, REQUEST_BLE_PERMISSIONS);
        }
    }

    @SuppressLint("MissingPermission")
    private void startDeviceScan() {
        mPermissionGranted = hasBlePermissions();
        if (!mPermissionGranted) {
            mScanStatusTextView.setText("Bluetoothのスキャン権限が必要です");
            requestBlePermissions();
            return;
        }

        mHasScanned = true;
        if (canStartScan(mOrpheLeft.status())) {
            mFoundDevicesLeft.clear();
            mDeviceIdLeft = null;
            mScanningLeft = true;
            mConnectionStatusTextViewLeft.setText("スキャン中...");
            mOrpheLeft.startScan();
        }
        if (canStartScan(mOrpheRight.status())) {
            mFoundDevicesRight.clear();
            mDeviceIdRight = null;
            mScanningRight = true;
            mConnectionStatusTextViewRight.setText("スキャン中...");
            mOrpheRight.startScan();
        }
        updateConnectionControls();
    }

    private boolean canStartScan(OrpheCoreStatus status) {
        return status == OrpheCoreStatus.none || status == OrpheCoreStatus.scanned;
    }

    private void updateConnectionControls() {
        if (mScanButton == null || mOrpheLeft == null || mOrpheRight == null) {
            return;
        }

        final OrpheCoreStatus leftStatus = mOrpheLeft.status();
        final OrpheCoreStatus rightStatus = mOrpheRight.status();
        updateConnectionButton(mConnectButtonLeft, "Left", leftStatus, mFoundDevicesLeft.size());
        updateConnectionButton(mConnectButtonRight, "Right", rightStatus, mFoundDevicesRight.size());
        mCalibrationButtonLeft.setEnabled(
                leftStatus == OrpheCoreStatus.connected && hasText(mDeviceIdLeft)
        );
        mCalibrationButtonRight.setEnabled(
                rightStatus == OrpheCoreStatus.connected && hasText(mDeviceIdRight)
        );

        final boolean scanning = mScanningLeft || mScanningRight;
        final boolean bothConnected = leftStatus == OrpheCoreStatus.connected
                && rightStatus == OrpheCoreStatus.connected;
        mScanButton.setEnabled(!scanning && !bothConnected);
        mScanButton.setText(mHasScanned ? "Scan Again" : "Scan Devices");

        if (!mPermissionGranted) {
            mScanStatusTextView.setText("Bluetoothのスキャン権限を許可してください");
        } else if (scanning) {
            mScanStatusTextView.setText("スキャン中... 見つかった側から接続できます");
        } else if (bothConnected) {
            mScanStatusTextView.setText("左右のデバイスが接続されました");
        } else if (!mFoundDevicesLeft.isEmpty() || !mFoundDevicesRight.isEmpty()) {
            mScanStatusTextView.setText("見つかった側のConnectボタンを押してください");
        } else if (mHasScanned) {
            mScanStatusTextView.setText("スキャン完了。見つからない場合は再試行してください");
        } else {
            mScanStatusTextView.setText("Scan Devicesを押してインソールを探してください");
        }
    }

    private void updateConnectionButton(
            Button button,
            String sideLabel,
            OrpheCoreStatus status,
            int candidateCount
    ) {
        changeButtonState(button, status == OrpheCoreStatus.none && candidateCount > 0
                ? OrpheCoreStatus.scanned
                : status);
        if (status == OrpheCoreStatus.connected) {
            button.setText(sideLabel + " Disconnect");
            button.setEnabled(true);
        } else if (status == OrpheCoreStatus.connecting) {
            button.setText(sideLabel + " Connecting...");
            button.setEnabled(false);
        } else if (status == OrpheCoreStatus.disconnecting) {
            button.setText(sideLabel + " Disconnecting...");
            button.setEnabled(false);
        } else if (candidateCount > 0) {
            button.setText(sideLabel + " Connect");
            button.setEnabled(true);
        } else {
            button.setText(sideLabel);
            button.setEnabled(false);
        }
    }

    @SuppressLint("MissingPermission")
    private void handleScanResult(
            boolean left,
            @Nullable BluetoothDevice bluetoothDevice,
            @Nullable OrpheScanedMeta meta
    ) {
        final LinkedHashMap<String, DeviceCandidate> candidates = candidatesFor(left);
        final TextView statusView = left
                ? mConnectionStatusTextViewLeft
                : mConnectionStatusTextViewRight;
        if (statusView == null) {
            return;
        }

        if (bluetoothDevice == null) {
            setScanning(left, false);
            if (candidates.isEmpty()) {
                statusView.setText("タイムアウト（Scan Devicesで再試行）");
            } else {
                updateCandidateStatus(statusView, candidates);
            }
            updateConnectionControls();
            return;
        }

        final String deviceId = resolveDeviceId(bluetoothDevice, meta);
        final String chargeStatus = meta != null && meta.chargeStatus != null
                ? meta.chargeStatus.toString()
                : "不明";
        candidates.put(
                resolveDeviceKey(bluetoothDevice, deviceId),
                new DeviceCandidate(bluetoothDevice, deviceId, chargeStatus)
        );
        updateCandidateStatus(statusView, candidates);
        updateConnectionControls();
    }

    private void updateCandidateStatus(
            TextView statusView,
            LinkedHashMap<String, DeviceCandidate> candidates
    ) {
        if (candidates.size() == 1) {
            final DeviceCandidate candidate = candidates.values().iterator().next();
            statusView.setText(String.format("%s：機器が見つかりました. 充電(%s)",
                    candidate.deviceId,
                    candidate.chargeStatus));
        } else {
            statusView.setText(String.format(
                    Locale.US,
                    "%d台の機器が見つかりました（Connectで選択）",
                    candidates.size()
            ));
        }
    }

    @SuppressLint("MissingPermission")
    private void handleConnected(boolean left, BluetoothDevice bluetoothDevice) {
        setScanning(left, false);
        candidatesFor(left).clear();
        if (left) {
            if (!hasText(mDeviceIdLeft)) {
                mDeviceIdLeft = resolveDeviceId(bluetoothDevice, null);
            }
        } else if (!hasText(mDeviceIdRight)) {
            mDeviceIdRight = resolveDeviceId(bluetoothDevice, null);
        }
        applyStoredPressureCalibration(left);
        final String deviceLabel = left ? mDeviceIdLeft : mDeviceIdRight;
        final TextView statusView = left
                ? mConnectionStatusTextViewLeft
                : mConnectionStatusTextViewRight;
        if (statusView != null) {
            statusView.setText(String.format("%s：機器に接続されました", deviceLabel));
        }
        updateConnectionControls();
        updateMeasurementButtonStates();
    }

    @SuppressLint("MissingPermission")
    private void handleDisconnected(boolean left, BluetoothDevice bluetoothDevice) {
        final String selectedDeviceId = left ? mDeviceIdLeft : mDeviceIdRight;
        final String deviceLabel = hasText(selectedDeviceId)
                ? selectedDeviceId
                : resolveDeviceId(bluetoothDevice, null);
        candidatesFor(left).clear();
        if (left) {
            mDeviceIdLeft = null;
        } else {
            mDeviceIdRight = null;
        }
        setScanning(left, true);
        final TextView statusView = left
                ? mConnectionStatusTextViewLeft
                : mConnectionStatusTextViewRight;
        if (statusView != null) {
            statusView.setText(String.format("%s：接続解除、再スキャン中", deviceLabel));
        }
        updateConnectionControls();
        updateMeasurementButtonStates();
    }

    private void handleConnectButton(boolean left) {
        final OrpheInsole insole = left ? mOrpheLeft : mOrpheRight;
        final OrpheCoreStatus status = insole.status();
        if (status == OrpheCoreStatus.connected) {
            insole.disconnect();
            updateConnectionControls();
            return;
        }
        if (status == OrpheCoreStatus.connecting || status == OrpheCoreStatus.disconnecting) {
            return;
        }

        final List<DeviceCandidate> candidates = new ArrayList<>(candidatesFor(left).values());
        if (candidates.isEmpty()) {
            return;
        }
        if (candidates.size() == 1) {
            connectToCandidate(left, candidates.get(0));
        } else {
            showDeviceSelectionDialog(left, candidates);
        }
    }

    private void showDeviceSelectionDialog(boolean left, List<DeviceCandidate> candidates) {
        final String[] labels = new String[candidates.size()];
        for (int i = 0; i < candidates.size(); i++) {
            labels[i] = candidates.get(i).displayLabel();
        }
        new AlertDialog.Builder(this)
                .setTitle("接続する" + (left ? "Left" : "Right") + "デバイスを選択")
                .setItems(labels, (dialog, which) -> {
                    if (which >= 0 && which < candidates.size()) {
                        connectToCandidate(left, candidates.get(which));
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void connectToCandidate(boolean left, DeviceCandidate candidate) {
        final OrpheInsole insole = left ? mOrpheLeft : mOrpheRight;
        final OrpheCoreStatus status = insole.status();
        if (status == OrpheCoreStatus.connected
                || status == OrpheCoreStatus.connecting
                || status == OrpheCoreStatus.disconnecting) {
            return;
        }
        if (left) {
            mDeviceIdLeft = candidate.deviceId;
        } else {
            mDeviceIdRight = candidate.deviceId;
        }
        insole.connect(candidate.device);
        updateConnectionControls();
    }

    private LinkedHashMap<String, DeviceCandidate> candidatesFor(boolean left) {
        return left ? mFoundDevicesLeft : mFoundDevicesRight;
    }

    private void setScanning(boolean left, boolean scanning) {
        if (left) {
            mScanningLeft = scanning;
        } else {
            mScanningRight = scanning;
        }
    }

    @SuppressLint("MissingPermission")
    private String resolveDeviceKey(BluetoothDevice bluetoothDevice, String deviceId) {
        if (bluetoothDevice != null && hasText(bluetoothDevice.getAddress())) {
            return bluetoothDevice.getAddress();
        }
        return deviceId;
    }

    @SuppressLint("MissingPermission")
    private String resolveDeviceId(BluetoothDevice bluetoothDevice, OrpheScanedMeta meta) {
        if (meta != null && hasText(meta.deviceId)) {
            return meta.deviceId;
        }
        if (bluetoothDevice != null && hasText(bluetoothDevice.getAddress())) {
            return bluetoothDevice.getAddress();
        }
        if (bluetoothDevice != null && hasText(bluetoothDevice.getName())) {
            return bluetoothDevice.getName();
        }
        return "Unknown device";
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private void showPressureCalibrationDialog(boolean left) {
        final OrpheInsole insole = left ? mOrpheLeft : mOrpheRight;
        final String deviceId = left ? mDeviceIdLeft : mDeviceIdRight;
        if (insole == null
                || insole.status() != OrpheCoreStatus.connected
                || !hasText(deviceId)) {
            Toast.makeText(this, "Connect the insole first", Toast.LENGTH_SHORT).show();
            return;
        }
        final String sideLabel = left ? "Left" : "Right";
        PressureCalibrationDialog.show(
                this,
                sideLabel + " pressure calibration",
                mPressureCalibrationStorage.load(deviceId),
                new PressureCalibrationDialog.Listener() {
                    @Override
                    public void onSave(@NonNull OrpheInsolePressureCalibration calibration) {
                        mPressureCalibrationStorage.save(deviceId, calibration);
                        insole.setPressureCalibration(calibration);
                        Toast.makeText(
                                MainActivity.this,
                                sideLabel + " calibration saved",
                                Toast.LENGTH_SHORT
                        ).show();
                    }

                    @Override
                    public void onReset() {
                        mPressureCalibrationStorage.clear(deviceId);
                        insole.setPressureCalibration(OrpheInsolePressureCalibration.DEFAULT);
                        Toast.makeText(
                                MainActivity.this,
                                sideLabel + " calibration reset",
                                Toast.LENGTH_SHORT
                        ).show();
                    }
                }
        );
    }

    private void applyStoredPressureCalibration(boolean left) {
        if (mPressureCalibrationStorage == null) {
            return;
        }
        final OrpheInsole insole = left ? mOrpheLeft : mOrpheRight;
        final String deviceId = left ? mDeviceIdLeft : mDeviceIdRight;
        if (insole == null || !hasText(deviceId)) {
            return;
        }
        insole.setPressureCalibration(mPressureCalibrationStorage.load(deviceId));
    }

    private OrpheInsoleSensorConfig createSensorConfig() {
        return new OrpheInsoleSensorConfig(mReceiveMode, mSamplingRate);
    }

    private void setupSensorSettingSpinners() {
        ArrayAdapter<String> receiveModeAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                new String[]{"Realtime", "FIFO"}
        );
        receiveModeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mReceiveModeSpinner.setAdapter(receiveModeAdapter);
        mReceiveModeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (mUpdatingSensorSettingSpinners) {
                    return;
                }
                final OrpheSensorReceiveMode selectedMode = position == 0
                        ? OrpheSensorReceiveMode.realtime
                        : OrpheSensorReceiveMode.fifo;
                if (mReceiveMode != selectedMode) {
                    mReceiveMode = selectedMode;
                    if (selectedMode == OrpheSensorReceiveMode.fifo) {
                        mSamplingRate = OrpheInsoleSamplingRate.hz200;
                    }
                    applySensorConfigToInsoles();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        ArrayAdapter<String> samplingRateAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                new String[]{"100Hz", "200Hz"}
        );
        samplingRateAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mSamplingRateSpinner.setAdapter(samplingRateAdapter);
        mSamplingRateSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (mUpdatingSensorSettingSpinners) {
                    return;
                }
                OrpheInsoleSamplingRate selectedRate = position == 0
                        ? OrpheInsoleSamplingRate.hz100
                        : OrpheInsoleSamplingRate.hz200;
                if (mSamplingRate != selectedRate) {
                    mSamplingRate = selectedRate;
                    applySensorConfigToInsoles();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    private void applySensorConfigToInsoles() {
        final OrpheInsoleSensorConfig sensorConfig = createSensorConfig();
        if (mOrpheLeft != null) {
            mOrpheLeft.setSensorConfig(sensorConfig);
        }
        if (mOrpheRight != null) {
            mOrpheRight.setSensorConfig(sensorConfig);
        }
        updateSensorSettingButtons();
    }

    private void updateSensorSettingButtons() {
        final String receiveModeLabel = receiveModeLabel();
        if (mSensorSettingTextView != null) {
            mSensorSettingTextView.setText(String.format(
                    "%s / %s",
                    receiveModeLabel,
                    mSamplingRate == OrpheInsoleSamplingRate.hz100 ? "100Hz" : "200Hz"
            ));
        }
        mUpdatingSensorSettingSpinners = true;
        if (mReceiveModeSpinner != null) {
            final int position = mReceiveMode == OrpheSensorReceiveMode.realtime
                    ? 0
                    : 1;
            mReceiveModeSpinner.setSelection(position);
        }
        if (mSamplingRateSpinner != null) {
            mSamplingRateSpinner.setSelection(mSamplingRate == OrpheInsoleSamplingRate.hz100 ? 0 : 1);
            mSamplingRateSpinner.setEnabled(mReceiveMode == OrpheSensorReceiveMode.realtime);
        }
        mUpdatingSensorSettingSpinners = false;
        updateQuaternionChartVisibility();
    }

    private void updateQuaternionChartVisibility() {
        final int visibility = mReceiveMode == OrpheSensorReceiveMode.realtime
                && mSamplingRate == OrpheInsoleSamplingRate.hz100
                ? View.VISIBLE
                : View.GONE;
        if (mLeftQuatChart != null) {
            if (visibility == View.GONE) {
                mLeftQuatChart.clear();
            }
            mLeftQuatChart.setVisibility(visibility);
        }
        if (mRightQuatChart != null) {
            if (visibility == View.GONE) {
                mRightQuatChart.clear();
            }
            mRightQuatChart.setVisibility(visibility);
        }
    }

    private String receiveModeLabel() {
        switch (mReceiveMode) {
            case fifo:
                return "FIFO";
            case realtime:
            default:
                return "Realtime";
        }
    }

    private void configureCharts() {
        mLeftAccChart.configure("Left acc", "X", "Y", "Z");
        mLeftGyroChart.configure("Left gyro", "X", "Y", "Z");
        mLeftQuatChart.configure("Left quat", "W", "X", "Y", "Z");
        mLeftPressureChart.configure("Left pressure", "TO", "MO", "TI", "C", "MI", "H");
        mRightAccChart.configure("Right acc", "X", "Y", "Z");
        mRightGyroChart.configure("Right gyro", "X", "Y", "Z");
        mRightQuatChart.configure("Right quat", "W", "X", "Y", "Z");
        mRightPressureChart.configure("Right pressure", "TO", "MO", "TI", "C", "MI", "H");
    }

    private void appendInsoleValues(
            OrpheInsoleValue[] values,
            SensorLineChartView accChart,
            SensorLineChartView gyroChart,
            SensorLineChartView quatChart,
            SensorLineChartView pressureChart
    ) {
        for (OrpheInsoleValue value : values) {
            accChart.addValues(value.accX, value.accY, value.accZ);
            gyroChart.addValues(value.gyroX, value.gyroY, value.gyroZ);
            if (quatChart.getVisibility() == View.VISIBLE) {
                quatChart.addValues(value.quatW, value.quatX, value.quatY, value.quatZ);
            }
            pressureChart.addValues(
                    value.pressureToeOutside,
                    value.pressureMidOutside,
                    value.pressureToeInside,
                    value.pressureCenter,
                    value.pressureMidInside,
                    value.pressureHeel
            );
        }
    }

    private boolean shouldUpdateChart(boolean left) {
        final long now = SystemClock.uptimeMillis();
        final long lastUpdate = left ? mLastLeftChartUpdateMillis : mLastRightChartUpdateMillis;
        if (now - lastUpdate < CHART_UPDATE_INTERVAL_MILLIS) {
            return false;
        }
        if (left) {
            mLastLeftChartUpdateMillis = now;
        } else {
            mLastRightChartUpdateMillis = now;
        }
        return true;
    }

    private void appendMeasuredValues(OrpheInsoleValue[] values, boolean left) {
        synchronized (mMeasurementLock) {
            if (!mIsMeasuring) {
                return;
            }
            List<OrpheInsoleValue> target = left ? mLeftMeasuredValues : mRightMeasuredValues;
            for (OrpheInsoleValue value : values) {
                target.add(value);
            }
        }
    }

    private void startMeasurement() {
        if (!hasConnectedDevice()) {
            Toast.makeText(this, "接続中のデバイスがありません", Toast.LENGTH_SHORT).show();
            return;
        }
        synchronized (mMeasurementLock) {
            mLeftMeasuredValues.clear();
            mRightMeasuredValues.clear();
            mMeasurementStartTimeMillis = System.currentTimeMillis();
            mMeasurementEndTimeMillis = 0L;
            mIsMeasuring = true;
        }
        clearCharts();
        updateMeasurementStatus();
        updateMeasurementButtonStates();
    }

    private void stopAndSaveMeasurement() {
        List<OrpheInsoleValue> leftValues;
        List<OrpheInsoleValue> rightValues;
        long startTime;
        long endTime = System.currentTimeMillis();
        synchronized (mMeasurementLock) {
            if (!mIsMeasuring) {
                return;
            }
            mIsMeasuring = false;
            mMeasurementEndTimeMillis = endTime;
            startTime = mMeasurementStartTimeMillis;
            leftValues = new ArrayList<>(mLeftMeasuredValues);
            rightValues = new ArrayList<>(mRightMeasuredValues);
        }
        if (leftValues.isEmpty() && rightValues.isEmpty()) {
            Toast.makeText(this, "保存する計測データがありません", Toast.LENGTH_SHORT).show();
            updateMeasurementStatus();
            updateMeasurementButtonStates();
            return;
        }
        try {
            MeasurementRecord record = mMeasurementStorage.saveMeasurement(
                    leftValues,
                    rightValues,
                    startTime,
                    endTime,
                    mSamplingRate == OrpheInsoleSamplingRate.hz100 ? "100Hz" : "200Hz",
                    receiveModeLabel()
            );
            Toast.makeText(this, "計測を保存しました: " + record.totalCount() + "件", Toast.LENGTH_SHORT).show();
            loadMeasurementHistory();
        } catch (Exception e) {
            Log.e(TAG, "Failed to save measurement", e);
            Toast.makeText(this, "計測の保存に失敗しました", Toast.LENGTH_SHORT).show();
        }
        updateMeasurementStatus();
        updateMeasurementButtonStates();
    }

    private void clearCurrentMeasurement() {
        synchronized (mMeasurementLock) {
            if (mIsMeasuring) {
                return;
            }
            mLeftMeasuredValues.clear();
            mRightMeasuredValues.clear();
            mMeasurementStartTimeMillis = 0L;
            mMeasurementEndTimeMillis = 0L;
        }
        clearCharts();
        updateMeasurementStatus();
        updateMeasurementButtonStates();
    }

    private void updateMeasurementStatus() {
        if (mMeasurementStatusTextView == null) {
            return;
        }
        int leftCount;
        int rightCount;
        boolean measuring;
        long startTime;
        synchronized (mMeasurementLock) {
            leftCount = mLeftMeasuredValues.size();
            rightCount = mRightMeasuredValues.size();
            measuring = mIsMeasuring;
            startTime = mMeasurementStartTimeMillis;
        }
        String state;
        if (measuring) {
            state = "Measuring";
        } else if (startTime > 0) {
            state = "Stopped";
        } else {
            state = "Ready";
        }
        mMeasurementStatusTextView.setText(String.format(
                Locale.US,
                "%s / Left: %d, Right: %d",
                state,
                leftCount,
                rightCount
        ));
    }

    private void updateMeasurementButtonStates() {
        if (mStartMeasurementButton == null) {
            return;
        }
        boolean measuring;
        boolean hasCurrentData;
        synchronized (mMeasurementLock) {
            measuring = mIsMeasuring;
            hasCurrentData = !mLeftMeasuredValues.isEmpty() || !mRightMeasuredValues.isEmpty();
        }
        mStartMeasurementButton.setEnabled(!measuring && hasConnectedDevice());
        mStopMeasurementButton.setEnabled(measuring);
        mClearMeasurementButton.setEnabled(!measuring && hasCurrentData);
    }

    private boolean hasConnectedDevice() {
        return (mOrpheLeft != null && mOrpheLeft.status() == OrpheCoreStatus.connected)
                || (mOrpheRight != null && mOrpheRight.status() == OrpheCoreStatus.connected);
    }

    private void clearCharts() {
        mLeftAccChart.clear();
        mLeftGyroChart.clear();
        mLeftQuatChart.clear();
        mLeftPressureChart.clear();
        mRightAccChart.clear();
        mRightGyroChart.clear();
        mRightQuatChart.clear();
        mRightPressureChart.clear();
    }

    private void loadMeasurementHistory() {
        if (mMeasurementStorage == null || mMeasurementHistoryContainer == null) {
            return;
        }
        mMeasurementHistoryContainer.removeAllViews();
        List<MeasurementRecord> records = mMeasurementStorage.loadRecords();
        if (records.isEmpty()) {
            TextView emptyView = new TextView(this);
            emptyView.setText("No measurements");
            emptyView.setPadding(dp(16), dp(8), dp(16), dp(8));
            mMeasurementHistoryContainer.addView(emptyView);
            return;
        }
        for (MeasurementRecord record : records) {
            mMeasurementHistoryContainer.addView(createMeasurementHistoryRow(record));
        }
    }

    private View createMeasurementHistoryRow(MeasurementRecord record) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setPadding(dp(12), dp(8), dp(12), dp(8));

        TextView title = new TextView(this);
        title.setText(String.format(
                Locale.US,
                "%s  %.1fs",
                mDateFormat.format(new Date(record.startTimeMillis)),
                record.durationMillis() / 1000.0
        ));
        title.setTextSize(15);
        title.setTextColor(Color.rgb(33, 33, 33));
        row.addView(title);

        TextView summary = new TextView(this);
        summary.setText(String.format(
                Locale.US,
                "Left: %d / Right: %d / %s / %s",
                record.leftCount,
                record.rightCount,
                record.receiveMode,
                record.samplingRate
        ));
        summary.setTextColor(Color.rgb(97, 97, 97));
        row.addView(summary);

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);

        Button detailButton = new Button(this);
        detailButton.setText("Detail");
        detailButton.setOnClickListener(v -> openMeasurementDetail(record));
        actions.addView(detailButton, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));

        Button shareButton = new Button(this);
        shareButton.setText("Share");
        shareButton.setOnClickListener(v -> shareMeasurement(record));
        actions.addView(shareButton, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));

        Button deleteButton = new Button(this);
        deleteButton.setText("Delete");
        deleteButton.setOnClickListener(v -> confirmDeleteMeasurement(record));
        actions.addView(deleteButton, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));

        row.addView(actions);
        return row;
    }

    private void openMeasurementDetail(MeasurementRecord record) {
        Intent intent = new Intent(this, MeasurementDetailActivity.class);
        intent.putExtra(MeasurementDetailActivity.EXTRA_MEASUREMENT_ID, record.id);
        startActivity(intent);
    }

    private void shareMeasurement(MeasurementRecord record) {
        ArrayList<Uri> uris = new ArrayList<>(mMeasurementStorage.getCsvUris(record));
        if (uris.isEmpty()) {
            Toast.makeText(this, "共有できるCSVがありません", Toast.LENGTH_SHORT).show();
            return;
        }
        Intent intent;
        if (uris.size() == 1) {
            intent = new Intent(Intent.ACTION_SEND);
            intent.setType("text/csv");
            intent.putExtra(Intent.EXTRA_STREAM, uris.get(0));
        } else {
            intent = new Intent(Intent.ACTION_SEND_MULTIPLE);
            intent.setType("text/csv");
            intent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris);
        }
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(Intent.createChooser(intent, "Share CSV"));
    }

    private void confirmDeleteMeasurement(MeasurementRecord record) {
        new AlertDialog.Builder(this)
                .setTitle("Delete measurement")
                .setMessage(mDateFormat.format(new Date(record.startTimeMillis)) + " を削除しますか？")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Delete", (dialog, which) -> {
                    if (mMeasurementStorage.deleteRecord(record.id)) {
                        Toast.makeText(this, "削除しました", Toast.LENGTH_SHORT).show();
                        loadMeasurementHistory();
                    } else {
                        Toast.makeText(this, "削除できませんでした", Toast.LENGTH_SHORT).show();
                    }
                })
                .show();
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density);
    }
}
