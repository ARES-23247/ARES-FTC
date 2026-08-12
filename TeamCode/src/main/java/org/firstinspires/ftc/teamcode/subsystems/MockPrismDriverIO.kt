// ARES OWNERSHIP: USER-OWNED
// Hand-authored simulator adapter. Code generation must never replace this file.
package org.firstinspires.ftc.teamcode.subsystems

import com.areslib.hardware.actuator.PrismDriverIO
import com.areslib.hardware.actuator.PrismPwmPreset

/**
 * Deterministic desktop implementation of the goBILDA Prism IO contract.
 *
 * The mock applies the same 500-2500 microsecond pulse-width bounds and red close/safe indication as
 * the FTC PWM adapter. Public fields make applied output and cleanup observable without hardware.
 */
class MockPrismDriverIO : PrismDriverIO, AutoCloseable {
    override var currentPulseWidthUs: Int = PrismPwmPreset.RAINBOW_FULL_COLOR.pulseWidthUs
        private set

    override var maxBrightnessPercent: Int = 75

    var isClosed: Boolean = false
        private set

    override fun setPulseWidthUs(pulseWidthUs: Int) {
        currentPulseWidthUs = pulseWidthUs.coerceIn(MIN_PULSE_WIDTH_US, MAX_PULSE_WIDTH_US)
    }

    override fun setSolidColorRgb(r: Int, g: Int, b: Int) {
        val red = r.coerceIn(0, 255) / 255.0
        val green = g.coerceIn(0, 255) / 255.0
        val blue = b.coerceIn(0, 255) / 255.0
        val maximum = maxOf(red, green, blue)
        val minimum = minOf(red, green, blue)
        val delta = maximum - minimum
        val hue = when {
            delta < 1e-4 -> 0.0
            maximum == red -> ((green - blue) / delta) % 6.0
            maximum == green -> ((blue - red) / delta) + 2.0
            else -> ((red - green) / delta) + 4.0
        } * 60.0
        val normalizedHue = (if (hue < 0.0) hue + 360.0 else hue) / 360.0
        setPulseWidthUs((SOLID_COLOR_MIN_US + normalizedHue * SOLID_COLOR_SPAN_US).toInt())
    }

    override fun safe() {
        setPreset(PrismPwmPreset.SOLID_RED)
    }

    override fun close() {
        safe()
        isClosed = true
    }

    private companion object {
        const val MIN_PULSE_WIDTH_US = 500
        const val MAX_PULSE_WIDTH_US = 2500
        const val SOLID_COLOR_MIN_US = 1050
        const val SOLID_COLOR_SPAN_US = 899.0
    }
}
