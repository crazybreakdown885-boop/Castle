# Castle Mobile 0.3.0 — integration and validation checklist

## Runtime
- [ ] Pin Wine source and build configuration.
- [ ] Pin Box64 source and ARM64 build configuration.
- [ ] Package compatible ARM64 userspace and required shared libraries.
- [ ] Add graphics translation and surface/window integration.
- [ ] Add audio backend.
- [ ] Initialize and persist Wine prefix inside app-private storage.
- [ ] Launch EXE imported through SAF; collect logs and exit code.

## Input
- [ ] Build normalized input model (device ID, player slot, key, axis).
- [ ] Support independent controller assignment and reconnect.
- [ ] Translate Android gamepad events to Wine XInput.
- [ ] Route external keyboard and mouse into the game.
- [ ] Make touchscreen controls emit the same normalized events.
- [ ] Test simultaneous multi-touch and controller input.

## Display and audio
- [ ] Apply resolution to the runtime/window, not just preferences.
- [ ] Apply volume to the audio backend.
- [ ] Handle rotation, fullscreen, pause/resume and focus loss.

## Multiplayer
- [ ] Implement LAN session negotiation and actual gameplay transport.
- [ ] Verify game protocol compatibility before claiming LAN co-op.
- [ ] Investigate Steam client/Steamworks availability inside the runtime.
- [ ] Test Steam P2P NAT traversal and required ports; do not equate NSD with Steam.

## Release gates (five independent passes)
1. Clean build/install and persisted storage access.
2. Wine/Box64 startup and real Castle.exe process execution.
3. Rendering, resolution and audio on device.
4. Touch, keyboard, mouse and multiple controllers.
5. Two-device LAN, Steam co-op, thermal/memory/stability tests.

No unchecked item may be represented as complete. Physical-device evidence is required for passes 2–5.
