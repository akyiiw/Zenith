create table if not exists public.sleep_records (
    id uuid primary key default gen_random_uuid(),
    user_id uuid not null references auth.users(id) on delete cascade,
    started_at timestamptz not null,
    ended_at timestamptz not null,
    duration_minutes integer not null,
    quality integer,
    source text not null default 'manual',
    created_at timestamptz not null default now(),
    constraint sleep_records_duration_check check (duration_minutes > 0),
    constraint sleep_records_quality_check check (quality is null or quality between 1 and 5),
    constraint sleep_records_source_check check (
        source in ('manual', 'health_connect', 'sleep_api', 'device_estimate')
    ),
    constraint sleep_records_period_check check (ended_at > started_at)
);

create index if not exists sleep_records_user_started_at_idx
    on public.sleep_records (user_id, started_at desc);

alter table public.sleep_records enable row level security;

grant select, insert, update, delete on public.sleep_records to authenticated;

drop policy if exists sleep_records_select_own on public.sleep_records;
create policy sleep_records_select_own
    on public.sleep_records
    for select
    to authenticated
    using (user_id = (select auth.uid()));

drop policy if exists sleep_records_insert_own on public.sleep_records;
create policy sleep_records_insert_own
    on public.sleep_records
    for insert
    to authenticated
    with check (user_id = (select auth.uid()));

drop policy if exists sleep_records_update_own on public.sleep_records;
create policy sleep_records_update_own
    on public.sleep_records
    for update
    to authenticated
    using (user_id = (select auth.uid()))
    with check (user_id = (select auth.uid()));

drop policy if exists sleep_records_delete_own on public.sleep_records;
create policy sleep_records_delete_own
    on public.sleep_records
    for delete
    to authenticated
    using (user_id = (select auth.uid()));
