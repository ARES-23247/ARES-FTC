package org.firstinspires.ftc.teamcode.opmodes

import com.qualcomm.robotcore.eventloop.opmode.Autonomous
import com.areslib.ftc.dsl.FtcMecanumAutoBase
import com.areslib.ftc.FtcMecanumRobot

@Autonomous(name = "ARES Mecanum Auto", group = "ARES")
class ARESAuto : FtcMecanumAutoBase<AresRobot>() {
    override fun buildRobot(): AresRobot {
        return AresRobot(hardwareMap, telemetry)
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

