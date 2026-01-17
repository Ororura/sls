# Инструкция по сборке DMG файла

## Требования

1. **JDK 14 или выше** с поддержкой `jpackage` (рекомендуется JDK 17+)
2. **macOS** - DMG можно собрать только на macOS
3. **Maven 3.6+**

## Проверка JDK

Убедитесь, что у вас установлен JDK с поддержкой jpackage:

```bash
java -version
jpackage --version
```

Если `jpackage` не найден, установите JDK 14+ (например, через Homebrew: `brew install openjdk@17`)

## Сборка DMG

### Вариант 1: Полная сборка (рекомендуется)

```bash
mvn clean package
```

Эта команда:
1. Очистит предыдущие сборки
2. Скомпилирует проект
3. Создаст runtime image через jlink
4. Создаст DMG файл через jpackage

DMG файл будет находиться в: `target/installer/Календарь занятий-1.0.0.dmg`

### Вариант 2: Только создание runtime image

```bash
mvn clean javafx:jlink@create-runtime-image
```

### Вариант 3: Только создание DMG (после создания runtime image)

```bash
mvn exec:exec@create-dmg
```

## Настройка параметров

Вы можете изменить параметры приложения в `pom.xml`:

```xml
<app.name>Календарь занятий</app.name>
<app.version>1.0.0</app.version>
<app.vendor>SLSEleven</app.vendor>
<app.identifier>com.ororura.slseleven</app.identifier>
```

## Добавление иконки (опционально)

Если хотите добавить иконку приложения, создайте файл `src/main/resources/icon.icns` и добавьте в конфигурацию jpackage:

```xml
<argument>--icon</argument>
<argument>${project.basedir}/src/main/resources/icon.icns</argument>
```

## Возможные проблемы

### Ошибка: "jpackage: command not found"
- Установите JDK 14+ с поддержкой jpackage
- Убедитесь, что JDK в PATH

### Ошибка: "No suitable JavaFX runtime found"
- Убедитесь, что JavaFX зависимости правильно настроены
- Проверьте версию JavaFX в pom.xml

### Ошибка при создании DMG
- Убедитесь, что вы на macOS
- Проверьте права доступа к директории target
