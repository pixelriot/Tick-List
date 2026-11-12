# Tick List for Android

Native Android port of Tick List 0.7.9. The SvelteKit and Tauri app in the repository root is unchanged.

The UI follows the Omarchy desktop: dark by default, JetBrains Mono, flat panels, 1px borders, and 2dp corners. Tokyo Night is the default palette (the Omarchy default). Catppuccin, Gruvbox, Nord, Everforest, and Kanagawa are selectable in settings, each with a dark and a light variant. Appearance can follow dark, light, or the system setting.

## What it does

- Lists overview: create, rename, and delete lists. Deleting a list removes it from this device only, same as the web app.
- List detail: add, check, edit, and delete items. Unchecked and completed items are sorted by name. Long-press a row or use the edit icon. Closing the edit sheet saves; amount `0` becomes `1`.
- Sharing: share codes are `TL` plus six digits. Enter a code in the new-list field to join. The share sheet copies the code.
- Offline cache: lists are stored as JSON on disk and shown immediately. Shared lists stay visible while a refresh runs.
- Sync: Supabase tables `lists`, `list_items`, and `list_shares`. Soft deletes use `deleted_at`. Item conflicts are last-write-wins on `updated_at`. Item edits are debounced (300ms) and sent with upserts. Realtime listens to `list_items` for the open shared list.

The application id is `com.pixelriot.ticklist`. The Tauri app uses `io.github.twinpixel.tick-list`, so both can be installed on the same device.

## Supabase configuration

The URL and publishable key are not stored in source. `app/build.gradle.kts` reads them into `BuildConfig` from, in order:

1. Gradle project properties: `-PSUPABASE_URL=... -PSUPABASE_ANON_KEY=...`
2. Environment variables: `SUPABASE_URL`, `SUPABASE_ANON_KEY`
3. `local.properties` in this directory

```bash
cp local.properties.example local.properties
```

`local.properties` is gitignored. Set `sdk.dir` there, or export `ANDROID_HOME`. Leave the Supabase keys blank to build an offline-only APK. The project’s public client values are the same ones already shipped with the web app; pass them at build time when you want sync.

Apply `src/lib/supabase/schema.sql` in the Supabase SQL editor and enable Realtime on `list_items` before sharing will succeed.

## Build

Requirements: JDK 17+, Android SDK platform 37, Android Gradle Plugin 9.5. `targetSdk` stays 35.

```bash
cd android-native
./gradlew assembleDebug
./gradlew testDebugUnitTest
```

The debug APK is `app/build/outputs/apk/debug/app-debug.apk` (`versionName` 0.7.9). It is signed with the debug keystore and can be sideloaded.

JetBrains Mono is bundled under `app/src/main/res/font`. Its license is `third_party/jetbrains-mono/OFL.txt`.
