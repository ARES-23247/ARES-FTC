package org.firstinspires.ftc.teamcode.opmodes

import com.qualcomm.robotcore.eventloop.opmode.Autonomous
import com.areslib.ftc.ARESMecanumAuto

@Autonomous(name = "TestPath Auto", group = "ARES")
class CustomPathAuto : ARESMecanumAuto() {
    override val pathName: String = "TestPath"
}
