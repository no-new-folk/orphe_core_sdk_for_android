package io.orphe.orphecoresdkforandroid;

import android.bluetooth.BluetoothDevice;

import java.util.UUID;

public abstract class OrpheCallback {

    public void gotData(byte[] data) {

    }

    public void gotStepsNumber(int stepsNumber) {

    }

    public void onScan(BluetoothDevice bluetoothDevice) {
    }

    public void onConnect(BluetoothDevice bluetoothDevice) {
    }

    public void onDisconnect(BluetoothDevice bluetoothDevice) {

    }

    public void onStartNotify(UUID characteristicUuid) {

    }

    public void onStopNotify(UUID characteristicUuid) {

    }
}
