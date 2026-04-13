package com.example.screencoach

import android.app.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import androidx.core.app.NotificationCompat

class ScreenMonitorService : Service() {

    private val CHANNEL_ID = "screen_monitor_channel"
    private val NUDGE_CHANNEL_ID = "screen_nudge_channel_v4"
    private val FOREGROUND_ID = 1
    private val NUDGE_NOTIFICATION_ID = 2
    private val PREFS_NAME = "ScreenCoachPrefs"
    
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var internalReceiver: BroadcastReceiver
    private var screenStartTime = SystemClock.elapsedRealtime()
    private var currentIndex = 0

    private val checkRunnable = object : Runnable {
        override fun run() {
            val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
            val isLocked = keyguardManager.isDeviceLocked

            if (!isLocked && screenStartTime == 0L) {
                screenStartTime = SystemClock.elapsedRealtime()
            }

            if (screenStartTime != 0L && !isLocked) {
                val elapsedSeconds = (SystemClock.elapsedRealtime() - screenStartTime) / 1000
                Log.d("ScreenCoach", "Tick - Active seconds: $elapsedSeconds")

                if (elapsedSeconds >= 600) {
                    val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    
                    val pauseNotification = NotificationCompat.Builder(this@ScreenMonitorService, NUDGE_CHANNEL_ID)
                        .setContentTitle("Gentle Reminder")
                        .setContentText("Pause. Calm-down. Breathe. Stretch. Focus.")
                        .setSmallIcon(R.drawable.ic_notification)
                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                        .setDefaults(Notification.DEFAULT_VIBRATE)
                        .setAutoCancel(true)
                        .build()
                    manager.notify(3, pauseNotification)

                    handler.postDelayed({
                        if (!keyguardManager.isDeviceLocked) {
                            manager.cancel(3)
                            triggerNudgeUpdate()
                        }
                    }, 5000)
                    
                    screenStartTime = SystemClock.elapsedRealtime()
                }
            }
            handler.postDelayed(this, 2000)
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
        
        // Correctly start foreground with service type for API 34+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                FOREGROUND_ID, 
                createForegroundNotification(), 
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            startForeground(FOREGROUND_ID, createForegroundNotification())
        }
        
        internalReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                when (intent.action) {
                    Intent.ACTION_USER_PRESENT -> {
                        screenStartTime = SystemClock.elapsedRealtime()
                    }
                    Intent.ACTION_SCREEN_OFF -> {
                        screenStartTime = 0L
                    }
                }
            }
        }
        
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_USER_PRESENT)
            addAction(Intent.ACTION_SCREEN_OFF)
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(internalReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(internalReceiver, filter)
        }

        handler.post(checkRunnable)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    override fun onDestroy() {
        handler.removeCallbacks(checkRunnable)
        try { unregisterReceiver(internalReceiver) } catch (e: Exception) {}
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java) ?: return
            
            // FIX: Create the missing background monitor channel
            if (manager.getNotificationChannel(CHANNEL_ID) == null) {
                val monitorChannel = NotificationChannel(
                    CHANNEL_ID,
                    "Screen Monitoring Status",
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = "Shows that Screen Coach is active and monitoring usage."
                }
                manager.createNotificationChannel(monitorChannel)
            }

            if (manager.getNotificationChannel(NUDGE_CHANNEL_ID) == null) {
                val nudgeChannel = NotificationChannel(
                    NUDGE_CHANNEL_ID,
                    "Health Reminders",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    setSound(null, null)
                    enableVibration(true)
                }
                manager.createNotificationChannel(nudgeChannel)
            }
        }
    }

    private fun createForegroundNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Screen Coach Active")
            .setContentText("Monitoring screen usage")
            .setSmallIcon(R.drawable.ic_notification)
            .setOngoing(true)
            .build()
    }

    private fun triggerNudgeUpdate() {
        val (impact, action) = NudgeMessages.messages[currentIndex]
        val actionText = "Recommendation: $action"

        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val editor = prefs.edit()
        val lastImpact = prefs.getString("current_impact", null)
        val lastAction = prefs.getString("current_action", null)
        if (lastImpact != null) {
            editor.putString("prev_impact", lastImpact)
            editor.putString("prev_action", lastAction)
        }
        editor.putString("current_impact", impact)
        editor.putString("current_action", actionText)
        editor.apply()

        val uiIntent = Intent("SCREEN_TIME_EVENT")
        uiIntent.putExtra("impact", impact)
        uiIntent.putExtra("action", actionText)
        sendBroadcast(uiIntent)

        showNudgeNotification("$impact $actionText")
        currentIndex = (currentIndex + 1) % NudgeMessages.messages.size
    }

    private fun showNudgeNotification(message: String) {
        val appIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val appPendingIntent = PendingIntent.getActivity(
            this, 0, appIntent, 
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, NUDGE_CHANNEL_ID)
            .setContentTitle("Screen Coach")
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setSmallIcon(R.drawable.ic_notification)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(Notification.DEFAULT_VIBRATE)
            .setAutoCancel(true)
            .setContentIntent(appPendingIntent)
            .build()

        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NUDGE_NOTIFICATION_ID, notification)
    }
}
