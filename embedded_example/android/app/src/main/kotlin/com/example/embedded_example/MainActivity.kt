package com.example.embedded_example

import android.os.Bundle
import android.util.Log
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel

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
}