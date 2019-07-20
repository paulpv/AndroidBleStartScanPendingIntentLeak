package com.github.paulpv.androidblestartscanpendingintentleak

import android.Manifest
import android.annotation.SuppressLint
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.livinglifetechway.quickpermissions_kotlin.runWithPermissions
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    lateinit var bleScannerManager: BleScannerManager

    @SuppressLint("MissingPermission")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val applicationMain = applicationContext as MainApplication
        bleScannerManager = applicationMain.bleScannerManager

        buttonPendingIntentScanStart.setOnClickListener {
            scan(BleScannerManager.ScanMode.PendingIndent, true)
        }

        buttonPendingIntentScanStop.setOnClickListener {
            scan(BleScannerManager.ScanMode.PendingIndent, false)
        }

        buttonScanCallbackScanStart.setOnClickListener {
            scan(BleScannerManager.ScanMode.ScanCallback, true)
        }

        buttonScanCallbackScanStop.setOnClickListener {
            scan(BleScannerManager.ScanMode.ScanCallback, false)
        }

        buttonResetCounters.setOnClickListener {
            bleScannerManager.resetCounters()
            invalidateCounters()
        }

        buttonBluetoothEnable.setOnClickListener {
            bleScannerManager.btAdapter?.enable()
        }
        buttonBluetoothDisable.setOnClickListener {
            bleScannerManager.btAdapter?.disable()
        }
    }

    private fun scan(mode: BleScannerManager.ScanMode, on: Boolean) =
        runWithPermissions(Manifest.permission.ACCESS_FINE_LOCATION) {
            bleScannerManager.scan(mode, on)
            invalidateCounters()
        }

    private fun invalidateCounters() {
        textCount.text = bleScannerManager.bleScannerStartScanCount.toString()
        //... = bleScannerStartScanFirstTimeMillis.toString()
    }

    companion object {
        const val TAG = "MainActivity"
    }
}
