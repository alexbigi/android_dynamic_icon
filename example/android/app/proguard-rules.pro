# Временно отключить R8 для тестирования
# -dontobfuscate
# -dontoptimize
# -dontpreverify

# Правила для нашего плагина
-keep class com.example.android_dynamic_icon.** { *; }
-dontwarn com.example.android_dynamic_icon.**

# Сохраняем FlutterPlugin классы
-keep class * implements io.flutter.embedding.engine.plugins.FlutterPlugin
-keep class * implements io.flutter.plugin.common.MethodChannel$MethodCallHandler {
    <methods>;
}

# Сохраняем конкретные классы
-keep class com.example.android_dynamic_icon.AndroidDynamicIconPlugin { *; }
-keep class com.example.android_dynamic_icon.MethodCallImplementation { *; }