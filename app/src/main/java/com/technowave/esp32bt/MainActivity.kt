package com.technowave.esp32bt

import android.bluetooth.BluetoothDevice
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.widget.*
import com.technowavegroup.printerlib.BTUtil
import com.technowavegroup.printerlib.BTUtil.CONVEYOR_START
import com.technowavegroup.printerlib.ConveyorListener

class MainActivity : AppCompatActivity(),
    ConveyorListener {
    private lateinit var btUtil: BTUtil
    private lateinit var message: EditText
    private lateinit var receivedMessage: TextView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        btUtil = BTUtil(this, this)

        receivedMessage = findViewById(R.id.receivedMessage)
        message = findViewById(R.id.message)
        findViewById<Button>(R.id.buzzOn).setOnClickListener {
            sendMessage('B')
        }

        findViewById<Button>(R.id.buzzOff).setOnClickListener {
            sendMessage('b')
        }

        findViewById<Button>(R.id.redOn).setOnClickListener {
            sendMessage('R')
        }

        findViewById<Button>(R.id.yellowOn).setOnClickListener {
            sendMessage('Y')
        }

        findViewById<Button>(R.id.yellowOff).setOnClickListener {
            sendMessage('y')
        }

        findViewById<Button>(R.id.redOff).setOnClickListener {
            sendMessage('r')
        }

        findViewById<Button>(R.id.greenOn).setOnClickListener {
            sendMessage('G')
        }

        findViewById<Button>(R.id.greenOff).setOnClickListener {
            sendMessage('g')
            //test
        }

        findViewById<Button>(R.id.chooseDevice).setOnClickListener {
            btUtil.chooseBTDeviceDialog()
        }
        findViewById<Button>(R.id.connect).setOnClickListener {
            btUtil.connectBTDevice()
        }
        findViewById<Button>(R.id.buttonDisconnect).setOnClickListener {
            btUtil.disconnectBTDevice()
        }

        findViewById<Button>(R.id.testDevice).setOnClickListener {
            flash()
        }

    }

    private fun flash() {

    }

    private fun sendMessage(message: Char) {
        btUtil.drive(message)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            BTUtil.BT_ENABLE_REQUEST_CODE -> {
                if (resultCode == RESULT_OK) {
                    btUtil.findBTDevices()
                } else {
                    showErrorMessage("Please enable bluetooth and restart application")
                }
            }
        }
    }

    override fun onDeviceConnected(
        isConnected: Boolean,
        statusMessage: String?,
        bluetoothDevice: BluetoothDevice?
    ) {
        showErrorMessage(statusMessage)
    }

    override fun onMotorDriveState(isDriven: Boolean, motorStatus: String?) {
        showErrorMessage(motorStatus)
    }

    override fun onDeviceDisconnected(isDisconnected: Boolean, statusMessage: String?) {
        showErrorMessage(statusMessage)
    }

    override fun onDeviceError(errorMessage: String?) {
        showErrorMessage(errorMessage)
    }

    private fun showErrorMessage(message: String?) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }
}