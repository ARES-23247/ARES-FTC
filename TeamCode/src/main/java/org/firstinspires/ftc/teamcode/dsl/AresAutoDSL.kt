package org.firstinspires.ftc.teamcode.dsl

import com.areslib.ftc.dsl.FtcMecanumAutoBase
import com.areslib.ftc.FtcMecanumRobot
import com.areslib.action.RobotAction
import com.areslib.state.Alliance
import org.firstinspires.ftc.teamcode.opmodes.AresRobot
/**
 * Documentation for AresAutoBase
 */

abstract class AresAutoBase : FtcMecanumAutoBase<AresRobot>() {
    
    // Auto OpModes shouldn't have to rewrite this boilerplate
    override fun buildRobot(): AresRobot {
        /**
         * Documentation for robot
         */
        val robot = AresRobot(hardwareMap, telemetry)
        // Set a default alliance. Typically Auto sets this based on which auto runs,
        // but can be default RED, or overridden by child.
        robot.base.store.dispatch(RobotAction.SetAlliance(Alliance.BLUE))
        return robot
    }

    override fun getMecanumRobot(robot: AresRobot): FtcMecanumRobot {
        return robot.base
    }

    override fun updateRobot(robot: AresRobot) {
        robot.update()
    }

    override fun closeRobot(robot: AresRobot) {
        robot.close()
    }
}
