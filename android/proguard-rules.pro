# Сохраняем все классы и методы плагина
-keep class com.example.android_dynamic_icon.** { *; }
-dontwarn com.example.android_dynamic_icon.**

# Сохраняем FlutterPlugin классы
-keep class * implements io.flutter.embedding.engine.plugins.FlutterPlugin

# Сохраняем методы, которые используются для регистрации плагинов
-keep class * {
    boolean usesFlutterBinding;
}

# Сохраняем все public методы в классах плагина
-keepclassmembers class com.example.android_dynamic_icon.** {
    public <methods>;
}

# Сохраняем методы, которые используются в MethodChannel
-keep class * implements io.flutter.plugin.common.MethodChannel$MethodCallHandler {
    <methods>;
}

# Сохраняем все конструкторы для плагинов
-keep public class * implements io.flutter.plugin.common.PluginRegistry$PluginRegistrantCallback

# Отключаем оптимизацию для плагинов
-keepattributes *Annotation*
-keep class io.flutter.plugins.** { *; }
-keep class * extends io.flutter.embedding.engine.FlutterEngine { *; }

# Сохраняем конкретные классы плагина
-keep class com.example.android_dynamic_icon.AndroidDynamicIconPlugin { *; }
-keep class com.example.android_dynamic_icon.MethodCallImplementation { *; }