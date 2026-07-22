package org.firstinspires.ftc.teamcode.opmodes.robot

import com.areslib.ftc.FtcMecanumRobot
import com.areslib.state.Alliance

class AresDriveController(private val base: FtcMecanumRobot) {
    fun driveFieldCentric(x: Double, y: Double, rotation: Double) {
        val mult = if (base.store.state.drive.alliance == Alliance.BLUE) -1.0 else 1.0
        base.driveFieldCentric(x * mult, y * mult, rotation)
    }

    fun driveRobotCentric(x: Double, y: Double, rotation: Double) {
        base.driveRobotCentric(x, y, rotation)
    }

    fun resetPoseForAlliance() {
        base.resetPoseForAlliance()
    }
}
