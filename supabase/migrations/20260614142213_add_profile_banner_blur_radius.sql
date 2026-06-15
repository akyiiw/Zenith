alter table public.profiles
    add column if not exists banner_blur_radius smallint not null default 10;

alter table public.profiles
    drop constraint if exists profiles_banner_blur_radius_check;

alter table public.profiles
    add constraint profiles_banner_blur_radius_check
    check (banner_blur_radius between 0 and 24);
