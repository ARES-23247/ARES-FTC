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
        return robot
    }

    protected fun configureAlliance(robot: AresRobot, alliance: Alliance) {
        robot.base.store.dispatch(RobotAction.SetAlliance(alliance))
        robot.resetPoseForAlliance()
    }

    override fun getMecanumRobot(robot: AresRobot): FtcMecanumRobot {
        return robot.base
    }

    override fun updateRobot(robot: AresRobot) {
        robot.update()
    }

    override fun closeRobot(robot: AresRobot) {
        org.firstinspires.ftc.teamcode.opmodes.TeamStateStorage.liftHeight = robot.base.store.state.superstructure.season.liftHeight
        robot.close()
    }
}
