package org.firstinspires.ftc.teamcode.opmodes

import com.qualcomm.robotcore.eventloop.opmode.Autonomous
import com.areslib.ftc.dsl.FtcMecanumAutoBase
import com.areslib.ftc.FtcMecanumRobot
import com.areslib.state.Alliance
import com.areslib.action.RobotAction


/**
 * Documentation for TestAutoRed
 */

@Autonomous(name = "TestAuto - RED", group = "ARES")
class TestAutoRed : org.firstinspires.ftc.teamcode.dsl.AresAutoBase() {
    override val pathName: String = "TestAuto"
    override fun buildRobot(): AresRobot {
        val robot = super.buildRobot()
        robot.base.store.dispatch(RobotAction.SetAlliance(Alliance.RED))
        return robot
    }
}
/**
 * Documentation for TestAutoBlue
 */

@Autonomous(name = "TestAuto - BLUE", group = "ARES")
class TestAutoBlue : org.firstinspires.ftc.teamcode.dsl.AresAutoBase() {
    override val pathName: String = "TestAuto"
    override fun buildRobot(): AresRobot {
        val robot = super.buildRobot()
        robot.base.store.dispatch(RobotAction.SetAlliance(Alliance.BLUE))
        return robot
    }
}
