package org.firstinspires.ftc.teamcode.subsystems

import com.areslib.Store
import com.areslib.state.RobotState
import com.areslib.subsystem.Subsystem
import org.firstinspires.ftc.teamcode.hardware.IntakeIO
import org.firstinspires.ftc.teamcode.hardware.FlywheelIO

import com.areslib.action.RobotAction

import org.firstinspires.ftc.teamcode.dsl.season

class IntakeSubsystem(private val io: IntakeIO) : Subsystem {
    override fun readSensors(store: Store, timestampMs: Long) {
        io.refresh()
    }

    override fun writeOutputs(state: RobotState, scale: Double) {
        val active = state.superstructure.season.intakeActive
        val voltage = if (active) state.tuning.intakeNominalVoltage * scale else 0.0
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
        
        val currentRpm = io.velocityRpm
        val timeSinceLastDispatch = timestampMs - lastDispatchTime
        
        if (timeSinceLastDispatch >= 50 && kotlin.math.abs(currentRpm - lastDispatchedRpm) >= 20.0) {
            val seasonState = store.state.superstructure.season
            store.dispatch(RobotAction.UpdateSubsystemState(seasonState.copy(flywheelCurrentRPM = currentRpm)))
            lastDispatchedRpm = currentRpm
            lastDispatchTime = timestampMs
        }
    }

    override fun writeOutputs(state: RobotState, scale: Double) {
        val active = state.superstructure.season.flywheelActive
        io.setVelocityRpm(if (active) state.superstructure.season.flywheelTargetRPM else 0.0)
    }

    override fun close() {
        (io as? AutoCloseable)?.close()
    }
}
