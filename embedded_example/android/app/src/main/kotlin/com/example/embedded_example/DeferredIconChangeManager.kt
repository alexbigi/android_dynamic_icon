package com.example.embedded_example

import android.app.Application
import android.os.Handler
import android.os.Looper
import android.util.Log
import java.text.SimpleDateFormat
import java.util.*

/**
 * Менеджер отложенной смены иконки
 * Следит за жизненным циклом приложения и сменяет иконку по дедлайну
 */
class DeferredIconChangeManager(private val application: Application) {
    companion object {
        private const val TAG = "[DeferredIconChangeManager]"
        private const val ICON_CHANGE_DELAY_MS = 1000L // 1 секунда задержки перед сменой
        private val DATE_FORMAT = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
    }

    private val iconManager = IconManager(application)
    private val handler = Handler(Looper.getMainLooper())
    private var pendingIconChange: String? = null // Иконка, которую нужно сменить при сворачивании
    private var scheduledIconChangeDeadline: Long = 0 // Дедлайн для смены иконки
    private var scheduledDelaySeconds: Int = 15 // Запланированная задержка в секундах
    private var scheduledIconChangeRunnable: Runnable? = null // Задача для смены иконки по дедлайну
    private var periodicCheckRunnable: Runnable? = null
    private var periodicCheckConditions: Map<*, *> = emptyMap<String, Any>()
    private var periodicCheckTargetIcon: String = "MainActivity"
    private var isAppInBackground = false
    private var iconChangeScheduled = false

    /**
     * Запланировать отложенную смену иконки
     * @param targetIconAlias имя целевой иконки
     * @param delaySeconds задержка в секундах
     */
    fun scheduleIconChange(targetIconAlias: String, delaySeconds: Int = 15) {
        if (!IconConfig.isIconAvailable(targetIconAlias)) {
            Log.e(TAG, "Cannot schedule icon change: icon '$targetIconAlias' is not available")
            return
        }
        
        // Отменяем предыдущую задачу, если есть
        scheduledIconChangeRunnable?.let { runnable ->
            handler.removeCallbacks(runnable)
        }
        
        pendingIconChange = targetIconAlias
        scheduledDelaySeconds = delaySeconds
        scheduledIconChangeDeadline = System.currentTimeMillis() + (delaySeconds * 1000L)
        
        // Создаем задачу для смены иконки по дедлайну
        scheduledIconChangeRunnable = Runnable {
            Log.d(TAG, "Deadline reached for icon change to $targetIconAlias")
            handleScheduledIconChange()
        }
        
        // Планируем выполнение задачи по дедлайну
        handler.postDelayed(scheduledIconChangeRunnable!!, delaySeconds * 1000L)
        
        Log.d(TAG, "Scheduled icon change to $targetIconAlias in $delaySeconds seconds (deadline: $scheduledIconChangeDeadline)")
    }
    
    /**
     * Запланировать периодическую проверку условий
     * @param targetIconAlias имя целевой иконки
     * @param intervalSeconds интервал проверки в секундах
     * @param conditions условия для смены иконки
     */
    fun schedulePeriodicCheck(
        targetIconAlias: String,
        intervalSeconds: Int,
        conditions: Map<*, *>
    ) {
        if (!IconConfig.isIconAvailable(targetIconAlias)) {
            Log.e(TAG, "Cannot schedule periodic check: icon '$targetIconAlias' is not available")
            return
        }
        
        // Отменяем предыдущую периодическую проверку
        cancelPeriodicCheck()
        
        periodicCheckTargetIcon = targetIconAlias
        periodicCheckConditions = conditions
        
        // Запускаем периодическую проверку
        periodicCheckRunnable = object : Runnable {
            override fun run() {
                checkConditionsAndChangeIcon()
                // Планируем следующую проверку
                handler.postDelayed(this, intervalSeconds * 1000L)
            }
        }
        
        // Запускаем первую проверку немедленно
        handler.post(periodicCheckRunnable!!)
        
        Log.d(TAG, "Scheduled periodic check every $intervalSeconds seconds for icon $targetIconAlias")
    }

    /**
     * Отменить запланированную смену иконки
     */
    fun cancelPendingIconChange() {
        pendingIconChange = null
        iconChangeScheduled = false
        handler.removeCallbacksAndMessages(null)
        Log.d(TAG, "Cancelled pending icon change")
    }
    
    /**
     * Отменить периодическую проверку
     */
    fun cancelPeriodicCheck() {
        periodicCheckRunnable?.let { runnable ->
            handler.removeCallbacks(runnable)
        }
        periodicCheckRunnable = null
        Log.d(TAG, "Cancelled periodic check")
    }

    /**
     * Немедленно сменить иконку (для тестирования)
     */
    fun changeIconImmediately(targetIconAlias: String) {
        if (!IconConfig.isIconAvailable(targetIconAlias)) {
            Log.e(TAG, "Cannot change icon immediately: icon '$targetIconAlias' is not available")
            return
        }
        
        iconManager.changeIcon(targetIconAlias)
        Log.d(TAG, "Icon changed immediately to $targetIconAlias")
    }

    /**
     * Приложение ушло в фоновый режим
     */
    fun onAppBackgrounded() {
        handleAppBackgrounded()
    }

    /**
     * Приложение вернулось на передний план
     */
    fun onAppForegrounded() {
        isAppInBackground = false
        // Отменяем запланированную смену иконки, если приложение вернулось на передний план
        if (iconChangeScheduled) {
            handler.removeCallbacksAndMessages(null)
            iconChangeScheduled = false
            Log.d(TAG, "Cancelled scheduled icon change - app returned to foreground")
        }
    }
    
    /**
     * Обработать запланированную смену иконки по наступлению дедлайна
     * Вызывается по таймеру - если время наступило, устанавливаем флаг для смены иконки
     */
    private fun handleScheduledIconChange() {
        pendingIconChange?.let { targetIcon ->
            Log.d(TAG, "Deadline reached for icon change to $targetIcon")
            
            // Устанавливаем флаг, что иконку нужно сменить
            iconChangeScheduled = true
            Log.d(TAG, "Scheduled icon change to $targetIcon - will execute on next background event")
            
            // Если приложение уже свернуто, меняем иконку немедленно
            if (isAppInBackground) {
                Log.d(TAG, "App is in background, changing icon to $targetIcon immediately")
                handler.postDelayed({
                    iconManager.changeIcon(targetIcon)
                    Log.d(TAG, "Icon changed to $targetIcon immediately in background")
                    pendingIconChange = null
                    iconChangeScheduled = false
                }, ICON_CHANGE_DELAY_MS)
            }
            // Если приложение открыто, иконка сменится при следующем сворачивании
        }
    }
    
    /**
     * Проверить условия и при необходимости сменить иконку
     */
    private fun checkConditionsAndChangeIcon() {
        try {
            val currentDate = Date()
            val currentDateStr = DATE_FORMAT.format(currentDate)
            
            // Проверяем условия
            val dateCondition = periodicCheckConditions["date"] as? String
            val dateRangeStart = periodicCheckConditions["dateRangeStart"] as? String
            val dateRangeEnd = periodicCheckConditions["dateRangeEnd"] as? String
            
            var shouldChangeIcon = false
            
            // Проверка конкретной даты
            if (dateCondition != null && dateCondition == currentDateStr) {
                shouldChangeIcon = true
                Log.d(TAG, "Date condition matched: $currentDateStr")
            }
            
            // Проверка диапазона дат
            if (dateRangeStart != null && dateRangeEnd != null) {
                try {
                    val startDate = DATE_FORMAT.parse(dateRangeStart)
                    val endDate = DATE_FORMAT.parse(dateRangeEnd)
                    if (startDate != null && endDate != null && 
                        currentDate.time >= startDate.time && 
                        currentDate.time <= endDate.time) {
                        shouldChangeIcon = true
                        Log.d(TAG, "Date range condition matched: $currentDateStr is between $dateRangeStart and $dateRangeEnd")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing date range: ${e.message}")
                }
            }
            
            // Если условия выполнены, меняем иконку
            if (shouldChangeIcon) {
                if (isAppInBackground) {
                    // Если приложение в фоне, меняем иконку немедленно
                    iconManager.changeIcon(periodicCheckTargetIcon)
                    Log.d(TAG, "Icon changed to $periodicCheckTargetIcon due to condition match (app in background)")
                } else {
                    // Если приложение на переднем плане, планируем отложенную смену
                    pendingIconChange = periodicCheckTargetIcon
                    Log.d(TAG, "Scheduled icon change to $periodicCheckTargetIcon due to condition match (app in foreground)")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking conditions: ${e.message}")
        }
    }

    private fun handleAppBackgrounded() {
        if (isAppInBackground) return
        
        isAppInBackground = true
        Log.d(TAG, "App went to background")
        
        // Если есть запланированная смена иконки и время наступило, выполняем смену
        if (iconChangeScheduled && pendingIconChange != null) {
            pendingIconChange?.let { targetIcon ->
                Log.d(TAG, "Icon change scheduled, changing icon to $targetIcon immediately")
                
                // Выполняем смену иконки немедленно с небольшой задержкой для лучшего UX
                handler.postDelayed({
                    if (isAppInBackground) {
                        iconManager.changeIcon(targetIcon)
                        Log.d(TAG, "Icon changed to $targetIcon after scheduled deadline")
                        pendingIconChange = null
                        iconChangeScheduled = false
                    }
                }, ICON_CHANGE_DELAY_MS)
            }
        } else if (pendingIconChange != null) {
            // Проверяем, наступило ли время дедлайна (если таймер еще не сработал)
            val currentTime = System.currentTimeMillis()
            if (currentTime >= scheduledIconChangeDeadline) {
                pendingIconChange?.let { targetIcon ->
                    Log.d(TAG, "Deadline reached ($currentTime >= $scheduledIconChangeDeadline), changing icon to $targetIcon immediately")
                    
                    // Выполняем смену иконки немедленно с небольшой задержкой для лучшего UX
                    handler.postDelayed({
                        if (isAppInBackground) {
                            iconManager.changeIcon(targetIcon)
                            Log.d(TAG, "Icon changed to $targetIcon after deadline")
                            pendingIconChange = null
                            iconChangeScheduled = false
                        }
                    }, ICON_CHANGE_DELAY_MS)
                }
            } else {
                val remainingTime = scheduledIconChangeDeadline - currentTime
                Log.d(TAG, "Deadline not reached yet. Remaining time: ${remainingTime}ms")
            }
        } else {
            Log.d(TAG, "No pending icon change scheduled")
        }
    }
}