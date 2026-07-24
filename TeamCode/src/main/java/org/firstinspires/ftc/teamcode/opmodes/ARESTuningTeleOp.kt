package org.firstinspires.ftc.teamcode.opmodes

import com.qualcomm.robotcore.eventloop.opmode.TeleOp
import org.firstinspires.ftc.teamcode.dsl.AresTeleOpBase

/**
 * Dedicated Live Tuning TeleOp OpMode for ARES Robotics.
 * Enables active NetworkTables synchronization with the ARES-Analytics desktop dashboard,
 * allowing live modification of PID gains, drivetrain scale, and EKF filtering parameters.
 */
@TeleOp(name = "ARES Live Tuning TeleOp", group = "Tuning")
class ARESTuningTeleOp : AresTeleOpBase() {

    override fun define() = aresTeleOp {
        
        onConfigure { robot, driver ->
            driver.y.onPress("Reset Field Centric Pose") {
                robot.resetPoseForAlliance()
            }
            driver.x.onPress("Toggle Alliance") {
                robot.toggleAlliance()
                robot.resetPoseForAlliance()
            }
        }

        onInit { robot, _ ->
            robot.base.isLiveTuningEnabled = true
            robot.base.store.dispatch(com.areslib.action.RobotAction.SetAlliance(com.areslib.state.Alliance.RED))
            robot.base.mecanumIO.slewRateLimit = 4.0
            robot.addTelemetry("Tuning", "LIVE NT4 DASHBOARD SYNC ACTIVE")
        }
        
        onLoop { robot, driver, _ ->
            robot.base.mecanumDrive.driveWithGamepad(driver, useHeadingLock = true)
        }
    }
}
