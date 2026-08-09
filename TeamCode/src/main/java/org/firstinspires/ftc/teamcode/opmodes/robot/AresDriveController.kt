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
        val deadzoned = if (kotlin.math.abs(input) < 0.05) 0.0 else (kotlin.math.abs(input) - 0.05) / 0.95 * kotlin.math.sign(input)
        return kotlin.math.sign(deadzoned) * kotlin.math.abs(deadzoned).pow(3)
    }

    private var smoothX = 0.0
    private var smoothY = 0.0
    private var smoothRot = 0.0

    private fun smoothTransition(x: Double, y: Double, rot: Double) {
        if (transitionFrames > 0) {
            transitionFrames--
        }
        val alpha = 0.4
        lastX = lastX * 0.6 + x * alpha
        lastY = lastY * 0.6 + y * alpha
        lastRot = lastRot * 0.6 + rot * alpha
        
        smoothX = lastX
        smoothY = lastY
        smoothRot = lastRot
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
        smoothTransition(px, py, prot)

        base.driveFieldCentric(smoothX, smoothY, smoothRot)
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
        smoothTransition(px, py, prot)

        base.driveRobotCentric(smoothX, smoothY, smoothRot)
    }

    fun driveWithGamepad(driver: com.areslib.telemetry.AresGamepad, useHeadingLock: Boolean = true) {
        if (!wasFieldCentric) {
            wasFieldCentric = true
            transitionFrames = 5
        }
        val px = processAxis(driver.left_stick_x.toDouble())
        val py = processAxis(-driver.left_stick_y.toDouble())
        val prot = processAxis(driver.right_stick_x.toDouble())
        smoothTransition(px, py, prot)
        
        base.mecanumDrive.fieldRelativeDrive(smoothX, smoothY, smoothRot, useHeadingLock)
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
