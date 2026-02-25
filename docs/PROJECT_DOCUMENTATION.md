# Техническая документация проекта SLSEleven

## 1. Обзор

`SLSEleven` — desktop-приложение на JavaFX для управления учебным расписанием.

Ключевая идея: разделить работу на

- фактические занятия в календаре (`lessons`),
- пул часов для автоназначения (`schedule_items`),
- настройки/ограничения распределения (`schedule_settings`, `schedule_subject_rules`, `instructors`, `rooms`, `instructor_duties`).

Все данные хранятся локально в SQLite.

## 2. Технологический стек

- Java 11 (`maven.compiler.release=11`)
- JavaFX 17 (`javafx-controls`, `javafx-fxml`)
- SQLite (`org.xerial:sqlite-jdbc`)
- Apache POI (`poi-ooxml`) для `XLSX`
- Maven
- JUnit 5

## 3. Запуск и инициализация приложения

Entry points:

- `com.ororura.slseleven.Launcher` (main)
- `com.ororura.slseleven.HelloApplication` (JavaFX `Application`)

Последовательность старта:

1. Создание каталога `~/.slseleven`.
2. Инициализация подключения к `~/.slseleven/lessons.db`.
3. `SchemaInitializer.init(provider)`:
   - создание таблиц;
   - мягкие `ALTER TABLE`;
   - нормализация структуры через временные таблицы;
   - заполнение defaults.
4. Сборка зависимостей слоями:
   - репозитории SQLite;
   - use case (`LessonUseCase`, `ScheduleUseCase`);
   - контроллеры JavaFX.
5. Загрузка `calendar-view.fxml` и запуск UI.

## 4. Архитектурная структура

### 4.1 Слои

- `domain/model` — доменные сущности.
- `domain/repository` — интерфейсы доступа к данным.
- `infrastructure/persistence/sqlite` — SQLite реализации репозиториев.
- `infrastructure/persistence/migrations` — инициализация/миграции схемы.
- `usecase` — бизнес-логика.
- `controller` — JavaFX контроллеры UI.
- `ui`, `util` — форматтеры, алерты, валидация, парсинг delimited-текста.

### 4.2 Главные контроллеры UI

- `CalendarController` — месяц, выбор даты, карточки занятий, календари/директории, печатные экспорты.
- `LessonsListController` — CRUD занятий, архив, импорт/экспорт, недельный отчёт.
- `SchedulePlannerController` — пул часов, лимиты, правила предметов, преподаватели/кабинеты/наряды, история, запуск авто- и перераспределения.

## 5. Доменная модель

### 5.1 Основные сущности

- `Lesson`
  - календарное занятие;
  - признаки `autoScheduled`, `archived`;
  - `durationHours` поддерживает блоки >1 часа.
- `ScheduleItem`
  - элемент пула распределения;
  - содержит `hours` и `consecutiveHours`.
- `SubjectScheduleRule`
  - allowed/exclusive дни;
  - `consecutiveHours`;
  - `maxLessonsPerDay`;
  - `fixedRoom`;
  - запреты `noConsecutiveWithSubjects`, `noSameDayWithSubjects`.
- `InstructorProfile`
  - имя + разрешённые дни.
- `InstructorDuty`
  - дата наряда преподавателя.
- `RoomProfile`
  - кабинет.
- `AppCalendar`
  - календарь + `directoryPath`.
- `ScheduleHistorySnapshot`
  - сериализованный снимок занятий и пула.

## 6. Схема базы данных (SQLite)

Инициализатор: `SchemaInitializer`.

### 6.1 Таблицы

#### `calendars`

- `id TEXT PRIMARY KEY`
- `name TEXT NOT NULL`
- `directory_path TEXT NOT NULL DEFAULT ''`
- `UNIQUE(directory_path, name)`

#### `app_settings`

- `key TEXT PRIMARY KEY`
- `value TEXT NOT NULL`

Используется для `active_calendar_id`.

#### `lessons`

- `id TEXT PRIMARY KEY`
- `calendar_id TEXT NOT NULL DEFAULT 'default'`
- `topic TEXT NOT NULL`
- `lesson_name TEXT NOT NULL`
- `class_name TEXT NOT NULL DEFAULT ''`
- `auto_scheduled INTEGER NOT NULL DEFAULT 0`
- `archived INTEGER NOT NULL DEFAULT 0`
- `duration_hours INTEGER NOT NULL DEFAULT 1`
- `time TEXT NOT NULL`
- `location TEXT NOT NULL`
- `instructor TEXT NOT NULL`
- `date TEXT NOT NULL`

#### `schedule_items`

- `id TEXT PRIMARY KEY`
- `calendar_id TEXT NOT NULL DEFAULT 'default'`
- `topic TEXT NOT NULL`
- `lesson_name TEXT NOT NULL`
- `class_name TEXT NOT NULL DEFAULT ''`
- `location TEXT NOT NULL`
- `instructor TEXT NOT NULL`
- `hours INTEGER NOT NULL`
- `consecutive_hours INTEGER NOT NULL DEFAULT 1`
- `created_at TEXT NOT NULL`

#### `schedule_settings`

- `calendar_id TEXT NOT NULL DEFAULT 'default'`
- `day_of_week INTEGER NOT NULL`
- `max_hours INTEGER NOT NULL`
- `PRIMARY KEY (calendar_id, day_of_week)`

#### `schedule_subject_rules`

- `calendar_id TEXT NOT NULL DEFAULT 'default'`
- `subject TEXT NOT NULL`
- `allowed_days INTEGER NOT NULL`
- `exclusive_days INTEGER NOT NULL`
- `consecutive_hours INTEGER NOT NULL DEFAULT 1`
- `max_lessons_per_day INTEGER NOT NULL DEFAULT 0`
- `fixed_room TEXT NOT NULL DEFAULT ''`
- `avoid_consecutive_with TEXT NOT NULL DEFAULT ''`
- `avoid_same_day_with TEXT NOT NULL DEFAULT ''`
- `PRIMARY KEY (calendar_id, subject)`

#### `instructors`

- `id TEXT PRIMARY KEY`
- `calendar_id TEXT NOT NULL DEFAULT 'default'`
- `name TEXT NOT NULL`
- `allowed_days INTEGER NOT NULL DEFAULT 127`

#### `rooms`

- `id TEXT PRIMARY KEY`
- `calendar_id TEXT NOT NULL DEFAULT 'default'`
- `name TEXT NOT NULL`

#### `instructor_duties`

- `calendar_id TEXT NOT NULL DEFAULT 'default'`
- `instructor_id TEXT NOT NULL`
- `duty_date TEXT NOT NULL`
- `PRIMARY KEY (calendar_id, instructor_id, duty_date)`

#### `schedule_history`

- `id TEXT PRIMARY KEY`
- `calendar_id TEXT NOT NULL DEFAULT 'default'`
- `created_at TEXT NOT NULL`
- `label TEXT NOT NULL`
- `lessons_blob TEXT NOT NULL`
- `schedule_items_blob TEXT NOT NULL`

### 6.2 Кодирование дней недели

Для `allowed_days`, `exclusive_days`, `instructors.allowed_days` используется bitmask:

- Monday = `1 << 0`
- Tuesday = `1 << 1`
- ...
- Sunday = `1 << 6`

`127` означает «разрешены все дни».

### 6.3 Миграции

`SchemaInitializer` делает:

- `CREATE TABLE IF NOT EXISTS` для целевой схемы;
- `ensureColumnExists` через безопасные `ALTER TABLE ... ADD COLUMN`;
- нормализацию таблиц:
  - `schedule_settings`
  - `schedule_subject_rules`
  - `calendars`
- вставку defaults:
  - календарь `default`
  - `active_calendar_id=default`
  - лимиты Пн–Пт = 2, Сб/Вс = 0.

## 7. Бизнес-логика

### 7.1 `LessonUseCase`

Ответственность:

- CRUD занятий текущего календаря;
- фильтрация активных/архивных;
- архивирование прошедших занятий;
- выборки по дате и диапазонам (в т.ч. across calendars).

Особенности:

- любые операции проверяют принадлежность к активному календарю;
- `durationHours <= 0` нормализуется к `1`.

### 7.2 `ScheduleUseCase`

Ответственность:

- управление пулом `schedule_items`;
- хранение/применение правил предметов;
- управление календарями, кабинетами, преподавателями и нарядами;
- автораспределение (`autoSchedule`);
- перераспределение (`reschedule`);
- возврат архивных/авто-занятий в пул;
- история снимков.

## 8. Алгоритм автоназначения

### 8.1 Слоты времени

Жёстко заданные стартовые слоты:

- `09:00`
- `09:50`
- `10:50`
- `11:40`
- `12:40`
- `13:30`
- `16:00`
- `16:50`

### 8.2 Общая схема

`autoSchedule(startDate, endDate)`:

1. Валидация диапазона дат.
2. Загрузка пула и настроек.
3. Проверка, что есть дневная ёмкость хотя бы в один день.
4. По дням диапазона:
   - вычисление доступных слотов с учётом уже существующих занятий;
   - подбор кандидата для каждого слота;
   - создание `Lesson` и уменьшение часов в пуле.
5. Удаление полностью израсходованных элементов пула.
6. Обновление часов у частично израсходованных.
7. Возврат `AutoScheduleResult`.

### 8.3 Подбор кандидата

Приоритеты:

1. валидный кандидат с **другим предметом** (чередование);
2. если тот же предмет, попытка ограничить серию до ~2 часов;
3. fallback на любого валидного кандидата.

### 8.4 Что валидируется

- часы по дню (`schedule_settings`);
- allowed/exclusive дни предмета;
- `maxLessonsPerDay` для предмета;
- запреты `noSameDayWithSubjects` (взаимные);
- запреты `noConsecutiveWithSubjects` (соседние слоты, взаимные);
- фиксированный кабинет (должен существовать);
- возможность разместить блок `consecutiveHours` в свободных слотах;
- доступность преподавателя:
  - его разрешённые дни;
  - если есть наряд на `date - 1`, кандидат отклоняется;
  - если наряд в текущий день, нельзя ставить блок, выходящий за первые 4 слота (`slotIndex + blockHours > 4`).

Fallback значения:

- при пустом списке кабинетов: `"Без кабинета"`;
- при пустом списке преподавателей: `"Без преподавателя"`.

### 8.5 Условия остановки

Планирование прерывается, если:

- часы пула закончились;
- достигнут `endDate` (если задан);
- более 60 дней подряд нет прогресса по созданию занятий.

## 9. Перераспределение (`reschedule`)

`reschedule(startDate, endDate)`:

1. Находит авто-занятия в диапазоне.
2. Если указан `endDate`, дополнительно включает авто-занятия **после** `endDate` для согласованности.
3. Возвращает их часы в агрегированный пул вместе с уже существующим пулом.
4. Удаляет старые авто-занятия.
5. Запускает `autoSchedule` заново.

## 10. История снимков

### 10.1 Создание

`createHistorySnapshot(label)` сохраняет:

- все текущие `lessons`;
- весь текущий `schedule_items`.

### 10.2 Формат

Сериализация в tab-delimited строки с Base64 для текстовых полей.

### 10.3 Восстановление

`restoreFromHistory(historyId)`:

- очищает текущие `lessons` и `schedule_items` активного календаря;
- восстанавливает состояние из snapshot.

## 11. Импорт/экспорт: технические детали

### 11.1 Парсер delimited текста

`DelimitedText.parse`:

- автоопределение разделителя: `\t`, `;`, `,`;
- поддержка кавычек и escaped `""`;
- нормализация заголовков (`lowercase`, удаление пробелов/`_`/`-`).

### 11.2 Импорт занятий

`LessonsListController`:

- поддержка заголовков RU/EN alias;
- обязательные поля: `time`, `topic`, `lesson`, `location`, `instructor`;
- `date` optional (default: `LocalDate.now()`);
- `class` optional (fallback: `lesson`).

### 11.3 Импорт пула

`SchedulePlannerController`:

- обязательные поля: `topic`, `lesson`, `hours`;
- `class` optional (fallback: `lesson`);
- `location`/`instructor` optional;
- `consecutiveHours` подтягивается из правил предмета (если есть).

## 12. Мультикалендарность

### 12.1 Изоляция данных

Почти все таблицы содержат `calendar_id`. Use-case работает в контексте активного календаря.

### 12.2 Режим «Все календари»

UI может агрегировать просмотры across calendars (для календарной сетки и чтения), но изменение данных в этом режиме ограничено.

### 12.3 Удаление календаря

При удалении календаря удаляются связанные данные из:

- `lessons`
- `schedule_items`
- `schedule_settings`
- `schedule_subject_rules`
- `instructor_duties`
- `instructors`
- `rooms`
- `schedule_history`
- `calendars`

Если удаляется активный календарь, выбирается fallback.

## 13. Сборка и упаковка

### 13.1 Запуск

```bash
./mvnw clean javafx:run
```

### 13.2 Тесты

```bash
./mvnw test
```

### 13.3 Пакетирование

- macOS x64: `./mvnw clean package -Pmac-x64`
- macOS arm64: `./mvnw clean package -Pmac-arm`
- Windows: `./mvnw clean package -Pwindows`

Результат: `target/installer/`.

### 13.4 Почему classpath-mode, а не jlink

Из-за зависимостей с автоматическими модулями (включая Apache POI) финальная упаковка строится в classpath-режиме через `jpackage --input target/app-libs`.

## 14. Тестовое покрытие

### 14.1 Интеграционные тесты use case

`ScheduleUseCaseIntegrationTest` покрывает:

- уважение `endDate` и остатка часов;
- перераспределение и сбор пула;
- open-ended диапазоны;
- валидацию диапазонов;
- перенос архива в пул;
- блоки `consecutiveHours`;
- чередование предметов;
- запреты подряд/в один день;
- `maxLessonsPerDay`.

### 14.2 Репозиторные тесты

- `LessonRepositorySQLiteTest`
- `ScheduleItemRepositorySQLiteTest`
- `ScheduleSettingsRepositorySQLiteTest`

Покрывают CRUD и базовые правила сохранения/чтения.

## 15. Ограничения текущей реализации

- встроенный фиксированный набор слотов времени;
- история хранится как сериализованные blob-строки, а не нормализованные версии;
- нет внешней серверной синхронизации;
- нет отдельной миграционной системы по версиям (Liquibase/Flyway).

## 16. Направления развития

- выделить scheduling engine в отдельный сервис;
- перейти на версионируемые SQL-миграции;
- добавить сценарные e2e-тесты UI;
- добавить экспорт/импорт настроек календаря целиком;
- добавить dry-run режим распределения с объяснением причин отказа по каждой строке пула.

## 17. Связанные документы

- [README](../README.md)
- [Руководство пользователя](USER_GUIDE.md)
- [BUILD.md](../BUILD.md)
- [BUILD_DMG.md](../BUILD_DMG.md)
- [BUILD_DMG_WITH_JRE.md](../BUILD_DMG_WITH_JRE.md)
