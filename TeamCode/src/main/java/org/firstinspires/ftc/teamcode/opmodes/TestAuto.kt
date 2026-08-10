package org.firstinspires.ftc.teamcode.opmodes

import com.qualcomm.robotcore.eventloop.opmode.Autonomous
import com.areslib.state.Alliance
import org.firstinspires.ftc.teamcode.dsl.AresAutoBase


/** Red-alliance validation entry point for the shared `TestAuto` asset. */
@Autonomous(name = "TestAuto - RED", group = "ARES")
class TestAutoRed : AresAutoBase() {
    override val pathName: String = "TestAuto"
    override fun buildRobot(): AresRobot {
        val robot = super.buildRobot()
        configureAlliance(robot, Alliance.RED)
        return robot
    }
}

/**
 * Blue-alliance validation entry point for `TestAuto`; the base mirrors path and start pose.
 */
@Autonomous(name = "TestAuto - BLUE", group = "ARES")
class TestAutoBlue : AresAutoBase() {
    override val pathName: String = "TestAuto"
    override fun buildRobot(): AresRobot {
        val robot = super.buildRobot()
        configureAlliance(robot, Alliance.BLUE)
        return robot
    }
}
