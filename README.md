# Castle Mobile 0.3.0

Android app that imports a user's Castle Crashers Windows game folder and launches it through an **embedded Winlator/Wine/Box64 runtime**. The runtime source, ARM64 native libraries and rootfs assets are built into the same APK; a separate Winlator installation is not required.

## Included in 0.3.0

- SAF folder import into app-private storage, with transactional replacement.
- PE inspection to verify that `castle.exe` is Windows x86/32-bit.
- Embedded Winlator engine source, Wine WoW64 rootfs, Box64, native X server/renderers, audio runtime and graphics-driver assets.
- Automatic Wine container creation and game copy into `C:\\CastleCrashers`.
- Launch through Winlator's X server activity.
- Settings applied to the Wine container: resolution, PulseAudio volume, DXVK/WineD3D and Vulkan/OpenGL driver combinations.
- Winlator's virtual Xbox-style gamepad profile for touchscreen play.
- Winlator input support for keyboard, mouse and up to four XInput gamepad slots with automatic controller assignment.

The APK does **not** contain the game. Import a game folder that you are legally entitled to use.

## Build result

GitHub Actions run [44598256750](https://github.com/crazybreakdown885-boop/Castle/actions/runs/44598212504) completed successfully and verified the embedded runtime payload.

Download the `CastleMobile-0.3.0-debug` artifact from that run's **Artifacts** section. It is a debug APK artifact, not a signed Play Store release.

## Important limitations

- CI confirms compilation and that runtime assets are packaged. The game has **not** been launched on the Moto G7 Play; the device test is left to the user.
- The Snapdragon 632 / Adreno 506 may need a different graphics-driver or WineD3D setting if the default Vulkan path is unstable or slow.
- Local co-op input on one phone is the current multiplayer target.
- LAN room discovery is only discovery; LAN gameplay synchronization is not implemented.
- Steam friends, matchmaking, invites and Steamworks online networking are not implemented.

## Build from source

Clone with submodules:

```bash
git clone --recurse-submodules https://github.com/crazybreakdown885-boop/Castle.git
cd Castle
gradle --no-daemon :app:assembleDebug
```

Requirements: JDK 17, Android SDK 35, Android NDK 24.0.8215888 and CMake 3.22.1. The GitHub Actions workflow applies the small host-activity/touch-profile adaptation to the pinned Winlator submodule before building.

## Licensing and game files

See [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md). The game EXE, DLLs and data are user-provided and must not be committed or redistributed in this repository.
