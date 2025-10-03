package com.example.embedded_example

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import android.os.Build

/**
 * Broadcast receiver to handle icon change intents
 * This works even when the app is killed
 */
class IconChangeReceiver : BroadcastReceiver() {
    companion object {
        private const val TAG = "[IconChangeReceiver]"
        const val ACTION_ICON_CHANGE = "com.example.embedded_example.ICON_CHANGE"
        const val EXTRA_TARGET_ICON = "target_icon"
        const val EXTRA_SCHEDULE_TIME = "schedule_time"
    }
    
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_ICON_CHANGE -> {
                val targetIcon = intent.getStringExtra(EXTRA_TARGET_ICON)
                val scheduleTime = intent.getLongExtra(EXTRA_SCHEDULE_TIME, 0)
                
                if (targetIcon != null) {
                    Log.d(TAG, "Received icon change request for $targetIcon at scheduled time: $scheduleTime")
                    
                    // Change the icon immediately
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
                                    action = "com.example.embedded_example.STOP_ICON_CHANGE_SERVICE"
                                }
                                context.stopService(stopIntent)
                            } catch (e: Exception) {
                                Log.e(TAG, "Error stopping service: ${e.message}")
                            }
                        }.start()
                    }
                } else {
                    Log.e(TAG, "Received icon change request with null target icon")
                }
            }
        }
    }
}