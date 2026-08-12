# Architecture and lifecycle

This guide describes the contracts between ARES-FTC and ARESLib-Kotlin. These contracts matter more than individual class names: breaking their ordering can create stale sensor data, one-frame-late safety decisions, duplicate hardware reads, inconsistent pose, or outputs that survive a failure.

## System boundary

ARES-FTC is a season-specific shell over ARESLib-Kotlin.

ARES-FTC owns:

- FTC Robot Controller entry points for the current season;
- physical configuration names and mechanism constants;
- FTC SDK implementations of intake and flywheel IO;
- DECODE superstructure state and driver bindings;
- current-season generated routines, field assets, and named mechanism commands.

ARESLib-Kotlin owns:

- `FtcMecanumRobot` and the platform lifecycle;
- immutable `RobotState`, `Store`, `RobotAction`, and `rootReducer`;
- drive kinematics, EKF pose estimation, vision fusion, and path following;
- power/brownout management and the shared subsystem interface;
- `HardwareRegistry`, telemetry, logging, NT4, and the desktop simulation framework.

If code is usable unchanged by another season or league, it probably belongs in ARESLib. Hardware names, mechanism wiring, and game-specific behavior belong here.

## Robot composition

`opmodes/AresRobot.kt` is the composition root. Its `base` is an ARESLib `FtcMecanumRobot`. During construction it attempts to add season mechanisms:

- `FtcIntakeIO` → `IntakeSubsystem`;
- `FtcFlywheelIO` → `FlywheelSubsystem`;
- zero, one, or two indicator-light subsystems;
- an optional Prism RGB subsystem using I2C first, then PWM fallback.

Intake, flywheel, and lighting failures are isolated during initialization and reported through telemetry. The base drivetrain remains available when optional season hardware is absent. The live named-command registry contains only capabilities backed by discovered hardware, so a routine requiring a missing mechanism/light is rejected instead of silently completing a no-op.

## Redux data flow

Robot intent flows in one direction:

```text
Driver binding / autonomous Task / sensor observation
                         │
                         ▼
                    RobotAction
                         │ dispatch
                         ▼
Store ─────────────── rootReducer
  │                      │
  └──── current immutable RobotState ◀──┘
                         │
                         ▼
          Subsystem.writeOutputs(state, scale)
                         │
                         ▼
                    hardware IO
```

The DECODE state is `SeasonSuperstructureState`, stored in `SuperstructureState.custom` and accessed through the safe `season` extension. If no season state has been installed, the accessor returns `DEFAULT_SEASON_STATE`. Updates dispatch `RobotAction.UpdateSubsystemState(seasonState.copy(...))`; state is never mutated in place.

Subsystem sensor observations follow the same rule. For example, `FlywheelSubsystem.readSensors` periodically dispatches measured RPM, while intake stall detection dispatches an action from the facade to turn off an active intake. IO classes do not modify Redux state.

## Per-frame lifecycle

The effective `AresRobot.update()` order is:

1. `base.readSensors()`
   - clears REV Lynx bulk caches once for the frame;
   - calls drivetrain/platform input refresh;
   - invokes `HardwareRegistry.refreshAll()`, which refreshes registered season IO;
   - reads Pinpoint (or the IMU fallback), dispatches the pose observation, and updates vision.
2. `base.readAllSensors(timestamp)`
   - invokes every registered season subsystem's `readSensors` against the just-refreshed IO caches;
   - sensor observations may dispatch actions.
3. Season safety logic
   - an intake current above 8 A for more than 250 ms latches `stalled`;
   - if intake was active, the facade dispatches state that disables it.
4. `base.writeAllOutputs(base.powerManager.powerScale)`
   - writes every season subsystem from one immutable state snapshot;
   - uses the previously calculated power scale for this output pass.
5. `base.update(gamepads)`
   - does not repeat sensor reads in the same frame because the base tracks that sampling already occurred;
   - applies loop pacing and updates power/brownout state, drivetrain logic, EKF-related platform work, and telemetry.
6. Driver Station telemetry is refreshed.

The pre-read is deliberate. Reordering `readAllSensors` before `base.readSensors` consumes stale season caches. Moving hardware reads into a property getter causes duplicate bus transactions and defeats bulk caching. Writing outputs after a fatal base update can re-enable a motor that `safeHardware()` just stopped, which is why season outputs are written before entering the base update's internal failure boundary.

### Read and write rules

- A hardware IO `refresh()` samples sensors once and stores primitive cached fields.
- Getters only return cached fields.
- `Subsystem.readSensors` may derive state or dispatch observations but does not command actuators.
- `Subsystem.writeOutputs` reads state and commands actuators but does not sample hardware.
- Use indexed/preallocated data structures on 50–100 Hz paths; avoid arrays, geometry objects, reflection, iterators, and other allocations inside periodic code.
- Use `RobotClock.currentTimeMillis()`/`nanoTime()` instead of the system clock.

## HardwareRegistry and subsystem registry

The two registries have different jobs:

| Registration | Purpose |
|---|---|
| `HardwareRegistry.registerDevice(...)` | Batch hardware refresh, telemetry/topology, emergency `safeAll`, background-poll lifecycle, and close handling |
| `base.registerSubsystem(...)` | Calls season `readSensors`, `writeOutputs`, safe zero-scale output, and subsystem close |

Season motor IO currently registers itself with `HardwareRegistry` as `Intake` or `Flywheel`; `AresRobot` separately registers the matching subsystem. New output hardware must be reachable from the safety path. Its `safe()` must explicitly command the neutral/zero state, and `close()` must be idempotent and safe.

`AresRobot.close()` first zeros registered subsystem outputs, then closes subsystems, then closes the base in `finally`. `HardwareRegistry` remains the last-resort platform stop when an exception escapes normal lifecycle code.

## Safety and power behavior

`AresRobot.update()` catches any `Throwable` from season lifecycle work, invokes both `base.safeAll()` and `base.safeHardware()`, then rethrows. This is intentionally broader than a normal application exception boundary because motor safety must also cover linkage and other serious runtime failures.

The power manager supplies a scale in `[0, 1]`:

- Intake voltage is scaled by the brownout factor. Intake and flywheel outputs both fail closed if an invalid state requests them simultaneously.
- Flywheel target RPM is not reduced during ordinary brownout scaling because target speed determines shot behavior. A scale of exactly zero is the lifecycle emergency-stop signal and commands zero applied voltage.
- Prism brightness is reduced with the power scale.
- Indicator lights preserve their commanded state except for their own output behavior.

Do not scale position or velocity targets merely to enforce a power cap. Bound the actuator effort when the IO/control interface supports it. Always preserve the special zero-scale emergency-stop behavior.

## Autonomous failure handling

`AresAutoBase` consumes only the Kotlin generated from the repository-root `.ares` project. During
INIT it selects an enabled catalog entry, resolves alliance geometry once, validates every reachable
drive target and complete swept robot footprint, then seeds Redux and Pinpoint to the same pose.

START retains the accepted routine execution ID. Completion is recognized only from a matching
Redux terminal lifecycle record. FAILED/CANCELLED tasks report failure, cancel remaining work,
neutralize registered outputs, and invalidate `PoseStorage`; an executor becoming empty without a
matching terminal record also fails. A final pose/alliance is handed to TeleOp only after confirmed
COMPLETED status. The shared TeleOp lifecycle centrally restores a valid pose/alliance or defaults
to red.

## Coordinates and alliance handling

ARES uses the mathematical convention everywhere:

- +X and +Y are field axes in meters;
- heading is radians, CCW-positive;
- `0 rad` points +X and `π/2 rad` points +Y.

The Pinpoint boundary is responsible for converting its configured heading convention to CCW-positive. Do not add a second heading negation in the facade, telemetry, or dashboard.

For field-centric driving on blue, `AresDriveController` negates both processed translational joystick components before calling the core drive. Rotation retains the CCW-positive sign. Robot-centric input is not alliance-mirrored.

## Hardware configuration

`config/HardwareConstants.kt` is the canonical source for required base names.

| Logical device | Robot configuration name(s) |
|---|---|
| Drive motors | `fl`, `fr`, `rl`, `rr` |
| Pinpoint | `pinpoint` |
| IMU | `imu` |
| Limelight | `limelight` |
| Intake | `intake` |
| Flywheel | `shooter` |
| Primary indicator | `indicator` |
| Secondary indicator | `indicator2` |
| Prism RGB | `prism`; initialized as I2C address `0x38` before same-name PWM fallback |

Right-side drive motors (`fr`, `rr`) are reversed in the team facade. Rear motors are named `rl`
and `rr` in production and diagnostics; alternate rear-motor aliases are not supported.

Flywheel conversion defaults to 28 encoder ticks/revolution and 6000 RPM maximum. Change those physical constants only after confirming the installed motor/encoder, then verify closed-loop and fallback open-loop behavior.

## Telemetry and networking

The robot publishes through ARESLib. NT4 topic names are canonicalized without a leading slash.
Physical remote drive consumes only `ARES/Input/driveFrame` v2 as an exact `double[8]`, requires a
neutral-first session handshake, honors bit 4 as field-relative (`1`) versus robot-relative (`0`),
and expires on a 200 ms receiver-time lease. Scalar axes, heartbeat, and v1 frames are not
supported.

The robot is offline-first. It serves logs locally for the desktop analytics app to pull. Cloud sync belongs on the laptop; do not add cloud credentials or upload calls to TeamCode.
