package com.atharok.screentime.common.extensions

import android.app.AppOpsManager
import android.os.Build

fun AppOpsManager.checkOpNoThrowCompat(op: String, uid: Int, packageName: String): Int {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        unsafeCheckOpNoThrow(op, uid, packageName)
    } else {
        @Suppress("DEPRECATION")
        checkOpNoThrow(op, uid, packageName)
    }
}
