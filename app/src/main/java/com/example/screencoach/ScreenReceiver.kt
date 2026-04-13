package com.example.screencoach

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class ScreenReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Log.d("RECEIVER_TEST", "ScreenReceiver triggered: ${intent.action}")

        when (intent.action) {
            Intent.ACTION_SCREEN_ON -> {
                Log.d("RECEIVER_TEST", "Screen turned ON")
                ScreenMonitor.startSession(context)
                Log.d("RECEIVER_TEST", "Is the ScreenMonitor.startSession(context) called?")
            }
            Intent.ACTION_SCREEN_OFF -> {
                Log.d("RECEIVER_TEST", "Screen turned OFF")
                ScreenMonitor.endSession(context)
            }
        }
    }
}


