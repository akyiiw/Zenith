create table if not exists public.user_badge_entitlements (
    user_id uuid not null references auth.users(id) on delete cascade,
    badge_id bigint not null references public.badge(id) on delete cascade,
    source text not null,
    created_at timestamptz not null default now(),
    primary key (user_id, badge_id),
    constraint user_badge_entitlements_source_check
        check (source in ('developer', 'premium', 'tester'))
);

create index if not exists user_badge_entitlements_badge_id_idx
    on public.user_badge_entitlements(badge_id);

alter table public.user_badge_entitlements enable row level security;

drop policy if exists user_badge_entitlements_select_own on public.user_badge_entitlements;

create policy user_badge_entitlements_select_own
    on public.user_badge_entitlements
    for select
    to authenticated
    using ((select auth.uid()) = user_id);

grant select on public.user_badge_entitlements to authenticated;

create or replace function private.sync_user_badge_entitlements(target_user_id uuid)
returns void
language plpgsql
security definer
set search_path = public, private
as $$
declare
    dev_badge_id bigint;
    premium_badge_id bigint;
    tester_badge_id bigint;
begin
    select id into dev_badge_id from public.badge where upper(title) = 'DEV' limit 1;
    select id into premium_badge_id from public.badge where upper(title) = 'PREMIUM' limit 1;
    select id into tester_badge_id from public.badge where upper(title) = 'TESTER' limit 1;

    delete from public.user_badge_entitlements
    where user_id = target_user_id
      and source in ('developer', 'premium', 'tester');

    if dev_badge_id is not null and exists (
        select 1 from private.app_developers where user_id = target_user_id
    ) then
        insert into public.user_badge_entitlements(user_id, badge_id, source)
        values (target_user_id, dev_badge_id, 'developer')
        on conflict (user_id, badge_id) do update set source = excluded.source;
    end if;

    if premium_badge_id is not null and exists (
        select 1 from public.profiles where id = target_user_id and "isPremium" = true
    ) then
        insert into public.user_badge_entitlements(user_id, badge_id, source)
        values (target_user_id, premium_badge_id, 'premium')
        on conflict (user_id, badge_id) do update set source = excluded.source;
    end if;

    if tester_badge_id is not null and exists (
        select 1 from public.premium_users where userid = target_user_id and "betaAccess" = true
    ) then
        insert into public.user_badge_entitlements(user_id, badge_id, source)
        values (target_user_id, tester_badge_id, 'tester')
        on conflict (user_id, badge_id) do update set source = excluded.source;
    end if;

end;
$$;

create or replace function private.clear_invalid_profile_badge(target_user_id uuid)
returns void
language plpgsql
security definer
set search_path = public, private
as $$
begin
    update public.profiles profile
    set badge_id = null
    where profile.id = target_user_id
      and profile.badge_id is not null
      and not exists (
          select 1
          from public.user_badge_entitlements entitlement
          where entitlement.user_id = target_user_id
            and entitlement.badge_id = profile.badge_id
      );
end;
$$;

create or replace function private.sync_profile_badge_entitlements_trigger()
returns trigger
language plpgsql
security definer
set search_path = public, private
as $$
begin
    perform private.sync_user_badge_entitlements(new.id);
    perform private.clear_invalid_profile_badge(new.id);
    return new;
end;
$$;

create or replace function private.sync_premium_badge_entitlements_trigger()
returns trigger
language plpgsql
security definer
set search_path = public, private
as $$
begin
    perform private.sync_user_badge_entitlements(coalesce(new.userid, old.userid));
    perform private.clear_invalid_profile_badge(coalesce(new.userid, old.userid));
    return coalesce(new, old);
end;
$$;

create or replace function private.sync_developer_badge_entitlements_trigger()
returns trigger
language plpgsql
security definer
set search_path = public, private
as $$
begin
    perform private.sync_user_badge_entitlements(coalesce(new.user_id, old.user_id));
    perform private.clear_invalid_profile_badge(coalesce(new.user_id, old.user_id));
    return coalesce(new, old);
end;
$$;

create or replace function private.validate_profile_badge_entitlement()
returns trigger
language plpgsql
security definer
set search_path = public, private
as $$
begin
    if new.badge_id is null then
        return new;
    end if;

    perform private.sync_user_badge_entitlements(new.id);

    if not exists (
        select 1
        from public.user_badge_entitlements entitlement
        where entitlement.user_id = new.id
          and entitlement.badge_id = new.badge_id
    ) then
        raise exception 'Badge indisponivel para este usuario';
    end if;

    return new;
end;
$$;

drop trigger if exists profiles_sync_badge_entitlements on public.profiles;
create trigger profiles_sync_badge_entitlements
    after insert or update of "isPremium" on public.profiles
    for each row
    execute function private.sync_profile_badge_entitlements_trigger();

drop trigger if exists premium_users_sync_badge_entitlements on public.premium_users;
create trigger premium_users_sync_badge_entitlements
    after insert or update of "betaAccess" or delete on public.premium_users
    for each row
    execute function private.sync_premium_badge_entitlements_trigger();

drop trigger if exists app_developers_sync_badge_entitlements on private.app_developers;
create trigger app_developers_sync_badge_entitlements
    after insert or delete on private.app_developers
    for each row
    execute function private.sync_developer_badge_entitlements_trigger();

drop trigger if exists profiles_validate_badge_entitlement on public.profiles;
create trigger profiles_validate_badge_entitlement
    before insert or update of badge_id on public.profiles
    for each row
    execute function private.validate_profile_badge_entitlement();

insert into public.user_badge_entitlements(user_id, badge_id, source)
select developer.user_id, badge.id, 'developer'
from private.app_developers developer
join public.badge badge on upper(badge.title) = 'DEV'
on conflict (user_id, badge_id) do update set source = excluded.source;

insert into public.user_badge_entitlements(user_id, badge_id, source)
select profile.id, badge.id, 'premium'
from public.profiles profile
join public.badge badge on upper(badge.title) = 'PREMIUM'
where profile."isPremium" = true
on conflict (user_id, badge_id) do update set source = excluded.source;

insert into public.user_badge_entitlements(user_id, badge_id, source)
select premium.userid, badge.id, 'tester'
from public.premium_users premium
join public.badge badge on upper(badge.title) = 'TESTER'
where premium."betaAccess" = true
on conflict (user_id, badge_id) do update set source = excluded.source;

update public.profiles profile
set badge_id = null
where badge_id is not null
  and not exists (
      select 1
      from public.user_badge_entitlements entitlement
      where entitlement.user_id = profile.id
        and entitlement.badge_id = profile.badge_id
  );
