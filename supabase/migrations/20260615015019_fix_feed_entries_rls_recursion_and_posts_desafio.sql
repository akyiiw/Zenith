alter table public.posts
    add column if not exists desafio_id uuid references public.desafios(id) on delete cascade;

create index if not exists posts_desafio_id_created_at_idx
    on public.posts (desafio_id, created_at desc);

create or replace function private.can_view_feed_activity(
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
        from public.feed_entries e
        where e.entry_type = 'activity'
          and e.activity_id = target_activity_id
          and private.are_accepted_friends(viewer_id, e.author_id)
    );
$$;

grant execute on function private.can_view_feed_activity(uuid, uuid) to authenticated;

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
        or private.can_view_feed_activity(atividades.id, (select auth.uid()))
        or private.can_view_activity_mentions_for_activity(atividades.id, (select auth.uid()))
    );

drop policy if exists posts_insert_own on public.posts;
create policy posts_insert_own
    on public.posts
    for insert
    to authenticated
    with check (
        author_id = (select auth.uid())
        and (
            desafio_id is null
            or exists (
                select 1
                from public.desafio_participacoes dp
                where dp.desafio_id = posts.desafio_id
                  and dp.user_id = (select auth.uid())
            )
        )
    );

drop policy if exists posts_select_challenge_visible on public.posts;
create policy posts_select_challenge_visible
    on public.posts
    for select
    to authenticated
    using (desafio_id is not null);
