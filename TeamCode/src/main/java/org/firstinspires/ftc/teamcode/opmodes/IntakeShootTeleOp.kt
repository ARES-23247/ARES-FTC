package org.firstinspires.ftc.teamcode.opmodes

import com.qualcomm.robotcore.eventloop.opmode.TeleOp
import org.firstinspires.ftc.teamcode.dsl.AresTeleOpBase

/**
 * Full-featured TeleOp with field-centric driving, intake toggle,
 * and shooter toggle. Works on both real hardware and the desktop simulator.
 *
 * Uses the standard AresTeleOpBase DSL for consistent lifecycle management,
 * while adding team-specific intake/shooter hardware via hardwareMap.
 */
@TeleOp(name = "Intake & Shoot TeleOp", group = "ARES")
class IntakeShootTeleOp : AresTeleOpBase() {

    private var lastLeftBumper = false
    private var lastRightBumper = false

    override fun define() = aresTeleOp {

        onInit { _, telemetry ->
            telemetry.addData("Status", "Intake & Shoot TeleOp Ready!")
            telemetry.addData("Controls", "LB=Intake, RB=Flywheel, RT=Shoot")
        }

        onLoop { robot, driver, telemetry ->
            val timestamp = com.areslib.util.RobotClock.currentTimeMillis()

            // --- Toggle intake on rising edge of left bumper ---
            val leftBumper = driver.leftBumper.isPressed
            if (leftBumper && !lastLeftBumper) {
                val currentIntake = robot.store.state.superstructure.intakeActive
                robot.store.dispatch(com.areslib.action.RobotAction.SetIntakeActive(!currentIntake, timestamp))
            }
            lastLeftBumper = leftBumper

            // --- Toggle shooter on rising edge of right bumper ---
            val rightBumper = driver.rightBumper.isPressed
            if (rightBumper && !lastRightBumper) {
                val currentFlywheel = robot.store.state.superstructure.flywheelActive
                robot.store.dispatch(com.areslib.action.RobotAction.SetFlywheelTargetRPM(2000.0, timestamp))
                robot.store.dispatch(com.areslib.action.RobotAction.SetFlywheelActive(!currentFlywheel, timestamp))
            }
            lastRightBumper = rightBumper

            // 1. Drivetrain Control (Standard Field Centric)
            val joystickForward = -driver.leftStickY.value.toDouble()
            val joystickLeft = -driver.leftStickX.value.toDouble()
            val rotate = -driver.rightStickX.value.toDouble()

            robot.driveFieldCentric(
                x = -joystickLeft,
                y = joystickForward,
                rotation = rotate
            )

            // 2. Read state from store for telemetry print
            val state = robot.store.state
            telemetry.addData("Intake", if (state.superstructure.intakeActive) "ACTIVE" else "INACTIVE")
            telemetry.addData("Shooter", if (state.superstructure.flywheelActive) "ACTIVE (2000 RPM)" else "INACTIVE")

            // 3. Feed mechanism (handled by sim InteractionModel on trigger press)
            if (driver.rightTrigger.value > 0.5) {
                telemetry.addData("Feed", "PUSHING BALL")
            }

            telemetry.addData("Inventory", "Check the ARES-Analytics Sim UI")
        }
    }
}
