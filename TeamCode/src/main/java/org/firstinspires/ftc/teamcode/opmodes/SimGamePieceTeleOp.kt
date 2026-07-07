package org.firstinspires.ftc.teamcode.opmodes

import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode
import com.qualcomm.robotcore.eventloop.opmode.TeleOp
import com.areslib.telemetry.AresGamepad
import com.areslib.ftc.toState

/**
 * A simulator-only OpMode for testing Game Pieces (Balls) with the newly added
 * Intake and Shooter simulated mechanics. 
 * This uses AresRobot which wraps FtcMecanumRobot and adds the team-specific mechanisms.
 */
@TeleOp(name = "Simulator: Intake & Shoot TeleOp", group = "ARES Sim")
class SimGamePieceTeleOp : LinearOpMode() {
    override fun runOpMode() {
        val robot = AresRobot(hardwareMap, telemetry)
        val driver = AresGamepad()
        driver.leftStick.label("Field-centric Translation (X/Y)")
        driver.rightStickX.label("Robot Rotation")
        driver.leftBumper.label("Intake")
        driver.rightBumper.label("Spin Up Shooter")
        driver.rightTrigger.label("Feed / Shoot")
        
        telemetry.addData("Status", "Simulator OpMode Ready!")
        telemetry.addData("Notice", "This OpMode is for Desktop Simulation only.")
        telemetry.update()
        
        var intakeActive = false
        var shooterActive = false
        
        driver.leftBumper.onPress("Toggle Intake") {
            intakeActive = !intakeActive
        }
        
        driver.rightBumper.onPress("Toggle Shooter") {
            shooterActive = !shooterActive
        }
        
        waitForStart()
        
        try {
            while (opModeIsActive()) {
                // Update sensors, EKF, and apply previous frame's motor commands first
                robot.update()

                val g1State = gamepad1.toState()
                driver.update(g1State)
                
                // 1. Drivetrain Control (Standard Field Centric)
                val joystickForward = -driver.leftStickY.value.toDouble()
                val joystickLeft = -driver.leftStickX.value.toDouble()
                val rotate = -driver.rightStickX.value.toDouble()
                
                robot.driveFieldCentric(
                    x = -joystickLeft, 
                    y = joystickForward, 
                    rotation = rotate
                )
                
                // 2. Intake Control (Left Bumper / Q key)
                if (intakeActive) {
                    robot.intake(1.0)
                    telemetry.addData("Intake", "ACTIVE")
                } else {
                    robot.intake(0.0)
                    telemetry.addData("Intake", "INACTIVE")
                }
                
                // 3. Shooter Control (Right Bumper / E key)
                // Simulated shooter requires velocity to be > 1000 to shoot
                if (shooterActive) {
                    robot.shoot(2000.0)
                    telemetry.addData("Shooter", "ACTIVE (2000 RPM)")
                } else {
                    robot.shoot(0.0)
                    telemetry.addData("Shooter", "INACTIVE")
                }
                
                // Note: The physical feed mechanism (Right Trigger / Shift) is handled automatically 
                // by the Simulator's InteractionModel when it detects the Transfer button press.
                if (driver.rightTrigger.value > 0.5) {
                    telemetry.addData("Feed", "PUSHING BALL")
                }
                
                telemetry.addData("Inventory", "Check the ARES-Analytics Sim UI")
                telemetry.update()
            }
        } finally {
            robot.close()
        }
    }
}
