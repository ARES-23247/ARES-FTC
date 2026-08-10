package org.firstinspires.ftc.teamcode.hardware

import org.firstinspires.ftc.teamcode.hardware.FlywheelIO
import com.qualcomm.robotcore.hardware.DcMotorEx
import com.qualcomm.robotcore.hardware.HardwareMap
import org.firstinspires.ftc.robotcore.external.navigation.CurrentUnit
import com.areslib.hardware.HardwareRegistry
/**
 * Documentation for FtcFlywheelIO
 */

class FtcFlywheelIO(
    hardwareMap: HardwareMap,
    private val ticksPerRev: Double = 28.0,
    private val maxRpm: Double = 6000.0
) : FlywheelIO, AutoCloseable {
    private var supportsVelocityControl = true
    private val motor: DcMotorEx? = com.areslib.ftc.hardware.CachedDcMotorEx(hardwareMap.get(DcMotorEx::class.java, "shooter"))
    private val voltageSensor = hardwareMap.voltageSensor.firstOrNull()

    @Volatile private var cachedVelocityRpm = 0.0
    @Volatile private var cachedVelocityValid = false
    @Volatile private var cachedAmps = 0.0
    private var cachedVoltage = 12.0
    private var lastPower = -999.0

    init {
        motor?.mode = com.qualcomm.robotcore.hardware.DcMotor.RunMode.RUN_USING_ENCODER
        // The base clears REV bulk caches before refreshing registered IO, and
        // registration makes crash/stop safety reach this season-layer motor.
        HardwareRegistry.registerDevice("Flywheel", this)
    }

    override fun setVelocityRpm(rpm: Double) {
        if (motor == null) return
        val safeRpm = rpm.takeIf { it.isFinite() } ?: 0.0
        // RPM to ticks per second: (RPM / 60) * ticksPerRev
        /**
         * Documentation for ticksPerSec
         */
        val ticksPerSec = (safeRpm / 60.0) * ticksPerRev
        when {
            supportsVelocityControl && ticksPerSec.isFinite() -> {
                try {
                    motor.velocity = ticksPerSec
                } catch (_: Exception) {
                    motor.mode = com.qualcomm.robotcore.hardware.DcMotor.RunMode.RUN_WITHOUT_ENCODER
                    supportsVelocityControl = false
                    /**
                     * Documentation for power
                     */
                    val power = calculateOpenLoopPower(safeRpm)
                    if (kotlin.math.abs(lastPower - power) > 1e-4) {
                        motor.power = power
                        lastPower = power
                    }
                }
            }
            else -> {
                /**
                 * Documentation for power
                 */
                supportsVelocityControl = false
                val power = calculateOpenLoopPower(safeRpm)
                if (kotlin.math.abs(lastPower - power) > 1e-4) {
                    motor.power = power
                    lastPower = power
                }
            }
        }
    }

    override fun setAppliedVoltage(volts: Double) {
        /**
         * Documentation for power
         */
        val power = if (volts.isFinite()) (volts / 12.0).coerceIn(-1.0, 1.0) else 0.0
        if (kotlin.math.abs(lastPower - power) > 1e-4) {
            try {
                motor?.power = power
                lastPower = power
            } catch (_: Exception) {}
        }
    }

    override val velocityRpm: Double
        get() = cachedVelocityRpm

    override val velocityValid: Boolean
        get() = cachedVelocityValid

    override val currentAmps: Double
        get() = cachedAmps

    override val tempCelsius: Double
        get() = 0.0

    override fun refresh() {
        if (motor != null) {
            try {
                /**
                 * Documentation for ticksPerSec
                 */
                val ticksPerSec = motor.velocity
                val rpm = (ticksPerSec / ticksPerRev) * 60.0
                cachedVelocityValid = rpm.isFinite()
                cachedVelocityRpm = if (cachedVelocityValid) rpm else 0.0
            } catch (_: Exception) {
                cachedVelocityRpm = 0.0
                cachedVelocityValid = false
            }

            try {
                val amps = motor.getCurrent(CurrentUnit.AMPS)
                cachedAmps = if (amps.isFinite()) amps else 0.0
            } catch (_: Exception) {
                // Retry on the next refresh; transient REV/CAN failures are recoverable.
                cachedAmps = 0.0
            }
        }
        try {
            val measuredVoltage = voltageSensor?.voltage
            cachedVoltage = measuredVoltage?.takeIf { it.isFinite() && it >= 8.0 } ?: 12.0
        } catch (_: Exception) {
            cachedVoltage = 12.0
        }
    }

    private fun calculateOpenLoopPower(rpm: Double): Double {
        val safeMaxRpm = maxRpm.takeIf { it.isFinite() && it > 0.0 } ?: return 0.0
        val safeVoltage = cachedVoltage.takeIf { it.isFinite() && it >= 8.0 } ?: 12.0
        return ((rpm / safeMaxRpm) * (12.0 / safeVoltage)).coerceIn(-1.0, 1.0)
    }

    override fun safe() {
        setAppliedVoltage(0.0)
    }

    override fun close() {
        safe()
    }
}
