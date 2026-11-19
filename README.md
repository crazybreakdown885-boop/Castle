# Castle Mobile 3.0 — launcher foundation

Android-native Kotlin launcher foundation for legally owned Castle Crashers game files.

## Implemented in this branch
- Android Storage Access Framework folder selection with persisted access.
- Scan the selected game folder for EXE files and detect `Castle.exe`.
- Physical USB/Bluetooth gamepad discovery through Android InputManager, per-device IDs and basic key/axis event visibility.
- Keyboard and mouse event detection.
- Virtual Xbox-style touch controller test surface.
- Settings UI for game resolution profile, volume profile, touch visibility and automatic controller assignment preference.
- Android NSD service advertisement and discovery for local-network room discovery.

## Not implemented / not claimed
- **The original Windows EXE does not launch yet.** Wine for Android, Box64, graphics/audio userspace, native runtime packaging and the process-launch bridge are not included in this branch. The launch action deliberately reports this instead of pretending to run the game.
- The touch pad is currently a visual/input test surface; it is not yet bridged to Wine XInput.
- Controller auto-assignment is discovery/basic enumeration only; per-player remapping and XInput virtual-device injection are pending.
- LAN currently discovers/announces a service only; it does not synchronize gameplay.
- Steamworks multiplayer is not implemented.
- Resolution/audio choices are stored as profiles and are not yet applied to a Windows runtime.

## Runtime architecture needed for a real port
A compatible Windows x86 runtime requires Wine plus Box64 (and the required ARM64 native libraries, graphics driver stack, audio stack, prefix bootstrap and Android process execution bridge). These must be built and licensed as part of the APK/runtime package. A folder picker and a shell command alone cannot run a Windows EXE on Android.

## Build
Run `gradle --no-daemon :app:assembleDebug` with JDK 17 and Android SDK 35.

This is a development foundation, **not a playable Castle Crashers port**. Do not label this APK as able to run Castle.exe until that has been tested on a physical Android device.
