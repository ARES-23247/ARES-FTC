# TeamCode package guide

This directory contains team 23247's Kotlin season code. It is not an empty FTC SDK template. Create or modify competition OpModes here; keep `FtcRobotController/` unchanged so SDK upgrades remain manageable.

## Package responsibilities

| Package | Responsibility |
|---|---|
| `config` | Canonical hardware-map names and robot-specific constants |
| `dsl` | Team adapters over ARESLib's TeleOp and mecanum autonomous DSLs; season state extension |
| `hardware` | FTC SDK implementations of mechanism IO interfaces; cached reads and safe writes |
| `opmodes` | `@TeleOp`/`@Autonomous` entry points, team robot facade, and persistent auto-to-TeleOp state |
| `opmodes.robot` | Drive, superstructure, and telemetry controllers used by the facade |
| `subsystems` | Redux-aware subsystem lifecycle implementations |

Start with `opmodes/ARESMecanumTeleOp.kt` for a driver-control example, `opmodes/ARESAuto.kt` for autonomous, and `opmodes/AresRobot.kt` for hardware composition. The detailed lifecycle contract is in the repository's [architecture guide](../../../../../../../../docs/ARCHITECTURE.md).

## Adding an OpMode

For a normal driver-controlled mode, extend `AresTeleOpBase` and return an `aresTeleOp { ... }` definition. Put one-time binding in `onConfigure`, state initialization in `onInit`, and per-frame driver intent in `onLoop`. The base owns robot construction, gamepad snapshots, periodic `robot.update(...)`, and guaranteed close.

For autonomous, extend `AresAutoBase`, set `pathName` to an auto in `src/main/assets/pathplanner/autos`, and call `configureAlliance(robot, Alliance.RED/BLUE)` from `buildRobot()`. The base loads the path, seeds localization, executes tasks, stops on completion/failure, persists the final pose, and closes hardware.

Do not copy an FTC sample's direct motor-write loop into a competition OpMode. Driver bindings should call the robot facade or dispatch `RobotAction`; registered subsystems translate immutable state to outputs.

## Adding a season mechanism

A mechanism normally needs:

1. An IO interface or shared ARESLib interface.
2. An FTC hardware implementation with cached sensor fields.
3. Immutable season state and actions needed to express intent/observations.
4. A subsystem whose `readSensors` consumes the cache and whose `writeOutputs` consumes `RobotState` plus the brownout scale.
5. Construction and registration in `AresRobot`.
6. Unit tests and a simulator/mock path.

Hardware reads belong in `refresh()` and happen after REV bulk caches are cleared. Property getters must return the cached fields. `safe()` and `close()` must drive outputs to their safe state. Register the IO with `HardwareRegistry` and register the subsystem with `base.registerSubsystem(...)`; these serve different but complementary lifecycle roles.

## Coordinates and controls

- Meters and seconds internally.
- Heading and angular velocity in radians, CCW-positive.
- Field-centric blue-alliance control negates both translational joystick axes before the core drive call; rotation keeps its CCW sign.
- Robot Controller drive names are `fl`, `fr`, `rl`, and `rr`.
- Use `RobotClock` for debounce and elapsed time.

From the repository root, run `.\gradlew.bat :TeamCode:testDebugUnitTest` in PowerShell.
