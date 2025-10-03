package com.example.embedded_example

import android.os.Bundle
import android.util.Log
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.content.ContextCompat

class MainActivity: FlutterActivity() {
    
    private val CHANNEL_ID = "AndroidDynamicIcon"
    private var classNames: List<String>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)
        
        // Создаем обработчик методов
        val methodChannel = MethodChannel(flutterEngine.dartExecutor.binaryMessenger, CHANNEL_ID)
        methodChannel.setMethodCallHandler { call, result ->
            when (call.method) {
                "initialize" -> {
                    @Suppress("UNCHECKED_CAST")
                    classNames = call.arguments as List<String>?
                    result.success(null)
                }
                "changeIcon" -> {
                    // Отложенная смена иконки (смена происходит при сворачивании/убийстве приложения)
                    changeIconDeferred(call)
                    result.success(null)
                }
                "changeIconImmediate" -> {
                    // Немедленная смена иконки
                    changeIconImmediate(call)
                    result.success(null)
                }
                "scheduleIconChange" -> {
                    // Планирование смены иконки через заданное количество секунд
                    scheduleIconChange(call)
                    result.success(null)
                }
                "schedulePeriodicCheck" -> {
                    // Планирование периодической проверки условий
                    schedulePeriodicCheck(call)
                    result.success(null)
                }
                "scheduleIconChangeWithExactAlarm" -> {
                    // Новая функция: смена иконки через Foreground Service и Exact Alarms
                    scheduleIconChangeWithExactAlarm(call)
                    result.success(null)
                }
                "startIconChangeService" -> {
                    // Запуск сервиса для управления иконками
                    startIconChangeService(call)
                    result.success(null)
                }
                "stopIconChangeService" -> {
                    // Остановка сервиса для управления иконками
                    stopIconChangeService()
                    result.success(null)
                }
                "cancelScheduledIconChange" -> {
                    // Отмена запланированной смены иконки
                    cancelScheduledIconChange(call)
                    result.success(null)
                }
                else -> result.notImplemented()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        Log.i("ChangeIcon", "Activity has resumed")
        // Сообщаем менеджеру, что приложение на переднем плане
        (application as? MainApplication)?.getDeferredIconChangeManager()?.onAppForegrounded()
    }

    override fun onPause() {
        super.onPause()
        Log.i("ChangeIcon", "The app has paused")
        // Сообщаем менеджеру, что приложение ушло в фон
        (application as? MainApplication)?.getDeferredIconChangeManager()?.onAppBackgrounded()
    }
    
    private fun changeIconDeferred(call: MethodCall) {
        if (classNames.isNullOrEmpty()) {
            Log.e("[android_dynamic_icon]", "Initialization Failed!")
            Log.i("[android_dynamic_icon]", "List all the activity-alias class names in initialize()")
            return
        }

        @Suppress("UNCHECKED_CAST")
        val args = call.arguments as List<String>?
        if (args.isNullOrEmpty()) return
        
        val targetIconAlias = args[0]
        
        // Планируем отложенную смену иконки
        (application as? MainApplication)?.getDeferredIconChangeManager()?.scheduleIconChange(targetIconAlias, 0) // 0 секунд для немедленного планирования
        Log.d("[android_dynamic_icon]", "Scheduled deferred icon change to $targetIconAlias")
    }
    
    private fun changeIconImmediate(call: MethodCall) {
        if (classNames.isNullOrEmpty()) {
            Log.e("[android_dynamic_icon]", "Initialization Failed!")
            Log.i("[android_dynamic_icon]", "List all the activity-alias class names in initialize()")
            return
        }

        @Suppress("UNCHECKED_CAST")
        val args = call.arguments as List<String>?
        if (args.isNullOrEmpty()) return
        
        val targetIconAlias = args[0]
        
        // Меняем иконку немедленно
        val iconManager = IconManager(this)
        iconManager.changeIcon(targetIconAlias)
        Log.d("[android_dynamic_icon]", "Icon changed immediately to $targetIconAlias")
    }
    
    private fun scheduleIconChange(call: MethodCall) {
        try {
            val arguments = call.arguments as? Map<*, *>
            if (arguments == null) {
                Log.e("[android_dynamic_icon]", "Invalid arguments for scheduleIconChange")
                return
            }
            
            val seconds = arguments["seconds"] as? Int ?: 15
            val targetIcon = arguments["targetIcon"] as? String ?: "MainActivity"
            
            Log.d("[android_dynamic_icon]", "Received scheduleIconChange request: seconds=$seconds, targetIcon=$targetIcon")
            
            // Планируем отложенную смену иконки через заданное количество секунд
            val deferredManager = (application as? MainApplication)?.getDeferredIconChangeManager()
            deferredManager?.scheduleIconChange(targetIcon, seconds)
            
            Log.d("[android_dynamic_icon]", "Scheduled icon change to $targetIcon in $seconds seconds")
        } catch (e: Exception) {
            Log.e("[android_dynamic_icon]", "Error scheduling icon change: ${e.message}")
        }
    }
    
    private fun schedulePeriodicCheck(call: MethodCall) {
        try {
            val arguments = call.arguments as? Map<*, *>
            if (arguments == null) {
                Log.e("[android_dynamic_icon]", "Invalid arguments for schedulePeriodicCheck")
                return
            }
            
            val intervalSeconds = arguments["intervalSeconds"] as? Int ?: 15 * 60
            val targetIcon = arguments["targetIcon"] as? String ?: "MainActivity"
            val conditions = arguments["conditions"] as? Map<*, *> ?: emptyMap<String, Any>()
            
            // Планируем периодическую проверку условий
            val deferredManager = (application as? MainApplication)?.getDeferredIconChangeManager()
            deferredManager?.schedulePeriodicCheck(targetIcon, intervalSeconds, conditions)
            
            Log.d("[android_dynamic_icon]", "Scheduled periodic check every $intervalSeconds seconds for icon $targetIcon")
        } catch (e: Exception) {
            Log.e("[android_dynamic_icon]", "Error scheduling periodic check: ${e.message}")
        }
    }
    
    private fun scheduleIconChangeWithExactAlarm(call: MethodCall) {
        try {
            val arguments = call.arguments as? Map<*, *>
            if (arguments == null) {
                Log.e("[android_dynamic_icon]", "Invalid arguments for scheduleIconChangeWithExactAlarm")
                return
            }
            
            val seconds = arguments["seconds"] as? Int ?: 15
            val targetIcon = arguments["targetIcon"] as? String ?: "MainActivity"
            
            // Проверяем, что иконка доступна
            if (!IconConfig.isIconAvailable(targetIcon)) {
                Log.e("[android_dynamic_icon]", "Icon alias '$targetIcon' is not available")
                return
            }
            
            val scheduleTime = System.currentTimeMillis() + (seconds * 1000L)
            Log.d("[android_dynamic_icon]", "Scheduling icon change with exact alarm: $targetIcon at ${java.util.Date(scheduleTime)}")
            
            // Запускаем сервис для смены иконки
            val serviceIntent = Intent(this, IconChangeService::class.java).apply {
                action = IconChangeService.getActionStart(this@MainActivity)
                putExtra(IconChangeService.EXTRA_TARGET_ICON, targetIcon)
                putExtra(IconChangeService.EXTRA_SCHEDULE_TIME, scheduleTime)
            }
            
            ContextCompat.startForegroundService(this, serviceIntent)
            
            Log.d("[android_dynamic_icon]", "Scheduled icon change with exact alarm: $targetIcon in $seconds seconds")
        } catch (e: Exception) {
            Log.e("[android_dynamic_icon]", "Error scheduling icon change with exact alarm: ${e.message}", e)
        }
    }
    
    private fun startIconChangeService(call: MethodCall) {
        try {
            val arguments = call.arguments as? Map<*, *>
            val targetIcon = arguments?.get("targetIcon") as? String ?: "MainActivity"
            val scheduleTime = arguments?.get("scheduleTime") as? Long ?: (System.currentTimeMillis() + 15000) // 15 seconds default
            
            // Check if exact alarms permission is granted
            val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val canScheduleExactAlarms = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                alarmManager.canScheduleExactAlarms()
            } else {
                true // On older versions, exact alarms are always allowed
            }
            
            if (!canScheduleExactAlarms) {
                Log.e("[android_dynamic_icon]", "Exact alarms permission not granted")
                // In a real app, you'd need to request permission
                return
            }
            
            val serviceIntent = Intent(this, IconChangeService::class.java).apply {
                action = IconChangeService.getActionStart(this@MainActivity)
                putExtra(IconChangeService.EXTRA_TARGET_ICON, targetIcon)
                putExtra(IconChangeService.EXTRA_SCHEDULE_TIME, scheduleTime)
            }
            
            ContextCompat.startForegroundService(this, serviceIntent)
            Log.d("[android_dynamic_icon]", "Started IconChangeService for $targetIcon")
        } catch (e: Exception) {
            Log.e("[android_dynamic_icon]", "Error starting icon change service: ${e.message}", e)
        }
    }
    
    private fun stopIconChangeService() {
        val serviceIntent = Intent(this, IconChangeService::class.java).apply {
            action = IconChangeService.getActionStop(this@MainActivity)
        }
        stopService(serviceIntent)
        Log.d("[android_dynamic_icon]", "Stopped IconChangeService")
    }
    
    private fun cancelScheduledIconChange(call: MethodCall) {
        try {
            val arguments = call.arguments as? Map<*, *>
            val targetIcon = arguments?.get("targetIcon") as? String ?: "MainActivity"
            
            // Cancel the scheduled alarm
            val serviceIntent = Intent(this, IconChangeService::class.java)
            (application as? MainApplication)?.getDeferredIconChangeManager()?.cancelScheduledIconChange(targetIcon)
            
            Log.d("[android_dynamic_icon]", "Cancelled scheduled icon change for $targetIcon")
        } catch (e: Exception) {
            Log.e("[android_dynamic_icon]", "Error cancelling scheduled icon change: ${e.message}", e)
        }
    }
}