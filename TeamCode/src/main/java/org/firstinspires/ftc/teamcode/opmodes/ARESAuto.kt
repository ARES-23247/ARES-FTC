package org.firstinspires.ftc.teamcode.opmodes

import com.qualcomm.robotcore.eventloop.opmode.Autonomous
import com.areslib.state.Alliance

import org.firstinspires.ftc.teamcode.dsl.AresAutoBase

/** Red-alliance competition entry point for the native `test-path` ARES auto. */
@Autonomous(name = "ARES Mecanum Auto", group = "ARES")
class ARESAuto : AresAutoBase() {
    override fun defineAuto() = auto {
        aresAuto("test-path")
        alliance(Alliance.RED)
    }
}

