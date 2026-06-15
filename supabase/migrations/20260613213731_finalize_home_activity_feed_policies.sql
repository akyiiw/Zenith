drop policy if exists feed_entries_select_friend_activity on public.feed_entries;
drop policy if exists feed_entries_select_own_activity on public.feed_entries;
drop policy if exists feed_entries_select_visible on public.feed_entries;

create policy feed_entries_select_visible
    on public.feed_entries
    for select
    to authenticated
    using (
        (
            entry_type = 'activity'
            and activity_id is not null
            and (
                author_id = (select auth.uid())
                or private.are_accepted_friends((select auth.uid()), author_id)
            )
        )
        or (
            entry_type = 'post'
            and post_id is not null
            and (
                author_id = (select auth.uid())
                or private.are_accepted_friends((select auth.uid()), author_id)
            )
        )
    );

drop policy if exists "Usuario le atividades de amigos" on public.atividades;
drop policy if exists "Usuario le proprias atividades" on public.atividades;
drop policy if exists atividades_select_published_friend_activity on public.atividades;
drop policy if exists atividades_select_visible on public.atividades;

create policy atividades_select_visible
    on public.atividades
    for select
    to authenticated
    using (
        user_id = (select auth.uid())
        or exists (
            select 1
            from public.profiles p
            where p.id = atividades.user_id
              and p.visibilidade_perfil = 'publico'
        )
        or exists (
            select 1
            from public.feed_entries e
            where e.entry_type = 'activity'
              and e.activity_id = atividades.id
              and private.are_accepted_friends((select auth.uid()), e.author_id)
        )
    );
