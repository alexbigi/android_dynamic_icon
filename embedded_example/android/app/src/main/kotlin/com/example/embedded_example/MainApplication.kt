package com.example.embedded_example

import android.app.Application
import android.app.Activity
import android.os.Bundle
import android.util.Log

class MainApplication : Application() {
    companion object {
        private const val TAG = "[MainApplication]"
    }
    
    private var deferredIconChangeManager: DeferredIconChangeManager? = null
    
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "MainApplication created")
        
        // Инициализируем менеджер отложенной смены иконки
        deferredIconChangeManager = DeferredIconChangeManager(this)
        
        // Регистрируем слушатель жизненного цикла активностей
        registerActivityLifecycleCallbacks(AppLifecycleTracker())
    }
    
    fun getDeferredIconChangeManager(): DeferredIconChangeManager? {
        return deferredIconChangeManager
    }
    
    /**
     * Трекер жизненного цикла активностей для отслеживания сворачивания/разворачивания приложения
     */
    private inner class AppLifecycleTracker : ActivityLifecycleCallbacks {
        private var startedActivityCount = 0
        
        override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
        
        override fun onActivityStarted(activity: Activity) {
            if (startedActivityCount == 0) {
                // Приложение вернулось на передний план
                deferredIconChangeManager?.onAppForegrounded()
                Log.d(TAG, "App moved to foreground")
            }
            startedActivityCount++
        }
        
        override fun onActivityResumed(activity: Activity) {}
        
        override fun onActivityPaused(activity: Activity) {}
        
        override fun onActivityStopped(activity: Activity) {
            startedActivityCount--
            if (startedActivityCount == 0) {
                // Приложение ушло в фоновый режим
                deferredIconChangeManager?.onAppBackgrounded()
                Log.d(TAG, "App moved to background")
            }
        }
        
        override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
        
        override fun onActivityDestroyed(activity: Activity) {}
    }
}