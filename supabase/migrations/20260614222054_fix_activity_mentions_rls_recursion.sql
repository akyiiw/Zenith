create or replace function private.user_owns_activity(
    target_activity_id uuid,
    viewer_id uuid
)
returns boolean
language sql
stable
security definer
set search_path = public, private
as $$
    select exists (
        select 1
        from public.atividades a
        where a.id = target_activity_id
          and a.user_id = viewer_id
    );
$$;

create or replace function private.can_view_activity_mentions_for_activity(
    target_activity_id uuid,
    viewer_id uuid
)
returns boolean
language sql
stable
security definer
set search_path = public, private
as $$
    select exists (
        select 1
        from public.activity_mentions m
        join public.profiles mentioned_profile
          on mentioned_profile.id = m.mentioned_user_id
        where m.activity_id = target_activity_id
          and m.status = 'accepted'
          and m.show_on_mentioned_profile
          and (
              m.mentioned_user_id = viewer_id
              or mentioned_profile.visibilidade_perfil = 'publico'
              or private.are_accepted_friends(viewer_id, m.mentioned_user_id)
          )
    );
$$;

grant execute on function private.user_owns_activity(uuid, uuid) to authenticated;
grant execute on function private.can_view_activity_mentions_for_activity(uuid, uuid) to authenticated;

drop policy if exists activity_mentions_insert_friend_mentions on public.activity_mentions;
create policy activity_mentions_insert_friend_mentions
    on public.activity_mentions
    for insert
    to authenticated
    with check (
        publisher_id = (select auth.uid())
        and private.user_owns_activity(activity_id, (select auth.uid()))
        and private.are_accepted_friends((select auth.uid()), mentioned_user_id)
    );

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
        or private.can_view_activity_mentions_for_activity(atividades.id, (select auth.uid()))
    );
