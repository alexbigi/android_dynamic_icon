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
import java.util.concurrent.atomic.AtomicReference
import kotlin.concurrent.thread

/**
 * Foreground Service for handling icon changes with Exact Alarms
 * Now supports autonomous operation with holiday configuration
 */
class IconChangeService : Service() {
    companion object {
        private const val TAG = "[IconChangeService]"
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "IconChangeChannel"
        private const val ACTION_START_SUFFIX = ".START_ICON_CHANGE_SERVICE"
        private const val ACTION_STOP_SUFFIX = ".STOP_ICON_CHANGE_SERVICE"
        private const val ACTION_SCHEDULE_HOLIDAY_CONFIG_SUFFIX = ".SCHEDULE_HOLIDAY_CONFIG"
        
        // Extra keys for intent extras
        const val EXTRA_TARGET_ICON = "target_icon"
        const val EXTRA_SCHEDULE_TIME = "schedule_time"
        const val EXTRA_CONFIG_JSON = "config_json"
        
        fun getActionStart(context: Context): String {
            return context.packageName + ACTION_START_SUFFIX
        }
        
        fun getActionStop(context: Context): String {
            return context.packageName + ACTION_STOP_SUFFIX
        }
        
        fun getActionScheduleHolidayConfig(context: Context): String {
            return context.packageName + ACTION_SCHEDULE_HOLIDAY_CONFIG_SUFFIX
        }
        
        fun createStartIntent(context: Context, targetIcon: String, scheduleTime: Long): Intent {
            return Intent(context, IconChangeService::class.java).apply {
                action = getActionStart(context)
                putExtra(EXTRA_TARGET_ICON, targetIcon)
                putExtra(EXTRA_SCHEDULE_TIME, scheduleTime)
            }
        }
        
        fun createStopIntent(context: Context): Intent {
            return Intent(context, IconChangeService::class.java).apply {
                action = getActionStop(context)
            }
        }
        
        fun createScheduleHolidayConfigIntent(context: Context, config: IconChangeConfig): Intent {
            val configJson = serializeConfig(config) // В реальном приложении использовать proper JSON
            return Intent(context, IconChangeService::class.java).apply {
                action = getActionScheduleHolidayConfig(context)
                putExtra(EXTRA_CONFIG_JSON, configJson)
            }
        }
        
        // Метод для сериализации конфигурации (в реальном приложении использовать JSON)
        fun serializeConfig(config: IconChangeConfig): String {
            // В продакшене использовать proper JSON serialization (например, Gson или kotlinx.serialization)
            // Это упрощенная версия для демонстрации
            val holidaysData = config.holidays.map { holiday ->
                "${holiday.iconAlias}|${holiday.startDate.time}|${holiday.endDate?.time}|${holiday.name}"
            }.joinToString(";")
            
            return "${config.defaultIcon}|${config.checkIntervalSeconds}|$holidaysData"
        }
    }
    
    private lateinit var iconManager: IconManager
    private lateinit var alarmManager: AlarmManager
    private val handler = Handler(Looper.getMainLooper())
    
    // Хранение конфигурации
    private var currentConfig: IconChangeConfig? = null
    
    override fun onCreate() {
        super.onCreate()
        iconManager = IconManager(this)
        alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        
        createNotificationChannel()
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            getActionStart(this) -> {
                val targetIcon = intent.getStringExtra(EXTRA_TARGET_ICON)
                val scheduleTime = intent.getLongExtra(EXTRA_SCHEDULE_TIME, 0)
                
                if (targetIcon != null && scheduleTime > 0) {
                    scheduleIconChange(targetIcon, scheduleTime)
                }
            }
            getActionStop(this) -> {
                stopSelf()
            }
            getActionScheduleHolidayConfig(this) -> {
                val configJson = intent.getStringExtra(EXTRA_CONFIG_JSON)
                if (configJson != null) {
                    // Десериализуем конфигурацию и запускаем автономную работу
                    val config = deserializeConfig(configJson)
                    if (config != null) {
                        startAutonomousOperation(config)
                    }
                }
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
     * Start autonomous operation with holiday configuration
     */
    private fun startAutonomousOperation(config: IconChangeConfig) {
        currentConfig = config
        
        // Сохраняем конфигурацию в постоянное хранилище
        val app = applicationContext as? MainApplication
        app?.getConfigManager()?.saveConfig(config)
        
        Log.d(TAG, "Started autonomous operation with ${config.holidays.size} holidays, default: ${config.defaultIcon}")
        
        // Начинаем проверку дат немедленно
        checkAndChangeIconByDate()
        
        // Планируем будущие проверки
        scheduleNextDateCheck()
    }
    
    /**
     * Check dates and change icon if needed
     */
    private fun checkAndChangeIconByDate() {
        val config = currentConfig ?: run {
            Log.e(TAG, "No configuration available for date check")
            return
        }
        
        try {
            val currentDate = Date()
            var targetIcon: String? = null
            
            // Проверяем, попадает ли текущая дата под какой-либо праздник
            for (holiday in config.holidays) {
                if (holiday.isCurrentDayMonthInRange(currentDate)) {
                    targetIcon = holiday.iconAlias
                    Log.d(TAG, "Holiday matched: ${holiday.name}, icon: $targetIcon")
                    break
                }
            }
            
            // Если ни один праздник не подошел, используем иконку по умолчанию
            if (targetIcon == null) {
                targetIcon = config.defaultIcon
                Log.d(TAG, "No holiday matched, using default icon: $targetIcon")
            }
            
            // Проверяем, отличается ли текущая иконка от целевой
            val currentIcon = iconManager.getCurrentActiveIcon()
            if (currentIcon != targetIcon) {
                Log.d(TAG, "Current icon ($currentIcon) differs from target icon ($targetIcon) - checking app state")
                
                // Проверяем состояние приложения
                val isAppInForeground = isAppInForeground()
                
                if (isAppInForeground) {
                    Log.d(TAG, "App is in foreground - storing pending change to $targetIcon")
                    // Если приложение открыто, сохраняем задачу до следующего сворачивания
                    // Но для автономной работы - меняем иконку через сервис
                    val app = applicationContext as? MainApplication
                    app?.getDeferredIconChangeManager()?.setPendingIconChange(targetIcon)
                } else {
                    Log.d(TAG, "App is in background/killed - changing icon immediately to $targetIcon")
                    // Если приложение в фоне или убито, меняем иконку сразу
                    iconManager.changeIcon(targetIcon)
                }
            } else {
                Log.d(TAG, "Current icon is already $targetIcon, no change needed")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking date and changing icon: ${e.message}", e)
        }
    }
    
    /**
     * Check if the app is in foreground
     */
    private fun isAppInForeground(): Boolean {
        try {
            val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val appProcesses = activityManager.runningAppProcesses ?: return false
            
            for (appProcess in appProcesses) {
                if (appProcess.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND &&
                    packageName == appProcess.processName) {
                    return true
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking if app is in foreground: ${e.message}")
        }
        return false
    }
    
    /**
     * Schedule the next date check
     */
    private fun scheduleNextDateCheck() {
        val config = currentConfig ?: return
        val configJson = serializeConfig(config)  // используем текущую конфигурацию
        
        try {
            val calendar = Calendar.getInstance().apply {
                timeInMillis = System.currentTimeMillis()
                add(Calendar.DAY_OF_MONTH, 1) // следующий день
                set(Calendar.HOUR_OF_DAY, 0)   // полночь
                set(Calendar.MINUTE, 0)        // начало часа
                set(Calendar.SECOND, 0)        // начало минуты
                set(Calendar.MILLISECOND, 0)   // начало секунды
            }
            
            val intent = Intent(this, DateCheckReceiver::class.java).apply {
                action = DateCheckReceiver.getActionName(this@IconChangeService)
                putExtra(DateCheckReceiver.EXTRA_CONFIG_JSON, configJson)
            }
            
            val pendingIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                android.app.PendingIntent.getBroadcast(
                    this,
                    0,
                    intent,
                    android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
                )
            } else {
                android.app.PendingIntent.getBroadcast(
                    this,
                    0,
                    intent,
                    android.app.PendingIntent.FLAG_UPDATE_CURRENT
                )
            }
            
            // Планируем точный будильник
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    android.app.AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            } else {
                alarmManager.setExact(
                    android.app.AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            }
            
            Log.d(TAG, "Scheduled next date check at: ${calendar.time}")
        } catch (e: Exception) {
            Log.e(TAG, "Error scheduling next date check: ${e.message}", e)
        }
    }
    
    /**
     * Schedule an icon change using exact alarm
     */
    private fun scheduleIconChange(targetIcon: String, scheduleTime: Long) {
        try {
            val intent = Intent(this, IconChangeReceiver::class.java).apply {
                action = IconChangeReceiver.getActionName(this@IconChangeService)
                putExtra(IconChangeReceiver.EXTRA_TARGET_ICON, targetIcon)
                putExtra(IconChangeReceiver.EXTRA_SCHEDULE_TIME, scheduleTime)
            }
            
            val pendingIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                android.app.PendingIntent.getBroadcast(
                    this,
                    targetIcon.hashCode(), // Unique request code
                    intent,
                    android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
                )
            } else {
                android.app.PendingIntent.getBroadcast(
                    this,
                    targetIcon.hashCode(),
                    intent,
                    android.app.PendingIntent.FLAG_UPDATE_CURRENT
                )
            }
            
            // Schedule the exact alarm
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    android.app.AlarmManager.RTC_WAKEUP,
                    scheduleTime,
                    pendingIntent
                )
            } else {
                alarmManager.setExact(
                    android.app.AlarmManager.RTC_WAKEUP,
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
            action = IconChangeReceiver.getActionName(this@IconChangeService)
            putExtra(IconChangeReceiver.EXTRA_TARGET_ICON, targetIcon)
        }
        
        val pendingIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            android.app.PendingIntent.getBroadcast(
                this,
                targetIcon.hashCode(),
                intent,
                android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
            )
        } else {
            android.app.PendingIntent.getBroadcast(
                this,
                targetIcon.hashCode(),
                intent,
                android.app.PendingIntent.FLAG_UPDATE_CURRENT
            )
        }
        
        alarmManager.cancel(pendingIntent)
        Log.d(TAG, "Cancelled scheduled icon change for $targetIcon")
    }
    
    /**
     * Deserialize configuration from JSON string
     */
    private fun deserializeConfig(configJson: String): IconChangeConfig? {
        // В реальном приложении использовать proper JSON deserialization
        // Это упрощенная версия для демонстрации
        try {
            val parts = configJson.split("|")
            if (parts.size < 3) return null
            
            val defaultIcon = parts[0]
            val checkInterval = parts[1].toIntOrNull() ?: 60
            val holidaysData = parts.drop(2).joinToString("|").split(";")
            
            val holidays = holidaysData.filter { it.isNotEmpty() }.mapNotNull { holidayStr ->
                val holidayParts = holidayStr.split("|")
                if (holidayParts.size >= 4) {
                    val iconAlias = holidayParts[0]
                    val startDate = Date(holidayParts[1].toLong())
                    val endDate = if (holidayParts[2].isNotEmpty()) Date(holidayParts[2].toLong()) else null
                    val name = holidayParts[3]
                    
                    Holiday(iconAlias, startDate, endDate, name)
                } else {
                    null
                }
            }
            
            return IconChangeConfig(holidays, defaultIcon, checkInterval)
        } catch (e: Exception) {
            Log.e(TAG, "Error deserializing config: ${e.message}")
            return null
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "IconChangeService destroyed")
    }
}