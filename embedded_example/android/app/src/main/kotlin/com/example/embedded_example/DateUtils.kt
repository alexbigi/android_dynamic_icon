package com.example.embedded_example

import java.text.SimpleDateFormat
import java.util.*

/**
 * Общий класс для работы с датами
 */
object DateUtils {
    val DATE_FORMAT = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
    
    /**
     * Разобрать строку даты в объект Date
     */
    fun parseDate(dateString: String): Date? {
        return try {
            DATE_FORMAT.parse(dateString)
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Форматировать Date в строку
     */
    fun formatDate(date: Date): String {
        return DATE_FORMAT.format(date)
    }
}