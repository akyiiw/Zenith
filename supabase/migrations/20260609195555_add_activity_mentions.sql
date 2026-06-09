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
        or (status = 'accepted' and show_on_mentioned_profile)
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
        and exists (
            select 1
            from public.amizades f
            where f.status = 'aceito'
              and (
                (f.user_id = publisher_id and f.amigo_id = mentioned_user_id)
                or (f.user_id = mentioned_user_id and f.amigo_id = publisher_id)
              )
        )
    );

drop policy if exists activity_mentions_update_mentioned_user on public.activity_mentions;
create policy activity_mentions_update_mentioned_user
    on public.activity_mentions
    for update
    to authenticated
    using (mentioned_user_id = (select auth.uid()))
    with check (mentioned_user_id = (select auth.uid()));
