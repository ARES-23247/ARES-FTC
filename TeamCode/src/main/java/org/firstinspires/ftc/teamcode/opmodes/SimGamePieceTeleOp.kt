package org.firstinspires.ftc.teamcode.opmodes

import com.qualcomm.robotcore.eventloop.opmode.TeleOp
import com.qualcomm.robotcore.hardware.DcMotor
import com.qualcomm.robotcore.hardware.DcMotorEx
import org.firstinspires.ftc.teamcode.dsl.AresTeleOpBase

/**
 * A simulator-only OpMode for testing Game Pieces (Balls) with the
 * Intake and Shooter simulated mechanics.
 *
 * Uses the standard AresTeleOpBase DSL for consistent lifecycle management,
 * while adding team-specific intake/shooter hardware via hardwareMap.
 */
@TeleOp(name = "Simulator: Intake & Shoot TeleOp", group = "ARES Sim")
class SimGamePieceTeleOp : AresTeleOpBase() {

    private var intake: DcMotor? = null
    private var shooter: DcMotorEx? = null
    private var intakeActive = false
    private var shooterActive = false
    private var lastLeftBumper = false
    private var lastRightBumper = false

    override fun define() = aresTeleOp {

        onInit { _, telemetry ->
            // Initialize team-specific hardware outside the core FtcMecanumRobot
            try {
                intake = hardwareMap.get(DcMotor::class.java, "intake")
                shooter = hardwareMap.get(DcMotorEx::class.java, "shooter")
                shooter?.mode = DcMotor.RunMode.RUN_USING_ENCODER
            } catch (e: Exception) {
                println("[SimGamePieceTeleOp] Optional intake/shooter missing. Drivebase-only mode.")
            }

            telemetry.addData("Status", "Simulator OpMode Ready!")
            telemetry.addData("Notice", "This OpMode is for Desktop Simulation only.")
        }

        onLoop { robot, driver, telemetry ->
            // --- Toggle intake on rising edge of left bumper ---
            val leftBumper = driver.leftBumper.isPressed
            if (leftBumper && !lastLeftBumper) intakeActive = !intakeActive
            lastLeftBumper = leftBumper

            // --- Toggle shooter on rising edge of right bumper ---
            val rightBumper = driver.rightBumper.isPressed
            if (rightBumper && !lastRightBumper) shooterActive = !shooterActive
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

            // 2. Intake Control
            intake?.power = if (intakeActive) 1.0 else 0.0
            telemetry.addData("Intake", if (intakeActive) "ACTIVE" else "INACTIVE")

            // 3. Shooter Control
            if (shooterActive) {
                try { shooter?.velocity = 2000.0 } catch (_: Exception) { shooter?.power = 1.0 }
                telemetry.addData("Shooter", "ACTIVE (2000 RPM)")
            } else {
                try { shooter?.velocity = 0.0 } catch (_: Exception) { shooter?.power = 0.0 }
                telemetry.addData("Shooter", "INACTIVE")
            }

            // 4. Feed mechanism (handled by sim InteractionModel on trigger press)
            if (driver.rightTrigger.value > 0.5) {
                telemetry.addData("Feed", "PUSHING BALL")
            }

            telemetry.addData("Inventory", "Check the ARES-Analytics Sim UI")
        }
    }
}
