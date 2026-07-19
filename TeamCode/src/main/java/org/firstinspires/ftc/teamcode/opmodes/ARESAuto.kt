package org.firstinspires.ftc.teamcode.opmodes

import com.qualcomm.robotcore.eventloop.opmode.Autonomous
import com.areslib.ftc.dsl.FtcMecanumAutoBase
import com.areslib.ftc.FtcMecanumRobot
import com.areslib.action.RobotAction
import com.areslib.state.Alliance

@Autonomous(name = "ARES Mecanum Auto", group = "ARES")
class ARESAuto : FtcMecanumAutoBase<AresRobot>() {
    override val pathName: String = "TestPath"

    override fun buildRobot(): AresRobot {
        val robot = AresRobot(hardwareMap, telemetry)
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

