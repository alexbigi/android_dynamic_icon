package com.example.embedded_example

import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.core.content.ContextCompat
import java.util.*

/**
 * Менеджер отложенной смены иконки
 * Следит за жизненным циклом приложения и сменяет иконку по дедлайну
 */
class DeferredIconChangeManager(private val application: Application) {
    companion object {
        private const val TAG = "[DeferredIconChangeManager]"
        private const val ICON_CHANGE_DELAY_MS = 1000L // 1 секунда задержки перед сменой
    }

    private val iconManager = IconManager(application)
    private val handler = Handler(Looper.getMainLooper())
    private var pendingIconChange: String? = null // Иконка, которую нужно сменить при сворачивании
    private var scheduledIconChangeDeadline: Long = 0 // Дедлайн для смены иконки
    private var scheduledDelaySeconds: Int = 15 // Запланированная задержка в секундах
    private var scheduledIconChangeRunnable: Runnable? = null // Задача для смены иконки по дедлайну
    private var periodicCheckRunnable: Runnable? = null
    private var periodicCheckHolidays: List<Holiday> = emptyList()
    private var periodicCheckDefaultIcon: String = "MainActivity"
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
    /**
     * Запланировать периодическую проверку дат для смены иконки (новый метод)
     * @param holidays список праздников с иконками
     * @param defaultIcon иконка по умолчанию
     * @param intervalSeconds интервал проверки в секундах
     */
    fun schedulePeriodicCheck(
        holidays: List<Holiday>,
        defaultIcon: String,
        intervalSeconds: Int
    ) {
        if (!IconConfig.isIconAvailable(defaultIcon)) {
            Log.e(TAG, "Cannot schedule periodic check: default icon '$defaultIcon' is not available")
            return
        }
        
        // Проверяем, что все иконки в праздниках доступны
        for (holiday in holidays) {
            if (!IconConfig.isIconAvailable(holiday.iconAlias)) {
                Log.e(TAG, "Cannot schedule periodic check: icon '${holiday.iconAlias}' is not available")
                return
            }
        }
        
        // Отменяем предыдущую периодическую проверку
        cancelPeriodicCheck()
        
        periodicCheckHolidays = holidays
        periodicCheckDefaultIcon = defaultIcon
        
        // Запускаем периодическую проверку
        periodicCheckRunnable = object : Runnable {
            override fun run() {
                checkHolidaysAndChangeIcon()
                // Планируем следующую проверку
                handler.postDelayed(this, intervalSeconds * 1000L)
            }
        }
        
        // Запускаем первую проверку немедленно
        handler.post(periodicCheckRunnable!!)
        
        Log.d(TAG, "Scheduled periodic check every $intervalSeconds seconds for ${holidays.size} holidays, default icon: $defaultIcon")
    }
    
    /** 
     * Запланировать периодическую проверку условий (старый метод для обратной совместимости)
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
        
        Log.d(TAG, "Scheduled periodic check (legacy) every $intervalSeconds seconds for icon $targetIconAlias")
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
     * Отменить запланированную смену иконки по имени
     */
    fun cancelScheduledIconChange(targetIcon: String) {
        if (pendingIconChange == targetIcon) {
            pendingIconChange = null
            iconChangeScheduled = false
            handler.removeCallbacksAndMessages(null)
            Log.d(TAG, "Cancelled scheduled icon change for $targetIcon")
        } else {
            Log.d(TAG, "No scheduled icon change for $targetIcon to cancel")
        }
    }
    
    /**
     * Установить отложенную смену иконки (для использования из системных компонентов)
     */
    fun setPendingIconChange(targetIcon: String) {
        pendingIconChange = targetIcon
        iconChangeScheduled = true
        Log.d(TAG, "Set pending icon change for $targetIcon - will execute on next background event")
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
     * Проверить даты праздников и при необходимости сменить иконку
     */
    private fun checkHolidaysAndChangeIcon() {
        try {
            val currentDate = Date()
            var targetIcon: String? = null
            
            // Проверяем, попадает ли текущая дата под какой-либо праздник
            for (holiday in periodicCheckHolidays) {
                if (holiday.isCurrentDayMonthInRange(currentDate)) {
                    targetIcon = holiday.iconAlias
                    Log.d(TAG, "Holiday matched: ${holiday.name}, icon: $targetIcon, date: ${DateUtils.DATE_FORMAT.format(currentDate)}")
                    break // Используем первую подходящую иконку
                }
            }
            
            // Если ни один праздник не подошел, используем иконку по умолчанию
            if (targetIcon == null) {
                targetIcon = periodicCheckDefaultIcon
                Log.d(TAG, "No holiday matched, using default icon: $targetIcon")
            }
            
            // Проверяем, отличается ли текущая иконка от целевой
            val currentIcon = iconManager.getCurrentActiveIcon()
            if (currentIcon != targetIcon) {
                Log.d(TAG, "Current icon ($currentIcon) differs from target icon ($targetIcon) - scheduling change via service")
                
                // Используем сервис для надежной смены иконки, как в scheduleIconChangeWithExactAlarm
                val context = application.applicationContext
                val scheduleTime = System.currentTimeMillis() + 1000L // Запланировать на 1 секунду в будущем
                
                val serviceIntent = Intent(context, IconChangeService::class.java).apply {
                    action = IconChangeService.getActionStart(context)
                    putExtra(IconChangeService.EXTRA_TARGET_ICON, targetIcon)
                    putExtra(IconChangeService.EXTRA_SCHEDULE_TIME, scheduleTime)
                }
                
                ContextCompat.startForegroundService(context, serviceIntent)
                Log.d(TAG, "Scheduled holiday icon change to $targetIcon via service at ${Date(scheduleTime)}")
            } else {
                Log.d(TAG, "Current icon is already $targetIcon, no change needed")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking holidays and changing icon: ${e.message}", e)
        }
    }

    /**
     * Конвертировать старый формат условий в список праздников (для обратной совместимости)
     */
    private fun convertConditionsToHolidays(targetIconAlias: String, conditions: Map<*, *>): List<Holiday> {
        val holidays = mutableListOf<Holiday>()
        
        try {
            val dateCondition = conditions["date"] as? String
            val dateRangeStart = conditions["dateRangeStart"] as? String
            val dateRangeEnd = conditions["dateRangeEnd"] as? String
            
            // Проверка конкретной даты
            if (dateCondition != null) {
                try {
                    val date = DateUtils.DATE_FORMAT.parse(dateCondition)
                    if (date != null) {
                        holidays.add(Holiday(
                            iconAlias = targetIconAlias,
                            startDate = date,
                            endDate = null,
                            name = "Single Date Holiday"
                        ))
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing single date: ${e.message}")
                }
            }
            
            // Проверка диапазона дат
            if (dateRangeStart != null && dateRangeEnd != null) {
                try {
                    val startDate = DateUtils.DATE_FORMAT.parse(dateRangeStart)
                    val endDate = DateUtils.DATE_FORMAT.parse(dateRangeEnd)
                    if (startDate != null && endDate != null) {
                        holidays.add(Holiday(
                            iconAlias = targetIconAlias,
                            startDate = startDate,
                            endDate = endDate,
                            name = "Date Range Holiday"
                        ))
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing date range: ${e.message}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error converting conditions to holidays: ${e.message}", e)
        }
        
        return holidays
    }

    /**
     * Выполнить отложенную смену иконки, если таковая имеется
     */
    fun executePendingIconChangeIfAny() {
        if (pendingIconChange != null && iconChangeScheduled) {
            pendingIconChange?.let { targetIcon ->
                Log.d(TAG, "Executing pending icon change to $targetIcon")
                
                // Выполняем смену иконки немедленно с небольшой задержкой для лучшего UX
                handler.postDelayed({
                    if (isAppInBackground) {
                        iconManager.changeIcon(targetIcon)
                        Log.d(TAG, "Icon changed to $targetIcon after background event")
                        pendingIconChange = null
                        iconChangeScheduled = false
                    }
                }, 500) // Небольшая задержка для корректной обработки
            }
        }
    }
    
    /**
     * Проверить изменение времени и при необходимости обновить иконку
     */
    fun checkTimeChangeAndIcon() {
        Log.d(TAG, "Checking time change and updating icon if needed")
        
        // Проверяем, запущен ли автономный сервис и есть ли у него конфигурация
        val service = application.getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
        // Для проверки времени используем текущую логику проверки праздников
        
        // Имитируем проверку праздников, как в автономном сервисе
        if (periodicCheckHolidays.isNotEmpty()) {
            checkHolidaysAndChangeIcon()
        }
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
            val currentDateStr = DateUtils.DATE_FORMAT.format(currentDate)
            
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
                    val startDate = DateUtils.DATE_FORMAT.parse(dateRangeStart)
                    val endDate = DateUtils.DATE_FORMAT.parse(dateRangeEnd)
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
        
        // Выполнить все запланированные изменения иконок (включая системные)
        executePendingIconChangeIfAny()
    }
}