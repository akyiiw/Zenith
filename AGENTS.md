# Repository Guidelines

## Project Structure & Module Organization

Zenith is a single-module Android project. The root Gradle files (`settings.gradle.kts`, `build.gradle.kts`, `gradle/libs.versions.toml`) define the build, plugin, and dependency versions. App code lives in `app/src/main/java/br/com/zenith`, organized by responsibility:

- `data/`, `data/models/`, and `data/sleep/` hold Supabase configuration, DTOs, repositories, and sleep data logic.
- `service/` contains Android services such as activity tracking.
- `ui/` contains Compose screens, components, animations, and theme files.
- `viewmodels/` contains feature-specific state and business logic.
- `utils/` contains shared helpers.

Resources are under `app/src/main/res`, fonts under `res/font`, images/icons under `res/drawable` and `res/mipmap`, and bundled assets under `app/src/main/assets`. Local unit tests belong in `app/src/test`; instrumented and Compose UI tests belong in `app/src/androidTest`.

## Build, Test, and Development Commands

- `./gradlew build`: compile, run checks, and package debug/release variants.
- `./gradlew :app:compileDebugKotlin`: fast Kotlin compilation check for app code.
- `./gradlew test`: run local JVM unit tests.
- `./gradlew connectedAndroidTest`: run instrumented tests on a connected emulator/device.
- `./gradlew installDebug`: install the debug APK on a connected emulator/device.

Use Android Studio for interactive Compose previews, emulator management, and logcat inspection.

## Coding Style & Naming Conventions

Use Kotlin with 4-space indentation and idiomatic Compose patterns. Keep composables in PascalCase with descriptive suffixes such as `HomeScreen`, `ProfileComponents`, or `ZenithLoading`. ViewModels should end in `ViewModel`; data models should be singular nouns. Keep package paths aligned with the existing `br.com.zenith` feature grouping. Prefer version catalog aliases in `gradle/libs.versions.toml` for dependency changes.

## Testing Guidelines

JUnit 4 is configured for local tests, and AndroidX JUnit, Espresso, and Compose UI test libraries are configured for instrumentation tests. Name test classes after the subject under test, for example `SleepSettingsRepositoryTest` or `HomeScreenTest`. Add local tests for pure Kotlin logic and instrumentation tests for Android framework, navigation, permissions, maps, or Compose UI behavior.

## Commit & Pull Request Guidelines

Recent history is informal, with one conventional-style example (`refactor: everything`). Prefer concise imperative commits and use a conventional prefix when useful, such as `fix:`, `feat:`, `refactor:`, or `test:`. Pull requests should include a short summary, test commands run, linked issues when applicable, and screenshots or screen recordings for UI changes.

## Security & Configuration Tips

Do not commit secrets. `local.properties` is ignored and should hold machine-local values such as `MAPS_API_KEY`. Review `app/src/main/assets/properties.env` before changes because bundled assets ship with the app. Keep Supabase keys and environment-specific values out of source whenever possible.
