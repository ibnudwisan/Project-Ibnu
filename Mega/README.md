# The Sims 2 (PS2) Android Launcher Prototype

This project builds an Android APK with:
- SAF document picker (no broad storage permission)
- Coroutine-based background ISO copy with byte/percentage progress
- Import-complete dialog with an OK action
- Compose mock controller overlay
- C++ JNI bridge and CMake build for `arm64-v8a`
- GitHub Actions artifact upload

## Important limitation
This is a launcher/import/controller prototype, not a PS2 emulator. `nativeStartGame()` intentionally returns false until a legally obtained, compatible PS2 emulator core is integrated. The overlay buttons are visual placeholders and do not yet send input to a game core.

The PS2 Emotion Engine is not interchangeable with ARM64. Do not execute ISO bytes as native code or cast guest virtual addresses to host pointers. A real emulator needs guest-memory translation, instruction interpretation/recompilation, device emulation, and validated input/video/audio bridges.

## Build
Push this repository to GitHub. Open Actions → Build Android APK → select the latest run → download `Sims2AndroidLauncher-debug-apk`.
The workflow provisions JDK 17, Gradle 8.9, Android SDK, NDK and CMake, generates the Gradle wrapper, then runs `./gradlew assembleDebug`.

## Notes
- Imported ISO is stored in app-private storage (`files/games/`), so it is removed when the app is uninstalled.
- A large ISO requires free space for a full copy. The app copies through a temporary `.part` file and renames it after success.
- `-O3` is used for native compilation, but it does not automatically make an emulator faster; profile performance after integrating a core.
