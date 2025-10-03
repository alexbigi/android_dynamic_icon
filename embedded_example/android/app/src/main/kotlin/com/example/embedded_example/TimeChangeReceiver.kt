package com.example.embedded_example

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * Broadcast receiver для отслеживания изменений системного времени
 * Срабатывает при ручной смене даты/времени на устройстве
 */
class TimeChangeReceiver : BroadcastReceiver() {
    companion object {
        private const val TAG = "[TimeChangeReceiver]"
    }
    
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            "android.intent.action.TIME_SET",
            "android.intent.action.DATE_CHANGED",
            "android.intent.action.TIMEZONE_CHANGED" -> {
                Log.d(TAG, "System time/date/timezone changed detected: ${intent.action}")
                
                // Вызываем проверку по изменению времени
                checkAndChangeIconByDate(context)
            }
        }
    }
    
    private fun checkAndChangeIconByDate(context: Context) {
        Thread {
            try {
                // Получаем актуальную конфигурацию из хранилища
                val app = context.applicationContext as? MainApplication
                val config = app?.getConfigManager()?.loadConfig()
                
                if (config != null) {
                    IconChangeHelper.checkDateAndChangeIcon(context, config)
                    Log.d(TAG, "Time change check completed with stored config")
                } else {
                    Log.d(TAG, "No stored config found for time change check")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error checking time change: ${e.message}", e)
            }
        }.start()
    }
}