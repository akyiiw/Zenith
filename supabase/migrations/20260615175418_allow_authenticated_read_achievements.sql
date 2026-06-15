drop policy if exists "Usuario le proprias conquistas" on public.conquistas;
drop policy if exists conquistas_select_authenticated on public.conquistas;

create policy conquistas_select_authenticated
    on public.conquistas
    for select
    to authenticated
    using (true);
