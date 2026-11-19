# Castle Mobile 0.3.0 — runtime integration work

Android-native project for launching legally owned Castle Crashers Windows game files.

## What this version actually contains

- Native Android launcher and persisted folder selection (Storage Access Framework).
- Detection of `Castle.exe` in the selected folder.
- USB/Bluetooth input-device enumeration and basic Android key/axis event reporting.
- Keyboard and mouse event detection while the launcher has focus.
- A visual Xbox-style touch-control prototype.
- Saved display/audio preferences (not yet connected to a Windows runtime).
- Android NSD room advertisement/discovery prototype (not gameplay networking).

## Important: current execution status

**This is not yet a playable Windows game port.** The current APK does not include Wine, Box64, a Linux userspace, graphics/audio translation libraries, or the native process bridge. Therefore it cannot execute `Castle.exe`. The touch prototype does not inject XInput into Wine. Detected controllers are not yet assigned to separate game players. LAN discovery does not synchronize gameplay. Steamworks is not integrated.

Do not treat a successful Gradle build as proof that the game runs.

## Runtime integration plan

The intended technical base is a Wine + Box64 Android runtime, with ARM64 native components, a compatible userspace, graphics translation, audio, prefix initialization, and an Android process/surface bridge. This is a substantial native integration, not a Gradle-only dependency.

1. **Runtime provenance and licensing:** pin upstream source revisions, preserve licenses/notices, document every redistributable binary, and build reproducibly.
2. **ARM64 runtime:** build/package Box64 and Wine plus required userspace libraries and prefix bootstrap.
3. **Graphics/audio:** integrate a supported Vulkan/OpenGL path and audio backend; expose real runtime configuration.
4. **Game launch:** copy/import game files safely from SAF into app-private storage, create a Wine prefix, launch the selected EXE, capture stdout/stderr and exit status, and render the game surface.
5. **Input:** map Android gamepad IDs to independent player slots; translate keys/axes to XInput; route mouse/keyboard; make touch controls emit the same logical input events.
6. **LAN:** implement an actual session protocol and gameplay transport, not just NSD discovery.
7. **Steam:** validate Steam client/Steamworks compatibility first. Castle Crashers uses Steam P2P networking for online co-op; LAN discovery alone cannot replace that integration.

## Device compatibility warning

Upstream Winlator WB64 lists Snapdragon 460 / Adreno 610 / Vulkan 1.1 as minimum requirements in its README. Devices below that class may fail or perform poorly even after integration. Compatibility must be tested on the target phone; no performance claim is made here.

## Five-pass validation gate

A release may only be called playable after all five passes have evidence:

1. **Build/install:** clean CI build, APK install, launch, storage permission persistence.
2. **Runtime:** Wine/Box64 bootstrap, EXE process starts, logs captured, clean exit/relaunch.
3. **Graphics/audio:** game renders, resolution changes take effect, audio plays and volume changes.
4. **Input:** touch, keyboard, mouse, one controller, then multiple independently assigned controllers.
5. **Networking/device:** two-device LAN gameplay and Steam online co-op tested; performance, thermal behavior, memory use and stability measured on physical hardware.

Current 0.3.0 status: launcher foundation only; passes 1 is only partially represented by CI compilation. Passes 2–5 are not passed.

## Build

`gradle --no-daemon :app:assembleDebug` with JDK 17 and Android SDK 35.
