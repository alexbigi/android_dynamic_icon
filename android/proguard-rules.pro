# Сохраняем все классы плагина
-keep class com.example.android_dynamic_icon.** { *; }
-dontwarn com.example.android_dynamic_icon.**

# Сохраняем основной класс плагина для FlutterPlugin
-keep class com.example.android_dynamic_icon.AndroidDynamicIconPlugin { *; }

# Сохраняем реализацию вызовов методов
-keep class com.example.android_dynamic_icon.MethodCallImplementation { *; }

# Сохраняем FlutterPlugin интерфейс реализации
-keep class * implements io.flutter.embedding.engine.plugins.FlutterPlugin {
    public <init>();
}

# Сохраняем MethodChannel вызовы
-keep class * implements io.flutter.plugin.common.MethodChannel$MethodCallHandler {
    public <init>();
}

# Сохраняем все методы в плагине
-keepclassmembers class com.example.android_dynamic_icon.AndroidDynamicIconPlugin {
    public <methods>;
}

# Сохраняем аннотации и сигнатуры
-keepattributes Signature, InnerClasses, EnclosingMethod

# Сохраняем все методы, которые могут быть вызваны нативно
-keepclasseswithmembernames class * {
    native <methods>;
}

# Сохраняем конструкторы FlutterPlugin
-keep public class * {
    public <init>(io.flutter.plugin.common.BinaryMessenger);
}

# Сохраняем FlutterEngine
-keep class io.flutter.embedding.engine.FlutterEngine { *; }

# Сохраняем все классы, которые могут использоваться для регистрации плагинов
-keep class * implements io.flutter.plugin.common.PluginRegistry$PluginRegistrantCallback

# Сохраняем все классы с регистрацией
-keep class * {
    public static * registerWith(...);
}