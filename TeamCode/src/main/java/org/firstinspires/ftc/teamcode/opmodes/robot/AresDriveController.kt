package org.firstinspires.ftc.teamcode.opmodes.robot

import com.areslib.ftc.FtcMecanumRobot
import com.areslib.state.Alliance
import kotlin.math.pow

class AresDriveController(private val base: FtcMecanumRobot) {
    private var wasFieldCentric = true
    private var transitionFrames = 0
    private var lastX = 0.0
    private var lastY = 0.0
    private var lastRot = 0.0

    private fun processAxis(input: Double): Double {
        val deadzoned = if (kotlin.math.abs(input) < 0.05) 0.0 else input - kotlin.math.sign(input) * 0.05
        return kotlin.math.sign(deadzoned) * kotlin.math.abs(deadzoned).pow(3)
    }

    private fun smoothTransition(x: Double, y: Double, rot: Double): Triple<Double, Double, Double> {
        if (transitionFrames > 0) {
            transitionFrames--
            val alpha = 0.2
            lastX = lastX * (1 - alpha) + x * alpha
            lastY = lastY * (1 - alpha) + y * alpha
            lastRot = lastRot * (1 - alpha) + rot * alpha
            return Triple(lastX, lastY, lastRot)
        }
        lastX = x
        lastY = y
        lastRot = rot
        return Triple(x, y, rot)
    }

    /**
     * Documentation for driveFieldCentric
     */
    fun driveFieldCentric(x: Double, y: Double, rotation: Double) {
        if (!wasFieldCentric) {
            wasFieldCentric = true
            transitionFrames = 5
        }
        val px = processAxis(x)
        val py = processAxis(y)
        val prot = processAxis(rotation)
        val (sx, sy, srot) = smoothTransition(px, py, prot)

        /**
         * Documentation for mult
         */
        val mult = if (base.store.state.drive.alliance == Alliance.BLUE) -1.0 else 1.0
        base.driveFieldCentric(sx * mult, sy * mult, srot)
    }
    /**
     * Documentation for driveRobotCentric
     */

    fun driveRobotCentric(x: Double, y: Double, rotation: Double) {
        if (wasFieldCentric) {
            wasFieldCentric = false
            transitionFrames = 5
        }
        val px = processAxis(x)
        val py = processAxis(y)
        val prot = processAxis(rotation)
        val (sx, sy, srot) = smoothTransition(px, py, prot)

        base.driveRobotCentric(sx, sy, srot)
    }

    fun driveWithGamepad(driver: com.areslib.telemetry.AresGamepad, useHeadingLock: Boolean = true) {
        base.mecanumDrive.driveWithGamepad(driver, useHeadingLock)
    }

    fun alignToTag(tagId: Int) {
        base.alignToTag(tagId)
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
