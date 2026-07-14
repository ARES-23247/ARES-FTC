package org.firstinspires.ftc.teamcode.hardware

import org.firstinspires.ftc.teamcode.hardware.IntakeIO
import com.qualcomm.robotcore.hardware.DcMotorEx
import com.qualcomm.robotcore.hardware.HardwareMap
import org.firstinspires.ftc.robotcore.external.navigation.CurrentUnit

class FtcIntakeIO(hardwareMap: HardwareMap) : IntakeIO, AutoCloseable {
    private val motor: DcMotorEx? = try {
        com.areslib.ftc.hardware.CachedDcMotorEx(hardwareMap.get(DcMotorEx::class.java, "intake"))
    } catch (_: Exception) {
        null
    }

    override fun setPivotAngle(degrees: Double) {}

    override fun setPivotVoltage(volts: Double) {}

    override fun setRollerVoltage(volts: Double) {
        val power = (volts / 12.0).coerceIn(-1.0, 1.0)
        motor?.power = power
    }

    override val pivotAngleDegrees: Double
        get() = 0.0

    override val pivotCurrentAmps: Double
        get() = 0.0

    private var cachedRollerAmps = 0.0
    private var loopCounter = 0

    override val rollerCurrentAmps: Double
        get() = cachedRollerAmps

    override fun refresh() {
        loopCounter++
        if (loopCounter % 10 == 0) {
            try {
                cachedRollerAmps = motor?.getCurrent(CurrentUnit.AMPS) ?: 0.0
            } catch (_: Exception) {
                cachedRollerAmps = 0.0
            }
        }
    }

    override fun safe() {
        setRollerVoltage(0.0)
    }

    override fun close() {
        safe()
    }
}
