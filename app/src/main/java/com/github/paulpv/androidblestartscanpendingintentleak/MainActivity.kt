package com.github.paulpv.androidblestartscanpendingintentleak

import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.bluetooth.BluetoothAdapter
import android.bluetooth.le.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.livinglifetechway.quickpermissions_kotlin.runWithPermissions
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    class BleScannerReceiver(private val mainActivity: MainActivity) : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val action = intent?.action
            if (ACTION_BLE_SCAN != action) {
                return
            }

            val errorCode = intent.getIntExtra(BluetoothLeScanner.EXTRA_ERROR_CODE, -1)
            if (errorCode != -1) {
                mainActivity.onScanFailed("PendingIntent", errorCode)
                return
            }

            val scanResults =
                intent.getParcelableArrayListExtra<ScanResult>(BluetoothLeScanner.EXTRA_LIST_SCAN_RESULT) ?: return

            val callbackType = intent.getIntExtra(BluetoothLeScanner.EXTRA_CALLBACK_TYPE, -1)
            if (callbackType == -1) {
                mainActivity.onBatchScanResults("PendingIntent", scanResults)
            } else {
                for (scanResult in scanResults) {
                    mainActivity.onScanResult("PendingIntent", callbackType, scanResult)
                }
            }
        }

        companion object {
            const val ACTION_BLE_SCAN = "com.github.paulpv.androidblestartscanpendingintentleak.ACTION_BLE_SCAN"

            private fun newIntent(context: Context): Intent {
                val intent = Intent(context, BleScannerReceiver::class.java)
                intent.action = ACTION_BLE_SCAN
                return intent
            }

            fun getBroadcast(context: Context, requestCode: Int): PendingIntent {
                return PendingIntent.getBroadcast(
                    context,
                    requestCode,
                    newIntent(context),
                    PendingIntent.FLAG_UPDATE_CURRENT
                )
            }
        }
    }

    private lateinit var btAdapter: BluetoothAdapter
    private lateinit var bleScanner: BluetoothLeScanner
    private lateinit var bleScannerScanCallback: ScanCallback
    private lateinit var bleScannerPendingIntent: PendingIntent

    private var bleScannerStartScanFirstTimeMillis: Long? = null
    private var bleScannerStartScanCount: Int = 0

    private val scanFilter: List<ScanFilter>
    private val scanSettings: ScanSettings

    init {
        scanFilter = mutableListOf()
        scanFilter.add(ScanFilter.Builder().build())

        val builder = ScanSettings.Builder()

        builder.setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
        builder.setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
        builder.setReportDelay(1000)
        builder.setNumOfMatches(ScanSettings.MATCH_NUM_MAX_ADVERTISEMENT)
        builder.setMatchMode(ScanSettings.MATCH_MODE_AGGRESSIVE)
        builder.setLegacy(false)
        builder.setPhy(ScanSettings.PHY_LE_ALL_SUPPORTED)

        //
        // BluetoothLeScannerCompat specific BEGIN
        //
        ////builder.setUseHardwareFilteringIfSupported(false);
        //builder.setUseHardwareBatchingIfSupported(true)
        ////builder.setUseHardwareCallbackTypesIfSupported(false);
        ////builder.setMatchOptions(ScanSettings.MATCH_LOST_DEVICE_TIMEOUT_DEFAULT, ScanSettings.MATCH_LOST_TASK_INTERVAL_DEFAULT);
        ////builder.setPowerSave(0, 0);
        //
        // BluetoothLeScannerCompat specific END
        //

        scanSettings = builder.build()
    }

    @SuppressLint("MissingPermission")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        btAdapter = BluetoothAdapter.getDefaultAdapter()
        bleScanner = btAdapter.bluetoothLeScanner

        bleScannerScanCallback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult?) {
                this@MainActivity.onScanResult("ScanCallback", callbackType, result)
            }

            override fun onBatchScanResults(results: MutableList<ScanResult>?) {
                this@MainActivity.onBatchScanResults("ScanCallback", results)
            }

            override fun onScanFailed(errorCode: Int) {
                this@MainActivity.onScanFailed("ScanCallback", errorCode)
            }
        }

        bleScannerPendingIntent = BleScannerReceiver.getBroadcast(this, REQUEST_CODE_BLE_SCANNER_PENDING_INTENT)

        buttonPendingIntentScanStart.setOnClickListener {
            scan(ScanMode.PendingIndent, true)
        }

        buttonPendingIntentScanStop.setOnClickListener {
            scan(ScanMode.PendingIndent, false)
        }

        buttonScanCallbackScanStart.setOnClickListener {
            scan(ScanMode.ScanCallback, true)
        }

        buttonScanCallbackScanStop.setOnClickListener {
            scan(ScanMode.ScanCallback, false)
        }

        buttonResetCounters.setOnClickListener {
            resetCounters()
        }

        buttonBluetoothEnable.setOnClickListener {
            btAdapter.enable()
        }
        buttonBluetoothDisable.setOnClickListener {
            btAdapter.disable()
        }
    }

    enum class ScanMode {
        PendingIndent,
        ScanCallback
    }

    private fun scan(mode: ScanMode, on: Boolean) = runWithPermissions(Manifest.permission.ACCESS_FINE_LOCATION) {
        when (mode) {
            ScanMode.PendingIndent -> {
                if (on) {
                    bleScanner.startScan(scanFilter, scanSettings, bleScannerPendingIntent)
                    incrementCounters()
                } else {
                    bleScanner.stopScan(bleScannerPendingIntent)
                }

            }
            ScanMode.ScanCallback -> {
                if (on) {
                    bleScanner.startScan(scanFilter, scanSettings, bleScannerScanCallback)
                    incrementCounters()
                } else {
                    bleScanner.stopScan(bleScannerScanCallback)
                }
            }
        }
    }

    fun onScanResult(caller: String, callbackType: Int, scanResult: ScanResult?) {
        Log.v(
            TAG, "#RAWSCAN $caller->onScanResult(" + quote(caller) +
                    ", callbackType=" + callbackTypeToString(callbackType) +
                    ", scanResult=" + toString(scanResult) + ')'
        )
    }

    fun onBatchScanResults(caller: String, scanResults: MutableList<ScanResult>?) {
//        String msg = "onBatchScanResults(scanResults:size()=" + scanResults.size() + ")";
//        try
//        {
//            PbLog.v(TAG, "+" + msg);
        if (scanResults != null) {
            for (scanResult in scanResults) {
                onScanResult("$caller->onBatchScanResults", ScanSettings.CALLBACK_TYPE_ALL_MATCHES, scanResult)
            }
        }
//        }
//        finally
//        {
//            PbLog.v(TAG, "-" + msg);
//        }
    }

    fun onScanFailed(caller: String, errorCode: Int) {
        Log.e(TAG, "$caller->onScanFailed(errorCode=" + scanCallbackErrorToString(errorCode) + ')')
    }

    private fun resetCounters() {
        bleScannerStartScanFirstTimeMillis = null
        bleScannerStartScanCount = 0
        invalidateCounters()
    }

    private fun incrementCounters() {
        if (bleScannerStartScanFirstTimeMillis == null) {
            bleScannerStartScanFirstTimeMillis = SystemClock.uptimeMillis()
        }
        val nowMillis = SystemClock.uptimeMillis()
        val elapsedMillisSinceBleScannerStartScanFirstTime = nowMillis - bleScannerStartScanFirstTimeMillis!!
        Log.e(
            TAG,
            "#GATT bleScannerStartScanCount=${++bleScannerStartScanCount}, elapsedMillisSinceBleScannerStartScanFirstTime=$elapsedMillisSinceBleScannerStartScanFirstTime"
        )
        invalidateCounters()
    }

    private fun invalidateCounters() {
        textCount.text = bleScannerStartScanCount.toString()
        //... = bleScannerStartScanFirstTimeMillis.toString()
    }

    companion object {
        const val TAG = "MainActivity"

        val REQUEST_CODE_BLE_SCANNER_PENDING_INTENT = 1000
    }
}
