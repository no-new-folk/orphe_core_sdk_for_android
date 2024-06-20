package io.orphe.orphecoresdkforandroid;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import io.orphe.orphecoresdk.Orphe;
import io.orphe.orphecoresdk.OrpheCallback;
import io.orphe.orphecoresdk.OrpheCoreStatus;
import io.orphe.orphecoresdk.OrpheInsole;
import io.orphe.orphecoresdk.OrpheInsoleCallback;
import io.orphe.orphecoresdk.OrpheInsoleValue;
import io.orphe.orphecoresdk.OrpheSensorValue;
import io.orphe.orphecoresdk.OrpheSidePosition;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = MainActivity.class.getSimpleName();
    private boolean mPermissionGranted = false;

    private Button mConnectButtonLeft;
    private Button mConnectButtonRight;
    private TextView mConnectionStatusTextViewLeft;
    private TextView mValueResultViewLeft;
    private TextView mConnectionStatusTextViewRight;
    private TextView mValueResultViewRight;
    private OrpheInsole mOrpheLeft;
    private OrpheInsole mOrpheRight;

    private BluetoothDevice mFoundDeviceLeft;
    private BluetoothDevice mFoundDeviceRight;

    private final OrpheInsoleCallback mOrpheCallbackLeft = new OrpheInsoleCallback() {
        @Override
        public void gotInsoleValue(OrpheInsoleValue value) {
            if (value != null) {
                if (mValueResultViewLeft != null) {
                    mValueResultViewLeft.setText(value.toString());
                }
            }
        }

        @SuppressLint("MissingPermission")
        @Override
        public void onScan(BluetoothDevice bluetoothDevice) {
            if (mConnectionStatusTextViewLeft != null) {
                if (bluetoothDevice != null) {
                    mFoundDeviceLeft = bluetoothDevice;
                    changeButtonState(mConnectButtonLeft, OrpheCoreStatus.scanned);
                    mConnectionStatusTextViewLeft.setText(
                            String.format("%s：機器が見つかりました", bluetoothDevice.getName()));
                } else {
                    mFoundDeviceLeft = null;
                    changeButtonState(mConnectButtonLeft, OrpheCoreStatus.none);
                    mConnectionStatusTextViewLeft.setText("機器が見つかりませんでした");
                }
            }
        }

        @SuppressLint("MissingPermission")
        @Override
        public void onConnect(BluetoothDevice bluetoothDevice) {
            if (mConnectionStatusTextViewLeft != null) {
                changeButtonState(mConnectButtonLeft, OrpheCoreStatus.connected);
                mConnectionStatusTextViewLeft.setText(
                        String.format("%s：機器に接続されました", bluetoothDevice.getName()));
            }
        }

        @SuppressLint("MissingPermission")
        @Override
        public void onDisconnect(BluetoothDevice bluetoothDevice) {
            if (mConnectionStatusTextViewLeft != null) {
                changeButtonState(mConnectButtonLeft, OrpheCoreStatus.none);
                mConnectionStatusTextViewLeft.setText(
                        String.format("%s：機器の接続が解除されました", bluetoothDevice.getName()));
            }
        }
    };


    private final OrpheInsoleCallback mOrpheCallbackRight = new OrpheInsoleCallback() {
        @Override
        public void gotInsoleValue(OrpheInsoleValue value) {
            if (value != null) {
                if (mValueResultViewRight != null) {
                    mValueResultViewRight.setText(value.toString());
                }
            }
        }

        @SuppressLint("MissingPermission")
        @Override
        public void onScan(BluetoothDevice bluetoothDevice) {
            if (mConnectionStatusTextViewRight != null) {
                changeButtonState(mConnectButtonRight, OrpheCoreStatus.scanned);
                if (bluetoothDevice != null) {
                    mFoundDeviceRight = bluetoothDevice;
                    mConnectionStatusTextViewRight.setText(
                            String.format("%s：機器が見つかりました", bluetoothDevice.getName()));
                } else {
                    mFoundDeviceRight = null;
                    changeButtonState(mConnectButtonRight, OrpheCoreStatus.none);
                    mConnectionStatusTextViewRight.setText("機器が見つかりませんでした");
                }
            }
        }

        @SuppressLint("MissingPermission")
        @Override
        public void onConnect(BluetoothDevice bluetoothDevice) {
            if (mConnectionStatusTextViewRight != null) {
                changeButtonState(mConnectButtonRight, OrpheCoreStatus.connected);
                mConnectionStatusTextViewRight.setText(
                        String.format("%s：機器に接続されました", bluetoothDevice.getName()));
            }
        }

        @SuppressLint("MissingPermission")
        @Override
        public void onDisconnect(BluetoothDevice bluetoothDevice) {
            if (mConnectionStatusTextViewRight != null) {
                changeButtonState(mConnectButtonRight, OrpheCoreStatus.none);
                mConnectionStatusTextViewRight.setText(
                        String.format("%s：機器の接続が解除されました", bluetoothDevice.getName()));
            }
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate");
        setContentView(R.layout.activity_main);
        mConnectButtonLeft = findViewById(R.id.button_connect_left);
        mConnectButtonRight = findViewById(R.id.button_connect_right);
        mConnectionStatusTextViewLeft = findViewById(R.id.text_connection_status_left);
        mValueResultViewLeft = findViewById(R.id.text_value_result_left);
        mConnectionStatusTextViewRight = findViewById(R.id.text_connection_status_right);
        mValueResultViewRight = findViewById(R.id.text_value_result_right);

        mConnectionStatusTextViewLeft.setText("NoConnection");
        mConnectionStatusTextViewRight.setText("NoConnection");
        mValueResultViewLeft.setText("NoValue");
        mValueResultViewRight.setText("NoValue");

        if (this.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            mPermissionGranted = false;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                requestPermissions(new String[]{
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.BLUETOOTH_CONNECT,
                        Manifest.permission.BLUETOOTH_SCAN}, 0);
            } else {
                requestPermissions(new String[]{
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.BLUETOOTH_ADMIN,
                        Manifest.permission.BLUETOOTH}, 0);
            }
        } else {
            mPermissionGranted = true;
        }
        mOrpheLeft = new OrpheInsole(this, mOrpheCallbackLeft, OrpheSidePosition.leftPlantar);
        mOrpheRight = new OrpheInsole(this, mOrpheCallbackRight, OrpheSidePosition.rightPlantar);
        changeButtonState(mConnectButtonLeft, OrpheCoreStatus.none);
        changeButtonState(mConnectButtonRight, OrpheCoreStatus.none);
        mConnectButtonLeft.setOnClickListener(v -> {
            final OrpheCoreStatus status = mOrpheLeft.status();
            if(status == OrpheCoreStatus.scanned && mFoundDeviceLeft != null){
                mOrpheLeft.connect(mFoundDeviceLeft);
            } else if(status == OrpheCoreStatus.connected){
                mOrpheLeft.disconnect();
            }
        });
        mConnectButtonRight.setOnClickListener(v -> {
            final OrpheCoreStatus status = mOrpheRight.status();           
            if(status == OrpheCoreStatus.scanned && mFoundDeviceRight != null){
                mOrpheRight.connect(mFoundDeviceRight);
            } else if(status == OrpheCoreStatus.connected){
                mOrpheRight.disconnect();
            }
        });
        mOrpheLeft.startScan();
        mOrpheRight.startScan();
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        Log.d(TAG, "onRequestPermissionsResult requestCode: " + requestCode);
        for (int i : grantResults) {
            if (i != PackageManager.PERMISSION_GRANTED) {
                mPermissionGranted = false;
                finish();
            }
        }
        mPermissionGranted = true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume");
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");
        mOrpheLeft.disconnect();
        mOrpheRight.disconnect();
        mOrpheLeft = null;
        mOrpheRight = null;
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
}