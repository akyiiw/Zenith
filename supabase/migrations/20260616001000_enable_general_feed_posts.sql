grant select, insert on public.posts to authenticated;
grant select, insert on public.post_media to authenticated;

drop policy if exists posts_select_general_feed_visible on public.posts;
create policy posts_select_general_feed_visible
    on public.posts
    for select
    to authenticated
    using (
        desafio_id is null
        and (
            author_id = (select auth.uid())
            or private.are_accepted_friends((select auth.uid()), author_id)
        )
    );

drop policy if exists post_media_select_general_feed_visible on public.post_media;
create policy post_media_select_general_feed_visible
    on public.post_media
    for select
    to authenticated
    using (
        exists (
            select 1
            from public.posts p
            where p.id = post_media.post_id
              and p.desafio_id is null
              and (
                  p.author_id = (select auth.uid())
                  or private.are_accepted_friends((select auth.uid()), p.author_id)
              )
        )
    );

drop policy if exists post_media_insert_own_post on public.post_media;
create policy post_media_insert_own_post
    on public.post_media
    for insert
    to authenticated
    with check (
        author_id = (select auth.uid())
        and exists (
            select 1
            from public.posts p
            where p.id = post_media.post_id
              and p.author_id = (select auth.uid())
        )
    );
