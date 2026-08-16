// ARES OWNERSHIP: USER-OWNED
// Hand-authored production example. Code generation must never replace this file.
package org.firstinspires.ftc.teamcode.subsystems

import com.areslib.hardware.actuator.PrismDriverIO
import com.areslib.state.RobotState
import com.areslib.Store
import com.areslib.subsystem.Subsystem

/**
 * Subsystem wrapper for the goBILDA Prism RGB LED Driver (SKU 3118-2855-0001).
 * Reads target pulse width in microseconds from [RobotState.superstructure.prismDrivers]
 * keyed by [name] and commands the hardware IO layer.
 *
 * [readSensors] snapshots Redux intent; [writeOutputs] applies that cached target. The power scale
 * changes brightness, not the effect pulse width, and clamps to `[0, 1]` for failure containment.
 *
 * @param io The hardware IO interface implementation.
 * @param name Hardware map name used to look up state in Redux store (default: "prism").
 * @param configuredMaxBrightness Baseline maximum brightness cap (default: 75%).
 */
class PrismSubsystem(
    private val io: PrismDriverIO,
    private val name: String = "prism",
    private val configuredMaxBrightness: Int = 75
) : Subsystem {

    init {
        require(configuredMaxBrightness in 0..100) {
            "Prism maximum brightness must be in 0..100: $configuredMaxBrightness"
        }
    }

    override fun readSensors(store: Store, timestampMs: Long) {
        // Write-only actuator subsystem; no hardware sensors to poll.
    }

    override fun writeOutputs(state: RobotState, scale: Double) {
        val targetPulseWidthUs = state.superstructure.prismDrivers[name] ?: return
        if (targetPulseWidthUs < 0) return

        // Preserve the selected effect while shedding nonessential LED power.
        val currentPowerScale = scale.coerceIn(0.0, 1.0)
        val dynamicBrightness = (configuredMaxBrightness * currentPowerScale).toInt()
        io.maxBrightnessPercent = dynamicBrightness

        io.setPulseWidthUs(targetPulseWidthUs)
    }

    override fun close() {
        (io as? AutoCloseable)?.close()
    }
}
