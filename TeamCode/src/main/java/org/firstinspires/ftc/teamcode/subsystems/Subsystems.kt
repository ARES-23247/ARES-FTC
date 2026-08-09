package org.firstinspires.ftc.teamcode.subsystems

import com.areslib.Store
import com.areslib.state.RobotState
import com.areslib.subsystem.Subsystem
import org.firstinspires.ftc.teamcode.hardware.IntakeIO
import org.firstinspires.ftc.teamcode.hardware.FlywheelIO

import com.areslib.action.RobotAction

import org.firstinspires.ftc.teamcode.dsl.season

class IntakeSubsystem(private val io: IntakeIO) : Subsystem {
    private var stallStartTime: Long? = null

    override fun readSensors(store: Store, timestampMs: Long) {
        io.refresh()
        val currentAmps = io.currentAmps
        if (currentAmps > 8.0) {
            if (stallStartTime == null) stallStartTime = timestampMs
            if (timestampMs - stallStartTime!! > 250) {
                val seasonState = store.state.superstructure.season
                store.dispatch(RobotAction.UpdateSubsystemState(seasonState.copy(intakeActive = false)))
            }
        } else {
            stallStartTime = null
        }
    }

    override fun writeOutputs(state: RobotState, scale: Double) {
        /**
         * Documentation for active
         */
        val active = state.superstructure.season.intakeActive
        /**
         * Documentation for voltage
         */
        val flywheelSpooling = state.superstructure.season.flywheelActive && state.superstructure.season.flywheelTargetRPM > state.superstructure.season.flywheelCurrentRPM
        val intakePowerScale = if (flywheelSpooling) 0.6 else 1.0
        val voltage = if (active) state.tuning.intakeNominalVoltage * scale * intakePowerScale else 0.0
        io.setRollerVoltage(voltage)
    }

    override fun close() {
        (io as? AutoCloseable)?.close()
    }
}

class FlywheelSubsystem(private val io: FlywheelIO) : Subsystem {
    private var lastDispatchedRpm = 0.0
    private var lastDispatchTime = 0L

    override fun readSensors(store: Store, timestampMs: Long) {
        io.refresh()
        /**
         * Documentation for currentRpm
         */
        
        val currentRpm = io.velocityRpm
        /**
         * Documentation for timeSinceLastDispatch
         */
        val timeSinceLastDispatch = timestampMs - lastDispatchTime
        
        val rpmDiff = kotlin.math.abs(currentRpm - lastDispatchedRpm)
        if ((timeSinceLastDispatch >= 50 && rpmDiff >= 20.0) || timeSinceLastDispatch >= 250) {
            /**
             * Documentation for seasonState
             */
            val seasonState = store.state.superstructure.season
            store.dispatch(RobotAction.UpdateSubsystemState(seasonState.copy(flywheelCurrentRPM = currentRpm)))
            lastDispatchedRpm = currentRpm
            lastDispatchTime = timestampMs
        }
    }

    /**
     * Flywheel target RPM must NOT be scaled down during brownouts because projectile launch velocity
     * depends directly on unscaled flywheel RPM to hit target scoring goals accurately.
     */
    override fun writeOutputs(state: RobotState, scale: Double) {
        /**
         * Documentation for active
         */
        val active = state.superstructure.season.flywheelActive
        io.setVelocityRpm(if (active) state.superstructure.season.flywheelTargetRPM else 0.0)
    }

    override fun close() {
        (io as? AutoCloseable)?.close()
    }
}
