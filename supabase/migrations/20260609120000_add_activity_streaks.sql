alter table public.profiles
    add column if not exists last_streak_activity_date date;

create or replace function public.record_activity_streak(p_activity_date date)
returns integer
language plpgsql
set search_path = public
as $$
declare
    current_user_id uuid := auth.uid();
    previous_date date;
    next_streak integer;
begin
    if current_user_id is null then
        raise exception 'Usuário não autenticado';
    end if;

    if p_activity_date is null then
        raise exception 'Data da atividade inválida';
    end if;

    select last_streak_activity_date
    into previous_date
    from public.profiles
    where id = current_user_id
    for update;

    if previous_date is not null and p_activity_date <= previous_date then
        select streak
        into next_streak
        from public.profiles
        where id = current_user_id;

        return coalesce(next_streak, 0);
    end if;

    update public.profiles
    set streak = case
            when previous_date is not null and p_activity_date = previous_date + 1 then coalesce(streak, 0) + 1
            else 1
        end,
        last_streak_activity_date = p_activity_date
    where id = current_user_id
    returning streak into next_streak;

    return coalesce(next_streak, 0);
end;
$$;

grant execute on function public.record_activity_streak(date) to authenticated;
