package org.firstinspires.ftc.teamcode.subsystems

import com.areslib.hardware.actuator.IndicatorLightIO
import com.areslib.state.RobotState
import com.areslib.Store
import com.areslib.subsystem.Subsystem

/**
 * Subsystem wrapper for a single GoBilda RGB Indicator Light.
 * Reads the target color from [RobotState.superstructure.indicatorLights]
 * keyed by [name] and writes the corresponding servo position to hardware.
 *
 * The subsystem is write-only — there are no sensors to read.
 * If no indicator light entry exists in the state map for this [name],
 * the light is left at its current position (no-op).
 *
 * @param io The hardware IO implementation (real FTC or mock).
 * @param name The hardware map name used to look up the target position in Redux state.
 */
class IndicatorLightSubsystem(
    private val io: IndicatorLightIO,
    private val name: String
) : Subsystem {

    override fun readSensors(store: Store, timestampMs: Long) {
        // Write-only device — nothing to read
    }

    override fun writeOutputs(state: RobotState, scale: Double) {
        val targetPosition = state.superstructure.indicatorLights[name]
        if (targetPosition != null) {
            io.setPosition(targetPosition)
        }
    }

    override fun close() {
        (io as? AutoCloseable)?.close()
    }
}
