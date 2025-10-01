# Удерживаем классы плагина от удаления или переименования
-keep class com.example.android_dynamic_icon.** { *; }
-dontwarn com.example.android_dynamic_icon.**

# Удерживаем методы, которые используются в Flutter
-keepclassmembers class * {
    @io.flutter.plugin.common.MethodChannel$MethodCallHandler <methods>;
}

# Удерживаем FlutterPlugin классы
-keep class * implements io.flutter.embedding.engine.plugins.FlutterPlugin {
    public <init>();
}