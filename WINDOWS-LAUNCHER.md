# Windows Launcher

For the easiest local launch, double-click:

```bat
launch-games-hub.bat
```

This script builds the project automatically if the launcher JAR is missing, then starts the hub with `javaw` so no terminal window stays open.

To build a Windows `.exe` package, double-click:

```bat
build-games-hub-exe.bat
```

Artifacts will be created in:

```text
dist\
```

Expected outputs:

- `dist\Java Games Hub\Java Games Hub.exe`

If WiX Toolset is installed and available in `PATH`, the script also creates:

- `dist\Java Games Hub-1.0.0.exe`
