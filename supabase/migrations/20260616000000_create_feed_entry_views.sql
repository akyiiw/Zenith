create table if not exists public.feed_entry_views (
    id uuid primary key default gen_random_uuid(),
    user_id uuid not null references public.profiles(id) on delete cascade,
    feed_entry_id uuid not null references public.feed_entries(id) on delete cascade,
    viewed_at timestamptz not null default now(),
    constraint feed_entry_views_user_entry_unique unique (user_id, feed_entry_id)
);

create index if not exists feed_entry_views_user_id_idx
    on public.feed_entry_views(user_id);

grant select, insert, delete on public.feed_entry_views to authenticated;

alter table public.feed_entry_views enable row level security;

drop policy if exists feed_entry_views_select_own on public.feed_entry_views;
create policy feed_entry_views_select_own
    on public.feed_entry_views
    for select
    to authenticated
    using (user_id = (select auth.uid()));

drop policy if exists feed_entry_views_insert_own on public.feed_entry_views;
create policy feed_entry_views_insert_own
    on public.feed_entry_views
    for insert
    to authenticated
    with check (user_id = (select auth.uid()));

drop policy if exists feed_entry_views_delete_own on public.feed_entry_views;
create policy feed_entry_views_delete_own
    on public.feed_entry_views
    for delete
    to authenticated
    using (user_id = (select auth.uid()));
