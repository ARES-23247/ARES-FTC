package org.firstinspires.ftc.teamcode.opmodes

import com.qualcomm.robotcore.eventloop.opmode.Autonomous
import com.areslib.state.Alliance
import org.firstinspires.ftc.teamcode.dsl.AresAutoBase


/** Red-alliance validation entry point for the shared native `test-auto` asset. */
@Autonomous(name = "TestAuto - RED", group = "ARES")
class TestAutoRed : AresAutoBase() {
    override fun defineAuto() = auto {
        aresAuto("test-auto")
        alliance(Alliance.RED)
    }
}

/**
 * Blue-alliance validation entry point for `test-auto`; the base transforms goals and start pose.
 */
@Autonomous(name = "TestAuto - BLUE", group = "ARES")
class TestAutoBlue : AresAutoBase() {
    override fun defineAuto() = auto {
        aresAuto("test-auto")
        alliance(Alliance.BLUE)
    }
}
