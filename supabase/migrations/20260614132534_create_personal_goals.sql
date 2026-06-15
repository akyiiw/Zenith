create table if not exists public.personal_goals (
    id uuid primary key default gen_random_uuid(),
    user_id uuid not null references auth.users(id) on delete cascade,
    title text not null,
    metric text not null,
    period text not null,
    target_value numeric not null,
    is_active boolean default true,
    created_at timestamptz default now(),
    updated_at timestamptz default now()
);

do $$
begin
    if not exists (
        select 1
        from pg_constraint
        where conname = 'personal_goals_metric_check'
    ) then
        alter table public.personal_goals
            add constraint personal_goals_metric_check
            check (metric in ('distance_km', 'steps', 'active_minutes', 'activities', 'sleep_hours'));
    end if;

    if not exists (
        select 1
        from pg_constraint
        where conname = 'personal_goals_period_check'
    ) then
        alter table public.personal_goals
            add constraint personal_goals_period_check
            check (period in ('weekly', 'monthly'));
    end if;

    if not exists (
        select 1
        from pg_constraint
        where conname = 'personal_goals_target_value_check'
    ) then
        alter table public.personal_goals
            add constraint personal_goals_target_value_check
            check (target_value > 0);
    end if;
end $$;

create index if not exists personal_goals_user_id_idx
    on public.personal_goals(user_id);

create index if not exists personal_goals_user_active_period_idx
    on public.personal_goals(user_id, is_active, period);

alter table public.personal_goals enable row level security;

drop policy if exists "Users can manage their own goals" on public.personal_goals;

create policy "Users can manage their own goals"
    on public.personal_goals
    for all
    to authenticated
    using ((select auth.uid()) = user_id)
    with check ((select auth.uid()) = user_id);

grant select, insert, update, delete on public.personal_goals to authenticated;
