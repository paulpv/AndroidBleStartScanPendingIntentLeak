package com.github.paulpv.androidblestartscanpendingintentleak

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.SystemClock
import android.util.Log
import no.nordicsemi.android.support.v18.scanner.*

class BleScannerManager(private var context: Context) {

    companion object {
        const val TAG = "BleScannerManager"

        const val REQUEST_CODE_BLE_SCANNER_PENDING_INTENT = 1000

        private fun isBluetoothSupported(context: Context): Boolean {
            return context.packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH)
        }

        private fun getBluetoothManager(context: Context): BluetoothManager? {
            return if (!isBluetoothSupported(context)) {
                null
            } else {
                context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
            }
        }

        @SuppressLint("ObsoleteSdkInt")
        fun getBluetoothAdapter(context: Context): BluetoothAdapter? {
            if (!isBluetoothSupported(context)) {
                return null
            }

            return if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                BluetoothAdapter.getDefaultAdapter()
            } else {
                getBluetoothManager(context)!!.adapter
            }
        }

        fun callbackTypeToString(callbackType: Int): String {
            val s = when (callbackType) {
                ScanSettings.CALLBACK_TYPE_ALL_MATCHES -> "CALLBACK_TYPE_ALL_MATCHES("
                ScanSettings.CALLBACK_TYPE_FIRST_MATCH -> "CALLBACK_TYPE_FIRST_MATCH("
                ScanSettings.CALLBACK_TYPE_MATCH_LOST -> "CALLBACK_TYPE_MATCH_LOST("
                else -> "UNKNOWN("
            }
            return "$s$callbackType)"
        }

        fun scanCallbackErrorToString(value: Int): String {
            return scanCallbackErrorToString(value, "UNKNOWN")
        }

        private fun scanCallbackErrorToString(value: Int, defaultValue: String): String {
            val name = when (value) {
                ScanCallback.SCAN_FAILED_ALREADY_STARTED -> "SCAN_FAILED_ALREADY_STARTED"
                ScanCallback.SCAN_FAILED_APPLICATION_REGISTRATION_FAILED -> "SCAN_FAILED_APPLICATION_REGISTRATION_FAILED"
                ScanCallback.SCAN_FAILED_INTERNAL_ERROR -> "SCAN_FAILED_INTERNAL_ERROR"
                ScanCallback.SCAN_FAILED_FEATURE_UNSUPPORTED -> "SCAN_FAILED_FEATURE_UNSUPPORTED"
                else -> defaultValue
            }
            return "$name($value)"
        }

        fun toString(scanResult: ScanResult?): String {
            return scanResult?.toString() ?: "null"
        }
    }

    val btAdapter: BluetoothAdapter? = getBluetoothAdapter(context)

    private val bleScanner: BluetoothLeScannerCompat? = BluetoothLeScannerCompat.getScanner()
    //private val bleScanner: BluetoothLeScanner? = btAdapter?.bluetoothLeScanner

    private val bleScannerScanCallback: ScanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            this@BleScannerManager.onScanResult("ScanCallback", callbackType, result)
        }

        override fun onBatchScanResults(results: MutableList<ScanResult>) {
            this@BleScannerManager.onBatchScanResults("ScanCallback", results)
        }

        override fun onScanFailed(errorCode: Int) {
            this@BleScannerManager.onScanFailed("ScanCallback", errorCode)
        }
    }

    private val bleScannerPendingIntent: PendingIntent = BleScannerReceiver.getBroadcast(
        context,
        REQUEST_CODE_BLE_SCANNER_PENDING_INTENT
    )

    private var bleScannerStartScanFirstTimeMillis: Long? = null
    var bleScannerStartScanCount: Int = 0
        private set(value) {
            field = value
        }

    private val scanFilter: List<ScanFilter>
    private val scanSettings: ScanSettings

    init {
        //
        // ScanFilter
        //

        scanFilter = mutableListOf()
        scanFilter.add(ScanFilter.Builder().build())

        //
        // ScanSettings
        //

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

    fun resetCounters() {
        bleScannerStartScanFirstTimeMillis = null
        bleScannerStartScanCount = 0
        //invalidateCounters()
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
        //invalidateCounters()
    }

    enum class ScanMode {
        PendingIndent,
        ScanCallback
    }

    fun scan(mode: ScanMode, on: Boolean) {
        when (mode) {
            ScanMode.PendingIndent -> {
                if (on) {
                    Log.v(TAG, "scan: #GATT +bleScanner.startScan(...)")
                    val result = bleScanner?.startScan(scanFilter, scanSettings, context, bleScannerPendingIntent) ?: 0
                    //val result = bleScanner?.startScan(scanFilter, scanSettings, bleScannerPendingIntent) ?: 0
                    Log.v(
                        TAG,
                        "scan: #GATT bleScanner.startScan(...); result=${scanCallbackErrorToString(result, "SUCCESS")}"
                    )
                    Log.v(TAG, "scan: #GATT -bleScanner.startScan(...)")
                    incrementCounters()
                } else {
                    Log.v(TAG, "scan: #GATT +bleScanner.stopScan(...)")
                    bleScanner?.stopScan(context, bleScannerPendingIntent)
                    //bleScanner?.stopScan(bleScannerPendingIntent)
                    Log.v(TAG, "scan: #GATT -bleScanner.stopScan(...)")
                }

            }
            ScanMode.ScanCallback -> {
                if (on) {
                    Log.v(TAG, "scan: #GATT +bleScanner.startScan(...)")
                    bleScanner?.startScan(scanFilter, scanSettings, bleScannerScanCallback)
                    Log.v(TAG, "scan: #GATT -bleScanner.startScan(...)")
                    incrementCounters()
                } else {
                    Log.v(TAG, "scan: #GATT +bleScanner.stopScan(...)")
                    bleScanner?.stopScan(bleScannerScanCallback)
                    Log.v(TAG, "scan: #GATT -bleScanner.stopScan(...)")
                }
            }
        }
    }

    fun onScanReceived(context: Context?, intent: Intent) {
        val errorCode = intent.getIntExtra(BluetoothLeScannerCompat.EXTRA_ERROR_CODE, -1)
        //val errorCode = intent.getIntExtra(BluetoothLeScanner.EXTRA_ERROR_CODE, -1)
        if (errorCode != -1) {
            onScanFailed("PendingIntent", errorCode)
            return
        }

        val scanResults =
            intent.getParcelableArrayListExtra<ScanResult>(BluetoothLeScannerCompat.EXTRA_LIST_SCAN_RESULT)
            //intent.getParcelableArrayListExtra<ScanResult>(BluetoothLeScanner.EXTRA_LIST_SCAN_RESULT)
                ?: return

        val callbackType = intent.getIntExtra(BluetoothLeScannerCompat.EXTRA_CALLBACK_TYPE, -1)
        //val callbackType = intent.getIntExtra(BluetoothLeScanner.EXTRA_CALLBACK_TYPE, -1)
        if (callbackType == -1) {
            onBatchScanResults("PendingIntent", scanResults)
        } else {
            for (scanResult in scanResults) {
                onScanResult("PendingIntent", callbackType, scanResult)
            }
        }
    }

    fun onScanResult(caller: String, callbackType: Int, scanResult: ScanResult?) {
        Log.v(
            TAG,
            "#RAWSCAN $caller->onScanResult(${quote(caller)}, callbackType=${callbackTypeToString(callbackType)}, scanResult=${toString(
                scanResult
            )})"
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

}