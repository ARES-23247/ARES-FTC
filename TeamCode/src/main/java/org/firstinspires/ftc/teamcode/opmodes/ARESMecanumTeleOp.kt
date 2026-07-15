package org.firstinspires.ftc.teamcode.opmodes

import com.qualcomm.robotcore.eventloop.opmode.TeleOp
import org.firstinspires.ftc.teamcode.dsl.AresTeleOpBase

/**
 * A highly optimized, modern FTC TeleOp demonstrating the new beginner-friendly DSL layout.
 * Optimized specifically for a 4-motor mecanum drivetrain, with a GoBilda Pinpoint 
 * connected to native I2C 1, and a GoBilda Floodgate connected to native Analog Port 1.
 */
@TeleOp(name = "Direct Mecanum Drivetrain", group = "ARES")
class ARESMecanumTeleOp : AresTeleOpBase() {

    override fun define() = aresTeleOp {
        
        onConfigure { robot, driver ->
            driver.y.onPress("Reset Field Centric Pose") {
                val alliance = robot.base.store.state.drive.alliance
                val initialHeading = if (alliance == com.areslib.state.Alliance.RED) Math.PI / 2.0 else -Math.PI / 2.0
                robot.base.resetPose(com.areslib.math.geometry.Pose2d(0.0, 0.0, com.areslib.math.geometry.Rotation2d(initialHeading)))
            }
        }

        onInit { robot, telemetry ->
            robot.base.store.dispatch(com.areslib.action.RobotAction.SetAlliance(com.areslib.state.Alliance.RED))
            
            // Auto-initialize pose with alliance starting orientation so field-centric is correct on start
            val alliance = robot.base.store.state.drive.alliance
            val initialHeading = if (alliance == com.areslib.state.Alliance.RED) Math.PI / 2.0 else -Math.PI / 2.0
            robot.base.resetPose(com.areslib.math.geometry.Pose2d(0.0, 0.0, com.areslib.math.geometry.Rotation2d(initialHeading)))

            robot.base.mecanumIO.slewRateLimit = 4.0 // Ramp up to full speed in 0.25 seconds

            telemetry.addData("Alliance", "RED")
            telemetry.addData("EKF Pose (X, Y, Deg)", String.format("(%.2f, %.2f) %.1f°",
                robot.base.store.state.drive.poseEstimator.estimatedPose.x,
                robot.base.store.state.drive.poseEstimator.estimatedPose.y,
                Math.toDegrees(robot.base.store.state.drive.poseEstimator.estimatedPose.heading.radians)
            ))
        }
        
        onLoop { robot, driver, telemetry ->

            val joystickForward = -driver.leftStickY.value.toDouble()
            val joystickLeft = -driver.leftStickX.value.toDouble()
            val rotate = -driver.rightStickX.value.toDouble()
            // 2. Drive the robot (Red Alliance Perspective)
            // - Pushing forward (+joystickForward) moves +Y (away from red driver)
            // - Pushing left (+joystickLeft) moves +X (to red driver's left -> wait, driver left is -X on field)
            // So: vy = joystickForward (forward = +Y)
            //     vx = -joystickLeft (left = -X)
            robot.base.mecanumDrive.fieldRelativeDrive(
                vx = -joystickLeft, 
                vy = joystickForward, 
                omega = rotate,
                useHeadingLock = true
            )
        }
    }
}
