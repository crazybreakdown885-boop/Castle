# Castle Crashers Mobile — Android prototype

An Android launcher prototype for organizing a locally owned Castle Crashers installation. Landscape UI, Android folder picker, and basic keyboard/gamepad detection.

## Important status
This is **not yet a playable Windows compatibility layer**. It does not currently run the Windows game executable, include the game, embed Steam, or provide Steam multiplayer. Selecting a folder only grants Android access to it.

Running the PC build locally requires a compatible Windows-on-Android runtime (for example Wine + Box64 with a suitable graphics stack) and device-specific testing. Steam multiplayer additionally requires the official Steam client and Steamworks to function in that environment. This project does not collect Steam credentials.

## Build
The GitHub Actions workflow builds a debug APK and publishes it as an Actions artifact. Download it from the latest successful workflow run under **Actions → Build Android APK → Artifacts**.

## Requirements
- Android Studio / Android SDK 35
- JDK 17
- Gradle 8.9
