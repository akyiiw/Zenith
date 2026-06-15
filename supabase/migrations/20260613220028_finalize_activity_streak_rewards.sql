create schema if not exists private;

alter table public.profiles
    add column if not exists last_streak_activity_date date;

create unique index if not exists conquistas_user_slug_unique_idx
    on public.conquistas(user_id, slug)
    where user_id is not null;

create unique index if not exists usuario_titulos_user_titulo_unique_idx
    on public.usuario_titulos(user_id, titulo_id)
    where user_id is not null and titulo_id is not null;

create index if not exists atividades_user_realizada_em_idx
    on public.atividades(user_id, realizada_em desc);

create or replace function private.unlock_activity_rewards(
    p_user_id uuid,
    p_streak integer,
    p_activity_count integer
)
returns void
language plpgsql
security definer
set search_path = public, pg_temp
as $$
declare
    dedicated_title_id uuid;
    legend_title_id uuid;
    athlete_title_id uuid;
begin
    if p_user_id is null or p_user_id <> auth.uid() then
        raise exception 'Usuario nao autorizado';
    end if;

    if coalesce(p_activity_count, 0) >= 1 then
        insert into public.conquistas(user_id, slug)
        values (p_user_id, 'primeira_atividade')
        on conflict do nothing;
    end if;

    if coalesce(p_streak, 0) >= 7 then
        insert into public.conquistas(user_id, slug)
        values (p_user_id, 'streak_7')
        on conflict do nothing;

        select id into dedicated_title_id
        from public.titulos
        where slug = 'dedicado'
        limit 1;

        if dedicated_title_id is not null then
            insert into public.usuario_titulos(user_id, titulo_id)
            values (p_user_id, dedicated_title_id)
            on conflict do nothing;
        end if;
    end if;

    if coalesce(p_streak, 0) >= 30 then
        insert into public.conquistas(user_id, slug)
        values (p_user_id, 'streak_30')
        on conflict do nothing;

        select id into legend_title_id
        from public.titulos
        where slug = 'lenda'
        limit 1;

        if legend_title_id is not null then
            insert into public.usuario_titulos(user_id, titulo_id)
            values (p_user_id, legend_title_id)
            on conflict do nothing;
        end if;
    end if;

    if coalesce(p_activity_count, 0) >= 10 then
        insert into public.conquistas(user_id, slug)
        values (p_user_id, 'atividades_10')
        on conflict do nothing;

        select id into athlete_title_id
        from public.titulos
        where slug = 'atleta'
        limit 1;

        if athlete_title_id is not null then
            insert into public.usuario_titulos(user_id, titulo_id)
            values (p_user_id, athlete_title_id)
            on conflict do nothing;
        end if;
    end if;
end;
$$;

revoke all on function private.unlock_activity_rewards(uuid, integer, integer) from public;
grant execute on function private.unlock_activity_rewards(uuid, integer, integer) to authenticated;

create or replace function public.record_activity_streak(p_activity_date date)
returns integer
language plpgsql
set search_path = public, pg_temp
as $$
declare
    current_user_id uuid := auth.uid();
    previous_date date;
    next_streak integer;
    activity_count integer;
begin
    if current_user_id is null then
        raise exception 'Usuario nao autenticado';
    end if;

    if p_activity_date is null then
        raise exception 'Data da atividade invalida';
    end if;

    select last_streak_activity_date
    into previous_date
    from public.profiles
    where id = current_user_id
    for update;

    if previous_date is not null and p_activity_date <= previous_date then
        select coalesce(streak, 0)
        into next_streak
        from public.profiles
        where id = current_user_id;
    else
        update public.profiles
        set streak = case
                when previous_date is not null and p_activity_date = previous_date + 1 then coalesce(streak, 0) + 1
                else 1
            end,
            last_streak_activity_date = p_activity_date
        where id = current_user_id
        returning coalesce(streak, 0) into next_streak;
    end if;

    select count(*)
    into activity_count
    from public.atividades
    where user_id = current_user_id;

    perform private.unlock_activity_rewards(current_user_id, coalesce(next_streak, 0), coalesce(activity_count, 0));

    return coalesce(next_streak, 0);
end;
$$;

grant execute on function public.record_activity_streak(date) to authenticated;
