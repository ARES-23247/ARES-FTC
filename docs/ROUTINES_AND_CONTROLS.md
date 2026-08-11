# FTC routines, autonomous selection, and controls

The FTC robot compiles the same project-authored routines and control schemes used by ARES
Analytics. Authors work on the laptop; neither the Control Hub nor the Driver Station needs to be
online while a routine or mapping is created.

## Source of truth

Canonical inputs live at the repository root:

```text
.ares/project.json
.ares/action-catalog.json
.ares/autonomous-catalog.json
.ares/routines/<id>.aresroutine
.ares/controllers/<id>.arescontroller
.ares/controls/<id>.arescontrols
```

`project.json` is the shared source of truth for FTC coordinates, field dimensions, and the
robot's footprint. Analytics uses it for placement limits and TeamCode uses the same generated
values for autonomous preflight; neither side guesses dimensions from the current UI profile.

A routine contains behavior and drive goals but no controller button or mandatory starting pose.
The autonomous catalog adds selectable match entry points and starting poses; a control scheme can
invoke the same routine as a teleop macro. This is the supported replacement for creating a path
file and a separate auto file for one behavior.

The action catalog is automatically loaded by Analytics. Each key must have a matching typed
implementation in the FTC generated-project capabilities. Keep the catalog, runtime factory, and
their contract test synchronized when adding an action.

## Generate and verify Kotlin

After saving in Analytics, run:

```powershell
# Update TeamCode's checked-in generated source
.\gradlew.bat :TeamCode:generateAresProject

# Confirm the checked-in source exactly matches the project files
.\gradlew.bat :TeamCode:verifyAresProject

# Compile the APK and run TeamCode tests
.\gradlew.bat :TeamCode:testDebugUnitTest :TeamCode:assembleDebug
```

The generated file is
`TeamCode/src/main/java/org/firstinspires/ftc/teamcode/generated/GeneratedAresProject.kt`. Do not
edit it directly. Every Kotlin compile depends on `verifyAresProject`, so a stale GUI export fails
the build instead of silently deploying older behavior. Generation runs on the development machine
and is not an ADB or network operation.

Commit the `.ares` documents and generated Kotlin together.

## Autonomous on the Driver Station

`AresAutoBase` builds its choices from the generated autonomous catalog during INIT:

- D-pad left/right selects an enabled entry;
- X toggles Red/Blue unless an OpMode locks the alliance;
- telemetry shows the display name, routine ID, alliance, and READY/BLOCKED state;
- START seeds localization from the alliance-adjusted catalog pose and starts the routine;
- a missing entry, validation error, hardware error, timeout, exception, or stop cancels the task
  tree and neutralizes registered outputs.

FTC autonomous has a 29.5-second default software deadline and never exceeds the 30-second match
limit. Photon is explicitly enabled by the shared auto and teleop base classes.

Routines are authored in the repository's canonical field coordinate convention: meters,
CCW-positive radians, `0 = +X`. Alliance transformation happens once at the FTC runtime boundary;
do not add a second mirror in a routine or drivetrain controller.

## Teleop control schemes

An `AresTeleOpBase` may name a generated control scheme. The FTC binding host maps logical slots as:

| Scheme slot | FTC device |
| --- | --- |
| `driver`, `gamepad1`, `controller1`, or `0` | `gamepad1` |
| `operator`, `gamepad2`, `controller2`, or `1` | `gamepad2` |

Any other slot fails during initialization. Per-loop input is copied through
`FtcInputFrameAdapter`, then the shared allocation-conscious binding runtime applies debounce,
hold/repeat/cooldown, chords, and analog policies.

The standard FTC SDK buttons and axes are supported. The Flydigi Vader 5 Pro can be drawn and
configured in Analytics, but its vendor-only extra buttons are usable on the Control Hub only when
the FTC Android/gamepad event path exposes them. Desktop GLFW learning does not establish the FTC
raw index. Learn and verify the `FTC` mapping explicitly; retain a standard-control fallback for
match-critical behavior.

Macros do not need a separate implementation: make a reusable routine and bind the desired event
to it. Bindings can start, restart, queue, run in parallel, toggle/cancel, or explicitly cancel a
routine.

## Legacy assets

PathPlanner assets and `.aresauto` import remain available for migration and focused compatibility
tests. New ARES autonomous work should use `.ares/routines` and the autonomous catalog. The APK
contains generated Kotlin, so canonical routine deployment does not depend on pushing loose path or
auto files to the Control Hub.

## Pre-match checklist

1. Open the FTC repository root in Analytics and confirm the action catalog is populated.
2. Save routines/controls and run `:TeamCode:generateAresProject`.
3. Run `:TeamCode:verifyAresProject` and `:TeamCode:testDebugUnitTest`.
4. Exercise the selected auto in the desktop simulator for both alliances.
5. Verify controller mappings on the actual Driver Station/Control Hub path, especially vendor
   extras and analog trigger thresholds.
6. On restrained hardware, confirm INIT selection, starting pose, cancellation, and safe outputs.
