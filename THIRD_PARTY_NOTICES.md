# Third-party notices

Castle Mobile includes source and runtime components from the upstream Winlator Android project.

- Project: https://github.com/brunodev85/winlator-app
- Pinned source revision: `ecd5ab2980de79eb955621cef26a3b4672013875`
- The upstream repository identifies its license as GNU LGPL v2.1. The complete upstream `LICENSE` file is retained in the `vendor/winlator-app` Git submodule and copied into `assets/licenses/` in the APK. The build also copies upstream `LICENSE*`, `COPYING*` and `NOTICE*` files found in the submodule into the APK.
- Upstream credits include Wine, Box86/Box64, Mesa (Turnip/Zink/VirGL), DXVK, VKD3D and CNC DDraw. Their own license and notice files must be retained when their corresponding binaries are distributed.

The Windows game files are not included in this repository or APK. Users select and import their own Castle Crashers installation. No game executable, DLL, soundtrack or data archive from the supplied game archive is committed here.

The exact source revision is pinned above and remains available in the public Git submodule. The APK includes the notices collected by the build; review individual third-party license obligations if the runtime dependency set changes.
