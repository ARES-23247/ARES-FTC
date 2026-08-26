# Hand-authored mechanisms in Robot Studio

The GoBilda robot keeps its mature intake and flywheel Kotlin implementations. Their
`.aressubsystem` documents register those implementations with Robot Studio; they do not generate,
rewrite, or take ownership of the Kotlin files.

This is the preferred migration path for an existing robot. Students can discover the mechanism,
hardware-map name, state, controller, safety rules, simulation role, and autonomous actions in the
GUI while the reviewed season code remains `USER_OWNED`.

## Runtime flow

Both mechanisms follow the same ARES boundary:

`driver or autonomous action -> Redux action/reducer -> immutable season state -> subsystem controller -> IO contract -> FTC or simulated hardware adapter`

The reducer never touches hardware. The subsystem reads cached observations, decides whether the
requested output is safe, and commands the IO contract. The FTC adapter is the only owner of SDK
motor calls.

## Intake roller

Robot Studio shows the hardware-map name `intake`. The adapter reads velocity and current once per
loop and caches both observations. The controller applies the configured nominal voltage only when:

- Redux requests collection;
- the flywheel is stopped;
- the global power budget permits output;
- current feedback is valid; and
- no jam is latched.

Fresh current above 8 A for 250 ms latches the jam fault. Invalid current while collecting also
fails closed after a 100 ms grace interval. Recovery requires the intake to be disabled and valid
current to remain below 6 A for 100 ms. A separate velocity-stall indication is diagnostic only
until the team calibrates it using physical-robot logs.

The named actions `intake.collect` and `intake.stop` let Autonomous Planner and controller bindings
use the same reviewed behavior without embedding Kotlin in a routine.

## Shooter flywheel

Robot Studio shows the hardware-map name `shooter`. The adapter caches motor RPM and current once per
loop. At full effort it uses the REV hub's encoder velocity controller. During power limiting it
preserves the requested scoring RPM and reduces only the allowed effort, using cached velocity
feedback plus feedforward. It never silently falls back to open-loop control when feedback is bad.

Invalid velocity while spinning latches a feedback fault after 150 ms. Recovery requires the
flywheel to be disabled, a valid feedback sample, and an explicit successful neutral write. The
intake and flywheel are mutually interlocked at both the intent/controller layer and the final
subsystem output boundary.

The named actions `flywheel.prepare` and `flywheel.stop` expose that behavior to Autonomous Planner
and controller bindings.

## What students may safely change

Start with the GUI-authored demo robot when learning or designing a new mechanism. For this existing
team robot, treat these hand-authored source files as reviewed examples:

- `TeamCode/src/main/java/org/firstinspires/ftc/teamcode/subsystems/Subsystems.kt`
- `TeamCode/src/main/java/org/firstinspires/ftc/teamcode/hardware/FtcIntakeIO.kt`
- `TeamCode/src/main/java/org/firstinspires/ftc/teamcode/hardware/FtcFlywheelIO.kt`

Robot Studio may edit the canonical descriptor only after showing a structured diff. It must never
silently overwrite those Kotlin files. A changed device name or safety contract also invalidates the
previous hardware review and requires a new review before physical deployment.

Simulation verifies software behavior, not wiring, motor direction, safe physical limits, or whether
the mechanism is safe to run on a robot. Complete the Port Map & Review checklist with the robot
disabled before physical testing.
