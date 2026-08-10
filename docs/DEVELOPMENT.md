# Development workflow

## Prerequisites

- Windows, macOS, or Linux with Git.
- Android Studio Ladybug (2024.2) or newer and an installed Android SDK.
- JDK 17 for Android/AGP builds.
- A JDK 21 toolchain for the standalone `simulator` module.
- ADB for Robot Controller deployment.
- The sibling `ARESLib-Kotlin` checkout for integrated development.

On Windows, the settings script can detect a standard JDK 17 or Android Studio's runtime when Gradle starts under an unsupported JVM. Prefer setting Android Studio's Gradle JVM explicitly rather than relying on the one-time fallback.

## Dependency behavior

Normal remote coordinates are changing JitPack artifacts:

```text
com.github.ARES-23247.ARESLib-Kotlin:{core,ftc-hardware,simulator,ftc-mocks}:master-SNAPSHOT
```

When `../ARESLib-Kotlin` exists, `settings.gradle` includes it as a composite build and substitutes both the JitPack and `com.areslib` coordinates with sibling projects. This is the preferred local development layout because TeamCode compiles against the exact shared source.

After changing ARESLib in the complete workspace, publish it before validating consumers that use Maven Local:

```powershell
Push-Location ..\ARESLib-Kotlin
.\gradlew.bat publishToMavenLocal
Pop-Location
```

## Build and test

Run commands from the ARES-FTC repository root.

```powershell
# Fast competition module build
.\gradlew.bat :TeamCode:assembleDebug

# JVM unit tests for the Android debug variant
.\gradlew.bat :TeamCode:testDebugUnitTest

# Build the full Robot Controller application
.\gradlew.bat assembleDebug

# Optional coverage report
.\gradlew.bat :TeamCode:koverHtmlReportDebug
```

Tests use JUnit 4 and Mockito. OpMode classes are excluded from Kover's configured TeamCode report, so test controller/subsystem behavior directly and cover OpMode integration in simulation.

For an ARESLib change used by this repository, a practical validation sequence is:

```powershell
Push-Location ..\ARESLib-Kotlin
.\gradlew.bat :core:test :ftc-hardware:test :simulator:test
.\gradlew.bat publishToMavenLocal
Pop-Location

.\gradlew.bat :TeamCode:testDebugUnitTest :TeamCode:assembleDebug
```

## Desktop simulation

The standalone simulator module compiles the real `TeamCode/src/main/java` sources against ARESLib's FTC mocks. Its Gradle toolchain is JDK 21.

```powershell
# Launch DesktopSimLauncher
.\gradlew.bat :simulator:run

# Pass launcher arguments through the project property
.\gradlew.bat :simulator:run -PappArgs="--headless"

# Exercise calibration/SysId routines
.\gradlew.bat :simulator:runCalibrationVerification
```

The Android `TeamCode` module also defines a headless runner whose classpath puts simulator/mocks before Android FTC classes:

```powershell
.\gradlew.bat :TeamCode:runSim
.\gradlew.bat :TeamCode:runSim -PappArgs="--opmode org.firstinspires.ftc.teamcode.opmodes.ARESMecanumTeleOp"
```

Launcher argument names can evolve in ARESLib; inspect `DesktopSimLauncher` before scripting a new option. The essential invariant is that simulation runs the same season facade and OpMode logic, not a rewritten simulator-only controller.

Simulation limitations:

- it validates state flow, path execution, coordinates, telemetry, and safe lifecycle behavior;
- mocks cannot establish real REV transaction timing, USB/I2C failures, motor current accuracy, radio congestion, or mechanical sign conventions;
- follow simulation with a restrained hardware test whenever a change affects physical IO or safety.

## Paths and autonomous assets

PathPlanner assets live in:

```text
TeamCode/src/main/assets/pathplanner/paths/*.path
TeamCode/src/main/assets/pathplanner/autos/*.auto
```

The `pathName` property on an autonomous class matches the auto filename without `.auto`. Every referenced path must exist, and the first path or auto starting pose must reflect the robot's intended starting placement.

To push assets without installing an APK:

```powershell
.\gradlew.bat :TeamCode:pushPaths
```

The task connects ADB to `192.168.43.1:5555`, creates `/sdcard/FIRST/paths` and `/sdcard/FIRST/autos`, and pushes the respective asset directories. `:TeamCode:installDebug` depends on `pushPaths`, so a normal debug install updates paths automatically.

The separate `TeamCode/src/main/assets/paths` directory contains field/obstacle/AprilTag data; it is not the PathPlanner `paths` directory pushed by `pushPaths`.

## Deploy

Connect the development machine to the Robot Controller network, then:

```powershell
adb connect 192.168.43.1:5555
.\gradlew.bat :TeamCode:installDebug
```

For a manually built APK, find the debug artifact under `TeamCode/build/outputs/apk/debug/` and use `adb install -r <apk>`. Prefer the Gradle install task because it also updates autonomous assets.

Before enabling a competition OpMode:

1. Confirm the Robot Controller hardware names against [ARCHITECTURE.md](ARCHITECTURE.md#hardware-configuration).
2. Place the robot on blocks and run `ARES Drivetrain Diagnostic` one motor at a time.
3. Confirm positive wheel/encoder directions and CCW-positive heading.
4. Verify emergency stop and OpMode close zero every mechanism.
5. Verify red and blue field-centric translation.
6. Run autonomous at reduced risk with a clear field and a second person ready to stop it.

## Adding a subsystem safely

Use the existing intake/flywheel structure as the smallest working example:

1. Define the mechanism's physical interface, preferably in ARESLib if it is reusable.
2. Implement FTC IO. Resolve hardware during construction; cache all sensor reads in `refresh()`.
3. Make `safe()` command neutral outputs and make `close()` call it.
4. Register IO with `HardwareRegistry` during initialization.
5. Add immutable state/actions and reducer behavior.
6. Implement `Subsystem.readSensors` and `Subsystem.writeOutputs` without crossing their responsibilities.
7. Register the subsystem once in `AresRobot`.
8. Add unit tests for absent/invalid sensors, power scale zero, and close/failure behavior.
9. Exercise it in the simulator and on restrained hardware.

Avoid allocations and direct hardware calls in the loop. A getter such as `currentAmps` must return the most recent cached value; it must not call `motor.getCurrent(...)`.

## Logs and dashboard connectivity

ARESLib runs the robot-side local services. The common addresses are:

- NT4: Robot Controller address on port `5810`;
- local log server: port `5002`;
- robot web/telemetry server: port `8082` where enabled.

The analytics desktop app connects over the local robot network and pulls logs. Do not place Firebase/GCS/API credentials on the Robot Controller or make robot behavior depend on internet connectivity.
