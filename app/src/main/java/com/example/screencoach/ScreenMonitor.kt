package com.example.screencoach

import android.content.Context
import android.util.Log

object ScreenMonitor {

    private var sessionStartTime: Long = 0L
    private var isSessionActive = false

    fun startSession(context: Context) {
        Log.d("MONITOR_TEST", "startSession CALLED")
        sessionStartTime = System.currentTimeMillis()
        isSessionActive = true
    }

    fun endSession(context: Context) {
        isSessionActive = false
        val duration = System.currentTimeMillis() - sessionStartTime
        Log.d("ScreenMonitor", "Session ended. Duration: $duration ms")
    }
}
