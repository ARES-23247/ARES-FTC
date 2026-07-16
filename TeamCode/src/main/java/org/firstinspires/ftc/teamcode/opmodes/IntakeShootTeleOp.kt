package org.firstinspires.ftc.teamcode.opmodes

import com.qualcomm.robotcore.eventloop.opmode.TeleOp
import org.firstinspires.ftc.teamcode.dsl.AresTeleOpBase
import org.firstinspires.ftc.teamcode.dsl.*

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

        onInit { robot, telemetry ->
            telemetry.addData("Status", "Intake & Shoot TeleOp Ready!")
            telemetry.addData("Controls", "LB=Intake, RB=Flywheel, RT=Shoot")
            
            // Auto-initialize pose with alliance starting orientation so field-centric is correct on start
            robot.resetPoseForAlliance()
        }

        onConfigure { robot, driver ->
            // --- Toggle intake on rising edge of left bumper ---
            driver.leftBumper.onPress("Toggle Intake") {
                val currentIntake = robot.base.store.state.superstructure.intakeActive
                robot.base.store.dispatch(com.areslib.action.RobotAction.UpdateSubsystemState(
                    IntakeState(intakeActive = !currentIntake)
                ))
            }

            // --- Toggle shooter on rising edge of right bumper ---
            driver.rightBumper.onPress("Toggle Shooter") {
                val currentFlywheel = robot.base.store.state.superstructure.flywheelActive
                val currentTarget = if (!currentFlywheel) 2000.0 else 0.0
                robot.base.store.dispatch(com.areslib.action.RobotAction.UpdateSubsystemState(
                    FlywheelState(
                        flywheelActive = !currentFlywheel,
                        flywheelTargetRPM = currentTarget,
                        currentRPM = robot.base.store.state.superstructure.currentRPM
                    )
                ))
            }
        }

        onLoop { robot, driver, telemetry ->
            // 1. Drivetrain Control (Standard Field Centric)
            robot.base.mecanumDrive.driveWithGamepad(driver, useHeadingLock = false)

            // 2. Read state from store for telemetry print
            val state = robot.base.store.state
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
