package com.example.embedded_example

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import android.os.Build
import android.app.ActivityManager
import android.content.Context.ACTIVITY_SERVICE

/**
 * Broadcast receiver to handle icon change intents
 * This works even when the app is killed
 */
class IconChangeReceiver : BroadcastReceiver() {
    companion object {
        private const val TAG = "[IconChangeReceiver]"
        private const val ACTION_SUFFIX = ".ICON_CHANGE"
        const val EXTRA_TARGET_ICON = "target_icon"
        const val EXTRA_SCHEDULE_TIME = "schedule_time"
        
        fun getActionName(context: Context): String {
            return context.packageName + ACTION_SUFFIX
        }
    }
    
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            getActionName(context) -> {
                val targetIcon = intent.getStringExtra(EXTRA_TARGET_ICON)
                val scheduleTime = intent.getLongExtra(EXTRA_SCHEDULE_TIME, 0)
                
                if (targetIcon != null) {
                    Log.d(TAG, "Received icon change request for $targetIcon at scheduled time: $scheduleTime")
                    
                    // Check if the app is currently in foreground
                    val isAppInForeground = isAppInForeground(context)
                    
                    if (isAppInForeground) {
                        Log.d(TAG, "App is in foreground - scheduling icon change for next background event")
                        // If app is in foreground, schedule the change for when app goes to background
                        // This is handled by storing the pending change in the application
                        val app = context.applicationContext as? MainApplication
                        app?.getDeferredIconChangeManager()?.setPendingIconChange(targetIcon)
                        
                        // Keep the service running to handle the change later
                        Log.d(TAG, "Pending icon change stored: $targetIcon")
                    } else {
                        Log.d(TAG, "App is in background/killed - changing icon immediately")
                        // If app is in background or killed, change the icon immediately
                        val iconManager = IconManager(context)
                        iconManager.changeIcon(targetIcon)
                        
                        Log.d(TAG, "Icon changed to $targetIcon successfully")
                        
                        // Stop the service if no more scheduled changes
                        // (in a real implementation, you'd track remaining scheduled changes)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            // Try to stop the service after a delay to ensure the change is processed
                            Thread {
                                try {
                                    Thread.sleep(2000) // Wait 2 seconds
                                    val stopIntent = Intent(context, IconChangeService::class.java).apply {
                                        action = context.packageName + ".STOP_ICON_CHANGE_SERVICE"
                                    }
                                    context.stopService(stopIntent)
                                } catch (e: Exception) {
                                    Log.e(TAG, "Error stopping service: ${e.message}")
                                }
                            }.start()
                        }
                    }
                } else {
                    Log.e(TAG, "Received icon change request with null target icon")
                }
            }
        }
    }
    
    /**
     * Check if the app is currently in foreground
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
}