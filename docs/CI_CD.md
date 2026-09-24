# CI/CD для SLSEleven

Это **десктопное JavaFX-приложение**, а не сервер. CD здесь означает публикацию установщиков в GitHub Releases, а не деплой в Docker/VPS. SQLite хранится **локально** у пользователя в `~/.slseleven/lessons.db` и не входит в релиз.

## Что запускается автоматически

| Событие | Результат |
| --- | --- |
| Pull request | Сборка и JUnit 5 на Linux, затем DMG (Intel), DMG (Apple Silicon), EXE (Windows x64). |
| Push в `main` | Те же проверки и сборки; установщики доступны в артефактах Actions 7 дней. |
| Push тега вида `v1.2.3` | Повторная проверка тестов и сборка всех платформ. Если все три сборки успешны, создаётся GitHub Release с установщиками и `SHA256SUMS.txt`. |

Теги другой формы, например `v1.2.3-rc1`, не поддерживаются: workflow завершится ошибкой до публикации. Версия установщиков берётся из тега через Maven property `app.version`; `pom.xml` при каждом релизе менять не нужно.

## Первое включение

1. В GitHub откройте **Settings → Actions → General** и разрешите GitHub Actions и используемые marketplace actions, если они ограничены настройками репозитория/аккаунта.
2. В **Settings → Actions → General → Workflow permissions** проверьте, что workflows разрешено создавать релизы через `GITHUB_TOKEN`. В самом release workflow для публикации запрашивается `contents: write`; отдельный персональный токен и Secrets не требуются.
3. Слейте PR с CI/CD в `main`. Дождитесь зелёного **CI** во вкладке Actions.
4. Рекомендуется создать в **Settings → Rules → Rulesets** защиту `main`: требовать Pull Request и успешные проверки **Tests (Linux)**, **Installer (macos-intel)**, **Installer (macos-apple-silicon)**, **Installer (windows-x64)**. Настройки Rulesets задаются владельцем репозитория отдельно, файлы workflow их не включают.

## Выпуск версии

Из локального клона, обновлённого до `origin/main`:

```bash
git switch main
git pull --ff-only origin main
git tag -a v1.0.0 -m "SLSEleven 1.0.0"
git push origin v1.0.0
```

Подставьте следующую неиспользованную версию. **Не перемещайте уже опубликованный тег**: создавайте следующий релиз отдельным тегом.

Проверьте **Actions → Release**, затем **Releases** на странице репозитория. Релиз не публикуется, если хотя бы одна из платформ не собрана или не прошли тесты. Артефакты CI — предварительные сборки; для пользователей предназначены файлы релиза.

## Локальная проверка

```bash
./mvnw -B -ntp clean verify
```

Чтобы собрать установщик, выполните на соответствующей ОС:

```bash
./mvnw -B -ntp clean package -Pmac-arm
./mvnw -B -ntp clean package -Pmac-x64
# Windows PowerShell:
.\mvnw.cmd -B -ntp clean package -Pwindows
```

Используйте для сборки macOS ARM машину для `mac-arm`, macOS Intel для `mac-x64`, Windows x64 для `windows`. Сборки macOS и Windows не кросс-компилируются на Linux. GitHub Actions использует JDK 21 с `jpackage`; исполняемая Java упаковывается в установщик.

## Подписи и установка

CI выпускает **неподписанные** DMG и EXE. macOS Gatekeeper и Windows SmartScreen могут предупредить пользователя. В проекте **пока не настроены** Apple Developer ID, notarization и Authenticode: для них нужны отдельные учётные записи/сертификаты и секреты. Наличие автоматического Release само по себе не означает, что инсталлятор подписан.

## Если сборка упала

Откройте **Actions → CI / Release → конкретный job**. Отчёты JUnit из CI доступны как `junit-reports` даже при падении тестов. При проблеме только одной ОС посмотрите её job; при падении `verify` публикация релиза не запускается. Windows EXE создаётся через WiX Toolset из образа `windows-2022`.
