package org.firstinspires.ftc.teamcode.hardware

import org.firstinspires.ftc.teamcode.hardware.IntakeIO
import com.qualcomm.robotcore.hardware.DcMotorEx
import com.qualcomm.robotcore.hardware.HardwareMap
import org.firstinspires.ftc.robotcore.external.navigation.CurrentUnit

import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

class FtcIntakeIO(hardwareMap: HardwareMap) : IntakeIO, AutoCloseable {
    private val motor: DcMotorEx? = try {
        com.areslib.ftc.hardware.CachedDcMotorEx(hardwareMap.get(DcMotorEx::class.java, "intake"))
    } catch (_: Exception) {
        null
    }

    private val scheduler: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor { thread ->
        Thread(thread, "ARES-IntakeTelemetry-Thread").apply { isDaemon = true }
    }

    @Volatile private var cachedRollerAmps = 0.0

    init {
        scheduler.scheduleAtFixedRate({
            if (motor != null) {
                try {
                    cachedRollerAmps = motor.getCurrent(CurrentUnit.AMPS)
                } catch (_: Exception) {
                    // Keep last cached values
                }
            }
        }, 0, 100, TimeUnit.MILLISECONDS)
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
        get() = cachedRollerAmps

    override fun refresh() {
        // Telemetry reads are performed in the background thread to prevent loop jitter
    }

    override fun safe() {
        setRollerVoltage(0.0)
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
