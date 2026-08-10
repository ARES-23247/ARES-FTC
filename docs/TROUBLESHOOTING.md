# Troubleshooting

Start with the safest test that can distinguish the failure. Keep the robot on blocks for drivetrain diagnosis and be ready to stop the OpMode whenever outputs are enabled.

## Build failures

### Gradle reports an unsupported Java version

Android builds require a JDK 17-compatible Gradle JVM. In Android Studio, select JDK 17 for Gradle. The standalone simulator intentionally requests JDK 21, so ensure a matching toolchain can be discovered.

Check the active JVM:

```powershell
.\gradlew.bat --version
```

### ARESLib classes are unresolved or stale

Confirm `../ARESLib-Kotlin` exists. `settings.gradle` should announce/use the included build and substitute local projects. If testing the wider workspace, publish the library and rebuild without relying on an old Maven cache:

```powershell
Push-Location ..\ARESLib-Kotlin
.\gradlew.bat publishToMavenLocal
Pop-Location
.\gradlew.bat :TeamCode:assembleDebug
```

### Android build works but simulator compilation fails

The simulator uses JDK 21 and compiles TeamCode against FTC mocks. Look for an unmocked Android/FTC API or a dependency available only on the Android classpath. Shared season logic should depend on the IO interfaces, not directly on an Android-only object.

## Robot initialization

### Drive motor is missing

Production names are exactly `fl`, `fr`, `rl`, and `rr`. The rear names are not `bl`/`br`. Run `ARES Drivetrain Diagnostic`; it reports each discovered motor and can power one motor at 0.4 while its button is held. Its `bl`/`br` lookup is diagnostic compatibility only—fix the Robot Controller configuration rather than depending on the alias.

### Intake or shooter reports “failed to load”

The intake must be `intake`; the flywheel must be `shooter`. They are optional in `AresRobot`, so their initialization exception is reported and drive initialization continues. Check the configured device type and hub port as well as its name.

### Pinpoint, IMU, or Limelight data is absent

Check `pinpoint`, `imu`, and `limelight` configuration names. Pinpoint is the primary pose input. The IMU fallback supplies heading but cannot recover field X/Y translation, so a robot apparently rotating correctly while pose remains at zero usually indicates unavailable odometry.

For Limelight target-space alignment, robot yaw comes from the negative target-space Y rotation; target-space Z rotation is tilt, not heading. Do not add an extra negation to field heading after the Pinpoint boundary.

### Optional lights do not appear

See the alias table in [ARCHITECTURE.md](ARCHITECTURE.md#hardware-configuration). The facade enumerates configured devices at startup. Prism tries I2C first and PWM second under its supported aliases. An absent optional light should produce telemetry but should not stop drive or autonomous; named lighting tasks no-op when their IO is absent.

## Driving and localization

### Robot drives diagonally or rotates during translation

Verify each motor individually before changing signs in software. The expected facade directions are forward for `fl`/`rl` and reverse for `fr`/`rr`. A wheel installed or wired differently should be reconciled deliberately; do not compensate by changing coordinate or odometry signs blindly.

### Field-centric direction is wrong only on blue

Both translational axes must be mirrored for blue. The team drive controller already performs that transformation. Confirm the Redux alliance value, then ensure the OpMode uses `driveWithGamepad`/`driveFieldCentric` rather than calling a lower-level field drive with a second mirror.

### Heading is reversed or 90 degrees off

ARES uses CCW-positive radians with zero on +X. Check the Pinpoint heading-direction configuration first. The Pinpoint IO boundary performs the convention conversion; remove any downstream extra negation. A 90-degree error in the desktop field icon may be a field-to-canvas rendering offset rather than robot pose, so compare numeric telemetry before changing robot math.

### Pose jumps at Auto-to-TeleOp transition

Autonomous saves the final pose and alliance through `PoseStorage`; the main TeleOp restores them when `hasValidPose` is true. Confirm autonomous reached its normal persistence path and that the Robot Controller was not restarted between OpModes. A new process cannot retain in-memory pose.

## Autonomous

### “Failed to load dynamic path”

Confirm:

- the class's `pathName` matches `TeamCode/src/main/assets/pathplanner/autos/<name>.auto` exactly;
- every path referenced by the auto exists under `pathplanner/paths`;
- the files were packaged or pushed after their last edit;
- JSON is valid for the parser version in ARESLib.

The autonomous base intentionally waits for start, displays the error, performs a full safety stop, and exits without running a partial sequence.

### Auto starts from the wrong side

Call `configureAlliance(robot, Alliance.RED/BLUE)` before execution. It dispatches the alliance and resets the corresponding pose before ARESLib mirrors the path. Also confirm the physical robot is placed at the path's declared starting pose.

### Auto stops after one loop error

This is fail-safe behavior. A loop exception clears the task executor and stops all registered season/drivetrain outputs; it does not retry stale targets. Read the `LOOP_ERROR` telemetry/log and fix the underlying missing hardware, invalid path, or controller exception.

## Mechanisms and safety

### Intake stops by itself

The intake stall detector requires valid current above 8 A continuously for more than 250 ms, then dispatches `intakeActive = false`. Check for a physical jam and drivetrain voltage before adjusting thresholds. An invalid current sample clears rather than latches stale overcurrent data.

### Flywheel target does not decrease during brownout scaling

That is intentional. Shot velocity depends on target RPM, so ordinary scaling does not alter the velocity setpoint. A zero power scale is reserved as the emergency-stop signal and commands zero RPM. Diagnose bus voltage and load rather than interpreting the unchanged target as an ignored safety state.

### A mechanism continues after stop or crash

Treat this as a critical lifecycle defect. Confirm its IO:

1. has an explicit neutral implementation in `safe()` and `close()`;
2. registers with `HardwareRegistry` during initialization;
3. is wrapped by a subsystem registered with `base.registerSubsystem`;
4. treats `writeOutputs(..., scale = 0.0)` as a stop;
5. does not write outputs asynchronously after close.

Do not resume operation until the restrained-hardware stop test passes.

## Networking and logs

### Dashboard shows duplicate or missing topics

Use NT4 keys without a leading slash, for example `ARES/Input/heartbeat`. Publishing `/ARES/Input/heartbeat` from custom code creates an inconsistent contract even though current server/client boundaries normalize canonical keys.

### Remote drive stops despite an apparent connection

Remote drive is heartbeat-gated. Verify the dashboard is updating `ARES/Input/heartbeat`, that the laptop and Robot Controller are on the same network, and that port `5810` is reachable. A stale heartbeat intentionally commands zero field-relative velocity.

### Logs are not in the cloud

The robot never uploads directly. Connect the ARES Analytics desktop application to the robot's local log service on port `5002`, import the files to the laptop, then sync from the laptop. Internet availability should not affect robot operation or local logging.

## Diagnostic OpModes

- `AAA Blank Null OpMode`: hardware-free control-system isolation; useful for distinguishing app/lifecycle issues from I2C or power trouble.
- `ARES Drivetrain Diagnostic`: discovers and individually powers drive motors; use on blocks.
- `ARES Live Tuning TeleOp`: exercises live tuning; do not use as the first test after a hardware/sign change.
- `ARES Remote Drive (NT4)`: tests dashboard input and heartbeat fail-safe.

When a problem remains ambiguous, capture the exact OpMode, hardware-map names, Driver Station error, local log, last valid sensor values, and whether the same behavior occurs in simulation.
