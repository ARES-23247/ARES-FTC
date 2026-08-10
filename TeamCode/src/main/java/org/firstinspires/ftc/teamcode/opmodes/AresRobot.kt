package org.firstinspires.ftc.teamcode.opmodes

import com.areslib.ftc.FtcMecanumRobot
import com.qualcomm.robotcore.hardware.HardwareMap
import org.firstinspires.ftc.robotcore.external.Telemetry
import org.firstinspires.ftc.teamcode.config.HardwareConstants.FLYWHEEL_MAX_RPM
import org.firstinspires.ftc.teamcode.config.HardwareConstants.FLYWHEEL_TICKS_PER_REV
import org.firstinspires.ftc.teamcode.config.HardwareConstants.IMU_BNO055
import org.firstinspires.ftc.teamcode.config.HardwareConstants.MOTOR_BACK_LEFT
import org.firstinspires.ftc.teamcode.config.HardwareConstants.MOTOR_BACK_RIGHT
import org.firstinspires.ftc.teamcode.config.HardwareConstants.MOTOR_FRONT_LEFT
import org.firstinspires.ftc.teamcode.config.HardwareConstants.MOTOR_FRONT_RIGHT
import org.firstinspires.ftc.teamcode.config.HardwareConstants.ODOMETRY_PINPOINT
import org.firstinspires.ftc.teamcode.config.HardwareConstants.VISION_LIMELIGHT
import org.firstinspires.ftc.teamcode.dsl.*
import org.firstinspires.ftc.teamcode.opmodes.robot.AresDriveController
import org.firstinspires.ftc.teamcode.opmodes.robot.AresSuperstructureController
import org.firstinspires.ftc.teamcode.opmodes.robot.AresTelemetryHelper

/**
 * Composition root for the FTC season layer over ARESLib's [FtcMecanumRobot].
 *
 * Required drivetrain/localization configuration is passed to [base]. Intake, flywheel,
 * indicators, and Prism are optional: initialization failure is reported without preventing
 * drivetrain use. Successfully constructed mechanism IO is registered both with ARESLib's
 * hardware safety registry and this robot's subsystem lifecycle.
 *
 * **Physical Units & Conventions:**
 * - Translational velocities: Meters per second ($m/s$).
 * - Angular velocities: Radians per second ($rad/s$).
 * - Heading: CCW-positive radians ($rad$).
 *
 * [update] preserves the hot-loop ordering: refresh hardware caches, consume cached season
 * sensors, apply interlocks, write season outputs, then run the shared drivetrain/power update.
 * Season outputs are written before the shared update so a fatal shared failure leaves the final
 * command as `HardwareRegistry.safeAll`, rather than allowing this wrapper to re-enable a motor.
 * Any exception escaping season work invokes both subsystem and platform safety before rethrowing.
 *
 * @param hardwareMap FTC device registry. Production drive names are `fl`, `fr`, `rl`, and `rr`.
 * @param localTelemetry optional Driver Station telemetry sink.
 */
class AresRobot(
    val hardwareMap: HardwareMap,
    val localTelemetry: Telemetry? = null
) {
    /** Shared drivetrain, Redux store, EKF, power, logging, telemetry, and hardware lifecycle. */
    val base = FtcMecanumRobot(
        hardwareMap = hardwareMap,
        flName = MOTOR_FRONT_LEFT,
        frName = MOTOR_FRONT_RIGHT,
        rlName = MOTOR_BACK_LEFT,
        rrName = MOTOR_BACK_RIGHT,
        flDirection = com.qualcomm.robotcore.hardware.DcMotorSimple.Direction.FORWARD,
        frDirection = com.qualcomm.robotcore.hardware.DcMotorSimple.Direction.REVERSE,
        rlDirection = com.qualcomm.robotcore.hardware.DcMotorSimple.Direction.FORWARD,
        rrDirection = com.qualcomm.robotcore.hardware.DcMotorSimple.Direction.REVERSE,
        pinpointName = ODOMETRY_PINPOINT,
        limelightName = VISION_LIMELIGHT,
        imuName = IMU_BNO055,
        localTelemetry = localTelemetry
    )

    private val driveController = AresDriveController(base)
    private val superstructureController = AresSuperstructureController(base)
    private val telemetryHelper = AresTelemetryHelper(base)
    /** Optional Prism IO, exposed for diagnostics and simulator inspection. */
    var prismIO: com.areslib.hardware.actuator.PrismDriverIO? = null

    /** Optional intake lifecycle controller; null when `intake` hardware failed to initialize. */
    var intakeSubsystem: org.firstinspires.ftc.teamcode.subsystems.IntakeSubsystem? = null

    /** Optional flywheel lifecycle controller; null when `shooter` hardware failed to initialize. */
    var flywheelSubsystem: org.firstinspires.ftc.teamcode.subsystems.FlywheelSubsystem? = null

    init {
        try {
            val intakeIO = org.firstinspires.ftc.teamcode.hardware.FtcIntakeIO(hardwareMap)
            intakeSubsystem = org.firstinspires.ftc.teamcode.subsystems.IntakeSubsystem(intakeIO)
            base.registerSubsystem(intakeSubsystem!!)
        } catch (e: Exception) {
            addTelemetry("Subsystem", "Intake failed to load: ${e.message}")
        }

        try {
            val flywheelIO = org.firstinspires.ftc.teamcode.hardware.FtcFlywheelIO(
                hardwareMap,
                ticksPerRev = FLYWHEEL_TICKS_PER_REV,
                maxRpm = FLYWHEEL_MAX_RPM
            )
            flywheelSubsystem = org.firstinspires.ftc.teamcode.subsystems.FlywheelSubsystem(flywheelIO)
            base.registerSubsystem(flywheelSubsystem!!)
        } catch (e: Exception) {
            addTelemetry("Subsystem", "Flywheel failed to load: ${e.message}")
        }

        val allDeviceNames = mutableSetOf<String>()
        for (device in hardwareMap) {
            allDeviceNames.addAll(hardwareMap.getNamesOf(device))
        }

        // --- Primary Indicator Light ("indicator") ---
        var primaryName: String? = null
        var primaryIO: com.areslib.ftc.hardware.FtcIndicatorLightIO? = null
        val primaryCandidates = listOf("indicator", "indicator1", "indicator_1", "light1", "light_1", "led1")
        for (candidateName in primaryCandidates) {
            if (candidateName in allDeviceNames) {
                try {
                    primaryIO = com.areslib.ftc.hardware.FtcIndicatorLightIO(hardwareMap, candidateName)
                    primaryName = candidateName
                    break
                } catch (e: Exception) {
                    addTelemetry("Init", "Indicator '$candidateName' failed: ${e.message}")
                }
            }
        }
        if (primaryIO != null && primaryName != null) {
            base.registerSubsystem(org.firstinspires.ftc.teamcode.subsystems.IndicatorLightSubsystem(primaryIO, "indicator"))
            setIndicatorColor("indicator", com.areslib.hardware.actuator.IndicatorLightColor.GREEN)
        } else {
            addTelemetry("Subsystem", "Primary indicator light not found in Hardware Map")
        }

        // --- Secondary Indicator Light ("indicator2") ---
        var secondaryIO: com.areslib.ftc.hardware.FtcIndicatorLightIO? = null
        var loadedSecondaryName: String? = null
        val secondaryCandidates = listOf("indicator2", "indicator_2", "second_indicator", "indicatorLight2", "light2", "light_2", "led2", "led_2")
        for (candidateName in secondaryCandidates) {
            if (candidateName != primaryName && candidateName in allDeviceNames) {
                try {
                    secondaryIO = com.areslib.ftc.hardware.FtcIndicatorLightIO(hardwareMap, candidateName)
                    loadedSecondaryName = candidateName
                    break
                } catch (_: Exception) {}
            }
        }

        // Fall back to any remaining servo whose name looks like an indicator light.
        if (secondaryIO == null) {
            try {
                for (entry in hardwareMap.servo.entrySet()) {
                    val deviceName = entry.key
                    if (deviceName != primaryName && (
                            deviceName.contains("indicator", ignoreCase = true) ||
                            deviceName.contains("light", ignoreCase = true) ||
                            deviceName.contains("led", ignoreCase = true))) {
                        try {
                            secondaryIO = com.areslib.ftc.hardware.FtcIndicatorLightIO(hardwareMap, deviceName)
                            loadedSecondaryName = deviceName
                            break
                        } catch (_: Exception) {}
                    }
                }
            } catch (_: Exception) {}
        }

        if (secondaryIO != null && loadedSecondaryName != null) {
            base.registerSubsystem(org.firstinspires.ftc.teamcode.subsystems.IndicatorLightSubsystem(secondaryIO, "indicator2"))
            setSecondIndicatorColor(com.areslib.hardware.actuator.IndicatorLightColor.BLUE)
            addTelemetry("Subsystem", "Secondary indicator light loaded as: $loadedSecondaryName")
        } else {
            addTelemetry("Subsystem", "Secondary indicator light (indicator2) not configured in Hardware Map")
        }

        // Always register indicator color commands so an auto that references them does not
        // crash when the indicator IO failed to init; the task no-ops if absent.
        com.areslib.hardware.actuator.IndicatorLightColor.entries.forEach { color ->
            com.areslib.pathing.NamedCommands.registerCommand(
                "SetIndicatorColor_${color.name}",
                object : com.areslib.sequencer.Task {
                    override val name = "SetIndicatorColor_${color.name}"
                    override fun isCompleted(state: com.areslib.state.RobotState, elapsedMs: Long) = true
                    override fun initialize(state: com.areslib.state.RobotState): List<com.areslib.action.RobotAction> =
                        if (primaryIO != null) listOf(com.areslib.action.RobotAction.SetIndicatorLight("indicator", color.position))
                        else emptyList()
                }
            )
            com.areslib.pathing.NamedCommands.registerCommand(
                "SetSecondIndicatorColor_${color.name}",
                object : com.areslib.sequencer.Task {
                    override val name = "SetSecondIndicatorColor_${color.name}"
                    override fun isCompleted(state: com.areslib.state.RobotState, elapsedMs: Long) = true
                    override fun initialize(state: com.areslib.state.RobotState): List<com.areslib.action.RobotAction> =
                        if (secondaryIO != null) listOf(com.areslib.action.RobotAction.SetIndicatorLight("indicator2", color.position))
                        else emptyList()
                }
            )
        }

        // --- goBILDA Prism RGB LED Driver ("prism") ---
        val prismCandidates = listOf("prism", "prism_driver", "gobilda_prism", "prism_led")
        var loadedPrismName: String? = null
        var prismIOInstance: com.areslib.hardware.actuator.PrismDriverIO? = null

        // 1. Try I2C Device initialization first (Address 0x38)
        for (candidateName in prismCandidates) {
            if (candidateName in allDeviceNames) {
                try {
                    prismIOInstance = com.areslib.ftc.hardware.FtcPrismDriverI2cIO(hardwareMap, candidateName)
                    loadedPrismName = "$candidateName (I2C 0x38)"
                    break
                } catch (e: Exception) {
                    addTelemetry("Init", "Prism I2C '$candidateName' failed: ${e.message}")
                }
            }
        }

        // 2. Fall back to PWM Servo initialization if I2C device is not configured
        if (prismIOInstance == null) {
            for (candidateName in prismCandidates) {
                if (candidateName in allDeviceNames) {
                    try {
                        prismIOInstance = com.areslib.ftc.hardware.FtcPrismDriverIO(hardwareMap, candidateName)
                        loadedPrismName = "$candidateName (PWM Servo)"
                        break
                    } catch (e: Exception) {
                        addTelemetry("Init", "Prism PWM '$candidateName' failed: ${e.message}")
                    }
                }
            }
        }

        if (prismIOInstance != null && loadedPrismName != null) {
            prismIO = prismIOInstance
            base.registerSubsystem(org.firstinspires.ftc.teamcode.subsystems.PrismSubsystem(prismIOInstance, "prism"))
            setPrismPreset(com.areslib.hardware.actuator.PrismPwmPreset.RAINBOW_FULL_COLOR)
            addTelemetry("Subsystem", "Prism RGB Driver loaded as: $loadedPrismName")
        } else {
            addTelemetry("Subsystem", "Prism RGB Driver (prism) optional")
        }

        // Always register prism preset commands so an auto that references them does not
        // crash when the Prism I2C/PWM device failed to init; the task no-ops if absent.
        com.areslib.hardware.actuator.PrismPwmPreset.entries.forEach { preset ->
            com.areslib.pathing.NamedCommands.registerCommand(
                "SetPrismPreset_${preset.name}",
                object : com.areslib.sequencer.Task {
                    override val name = "SetPrismPreset_${preset.name}"
                    override fun isCompleted(state: com.areslib.state.RobotState, elapsedMs: Long) = true
                    override fun initialize(state: com.areslib.state.RobotState): List<com.areslib.action.RobotAction> =
                        if (prismIO != null) listOf(com.areslib.action.RobotAction.SetPrismDriver("prism", preset.pulseWidthUs))
                        else emptyList()
                }
            )
        }
    }

    /**
     * Safely adds a key-value pair to the robot telemetry stream.
     * @param key The telemetry category label.
     * @param value The telemetry data value.
     */
    fun addTelemetry(key: String, value: Any) = telemetryHelper.addTelemetry(key, value)

    /**
     * Executes one complete season and shared robot frame.
     *
     * Normal sampling/output work preserves the library's zero-allocation hot-path design. Fault
     * transitions and low-rate telemetry may allocate because they are outside the steady-state
     * motor-control path.
     *
     * @param gamepad1 The primary gamepad telemetry state.
     * @param gamepad2 The secondary gamepad telemetry state.
     */
    @kotlin.jvm.JvmOverloads
    fun update(
        gamepad1: com.areslib.telemetry.GamepadState? = null,
        gamepad2: com.areslib.telemetry.GamepadState? = null
    ) {
        try {
            // Clear REV bulk caches and refresh registered IO before consuming season sensors.
            base.readSensors()

            // Poll subsystem state from the freshly cached IO values.
            val timestamp = com.areslib.util.RobotClock.currentTimeMillis()
            base.readAllSensors(timestamp)
        
            if (intakeSubsystem?.stalled == true) {
                val seasonState = base.store.state.superstructure.season
                if (seasonState.intakeActive) {
                    base.store.dispatch(com.areslib.action.RobotAction.UpdateSubsystemState(seasonState.copy(intakeActive = false)))
                }
            }
            // Command season actuators before entering the base update. If the base
            // catches a fatal drivetrain/update failure, its final action remains the
            // HardwareRegistry safety stop instead of this wrapper re-enabling motors.
            base.writeAllOutputs(base.powerManager.powerScale)

            // Update drivebase sensors, EKF, kinematics, and the next power scale.
            base.update(gamepad1, gamepad2)

            // Continuously update core Driver Station telemetry.
            telemetryHelper.updateTelemetry()
        } catch (t: Throwable) {
            // Season work happens outside FtcBaseRobot.update's catch block.
            base.safeAll()
            base.safeHardware()
            throw t
        }
    }
    /** Commands shaped, alliance-aware field-relative translation and CCW-positive rotation. */
    fun driveFieldCentric(x: Double, y: Double, rotation: Double) = driveController.driveFieldCentric(x, y, rotation)

    /** Commands field-relative drive from a cached gamepad snapshot. */
    fun driveWithGamepad(driver: com.areslib.telemetry.AresGamepad, useHeadingLock: Boolean = true) = driveController.driveWithGamepad(driver, useHeadingLock)

    /** Resets localization to the configured origin for the current Redux alliance. */
    fun resetPoseForAlliance() = driveController.resetPoseForAlliance()

    /** Dispatches debounced intake intent. */
    fun toggleIntake() = superstructureController.toggleIntake()

    /** Dispatches debounced shooter intent subject to the intake interlock. */
    fun toggleShooter() = superstructureController.toggleShooter()

    /** Toggles Redux alliance; the caller decides whether to reset pose. */
    fun toggleAlliance() = superstructureController.toggleAlliance()

    /** Dispatches the primary optional indicator color. */
    fun setIndicatorColor(color: com.areslib.hardware.actuator.IndicatorLightColor) = telemetryHelper.setIndicatorColor(color)
    fun setSecondIndicatorColor(color: com.areslib.hardware.actuator.IndicatorLightColor) = telemetryHelper.setSecondIndicatorColor(color)
    fun setIndicatorColor(name: String, color: com.areslib.hardware.actuator.IndicatorLightColor) = telemetryHelper.setIndicatorColor(name, color)

    fun setPrismPreset(preset: com.areslib.hardware.actuator.PrismPwmPreset) = telemetryHelper.setPrismPreset("prism", preset)
    fun setPrismPreset(name: String, preset: com.areslib.hardware.actuator.PrismPwmPreset) = telemetryHelper.setPrismPreset(name, preset)
    /** Zeroes outputs, closes season subsystems, then always closes shared robot resources. */
    fun close() {
        try {
            base.safeAll()
            base.closeSubsystems()
        } finally {
            base.close()
        }
    }
}
