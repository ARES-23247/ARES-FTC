package org.firstinspires.ftc.teamcode.hardware

import org.firstinspires.ftc.teamcode.hardware.IntakeIO
import com.areslib.util.RobotClock
import com.qualcomm.robotcore.hardware.DcMotorEx
import com.qualcomm.robotcore.hardware.HardwareMap
import org.firstinspires.ftc.robotcore.external.navigation.CurrentUnit



class FtcIntakeIO(hardwareMap: HardwareMap) : IntakeIO, AutoCloseable {
    @Volatile private var supportsCurrentSensing = true
    @Volatile private var lastCurrentReadTimeMs = 0L
    private val currentReadIntervalMs = 50L
    private val motor: DcMotorEx? = try {
        com.areslib.ftc.hardware.CachedDcMotorEx(hardwareMap.get(DcMotorEx::class.java, "intake"))
    } catch (_: Exception) {
        null
    }

    @Volatile private var cachedRollerAmps = 0.0

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
        get() = cachedRollerAmps

    override fun refresh() {
        if (motor != null && supportsCurrentSensing) {
            val now = RobotClock.currentTimeMillis()
            if (now - lastCurrentReadTimeMs >= currentReadIntervalMs) {
                lastCurrentReadTimeMs = now
                try {
                    cachedRollerAmps = motor.getCurrent(CurrentUnit.AMPS)
                } catch (_: Exception) {
                    supportsCurrentSensing = false
                }
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
