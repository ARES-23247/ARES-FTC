package org.firstinspires.ftc.teamcode.subsystems

import com.areslib.Store
import com.areslib.state.RobotState
import com.areslib.subsystem.Subsystem
import org.firstinspires.ftc.teamcode.hardware.IntakeIO
import org.firstinspires.ftc.teamcode.hardware.FlywheelIO
import org.firstinspires.ftc.teamcode.dsl.IntakeState
import org.firstinspires.ftc.teamcode.dsl.FlywheelState
import org.firstinspires.ftc.teamcode.dsl.intakeActive
import org.firstinspires.ftc.teamcode.dsl.flywheelActive
import org.firstinspires.ftc.teamcode.dsl.flywheelTargetRPM
import com.areslib.action.RobotAction

class IntakeSubsystem(private val io: IntakeIO) : Subsystem {
    override fun readSensors(store: Store, timestampMs: Long) {
        io.refresh()
    }

    override fun writeOutputs(state: RobotState, scale: Double) {
        val active = state.superstructure.intakeActive
        val voltage = if (active) state.tuning.intakeNominalVoltage * scale else 0.0
        io.setRollerVoltage(voltage)
    }

    override fun close() {
        (io as? AutoCloseable)?.close()
    }
}

class FlywheelSubsystem(private val io: FlywheelIO) : Subsystem {
    override fun readSensors(store: Store, timestampMs: Long) {
        io.refresh()
        val superstructure = store.state.superstructure
        if (superstructure.has(FlywheelState::class.java)) {
            val current = superstructure.get(FlywheelState::class.java)
            if (kotlin.math.abs(current.currentRPM - io.velocityRpm) > 5.0) {
                store.dispatch(
                    RobotAction.UpdateSubsystemState(
                        current.copy(currentRPM = io.velocityRpm),
                        timestampMs
                    )
                )
            }
        }
    }

    override fun writeOutputs(state: RobotState, scale: Double) {
        val active = state.superstructure.flywheelActive
        if (active) {
            val targetRpm = state.superstructure.flywheelTargetRPM
            io.setVelocityRpm(targetRpm)
        } else {
            io.setVelocityRpm(0.0)
        }
    }

    override fun close() {
        (io as? AutoCloseable)?.close()
    }
}
