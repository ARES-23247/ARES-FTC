package org.firstinspires.ftc.teamcode.subsystems

import com.areslib.Store
import com.areslib.state.RobotState
import com.areslib.subsystem.Subsystem
import com.areslib.hardware.actuator.IntakeIO
import com.areslib.hardware.actuator.FlywheelIO

import com.areslib.action.RobotAction

import org.firstinspires.ftc.teamcode.dsl.season

/**
 * Intake lifecycle controller with a current-based jam latch.
 *
 * Only fresh current above 8 A contributes to the 250 ms overcurrent dwell. Invalid current is
 * tolerated for a bounded 100 ms grace period, then fails closed into the same safety latch.
 * A latch stops roller output and can clear only while the intake is disabled after a sustained
 * valid, low-current recovery observation. Output otherwise follows Redux intent and the global
 * power budget. An invalid state requesting intake and flywheel together fails closed at this
 * final hardware boundary even if an upstream producer bypasses the season controller.
 */
class IntakeSubsystem(private val io: IntakeIO) : Subsystem {
    private var overcurrentStartTimeMs = UNSET_TIME
    private var invalidCurrentStartTimeMs = UNSET_TIME
    private var recoveryStartTimeMs = UNSET_TIME

    override fun readSensors(store: Store, timestampMs: Long) {
        val currentAmps = io.rollerCurrentAmps
        val currentValid = io.rollerCurrentValid && currentAmps.isFinite() && currentAmps >= 0.0
        val intakeActive = store.state.superstructure.season.intakeActive

        if (stalled) {
            overcurrentStartTimeMs = UNSET_TIME
            invalidCurrentStartTimeMs = UNSET_TIME
            if (!intakeActive && currentValid && currentAmps <= RECOVERY_CURRENT_AMPS) {
                if (recoveryStartTimeMs == UNSET_TIME) recoveryStartTimeMs = timestampMs
                if (elapsedAtLeast(timestampMs, recoveryStartTimeMs, RECOVERY_DWELL_MS)) {
                    stalled = false
                    recoveryStartTimeMs = UNSET_TIME
                }
            } else {
                recoveryStartTimeMs = UNSET_TIME
            }
            return
        }

        recoveryStartTimeMs = UNSET_TIME
        if (!intakeActive) {
            overcurrentStartTimeMs = UNSET_TIME
            invalidCurrentStartTimeMs = UNSET_TIME
            return
        }

        if (!currentValid) {
            overcurrentStartTimeMs = UNSET_TIME
            if (invalidCurrentStartTimeMs == UNSET_TIME) invalidCurrentStartTimeMs = timestampMs
            stalled = elapsedAtLeast(timestampMs, invalidCurrentStartTimeMs, INVALID_CURRENT_GRACE_MS)
            return
        }

        invalidCurrentStartTimeMs = UNSET_TIME
        if (currentAmps > STALL_CURRENT_AMPS) {
            if (overcurrentStartTimeMs == UNSET_TIME) overcurrentStartTimeMs = timestampMs
            stalled = elapsedAtLeast(timestampMs, overcurrentStartTimeMs, STALL_DWELL_MS)
        } else {
            overcurrentStartTimeMs = UNSET_TIME
        }
    }

    var stalled = false
        private set

    override fun writeOutputs(state: RobotState, scale: Double) {
        val season = state.superstructure.season
        val active = season.intakeActive && !season.flywheelActive && season.flywheelTargetRPM == 0.0
        val safeScale = scale.takeIf { it.isFinite() }?.coerceIn(0.0, 1.0) ?: 0.0
        val nominalVoltage = state.tuning.subsystem.ftc.intakeNominalVoltage.takeIf { it.isFinite() } ?: 0.0
        val voltage = if (active && !stalled) nominalVoltage * safeScale else 0.0
        io.setRollerVoltage(voltage)
    }

    override fun close() {
        (io as? AutoCloseable)?.close()
    }

    private companion object {
        const val UNSET_TIME = -1L
        const val STALL_CURRENT_AMPS = 8.0
        const val RECOVERY_CURRENT_AMPS = 6.0
        const val STALL_DWELL_MS = 250L
        const val INVALID_CURRENT_GRACE_MS = 100L
        const val RECOVERY_DWELL_MS = 100L

        fun elapsedAtLeast(nowMs: Long, startMs: Long, durationMs: Long): Boolean {
            val elapsedMs = nowMs - startMs
            return elapsedMs >= durationMs && elapsedMs >= 0L
        }
    }
}

/**
 * Flywheel lifecycle controller that publishes measured RPM into immutable season state.
 * Dispatch is bounded to at most 20 Hz for meaningful changes and at least every 250 ms. The
 * target is intentionally not brownout-scaled because that changes shot speed; scale zero remains
 * the emergency-stop signal.
 */
class FlywheelSubsystem(private val io: FlywheelIO) : Subsystem {
    private var lastDispatchedRpm = 0.0
    private var lastDispatchTime = 0L
    private var lastVelocityValid = false
    private var invalidVelocityStartTimeMs = UNSET_TIME
    private var recoveryStartTimeMs = UNSET_TIME

    override fun readSensors(store: Store, timestampMs: Long) {
        val measuredRpm = io.velocityRpm
        val velocityValid = io.velocityValid && measuredRpm.isFinite()
        val currentRpm = if (velocityValid) measuredRpm else 0.0
        val timeSinceLastDispatch = timestampMs - lastDispatchTime
        val flywheelActive = store.state.superstructure.season.flywheelActive

        if (feedbackFaultLatched) {
            invalidVelocityStartTimeMs = UNSET_TIME
            if (!flywheelActive && velocityValid) {
                if (recoveryStartTimeMs == UNSET_TIME) recoveryStartTimeMs = timestampMs
                if (elapsedAtLeast(timestampMs, recoveryStartTimeMs, RECOVERY_DWELL_MS)) {
                    feedbackFaultLatched = false
                    recoveryStartTimeMs = UNSET_TIME
                }
            } else {
                recoveryStartTimeMs = UNSET_TIME
            }
        } else if (!flywheelActive || velocityValid) {
            invalidVelocityStartTimeMs = UNSET_TIME
            recoveryStartTimeMs = UNSET_TIME
        } else {
            if (invalidVelocityStartTimeMs == UNSET_TIME) invalidVelocityStartTimeMs = timestampMs
            feedbackFaultLatched = elapsedAtLeast(
                timestampMs,
                invalidVelocityStartTimeMs,
                INVALID_VELOCITY_GRACE_MS
            )
        }
        
        val rpmDiff = kotlin.math.abs(currentRpm - lastDispatchedRpm)
        if (velocityValid != lastVelocityValid ||
            (timeSinceLastDispatch >= 50 && rpmDiff >= 20.0) || timeSinceLastDispatch >= 250
        ) {
            this.currentRpm = currentRpm
            lastDispatchedRpm = currentRpm
            lastDispatchTime = timestampMs
            lastVelocityValid = velocityValid
            
            val seasonState = store.state.superstructure.season
            store.dispatch(RobotAction.UpdateSubsystemState(
                seasonState.copy(
                    flywheelCurrentRPM = currentRpm,
                    flywheelVelocityValid = velocityValid
                )
            ))
        }
    }

    var currentRpm: Double = 0.0
    var feedbackFaultLatched: Boolean = false
        private set

    /**
     * Flywheel target RPM must NOT be scaled down during brownouts because projectile launch velocity
     * depends directly on unscaled flywheel RPM to hit target scoring goals accurately.
     */
    override fun writeOutputs(state: RobotState, scale: Double) {
        val season = state.superstructure.season
        val active = season.flywheelActive && !season.intakeActive
        val targetRpm = season.flywheelTargetRPM
            .takeIf { it.isFinite() && it > 0.0 }
            ?: 0.0
        val effortScale = scale.takeIf { it.isFinite() }?.coerceIn(0.0, 1.0) ?: 0.0

        // Zero output is explicit rather than delegated through an optional compatibility overload.
        // This guarantees lifecycle stops and latched feedback faults cannot retain a velocity target.
        if (!active || feedbackFaultLatched || targetRpm == 0.0 || effortScale == 0.0) {
            io.setAppliedVoltage(0.0)
            return
        }

        // Preserve the scoring target while limiting the effort/acceleration used to reach it.
        io.setVelocityRpm(targetRpm, effortScale)
    }

    override fun close() {
        (io as? AutoCloseable)?.close()
    }

    private companion object {
        const val UNSET_TIME = -1L
        const val INVALID_VELOCITY_GRACE_MS = 150L
        const val RECOVERY_DWELL_MS = 100L

        fun elapsedAtLeast(nowMs: Long, startMs: Long, durationMs: Long): Boolean {
            val elapsedMs = nowMs - startMs
            return elapsedMs >= durationMs && elapsedMs >= 0L
        }
    }
}
