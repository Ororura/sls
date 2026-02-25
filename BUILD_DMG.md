# Сборка DMG (macOS)

> Требуется JDK 17+ с поддержкой `jpackage`.

## Быстрый старт (Intel macOS)

```bash
./mvnw clean package -Pmac-x64
```

## Apple Silicon (arm64)

```bash
./mvnw clean package -Pmac-arm
```

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

## Как устроена сборка

Проект собирается в **classpath‑режиме** (без `jlink`), т.к. Apache POI является автоматическим модулем и несовместим с `jlink`.

`jpackage` получает:

- `--input target/app-libs` (все зависимости)
- `--main-jar target/${project.build.finalName}.jar`
- `--main-class com.ororura.slseleven.Launcher`

## Частые ошибки

### JavaFX runtime components are missing

Проверьте, что в `target/app-libs` есть JavaFX jars:

```
ls target/app-libs | grep javafx
```

### DMG не создаётся

Убедитесь, что:

- используется `./mvnw clean package`;
- `jpackage` доступен (`jpackage --version`).

## См. также

- `BUILD.md`
- `docs/USER_GUIDE.md`
