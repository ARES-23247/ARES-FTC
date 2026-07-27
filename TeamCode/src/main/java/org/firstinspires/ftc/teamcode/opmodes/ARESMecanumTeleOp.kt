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
                robot.toggleAlliance()
                robot.resetPoseForAlliance()
            }

            // --- Cycle Indicator Light 1 ("indicator") with D-Pad Up / Down ---
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

            // --- Cycle Indicator Light 2 ("indicator2") with D-Pad Left / Right ---
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
            robot.base.store.dispatch(com.areslib.action.RobotAction.SetAlliance(com.areslib.state.Alliance.RED))

            robot.base.mecanumIO.slewRateLimit = 4.0 // Ramp up to full speed in 0.25 seconds
        }
        
        onLoop { robot, driver, _ ->

            // 2. Drive the robot (Field-Centric Perspective)
            robot.base.mecanumDrive.driveWithGamepad(driver, useHeadingLock = true)
        }
    }
}
