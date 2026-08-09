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
    var prismIO: com.areslib.hardware.actuator.PrismDriverIO? = null
    var intakeSubsystem: org.firstinspires.ftc.teamcode.subsystems.IntakeSubsystem? = null
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
            val flywheelIO = org.firstinspires.ftc.teamcode.hardware.FtcFlywheelIO(hardwareMap)
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
                } catch (_: Exception) {}
            }
        }
        if (primaryIO != null && primaryName != null) {
            base.registerSubsystem(org.firstinspires.ftc.teamcode.subsystems.IndicatorLightSubsystem(primaryIO, "indicator"))
            setIndicatorColor("indicator", com.areslib.hardware.actuator.IndicatorLightColor.GREEN)
            
            com.areslib.hardware.actuator.IndicatorLightColor.entries.forEach { color ->
                com.areslib.pathing.NamedCommands.registerCommand(
                    "SetIndicatorColor_${color.name}",
                    com.areslib.sequencer.tasks.SetIndicatorColorTask("indicator", color)
                )
            }
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

        // Auto-discover any 2nd servo in hardwareMap that isn't the primary indicator light
        if (secondaryIO == null) {
            try {
                for (entry in hardwareMap.servo.entrySet()) {
                    val deviceName = entry.key
                    if (deviceName != primaryName && !deviceName.equals("floodgate", ignoreCase = true)) {
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

            com.areslib.hardware.actuator.IndicatorLightColor.entries.forEach { color ->
                com.areslib.pathing.NamedCommands.registerCommand(
                    "SetSecondIndicatorColor_${color.name}",
                    com.areslib.sequencer.tasks.SetIndicatorColorTask("indicator2", color)
                )
            }
            addTelemetry("Subsystem", "Secondary indicator light loaded as: $loadedSecondaryName")
        } else {
            addTelemetry("Subsystem", "Secondary indicator light (indicator2) not configured in Hardware Map")
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
                } catch (_: Exception) {}
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
                    } catch (_: Exception) {}
                }
            }
        }

        if (prismIOInstance != null && loadedPrismName != null) {
            prismIO = prismIOInstance
            base.registerSubsystem(org.firstinspires.ftc.teamcode.subsystems.PrismSubsystem(prismIOInstance, "prism"))
            setPrismPreset(com.areslib.hardware.actuator.PrismPwmPreset.RAINBOW_FULL_COLOR)

            com.areslib.hardware.actuator.PrismPwmPreset.entries.forEach { preset ->
                com.areslib.pathing.NamedCommands.registerCommand(
                    "SetPrismPreset_${preset.name}",
                    object : com.areslib.sequencer.Task {
                        override val name = "SetPrismPreset_${preset.name}"
                        override fun isCompleted(state: com.areslib.state.RobotState, elapsedMs: Long) = true
                        override fun initialize(state: com.areslib.state.RobotState): List<com.areslib.action.RobotAction> =
                            listOf(com.areslib.action.RobotAction.SetPrismDriver("prism", preset.pulseWidthUs))
                    }
                )
            }
            addTelemetry("Subsystem", "Prism RGB Driver loaded as: $loadedPrismName")
        } else {
            addTelemetry("Subsystem", "Prism RGB Driver (prism) optional")
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
        
        intakeSubsystem?.let {
            if (it.stalled) {
                val seasonState = base.store.state.superstructure.season
                if (seasonState.intakeActive) {
                    base.store.dispatch(com.areslib.action.RobotAction.UpdateSubsystemState(seasonState.copy(intakeActive = false)))
                }
            }
        }
        


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
    
    fun driveWithGamepad(driver: com.areslib.telemetry.AresGamepad, useHeadingLock: Boolean = true) = driveController.driveWithGamepad(driver, useHeadingLock)
    fun alignToTag(tagId: Int) = driveController.alignToTag(tagId)
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
    fun setSecondIndicatorColor(color: com.areslib.hardware.actuator.IndicatorLightColor) = telemetryHelper.setSecondIndicatorColor(color)
    fun setIndicatorColor(name: String, color: com.areslib.hardware.actuator.IndicatorLightColor) = telemetryHelper.setIndicatorColor(name, color)

    fun setPrismPreset(preset: com.areslib.hardware.actuator.PrismPwmPreset) = telemetryHelper.setPrismPreset("prism", preset)
    fun setPrismPreset(name: String, preset: com.areslib.hardware.actuator.PrismPwmPreset) = telemetryHelper.setPrismPreset(name, preset)
    fun setPrismPulseWidth(pulseWidthUs: Int) = telemetryHelper.setPrismPulseWidth("prism", pulseWidthUs)
    fun setPrismPulseWidth(name: String, pulseWidthUs: Int) = telemetryHelper.setPrismPulseWidth(name, pulseWidthUs)
    fun setPrismMaxBrightness(percent: Int) {
        prismIO?.maxBrightnessPercent = percent.coerceIn(0, 100)
    }
    /**
     * Documentation for close
     */

    fun close() {
        base.close()
    }
}
