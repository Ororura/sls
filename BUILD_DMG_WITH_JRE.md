# Инструкция по сборке DMG с встроенным JRE

## Обзор

Эта конфигурация создает самодостаточный DMG файл, который включает в себя:
- ✅ Полный Java Runtime Environment (JRE)
- ✅ Все необходимые модули JavaFX
- ✅ SQLite JDBC драйвер
- ✅ Все зависимости приложения

**Пользователям НЕ нужно устанавливать Java отдельно!**

## Требования

1. **JDK 14 или выше** с поддержкой `jpackage` и `jlink` (рекомендуется JDK 17+)
2. **macOS** - DMG можно собрать только на macOS
3. **Maven 3.6+**

## Проверка JDK

Убедитесь, что у вас установлен JDK с поддержкой jpackage и jlink:

```bash
java -version
jpackage --version
jlink --version
```

Если команды не найдены, установите JDK 17+:
```bash
brew install openjdk@17
```

## Процесс сборки

### Полная сборка (рекомендуется)

```bash
mvn clean package
```

Эта команда автоматически:
1. ✅ Компилирует проект
2. ✅ Создает runtime image с JRE через `jlink`
3. ✅ Копирует SQLite JDBC в runtime image
4. ✅ Создает DMG файл через `jpackage`

**DMG файл будет находиться в:** `target/installer/Календарь занятий-1.0.0.dmg`

### Пошаговая сборка

Если хотите выполнить шаги отдельно:

1. **Создать runtime image с JRE:**
```bash
mvn clean javafx:jlink@create-runtime-image
```

2. **Скопировать SQLite JDBC:**
```bash
mvn dependency:copy@copy-sqlite-jdbc
```

3. **Создать DMG:**
```bash
mvn exec:exec@create-dmg
```

## Структура сборки

После выполнения `mvn clean package`:

```
target/
├── calendar-runtime/          # Runtime image с JRE
│   ├── bin/                   # Исполняемые файлы
│   ├── lib/                   # Библиотеки (включая SQLite JDBC)
│   └── ...
├── installer/                 # Готовые установщики
│   └── Календарь занятий-1.0.0.dmg
└── classes/                   # Скомпилированные классы
```

## Настройка параметров

Вы можете изменить параметры приложения в `pom.xml`:

```xml
<properties>
    <app.name>Календарь занятий</app.name>
    <app.version>1.0.0</app.version>
    <app.vendor>SLSEleven</app.vendor>
    <app.identifier>com.ororura.slseleven</app.identifier>
</properties>
```

## Добавление иконки (опционально)

1. Создайте файл `src/main/resources/icon.icns`
2. Добавьте в конфигурацию jpackage в `pom.xml`:

```xml
<argument>--icon</argument>
<argument>${project.basedir}/src/main/resources/icon.icns</argument>
```

## Размер итогового DMG

Ожидаемый размер: **~150-200 MB** (включая JRE)

Это нормально, так как включает:
- JRE (~100-120 MB)
- JavaFX модули (~30-40 MB)
- SQLite JDBC (~1 MB)
- Ваше приложение (~1-2 MB)

## Возможные проблемы

### Ошибка: "jpackage: command not found"
- Установите JDK 14+ с поддержкой jpackage
- Убедитесь, что JDK в PATH

### Ошибка: "No suitable JavaFX runtime found"
- Проверьте версию JavaFX в pom.xml
- Убедитесь, что модули правильно указаны в jlink конфигурации

### Ошибка: "SQLite JDBC not found"
- Убедитесь, что maven-dependency-plugin скопировал JAR файл
- Проверьте путь: `target/calendar-runtime/lib/sqlite-jdbc-3.51.1.0.jar`

### Ошибка при создании DMG
- Убедитесь, что вы на macOS
- Проверьте права доступа к директории target
- Убедитесь, что runtime image создан перед запуском jpackage

### Большой размер DMG
- Это нормально для приложений с встроенным JRE
- Можно уменьшить, используя `--compress=2` (уже включено)
- Можно использовать `--strip-native-commands` для дополнительной оптимизации

## Тестирование DMG

1. Откройте созданный DMG файл
2. Перетащите приложение в Applications
3. Запустите приложение
4. Убедитесь, что оно работает без установленного Java

## Дополнительные опции jpackage

Вы можете добавить дополнительные опции в конфигурацию jpackage:

```xml
<!-- Подпись приложения (требует сертификат разработчика) -->
<argument>--mac-sign</argument>
<argument>--mac-signing-key-user-name</argument>
<argument>Developer ID Application: Your Name</argument>

<!-- Минимальная версия macOS -->
<argument>--mac-app-store</argument>

<!-- Дополнительные JVM опции -->
<argument>--java-options</argument>
<argument>-Djava.library.path=/path/to/native/libs</argument>
```

## Оптимизация размера

Для уменьшения размера можно:

1. Использовать более агрессивное сжатие:
```xml
<compress>2</compress>  <!-- Уже включено -->
```

2. Исключить ненужные модули из runtime image

3. Использовать `--strip-native-commands` в jpackage

## Проверка содержимого runtime image

После создания runtime image можно проверить его содержимое:

```bash
ls -la target/calendar-runtime/lib/
```

Должны быть видны:
- Модули JavaFX
- SQLite JDBC JAR
- Другие зависимости
