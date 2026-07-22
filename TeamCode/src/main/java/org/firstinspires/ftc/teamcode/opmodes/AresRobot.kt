package org.firstinspires.ftc.teamcode.opmodes

import com.areslib.ftc.FtcMecanumRobot
import com.qualcomm.robotcore.hardware.HardwareMap
import org.firstinspires.ftc.robotcore.external.Telemetry
import org.firstinspires.ftc.teamcode.config.HardwareConstants.ODOMETRY_PINPOINT
import org.firstinspires.ftc.teamcode.config.HardwareConstants.VISION_LIMELIGHT
import org.firstinspires.ftc.teamcode.dsl.*
import org.firstinspires.ftc.teamcode.opmodes.robot.AresDriveController
import org.firstinspires.ftc.teamcode.opmodes.robot.AresSuperstructureController
import org.firstinspires.ftc.teamcode.opmodes.robot.AresTelemetryHelper

/**
 * Team-specific wrapper around the core FtcMecanumRobot.
 * Refactored into a facade delegating to dedicated single-responsibility controllers.
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
        pinpointName = ODOMETRY_PINPOINT,
        limelightName = VISION_LIMELIGHT,
        localTelemetry = localTelemetry
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

        // --- Indicator Light ---
        try {
            val indicatorIO = com.areslib.ftc.hardware.FtcIndicatorLightIO(hardwareMap, "indicator")
            base.registerSubsystem(org.firstinspires.ftc.teamcode.subsystems.IndicatorLightSubsystem(indicatorIO, "indicator"))
            
            // Register PathPlanner named commands for each color
            com.areslib.hardware.actuator.IndicatorLightColor.values().forEach { color ->
                com.areslib.pathing.NamedCommands.registerCommand(
                    "SetIndicatorColor_${color.name}",
                    com.areslib.sequencer.tasks.SetIndicatorColorTask("indicator", color)
                )
            }
        } catch (e: Exception) {
            addTelemetry("Subsystem", "Indicator light failed to load: ${e.message}")
        }
    }

    private val driveController = AresDriveController(base)
    private val superstructureController = AresSuperstructureController(base)
    private val telemetryHelper = AresTelemetryHelper(base)

    fun addTelemetry(key: String, value: Any) = telemetryHelper.addTelemetry(key, value)

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

    fun driveFieldCentric(x: Double, y: Double, rotation: Double) = driveController.driveFieldCentric(x, y, rotation)
    fun driveRobotCentric(x: Double, y: Double, rotation: Double) = driveController.driveRobotCentric(x, y, rotation)
    fun resetPoseForAlliance() = driveController.resetPoseForAlliance()

    fun toggleIntake() = superstructureController.toggleIntake()
    fun toggleShooter() = superstructureController.toggleShooter()
    fun toggleAlliance() = superstructureController.toggleAlliance()

    fun setIndicatorColor(color: com.areslib.hardware.actuator.IndicatorLightColor) = telemetryHelper.setIndicatorColor(color)

    fun close() {
        base.close()
    }
}
