grant select on public.profiles to authenticated;
grant update on public.profiles to authenticated;
grant select on public.atividades to authenticated;
grant select on public.titulos to authenticated;
grant select on public.conquistas to authenticated;
grant select on public.usuario_titulos to authenticated;

grant usage on schema private to authenticated;
grant execute on function private.unlock_activity_rewards(uuid, integer, integer) to authenticated;
grant execute on function public.record_activity_streak(date) to authenticated;
