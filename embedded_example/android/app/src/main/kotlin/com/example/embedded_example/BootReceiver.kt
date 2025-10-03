package com.example.embedded_example

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * Broadcast receiver для запуска сервиса при загрузке системы
 */
class BootReceiver : BroadcastReceiver() {
    companion object {
        private const val TAG = "[BootReceiver]"
    }
    
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED || 
            intent.action == "android.intent.action.QUICKBOOT_POWERON" ||
            intent.action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            
            Log.d(TAG, "Boot completed or package replaced, starting icon change service")
            
            // Проверяем, есть ли сохраненная конфигурация и если есть, запускаем автономный сервис
            Thread {
                try {
                    val app = context.applicationContext as? MainApplication
                    val config = app?.getConfigManager()?.loadConfig()
                    
                    if (config != null) {
                        Log.d(TAG, "Found saved config, starting autonomous service")
                        
                        val serviceIntent = Intent(context, IconChangeService::class.java).apply {
                            action = IconChangeService.getActionScheduleHolidayConfig(context)
                            putExtra(IconChangeService.EXTRA_CONFIG_JSON, IconChangeService.serializeConfig(config))
                        }
                        
                        context.startForegroundService(serviceIntent)
                    } else {
                        Log.d(TAG, "No saved config found")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error starting service on boot: ${e.message}", e)
                }
            }.start()
        }
    }
}