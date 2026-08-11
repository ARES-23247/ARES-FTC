package org.firstinspires.ftc.teamcode.hardware

import org.firstinspires.ftc.teamcode.hardware.IntakeIO
import com.qualcomm.robotcore.hardware.DcMotor
import com.qualcomm.robotcore.hardware.DcMotorEx
import com.qualcomm.robotcore.hardware.HardwareMap
import org.firstinspires.ftc.robotcore.external.navigation.CurrentUnit
import com.areslib.hardware.HardwareRegistry

/**
 * Cached FTC hardware boundary for the `intake` roller motor.
 *
 * The current DECODE robot has no intake pivot, so pivot commands intentionally no-op. [refresh]
 * performs the sole velocity/current/voltage reads after REV bulk-cache clearing. Getters expose
 * only cached values, and failed/non-finite current is marked invalid so stall detection cannot
 * latch stale current. Output voltage uses the last valid battery sample, falling back to 12 V.
 */
class FtcIntakeIO(hardwareMap: HardwareMap) : IntakeIO, AutoCloseable {
    private val motor: DcMotorEx? = com.areslib.ftc.hardware.CachedDcMotorEx(hardwareMap.get(DcMotorEx::class.java, "intake"))

    @Volatile private var cachedRollerAmps = 0.0
    @Volatile private var cachedRollerCurrentValid = false
    @Volatile private var cachedRollerVelocity = 0.0
    private var cachedVoltage = 12.0
    private var lastPower = -999.0
    private val voltageSensor = hardwareMap.voltageSensor.firstOrNull()

    init {
        // The base clears REV bulk caches before refreshing registered IO, and
        // registration makes crash/stop safety reach this season-layer motor.
        HardwareRegistry.registerDevice("Intake", this)
    }

    override fun setPivotAngle(degrees: Double) {}
    override fun setPivotVoltage(volts: Double) {}

    override fun setRollerVoltage(volts: Double) {
        val safeVolts = volts.takeIf { it.isFinite() } ?: 0.0
        val vBattery = cachedVoltage.takeIf { it.isFinite() && it >= MIN_VALID_VOLTAGE } ?: NOMINAL_VOLTAGE
        val power = (safeVolts / vBattery).coerceIn(-1.0, 1.0)
        if (kotlin.math.abs(lastPower - power) > 1e-4) {
            try {
                motor?.power = power
                lastPower = power
            } catch (_: Exception) {}
        }
    }

    override val rollerCurrentAmps: Double
        get() = cachedRollerAmps

    override val rollerCurrentValid: Boolean
        get() = cachedRollerCurrentValid

    override val rollerVelocityTicksPerSec: Double
        get() = cachedRollerVelocity

    override fun refresh() {
        if (motor != null) {
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
        try {
            val measuredVoltage = voltageSensor?.voltage ?: 12.0
            cachedVoltage = measuredVoltage.takeIf { it.isFinite() && it >= MIN_VALID_VOLTAGE }
                ?: NOMINAL_VOLTAGE
        } catch (_: Exception) {
            cachedVoltage = NOMINAL_VOLTAGE
        }
    }

    override fun safe() {
        setRollerVoltage(0.0)
    }

    override fun close() {
        safe()
    }

    private companion object {
        const val NOMINAL_VOLTAGE = 12.0
        const val MIN_VALID_VOLTAGE = 8.0
    }
}
