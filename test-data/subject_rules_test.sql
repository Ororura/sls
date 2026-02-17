-- Тестовые правила для active calendar = 'default'
-- 1) Строевые занятия не ставить подряд с физподготовкой
-- 2) Строевые занятия и физподготовку не ставить в один день

DELETE FROM schedule_subject_rules WHERE calendar_id = 'default' AND subject IN ('Строевые занятия', 'Физическая подготовка');

INSERT INTO schedule_subject_rules (
    calendar_id,
    subject,
    allowed_days,
    exclusive_days,
    consecutive_hours,
    fixed_room,
    avoid_consecutive_with,
    avoid_same_day_with
) VALUES
(
    'default',
    'Строевые занятия',
    127,
    0,
    1,
    '',
    'физическая подготовка',
    'физическая подготовка'
),
(
    'default',
    'Физическая подготовка',
    127,
    0,
    1,
    '',
    'строевые занятия',
    'строевые занятия'
);
