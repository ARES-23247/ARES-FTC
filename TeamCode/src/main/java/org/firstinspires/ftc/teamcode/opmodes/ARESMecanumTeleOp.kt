package org.firstinspires.ftc.teamcode.opmodes

import com.qualcomm.robotcore.eventloop.opmode.TeleOp
import org.firstinspires.ftc.teamcode.dsl.AresTeleOpBase
import org.firstinspires.ftc.teamcode.dsl.season

/**
 * Primary field-centric driver OpMode for the four-motor DECODE robot.
 *
 * Restores a valid Auto pose/alliance from process-local storage, otherwise starts red. Both
 * translation axes are alliance-mirrored by [org.firstinspires.ftc.teamcode.opmodes.robot.AresDriveController];
 * heading remains CCW-positive radians. Optional indicator bindings safely no-op when hardware is absent.
 */
@TeleOp(name = "Direct Mecanum Drivetrain", group = "ARES")
class ARESMecanumTeleOp : AresTeleOpBase() {

    override fun define() = aresTeleOp {
        
        var isHeadingLockEnabled = true
        
        onConfigure { robot, driver ->
            driver.leftStickButton.onPress("Toggle Heading Lock") {
                isHeadingLockEnabled = !isHeadingLockEnabled
            }

            driver.y.onPress("Reset Field Centric Pose") {
                robot.resetPoseForAlliance()
            }
            driver.x.onPress("Toggle Alliance") {
                robot.toggleAlliance()
                robot.resetPoseForAlliance()
            }

            // Primary indicator: D-pad up/down. Secondary indicator: left/right.
            val indicatorColors = com.areslib.hardware.actuator.IndicatorLightColor.entries
            var light1Index = 0
            var light2Index = 0

            driver.dpadUp.onPress("Light 1 Next Color") {
                light1Index = (light1Index + 1) % indicatorColors.size
                robot.setIndicatorColor(indicatorColors[light1Index])
            }
            driver.dpadDown.onPress("Light 1 Prev Color") {
                light1Index = (light1Index - 1 + indicatorColors.size) % indicatorColors.size
                robot.setIndicatorColor(indicatorColors[light1Index])
            }

            driver.dpadRight.onPress("Light 2 Next Color") {
                light2Index = (light2Index + 1) % indicatorColors.size
                robot.setSecondIndicatorColor(indicatorColors[light2Index])
            }
            driver.dpadLeft.onPress("Light 2 Prev Color") {
                light2Index = (light2Index - 1 + indicatorColors.size) % indicatorColors.size
                robot.setSecondIndicatorColor(indicatorColors[light2Index])
            }
        }

        onInit { robot, _ ->
            // PoseStorage survives OpMode changes in one RC process, not a reboot.
            if (com.areslib.util.PoseStorage.hasValidPose) {
                robot.base.store.dispatch(com.areslib.action.RobotAction.SetAlliance(com.areslib.util.PoseStorage.alliance))
                robot.base.resetPose(com.areslib.util.PoseStorage.currentPose)
            } else {
                robot.base.store.dispatch(com.areslib.action.RobotAction.SetAlliance(com.areslib.state.Alliance.RED))
            }

            val seasonState = robot.base.store.state.superstructure.season.copy(liftHeight = org.firstinspires.ftc.teamcode.opmodes.TeamStateStorage.liftHeight)
            robot.base.store.dispatch(com.areslib.action.RobotAction.UpdateSubsystemState(seasonState))

            robot.base.mecanumIO.slewRateLimit = 4.0 // Ramp up to full speed in 0.25 seconds
        }
        
        onLoop { robot, driver, _ ->
            robot.driveWithGamepad(driver, useHeadingLock = isHeadingLockEnabled)
        }
    }
}
