package com.example.embedded_example

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import java.util.*

/**
 * Класс для работы с сохранением и загрузкой конфигурации смены иконок
 * Использует простую сериализацию с разделителями
 */
class IconChangeConfigManager(private val context: Context) {
    companion object {
        private const val TAG = "[IconChangeConfigManager]"
        private const val PREFS_NAME = "IconChangePrefs"
        private const val CONFIG_KEY = "autonomous_config"
    }
    
    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
    
    /**
     * Сохранить конфигурацию
     */
    fun saveConfig(config: IconChangeConfig) {
        try {
            val serialized = serializeConfig(config)
            with(prefs.edit()) {
                putString(CONFIG_KEY, serialized)
                apply()
            }
            Log.d(TAG, "Configuration saved successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error saving configuration: ${e.message}", e)
        }
    }
    
    /**
     * Загрузить конфигурацию
     */
    fun loadConfig(): IconChangeConfig? {
        return try {
            val serialized = prefs.getString(CONFIG_KEY, null)
            if (serialized != null) {
                deserializeConfig(serialized)
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading configuration: ${e.message}", e)
            null
        }
    }
    
    /**
     * Очистить saved конфигурацию
     */
    fun clearConfig() {
        with(prefs.edit()) {
            remove(CONFIG_KEY)
            apply()
        }
        Log.d(TAG, "Configuration cleared")
    }
    
    /**
     * Сериализовать конфигурацию в строку
     */
    private fun serializeConfig(config: IconChangeConfig): String {
        val holidaysData = config.holidays.map { holiday ->
            "${holiday.iconAlias}|${holiday.startDate.time}|${holiday.endDate?.time}|${holiday.name}"
        }.joinToString(";")
        
        return "${config.defaultIcon}|${config.checkIntervalSeconds}|$holidaysData"
    }
    
    /**
     * Десериализовать конфигурацию из строки
     */
    private fun deserializeConfig(serialized: String): IconChangeConfig? {
        try {
            val parts = serialized.split("|")
            if (parts.size < 3) return null
            
            val defaultIcon = parts[0]
            val checkInterval = parts[1].toIntOrNull() ?: 60
            val holidaysData = parts.drop(2).joinToString("|").split(";")
            
            val holidays = holidaysData.filter { it.isNotEmpty() }.mapNotNull { holidayStr ->
                val holidayParts = holidayStr.split("|")
                if (holidayParts.size >= 4) {
                    val iconAlias = holidayParts[0]
                    val startDate = Date(holidayParts[1].toLong())
                    val endDate = if (holidayParts[2].isNotEmpty() && holidayParts[2] != "null") Date(holidayParts[2].toLong()) else null
                    val name = holidayParts[3]
                    
                    Holiday(iconAlias, startDate, endDate, name)
                } else {
                    null
                }
            }
            
            return IconChangeConfig(holidays, defaultIcon, checkInterval)
        } catch (e: Exception) {
            Log.e(TAG, "Error deserializing config: ${e.message}", e)
            return null
        }
    }
}