package org.firstinspires.ftc.teamcode.opmodes

import com.qualcomm.robotcore.eventloop.opmode.TeleOp
import org.firstinspires.ftc.teamcode.dsl.AresTeleOpBase

/**
 * Primary field-centric driver OpMode for the four-motor DECODE robot.
 *
 * Restores a valid Auto pose/alliance from process-local storage, otherwise starts red. Both
 * translation axes are alliance-mirrored by [org.firstinspires.ftc.teamcode.opmodes.robot.AresDriveController];
 * heading remains CCW-positive radians. Optional indicator bindings safely no-op when hardware is absent.
 */
@TeleOp(name = "Direct Mecanum Drivetrain", group = "ARES")
class ARESMecanumTeleOp : AresTeleOpBase() {

    /** Scheme-authored drive bindings replace the hand-written gamepad drive when present. */
    override val allowGeneratedDrive: Boolean = true

    override fun define() = teleOp {
        
        var isHeadingLockEnabled = true
        
        controls {
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

        setup {
            robot.base.mecanumIO.slewRateLimit = 4.0 // Ramp up to full speed in 0.25 seconds
        }
        
        everyLoop {
            // Generated drive bindings already shaped and mirrored the axes; only OpModes without
            // scheme-authored drive fall back to the hand-written controller here.
            if (!usesGeneratedDriveBindings) {
                robot.driveWithGamepad(driver, useHeadingLock = isHeadingLockEnabled)
            }
        }
    }
}
