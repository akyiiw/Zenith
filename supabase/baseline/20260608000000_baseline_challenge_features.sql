alter table public.desafios
    add column if not exists banner_hash text,
    add column if not exists atividade_designada text not null default 'caminhada',
    add column if not exists apenas_premium boolean not null default false,
    add column if not exists modo_meta text not null default 'fixa',
    add column if not exists metrica text not null default 'distancia',
    add column if not exists visibilidade text not null default 'publico',
    add column if not exists max_participantes integer,
    add column if not exists aceita_registro_manual boolean not null default false,
    add column if not exists ranking_tipo text not null default 'menor_tempo',
    add column if not exists objetivo_metrica text,
    add column if not exists objetivo_valor double precision;

alter table public.atividades
    add column if not exists passos integer,
    add column if not exists distancia_bruta double precision,
    add column if not exists gps_accuracy_media double precision,
    add column if not exists gps_pontos_aceitos integer,
    add column if not exists gps_pontos_rejeitados integer,
    add column if not exists gps_qualidade text;

alter table public.posts
    add column if not exists desafio_id uuid references public.desafios(id) on delete cascade;

create index if not exists posts_desafio_id_created_at_idx
    on public.posts (desafio_id, created_at desc);

do $$
begin
    if not exists (select 1 from pg_constraint where conname = 'desafios_atividade_designada_check') then
        alter table public.desafios add constraint desafios_atividade_designada_check
            check (atividade_designada in ('caminhada', 'corrida', 'ciclismo'));
    end if;

    if not exists (select 1 from pg_constraint where conname = 'desafios_modo_meta_check') then
        alter table public.desafios add constraint desafios_modo_meta_check
            check (modo_meta in ('livre', 'fixa'));
    end if;

    if not exists (select 1 from pg_constraint where conname = 'desafios_metrica_check') then
        alter table public.desafios add constraint desafios_metrica_check
            check (metrica in ('distancia', 'passos', 'tempo'));
    end if;

    if not exists (select 1 from pg_constraint where conname = 'desafios_visibilidade_check') then
        alter table public.desafios add constraint desafios_visibilidade_check
            check (visibilidade in ('publico', 'amigos', 'convite'));
    end if;

    if not exists (select 1 from pg_constraint where conname = 'desafios_max_participantes_check') then
        alter table public.desafios add constraint desafios_max_participantes_check
            check (max_participantes is null or max_participantes > 0);
    end if;

    if not exists (select 1 from pg_constraint where conname = 'desafios_ranking_tipo_check') then
        alter table public.desafios add constraint desafios_ranking_tipo_check
            check (ranking_tipo in ('menor_tempo', 'maior_distancia', 'menor_pace', 'tempo_total', 'distancia_total'));
    end if;

    if not exists (select 1 from pg_constraint where conname = 'desafios_objetivo_metrica_check') then
        alter table public.desafios add constraint desafios_objetivo_metrica_check
            check (objetivo_metrica is null or objetivo_metrica in ('distancia', 'tempo'));
    end if;

    if not exists (select 1 from pg_constraint where conname = 'desafios_objetivo_valor_check') then
        alter table public.desafios add constraint desafios_objetivo_valor_check
            check (objetivo_valor is null or objetivo_valor > 0);
    end if;

    if not exists (select 1 from pg_constraint where conname = 'atividades_passos_check') then
        alter table public.atividades add constraint atividades_passos_check
            check (passos is null or passos >= 0);
    end if;

    if not exists (select 1 from pg_constraint where conname = 'atividades_gps_qualidade_check') then
        alter table public.atividades add constraint atividades_gps_qualidade_check
            check (gps_qualidade is null or gps_qualidade in ('boa', 'media', 'ruim'));
    end if;

    if not exists (select 1 from pg_constraint where conname = 'atividades_gps_pontos_check') then
        alter table public.atividades add constraint atividades_gps_pontos_check
            check (
                (gps_pontos_aceitos is null or gps_pontos_aceitos >= 0)
                and (gps_pontos_rejeitados is null or gps_pontos_rejeitados >= 0)
            );
    end if;
end $$;

insert into storage.buckets (id, name, public, file_size_limit, allowed_mime_types)
values
    ('challenge-banners', 'challenge-banners', true, 5242880, array['image/jpeg', 'image/png', 'image/webp']::text[]),
    ('post-media', 'post-media', true, 10485760, array['image/jpeg', 'image/png', 'image/webp']::text[])
on conflict (id) do update
set public = excluded.public,
    file_size_limit = excluded.file_size_limit,
    allowed_mime_types = excluded.allowed_mime_types;

drop policy if exists posts_insert_own on public.posts;
create policy posts_insert_own
    on public.posts
    for insert
    to authenticated
    with check (
        author_id = (select auth.uid())
        and (
            desafio_id is null
            or exists (
                select 1
                from public.desafio_participacoes dp
                where dp.desafio_id = posts.desafio_id
                  and dp.user_id = (select auth.uid())
            )
        )
    );

drop policy if exists feed_comments_insert_own on public.feed_comments;
create policy feed_comments_insert_own
    on public.feed_comments
    for insert
    to authenticated
    with check (
        author_id = (select auth.uid())
        and exists (
            select 1
            from public.feed_entries e
            left join public.posts p on p.id = e.post_id
            where e.id = feed_comments.entry_id
              and (
                  p.desafio_id is null
                  or exists (
                      select 1
                      from public.desafio_participacoes dp
                      where dp.desafio_id = p.desafio_id
                        and dp.user_id = (select auth.uid())
                  )
              )
        )
    );

do $$
begin
    if not exists (select 1 from pg_policies where schemaname = 'public' and tablename = 'posts' and policyname = 'posts_select_challenge_visible') then
        create policy posts_select_challenge_visible
            on public.posts
            for select
            to authenticated
            using (desafio_id is not null);
    end if;

    if not exists (select 1 from pg_policies where schemaname = 'public' and tablename = 'post_media' and policyname = 'post_media_select_challenge_visible') then
        create policy post_media_select_challenge_visible
            on public.post_media
            for select
            to authenticated
            using (
                exists (
                    select 1
                    from public.posts p
                    where p.id = post_media.post_id
                      and p.desafio_id is not null
                )
            );
    end if;

    if not exists (select 1 from pg_policies where schemaname = 'public' and tablename = 'feed_entries' and policyname = 'feed_entries_select_challenge_visible') then
        create policy feed_entries_select_challenge_visible
            on public.feed_entries
            for select
            to authenticated
            using (
                exists (
                    select 1
                    from public.posts p
                    where p.id = feed_entries.post_id
                      and p.desafio_id is not null
                )
            );
    end if;

    if not exists (select 1 from pg_policies where schemaname = 'public' and tablename = 'feed_comments' and policyname = 'feed_comments_select_challenge_visible') then
        create policy feed_comments_select_challenge_visible
            on public.feed_comments
            for select
            to authenticated
            using (
                exists (
                    select 1
                    from public.feed_entries e
                    join public.posts p on p.id = e.post_id
                    where e.id = feed_comments.entry_id
                      and p.desafio_id is not null
                )
            );
    end if;

    if not exists (select 1 from pg_policies where schemaname = 'public' and tablename = 'feed_entries' and policyname = 'feed_entries_insert_own_post') then
        create policy feed_entries_insert_own_post
            on public.feed_entries
            for insert
            to authenticated
            with check (
                author_id = (select auth.uid())
                and entry_type = 'post'
                and post_id is not null
                and exists (
                    select 1
                    from public.posts p
                    where p.id = feed_entries.post_id
                      and p.author_id = (select auth.uid())
                      and (
                          p.desafio_id is null
                          or exists (
                              select 1
                              from public.desafio_participacoes dp
                              where dp.desafio_id = p.desafio_id
                                and dp.user_id = (select auth.uid())
                          )
                      )
                )
            );
    end if;

    if not exists (select 1 from pg_policies where schemaname = 'storage' and tablename = 'objects' and policyname = 'Challenge banners are publicly readable') then
        create policy "Challenge banners are publicly readable"
            on storage.objects
            for select
            to public
            using (bucket_id = 'challenge-banners');
    end if;

    if not exists (select 1 from pg_policies where schemaname = 'storage' and tablename = 'objects' and policyname = 'Users can upload their challenge banners') then
        create policy "Users can upload their challenge banners"
            on storage.objects
            for insert
            to authenticated
            with check (
                bucket_id = 'challenge-banners'
                and (storage.foldername(name))[1] = auth.uid()::text
            );
    end if;

    if not exists (select 1 from pg_policies where schemaname = 'storage' and tablename = 'objects' and policyname = 'Users can update their challenge banners') then
        create policy "Users can update their challenge banners"
            on storage.objects
            for update
            to authenticated
            using (
                bucket_id = 'challenge-banners'
                and (storage.foldername(name))[1] = auth.uid()::text
            )
            with check (
                bucket_id = 'challenge-banners'
                and (storage.foldername(name))[1] = auth.uid()::text
            );
    end if;

    if not exists (select 1 from pg_policies where schemaname = 'storage' and tablename = 'objects' and policyname = 'Post media is publicly readable') then
        create policy "Post media is publicly readable"
            on storage.objects
            for select
            to public
            using (bucket_id = 'post-media');
    end if;

    if not exists (select 1 from pg_policies where schemaname = 'storage' and tablename = 'objects' and policyname = 'Users can upload their post media') then
        create policy "Users can upload their post media"
            on storage.objects
            for insert
            to authenticated
            with check (
                bucket_id = 'post-media'
                and (storage.foldername(name))[1] = auth.uid()::text
            );
    end if;
end $$;
