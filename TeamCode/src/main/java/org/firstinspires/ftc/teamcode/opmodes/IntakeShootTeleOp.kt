package org.firstinspires.ftc.teamcode.opmodes

import com.qualcomm.robotcore.eventloop.opmode.TeleOp
import org.firstinspires.ftc.teamcode.dsl.AresTeleOpBase
import org.firstinspires.ftc.teamcode.dsl.*

/**
 * Driver mode for field-centric driving plus the optional intake and flywheel.
 *
 * Button callbacks dispatch immutable superstructure state through the facade. Right trigger is
 * telemetry-only because no feed actuator is wired; it must never be interpreted as a motor command.
 */
@TeleOp(name = "Intake & Shoot TeleOp", group = "ARES")
class IntakeShootTeleOp : AresTeleOpBase() {
    private var prevTriggerState = false

    override fun define() = aresTeleOp {

        onInit { robot, _ ->
            robot.addTelemetry("Status", "Intake & Shoot TeleOp Ready!")
            robot.addTelemetry("Controls", "LB=Intake, RB=Flywheel, RT=(feed not wired)")
        }

        onConfigure { robot, driver ->
            driver.leftBumper.onPress("Toggle Intake") {
                robot.toggleIntake()
            }

            driver.rightBumper.onPress("Toggle Shooter") {
                robot.toggleShooter()
            }

            // Optional primary indicator color selection.
            val indicatorColors = com.areslib.hardware.actuator.IndicatorLightColor.entries
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

        onLoop { robot, driver, _ ->
            robot.driveWithGamepad(driver, useHeadingLock = false)

            // Read one immutable snapshot so telemetry fields describe the same reducer state.
            val state = robot.base.store.state
            robot.addTelemetry("Intake", if (state.superstructure.season.intakeActive) "ACTIVE" else "INACTIVE")
            robot.addTelemetry("Shooter", if (state.superstructure.season.flywheelActive) "ACTIVE" else "INACTIVE")

            // Feed is not wired; report only the rising edge to avoid repetitive telemetry churn.
            val currentTriggerState = driver.rightTrigger.value > state.tuning.driverTriggerThreshold
            if (currentTriggerState && !prevTriggerState) {
                robot.addTelemetry("Feed", "RT held (feed not wired)")
            }
            prevTriggerState = currentTriggerState

            robot.addTelemetry("Inventory", "Check the ARES-Analytics Sim UI")
        }
    }
}
