# Android Dynamic Icon Manager

## Система управления динамическими иконками для Flutter приложений

### Обзор

Эта система позволяет динамически менять иконку Android приложения во время выполнения. Иконка меняется после сворачивания или убийства приложения, чтобы не нарушать пользовательский опыт.

### Как использовать

#### В Dart коде:

```dart
// Инициализация (выполняется один раз при запуске)
await AndroidDynamicIcon.initialize(classNames: ['MainActivity', 'IconOne', 'IconTwo']);

// Отложенная смена иконки (иконка меняется при сворачивании/убийстве приложения)
await AndroidDynamicIcon.changeIcon(classNames: ['IconOne', '']);

// Немедленная смена иконки (иконка меняется сразу)
await AndroidDynamicIcon.changeIconImmediate(classNames: ['IconTwo', '']);

// Планирование смены иконки через заданное количество секунд
await AndroidDynamicIcon.scheduleIconChange(seconds: 15, targetIcon: 'IconOne');

// Планирование периодической проверки условий и автосмены иконки
await AndroidDynamicIcon.schedulePeriodicCheck(
  intervalSeconds: 15 * 60, // 15 минут
  targetIcon: 'IconTwo',
  conditions: {
    'date': '04.10.2025', // Конкретная дата
    'dateRangeStart': '30.12.2025', // Диапазон дат (начало)
    'dateRangeEnd': '05.01.2026', // Диапазон дат (конец)
  }
);
```

### Как добавить новую иконку

Чтобы добавить новую иконку в систему, выполните следующие шаги:

#### 1. Добавьте информацию об иконке в конфигурацию

Откройте файл `android/app/src/main/kotlin/com/example/embedded_example/IconConfig.kt` и добавьте новую иконку в список `AVAILABLE_ICONS`:

```kotlin
val AVAILABLE_ICONS = listOf(
    IconInfo("IconOne", "iconone"),
    IconInfo("IconTwo", "icontwo"),
    IconInfo("IconThree", "iconthree") // Добавляем новую иконку
)
```

#### 2. Добавьте activity-alias в AndroidManifest.xml

Добавьте новый `activity-alias` в файл `android/app/src/main/AndroidManifest.xml`:

```xml
<activity-alias
    android:label="app"
    android:icon="@drawable/iconthree" <!-- Укажите ваш drawable ресурс -->
    android:name=".IconThree"           <!-- Укажите уникальное имя -->
    android:enabled="false"
    android:exported="true"
    android:targetActivity=".MainActivity">
    <intent-filter>
        <action android:name="android.intent.action.MAIN" />
        <category android:name="android.intent.category.LAUNCHER" />
    </intent-filter>
</activity-alias>
```

#### 3. Добавьте drawable ресурс

Поместите файл изображения иконки в папку `android/app/src/main/res/drawable/`:
- Назовите файл в соответствии с указанным в `IconConfig.kt` (например, `iconthree.png`)
- Убедитесь, что изображение имеет правильный размер (обычно 48x48, 72x72, 96x96, 144x144, 192x192 пикселей)

#### 4. Используйте новую иконку в Dart коде

Теперь вы можете использовать новую иконку в Dart коде:

```dart
// Отложенная смена иконки
await AndroidDynamicIcon.changeIcon(classNames: ['IconThree', '']);

// Немедленная смена иконки
await AndroidDynamicIcon.changeIconImmediate(classNames: ['IconThree', '']);
```

### Как это работает

#### Архитектура системы

1. **IconConfig** - центральная конфигурация всех доступных иконок
2. **IconManager** - управляет фактической сменой иконок через PackageManager
3. **DeferredIconChangeManager** - управляет отложенной сменой иконок
4. **MainApplication** - отслеживает жизненный цикл приложения
5. **MainActivity** - предоставляет интерфейс для Dart кода

#### Режимы смены иконок

1. **Отложенная смена** (`changeIcon`):
   - Иконка планируется к смене
   - Фактическая смена происходит при сворачивании или убийстве приложения
   - Предотвращает резкий перезапуск во время использования

2. **Немедленная смена** (`changeIconImmediate`):
   - Иконка меняется сразу
   - Может вызвать кратковременное обновление лаунчера

#### Отслеживание жизненного цикла

Система отслеживает когда приложение:
- Переходит в фоновый режим (onPause)
- Возвращается на передний план (onResume)

При переходе в фоновый режим, если запланирована смена иконки, она выполняется с небольшой задержкой (1 секунда) для лучшего пользовательского опыта.

### Преимущества

1. **Безопасность UX** - иконка не меняется во время активного использования
2. **Гибкость** - легко добавлять/удалять иконки без изменения кода
3. **Надежность** - централизованное управление снижает вероятность ошибок
4. **Поддержка** - понятная структура упрощает обслуживание
5. **Совместимость** - сохранена полная совместимость с существующим Dart API

### Устранение неполадок

#### Иконка не меняется

1. Убедитесь, что иконка добавлена в `IconConfig.AVAILABLE_ICONS`
2. Проверьте, что `activity-alias` правильно добавлен в `AndroidManifest.xml`
3. Убедитесь, что drawable ресурс существует

#### Приложение перезапускается во время использования

Используйте метод `changeIcon()` вместо `changeIconImmediate()` для отложенной смены иконки.

#### Иконка не отображается правильно

1. Проверьте размеры drawable ресурса (должны быть стандартными: 48x48, 72x72, 96x96, 144x144, 192x192)
2. Убедитесь, что формат изображения поддерживается (обычно PNG)
3. Проверьте, что имя drawable ресурса совпадает с указанным в `IconConfig.kt`

### Лицензия

MIT