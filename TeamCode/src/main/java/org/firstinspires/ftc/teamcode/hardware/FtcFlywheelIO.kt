package org.firstinspires.ftc.teamcode.hardware

import org.firstinspires.ftc.teamcode.hardware.FlywheelIO
import com.qualcomm.robotcore.hardware.DcMotorEx
import com.qualcomm.robotcore.hardware.HardwareMap
import org.firstinspires.ftc.robotcore.external.navigation.CurrentUnit
import com.areslib.hardware.HardwareRegistry
import com.areslib.hardware.SyncPolledDevice
/**
 * Documentation for FtcFlywheelIO
 */

class FtcFlywheelIO(hardwareMap: HardwareMap) : FlywheelIO, AutoCloseable {
    private var supportsVelocityControl = true
    @Volatile private var supportsCurrentSensing = true
    private val motor: DcMotorEx? = com.areslib.ftc.hardware.CachedDcMotorEx(hardwareMap.get(DcMotorEx::class.java, "shooter"))
    private val voltageSensor = hardwareMap.voltageSensor.firstOrNull()

    // Gearing / Encoder conversion: GoBilda motor has 28 ticks per motor shaft revolution.
    // If it's a bare motor (like for a flywheel), ticksPerRev is 28.0.
    private val ticksPerRev = 28.0

    @Volatile private var cachedVelocityRpm = 0.0
    @Volatile private var cachedAmps = 0.0
    private var cachedVoltage = 12.0
    private var lastPower = -999.0

    private val maxRpm = 6000.0

    init {
        motor?.mode = com.qualcomm.robotcore.hardware.DcMotor.RunMode.RUN_USING_ENCODER
    }

    override fun setVelocityRpm(rpm: Double) {
        if (motor == null) return
        // RPM to ticks per second: (RPM / 60) * ticksPerRev
        /**
         * Documentation for ticksPerSec
         */
        val ticksPerSec = (rpm / 60.0) * ticksPerRev
        when {
            supportsVelocityControl -> {
                try {
                    motor.velocity = ticksPerSec
                } catch (_: Exception) {
                    motor?.mode = com.qualcomm.robotcore.hardware.DcMotor.RunMode.RUN_WITHOUT_ENCODER
                    supportsVelocityControl = false
                    /**
                     * Documentation for power
                     */
                    val vBattery = cachedVoltage
                    val power = ((rpm / maxRpm) * (12.0 / vBattery)).coerceIn(-1.0, 1.0)
                    if (kotlin.math.abs(lastPower - power) > 1e-4) {
                        motor?.power = power
                        lastPower = power
                    }
                }
            }
            else -> {
                /**
                 * Documentation for power
                 */
                val vBattery = cachedVoltage
                val power = ((rpm / maxRpm) * (12.0 / vBattery)).coerceIn(-1.0, 1.0)
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
        val power = (volts / 12.0).coerceIn(-1.0, 1.0)
        if (kotlin.math.abs(lastPower - power) > 1e-4) {
            try {
                motor?.power = power
                lastPower = power
            } catch (_: Exception) {}
        }
    }

    override val velocityRpm: Double
        get() = cachedVelocityRpm

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
                cachedVelocityRpm = (ticksPerSec / ticksPerRev) * 60.0
            } catch (_: Exception) {}
            
            if (supportsCurrentSensing) {
                try {
                    cachedAmps = motor.getCurrent(CurrentUnit.AMPS)
                } catch (_: Exception) {
                    supportsCurrentSensing = false
                }
            }
        }
        try {
            cachedVoltage = voltageSensor?.voltage?.coerceAtLeast(8.0) ?: 12.0
        } catch (e: Exception) {
            cachedVoltage = 12.0
        }
    }

    override fun safe() {
        setAppliedVoltage(0.0)
    }

    override fun close() {
        safe()
    }
}
