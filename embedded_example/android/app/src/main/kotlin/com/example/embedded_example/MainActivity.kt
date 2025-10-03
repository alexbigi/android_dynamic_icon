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
        
        // Проверить дату при запуске приложения (для обновления иконки при смене даты вручную)
        checkDateAndIconOnAppStart()
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
                "startAutonomousIconChange" -> {
                    // Запуск автономного сервиса смены иконок
                    startAutonomousIconChange(call)
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
    
    private fun startAutonomousIconChange(call: MethodCall) {
        try {
            val arguments = call.arguments as? Map<*, *>
            val defaultIcon = arguments?.get("defaultIcon") as? String ?: "MainActivity"
            val holidaysList = arguments?.get("holidays") as? List<Map<*, *>> ?: emptyList()
            
            // Конвертируем список праздников
            val holidays = convertHolidayMapsToHolidayObjects(holidaysList)
            val config = IconChangeConfig(holidays, defaultIcon)
            
            // Запускаем автономный сервис 
            val serviceIntent = Intent(this, IconChangeService::class.java).apply {
                action = IconChangeService.getActionScheduleHolidayConfig(this@MainActivity)
                putExtra(IconChangeService.EXTRA_CONFIG_JSON, IconChangeService.serializeConfig(config))
            }
            
            ContextCompat.startForegroundService(this, serviceIntent)
            Log.d("[android_dynamic_icon]", "Started autonomous icon change service with ${holidays.size} holidays, default: $defaultIcon")
        } catch (e: Exception) {
            Log.e("[android_dynamic_icon]", "Error starting autonomous icon change service: ${e.message}", e)
        }
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
            val defaultIcon = arguments["defaultIcon"] as? String ?: "MainActivity"
            val holidaysList = arguments["holidays"] as? List<Map<*, *>> ?: emptyList()
            
            if (holidaysList.isNotEmpty()) {
                // Используем новый формат с праздниками
                val holidays = convertHolidayMapsToHolidayObjects(holidaysList)
                val deferredManager = (application as? MainApplication)?.getDeferredIconChangeManager()
                deferredManager?.schedulePeriodicCheck(holidays, defaultIcon, intervalSeconds)
                
                Log.d("[android_dynamic_icon]", "Scheduled periodic check with ${holidays.size} holidays, default icon: $defaultIcon, interval: $intervalSeconds seconds")
            } else {
                // Используем старый формат для обратной совместимости
                val conditions = arguments["conditions"] as? Map<*, *> ?: emptyMap<String, Any>()
                
                val deferredManager = (application as? MainApplication)?.getDeferredIconChangeManager()
                deferredManager?.schedulePeriodicCheck(targetIcon, intervalSeconds, conditions)
                
                Log.d("[android_dynamic_icon]", "Scheduled periodic check (legacy format) every $intervalSeconds seconds for icon $targetIcon")
            }
        } catch (e: Exception) {
            Log.e("[android_dynamic_icon]", "Error scheduling periodic check: ${e.message}", e)
        }
    }
    
    /**
     * Конвертировать список Map в список Holiday объектов
     */
    private fun convertHolidayMapsToHolidayObjects(holidaysList: List<Map<*, *>>): List<Holiday> {
        val holidays = mutableListOf<Holiday>()
        
        for (holidayMap in holidaysList) {
            try {
                val iconAlias = holidayMap["iconAlias"] as? String ?: continue
                val startDateStr = holidayMap["startDate"] as? String ?: continue
                val endDateStr = holidayMap["endDate"] as? String
                val name = holidayMap["name"] as? String ?: ""
                
                val startDate = DateUtils.parseDate(startDateStr)
                val endDate = if (endDateStr != null) DateUtils.parseDate(endDateStr) else null
                
                if (startDate != null) {
                    holidays.add(Holiday(
                        iconAlias = iconAlias,
                        startDate = startDate,
                        endDate = endDate,
                        name = name
                    ))
                }
            } catch (e: Exception) {
                Log.e("[android_dynamic_icon]", "Error parsing holiday data: ${e.message}", e)
            }
        }
        
        return holidays
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
    
    /**
     * Проверить дату и при необходимости обновить иконку при запуске приложения
     */
    private fun checkDateAndIconOnAppStart() {
        Thread {
            try {
                val app = applicationContext as? MainApplication
                val config = app?.getConfigManager()?.loadConfig()
                
                if (config != null) {
                    IconChangeHelper.checkDateAndChangeIcon(this, config)
                    Log.d("[android_dynamic_icon]", "Date check completed on app start")
                }
            } catch (e: Exception) {
                Log.e("[android_dynamic_icon]", "Error checking date on app start: ${e.message}", e)
            }
        }.start()
    }
}