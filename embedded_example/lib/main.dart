import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

void main() {
  runApp(const MyApp());
}

class AndroidDynamicIcon {
  static const MethodChannel _channel = MethodChannel('AndroidDynamicIcon');

  static Future<void> initialize({required List<String> classNames}) async {
    await _channel.invokeMethod('initialize', classNames);
  }

  Future<void> changeIcon({required List<String> classNames}) async {
    await _channel.invokeMethod('changeIcon', classNames);
  }

  // Метод для немедленной смены иконки
  Future<void> changeIconImmediate({required List<String> classNames}) async {
    await _channel.invokeMethod('changeIconImmediate', classNames);
  }
  
  // Метод для планирования смены иконки через заданное количество секунд
  Future<void> scheduleIconChange({required int seconds, required String targetIcon}) async {
    await _channel.invokeMethod('scheduleIconChange', {
      'seconds': seconds,
      'targetIcon': targetIcon
    });
  }
  
  // Метод для периодической проверки условий и автосмены иконки
  Future<void> schedulePeriodicCheck({
    required int intervalSeconds, 
    required String targetIcon,
    Map<String, dynamic>? conditions
  }) async {
    await _channel.invokeMethod('schedulePeriodicCheck', {
      'intervalSeconds': intervalSeconds,
      'targetIcon': targetIcon,
      'conditions': conditions ?? {}
    });
  }
}

class MyApp extends StatefulWidget {
  const MyApp({super.key});

  @override
  State<MyApp> createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> {
  final _androidDynamicIconPlugin = AndroidDynamicIcon();

  @override
  void initState() {
    AndroidDynamicIcon.initialize(
        classNames: ['MainActivity', 'IconOne', 'IconTwo']);
    
    // Планируем смену иконки через 15 секунд после запуска
    WidgetsBinding.instance.addPostFrameCallback((_) {
      _androidDynamicIconPlugin.scheduleIconChange(
        seconds: 15,
        targetIcon: 'IconOne'
      );
      
      // Планируем периодическую проверку каждые 15 минут
      _androidDynamicIconPlugin.schedulePeriodicCheck(
        intervalSeconds: 15 * 60, // 15 минут
        targetIcon: 'IconTwo',
        conditions: {
          'date': '04.10.2025', // Пример условия - конкретная дата
          'dateRangeStart': '30.12.2025', // Пример условия - диапазон дат (начало)
          'dateRangeEnd': '05.01.2026', // Пример условия - диапазон дат (конец)
        }
      );
    });
    
    super.initState();
  }

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      home: Scaffold(
        appBar: AppBar(
          title: const Text('Plugin example app'),
        ),
        body: Column(
          children: [
            TextButton(
              onPressed: () async {
                //To set Icon One
                await _androidDynamicIconPlugin
                    .changeIcon(classNames: ['IconOne', '']);
              },
              child: const Center(
                child: Text('Change Icon 1 (on app pause)'),
              ),
            ),
            TextButton(
              onPressed: () async {
                //To set Icon Two
                await _androidDynamicIconPlugin
                    .changeIcon(classNames: ['IconTwo', '']);
              },
              child: const Center(
                child: Text('Change Icon 2 (on app pause)'),
              ),
            ),
            TextButton(
              onPressed: () async {
                //To set Default Icon
                await _androidDynamicIconPlugin
                    .changeIcon(classNames: ['MainActivity', '']);
              },
              child: const Center(
                child: Text('Change to default (on app pause)'),
              ),
            ),
            TextButton(
              onPressed: () async {
                // Immediate change icon
                await _androidDynamicIconPlugin
                    .changeIconImmediate(classNames: ['IconOne', '']);
              },
              child: const Center(
                child: Text('Change Icon 1 (immediate)'),
              ),
            ),
            TextButton(
              onPressed: () async {
                // Immediate change icon
                await _androidDynamicIconPlugin
                    .changeIconImmediate(classNames: ['IconTwo', '']);
              },
              child: const Center(
                child: Text('Change Icon 2 (immediate)'),
              ),
            ),
            TextButton(
              onPressed: () async {
                // Immediate change to default icon
                await _androidDynamicIconPlugin
                    .changeIconImmediate(classNames: ['MainActivity', '']);
              },
              child: const Center(
                child: Text('Change to default (immediate)'),
              ),
            ),
          ],
        ),
      ),
    );
  }
}