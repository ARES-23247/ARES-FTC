package org.firstinspires.ftc.teamcode.opmodes

import com.areslib.ftc.FtcMecanumRobot
import com.qualcomm.robotcore.hardware.HardwareMap
import org.firstinspires.ftc.robotcore.external.Telemetry
import com.areslib.hardware.HardwareRegistry
import com.areslib.action.RobotAction
import org.firstinspires.ftc.teamcode.dsl.*

/**
 * Team-specific wrapper around the core FtcMecanumRobot.
 * Stripped to drive-only for maximum loop performance.
 * Subsystem IOs (intake, flywheel) can be re-added when physical hardware is present.
 */
class AresRobot(
    val hardwareMap: HardwareMap,
    val localTelemetry: Telemetry? = null
) {
    val base = FtcMecanumRobot(
        hardwareMap = hardwareMap,
        rlName = "rl",
        rrName = "rr",
        flDirection = com.qualcomm.robotcore.hardware.DcMotorSimple.Direction.FORWARD,
        frDirection = com.qualcomm.robotcore.hardware.DcMotorSimple.Direction.REVERSE,
        rlDirection = com.qualcomm.robotcore.hardware.DcMotorSimple.Direction.FORWARD,
        rrDirection = com.qualcomm.robotcore.hardware.DcMotorSimple.Direction.REVERSE,
        pinpointName = org.firstinspires.ftc.teamcode.config.HardwareConstants.ODOMETRY_PINPOINT,
        limelightName = org.firstinspires.ftc.teamcode.config.HardwareConstants.VISION_LIMELIGHT,
        localTelemetry = localTelemetry,
        driveFeedforward = com.areslib.control.tuning.SimpleFeedforwardCoeffs(
            kS = 0.05,
            kV = 0.12,
            kA = 0.01
        ),
        motorGains = com.areslib.control.tuning.PIDFCoefficients(
            kP = 12.0,
            kI = 3.0,
            kD = 0.0,
            kF = 0.0
        )
    )

    init {
        try {
            val intakeIO = org.firstinspires.ftc.teamcode.hardware.FtcIntakeIO(hardwareMap)
            base.registerSubsystem(org.firstinspires.ftc.teamcode.subsystems.IntakeSubsystem(intakeIO))
        } catch (e: Exception) {
            addTelemetry("Subsystem", "Intake failed to load: ${e.message}")
        }

        try {
            val flywheelIO = org.firstinspires.ftc.teamcode.hardware.FtcFlywheelIO(hardwareMap)
            base.registerSubsystem(org.firstinspires.ftc.teamcode.subsystems.FlywheelSubsystem(flywheelIO))
        } catch (e: Exception) {
            addTelemetry("Subsystem", "Flywheel failed to load: ${e.message}")
        }
    }

    fun addTelemetry(key: String, value: Any) {
        base.telemetryManager.customDriverStationText[key] = value.toString()
    }

    @kotlin.jvm.JvmOverloads
    fun update(
        gamepad1: com.areslib.telemetry.GamepadState? = null,
        gamepad2: com.areslib.telemetry.GamepadState? = null
    ) {
        // 1. Poll subsystem sensors (e.g. flywheel encoder) before drivebase update
        val timestamp = com.areslib.util.RobotClock.currentTimeMillis()
        base.readAllSensors(timestamp)

        // 2. Update drivebase sensors, EKF, and kinematics
        base.update(gamepad1, gamepad2)

        // 3. Command subsystem actuators with brownout-adjusted power scale
        base.writeAllOutputs(base.powerManager.powerScale)
    }

    fun driveFieldCentric(x: Double, y: Double, rotation: Double) {
        base.driveFieldCentric(x, y, rotation)
    }

    fun driveRobotCentric(x: Double, y: Double, rotation: Double) {
        base.driveRobotCentric(x, y, rotation)
    }

    fun resetPoseForAlliance() {
        base.resetPoseForAlliance()
    }

    fun close() {
        base.close()
    }
}
