package com.example.embedded_example

import android.content.Context
import android.content.pm.PackageManager
import android.content.ComponentName
import android.util.Log

/**
 * Менеджер управления иконками приложения
 * Централизованно управляет сменой иконок и их состоянием
 */
class IconManager(private val context: Context) {
    companion object {
        private const val TAG = "[IconManager]"
    }

    /**
     * Сменить иконку приложения
     * @param targetIconAlias имя target activity-alias (например, "IconOne", "IconTwo")
     */
    fun changeIcon(targetIconAlias: String) {
        try {
            if (!IconConfig.isIconAvailable(targetIconAlias)) {
                Log.e(TAG, "Icon alias '$targetIconAlias' is not available")
                return
            }

            val packageName = context.packageName
            val pm = context.packageManager
            
            // Отключаем все иконки, кроме целевой
            disableAllIconsExcept(pm, packageName, targetIconAlias)
            
            // Включаем целевую иконку
            enableIcon(pm, packageName, targetIconAlias)
            
            Log.d(TAG, "Icon successfully changed to $targetIconAlias")
        } catch (e: Exception) {
            Log.e(TAG, "Error changing icon to $targetIconAlias: ${e.message}")
        }
    }
    
    /**
     * Сбросить к основной иконке
     */
    fun resetToMainIcon() {
        changeIcon(IconConfig.MAIN_ICON_ALIAS)
    }
    
    /**
     * Получить текущую активную иконку
     */
    fun getCurrentActiveIcon(): String? {
        try {
            val packageName = context.packageName
            val pm = context.packageManager
            
            // Проверяем MainActivity
            val mainComponent = ComponentName(packageName, "$packageName.MainActivity")
            if (pm.getComponentEnabledSetting(mainComponent) == PackageManager.COMPONENT_ENABLED_STATE_ENABLED) {
                return IconConfig.MAIN_ICON_ALIAS
            }
            
            // Проверяем дополнительные иконки
            for (iconInfo in IconConfig.AVAILABLE_ICONS) {
                val component = ComponentName(packageName, "$packageName.${iconInfo.aliasName}")
                if (pm.getComponentEnabledSetting(component) == PackageManager.COMPONENT_ENABLED_STATE_ENABLED) {
                    return iconInfo.aliasName
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting current active icon: ${e.message}")
        }
        return null
    }
    
    private fun disableAllIconsExcept(pm: PackageManager, packageName: String, exceptAlias: String) {
        try {
            // Отключаем MainActivity, если это не целевая иконка
            if (exceptAlias != IconConfig.MAIN_ICON_ALIAS) {
                val mainComponent = ComponentName(packageName, "$packageName.MainActivity")
                pm.setComponentEnabledSetting(
                    mainComponent,
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                    PackageManager.DONT_KILL_APP
                )
            } else {
                // Если целевая иконка - основная, то отключаем все дополнительные
                for (iconInfo in IconConfig.AVAILABLE_ICONS) {
                    val component = ComponentName(packageName, "$packageName.${iconInfo.aliasName}")
                    pm.setComponentEnabledSetting(
                        component,
                        PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                        PackageManager.DONT_KILL_APP
                    )
                }
                return
            }
            
            // Отключаем все дополнительные иконки, кроме целевой
            for (iconInfo in IconConfig.AVAILABLE_ICONS) {
                if (iconInfo.aliasName != exceptAlias) {
                    val component = ComponentName(packageName, "$packageName.${iconInfo.aliasName}")
                    pm.setComponentEnabledSetting(
                        component,
                        PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                        PackageManager.DONT_KILL_APP
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error disabling icons: ${e.message}")
        }
    }
    
    private fun enableIcon(pm: PackageManager, packageName: String, targetAlias: String) {
        try {
            val componentName = if (targetAlias == IconConfig.MAIN_ICON_ALIAS) {
                "$packageName.MainActivity"
            } else {
                "$packageName.$targetAlias"
            }
            
            val component = ComponentName(packageName, componentName)
            pm.setComponentEnabledSetting(
                component,
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error enabling icon $targetAlias: ${e.message}")
        }
    }
}