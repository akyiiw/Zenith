grant select on public.medalhas to authenticated;
grant select on public.usuario_medalhas to authenticated;
grant select, insert, update, delete on public.bloqueio_aplicativos to authenticated;

alter table public.medalhas enable row level security;
alter table public.usuario_medalhas enable row level security;
alter table public.bloqueio_aplicativos enable row level security;

drop policy if exists "Medalhas sao visiveis para usuarios autenticados" on public.medalhas;
create policy "Medalhas sao visiveis para usuarios autenticados"
on public.medalhas
for select
to authenticated
using (true);

drop policy if exists "Usuario le medalhas proprias" on public.usuario_medalhas;
drop policy if exists "Medalhas de usuarios sao visiveis para autenticados" on public.usuario_medalhas;
create policy "Medalhas de usuarios sao visiveis para autenticados"
on public.usuario_medalhas
for select
to authenticated
using (true);

drop policy if exists "Usuario le proprios bloqueios" on public.bloqueio_aplicativos;
create policy "Usuario le proprios bloqueios"
on public.bloqueio_aplicativos
for select
to authenticated
using (id_usuario = (select auth.uid()));

drop policy if exists "Usuario cria proprios bloqueios" on public.bloqueio_aplicativos;
create policy "Usuario cria proprios bloqueios"
on public.bloqueio_aplicativos
for insert
to authenticated
with check (id_usuario = (select auth.uid()));

drop policy if exists "Usuario atualiza proprios bloqueios" on public.bloqueio_aplicativos;
create policy "Usuario atualiza proprios bloqueios"
on public.bloqueio_aplicativos
for update
to authenticated
using (id_usuario = (select auth.uid()))
with check (id_usuario = (select auth.uid()));

drop policy if exists "Usuario remove proprios bloqueios" on public.bloqueio_aplicativos;
create policy "Usuario remove proprios bloqueios"
on public.bloqueio_aplicativos
for delete
to authenticated
using (id_usuario = (select auth.uid()));
