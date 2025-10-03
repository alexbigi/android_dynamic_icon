# Удерживаем классы встроенного плагина от удаления или переименования
-keep class com.example.embedded_example.plugins.android_dynamic_icon.** { *; }
-dontwarn com.example.embedded_example.plugins.android_dynamic_icon.**

# Удерживаем FlutterPlugin классы
-keep class * implements io.flutter.embedding.engine.plugins.FlutterPlugin {
    public <init>();
}

# Удерживаем MethodChannel вызовы
-keep class * implements io.flutter.plugin.common.MethodChannel$MethodCallHandler {
    public <init>();
}

# Удерживаем основной класс плагина
-keep class com.example.embedded_example.plugins.android_dynamic_icon.AndroidDynamicIconPlugin { *; }

# Удерживаем реализацию вызовов
-keep class com.example.embedded_example.plugins.android_dynamic_icon.MethodCallImplementation { *; }

# Аннотация Keep
-keep @androidx.annotation.Keep class *
-keep class * {
    @androidx.annotation.Keep <methods>;
}
-keepclassmembers class * {
    @androidx.annotation.Keep <methods>;
}