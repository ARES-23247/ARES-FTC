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
 * @param io The hardware IO interface implementation.
 * @param name Hardware map name used to look up state in Redux store (default: "prism").
 */
class PrismSubsystem(
    private val io: PrismDriverIO,
    private val name: String = "prism"
) : Subsystem {

    override fun readSensors(store: Store, timestampMs: Long) {
        // Write-only device — no sensor reads needed
    }

    override fun writeOutputs(state: RobotState, scale: Double) {
        val targetPulseWidthUs = state.superstructure.prismDrivers[name] ?: return
        io.setPulseWidthUs(targetPulseWidthUs)
    }

    override fun close() {
        (io as? AutoCloseable)?.close()
    }
}
