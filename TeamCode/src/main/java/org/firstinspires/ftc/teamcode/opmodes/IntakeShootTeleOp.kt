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

    override fun define() = aresTeleOp {

        onInit { robot, telemetry ->
            robot.addTelemetry("Status", "Intake & Shoot TeleOp Ready!")
            robot.addTelemetry("Controls", "LB=Intake, RB=Flywheel, RT=Shoot")
        }

        onConfigure { robot, driver ->
            driver.leftBumper.onPress("Toggle Intake") {
                robot.toggleIntake()
            }

            // --- Toggle shooter on rising edge of right bumper ---
            driver.rightBumper.onPress("Toggle Shooter") {
                robot.toggleShooter()
            }

            // --- Cycle indicator light color with dpad ---
            /**
             * Documentation for indicatorColors
             */
            val indicatorColors = com.areslib.hardware.actuator.IndicatorLightColor.entries
            /**
             * Documentation for indicatorIndex
             */
            var indicatorIndex = 0

            driver.dpadUp.onPress("Indicator Next Color") {
                indicatorIndex = (indicatorIndex + 1) % indicatorColors.size
                robot.setIndicatorColor(indicatorColors[indicatorIndex])
            }
            driver.dpadDown.onPress("Indicator Prev Color") {
                indicatorIndex = (indicatorIndex - 1 + indicatorColors.size) % indicatorColors.size
                robot.setIndicatorColor(indicatorColors[indicatorIndex])
            }
        }

        onLoop { robot, driver, telemetry ->
            // 1. Drivetrain Control (Standard Field Centric)
            robot.base.mecanumDrive.driveWithGamepad(driver, useHeadingLock = false)

            // 2. Read state from store for telemetry print
            /**
             * Documentation for state
             */
            val state = robot.base.store.state
            robot.addTelemetry("Intake", if (state.superstructure.season.intakeActive) "ACTIVE" else "INACTIVE")
            robot.addTelemetry("Shooter", if (state.superstructure.season.flywheelActive) "ACTIVE" else "INACTIVE")

            // 3. Feed mechanism (handled by sim InteractionModel on trigger press)
            if (driver.rightTrigger.value > state.tuning.driverTriggerThreshold) {
                robot.addTelemetry("Feed", "PUSHING BALL")
            }

            robot.addTelemetry("Inventory", "Check the ARES-Analytics Sim UI")
        }
    }
}
