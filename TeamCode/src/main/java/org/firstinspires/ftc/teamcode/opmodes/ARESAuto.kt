package org.firstinspires.ftc.teamcode.opmodes

import com.qualcomm.robotcore.eventloop.opmode.Autonomous
import com.areslib.state.Alliance

import org.firstinspires.ftc.teamcode.dsl.AresAutoBase

@Autonomous(name = "ARES Mecanum Auto", group = "ARES")
class ARESAuto : AresAutoBase() {
    override val pathName: String = "TestPath"

    override fun buildRobot(): AresRobot {
        val robot = super.buildRobot()
        configureAlliance(robot, Alliance.RED)
        return robot
    }
}

