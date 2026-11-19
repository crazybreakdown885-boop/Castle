# Third-party notices

Castle Mobile includes source and runtime components from the upstream Winlator Android project.

- Project: https://github.com/brunodev85/winlator-app
- Pinned source revision: `ecd5ab2980de79eb955621cef26a3b4672013875`
- The upstream repository identifies its license as GNU LGPL v2.1. The complete upstream `LICENSE` file is retained in the `vendor/winlator-app` Git submodule.
- Upstream credits include Wine, Box86/Box64, Mesa (Turnip/Zink/VirGL), DXVK, VKD3D and CNC DDraw. Their own license and notice files must be retained when their corresponding binaries are distributed.

The Windows game files are not included in this repository or APK. Users select and import their own Castle Crashers installation. No game executable, DLL, soundtrack or data archive from the supplied game archive is committed here.

Before publishing a signed/release APK, verify the license and source-offer obligations for every runtime component included in that exact build.
