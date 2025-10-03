package com.example.embedded_example

/**
 * Конфигурация иконок приложения
 * Здесь определяются все доступные иконки и их настройки
 * 
 * Чтобы добавить новую иконку:
 * 1. Добавьте запись в AVAILABLE_ICONS в формате IconInfo("AliasName", "drawable_resource_name")
 * 2. Добавьте соответствующий activity-alias в AndroidManifest.xml
 * 3. Добавьте drawable ресурс в папку android/app/src/main/res/drawable/
 */
object IconConfig {
    // Основная иконка (по умолчанию)
    const val MAIN_ICON_ALIAS = "MainActivity"
    
    // Список всех доступных дополнительных иконок
    // Добавляйте сюда новые иконки при необходимости
    val AVAILABLE_ICONS = listOf(
        IconInfo("IconOne", "iconone"),
        IconInfo("IconTwo", "icontwo")
        // Пример добавления новой иконки:
        // IconInfo("IconThree", "iconthree"),
        // IconInfo("IconFour", "iconfour")
    )
    
    /**
     * Информация об иконке
     * @param aliasName имя activity-alias в AndroidManifest.xml
     * @param drawableResource имя ресурса drawable (без префикса @drawable/)
     */
    data class IconInfo(
        val aliasName: String,
        val drawableResource: String
    )
    
    /**
     * Получить информацию об иконке по имени alias
     */
    fun getIconInfo(aliasName: String): IconInfo? {
        return if (aliasName == MAIN_ICON_ALIAS) {
            // Для основной иконки возвращаем специальную информацию
            IconInfo(MAIN_ICON_ALIAS, "ic_launcher") // или другую иконку по умолчанию
        } else {
            AVAILABLE_ICONS.find { it.aliasName == aliasName }
        }
    }
    
    /**
     * Проверить, является ли иконка доступной
     */
    fun isIconAvailable(aliasName: String): Boolean {
        return aliasName == MAIN_ICON_ALIAS || AVAILABLE_ICONS.any { it.aliasName == aliasName }
    }
    
    /**
     * Получить список всех доступных иконок (включая основную)
     */
    fun getAllAvailableIcons(): List<String> {
        return listOf(MAIN_ICON_ALIAS) + AVAILABLE_ICONS.map { it.aliasName }
    }
}