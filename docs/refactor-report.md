# Relatorio de refatoracao

## Problemas encontrados

- `gradle/libs.versions.toml` tinha aliases duplicados para `androidx.compose.ui:ui` e `androidx.compose.ui:ui-text`.
- `supabase-realtime` era instalado no cliente, mas nenhum codigo Android usava canais, presence ou streaming realtime.
- `app/.idea` duplicava metadados de IDE dentro do modulo Android.
- Havia comentarios de TODO/debug e logs com dados de sessao/usuario em ViewModels.
- As 5 migrations locais eram incrementais e corrigiam/expandiam a mesma feature de desafios.
- Telas Compose grandes ainda existem (`ChallengeComponents`, `RegisterActivityScreen`, `ActivityDetailScreen`, `CreateChallengeScreen`). Elas compilam, mas ainda exigem refatoracao manual por fluxo para reduzir risco visual.

## Codigo morto removido

- Instalacao do plugin `Realtime` em `SupabaseConfig`.
- Logs permanentes de debug em `AuthViewModel` e `UserViewModel`.
- Comentarios TODO e comentarios obvios em telas/modelos.

## Dependencias removidas

- `libs.supabase.realtime`.
- `libs.androidx.compose.ui.text.google.fonts`.
- Aliases duplicados `libs.androidx.ui`, `libs.androidx.ui.text` e `libs.androidx.compose.ui.text`.

## Arquivos removidos

- `app/.idea/AndroidProjectSystem.xml`
- `app/.idea/gradle.xml`
- `app/.idea/migrations.xml`
- `app/.idea/misc.xml`
- `app/.idea/runConfigurations.xml`
- `app/.idea/workspace.xml`

## Migrations removidas

- `20260606120000_add_challenge_banners.sql`: incorporada na baseline.
- `20260606123000_add_challenge_activity_and_premium.sql`: incorporada na baseline.
- `20260606124500_add_challenge_forum.sql`: incorporada na baseline.
- `20260607120000_expand_challenge_rules.sql`: incorporada na baseline.
- `20260607133000_add_challenge_ranking_and_gps_quality.sql`: incorporada na baseline.

Todas alteravam a mesma area funcional e nao eram necessarias para recriar o estado final local quando substituidas por uma baseline.

## Nova estrategia de migrations

- Manter `supabase/migrations/20260608000000_baseline_challenge_features.sql` como baseline local das features atuais de desafios/forum.
- Novas mudancas devem ser criadas como migrations pequenas depois da baseline.
- Revisao humana necessaria: em producao, nao remover migrations antigas do historico remoto sem conferir `supabase migration list` e a tabela `supabase_migrations.schema_migrations`.
- Estrategia segura para producao: criar branch/staging, aplicar baseline em banco limpo, comparar schema com producao, e so entao alinhar o historico de migrations.

## Objetos Supabase removiveis

Revisao humana necessaria: sem acesso ao projeto Supabase remoto e sem telemetry de uso, nao ha objeto de banco que possa ser removido com seguranca total.

Candidatos para auditoria:

- Policies de forum de desafio se a feature de forum for removida.
- Bucket `post-media` se posts com midia forem removidos do produto.
- Bucket `challenge-banners` se banners de desafio forem removidos.
- Colunas de GPS em `atividades` se ranking por qualidade de rota nao for usado fora do Android.

## SQL de limpeza

Arquivo gerado: `supabase/cleanup_review.sql`.

Ele contem consultas de inventario para policies, colunas, indices, triggers e functions. Drops destrutivos foram deixados comentados porque exigem confirmacao em staging/producao.

## Alteracoes importantes

- Baseline unica para as migrations locais.
- Cliente Supabase sem Realtime para reduzir dependencias e inicializacao.
- Remocao de logs que podiam expor dados de sessao em debug.
- README simplificado para onboarding de desenvolvedores junior.

## Riscos

- Revisao humana necessaria: a baseline substitui historico local, mas ambientes que ja aplicaram as migrations antigas precisam de estrategia de alinhamento antes de apagar historico remoto.
- Revisao humana necessaria: objetos Supabase so devem ser removidos depois de confirmar uso em outros clientes, scripts, dashboards ou Edge Functions.
