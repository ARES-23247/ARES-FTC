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
        val season = base.store.state.superstructure.season
        // Stopping is a safety action and must never be blocked by debounce or another mechanism.
        if (season.intakeActive) {
            lastIntakeToggleTimeMs = now
            base.store.dispatch(RobotAction.UpdateSubsystemState(season.copy(intakeActive = false)))
            return
        }
        if (now - lastIntakeToggleTimeMs < TOGGLE_DEBOUNCE_MS) return
        lastIntakeToggleTimeMs = now
        // Starting intake atomically removes every shooter command so the two mechanisms can
        // never be active together, even when their button edges arrive in adjacent frames.
        base.store.dispatch(RobotAction.UpdateSubsystemState(
            state = season.copy(
                intakeActive = true,
                flywheelActive = false,
                flywheelTargetRPM = 0.0
            )
        ))
    }
    /** Toggles flywheel intent unless debounce or the intake interlock rejects the request. */
    fun toggleShooter() {
        val now = RobotClock.currentTimeMillis()
        val season = base.store.state.superstructure.season
        // A stop request always wins, including immediately after a start edge and while intake is
        // active because of a stale/restored state.
        if (season.flywheelActive || season.flywheelTargetRPM != 0.0) {
            lastShooterToggleTimeMs = now
            base.store.dispatch(RobotAction.UpdateSubsystemState(
                season.copy(flywheelActive = false, flywheelTargetRPM = 0.0)
            ))
            return
        }
        if (now - lastShooterToggleTimeMs < TOGGLE_DEBOUNCE_MS) return
        lastShooterToggleTimeMs = now
        if (season.intakeActive) return
        val configuredTarget = base.store.state.tuning.subsystem.ftc.flywheelTargetRpmPreset
        val currentTarget = configuredTarget.takeIf { it.isFinite() }
            ?.coerceIn(0.0, MAX_FLYWHEEL_RPM)
            ?: 0.0
        base.store.dispatch(RobotAction.UpdateSubsystemState(
            state = season.copy(
                flywheelActive = currentTarget > 0.0,
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
