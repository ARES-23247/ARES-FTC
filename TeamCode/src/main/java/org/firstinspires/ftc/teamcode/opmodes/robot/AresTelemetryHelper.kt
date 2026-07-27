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
        addTelemetry("EKF Pose X", estPose.x.toString())
        addTelemetry("EKF Pose Y", estPose.y.toString())
        addTelemetry("EKF Pose Deg", Math.toDegrees(estPose.heading.radians).toString())
    }

    companion object {
        private const val TELEMETRY_PERIOD_MS = 100L
    }

    fun setIndicatorColor(name: String = "indicator", color: IndicatorLightColor) {
        base.store.dispatch(RobotAction.SetIndicatorLight(name, color.position))
    }

    fun setIndicatorColor(color: IndicatorLightColor) {
        setIndicatorColor("indicator", color)
    }

    fun setSecondIndicatorColor(color: IndicatorLightColor) {
        setIndicatorColor("indicator2", color)
    }

    fun setPrismPreset(name: String = "prism", preset: com.areslib.hardware.actuator.PrismPwmPreset) {
        base.store.dispatch(RobotAction.SetPrismDriver(name, preset.pulseWidthUs))
    }

    fun setPrismPulseWidth(name: String = "prism", pulseWidthUs: Int) {
        base.store.dispatch(RobotAction.SetPrismDriver(name, pulseWidthUs))
    }
}

