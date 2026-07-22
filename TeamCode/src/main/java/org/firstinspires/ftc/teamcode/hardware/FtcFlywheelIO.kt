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

class FtcFlywheelIO(hardwareMap: HardwareMap) : FlywheelIO, SyncPolledDevice, AutoCloseable {
    private var supportsVelocityControl = true
    @Volatile private var supportsCurrentSensing = true
    private val motor: DcMotorEx? = try {
        com.areslib.ftc.hardware.CachedDcMotorEx(hardwareMap.get(DcMotorEx::class.java, "shooter"))
    } catch (_: Exception) {
        null
    }

    // Gearing / Encoder conversion: GoBilda motor has 28 ticks per motor shaft revolution.
    // If it's a bare motor (like for a flywheel), ticksPerRev is 28.0.
    private val ticksPerRev = 28.0

    @Volatile private var cachedVelocityRpm = 0.0
    @Volatile private var cachedAmps = 0.0
    private var lastPower = -999.0

    init {
        HardwareRegistry.registerRoundRobinDevice(this)
    }

    override fun pollSync() {
        if (motor != null && supportsCurrentSensing) {
            try {
                cachedAmps = motor.getCurrent(CurrentUnit.AMPS)
            } catch (_: Exception) {
                supportsCurrentSensing = false
            }
        }
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
                    supportsVelocityControl = false
                    /**
                     * Documentation for power
                     */
                    val power = if (rpm > 0.0) 1.0 else 0.0
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
                val power = if (rpm > 0.0) 1.0 else 0.0
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
        try {
            motor?.power = power
        } catch (_: Exception) {}
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
        }
    }

    override fun safe() {
        setAppliedVoltage(0.0)
    }

    override fun close() {
        safe()
    }
}
