package org.firstinspires.ftc.teamcode.opmodes

import com.qualcomm.robotcore.eventloop.opmode.TeleOp
import org.firstinspires.ftc.teamcode.dsl.AresTeleOpBase

/**
 * Field-centric drive mode that enables ARESLib's local NT4 live-tuning synchronizer.
 * It intentionally starts on red alliance; use the X binding to change alliance and reset pose.
 * Tuning traffic remains local to the robot network and is disabled in normal competition TeleOp.
 */
@TeleOp(name = "ARES Live Tuning TeleOp", group = "Tuning")
class ARESTuningTeleOp : AresTeleOpBase() {

    override fun define() = teleOp {
        
        controls {
            driver.y.onPress("Reset Field Centric Pose") {
                robot.resetPoseForAlliance()
            }
            driver.x.onPress("Toggle Alliance") {
                robot.toggleAlliance()
                robot.resetPoseForAlliance()
            }
        }

        setup {
            robot.base.isLiveTuningEnabled = true
            robot.base.store.dispatch(com.areslib.action.RobotAction.SetAlliance(com.areslib.state.Alliance.RED))
            robot.base.mecanumIO.slewRateLimit = 4.0
            robot.addTelemetry("Tuning", "LIVE NT4 DASHBOARD SYNC ACTIVE")
        }
        
        everyLoop {
            robot.driveWithGamepad(driver, useHeadingLock = true)
        }
    }
}
