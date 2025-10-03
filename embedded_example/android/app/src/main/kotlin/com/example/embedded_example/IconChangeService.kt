package com.example.embedded_example

import android.app.*
import android.content.Intent
import android.content.Context
import android.os.Build
import android.os.IBinder
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import java.util.*

/**
 * Foreground Service for handling icon changes with Exact Alarms
 */
class IconChangeService : Service() {
    companion object {
        private const val TAG = "[IconChangeService]"
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "IconChangeChannel"
        private const val ACTION_START = "com.example.embedded_example.START_ICON_CHANGE_SERVICE"
        private const val ACTION_STOP = "com.example.embedded_example.STOP_ICON_CHANGE_SERVICE"
        private const val ACTION_SCHEDULE_ICON_CHANGE = "com.example.embedded_example.SCHEDULE_ICON_CHANGE"
        
        // Extra keys for intent extras
        const val EXTRA_TARGET_ICON = "target_icon"
        const val EXTRA_SCHEDULE_TIME = "schedule_time"
        
        fun createStartIntent(context: Context, targetIcon: String, scheduleTime: Long): Intent {
            return Intent(context, IconChangeService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_TARGET_ICON, targetIcon)
                putExtra(EXTRA_SCHEDULE_TIME, scheduleTime)
            }
        }
        
        fun createStopIntent(context: Context): Intent {
            return Intent(context, IconChangeService::class.java).apply {
                action = ACTION_STOP
            }
        }
    }
    
    private lateinit var iconManager: IconManager
    private lateinit var alarmManager: AlarmManager
    private val handler = Handler(Looper.getMainLooper())
    
    override fun onCreate() {
        super.onCreate()
        iconManager = IconManager(this)
        alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        
        createNotificationChannel()
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val targetIcon = intent.getStringExtra(EXTRA_TARGET_ICON)
                val scheduleTime = intent.getLongExtra(EXTRA_SCHEDULE_TIME, 0)
                
                if (targetIcon != null && scheduleTime > 0) {
                    scheduleIconChange(targetIcon, scheduleTime)
                }
            }
            ACTION_STOP -> {
                stopSelf()
            }
        }
        
        // Start foreground service to keep it running
        startForeground(NOTIFICATION_ID, createNotification())
        
        return START_STICKY // Restart if killed
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Icon Change Service")
            .setContentText("Managing scheduled icon changes")
            .setSmallIcon(R.drawable.iconone) // Use your app icon
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setOngoing(true)
            .build()
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Icon Change Service",
                NotificationManager.IMPORTANCE_MIN
            ).apply {
                description = "Channel for icon change service notifications"
            }
            
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    /**
     * Schedule an icon change using exact alarm
     */
    private fun scheduleIconChange(targetIcon: String, scheduleTime: Long) {
        try {
            val intent = Intent(this, IconChangeReceiver::class.java).apply {
                action = IconChangeReceiver.ACTION_ICON_CHANGE
                putExtra(IconChangeReceiver.EXTRA_TARGET_ICON, targetIcon)
                putExtra(IconChangeReceiver.EXTRA_SCHEDULE_TIME, scheduleTime)
            }
            
            val pendingIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                PendingIntent.getBroadcast(
                    this,
                    targetIcon.hashCode(), // Unique request code
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            } else {
                PendingIntent.getBroadcast(
                    this,
                    targetIcon.hashCode(),
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT
                )
            }
            
            // Schedule the exact alarm
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    scheduleTime,
                    pendingIntent
                )
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    scheduleTime,
                    pendingIntent
                )
            }
            
            Log.d(TAG, "Scheduled exact alarm for icon change to $targetIcon at ${Date(scheduleTime)}")
        } catch (e: Exception) {
            Log.e(TAG, "Error scheduling icon change: ${e.message}", e)
        }
    }
    
    /**
     * Cancel a scheduled icon change
     */
    fun cancelScheduledIconChange(targetIcon: String) {
        val intent = Intent(this, IconChangeReceiver::class.java).apply {
            action = IconChangeReceiver.ACTION_ICON_CHANGE
            putExtra(IconChangeReceiver.EXTRA_TARGET_ICON, targetIcon)
        }
        
        val pendingIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.getBroadcast(
                this,
                targetIcon.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        } else {
            PendingIntent.getBroadcast(
                this,
                targetIcon.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT
            )
        }
        
        alarmManager.cancel(pendingIntent)
        Log.d(TAG, "Cancelled scheduled icon change for $targetIcon")
    }
    
    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "IconChangeService destroyed")
    }
}