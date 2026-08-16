package org.firstinspires.ftc.teamcode.hardware

import com.areslib.hardware.actuator.FlywheelIO
import com.qualcomm.robotcore.hardware.DcMotorEx
import com.qualcomm.robotcore.hardware.HardwareMap
import org.firstinspires.ftc.robotcore.external.navigation.CurrentUnit
import com.areslib.hardware.HardwareRegistry

/**
 * FTC hardware boundary for the `shooter` flywheel motor.
 *
 * [refresh] is the only hardware sensor-read path. It snapshots encoder velocity and current
 * after REV bulk-cache clearing; bus voltage comes from the shared cached power-manager sample.
 * Velocity validity is cleared on a failed/non-finite read so stale RPM cannot masquerade as
 * fresh feedback.
 * If the controller rejects a velocity command, output stops immediately and remains disarmed
 * until a healthy feedback sample is followed by an explicit zero-output command. Reduced-effort
 * velocity requests use bounded software feedback only while the cached encoder sample is valid;
 * they never degrade silently to feedforward-only open-loop output.
 *
 * Registration with [HardwareRegistry] makes drivetrain crash safety reach this season motor.
 *
 * @param ticksPerRev encoder ticks per motor revolution.
 * @param maxRpm motor RPM represented by the nominal 12 V software-loop feedforward.
 */
class FtcFlywheelIO(
    hardwareMap: HardwareMap,
    private val ticksPerRev: Double = 28.0,
    private val maxRpm: Double = 6000.0,
    private val batteryVoltageSupplier: () -> Double = { NOMINAL_VOLTAGE },
) : FlywheelIO, AutoCloseable {
    private var velocityControlFaulted = false
    // This boundary owns its output cache. Wrapping it in CachedDcMotorEx is unsafe because that
    // decorator cannot observe velocity-mode commands and may suppress a later power=0 hard stop.
    private val motor: DcMotorEx = requireNotNull(hardwareMap.get(DcMotorEx::class.java, "shooter")) {
        "Required FTC motor 'shooter' was not discovered"
    }

    @Volatile private var cachedVelocityRpm = 0.0
    @Volatile private var cachedVelocityValid = false
    @Volatile private var cachedAmps = 0.0
    @Volatile private var cachedCurrentValid = false
    private var lastPower = UNKNOWN_POWER
    @Volatile
    internal var outputApplied = false
        private set

    init {
        require(ticksPerRev.isFinite() && ticksPerRev > 0.0) { "ticksPerRev must be finite and positive" }
        require(maxRpm.isFinite() && maxRpm > 0.0) { "maxRpm must be finite and positive" }
        motor.mode = com.qualcomm.robotcore.hardware.DcMotor.RunMode.RUN_USING_ENCODER
        // The base clears REV bulk caches before refreshing registered IO, and
        // registration makes crash/stop safety reach this season-layer motor.
        HardwareRegistry.registerDevice("Flywheel", this)
    }

    override fun setVelocityRpm(rpm: Double) {
        if (!rpm.isFinite()) {
            stopMotor(allowRecovery = false)
            return
        }
        val safeRpm = rpm.coerceIn(-maxRpm, maxRpm)
        if (safeRpm == 0.0) {
            stopMotor(allowRecovery = true)
            return
        }
        if (velocityControlFaulted) {
            stopMotor(allowRecovery = false)
            return
        }

        // FTC velocity control consumes encoder ticks/second, not RPM.
        val ticksPerSec = (safeRpm / 60.0) * ticksPerRev
        try {
            motor.velocity = ticksPerSec
            // A velocity-mode write can make the motor move even if the last raw-power command
            // was zero. Invalidate the cache so the next stop always reaches the controller.
            lastPower = UNKNOWN_POWER
            outputApplied = true
        } catch (_: Exception) {
            velocityControlFaulted = true
            lastPower = UNKNOWN_POWER
            stopMotor(allowRecovery = false)
        }
    }

    /**
     * Retains the requested RPM while bounding the voltage available to the software velocity
     * loop. FTC's DcMotorEx velocity API has no independent maximum-effort parameter, so partial
     * brownout operation uses feedforward plus cached velocity error. Full-effort commands return
     * to the hub's closed-loop controller only after an explicit healthy zero-output recovery.
     */
    override fun setVelocityRpm(rpm: Double, maxEffortScale: Double) {
        if (!rpm.isFinite() || !maxEffortScale.isFinite()) {
            stopMotor(allowRecovery = false)
            return
        }
        val effortScale = maxEffortScale.coerceIn(0.0, 1.0)
        val safeRpm = rpm.coerceIn(-maxRpm, maxRpm)
        if (effortScale >= FULL_EFFORT_THRESHOLD) {
            setVelocityRpm(safeRpm)
            return
        }
        if (effortScale <= 0.0 || safeRpm == 0.0) {
            stopMotor(allowRecovery = true)
            return
        }
        if (velocityControlFaulted || !cachedVelocityValid) {
            stopMotor(allowRecovery = false)
            return
        }

        // This is an explicit bounded software feedback loop, not an open-loop fallback: a fresh
        // velocity observation is mandatory and loss of that observation commands a hard stop.
        val feedforwardVolts = safeRpm / maxRpm * NOMINAL_VOLTAGE
        val feedbackVolts = (safeRpm - cachedVelocityRpm) * VELOCITY_ERROR_VOLTS_PER_RPM
        val maxEffortVolts = NOMINAL_VOLTAGE * effortScale
        setAppliedVoltage((feedforwardVolts + feedbackVolts).coerceIn(-maxEffortVolts, maxEffortVolts))
    }

    override fun setAppliedVoltage(volts: Double) {
        if (!volts.isFinite()) {
            stopMotor(allowRecovery = false)
            return
        }
        if (volts == 0.0) {
            stopMotor(allowRecovery = true)
            return
        }
        // A failed velocity/PIDF/raw-power transaction disarms every flywheel output path. SysId
        // and reduced-effort control must not bypass the same neutral-first recovery contract by
        // switching to a nonzero raw-voltage command on the next frame.
        if (velocityControlFaulted) {
            stopMotor(allowRecovery = false)
            return
        }
        val suppliedVoltage = try { batteryVoltageSupplier() } catch (_: Throwable) { Double.NaN }
        val busVoltage = suppliedVoltage.takeIf { it.isFinite() && it >= MIN_VALID_VOLTAGE }
            ?: NOMINAL_VOLTAGE
        val power = (volts / busVoltage).coerceIn(-1.0, 1.0)
        if (kotlin.math.abs(lastPower - power) > 1e-4) {
            try {
                motor.power = power
                lastPower = power
                outputApplied = true
            } catch (_: Exception) {
                velocityControlFaulted = true
                lastPower = UNKNOWN_POWER
                stopMotor(allowRecovery = false)
            }
        }
    }

    override val velocityRpm: Double
        get() = cachedVelocityRpm

    override val velocityValid: Boolean
        get() = cachedVelocityValid && !velocityControlFaulted

    override val currentAmps: Double
        get() = cachedAmps

    override val currentReadingValid: Boolean
        get() = cachedCurrentValid

    override fun isCurrentReadingValid(readingAmps: Double): Boolean =
        cachedCurrentValid && readingAmps.isFinite() && readingAmps >= 0.0

    override val tempCelsius: Double
        get() = Double.NaN

    override fun refresh() {
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
            cachedCurrentValid = amps.isFinite() && amps >= 0.0
            cachedAmps = if (cachedCurrentValid) amps else 0.0
        } catch (_: Exception) {
            // Retry on the next refresh; transient REV/CAN failures are recoverable.
            cachedAmps = 0.0
            cachedCurrentValid = false
        }
    }

    override fun configureVelocityController(
        gains: com.areslib.control.tuning.PIDFCoefficients,
        feedforward: com.areslib.control.tuning.SimpleFeedforwardCoeffs
    ) {
        // All-zero is the shared "not calibrated" sentinel, not a valid controller profile. Never
        // replace the REV controller's working defaults merely because tuning has not loaded yet.
        if (gains.kP == 0.0 && gains.kI == 0.0 && gains.kD == 0.0 && gains.kF == 0.0 &&
            feedforward.kS == 0.0 && feedforward.kV == 0.0 && feedforward.kA == 0.0
        ) return
        val unitScale = (2.0 * Math.PI) / (12.0 * ticksPerRev)
        val p = (gains.kP * unitScale).takeIf { it.isFinite() && it >= 0.0 } ?: return
        val i = (gains.kI * unitScale).takeIf { it.isFinite() && it >= 0.0 } ?: return
        val d = (gains.kD * unitScale).takeIf { it.isFinite() && it >= 0.0 } ?: return
        val f = (feedforward.kV * unitScale).takeIf { it.isFinite() && it >= 0.0 } ?: return
        // kS/kA cannot be represented by the REV PIDF slot. A profile containing only those
        // terms must not be collapsed into a destructive all-zero controller write.
        if (p == 0.0 && i == 0.0 && d == 0.0 && f == 0.0) return
        try {
            motor.setPIDFCoefficients(
                com.qualcomm.robotcore.hardware.DcMotor.RunMode.RUN_USING_ENCODER,
                com.qualcomm.robotcore.hardware.PIDFCoefficients(p, i, d, f)
            )
        } catch (_: Exception) {
            velocityControlFaulted = true
            lastPower = UNKNOWN_POWER
            stopMotor(allowRecovery = false)
        }
    }

    private fun stopMotor(allowRecovery: Boolean) {
        var stopSucceeded = true
        if (lastPower != 0.0) {
            try {
                motor.power = 0.0
                lastPower = 0.0
                outputApplied = false
            } catch (_: Exception) {
                lastPower = UNKNOWN_POWER
                stopSucceeded = false
                velocityControlFaulted = true
            }
        }

        if (allowRecovery && stopSucceeded && cachedVelocityValid) {
            try {
                motor.mode = com.qualcomm.robotcore.hardware.DcMotor.RunMode.RUN_USING_ENCODER
                velocityControlFaulted = false
            } catch (_: Exception) {
                velocityControlFaulted = true
            }
        }
    }

    override fun safe() {
        setAppliedVoltage(0.0)
    }

    override fun close() {
        safe()
    }

    private companion object {
        const val UNKNOWN_POWER = -999.0
        const val NOMINAL_VOLTAGE = 12.0
        const val MIN_VALID_VOLTAGE = 8.0
        const val FULL_EFFORT_THRESHOLD = 0.999
        const val VELOCITY_ERROR_VOLTS_PER_RPM = 0.001
    }
}
