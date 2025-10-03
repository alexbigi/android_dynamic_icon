package com.example.embedded_example

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import android.os.Build
import android.app.ActivityManager
import android.content.Context.ACTIVITY_SERVICE
import java.util.*

/**
 * Broadcast receiver для проверки дат и смены иконки по расписанию
 * Используется для автономной проверки дат и праздников
 */
class DateCheckReceiver : BroadcastReceiver() {
    companion object {
        private const val TAG = "[DateCheckReceiver]"
        private const val ACTION_CHECK_DATE = ".CHECK_DATE_ACTION"
        const val EXTRA_CONFIG_JSON = "config_json"
        
        fun getActionName(context: Context): String {
            return context.packageName + ACTION_CHECK_DATE
        }
    }
    
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            getActionName(context) -> {
                Log.d(TAG, "Received date check request")
                
                try {
                    val configJson = intent.getStringExtra(EXTRA_CONFIG_JSON)
                    if (configJson != null) {
                        // Десериализуем конфигурацию (в реальном приложении нужно использовать proper JSON serialization)
                        val config = deserializeConfig(configJson)
                        
                        if (config != null) {
                            checkAndChangeIconByDate(context, config)
                        }
                    } else {
                        Log.e(TAG, "No configuration provided for date check")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error in date check: ${e.message}", e)
                }
                
                // Запланировать следующую проверку
                scheduleNextCheck(context)
            }
        }
    }
    
    private fun checkAndChangeIconByDate(context: Context, config: IconChangeConfig) {
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
            val iconManager = IconManager(context)
            val currentIcon = iconManager.getCurrentActiveIcon()
            
            if (currentIcon != targetIcon) {
                Log.d(TAG, "Current icon ($currentIcon) differs from target icon ($targetIcon) - changing icon")
                
                // Проверяем состояние приложения
                val isAppInForeground = isAppInForeground(context)
                
                if (isAppInForeground) {
                    Log.d(TAG, "App is in foreground - scheduling icon change for next background event")
                    // Если приложение открыто, запланировать смену до следующего сворачивания
                    val app = context.applicationContext as? MainApplication
                    app?.getDeferredIconChangeManager()?.setPendingIconChange(targetIcon)
                } else {
                    Log.d(TAG, "App is in background/killed - changing icon immediately")
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
     * Проверить, находится ли приложение в foreground
     */
    private fun isAppInForeground(context: Context): Boolean {
        try {
            val activityManager = context.getSystemService(ACTIVITY_SERVICE) as ActivityManager
            val appProcesses = activityManager.runningAppProcesses ?: return false
            
            for (appProcess in appProcesses) {
                if (appProcess.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND &&
                    context.packageName == appProcess.processName) {
                    return true
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking if app is in foreground: ${e.message}")
        }
        return false
    }
    
    /**
     * Десериализовать конфигурацию из JSON строки
     * В реальном приложении нужно использовать proper JSON serialization
     */
    private fun deserializeConfig(configJson: String): IconChangeConfig? {
        // В продакшене нужно использовать.gson или другую библиотеку для JSON
        // Для упрощения в этом примере возвращаем null - детализация будет в реализации
        return null
    }
    
    /**
     * Запланировать следующую проверку даты
     */
    private fun scheduleNextCheck(context: Context) {
        try {
            val service = context.getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
            
            // Рассчитываем время следующей проверки (например, начало следующего часа или дня)
            val calendar = Calendar.getInstance().apply {
                timeInMillis = System.currentTimeMillis()
                add(Calendar.DAY_OF_MONTH, 1) // следующий день
                set(Calendar.HOUR_OF_DAY, 0)   // полночь
                set(Calendar.MINUTE, 0)        // начало часа
                set(Calendar.SECOND, 0)        // начало минуты
                set(Calendar.MILLISECOND, 0)   // начало секунды
            }
            
            val intent = Intent(context, DateCheckReceiver::class.java).apply {
                action = getActionName(context)
            }
            
            val pendingIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                android.app.PendingIntent.getBroadcast(
                    context,
                    0,
                    intent,
                    android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
                )
            } else {
                android.app.PendingIntent.getBroadcast(
                    context,
                    0,
                    intent,
                    android.app.PendingIntent.FLAG_UPDATE_CURRENT
                )
            }
            
            // Планируем точный будильник
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                service.setExactAndAllowWhileIdle(
                    android.app.AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            } else {
                service.setExact(
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
}