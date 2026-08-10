package org.firstinspires.ftc.teamcode.subsystems

import com.areslib.Store
import com.areslib.state.RobotState
import com.areslib.subsystem.Subsystem
import org.firstinspires.ftc.teamcode.hardware.IntakeIO
import org.firstinspires.ftc.teamcode.hardware.FlywheelIO

import com.areslib.action.RobotAction

import org.firstinspires.ftc.teamcode.dsl.season

/**
 * Intake lifecycle controller with a current-based jam latch.
 *
 * Only fresh current above 8 A contributes to the 250 ms dwell. Invalid or low-current samples
 * reset the dwell and latch. Output follows Redux intent and the global power budget, with an
 * additional 60% cap while the flywheel accelerates.
 */
class IntakeSubsystem(private val io: IntakeIO) : Subsystem {
    private var stallStartTime: Long = -1L

    override fun readSensors(store: Store, timestampMs: Long) {
        val currentAmps = io.rollerCurrentAmps
        if (!io.rollerCurrentValid || currentAmps <= STALL_CURRENT_AMPS) {
            stallStartTime = -1L
            stalled = false
            return
        }
        if (stallStartTime < 0L) stallStartTime = timestampMs
        stalled = timestampMs - stallStartTime > STALL_DWELL_MS
    }

    var stalled = false

    override fun writeOutputs(state: RobotState, scale: Double) {
        val active = state.superstructure.season.intakeActive
        val season = state.superstructure.season
        val flywheelSpooling = season.flywheelActive &&
            (!season.flywheelVelocityValid || season.flywheelTargetRPM > season.flywheelCurrentRPM)
        val intakePowerScale = if (flywheelSpooling) 0.6 else 1.0
        val voltage = if (active) state.tuning.intakeNominalVoltage * scale * intakePowerScale else 0.0
        io.setRollerVoltage(voltage)
    }

    override fun close() {
        (io as? AutoCloseable)?.close()
    }

    private companion object {
        const val STALL_CURRENT_AMPS = 8.0
        const val STALL_DWELL_MS = 250L
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

    override fun readSensors(store: Store, timestampMs: Long) {
        val velocityValid = io.velocityValid && io.velocityRpm.isFinite()
        val currentRpm = if (velocityValid) io.velocityRpm else 0.0
        val timeSinceLastDispatch = timestampMs - lastDispatchTime
        
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

    /**
     * Flywheel target RPM must NOT be scaled down during brownouts because projectile launch velocity
     * depends directly on unscaled flywheel RPM to hit target scoring goals accurately.
     */
    override fun writeOutputs(state: RobotState, scale: Double) {
        val active = state.superstructure.season.flywheelActive
        // Brownout scaling must not change launch velocity, but zero is the
        // lifecycle emergency-stop signal and must always stop the motor.
        val targetRpm = if (active && scale > 0.0) state.superstructure.season.flywheelTargetRPM else 0.0
        io.setVelocityRpm(targetRpm)
    }

    override fun close() {
        (io as? AutoCloseable)?.close()
    }
}
