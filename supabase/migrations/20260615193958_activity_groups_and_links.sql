create table if not exists public.activity_groups (
    id uuid primary key default gen_random_uuid(),
    user_id uuid not null references public.profiles(id) on delete cascade,
    name text not null,
    description text,
    created_at timestamptz not null default now(),
    constraint activity_groups_name_not_blank check (length(trim(name)) > 0)
);

create table if not exists public.activity_group_items (
    group_id uuid not null references public.activity_groups(id) on delete cascade,
    activity_id uuid not null references public.atividades(id) on delete cascade,
    created_at timestamptz not null default now(),
    primary key (group_id, activity_id)
);

create table if not exists public.activity_links (
    source_activity_id uuid not null references public.atividades(id) on delete cascade,
    target_activity_id uuid not null references public.atividades(id) on delete cascade,
    link_type text not null,
    created_at timestamptz not null default now(),
    primary key (source_activity_id, target_activity_id, link_type),
    constraint activity_links_distinct_activities check (source_activity_id <> target_activity_id),
    constraint activity_links_type_check check (link_type in ('participation', 'related'))
);

create index if not exists activity_groups_user_id_idx
    on public.activity_groups(user_id);

create index if not exists activity_group_items_activity_id_idx
    on public.activity_group_items(activity_id);

create index if not exists activity_links_target_activity_id_idx
    on public.activity_links(target_activity_id);

grant select, insert, update, delete on public.activity_groups to authenticated;
grant select, insert, delete on public.activity_group_items to authenticated;
grant select, insert, delete on public.activity_links to authenticated;

alter table public.activity_groups enable row level security;
alter table public.activity_group_items enable row level security;
alter table public.activity_links enable row level security;

drop policy if exists activity_groups_select_visible on public.activity_groups;
create policy activity_groups_select_visible
    on public.activity_groups
    for select
    to authenticated
    using (
        user_id = (select auth.uid())
        or exists (
            select 1
            from public.profiles p
            where p.id = activity_groups.user_id
              and (
                  p.visibilidade_perfil = 'publico'
                  or private.are_accepted_friends((select auth.uid()), activity_groups.user_id)
              )
        )
        or exists (
            select 1
            from public.activity_group_items i
            join public.atividades a on a.id = i.activity_id
            where i.group_id = activity_groups.id
              and (
                  a.user_id = (select auth.uid())
                  or exists (
                      select 1
                      from public.feed_entries e
                      where e.entry_type = 'activity'
                        and e.activity_id = a.id
                        and private.are_accepted_friends((select auth.uid()), e.author_id)
                  )
                  or exists (
                      select 1
                      from public.activity_mentions m
                      where m.activity_id = a.id
                        and m.status = 'accepted'
                        and m.show_on_mentioned_profile
                        and m.mentioned_user_id = (select auth.uid())
                  )
              )
        )
    );

drop policy if exists activity_groups_insert_owner on public.activity_groups;
create policy activity_groups_insert_owner
    on public.activity_groups
    for insert
    to authenticated
    with check (user_id = (select auth.uid()));

drop policy if exists activity_groups_update_owner on public.activity_groups;
create policy activity_groups_update_owner
    on public.activity_groups
    for update
    to authenticated
    using (user_id = (select auth.uid()))
    with check (user_id = (select auth.uid()));

drop policy if exists activity_groups_delete_owner on public.activity_groups;
create policy activity_groups_delete_owner
    on public.activity_groups
    for delete
    to authenticated
    using (user_id = (select auth.uid()));

drop policy if exists activity_group_items_select_visible on public.activity_group_items;
create policy activity_group_items_select_visible
    on public.activity_group_items
    for select
    to authenticated
    using (
        exists (
            select 1
            from public.activity_groups g
            where g.id = activity_group_items.group_id
              and (
                  g.user_id = (select auth.uid())
                  or exists (
                      select 1
                      from public.profiles p
                      where p.id = g.user_id
                        and (
                            p.visibilidade_perfil = 'publico'
                            or private.are_accepted_friends((select auth.uid()), g.user_id)
                        )
                  )
              )
        )
    );

drop policy if exists activity_group_items_insert_owner on public.activity_group_items;
create policy activity_group_items_insert_owner
    on public.activity_group_items
    for insert
    to authenticated
    with check (
        exists (
            select 1
            from public.activity_groups g
            join public.atividades a on a.id = activity_group_items.activity_id
            where g.id = activity_group_items.group_id
              and g.user_id = (select auth.uid())
              and a.user_id = (select auth.uid())
        )
    );

drop policy if exists activity_group_items_delete_owner on public.activity_group_items;
create policy activity_group_items_delete_owner
    on public.activity_group_items
    for delete
    to authenticated
    using (
        exists (
            select 1
            from public.activity_groups g
            where g.id = activity_group_items.group_id
              and g.user_id = (select auth.uid())
        )
    );

drop policy if exists activity_links_select_visible on public.activity_links;
create policy activity_links_select_visible
    on public.activity_links
    for select
    to authenticated
    using (
        exists (
            select 1
            from public.atividades a
            where a.id in (activity_links.source_activity_id, activity_links.target_activity_id)
              and a.user_id = (select auth.uid())
        )
        or exists (
            select 1
            from public.activity_mentions m
            where m.activity_id in (activity_links.source_activity_id, activity_links.target_activity_id)
              and m.mentioned_user_id = (select auth.uid())
              and m.status = 'accepted'
        )
    );

drop policy if exists activity_links_insert_owner_or_participant on public.activity_links;
create policy activity_links_insert_owner_or_participant
    on public.activity_links
    for insert
    to authenticated
    with check (
        exists (
            select 1
            from public.atividades source_activity
            where source_activity.id = activity_links.source_activity_id
              and source_activity.user_id = (select auth.uid())
        )
        or exists (
            select 1
            from public.activity_mentions m
            where m.activity_id = activity_links.source_activity_id
              and m.mentioned_user_id = (select auth.uid())
              and m.status = 'accepted'
        )
    );

drop policy if exists activity_links_delete_owner on public.activity_links;
create policy activity_links_delete_owner
    on public.activity_links
    for delete
    to authenticated
    using (
        exists (
            select 1
            from public.atividades source_activity
            where source_activity.id = activity_links.source_activity_id
              and source_activity.user_id = (select auth.uid())
        )
    );
