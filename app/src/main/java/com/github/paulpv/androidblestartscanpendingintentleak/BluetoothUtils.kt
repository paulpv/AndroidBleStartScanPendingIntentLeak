package com.github.paulpv.androidblestartscanpendingintentleak

import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings

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

fun scanCallbackErrorToString(value: Int, defaultValue: String): String {
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
