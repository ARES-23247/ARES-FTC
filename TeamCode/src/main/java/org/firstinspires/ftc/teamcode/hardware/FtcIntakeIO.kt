package org.firstinspires.ftc.teamcode.hardware

import com.areslib.hardware.actuator.IntakeIO
import com.qualcomm.robotcore.hardware.DcMotor
import com.qualcomm.robotcore.hardware.DcMotorEx
import com.qualcomm.robotcore.hardware.HardwareMap
import org.firstinspires.ftc.robotcore.external.navigation.CurrentUnit
import com.areslib.hardware.HardwareRegistry

/**
 * Cached FTC hardware boundary for the `intake` roller motor.
 *
 * The current DECODE robot has no intake pivot, so pivot commands intentionally no-op. [refresh]
 * performs the sole velocity/current reads after REV bulk-cache clearing. Getters expose
 * only cached values, and failed/non-finite current is marked invalid so stall detection cannot
 * latch stale current. Output voltage uses the shared power manager's cached sample.
 */
class FtcIntakeIO(
    hardwareMap: HardwareMap,
    private val batteryVoltageSupplier: () -> Double = { NOMINAL_VOLTAGE },
) : IntakeIO, AutoCloseable {
    // This boundary owns output caching so failed writes cannot poison a decorator's last-value
    // cache and bypass the neutral-first recovery latch.
    private val motor: DcMotorEx = requireNotNull(hardwareMap.get(DcMotorEx::class.java, "intake")) {
        "Required FTC motor 'intake' was not discovered"
    }

    @Volatile private var cachedRollerAmps = 0.0
    @Volatile private var cachedRollerCurrentValid = false
    @Volatile private var cachedRollerVelocity = 0.0
    private var lastPower = UNKNOWN_POWER
    private var outputFaulted = false
    @Volatile
    internal var outputApplied = false
        private set

    init {
        // The base clears REV bulk caches before refreshing registered IO, and
        // registration makes crash/stop safety reach this season-layer motor.
        HardwareRegistry.registerDevice("Intake", this)
    }

    override fun setPivotAngle(degrees: Double) {}
    override fun setPivotVoltage(volts: Double) {}

    override fun setRollerVoltage(volts: Double) {
        val safeVolts = volts.takeIf { it.isFinite() } ?: 0.0
        val suppliedVoltage = try { batteryVoltageSupplier() } catch (_: Throwable) { Double.NaN }
        val vBattery = suppliedVoltage.takeIf { it.isFinite() && it >= MIN_VALID_VOLTAGE } ?: NOMINAL_VOLTAGE
        val power = (safeVolts / vBattery).coerceIn(-1.0, 1.0)
        if (power == 0.0) {
            if (!outputFaulted && lastPower == 0.0) return
            // Recovery is deliberately neutral-first: a failed nonzero write can only re-arm after
            // a later explicit zero command successfully reaches the hub.
            val wasFaulted = outputFaulted
            if (writePower(0.0)) {
                if (wasFaulted) outputFaulted = false
            } else {
                // A failed STOP leaves the previous nonzero command physically unconfirmed. It is
                // a safety fault just like a failed motion write and must block every later
                // nonzero request until a distinct neutral command succeeds.
                outputFaulted = true
            }
            return
        }
        if (outputFaulted) {
            writePower(0.0)
            return
        }
        if (kotlin.math.abs(lastPower - power) > 1e-4 && !writePower(power)) {
            outputFaulted = true
            lastPower = UNKNOWN_POWER
            writePower(0.0)
        }
    }

    private fun writePower(power: Double): Boolean = try {
        motor.power = power
        lastPower = power
        outputApplied = power != 0.0
        true
    } catch (_: Exception) {
        lastPower = UNKNOWN_POWER
        false
    }

    override val rollerCurrentAmps: Double
        get() = cachedRollerAmps

    override val rollerCurrentValid: Boolean
        get() = cachedRollerCurrentValid

    /**
     * This robot has only the roller branch represented by [IntakeIO]. Do not inherit the
     * dual-branch aggregate, whose absent pivot current is intentionally unknown and would make
     * an otherwise valid roller observation disappear from the system power budget.
     */
    override val currentAmps: Double
        get() = cachedRollerAmps

    override fun isCurrentReadingValid(readingAmps: Double): Boolean =
        cachedRollerCurrentValid && readingAmps.isFinite() && readingAmps >= 0.0

    override val rollerVelocityTicksPerSec: Double
        get() = cachedRollerVelocity

    override fun refresh() {
        // Velocity comes from the already-refreshed REV bulk response.
        try { cachedRollerVelocity = motor.velocity } catch (_: Exception) { cachedRollerVelocity = 0.0 }

        try {
            val current = motor.getCurrent(CurrentUnit.AMPS)
            cachedRollerCurrentValid = current.isFinite() && current >= 0.0
            cachedRollerAmps = if (cachedRollerCurrentValid) current else 0.0
        } catch (_: Exception) {
            // A transient hub/CAN read failure must not retain a stale overcurrent
            // or permanently disable future current sensing attempts.
            cachedRollerAmps = 0.0
            cachedRollerCurrentValid = false
        }
    }

    override fun safe() {
        setRollerVoltage(0.0)
    }

    override fun close() {
        safe()
    }

    private companion object {
        const val UNKNOWN_POWER = -999.0
        const val NOMINAL_VOLTAGE = 12.0
        const val MIN_VALID_VOLTAGE = 8.0
    }
}
