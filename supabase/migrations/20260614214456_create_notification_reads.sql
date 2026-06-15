create table if not exists public.notification_reads (
    id uuid primary key default gen_random_uuid(),
    user_id uuid not null references public.profiles(id) on delete cascade,
    notification_id text not null,
    read_at timestamptz not null default now(),
    constraint notification_reads_user_notification_unique unique (user_id, notification_id)
);

create index if not exists notification_reads_user_id_idx
    on public.notification_reads(user_id);

grant select, insert, delete on public.notification_reads to authenticated;

alter table public.notification_reads enable row level security;

drop policy if exists notification_reads_select_own on public.notification_reads;
create policy notification_reads_select_own
    on public.notification_reads
    for select
    to authenticated
    using (user_id = (select auth.uid()));

drop policy if exists notification_reads_insert_own on public.notification_reads;
create policy notification_reads_insert_own
    on public.notification_reads
    for insert
    to authenticated
    with check (user_id = (select auth.uid()));

drop policy if exists notification_reads_delete_own on public.notification_reads;
create policy notification_reads_delete_own
    on public.notification_reads
    for delete
    to authenticated
    using (user_id = (select auth.uid()));
