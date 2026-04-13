package com.example.screencoach

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
//import com.sarvani.screencoach.ScreenMonitorService

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            // Start the foreground service
            val serviceIntent = Intent(context, ScreenMonitorService::class.java)
            ContextCompat.startForegroundService(context,serviceIntent)
        }
    }
}