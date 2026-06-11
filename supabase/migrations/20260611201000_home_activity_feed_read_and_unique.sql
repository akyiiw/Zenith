create unique index if not exists feed_entries_activity_unique_idx
    on public.feed_entries (activity_id)
    where entry_type = 'activity' and activity_id is not null;

do $$
begin
    if not exists (select 1 from pg_policies where schemaname = 'public' and tablename = 'feed_entries' and policyname = 'feed_entries_select_own_activity') then
        create policy feed_entries_select_own_activity
            on public.feed_entries
            for select
            to authenticated
            using (
                entry_type = 'activity'
                and activity_id is not null
                and author_id = (select auth.uid())
            );
    end if;

    if not exists (select 1 from pg_policies where schemaname = 'public' and tablename = 'atividades' and policyname = 'atividades_select_published_friend_activity') then
        create policy atividades_select_published_friend_activity
            on public.atividades
            for select
            to authenticated
            using (
                exists (
                    select 1
                    from public.feed_entries e
                    join public.amizades am
                      on am.status = 'aceito'
                     and (
                         (am.user_id = (select auth.uid()) and am.amigo_id = e.author_id)
                         or (am.amigo_id = (select auth.uid()) and am.user_id = e.author_id)
                     )
                    where e.entry_type = 'activity'
                      and e.activity_id = atividades.id
                )
            );
    end if;
end $$;
