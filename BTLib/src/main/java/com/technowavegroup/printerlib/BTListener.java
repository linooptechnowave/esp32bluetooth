package com.technowavegroup.printerlib;

import android.bluetooth.BluetoothDevice;

public interface BTListener {

    void onDeviceConnected(boolean isConnected, String statusMessage, BluetoothDevice bluetoothDevice);

    void onDeviceStateChange(boolean completed, String status);

    void onDeviceDisconnected(boolean isDisconnected, String statusMessage);

    void onDeviceError(String errorMessage);
}
