package org.firstinspires.ftc.teamcode.opmodes

import com.qualcomm.robotcore.eventloop.opmode.Autonomous
import com.areslib.ftc.dsl.FtcMecanumAutoBase
import com.areslib.ftc.FtcMecanumRobot
import com.areslib.state.Alliance
import com.areslib.action.RobotAction

abstract class TestAutoBase(private val alliance: Alliance) : FtcMecanumAutoBase<AresRobot>() {
    override val pathName: String = "TestAuto"
    
    override fun buildRobot(): AresRobot {
        /**
         * Documentation for robot
         */
        val robot = AresRobot(hardwareMap, telemetry)
        robot.base.store.dispatch(RobotAction.SetAlliance(alliance))
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
/**
 * Documentation for TestAutoRed
 */

@Autonomous(name = "TestAuto - RED", group = "ARES")
class TestAutoRed : TestAutoBase(Alliance.RED)
/**
 * Documentation for TestAutoBlue
 */

@Autonomous(name = "TestAuto - BLUE", group = "ARES")
class TestAutoBlue : TestAutoBase(Alliance.BLUE)
