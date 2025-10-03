package com.example.embedded_example

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import android.os.Build
import java.util.*
import kotlin.concurrent.thread

/**
 * Broadcast receiver для регулярной проверки даты и смены иконки
 * Работает через AlarmManager с интервалом в несколько минут
 */
class DateCheckIntervalReceiver : BroadcastReceiver() {
    companion object {
        private const val TAG = "[DateCheckIntervalReceiver]"
        private const val ACTION_CHECK_DATE_INTERVAL = ".CHECK_DATE_INTERVAL_ACTION"
        
        fun getActionName(context: Context): String {
            return context.packageName + ACTION_CHECK_DATE_INTERVAL
        }
    }
    
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            getActionName(context) -> {
                Log.d(TAG, "Received interval date check request")
                
                thread {
                    try {
                        // Получаем актуальную конфигурацию из хранилища
                        val app = context.applicationContext as? MainApplication
                        val config = app?.getConfigManager()?.loadConfig()
                        
                        if (config != null) {
                            IconChangeHelper.checkDateAndChangeIcon(context, config)
                            Log.d(TAG, "Interval date check completed with stored config")
                            
                            // Запланировать следующую проверку
                            scheduleNextIntervalCheck(context, config.checkIntervalSeconds)
                        } else {
                            Log.d(TAG, "No stored config found for interval check")
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error in interval date check: ${e.message}", e)
                    }
                }
            }
        }
    }
    
    /**
     * Запланировать следующую проверку с интервалом
     */
    fun scheduleNextIntervalCheck(context: Context, intervalSeconds: Int = 600) { // 10 минут по умолчанию
        try {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
            
            // Планируем проверку с заданным интервалом
            val calendar = Calendar.getInstance().apply {
                timeInMillis = System.currentTimeMillis()
                add(Calendar.SECOND, intervalSeconds) // добавляем интервал в секундах
            }
            
            val intent = Intent(context, DateCheckIntervalReceiver::class.java).apply {
                action = getActionName(context)
            }
            
            val pendingIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                android.app.PendingIntent.getBroadcast(
                    context,
                    1, // используем другой requestCode для отличия от других будильников
                    intent,
                    android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
                )
            } else {
                android.app.PendingIntent.getBroadcast(
                    context,
                    1,
                    intent,
                    android.app.PendingIntent.FLAG_UPDATE_CURRENT
                )
            }
            
            // Планируем будильник с разумным интервалом
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    android.app.AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                alarmManager.setExact(
                    android.app.AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            } else {
                alarmManager.set(
                    android.app.AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            }
            
            Log.d(TAG, "Scheduled next interval date check at: ${calendar.time}, interval: ${intervalSeconds}s")
        } catch (e: Exception) {
            Log.e(TAG, "Error scheduling next interval check: ${e.message}", e)
        }
    }
    
    /**
     * Отменить запланированную проверку
     */
    fun cancelScheduledCheck(context: Context) {
        try {
            val intent = Intent(context, DateCheckIntervalReceiver::class.java).apply {
                action = getActionName(context)
            }
            
            val pendingIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                android.app.PendingIntent.getBroadcast(
                    context,
                    1,
                    intent,
                    android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
                )
            } else {
                android.app.PendingIntent.getBroadcast(
                    context,
                    1,
                    intent,
                    android.app.PendingIntent.FLAG_UPDATE_CURRENT
                )
            }
            
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
            alarmManager.cancel(pendingIntent)
            Log.d(TAG, "Cancelled interval date check")
        } catch (e: Exception) {
            Log.e(TAG, "Error cancelling interval check: ${e.message}", e)
        }
    }
}