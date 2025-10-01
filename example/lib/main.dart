import 'package:flutter/material.dart';
import 'package:android_dynamic_icon/android_dynamic_icon.dart';

void main() {
  runApp(const MyApp());
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
                child: Text('Change Icon 1'),
              ),
            ),
            TextButton(
              onPressed: () async {
                // //To set Icon Two
                await _androidDynamicIconPlugin
                    .changeIcon(classNames: ['IconTwo', '']);
              },
              child: const Center(
                child: Text('Change Icon 2'),
              ),
            ),
            TextButton(
              onPressed: () async {
                // //To set Default Icon
                await _androidDynamicIconPlugin
                    .changeIcon(classNames: ['MainActivity', '']);
              },
              child: const Center(
                child: Text('Change to default'),
              ),
            ),
          ],
        ),
      ),
    );
  }
}