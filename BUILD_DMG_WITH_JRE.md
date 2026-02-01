# Сборка DMG с встроенным JRE (macOS)

> Требуется JDK 17+ с поддержкой `jpackage`.

## Важно

В проекте есть зависимости (Apache POI), которые являются автоматическими модулями и **не совместимы с jlink**. Поэтому сборка выполняется в **classpath‑режиме** (без `jlink`).

## Быстрый старт (Intel macOS)

```bash
mvn clean package
```

## Apple Silicon (arm64)

```bash
mvn clean package -Pmac-arm
```

## Что делает сборка

1. Компилирует проект
2. Копирует все runtime‑зависимости в `target/app-libs`
3. Копирует основной jar в `target/app-libs`
4. Создаёт DMG через `jpackage`

## Выходные файлы

DMG находится в:

```
target/installer/
```

## Иконка приложения

Используется:

```
src/main/resources/macos.icns
```

## Частые ошибки

### JavaFX runtime components are missing

Проверьте, что в `target/app-libs` есть JavaFX jars:

```
ls target/app-libs | grep javafx
```

### DMG не создаётся

Убедитесь, что `jpackage` доступен:

```
jpackage --version
```
