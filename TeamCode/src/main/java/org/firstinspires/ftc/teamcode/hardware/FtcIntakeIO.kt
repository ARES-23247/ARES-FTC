package org.firstinspires.ftc.teamcode.hardware

import org.firstinspires.ftc.teamcode.hardware.IntakeIO
import com.qualcomm.robotcore.hardware.DcMotor
import com.qualcomm.robotcore.hardware.DcMotorEx
import com.qualcomm.robotcore.hardware.HardwareMap
import org.firstinspires.ftc.robotcore.external.navigation.CurrentUnit
import com.areslib.hardware.HardwareRegistry
import com.areslib.hardware.SyncPolledDevice
/**
 * Documentation for FtcIntakeIO
 */

class FtcIntakeIO(hardwareMap: HardwareMap) : IntakeIO, AutoCloseable {
    @Volatile private var supportsCurrentSensing = true
    private val motor: DcMotorEx? = com.areslib.ftc.hardware.CachedDcMotorEx(hardwareMap.get(DcMotorEx::class.java, "intake"))

    @Volatile private var cachedRollerAmps = 0.0
    @Volatile private var cachedRollerVelocity = 0.0
    private var lastPower = -999.0
    private val voltageSensor = hardwareMap.voltageSensor.firstOrNull()

    init {
    }

    private val servo = try { hardwareMap.servo.get("intake_pivot") } catch (e: Exception) { hardwareMap.servo.firstOrNull() }
    private var currentPos = 0.5

    override fun setPivotAngle(degrees: Double) {
        if (servo == null) return
        val targetPos = degrees
        val maxDelta = 0.02
        val clamped = targetPos.coerceIn(0.1, 0.9)
        currentPos += (clamped - currentPos).coerceIn(-maxDelta, maxDelta)
        servo.position = currentPos
    }

    override fun setPivotVoltage(volts: Double) {}

    override fun setRollerVoltage(volts: Double) {
        /**
         * Documentation for power
         */
        val vBattery = voltageSensor?.voltage ?: 12.0
        val power = (volts / vBattery).coerceIn(-1.0, 1.0)
        if (kotlin.math.abs(lastPower - power) > 1e-4) {
            try {
                motor?.power = power
                lastPower = power
            } catch (_: Exception) {}
        }
    }

    override val pivotAngleDegrees: Double
        get() = 0.0

    override val pivotCurrentAmps: Double
        get() = 0.0

    override val rollerCurrentAmps: Double
        get() = cachedRollerAmps

    override val rollerVelocityTicksPerSec: Double
        get() = cachedRollerVelocity

    override fun refresh() {
        if (motor != null) {
            // Velocity is part of REV bulk cache — zero additional I2C cost
            try { cachedRollerVelocity = motor.velocity } catch (_: Exception) { cachedRollerVelocity = 0.0 }

            if (supportsCurrentSensing) {
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
