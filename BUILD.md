# Сборка и упаковка приложения

> Требуется JDK 17+ с поддержкой `jpackage`.

## Быстрый обзор

- **macOS (Intel/Apple Silicon)**: DMG через `jpackage`.
- **Windows**: EXE через `jpackage`.
- **JavaFX** и зависимости упаковываются в classpath‑режиме (без `jlink`).

## macOS

### Intel macOS (x64)

```bash
mvn clean package
```

### Apple Silicon (arm64)

```bash
mvn clean package -Pmac-arm
```

Результат:

```
target/installer/
```

### Иконка

Используется:

```
src/main/resources/macos.icns
```

## Windows

```bash
mvn clean package -Pwindows
```

Результат:

```
target/installer/
```

### Иконка

Для Windows нужен `.ico`. Пока не задан.

## Почему не используется jlink

В проекте есть зависимости (например Apache POI), которые являются автоматическими модулями и не поддерживаются `jlink`. Поэтому сборка выполнена через classpath‑режим, а `jpackage` получает:

- входной каталог с зависимостями `target/app-libs`
- главный jar `target/${project.build.finalName}.jar`

## Частые проблемы

### Приложение стартует и сразу закрывается

Запусти из терминала и посмотри ошибку:

```bash
/Applications/Календарь\ занятий.app/Contents/MacOS/Календарь\ занятий
```

### JavaFX runtime components are missing

Убедись, что:

- сборка выполнена с `clean`;
- в `target/app-libs` есть `javafx-*-mac-aarch64.jar` (или `javafx-*-win.jar`).

## Примеры XLSX для импорта

Готовые файлы находятся в корне проекта:

- `lesson_import_sample.xlsx` — для импорта занятий
- `schedule_import_sample.xlsx` — для импорта авторасписания
