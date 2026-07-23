package org.firstinspires.ftc.teamcode.opmodes.robot

import com.areslib.ftc.FtcMecanumRobot
import com.areslib.state.Alliance

class AresDriveController(private val base: FtcMecanumRobot) {
    /**
     * Documentation for driveFieldCentric
     */
    fun driveFieldCentric(x: Double, y: Double, rotation: Double) {
        /**
         * Documentation for mult
         */
        val mult = if (base.store.state.drive.alliance == Alliance.BLUE) -1.0 else 1.0
        base.driveFieldCentric(x * mult, y * mult, rotation)
    }
    /**
     * Documentation for driveRobotCentric
     */

    fun driveRobotCentric(x: Double, y: Double, rotation: Double) {
        base.driveRobotCentric(x, y, rotation)
    }
    /**
     * Documentation for resetPoseForAlliance
     */

    fun resetPoseForAlliance() {
        base.resetPoseForAlliance()
    }

    fun resetPose(pose: com.areslib.math.geometry.Pose2d = com.areslib.math.geometry.Pose2d()) {
        base.resetPose(pose)
    }
}
