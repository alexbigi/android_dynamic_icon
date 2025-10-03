package com.example.embedded_example

import android.content.Context
import android.util.Log
import androidx.core.content.ContextCompat
import java.util.*

/**
 * Вспомогательный класс для автономной проверки дат и смены иконок
 * Разделяется между сервисами и receivers
 */
object IconChangeHelper {
    private const val TAG = "[IconChangeHelper]"
    
    /**
     * Проверить дату и при необходимости сменить иконку
     * Используется из разных компонентов (TimeChangeReceiver, автономный сервис и т.д.)
     */
    fun checkDateAndChangeIcon(context: Context, config: IconChangeConfig) {
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
                    Log.d(TAG, "App is in foreground - storing pending change to $targetIcon")
                    // Если приложение открыто, сохраняем задачу до следующего сворачивания
                    val app = context.applicationContext as? MainApplication
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
     * Проверить, находится ли приложение в foreground
     */
    private fun isAppInForeground(context: Context): Boolean {
        try {
            val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
            val appProcesses = activityManager.runningAppProcesses ?: return false
            
            for (appProcess in appProcesses) {
                if (appProcess.importance == android.app.ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND &&
                    context.packageName == appProcess.processName) {
                    return true
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking if app is in foreground: ${e.message}")
        }
        return false
    }
}