package org.firstinspires.ftc.teamcode.opmodes

import com.areslib.ftc.FtcMecanumRobot
import com.qualcomm.robotcore.hardware.HardwareMap
import org.firstinspires.ftc.robotcore.external.Telemetry
import org.firstinspires.ftc.teamcode.config.HardwareConstants.ODOMETRY_PINPOINT
import org.firstinspires.ftc.teamcode.config.HardwareConstants.VISION_LIMELIGHT
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
        val mult = if (base.store.state.drive.alliance == com.areslib.state.Alliance.BLUE) -1.0 else 1.0
        base.driveFieldCentric(x * mult, y * mult, rotation)
    }

    fun driveRobotCentric(x: Double, y: Double, rotation: Double) {
        base.driveRobotCentric(x, y, rotation)
    }

    fun resetPoseForAlliance() {
        base.resetPoseForAlliance()
    }

    fun toggleIntake() {
        val season = base.store.state.superstructure.season
        base.store.dispatch(com.areslib.action.RobotAction.UpdateSubsystemState(
            state = season.copy(intakeActive = !season.intakeActive)
        ))
    }

    fun toggleShooter() {
        val season = base.store.state.superstructure.season
        val currentTarget = if (!season.flywheelActive) base.store.state.tuning.flywheelTargetRpmPreset else 0.0
        base.store.dispatch(com.areslib.action.RobotAction.UpdateSubsystemState(
            state = season.copy(
                flywheelActive = !season.flywheelActive,
                flywheelTargetRPM = currentTarget
            )
        ))
    }

    fun toggleAlliance() {
        val currentAlliance = base.store.state.drive.alliance
        val newAlliance = when (currentAlliance) {
            com.areslib.state.Alliance.RED -> com.areslib.state.Alliance.BLUE
            com.areslib.state.Alliance.BLUE -> com.areslib.state.Alliance.RED
        }
        base.store.dispatch(com.areslib.action.RobotAction.SetAlliance(newAlliance))
    }

    fun setIndicatorColor(color: com.areslib.hardware.actuator.IndicatorLightColor) {
        base.store.dispatch(com.areslib.action.RobotAction.SetIndicatorLight("indicator", color.position))
    }

    fun close() {
        base.close()
    }
}
