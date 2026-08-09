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
        val truncated = value.toString().take(150)
        base.telemetryManager.customDriverStationText[key] = truncated
    }

    fun clearTelemetry() {
        base.telemetryManager.customDriverStationText.clear()
    }

    fun updateTelemetry() {
        val now = com.areslib.util.RobotClock.currentTimeMillis()
        if (now - lastTelemetryUpdateMs < TELEMETRY_PERIOD_MS) return
        lastTelemetryUpdateMs = now
        clearTelemetry()

        val alliance = base.store.state.drive.alliance.name
        val estPose = base.store.state.drive.poseEstimator.estimatedPose
        addTelemetry("Alliance", alliance)
        addTelemetry("EKF Pose X", estPose.x)
        addTelemetry("EKF Pose Y", estPose.y)
        addTelemetry("EKF Pose Deg", Math.toDegrees(estPose.heading.radians))
        
        val voltage = base.powerManager.batteryVoltage
        if (voltage < 11.5) {
            addTelemetry("Battery V", "<font color='red'><b>%.1fV (LOW)</b></font>".format(voltage))
        } else {
            addTelemetry("Battery V", voltage)
        }
        
        addTelemetry("Power Scale", base.powerManager.powerScale)
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

    private var lastRumbleTimeMs = 0L

    fun rumbleDriver(gamepad: com.qualcomm.robotcore.hardware.Gamepad, durationMs: Int) {
        val now = com.areslib.util.RobotClock.currentTimeMillis()
        if (now - lastRumbleTimeMs >= 1000L) {
            gamepad.rumble(1.0, 1.0, durationMs.coerceAtMost(1000))
            lastRumbleTimeMs = now
        }
    }
}

