package com.dockeredly.app.util

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.os.Process

/** [Application.getProcessName] only exists on API 28+; below that, ask ActivityManager. */
object ProcessNameCompat {
    fun currentProcessName(context: Context): String {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            return android.app.Application.getProcessName()
        }
        val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        val pid = Process.myPid()
        return manager?.runningAppProcesses.orEmpty().firstOrNull { it.pid == pid }?.processName
            ?: context.packageName
    }
}
