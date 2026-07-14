package org.firstinspires.ftc.teamcode.opmodes

import com.qualcomm.robotcore.eventloop.opmode.Autonomous
import com.areslib.ftc.dsl.FtcMecanumAutoBase
import com.areslib.ftc.FtcMecanumRobot

@Autonomous(name = "TestPath Auto", group = "ARES")
class CustomPathAuto : FtcMecanumAutoBase<AresRobot>() {
    override val pathName: String = "TestPath"
    
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

