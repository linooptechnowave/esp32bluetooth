package com.technowavegroup.printerlib;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Handler;
import android.os.ParcelUuid;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class BTUtil {
    public static final char CONVEYOR_START = 'a';
    public static final char CONVEYOR_STOP = 'b';
    public static final char CONVEYOR_FORWARD = 'c';
    public static final char CONVEYOR_REVERSE = 'd';
    public static final char BUZZ_SUCCESS = 'e';
    public static final char BUZZ_ERROR = 'f';
    public static final char POWER_ON = 'g';
    public static final char POWER_OFF = 'h';
    public static final char RESET = 'i';
    public static final int BT_ENABLE_REQUEST_CODE = 100;
    public static final String DEVICE_UUID = "00001101-0000-1000-8000-00805f9b34fb";
    @SuppressLint("StaticFieldLeak")
    private static BTUtil btUtilInstance;
    private final Activity activity;
    private final List<BluetoothDevice> paredDevices = new ArrayList<>();
    private BluetoothSocket bluetoothSocket;
    private OutputStream outputStream;
    private InputStream inputStream;
    private BluetoothDevice bluetoothDevice = null;
    private final String defaultDeviceMac;


    private boolean driveActionSuccess;
    private boolean isConnected = false;
    private Handler handler;
    private int readBufferPosition;
    private volatile boolean stopWorker;

    private final BTListener BTListener;

    /*public static synchronized BTUtil getInstance(Activity activity, PrintStatusListener printStatusListener) {
        if (btUtilInstance == null) {
            btUtilInstance = new BTUtil(activity, printStatusListener);
        }
        return btUtilInstance;
    }*/

    public BTUtil(Activity activity, BTListener BTListener) {
        this.activity = activity;
        this.BTListener = BTListener;
        defaultDeviceMac = BTPrefManager.getInstance(activity).getDeviceMacAddress();
        findBTDevices();
    }

    public void findBTDevices() {
        try {
            BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            if (bluetoothAdapter == null) {
                BTListener.onDeviceError("Bluetooth adapter not found!");
            }
            //assert bluetoothAdapter != null;
            if (!bluetoothAdapter.isEnabled()) {
                Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                activity.startActivityForResult(intent, BT_ENABLE_REQUEST_CODE);
            }
            Set<BluetoothDevice> availParedDevices = bluetoothAdapter.getBondedDevices();
            if (availParedDevices.size() > 0) {
                for (BluetoothDevice device : availParedDevices) {
                    ParcelUuid[] uuids = device.getUuids();
                    /*for (ParcelUuid uuid : uuids) {
                        if (uuid.getUuid().toString().equals(PRINTER_UUID)) {
                            paredDevices.add(device);
                            break;
                        }
                    }*/
                    paredDevices.add(device);
                    if (defaultDeviceMac.equals(device.getAddress())) {
                        bluetoothDevice = device;
                    }
                }
            } else {
                BTListener.onDeviceError("No bluetooth device available!");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void chooseBTDeviceDialog() {
        BTDeviceListDialog btDeviceListDialog = new BTDeviceListDialog(activity, paredDevices, device -> {
            bluetoothDevice = device;
            connectBTDevice();
        });
        btDeviceListDialog.show();
    }

    public void connectBTDevice() {
        /*if (defaultPrinterMac.equals("")) {
            printStatusListener.onDeviceConnected(false, "Failed to get default printer!", bluetoothDevice);
            return;
        }*/
        handler = new Handler();
        ProgressDialog progressDialog = new ProgressDialog(activity);
        progressDialog.showProgress("Please wait", bluetoothDevice != null ? "Connecting to " + bluetoothDevice.getName() : "Bluetooth device error");
        progressDialog.show();
        new Thread(() -> {
            try {
                if (bluetoothDevice != null) {
                    //Old code
                    UUID uuid = UUID.fromString(DEVICE_UUID);
                    bluetoothSocket = bluetoothDevice.createRfcommSocketToServiceRecord(uuid);
                    bluetoothSocket.connect();
                    outputStream = bluetoothSocket.getOutputStream();
                    inputStream = bluetoothSocket.getInputStream();

                    /*
                     * New code
                     * */
                    /*connection = new BluetoothConnectionInsecure(bluetoothDevice.getAddress());
                    if (Looper.myLooper() == null) {
                        Looper.prepare();
                    }
                    connection.open();*/
                    isConnected = true;
                    beginListenForData();
                } else {
                    isConnected = false;
                }
                progressDialog.dismissProgress();
            } catch (Exception e) {
                e.printStackTrace();
                isConnected = false;
                progressDialog.dismissProgress();
            } finally {
                handler.post(() -> {
                    if (isConnected) {
                        BTListener.onDeviceConnected(true, "Device connected successfully", bluetoothDevice);
                        BTPrefManager.getInstance(activity).saveDeviceMacAddress(bluetoothDevice.getAddress());
                    } else {
                        BTListener.onDeviceConnected(false, "Failed to connect device!", bluetoothDevice);
                    }
                });
            }
        }).start();
    }

    public void towerLight(char command) {
        handler = new Handler();
        if (bluetoothDevice != null && isConnected) {
            new Thread(() -> {
                driveActionSuccess = false;
                try {
                    //Old codes
                    outputStream.write(String.valueOf(command).getBytes());
                    //connection.write(text.getBytes());
                    driveActionSuccess = true;
                } catch (Exception e) {
                    e.printStackTrace();
                    driveActionSuccess = false;
                } finally {
                    handler.post(() -> {
                        if (driveActionSuccess)
                            BTListener.onMotorDriveState(true, "Action completed");
                        else
                            BTListener.onMotorDriveState(false, "Operation failure!!!");
                    });
                }
            }).start();
        } else {
            BTListener.onMotorDriveState(false, "Failed to connect device!");
        }
    }

    public void beginListenForData() {
        // final Handler handler = new Handler();
        final byte delimiter = 10; //This is the ASCII code for a newline character

        stopWorker = false;
        readBufferPosition = 0;
        byte[] readBuffer = new byte[1024];
        new Thread(() -> {
            while (!Thread.currentThread().isInterrupted() && !stopWorker) {
                try {
                    int bytesAvailable = inputStream.available();
                    if (bytesAvailable > 0) {
                        byte[] packetBytes = new byte[bytesAvailable];
                        inputStream.read(packetBytes);
                        for (int i = 0; i < bytesAvailable; i++) {
                            byte b = packetBytes[i];
                            if (b == delimiter) {
                                byte[] encodedBytes = new byte[readBufferPosition];
                                System.arraycopy(readBuffer, 0, encodedBytes, 0, encodedBytes.length);
                                final String data = new String(encodedBytes, "US-ASCII");
                                readBufferPosition = 0;

                                handler.post(() -> {
                                    //Received Data
                                    Log.d("ReceivedData", data);
                                });
                            } else {
                                readBuffer[readBufferPosition++] = b;
                            }
                        }
                    }
                } catch (IOException ex) {
                    stopWorker = true;
                }
            }
        }).start();
    }

    public void disconnectBTDevice() {
        handler = new Handler();
        if (bluetoothDevice != null && isConnected) {
            new Thread(() -> {
                try {
                    //Old code
                    outputStream.close();
                    inputStream.close();
                    bluetoothSocket.close();
                    //New code
                    /*connection.close();
                    Looper.myLooper().quit();*/
                    isConnected = false;
                    Log.d("Device connection", "Device disconnected successfully");
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    handler.post(() -> {
                        if (isConnected) {
                            BTListener.onDeviceDisconnected(false, "Failed to disconnect!!");
                            Log.d("Device connection", "Failed to disconnect!!");
                        } else {
                            BTListener.onDeviceDisconnected(true, "Device disconnected successfully");
                        }
                    });
                }
            }).start();
        } else {
            BTListener.onDeviceDisconnected(false, "Device not found");
        }
    }
}
