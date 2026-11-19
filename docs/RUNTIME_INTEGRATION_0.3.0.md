# Castle Mobile 0.3.0 — runtime integration notes

## Verified inputs (2026-11-19)

### Game archive supplied in this conversation
- Archive: `CastleCrashers Opti.zip`
- 713 ZIP entries total (702 files), including 664 files under `data` and 34 entries under `steam_settings`.
- Main executable: `castle.exe` (1,943,552 bytes).
- Adjacent Windows modules include `D3DX9_43.dll`, `dinput8.dll`, `steam_api.dll`, and `optiprojects.asi`.
- PE COFF machine for `castle.exe` and the listed DLLs: `0x014c` (IMAGE_FILE_MACHINE_I386); PE optional-header magic: `0x010b` (PE32). This is a 32-bit Windows x86 program, not an x86-64 Windows program.
- The game archive must remain user-provided. Do not commit or publish its EXE, DLLs, data, or Steam files.

### Winlator APK supplied in this conversation
- APK: `Winlator_11.2.apk`
- Android native ABI observed: `arm64-v8a`.
- Bundled asset `assets/box64/box64-0.4.4.tzst`.
- Bundled `assets/rootfs.tzst` contains `/opt/wine/bin/wine`, `/opt/wine/bin/wineserver`, `/opt/wine/lib/wine/x86_64-unix/ntdll.so`, and extensive `/opt/wine/lib/wine/i386-windows/` built-ins, including `winexinput.sys`.
- Rootfs also contains the PulseAudio runtime and the APK contains Turnip, VirGL, Zink and Vortek driver assets.
- The supplied APK does not expose a standalone `libbox86.so`; its apparent 32-bit Windows path is therefore based on its x86_64 Wine/Box64 WoW64 arrangement, which must be confirmed from the exact upstream source and runtime configuration before reuse.
- Do not repackage the supplied APK's proprietary/third-party binary assets blindly. Integrate from source/build outputs with the required notices, source offers and license compliance.

## Implementation decision

The game is PE32 x86. The first runtime candidate is an ARM64 Android host running an x86_64 Wine WoW64 build under Box64 with 32-bit Windows support enabled, plus a D3D9 translation path. This is not the same as executing an x86 Linux binary directly and is not solved by Box64 alone. Validate the exact Wine build, Box64 compile options, WoW64 prefix architecture and 32-bit D3D9 libraries before enabling the launch button.

## Work completed in this branch

- Added `GameFolderInstaller`: recursively copies the complete SAF-selected game folder to app-private storage, preserving subdirectories and reporting progress.
- The launcher now searches the imported private copy for `castle.exe` and explicitly reports that runtime execution remains unavailable until the Wine/WoW64/Box64 stack is integrated.
- No game binaries from the user archive are committed.

## Remaining engineering gates

1. Build a reproducible, license-compliant ARM64 runtime bundle (Wine WoW64 + Box64 with the required 32-bit Windows support + libc/runtime dependencies).
2. Add the Android native process/bootstrap layer, Wine prefix creation, environment setup, and safe process lifecycle/log capture.
3. Validate D3D9 rendering on Adreno 506; compare WineD3D and DXVK paths and test the game's supplied D3DX9 module.
4. Connect Android touch and physical input to a real virtual XInput/DirectInput device inside the Wine environment; support independent player slots.
5. Apply resolution/audio settings to the runtime rather than only saving preferences.
6. Implement LAN gameplay transport only if the game exposes a compatible protocol/mod path; service discovery alone is not gameplay networking.
7. Treat Steamworks as a separate compatibility and legal/technical investigation. A local `steam_api.dll` or `steam_settings` folder does not prove Steam P2P compatibility.
8. Run install, launch, gameplay, audio, graphics, controller, multi-controller and network tests on the actual Moto G7 Play/custom ROM. CI compilation is not device validation.

## Five review passes required before calling the port functional

- Pass 1 — APK build, install, app-private storage and permissions.
- Pass 2 — runtime extraction, ELF dependencies, executable permissions, Wine prefix and clean shutdown.
- Pass 3 — Castle Crashers startup, D3D9 renderer, frame pacing, audio and stability.
- Pass 4 — touchscreen mapping, keyboard/mouse, hot-plug and independent controller/player assignment.
- Pass 5 — LAN gameplay, Steamworks findings, regression and real-device performance.

A pass is only marked complete when test output or reproducible device evidence exists.
