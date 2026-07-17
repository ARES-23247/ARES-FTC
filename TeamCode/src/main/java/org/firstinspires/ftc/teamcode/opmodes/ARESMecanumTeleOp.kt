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
                robot.resetPoseForAlliance()
            }
            driver.x.onPress("Toggle Alliance") {
                val currentAlliance = robot.base.store.state.drive.alliance
                val newAlliance = when (currentAlliance) {
                    com.areslib.state.Alliance.RED -> com.areslib.state.Alliance.BLUE
                    com.areslib.state.Alliance.BLUE -> com.areslib.state.Alliance.RED
                }
                robot.base.store.dispatch(com.areslib.action.RobotAction.SetAlliance(newAlliance))
                robot.resetPoseForAlliance()
            }
        }

        onInit { robot, telemetry ->
            robot.base.store.dispatch(com.areslib.action.RobotAction.SetAlliance(com.areslib.state.Alliance.RED))
            
            // Auto-initialize pose with alliance starting orientation so field-centric is correct on start
            robot.resetPoseForAlliance()

            robot.base.mecanumIO.slewRateLimit = 4.0 // Ramp up to full speed in 0.25 seconds

            robot.addTelemetry("Alliance", robot.base.store.state.drive.alliance.name)
            val estPose = robot.base.store.state.drive.poseEstimator.estimatedPose
            robot.addTelemetry("EKF Pose (X, Y, Deg)", String.format("(%.2f, %.2f) %.1f°",
                estPose.x,
                estPose.y,
                Math.toDegrees(estPose.heading.radians)
            ))
        }
        
        onLoop { robot, driver, _ ->

            // 2. Drive the robot (Field-Centric Perspective)
            robot.base.mecanumDrive.driveWithGamepad(driver, useHeadingLock = true)
        }
    }
}
