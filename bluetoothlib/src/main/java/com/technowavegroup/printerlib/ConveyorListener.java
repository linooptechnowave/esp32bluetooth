package com.technowavegroup.printerlib;

import android.bluetooth.BluetoothDevice;

public interface ConveyorListener {

    void onDeviceConnected(boolean isConnected, String statusMessage, BluetoothDevice bluetoothDevice);

    void onMotorDriveState(boolean isDriven, String motorStatus);

    void onDeviceDisconnected(boolean isDisconnected, String statusMessage);

    void onDeviceError(String errorMessage);
}
