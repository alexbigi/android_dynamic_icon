package com.example.embedded_example

import java.util.*

/**
 * Конфигурация для автономного сервиса смены иконок
 */
data class IconChangeConfig(
    val holidays: List<Holiday>,
    val defaultIcon: String,
    val checkIntervalSeconds: Int = 60 // интервал проверки в секундах
)

/**
 * Класс для представления праздника с настройками иконки
 */
data class Holiday(
    val iconAlias: String,  // Имя alias иконки (например, "IconOne", "IconTwo")
    val startDate: Date,    // Дата начала праздника
    val endDate: Date?,     // Дата окончания праздника (null если однодневный праздник)
    val name: String = ""   // Название праздника (для логирования)
) {
    /**
     * Проверяет, попадает ли текущий день/месяц в период праздника (игнорируя год)
     * Используется для повторяющихся ежегодных праздников
     */
    fun isCurrentDayMonthInRange(currentDate: Date): Boolean {
        val currentCalendar = Calendar.getInstance().apply { time = currentDate }
        
        return if (endDate != null) {
            // Для диапазона с учетом года
            val startCalendar = Calendar.getInstance().apply { time = startDate }
            val endCalendar = Calendar.getInstance().apply { time = endDate }
            
            // Если диапазон в пределах одного года
            if (startCalendar.get(Calendar.YEAR) == endCalendar.get(Calendar.YEAR)) {
                val startDayOfYear = startCalendar.get(Calendar.DAY_OF_YEAR)
                val endDayOfYear = endCalendar.get(Calendar.DAY_OF_YEAR)
                val currentDayOfYear = currentCalendar.get(Calendar.DAY_OF_YEAR)
                
                currentDayOfYear >= startDayOfYear && currentDayOfYear <= endDayOfYear
            } else {
                // Если диапазон пересекает год (например, с декабря по январь)
                val startDayOfYear = startCalendar.get(Calendar.DAY_OF_YEAR)
                val endDayOfYear = endCalendar.get(Calendar.DAY_OF_YEAR)
                val currentDayOfYear = currentCalendar.get(Calendar.DAY_OF_YEAR)
                
                // Проверяем, что текущий день находится в конце года или начале года
                (startDayOfYear > endDayOfYear) && 
                ((currentDayOfYear >= startDayOfYear && currentDayOfYear <= 365) || 
                 (currentDayOfYear >= 1 && currentDayOfYear <= endDayOfYear))
            }
        } else {
            // Для однодневного праздника
            val startCalendar = Calendar.getInstance().apply { time = startDate }
            currentCalendar.get(Calendar.DAY_OF_YEAR) == startCalendar.get(Calendar.DAY_OF_YEAR)
        }
    }
}