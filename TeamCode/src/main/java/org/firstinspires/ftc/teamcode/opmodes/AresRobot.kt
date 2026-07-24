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
 * Refactored into a Facade delegating to dedicated single-responsibility controllers.
 * Stripped to drive-only for maximum loop performance.
 * Subsystem IOs (intake, flywheel) can be re-added when physical hardware is present.
 *
 * **Physical Units & Conventions:**
 * - Translational velocities: Meters per second ($m/s$).
 * - Angular velocities: Radians per second ($rad/s$).
 * - Heading: CCW-positive radians ($rad$).
 *
 * **Performance Guarantees:**
 * - Zero-GC Allocations in the hot teleop `update()` loop.
 */
class AresRobot(
    /**
     * Documentation for hardwareMap
     */
    val hardwareMap: HardwareMap,
    /**
     * Documentation for localTelemetry
     */
    val localTelemetry: Telemetry? = null
) {
    /**
     * Documentation for base
     */
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

    private val driveController = AresDriveController(base)
    private val superstructureController = AresSuperstructureController(base)
    private val telemetryHelper = AresTelemetryHelper(base)

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
            setIndicatorColor(com.areslib.hardware.actuator.IndicatorLightColor.GREEN)
            
            // Register PathPlanner named commands for each color
            com.areslib.hardware.actuator.IndicatorLightColor.entries.forEach { color ->
                com.areslib.pathing.NamedCommands.registerCommand(
                    "SetIndicatorColor_${color.name}",
                    com.areslib.sequencer.tasks.SetIndicatorColorTask("indicator", color)
                )
            }
        } catch (e: Exception) {
            addTelemetry("Subsystem", "Indicator light failed to load: ${e.message}")
        }
    }

    /**
     * Safely adds a key-value pair to the robot telemetry stream.
     * @param key The telemetry category label.
     * @param value The telemetry data value.
     */
    fun addTelemetry(key: String, value: Any) = telemetryHelper.addTelemetry(key, value)

    /**
     * Updates the robot state by polling sensors and writing to actuators.
     * Guaranteed Zero-GC allocations in this hot path loop.
     *
     * @param gamepad1 The primary gamepad telemetry state.
     * @param gamepad2 The secondary gamepad telemetry state.
     */
    @kotlin.jvm.JvmOverloads
    fun update(
        gamepad1: com.areslib.telemetry.GamepadState? = null,
        gamepad2: com.areslib.telemetry.GamepadState? = null
    ) {
        // 1. Poll subsystem sensors (e.g. flywheel encoder) before drivebase update
        /**
         * Documentation for timestamp
         */
        val timestamp = com.areslib.util.RobotClock.currentTimeMillis()
        base.readAllSensors(timestamp)

        // 2. Update drivebase sensors, EKF, and kinematics
        base.update(gamepad1, gamepad2)

        // 3. Command subsystem actuators with brownout-adjusted power scale
        base.writeAllOutputs(base.powerManager.powerScale)

        // 4. Continuously update core Driver Station telemetry
        telemetryHelper.updateTelemetry()
    }
    /**
     * Documentation for driveFieldCentric
     */

    fun driveFieldCentric(x: Double, y: Double, rotation: Double) = driveController.driveFieldCentric(x, y, rotation)
    /**
     * Documentation for driveRobotCentric
     */
    fun driveRobotCentric(x: Double, y: Double, rotation: Double) = driveController.driveRobotCentric(x, y, rotation)
    /**
     * Documentation for resetPoseForAlliance
     */
    fun resetPoseForAlliance() = driveController.resetPoseForAlliance()
    /**
     * Documentation for toggleIntake
     */

    fun toggleIntake() = superstructureController.toggleIntake()
    /**
     * Documentation for toggleShooter
     */
    fun toggleShooter() = superstructureController.toggleShooter()
    /**
     * Documentation for toggleAlliance
     */
    fun toggleAlliance() = superstructureController.toggleAlliance()
    /**
     * Documentation for setIndicatorColor
     */

    fun setIndicatorColor(color: com.areslib.hardware.actuator.IndicatorLightColor) = telemetryHelper.setIndicatorColor(color)
    /**
     * Documentation for close
     */

    fun close() {
        base.close()
    }
}
