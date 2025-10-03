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
  
  // Метод для планирования смены иконки с использованием Foreground Service и Exact Alarms
  Future<void> scheduleIconChangeWithExactAlarm({
    required int seconds,
    required String targetIcon
  }) async {
    await _channel.invokeMethod('scheduleIconChangeWithExactAlarm', {
      'seconds': seconds,
      'targetIcon': targetIcon
    });
  }
  
  // Метод для запуска сервиса смены иконки
  Future<void> startIconChangeService({
    required String targetIcon,
    int? scheduleTime
  }) async {
    await _channel.invokeMethod('startIconChangeService', {
      'targetIcon': targetIcon,
      'scheduleTime': scheduleTime
    });
  }
  
  // Метод для остановки сервиса смены иконки
  Future<void> stopIconChangeService() async {
    await _channel.invokeMethod('stopIconChangeService');
  }
  
  // Метод для отмены запланированной смены иконки
  Future<void> cancelScheduledIconChange({required String targetIcon}) async {
    await _channel.invokeMethod('cancelScheduledIconChange', {
      'targetIcon': targetIcon
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
      // Using the traditional method
      _androidDynamicIconPlugin.scheduleIconChange(
        seconds: 15,
        targetIcon: 'IconOne'
      );
      
      // Using the new exact alarm method - IconTwo after 20 seconds
      _androidDynamicIconPlugin.scheduleIconChangeWithExactAlarm(
        seconds: 20,
        targetIcon: 'IconTwo'
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
            const SizedBox(height: 20),
            const Text('New Foreground Service & Exact Alarms Features:',
              style: TextStyle(fontWeight: FontWeight.bold)),
            TextButton(
              onPressed: () async {
                // Schedule icon change with exact alarm (IconOne in 10 seconds)
                await _androidDynamicIconPlugin
                    .scheduleIconChangeWithExactAlarm(
                      seconds: 10,
                      targetIcon: 'IconOne');
              },
              child: const Center(
                child: Text('Schedule IconOne (Exact Alarm, 10s)'),
              ),
            ),
            TextButton(
              onPressed: () async {
                // Schedule icon change with exact alarm (IconTwo in 15 seconds)
                await _androidDynamicIconPlugin
                    .scheduleIconChangeWithExactAlarm(
                      seconds: 15,
                      targetIcon: 'IconTwo');
              },
              child: const Center(
                child: Text('Schedule IconTwo (Exact Alarm, 15s)'),
              ),
            ),
            TextButton(
              onPressed: () async {
                // Start the icon change service
                await _androidDynamicIconPlugin
                    .startIconChangeService(targetIcon: 'IconOne');
              },
              child: const Center(
                child: Text('Start Icon Change Service'),
              ),
            ),
            TextButton(
              onPressed: () async {
                // Stop the icon change service
                await _androidDynamicIconPlugin
                    .stopIconChangeService();
              },
              child: const Center(
                child: Text('Stop Icon Change Service'),
              ),
            ),
            TextButton(
              onPressed: () async {
                // Cancel scheduled icon change
                await _androidDynamicIconPlugin
                    .cancelScheduledIconChange(targetIcon: 'IconOne');
              },
              child: const Center(
                child: Text('Cancel Scheduled IconOne Change'),
              ),
            ),
          ],
        ),
      ),
    );
  }
}