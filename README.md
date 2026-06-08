# Zenith

Aplicativo Android em Kotlin/Jetpack Compose para atividades, perfil social, desafios e integração com Supabase.

## Estrutura

- `app/src/main/java/br/com/zenith/data`: cliente Supabase, modelos e lógica de dados.
- `app/src/main/java/br/com/zenith/service`: serviços Android, como rastreamento de atividade.
- `app/src/main/java/br/com/zenith/ui`: telas, componentes Compose, animações e tema.
- `app/src/main/java/br/com/zenith/viewmodels`: estado e regras de cada fluxo.
- `app/src/main/res`: imagens, fontes, drawables e strings.
- `supabase/migrations`: baseline SQL do schema usado pelas features atuais.

## Configuração local

1. Crie `local.properties` com `MAPS_API_KEY=...`.
2. Revise `app/src/main/assets/properties.env` com `SUPABASE_URL` e `ANON_KEY`.
3. Abra o projeto no Android Studio ou use Gradle pelo terminal.

## Comandos úteis

```bash
./gradlew :app:compileDebugKotlin
./gradlew build
./gradlew test
./gradlew installDebug
```

## Convenções

- Use Kotlin com indentação de 4 espaços.
- Composables usam `PascalCase` e terminam com `Screen`, `Components` ou nome descritivo.
- ViewModels terminam com `ViewModel`.
- Modelos ficam em `data/models` e representam tabelas/respostas do Supabase.
- Evite comentários óbvios, logs de debug permanentes e abstrações sem uso claro.

## Migrations

O projeto usa uma baseline única: `20260608000000_baseline_challenge_features.sql`.

Para produção, nao apague migrations já aplicadas sem alinhar o histórico remoto. Se o banco já recebeu as migrations antigas, marque a baseline como aplicada apenas depois de validar o schema atual em staging.
