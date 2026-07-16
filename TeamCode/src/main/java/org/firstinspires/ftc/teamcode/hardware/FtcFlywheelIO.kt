package org.firstinspires.ftc.teamcode.hardware

import org.firstinspires.ftc.teamcode.hardware.FlywheelIO
import com.qualcomm.robotcore.hardware.DcMotorEx
import com.qualcomm.robotcore.hardware.HardwareMap
import org.firstinspires.ftc.robotcore.external.navigation.CurrentUnit

import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

class FtcFlywheelIO(hardwareMap: HardwareMap) : FlywheelIO, AutoCloseable {
    private val motor: DcMotorEx? = try {
        com.areslib.ftc.hardware.CachedDcMotorEx(hardwareMap.get(DcMotorEx::class.java, "shooter"))
    } catch (_: Exception) {
        null
    }

    // Gearing / Encoder conversion: GoBilda motor has 28 ticks per motor shaft revolution.
    // If it's a bare motor (like for a flywheel), ticksPerRev is 28.0.
    private val ticksPerRev = 28.0

    private val scheduler: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor { thread ->
        Thread(thread, "ARES-FlywheelTelemetry-Thread").apply { isDaemon = true }
    }

    @Volatile private var cachedVelocityRpm = 0.0
    @Volatile private var cachedAmps = 0.0

    init {
        scheduler.scheduleAtFixedRate({
            if (motor != null) {
                try {
                    val current = motor.getCurrent(CurrentUnit.AMPS)
                    val ticksPerSec = motor.velocity
                    cachedAmps = current
                    cachedVelocityRpm = (ticksPerSec / ticksPerRev) * 60.0
                } catch (_: Exception) {
                    // Keep last cached values
                }
            }
        }, 0, 100, TimeUnit.MILLISECONDS)
    }

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
        get() = cachedVelocityRpm

    override val currentAmps: Double
        get() = cachedAmps

    override val tempCelsius: Double
        get() = 0.0

    override fun refresh() {
        // Telemetry reads are performed in the background thread to prevent loop jitter
    }

    override fun safe() {
        setAppliedVoltage(0.0)
    }

    override fun close() {
        safe()
        scheduler.shutdown()
        try {
            if (!scheduler.awaitTermination(100, TimeUnit.MILLISECONDS)) {
                scheduler.shutdownNow()
            }
        } catch (_: InterruptedException) {
            scheduler.shutdownNow()
            Thread.currentThread().interrupt()
        }
    }
}
