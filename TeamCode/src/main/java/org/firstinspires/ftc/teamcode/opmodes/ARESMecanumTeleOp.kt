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
        
        onInit { robot, telemetry ->
            robot.mecanumIO.slewRateLimit = 4.0 // Ramp up to full speed in 0.25 seconds

            telemetry.addData("Status", "Ready! Aligning via Limelight...")
            telemetry.addData("Alliance", "RED")
            telemetry.addData("EKF Pose (X, Y, Deg)", String.format("(%.2f, %.2f) %.1f°",
                robot.store.state.drive.poseEstimator.estimatedPose.x,
                robot.store.state.drive.poseEstimator.estimatedPose.y,
                Math.toDegrees(robot.store.state.drive.poseEstimator.estimatedPose.heading.radians)
            ))
            telemetry.addData("Vision Status", robot.visionTracker.lastVisionStatus)
        }
        
        onLoop { robot, driver, telemetry ->
            
            // 1. Check if AprilTag ID 1 is currently visible (for telemetry purposes)
            val now = com.areslib.util.RobotClock.currentTimeMillis()
            var tag1MeasurementTele: com.areslib.state.VisionMeasurement? = null
            val measurements = robot.store.state.vision.measurements
            for (i in 0 until measurements.size) {
                val it = measurements[i]
                if (it.tagId == 1 && (now - it.timestampMs) < 1000L) {
                    tag1MeasurementTele = it
                    break
                }
            }

            if (tag1MeasurementTele != null) {
                val targetSpace = tag1MeasurementTele.robotPoseTargetSpace
                val ageMs = now - tag1MeasurementTele.timestampMs
                telemetry.addData("Tag 1 Tracking", "VISIBLE")
                telemetry.addData("Tag 1 Age Ms", ageMs)
                telemetry.addData("Tag 1 Raw Z", targetSpace.z)
                telemetry.addData("Tag 1 Raw X", targetSpace.x)
                telemetry.addData("Tag 1 Raw Yaw Rad", targetSpace.rotation.y)
            } else {
                telemetry.addData("Tag 1 Tracking", "NOT VISIBLE")
            }

            val joystickForward = -driver.leftStickY.value.toDouble()
            val joystickLeft = -driver.leftStickX.value.toDouble()
            val rotate = -driver.rightStickX.value.toDouble()
            val hasManualInput = kotlin.math.abs(joystickForward) > 0.05 ||
                                 kotlin.math.abs(joystickLeft) > 0.05 ||
                                 kotlin.math.abs(rotate) > 0.05

            // 2. Drive the robot
            when {
                hasManualInput -> {
                    telemetry.addData("Alignment Mode", "INACTIVE (Manual Override)")
                    robot.driveToWaypoint("BackdropLeft", false)
                    robot.driveToWaypoint("TestWaypoint", false)
                    
                    robot.mecanumDrive.fieldRelativeDrive(
                        vx = -joystickLeft, 
                        vy = joystickForward, 
                        omega = rotate,
                        useHeadingLock = true
                    )
                }
                driver.b.isPressed -> {
                    telemetry.addData("Alignment Mode", "ACTIVE (Tag 1 Seen & B Held!)")
                    robot.alignToTag(1)
                }
                driver.a.isPressed -> {
                    telemetry.addData("Alignment Mode", "ACTIVE (Drive to BackdropLeft Held!)")
                    robot.driveToWaypoint("BackdropLeft", true)
                }
                driver.x.isPressed -> {
                    telemetry.addData("Alignment Mode", "ACTIVE (Drive to TestWaypoint Held!)")
                    robot.driveToWaypoint("TestWaypoint", true)
                }
                else -> {
                    telemetry.addData("Alignment Mode", "INACTIVE (Manual Drive)")
                    robot.driveToWaypoint("BackdropLeft", false)
                    robot.driveToWaypoint("TestWaypoint", false)
                    
                    robot.mecanumDrive.fieldRelativeDrive(
                        vx = -joystickLeft, 
                        vy = joystickForward, 
                        omega = rotate,
                        useHeadingLock = true
                    )
                }
            }
        }
    }
}
