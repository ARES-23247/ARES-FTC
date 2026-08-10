package org.firstinspires.ftc.teamcode.hardware

import org.firstinspires.ftc.teamcode.hardware.FlywheelIO
import com.qualcomm.robotcore.hardware.DcMotorEx
import com.qualcomm.robotcore.hardware.HardwareMap
import org.firstinspires.ftc.robotcore.external.navigation.CurrentUnit
import com.areslib.hardware.HardwareRegistry

/**
 * FTC hardware boundary for the `shooter` flywheel motor.
 *
 * [refresh] is the only sensor-read path. It snapshots encoder velocity, current, and bus
 * voltage after REV bulk-cache clearing; getters return only cached values. Velocity validity
 * is cleared on a failed/non-finite read so stale RPM cannot masquerade as fresh feedback.
 * If the controller rejects velocity commands, output switches once to voltage-compensated
 * open-loop control. Invalid commands and electrical observations fail to safe values.
 *
 * Registration with [HardwareRegistry] makes drivetrain crash safety reach this season motor.
 *
 * @param ticksPerRev encoder ticks per motor revolution.
 * @param maxRpm motor RPM represented by full open-loop output.
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
        require(ticksPerRev.isFinite() && ticksPerRev > 0.0) { "ticksPerRev must be finite and positive" }
        require(maxRpm.isFinite() && maxRpm > 0.0) { "maxRpm must be finite and positive" }
        motor?.mode = com.qualcomm.robotcore.hardware.DcMotor.RunMode.RUN_USING_ENCODER
        // The base clears REV bulk caches before refreshing registered IO, and
        // registration makes crash/stop safety reach this season-layer motor.
        HardwareRegistry.registerDevice("Flywheel", this)
    }

    override fun setVelocityRpm(rpm: Double) {
        if (motor == null) return
        val safeMaxRpm = maxRpm.takeIf { it.isFinite() && it > 0.0 } ?: 0.0
        val safeRpm = rpm.takeIf { it.isFinite() }?.coerceIn(-safeMaxRpm, safeMaxRpm) ?: 0.0
        // FTC velocity control consumes encoder ticks/second, not RPM.
        val ticksPerSec = (safeRpm / 60.0) * ticksPerRev
        when {
            supportsVelocityControl && ticksPerSec.isFinite() -> {
                try {
                    motor.velocity = ticksPerSec
                } catch (_: Exception) {
                    motor.mode = com.qualcomm.robotcore.hardware.DcMotor.RunMode.RUN_WITHOUT_ENCODER
                    supportsVelocityControl = false
                    val power = calculateOpenLoopPower(safeRpm)
                    if (kotlin.math.abs(lastPower - power) > 1e-4) {
                        motor.power = power
                        lastPower = power
                    }
                }
            }
            else -> {
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
        val busVoltage = cachedVoltage.takeIf { it.isFinite() && it >= 8.0 } ?: 12.0
        val power = if (volts.isFinite()) (volts / busVoltage).coerceIn(-1.0, 1.0) else 0.0
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

    override fun configureVelocityController(
        gains: com.areslib.control.tuning.PIDFCoefficients,
        feedforward: com.areslib.control.tuning.SimpleFeedforwardCoeffs
    ) {
        val unitScale = (2.0 * Math.PI) / (12.0 * ticksPerRev)
        val p = (gains.kP * unitScale).takeIf { it.isFinite() && it >= 0.0 } ?: return
        val i = (gains.kI * unitScale).takeIf { it.isFinite() && it >= 0.0 } ?: return
        val d = (gains.kD * unitScale).takeIf { it.isFinite() && it >= 0.0 } ?: return
        val f = (feedforward.kV * unitScale).takeIf { it.isFinite() && it >= 0.0 } ?: return
        try {
            motor?.setPIDFCoefficients(
                com.qualcomm.robotcore.hardware.DcMotor.RunMode.RUN_USING_ENCODER,
                com.qualcomm.robotcore.hardware.PIDFCoefficients(p, i, d, f)
            )
        } catch (_: Exception) {
            supportsVelocityControl = false
        }
    }

    private fun calculateOpenLoopPower(rpm: Double): Double {
        // Preserve nominal-12-V duty without exceeding the FTC power range.
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
