# ARES FTC

ARES FTC is team 23247's Kotlin robot application for the 2025–2026 **DECODE** season. It is the FTC season layer in the larger ARES robotics workspace: this repository owns the robot-specific hardware bindings, controls, OpModes, generated ARES routines, and field assets, while reusable state, math, localization, path following, safety, telemetry, and simulation live in the sibling [ARESLib-Kotlin](../ARESLib-Kotlin/) repository.

The project is based on FTC SDK 11.1. `FtcRobotController/` remains upstream SDK application code; team-owned robot code belongs in `TeamCode/`.

## Start here

- [Architecture and control lifecycle](docs/ARCHITECTURE.md)
- [Build, simulate, test, and deploy](docs/DEVELOPMENT.md)
- [Subsystem generator and hand-authoring guide](docs/SUBSYSTEM_AUTHORING.md)
- [Routines, autonomous selection, and visual controls](docs/ROUTINES_AND_CONTROLS.md)
- [Troubleshooting and safe diagnostics](docs/TROUBLESHOOTING.md)
- [Team code package guide](TeamCode/src/main/java/org/firstinspires/ftc/teamcode/readme.md)

## Repository layout

```text
ARES-FTC/
├── TeamCode/
│   └── src/main/
│       ├── java/org/firstinspires/ftc/teamcode/
│       │   ├── config/       Hardware-map names and physical constants
│       │   ├── dsl/          Team adapters over the ARES FTC OpMode DSL
│       │   ├── hardware/     FTC SDK implementations of season mechanism IO
│       │   ├── opmodes/      Driver Station entry points and robot facade
│       │   └── subsystems/   Sensor-to-state and state-to-output controllers
│       └── assets/           Canonical field data and derived Limelight map
├── simulator/                Desktop JVM launcher using real TeamCode + FTC mocks
├── FtcRobotController/       Upstream FTC SDK application module
├── .ares/                    Canonical routines, action/auto catalogs, and controller mappings
├── docs/                     Project documentation
└── .ares-robot.json          Team/season/robot identity for ARES tooling
```

Although Kotlin sources are under a directory named `java`, they are Kotlin and are compiled by the Kotlin Android plugin.

## Architecture in one minute

The season repositories are intentionally thin. Driver input or autonomous tasks dispatch `RobotAction` values to ARESLib's Redux store. Reducers create immutable `RobotState` snapshots. Registered subsystems read only cached sensor values, optionally dispatch observations, then command hardware from the current state.

```text
gamepad / auto task
       ↓
RobotAction → Store + rootReducer → immutable RobotState
                                      ↓
hardware refresh → readSensors() → writeOutputs(powerScale)
                                      ↓
                                  motor / LED IO
```

The team facade is `opmodes/AresRobot.kt`. It wraps ARESLib's `FtcMecanumRobot`, registers optional season mechanisms, and delegates driving, superstructure, and telemetry responsibilities to small controllers.

Key rules:

1. Use `RobotClock`, never a direct wall clock, so simulation and replay stay deterministic.
2. Keep heading CCW-positive in radians: `0` is +X and `π/2` is +Y.
3. Read hardware once per loop. Getters expose cached values; output methods must not trigger sensor reads.
4. Mutate robot intent through actions and reducers, not by editing state or writing motors from an OpMode.
5. Register season IO with `HardwareRegistry` so refresh, emergency stop, diagnostics, and close paths include it.

See [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) before changing lifecycle, coordinates, or safety code.

## Hardware-map contract

The canonical Robot Controller configuration names are:

| Device | Required name | Notes |
|---|---|---|
| Front-left drive | `fl` | Forward motor direction |
| Front-right drive | `fr` | Reversed motor direction |
| Rear-left drive | `rl` | Forward; use `rear`, not `back`, in the configured name |
| Rear-right drive | `rr` | Reversed |
| goBILDA Pinpoint | `pinpoint` | Primary odometry |
| IMU | `imu` | Fallback heading input |
| Limelight | `limelight` | Vision localization/alignment |
| Intake motor | `intake` | Optional season mechanism |
| Flywheel motor | `shooter` | Optional; encoder configured for 28 ticks/rev by default |

Indicator and Prism devices are optional and support the names documented in [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md#hardware-configuration). Missing optional season mechanisms report telemetry and do not prevent the drivetrain from initializing. Drivetrain names are exactly `fl`, `fr`, `rl`, and `rr`.

## Quick commands

From PowerShell in this repository:

```powershell
# Compile and package the Robot Controller app
.\gradlew.bat :TeamCode:assembleDebug

# Run TeamCode unit tests
.\gradlew.bat :TeamCode:testDebugUnitTest

# Regenerate and verify Kotlin compiled from the offline .ares project
.\gradlew.bat :TeamCode:generateAresProject
.\gradlew.bat :TeamCode:verifyAresProject

# Preview or create subsystem starters; normal project generation refreshes plumbing
.\gradlew.bat :TeamCode:previewSubsystemChanges
.\gradlew.bat :TeamCode:generateSubsystemStarters
.\gradlew.bat :TeamCode:generateAresProject

# Run the desktop simulator with TeamCode and FTC mocks
.\gradlew.bat :simulator:run

# Run a headless simulation through the Android module's classpath
.\gradlew.bat :TeamCode:runSim

# Install the debug app (generated routines are compiled into the APK)
.\gradlew.bat :TeamCode:installDebug
```

Android builds require JDK 17. The standalone simulator uses a JDK 21 toolchain. When this repository is beside `ARESLib-Kotlin`, `settings.gradle` substitutes local ARESLib projects automatically, so changes to the shared library are tested against their source. In the complete four-repository workspace, publish shared changes first when another consumer relies on Maven Local:

```powershell
Push-Location ..\ARESLib-Kotlin
.\gradlew.bat publishToMavenLocal
Pop-Location
```

See [docs/DEVELOPMENT.md](docs/DEVELOPMENT.md) for prerequisites, path deployment, simulator arguments, and validation order.

## Safety expectations

`AresRobot.update()` wraps all season work in a fail-safe boundary. If a season sensor, subsystem, or telemetry path throws, it calls both the registered-subsystem stop and the platform hardware stop before rethrowing. Autonomous additionally aborts on path-load or loop failure, clears the task executor, zeros drivetrain and season outputs, and always closes the robot in `finally`.

That protection depends on every output device participating in the lifecycle. New IO must implement a zero-output `safe()`/`close()` path and be registered during initialization. Never catch an actuator failure and continue applying an old command.

## Contributing

- Do not edit `FtcRobotController/` for team features.
- Keep season-specific code in `TeamCode`; move generally reusable behavior into ARESLib-Kotlin.
- Add a test or simulator check for control, lifecycle, coordinate, or safety changes.
- Verify both the Android module and desktop simulator after changing shared TeamCode sources.
- Preserve the offline-first design: the robot serves local logs; the desktop analytics application is responsible for cloud synchronization.

FIRST's general SDK documentation remains available at [FTC Docs](https://ftc-docs.firstinspires.org/) and the [FTC SDK Javadoc](https://javadoc.io/doc/org.firstinspires.ftc).
