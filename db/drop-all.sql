-- ОЧИСТКА СХЕМЫ. ДЕЙСТВИЕ НЕОБРАТИМО.
--
-- На helios база studs общая на весь курс, у студента есть только собственная схема
-- (права CREATE на базу нет, проверено: has_database_privilege(..., 'CREATE') = false).
-- Схема s409449 была занята таблицами Camunda от курса BLPS; по решению владельца
-- они удаляются, чтобы освободить место под ЛР2.
--
-- После выполнения развёрнутое приложение BLPS работать перестанет: его датасорс
-- смотрит в эти же таблицы. Бэкапа нет.
--
-- Запуск:
--   psql -h localhost -U <логин> -d studs -f db/drop-all.sql

\echo 'Текущая схема:'
SELECT current_schema();

\echo 'Будет удалено объектов:'
SELECT
    (SELECT count(*) FROM information_schema.tables
      WHERE table_schema = current_schema() AND table_type = 'BASE TABLE') AS tables,
    (SELECT count(*) FROM information_schema.views
      WHERE table_schema = current_schema())                               AS views,
    (SELECT count(*) FROM information_schema.sequences
      WHERE sequence_schema = current_schema())                            AS sequences;

DO $$
DECLARE
    obj record;
BEGIN
    -- Представления сносим первыми: они могут зависеть от таблиц.
    FOR obj IN
        SELECT table_name FROM information_schema.views
        WHERE table_schema = current_schema()
    LOOP
        EXECUTE format('DROP VIEW IF EXISTS %I CASCADE', obj.table_name);
    END LOOP;

    -- CASCADE снимает внешние ключи между таблицами, поэтому порядок удаления не важен.
    FOR obj IN
        SELECT table_name FROM information_schema.tables
        WHERE table_schema = current_schema() AND table_type = 'BASE TABLE'
    LOOP
        EXECUTE format('DROP TABLE IF EXISTS %I CASCADE', obj.table_name);
    END LOOP;

    -- Последовательности, не привязанные к колонкам (привязанные уходят вместе с таблицами).
    FOR obj IN
        SELECT sequence_name FROM information_schema.sequences
        WHERE sequence_schema = current_schema()
    LOOP
        EXECUTE format('DROP SEQUENCE IF EXISTS %I CASCADE', obj.sequence_name);
    END LOOP;
END $$;

\echo 'Осталось объектов (ожидается 0):'
SELECT count(*) AS remaining
FROM information_schema.tables
WHERE table_schema = current_schema();
