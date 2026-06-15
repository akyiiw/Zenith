create table if not exists public.activity_mentions (
    id uuid primary key default gen_random_uuid(),
    activity_id uuid not null references public.atividades(id) on delete cascade,
    publisher_id uuid not null references public.profiles(id) on delete cascade,
    mentioned_user_id uuid not null references public.profiles(id) on delete cascade,
    status text not null default 'pending',
    show_on_mentioned_profile boolean not null default true,
    created_at timestamptz not null default now(),
    responded_at timestamptz,
    constraint activity_mentions_unique_activity_user unique (activity_id, mentioned_user_id),
    constraint activity_mentions_distinct_users check (publisher_id <> mentioned_user_id),
    constraint activity_mentions_status_check check (status in ('pending', 'accepted', 'declined'))
);

create index if not exists activity_mentions_activity_id_idx
    on public.activity_mentions(activity_id);

create index if not exists activity_mentions_publisher_id_idx
    on public.activity_mentions(publisher_id);

create index if not exists activity_mentions_mentioned_user_id_idx
    on public.activity_mentions(mentioned_user_id);

grant select, insert, update on public.activity_mentions to authenticated;

alter table public.activity_mentions enable row level security;

drop policy if exists activity_mentions_select_participants on public.activity_mentions;
create policy activity_mentions_select_participants
    on public.activity_mentions
    for select
    to authenticated
    using (
        publisher_id = (select auth.uid())
        or mentioned_user_id = (select auth.uid())
        or (
            status = 'accepted'
            and show_on_mentioned_profile
            and exists (
                select 1
                from public.profiles p
                where p.id = activity_mentions.mentioned_user_id
                  and (
                      p.visibilidade_perfil = 'publico'
                      or private.are_accepted_friends((select auth.uid()), activity_mentions.mentioned_user_id)
                  )
            )
        )
    );

drop policy if exists activity_mentions_insert_friend_mentions on public.activity_mentions;
create policy activity_mentions_insert_friend_mentions
    on public.activity_mentions
    for insert
    to authenticated
    with check (
        publisher_id = (select auth.uid())
        and exists (
            select 1
            from public.atividades a
            where a.id = activity_id
              and a.user_id = (select auth.uid())
        )
        and private.are_accepted_friends((select auth.uid()), mentioned_user_id)
    );

drop policy if exists activity_mentions_update_mentioned_user on public.activity_mentions;
create policy activity_mentions_update_mentioned_user
    on public.activity_mentions
    for update
    to authenticated
    using (mentioned_user_id = (select auth.uid()))
    with check (mentioned_user_id = (select auth.uid()));

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
        or exists (
            select 1
            from public.activity_mentions m
            join public.profiles mentioned_profile on mentioned_profile.id = m.mentioned_user_id
            where m.activity_id = atividades.id
              and m.status = 'accepted'
              and m.show_on_mentioned_profile
              and (
                  m.mentioned_user_id = (select auth.uid())
                  or mentioned_profile.visibilidade_perfil = 'publico'
                  or private.are_accepted_friends((select auth.uid()), m.mentioned_user_id)
              )
        )
    );
