-- Manual reset for activity/progress data.
-- Run intentionally in SQL Editor/psql only when you want to clear progress data.

begin;

delete from public.feed_comments
where entry_id in (
    select id
    from public.feed_entries
    where activity_id is not null
);

delete from public.feed_entries
where activity_id is not null;

delete from public.desafio_participacoes;
delete from public.atividades;
delete from public.sleep_records;

update public.profiles
set streak = 0,
    last_streak_activity_date = null;

commit;
