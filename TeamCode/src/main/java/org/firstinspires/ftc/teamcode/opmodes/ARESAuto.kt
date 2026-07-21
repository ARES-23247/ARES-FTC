package org.firstinspires.ftc.teamcode.opmodes

import com.qualcomm.robotcore.eventloop.opmode.Autonomous
import com.areslib.ftc.dsl.FtcMecanumAutoBase
import com.areslib.ftc.FtcMecanumRobot
import com.areslib.action.RobotAction
import com.areslib.state.Alliance

import org.firstinspires.ftc.teamcode.dsl.AresAutoBase

@Autonomous(name = "ARES Mecanum Auto", group = "ARES")
class ARESAuto : AresAutoBase() {
    override val pathName: String = "TestPath"
}

