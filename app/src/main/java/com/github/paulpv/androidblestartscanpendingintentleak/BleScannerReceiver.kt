package com.github.paulpv.androidblestartscanpendingintentleak

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BleScannerReceiver : BroadcastReceiver() {
    companion object {
        const val ACTION_BLE_SCAN = "com.github.paulpv.androidblestartscanpendingintentleak.ACTION_BLE_SCAN"

        private fun newIntent(context: Context): Intent {
            val intent = Intent(
                context,
                BleScannerReceiver::class.java
            )
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

    private var bleScannerManager: BleScannerManager? = null

    override fun onReceive(context: Context?, intent: Intent?) {
        val action = intent?.action
        if (ACTION_BLE_SCAN != action) {
            return
        }

        if (bleScannerManager == null) {
            val applicationContext = context?.applicationContext
            if (applicationContext is MainApplication) {
                bleScannerManager = applicationContext.bleScannerManager
            }
        }

        bleScannerManager?.onScanReceived(context, intent)
    }
}