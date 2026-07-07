package org.firstinspires.ftc.teamcode.opmodes

import com.areslib.ftc.FtcMecanumRobot
import com.qualcomm.robotcore.hardware.HardwareMap
import com.qualcomm.robotcore.hardware.DcMotor
import com.qualcomm.robotcore.hardware.DcMotorEx
import org.firstinspires.ftc.robotcore.external.Telemetry

/**
 * Team-specific wrapper around the core FtcMecanumRobot.
 * Adds specialized mechanisms like intake and shooter.
 */
class AresRobot(
    val hardwareMap: HardwareMap,
    val localTelemetry: Telemetry? = null
) {
    val base = FtcMecanumRobot(
        hardwareMap = hardwareMap,
        localTelemetry = localTelemetry
    )
    
    private var intake: DcMotor? = null
    private var shooter: DcMotorEx? = null

    init {
        try {
            intake = hardwareMap.get(DcMotor::class.java, "intake")
            shooter = hardwareMap.get(DcMotorEx::class.java, "shooter")
            shooter?.mode = DcMotor.RunMode.RUN_USING_ENCODER
        } catch (e: Exception) {
            println("[AresRobot] Optional intake/shooter missing. Continuing in Drivebase-Only mode.")
        }
    }

    fun update() {
        base.update()
    }

    fun driveFieldCentric(x: Double, y: Double, rotation: Double) {
        base.driveFieldCentric(x, y, rotation)
    }

    fun driveRobotCentric(x: Double, y: Double, rotation: Double) {
        base.driveRobotCentric(x, y, rotation)
    }

    fun intake(power: Double) {
        intake?.power = power
    }

    fun shoot(rpm: Double) {
        if (shooter != null) {
            try {
                // Type-safe approach relying on the Kotlin wrapper property mapped to SDK's setter
                shooter?.velocity = rpm
            } catch (e: Exception) {
                // If velocity isn't settable (e.g. in mocks), try to set power instead as a simulation fallback
                shooter?.power = if (rpm > 0) 1.0 else 0.0
            }
        }
    }

    fun stopAll() {
        base.stopAll()
        intake?.power = 0.0
        
        // Use a safe cast or call method if velocity is read-only in mocks
        if (shooter != null) {
            try {
                shooter?.velocity = 0.0
            } catch (e: Exception) {
                // It's a mock or something else where velocity is a val
                shooter?.power = 0.0
            }
        }
    }

    fun close() {
        base.close()
    }
}
