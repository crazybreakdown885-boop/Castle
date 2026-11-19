# Multiplayer status — Castle Mobile 0.3.0

## Working input path

The embedded Winlator engine's `GamepadHandler` exposes four Windows gamepad slots and defaults to XInput mapping. When no manual player-slot configuration overrides it, it auto-assigns connected Android controllers. The built-in `Virtual Gamepad` profile is enabled for touchscreen play, occupying one slot when touch controls are on.

This supports local same-device co-op input, subject to the game and device accepting the mapped controllers.

## LAN discovery is not LAN gameplay

Castle Mobile's NSD service currently advertises/discovers a service named `_castlecoop._tcp.`. That is only room discovery. It does not transport game state, input, save data, lobby messages or synchronization. The UI must label it as a discovery test only.

## Steamworks status

The supplied game folder contains a `steam_settings` directory with a Goldberg-style configuration (including a local emulator account name and a configured listen port). That is not proof of an authenticated Steam client session or official Steamworks connectivity. Castle Mobile does not bundle or activate a Steam API emulator and does not use it to bypass Steam authentication.

A legitimate Steam online path would require the official game/Steam account environment and a compatible Steam client/Steamworks runtime inside the Wine environment. That integration is not implemented in 0.3.0.

## Do not claim yet

- LAN co-op between separate phones.
- Steam friends, matchmaking, invites or Steam networking.
- Any online play merely because an NSD room appears.

The supported target for this build is local co-op controls on one Android device. Online multiplayer remains a separate engineering task.
