package io.orphe.orphecoresdkforandroid;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.content.pm.PackageManager;
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
import io.orphe.orphecoresdk.OrpheSensorValue;
import io.orphe.orphecoresdk.OrpheSidePosition;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = MainActivity.class.getSimpleName();
    private boolean mPermissionGranted = false;
    private TextView mConnectionStatusTextViewLeft;
    private TextView mValueResultViewLeft;
    private TextView mConnectionStatusTextViewRight;
    private TextView mValueResultViewRight;
    private Orphe mOrpheLeft;
    private Orphe mOrpheRight;

    private final OrpheCallback mOrpheCallbackLeft = new OrpheCallback() {
        @Override
        public void gotSensorValues(OrpheSensorValue[] values) {
            if (values != null && values.length > 0) {
                if (mValueResultViewLeft != null) {
                    mValueResultViewLeft.setText(values[0].toString());
                }
            }
        }

        @SuppressLint("MissingPermission")
        @Override
        public void onScan(BluetoothDevice bluetoothDevice) {
            if (mConnectionStatusTextViewLeft != null) {
                if (bluetoothDevice != null) {
                    mConnectionStatusTextViewLeft.setText(
                            String.format("%s：機器が見つかりました", bluetoothDevice.getName()));
                } else {
                    mConnectionStatusTextViewLeft.setText("機器が見つかりませんでした");
                }
            }
        }

        @SuppressLint("MissingPermission")
        @Override
        public void onConnect(BluetoothDevice bluetoothDevice) {
            if (mConnectionStatusTextViewLeft != null) {
                mConnectionStatusTextViewLeft.setText(
                        String.format("%s：機器に接続されました", bluetoothDevice.getName()));
            }
        }

        @SuppressLint("MissingPermission")
        @Override
        public void onDisconnect(BluetoothDevice bluetoothDevice) {
            if (mConnectionStatusTextViewLeft != null) {
                mConnectionStatusTextViewLeft.setText(
                        String.format("%s：機器の接続が解除されました", bluetoothDevice.getName()));
            }
        }
    };


    private final OrpheCallback mOrpheCallbackRight = new OrpheCallback() {
        @Override
        public void gotSensorValues(OrpheSensorValue[] values) {
            Log.d(TAG, "" + values.length);
        }

        @SuppressLint("MissingPermission")
        @Override
        public void onScan(BluetoothDevice bluetoothDevice) {
            if (mConnectionStatusTextViewRight != null) {
                if (bluetoothDevice != null) {
                    mConnectionStatusTextViewRight.setText(
                            String.format("%s：機器が見つかりました", bluetoothDevice.getName()));
                } else {
                    mConnectionStatusTextViewRight.setText("機器が見つかりませんでした");
                }
            }
        }

        @SuppressLint("MissingPermission")
        @Override
        public void onConnect(BluetoothDevice bluetoothDevice) {
            if (mConnectionStatusTextViewRight != null) {
                mConnectionStatusTextViewRight.setText(
                        String.format("%s：機器に接続されました", bluetoothDevice.getName()));
            }
        }

        @SuppressLint("MissingPermission")
        @Override
        public void onDisconnect(BluetoothDevice bluetoothDevice) {
            if (mConnectionStatusTextViewRight != null) {
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
        Button connectButtonLeft = findViewById(R.id.button_connect_left);
        Button connectButtonRight = findViewById(R.id.button_connect_right);
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
        mOrpheLeft = new Orphe(this, mOrpheCallbackLeft, OrpheSidePosition.leftPlantar);
        mOrpheRight = new Orphe(this, mOrpheCallbackRight, OrpheSidePosition.rightPlantar);
        connectButtonLeft.setOnClickListener(v -> {
            mConnectionStatusTextViewLeft.setText("機器をスキャン中");
            mOrpheLeft.begin();
        });
        connectButtonRight.setOnClickListener(v -> {
            mConnectionStatusTextViewRight.setText("機器をスキャン中");
            mOrpheRight.begin();
        });
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
        mOrpheLeft.stop();
        mOrpheRight.stop();
        mOrpheLeft = null;
        mOrpheRight = null;
    }
}