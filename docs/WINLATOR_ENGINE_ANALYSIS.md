# Winlator engine integration analysis

Date: 2026-11-19

## Source inspected

Upstream repositories:
- `brunodev85/winlator` (LGPL-2.1 project)
- `brunodev85/winlator-app` (Android application source)

The source was inspected for the current `main` branch. This document records the pinned engine source and the current single-APK integration. A successful build and device launch still need to be confirmed.

## What the upstream engine actually does

A direct `ProcessBuilder("box64", "wine", "castle.exe")` call is not a complete launch path.

The upstream launcher constructs a Wine environment around a RootFS and starts an X server environment. Relevant upstream classes include:

- `RootFS`: app-private root filesystem, Wine path, home, tmp and library paths.
- `XEnvironment` and `XServerDisplayActivity`: start and host the graphical session.
- `GuestProgramLauncherComponent`: extracts Box64, sets Box64/Wine environment variables, sets `DISPLAY=:0`, and launches the guest program through the root filesystem.
- `ContainerManager` / `Container`: creates and activates a Wine prefix and stores driver/audio/display settings.
- `InputControlsManager`, `InputControlsView` and `ExternalController`: provide the actual controller and touchscreen bridge.
- `WineInstaller`, `GeneralComponents` and `TarCompressorUtils`: install and unpack runtime components and the Box64 build.

The upstream native build also contains Winlator's X-server bridge and graphics renderer libraries. Its assets include a compressed rootfs, Box64 bundle, Wine/graphics components and audio runtime. The root project has separate `vortek` and `gladio` submodules.

## Castle Crashers compatibility target

The supplied game executable was inspected as PE32, machine `0x014c` (Windows x86, 32-bit). It therefore needs a Wine WoW64 configuration that supports 32-bit Windows applications, plus the ARM64 Box64/Linux userspace arrangement used by the selected Winlator build. Box64 by itself is not a Windows runtime.

The first graphics candidate is the upstream Wine D3D9 path; DXVK should be an optional experiment only after checking Vulkan support and stability on the target Adreno 506 device. The APK must not assume that a Vulkan driver available on a newer GPU is available here.

## Integration requirements before the game can launch

1. Pin compatible upstream Winlator, Vortek and Gladio source revisions and initialize all required submodules in CI.
2. Integrate the engine's Android Java sources, resources, manifest entries, native CMake targets and JNI libraries into the Castle app without creating a second installed app.
3. Bundle or reproducibly build the runtime assets and comply with each component's license and source/notice requirements.
4. Adapt the game import path to a Winlator Wine prefix / C: drive mapping without modifying the user's original game files.
5. Launch through the XEnvironment/XServer lifecycle, not a bare shell process.
6. Connect Castle Mobile's touch/gamepad UI to the upstream input-controls bridge and map independent Android device IDs to player slots.
7. Pass resolution, audio and renderer settings into the actual Wine container configuration.
8. Verify D3D9 startup and input on the real device. LAN discovery is not game networking; Steamworks support remains a separate compatibility investigation.

## Current Castle Mobile status

Implemented in branch `v0.3.0`:
- Pinned `brunodev85/winlator-app` as a Git submodule at commit `ecd5ab2980de79eb955621cef26a3b4672013875`.
- Merged Winlator Java sources, resources, assets, JNI libraries and CMake targets into the Castle application module so the runtime is intended to ship inside one APK, not as a second installed app.
- Added a runtime bridge that installs the Winlator rootfs, creates a Wine container, copies the imported game into `C:\\CastleCrashers` and opens `XServerDisplayActivity` with `castle.exe`.
- Added runtime configuration for resolution, PulseAudio volume and a selectable DXVK/WineD3D path.
- Configured Winlator's built-in `Virtual Gamepad` profile (profile 3) to appear automatically when touchscreen controls are enabled. The upstream `GamepadHandler` has four gamepad slots and auto-assigns connected external controllers when no manual player assignment overrides it.
- Added PE header inspection and transactional game import.

Confirmed by CI:
- The merged app module and native CMake targets compile successfully.
- GitHub Actions run `44598256750` produced a 160,824,710-byte debug APK and passed checks for the embedded rootfs, Box64, zstd JNI, audio, touch profile and native graphics libraries.

Still awaiting device validation:
- First launch, Wine prefix creation, D3D9 rendering, sound and controller behavior on the Moto G7 Play. The user will perform this test.

Not implemented:
- Castle Crashers LAN gameplay transport or Steamworks multiplayer. Local room discovery is not a gameplay network implementation.

Do not treat the APK as playable until CI produces the merged APK and the game starts on the device.
