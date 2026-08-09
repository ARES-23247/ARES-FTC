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
 * Automatically integrates with robot power management: dynamically scales LED brightness
 * down whenever power scale drops below 1.0 during high current draw or battery voltage dips.
 *
 * @param io The hardware IO interface implementation.
 * @param name Hardware map name used to look up state in Redux store (default: "prism").
 * @param configuredMaxBrightness Baseline maximum brightness cap (default: 75%).
 */
class PrismSubsystem(
    private val io: PrismDriverIO,
    private val name: String = "prism",
    var configuredMaxBrightness: Int = 75
) : Subsystem {

    private var cachedPulseWidthUs: Int? = null

    override fun readSensors(store: Store, timestampMs: Long) {
        cachedPulseWidthUs = store.state.superstructure.prismDrivers[name]
    }

    override fun writeOutputs(state: RobotState, scale: Double) {
        val targetPulseWidthUs = cachedPulseWidthUs ?: return

        // Dynamically scale LED brightness down during high current draw or battery dips (scale < 1.0)
        val currentPowerScale = scale.coerceIn(0.0, 1.0)
        val dynamicBrightness = (configuredMaxBrightness * currentPowerScale).toInt()
        io.maxBrightnessPercent = dynamicBrightness

        io.setPulseWidthUs(targetPulseWidthUs)
    }

    override fun close() {
        (io as? AutoCloseable)?.close()
    }
}
