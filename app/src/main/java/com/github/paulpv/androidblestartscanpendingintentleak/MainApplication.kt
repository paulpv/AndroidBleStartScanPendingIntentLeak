package com.github.paulpv.androidblestartscanpendingintentleak

import android.app.Application

class MainApplication : Application() {

    private lateinit var _bleScannerManager: BleScannerManager
    var bleScannerManager: BleScannerManager
        get() = _bleScannerManager
        private set(value) {
            _bleScannerManager = value
        }

    override fun onCreate() {
        super.onCreate()
        bleScannerManager = BleScannerManager(this)
    }
}