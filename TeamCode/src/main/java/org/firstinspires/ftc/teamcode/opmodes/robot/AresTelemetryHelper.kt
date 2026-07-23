package org.firstinspires.ftc.teamcode.opmodes.robot

import com.areslib.action.RobotAction
import com.areslib.ftc.FtcMecanumRobot
import com.areslib.hardware.actuator.IndicatorLightColor

class AresTelemetryHelper(private val base: FtcMecanumRobot) {
    private var lastTelemetryUpdateMs: Long = 0L

    /**
     * Documentation for addTelemetry
     */
    fun addTelemetry(key: String, value: Any) {
        base.telemetryManager.customDriverStationText[key] = value.toString()
    }

    fun updateTelemetry() {
        val now = com.areslib.util.RobotClock.currentTimeMillis()
        if (now - lastTelemetryUpdateMs < TELEMETRY_PERIOD_MS) return
        lastTelemetryUpdateMs = now

        val alliance = base.store.state.drive.alliance.name
        val estPose = base.store.state.drive.poseEstimator.estimatedPose
        addTelemetry("Alliance", alliance)
        addTelemetry("EKF Pose (X, Y, Deg)", String.format("(%.2f, %.2f) %.1f°",
            estPose.x,
            estPose.y,
            Math.toDegrees(estPose.heading.radians)
        ))
    }

    companion object {
        private const val TELEMETRY_PERIOD_MS = 100L
    }

    fun setIndicatorColor(color: IndicatorLightColor) {
        base.store.dispatch(RobotAction.SetIndicatorLight("indicator", color.position))
    }
}

