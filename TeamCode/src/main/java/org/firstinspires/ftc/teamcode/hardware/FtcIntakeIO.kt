package org.firstinspires.ftc.teamcode.hardware

import com.areslib.hardware.IntakeIO
import com.qualcomm.robotcore.hardware.DcMotorEx
import com.qualcomm.robotcore.hardware.HardwareMap
import org.firstinspires.ftc.robotcore.external.navigation.CurrentUnit

class FtcIntakeIO(hardwareMap: HardwareMap) : IntakeIO, AutoCloseable {
    private val motor: DcMotorEx? = try {
        hardwareMap.get(DcMotorEx::class.java, "intake")
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

    override val rollerCurrentAmps: Double
        get() = try {
            motor?.getCurrent(CurrentUnit.AMPS) ?: 0.0
        } catch (_: Exception) {
            0.0
        }

    override fun refresh() {}

    override fun safe() {
        setRollerVoltage(0.0)
    }

    override fun close() {
        safe()
    }
}
