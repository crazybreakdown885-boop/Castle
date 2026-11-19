# Castle Mobile 0.3.0 — runtime integration status

## Confirmed source inputs

### User-supplied game archive
- `CastleCrashers Opti.zip` contains the Windows `castle.exe` and its game data.
- `castle.exe` is PE32 x86: COFF machine `0x014c`, optional-header magic `0x010b`.
- The game archive remains user-provided. Its EXE, DLLs, data and Steam configuration are not committed to the repository or bundled in the APK.

### Upstream Windows compatibility engine
- The project pins `brunodev85/winlator-app` as a Git submodule at `ecd5ab2980de79eb955621cef26a3b4672013875`.
- The APK builds the upstream Java sources, resources, assets, JNI libraries and native CMake targets into the Castle application module. It is intended to install as one APK, not as a second Winlator app.
- The build uses the upstream ARM64 rootfs, Box64 bundle, Wine WoW64 components, graphics backends and audio runtime. The zstd Android AAR supplies its ARM64 JNI library.

## What 0.3.0 now implements

- SAF import of the complete selected game folder into app-private storage.
- Transactional import: the old game copy is preserved until the new copy has been copied and validated.
- PE inspection rejects an invalid executable and identifies the expected Windows x86/32-bit target.
- Runtime bootstrap through Winlator's rootfs installer.
- Wine container creation and persistence.
- Copying the imported game into the container's `C:\\CastleCrashers` directory.
- Launching the executable through Winlator's `XServerDisplayActivity`, not through a fake button or a bare `ProcessBuilder`.
- Settings passed to the Wine container: resolution, PulseAudio volume, DXVK/WineD3D and Vulkan/OpenGL driver pair.
- Touch controls use Winlator's built-in `Virtual Gamepad` profile (profile 3), enabled by the Castle touch setting.
- Upstream `GamepadHandler` provides four gamepad slots and automatic external-controller assignment when manual slot bindings are not configured. The engine also handles keyboard and mouse input.

## CI build and package verification

GitHub Actions run **44598212504** completed successfully on commit `9d59dad4126b6517207a0e744a26dc0e817b9c0f`.

- Artifact: `CastleMobile-0.3.0-debug`
- APK artifact size: 160,824,665 bytes
- CI also checked that the APK contains the rootfs, container pattern, Box64, PulseAudio, virtual gamepad profile, zstd JNI library and Winlator/Vortek/Gladio/VirGL/MIDI native libraries.

This confirms the Android project compiles and the required runtime payload is embedded in the APK. It does **not** prove that Castle Crashers launches or runs smoothly on the Moto G7 Play; that device test is intentionally left to the user.

## Multiplayer boundary

- Local co-op input on one phone is supported through the game's local controller support and Winlator's four XInput slots.
- LAN service discovery is only a discovery test. It does not synchronize game state or create a LAN game.
- Steam friends, matchmaking, invites and Steamworks networking are not implemented. A `steam_api.dll` or `steam_settings` directory does not establish official Steam connectivity.

## Device validation still needed

The user will perform the device test. The main things to observe are first-run rootfs extraction/storage, Wine prefix creation, game startup, D3D9 renderer choice, audio, touchscreen controls, hot-plugged controllers and performance. The Moto G7 Play's Adreno 506 may need WineD3D or a different graphics-driver selection if the default Vulkan path is unstable.
