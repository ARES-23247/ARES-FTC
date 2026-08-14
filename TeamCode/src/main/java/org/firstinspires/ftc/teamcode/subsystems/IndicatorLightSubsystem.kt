// ARES OWNERSHIP: USER-OWNED
// Hand-authored production example. Code generation must never replace this file.
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
 *
 * @param io The hardware IO implementation (real FTC or mock).
 * @param name The hardware map name used to look up the target position in Redux state.
 */
class IndicatorLightSubsystem(
    private val io: IndicatorLightIO,
    private val name: String
) : Subsystem {

    override fun readSensors(store: Store, timestampMs: Long) {
        // Write-only actuator subsystem; no hardware sensors to poll.
    }

    override fun writeOutputs(state: RobotState, scale: Double) {
        val targetPosition = state.superstructure.indicatorLights[name] ?: return
        if (targetPosition.isNaN()) return
        if (targetPosition < 0.0) {
            // Negative is the RAINBOW sentinel; RobotClock keeps replay deterministic.
            val nowMs = com.areslib.util.RobotClock.currentTimeMillis()
            // Offset the second indicator by 500 ms to produce a visible wave.
            val offset = if (name.contains("2")) 500L else 0L
            val cycleTimeMs = 2500.0
            val progress = ((nowMs + offset) % cycleTimeMs.toLong()) / cycleTimeMs
            val minPos = com.areslib.hardware.actuator.IndicatorLightColor.RED.position
            val maxPos = com.areslib.hardware.actuator.IndicatorLightColor.PURPLE.position
            val sweep = if (progress < 0.5) progress * 2.0 else 2.0 * (1.0 - progress)
            val rainbowPos = minPos + (maxPos - minPos) * sweep
            io.setPosition(rainbowPos)
        } else {
            io.setPosition(targetPosition.coerceIn(0.0, 1.0))
        }
    }

    override fun close() {
        (io as? AutoCloseable)?.close()
    }
}
