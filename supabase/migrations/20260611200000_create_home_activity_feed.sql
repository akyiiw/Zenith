alter table public.profiles
    add column if not exists visibilidade_perfil text not null default 'publico';

do $$
begin
    if not exists (select 1 from pg_constraint where conname = 'profiles_visibilidade_perfil_check') then
        alter table public.profiles add constraint profiles_visibilidade_perfil_check
            check (visibilidade_perfil in ('publico', 'privado'));
    end if;
end $$;

create index if not exists feed_entries_activity_created_at_idx
    on public.feed_entries (activity_id, created_at desc)
    where entry_type = 'activity' and activity_id is not null;

drop policy if exists feed_entries_insert_own_activity on public.feed_entries;
create policy feed_entries_insert_own_activity
    on public.feed_entries
    for insert
    to authenticated
    with check (
        author_id = (select auth.uid())
        and entry_type = 'activity'
        and activity_id is not null
        and exists (
            select 1
            from public.atividades a
            where a.id = feed_entries.activity_id
              and a.user_id = (select auth.uid())
        )
    );

do $$
begin
    if not exists (select 1 from pg_policies where schemaname = 'public' and tablename = 'feed_entries' and policyname = 'feed_entries_select_friend_activity') then
        create policy feed_entries_select_friend_activity
            on public.feed_entries
            for select
            to authenticated
            using (
                entry_type = 'activity'
                and activity_id is not null
                and exists (
                    select 1
                    from public.amizades am
                    where am.status = 'aceito'
                      and (
                          (am.user_id = (select auth.uid()) and am.amigo_id = feed_entries.author_id)
                          or (am.amigo_id = (select auth.uid()) and am.user_id = feed_entries.author_id)
                      )
                )
            );
    end if;
end $$;
