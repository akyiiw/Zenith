-- Revisao humana necessaria: execute primeiro em staging.
-- Estes objetos aparecem nas migrations locais, mas nao foram encontrados no app Android atual.
-- Remova apenas se confirmar que nenhum outro cliente, dashboard, job ou Edge Function usa os objetos.

-- A migration baseline preserva este indice porque o forum de desafio usa posts.desafio_id.
-- Mantido aqui como exemplo de rollback caso a feature de forum seja removida por produto.
-- drop index if exists public.posts_desafio_id_created_at_idx;

-- Objetos candidatos a revisao por baixo sinal no cliente Android:
-- views: nenhuma view local identificada.
-- triggers: nenhum trigger local identificado.
-- functions: nenhuma function local identificada.

-- Consultas para auditoria no Supabase antes de remover objetos:
select schemaname, tablename, policyname
from pg_policies
where schemaname in ('public', 'storage')
order by schemaname, tablename, policyname;

select table_schema, table_name, column_name
from information_schema.columns
where table_schema = 'public'
order by table_name, ordinal_position;

select schemaname, tablename, indexname
from pg_indexes
where schemaname = 'public'
order by tablename, indexname;

select trigger_schema, event_object_table, trigger_name
from information_schema.triggers
where trigger_schema = 'public'
order by event_object_table, trigger_name;

select routine_schema, routine_name, routine_type
from information_schema.routines
where routine_schema = 'public'
order by routine_name;
