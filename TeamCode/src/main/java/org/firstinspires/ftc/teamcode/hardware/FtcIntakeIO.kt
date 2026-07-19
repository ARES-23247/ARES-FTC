package org.firstinspires.ftc.teamcode.hardware

import org.firstinspires.ftc.teamcode.hardware.IntakeIO
import com.qualcomm.robotcore.hardware.DcMotor
import com.qualcomm.robotcore.hardware.DcMotorEx
import com.qualcomm.robotcore.hardware.HardwareMap
import org.firstinspires.ftc.robotcore.external.navigation.CurrentUnit
import com.areslib.hardware.HardwareRegistry
import com.areslib.hardware.SyncPolledDevice

class FtcIntakeIO(hardwareMap: HardwareMap) : IntakeIO, SyncPolledDevice, AutoCloseable {
    @Volatile private var supportsCurrentSensing = true
    private val motor: DcMotorEx? = try {
        com.areslib.ftc.hardware.CachedDcMotorEx(hardwareMap.get(DcMotorEx::class.java, "intake"))
    } catch (_: Exception) {
        null
    }

    @Volatile private var cachedRollerAmps = 0.0
    private var lastPower = -999.0

    init {
        HardwareRegistry.registerRoundRobinDevice(this)
    }

    override fun pollSync() {
        if (motor != null && supportsCurrentSensing) {
            try {
                cachedRollerAmps = motor.getCurrent(CurrentUnit.AMPS)
            } catch (_: Exception) {
                supportsCurrentSensing = false
            }
        }
    }

    override fun setPivotAngle(degrees: Double) {}

    override fun setPivotVoltage(volts: Double) {}

    override fun setRollerVoltage(volts: Double) {
        val power = (volts / 12.0).coerceIn(-1.0, 1.0)
        if (kotlin.math.abs(lastPower - power) > 1e-4) {
            motor?.power = power
            lastPower = power
        }
    }

    override val pivotAngleDegrees: Double
        get() = 0.0

    override val pivotCurrentAmps: Double
        get() = 0.0

    override val rollerCurrentAmps: Double
        get() = cachedRollerAmps

    override fun refresh() {
        // Cached values are updated in the background coroutine
    }

    override fun safe() {
        setRollerVoltage(0.0)
    }

    override fun close() {
        safe()
    }
}
