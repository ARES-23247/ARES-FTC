package org.firstinspires.ftc.teamcode.opmodes.robot

import com.areslib.action.RobotAction
import com.areslib.ftc.FtcMecanumRobot
import com.areslib.state.Alliance
import org.firstinspires.ftc.teamcode.dsl.*

import com.areslib.util.RobotClock

/**
 * Dispatches debounced DECODE mechanism and alliance intents into the Redux store.
 *
 * This controller never writes hardware. Shooter start is interlocked while intake is active;
 * shooter stop always clears target RPM. Debounce timestamps use [RobotClock] for deterministic
 * simulation and tests.
 */
class AresSuperstructureController(private val base: FtcMecanumRobot) {
    private var lastIntakeToggleTimeMs = 0L
    private var lastShooterToggleTimeMs = 0L
    private var lastAllianceToggleTimeMs = 0L

    /** Toggles intake intent after the 200 ms edge debounce. */
    fun toggleIntake() {
        val now = RobotClock.currentTimeMillis()
        if (now - lastIntakeToggleTimeMs < TOGGLE_DEBOUNCE_MS) return
        lastIntakeToggleTimeMs = now
        val season = base.store.state.superstructure.season
        base.store.dispatch(RobotAction.UpdateSubsystemState(
            state = season.copy(intakeActive = !season.intakeActive)
        ))
    }
    /** Toggles flywheel intent unless debounce or the intake interlock rejects the request. */
    fun toggleShooter() {
        val now = RobotClock.currentTimeMillis()
        if (now - lastShooterToggleTimeMs < TOGGLE_DEBOUNCE_MS) return
        lastShooterToggleTimeMs = now
        val season = base.store.state.superstructure.season
        if (season.intakeActive) return
        val configuredTarget = base.store.state.tuning.flywheelTargetRpmPreset
        val currentTarget = if (!season.flywheelActive) {
            configuredTarget.takeIf { it.isFinite() }?.coerceIn(0.0, MAX_FLYWHEEL_RPM) ?: 0.0
        } else {
            0.0
        }
        base.store.dispatch(RobotAction.UpdateSubsystemState(
            state = season.copy(
                flywheelActive = !season.flywheelActive,
                flywheelTargetRPM = currentTarget
            )
        ))
    }
    /** Toggles the Redux alliance; callers reset field pose separately when appropriate. */
    fun toggleAlliance() {
        val now = RobotClock.currentTimeMillis()
        if (now - lastAllianceToggleTimeMs < TOGGLE_DEBOUNCE_MS) return
        lastAllianceToggleTimeMs = now
        val currentAlliance = base.store.state.drive.alliance
        val newAlliance = when (currentAlliance) {
            Alliance.RED -> Alliance.BLUE
            Alliance.BLUE -> Alliance.RED
        }
        base.store.dispatch(RobotAction.SetAlliance(newAlliance))
    }

    private companion object {
        const val TOGGLE_DEBOUNCE_MS = 200L
        const val MAX_FLYWHEEL_RPM = 6000.0
    }
}
