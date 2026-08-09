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
    private var cachedVoltage = 12.0
    private var lastPower = -999.0
    private val voltageSensor = hardwareMap.voltageSensor.firstOrNull()

    init {
    }

    private val servo = try { hardwareMap.servo.get("intake_pivot") } catch (e: Exception) { null }
    private var currentPos = 0.5
    private var lastTimeMs = -1L
    private val MAX_SERVO_SPEED = 1.0

    override fun setPivotAngle(degrees: Double) {
        if (servo == null) return
        val targetPos = (degrees / 270.0).coerceIn(0.0, 1.0)
        val now = com.areslib.util.RobotClock.currentTimeMillis()
        if (lastTimeMs < 0) {
            currentPos = servo.position
            lastTimeMs = now
        }
        val dt = (now - lastTimeMs) / 1000.0
        lastTimeMs = now

        val maxDelta = MAX_SERVO_SPEED * dt
        currentPos += (targetPos - currentPos).coerceIn(-maxDelta, maxDelta)
        servo.position = currentPos
    }

    override fun setPivotVoltage(volts: Double) {}

    override fun setRollerVoltage(volts: Double) {
        /**
         * Documentation for power
         */
        val vBattery = cachedVoltage
        val power = (volts / vBattery).coerceIn(-1.0, 1.0)
        if (kotlin.math.abs(lastPower - power) > 1e-4) {
            try {
                motor?.power = power
                lastPower = power
            } catch (_: Exception) {}
        }
    }

    private var cachedServoPosition = 0.5

    override val pivotAngleDegrees: Double
        get() = cachedServoPosition * 270.0

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
        try {
            cachedVoltage = voltageSensor?.voltage ?: 12.0
        } catch (_: Exception) {
            cachedVoltage = 12.0
        }
        try {
            cachedServoPosition = servo?.position ?: 0.5
        } catch (_: Exception) {
            // Keep last cached position on read failure
        }
    }

    override fun safe() {
        setRollerVoltage(0.0)
    }

    override fun close() {
        safe()
    }
}
