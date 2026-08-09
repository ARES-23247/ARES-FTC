package org.firstinspires.ftc.teamcode.opmodes.robot

import com.areslib.ftc.FtcMecanumRobot
import com.areslib.state.Alliance
import kotlin.math.pow

class AresDriveController(private val base: FtcMecanumRobot) {
    private var lastX = 0.0
    private var lastY = 0.0
    private var lastRot = 0.0

    private fun processAxis(input: Double): Double {
        val magnitude = kotlin.math.abs(input)
        val deadzoned = if (magnitude < DEFAULT_DEADZONE) 0.0
            else (magnitude - DEFAULT_DEADZONE) / (1.0 - DEFAULT_DEADZONE) * kotlin.math.sign(input)
        val exponent = base.store.state.tuning.driverDeadbandExponent
            .let { if (it > 0.0) it else DEFAULT_CURVE_EXPONENT }
        return kotlin.math.sign(deadzoned) * kotlin.math.abs(deadzoned).pow(exponent)
    }

    private var smoothX = 0.0
    private var smoothY = 0.0
    private var smoothRot = 0.0

    private fun smoothTransition(x: Double, y: Double, rot: Double) {
        // Constant first-order EMA (alpha = 0.4) over the processed joystick input.
        // Intentional: this is the actual input smoothing in effect; the former
        // transitionFrames/wasFieldCentric bookkeeping was never read and is removed.
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
        val px = processAxis(x)
        val py = processAxis(y)
        val prot = processAxis(rotation)
        smoothTransition(px, py, prot)

        // CCW-positive convention: blue alliance mirrors forward/left intents.
        val blueAlliance = base.store.state.drive.alliance == Alliance.BLUE
        val outX = if (blueAlliance) -smoothX else smoothX
        val outY = if (blueAlliance) -smoothY else smoothY
        base.driveFieldCentric(outX, outY, smoothRot)
    }
    /**
     * Documentation for driveRobotCentric
     */

    fun driveRobotCentric(x: Double, y: Double, rotation: Double) {
        val px = processAxis(x)
        val py = processAxis(y)
        val prot = processAxis(rotation)
        smoothTransition(px, py, prot)

        base.driveRobotCentric(smoothX, smoothY, smoothRot)
    }

    fun driveWithGamepad(driver: com.areslib.telemetry.AresGamepad, useHeadingLock: Boolean = true) {
        val px = processAxis(driver.leftStickX.value.toDouble())
        val py = processAxis(-driver.leftStickY.value.toDouble())
        val prot = processAxis(driver.rightStickX.value.toDouble())
        smoothTransition(px, py, prot)

        // CCW-positive convention: blue alliance mirrors forward/left intents.
        val blueAlliance = base.store.state.drive.alliance == Alliance.BLUE
        val outX = if (blueAlliance) -smoothX else smoothX
        val outY = if (blueAlliance) -smoothY else smoothY
        base.mecanumDrive.fieldRelativeDrive(outX, outY, smoothRot, useHeadingLock)
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

    companion object {
        /** Joystick deadband threshold; values below this are treated as zero. */
        const val DEFAULT_DEADZONE = 0.05
        /** Fallback response-curve exponent when live tuning provides no valid value. */
        const val DEFAULT_CURVE_EXPONENT = 3.0
    }
}
