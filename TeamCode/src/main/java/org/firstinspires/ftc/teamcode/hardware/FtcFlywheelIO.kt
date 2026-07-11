package org.firstinspires.ftc.teamcode.hardware

import com.areslib.hardware.FlywheelIO
import com.qualcomm.robotcore.hardware.DcMotorEx
import com.qualcomm.robotcore.hardware.HardwareMap
import org.firstinspires.ftc.robotcore.external.navigation.CurrentUnit

class FtcFlywheelIO(hardwareMap: HardwareMap) : FlywheelIO, AutoCloseable {
    private val motor: DcMotorEx? = try {
        hardwareMap.get(DcMotorEx::class.java, "shooter")
    } catch (_: Exception) {
        null
    }

    // Gearing / Encoder conversion: GoBilda motor has 28 ticks per motor shaft revolution.
    // If it's a bare motor (like for a flywheel), ticksPerRev is 28.0.
    private val ticksPerRev = 28.0

    override fun setVelocityRpm(rpm: Double) {
        if (motor == null) return
        // RPM to ticks per second: (RPM / 60) * ticksPerRev
        val ticksPerSec = (rpm / 60.0) * ticksPerRev
        try {
            motor.velocity = ticksPerSec
        } catch (_: Exception) {
            // Fallback if closed loop velocity is not supported/configured
            motor.power = if (rpm > 0.0) 1.0 else 0.0
        }
    }

    override fun setAppliedVoltage(volts: Double) {
        val power = (volts / 12.0).coerceIn(-1.0, 1.0)
        motor?.power = power
    }

    override val velocityRpm: Double
        get() {
            if (motor == null) return 0.0
            // ticks per second to RPM: (ticksPerSec / ticksPerRev) * 60
            val ticksPerSec = motor.velocity
            return (ticksPerSec / ticksPerRev) * 60.0
        }

    override val currentAmps: Double
        get() = try {
            motor?.getCurrent(CurrentUnit.AMPS) ?: 0.0
        } catch (_: Exception) {
            0.0
        }

    override val tempCelsius: Double
        get() = 0.0

    override fun refresh() {}

    override fun safe() {
        setAppliedVoltage(0.0)
    }

    override fun close() {
        safe()
    }
}
