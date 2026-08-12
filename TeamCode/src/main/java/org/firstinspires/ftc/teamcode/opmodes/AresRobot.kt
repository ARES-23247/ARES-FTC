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
import org.firstinspires.ftc.teamcode.dsl.FtcAutoCapabilities
import org.firstinspires.ftc.teamcode.dsl.SeasonSuperstructureState
import org.firstinspires.ftc.teamcode.dsl.season
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
 * [update] preserves the hot-loop ordering: the shared frame refreshes every registered hardware
 * cache and computes power protection once, then the season layer consumes those caches, applies
 * interlocks, and writes mechanisms with that same frame's scale. Any exception escaping either
 * layer invokes both subsystem and platform safety before rethrowing.
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
    private var fatalSeasonFailure: Throwable? = null
    private var closed = false
    private var intakeIO: org.firstinspires.ftc.teamcode.hardware.FtcIntakeIO? = null
    private var flywheelIO: org.firstinspires.ftc.teamcode.hardware.FtcFlywheelIO? = null
    /** True only after the checked-in season field and its AprilTag projection validate. */
    var hasCanonicalFieldContract: Boolean = false
        private set
    /** Optional Prism IO, exposed for diagnostics and simulator inspection. */
    var prismIO: com.areslib.hardware.actuator.PrismDriverIO? = null

    /** Optional intake lifecycle controller; null when `intake` hardware failed to initialize. */
    var intakeSubsystem: org.firstinspires.ftc.teamcode.subsystems.IntakeSubsystem? = null

    /** Optional flywheel lifecycle controller; null when `shooter` hardware failed to initialize. */
    var flywheelSubsystem: org.firstinspires.ftc.teamcode.subsystems.FlywheelSubsystem? = null

    init {
        // Field symmetry changes by season. Load the checked-in field contract before any
        // autonomous target, waypoint, or costmap is resolved.
        runCatching {
            hardwareMap.appContext.assets.open("paths/field.json").bufferedReader().use { reader ->
                com.areslib.state.RobotFieldDocument.decode(reader.readText())
            }
        }.mapCatching { config ->
            require(config.fieldType == com.areslib.state.FieldType.FTC) {
                "Canonical season field must declare FTC geometry"
            }
            require(config.apriltags.isNotEmpty()) { "FTC field must declare its AprilTag layout" }
            val tagIds = HashSet<Int>(config.apriltags.size)
            require(config.apriltags.all { tagIds.add(it.id) }) {
                "FTC field contains duplicate AprilTag IDs"
            }
            val tags = config.apriltags.associate { tag ->
                require(tag.id > 0 && tag.x.isFinite() && tag.y.isFinite() && tag.z.isFinite() && tag.yaw.isFinite()) {
                    "FTC field contains an invalid AprilTag"
                }
                tag.id to com.areslib.math.geometry.Pose3d(
                    com.areslib.math.geometry.Translation3d(tag.x, tag.y, tag.z),
                    com.areslib.math.geometry.Rotation3d(0.0, 0.0, Math.toRadians(tag.yaw))
                )
            }
            config to tags
        }.onSuccess { (config, tags) ->
            com.areslib.state.RobotFieldManager.setActiveConfig(config)
            // Auto and every TeleOp use the same checked-in field document. This assignment also
            // replaces the shared generic 1-4 square layout selected before this facade is built.
            com.areslib.math.estimation.PoseEstimator.activeTags = tags
            hasCanonicalFieldContract = true
        }.onFailure { error ->
            // Never continue vision localization against the generic/shared tag layout when the
            // season contract is missing or invalid. Manual drive remains available without tags.
            com.areslib.state.RobotFieldManager.setActiveConfig(
                com.areslib.state.RobotFieldConfig(
                    id = "unavailable-ftc-season-field",
                    name = "Unavailable FTC season field",
                    fieldType = com.areslib.state.FieldType.FTC,
                    widthMeters = 3.6576,
                    heightMeters = 3.6576,
                    apriltags = emptyList(),
                )
            )
            com.areslib.math.estimation.PoseEstimator.activeTags = emptyMap()
            hasCanonicalFieldContract = false
            addTelemetry("Field", "Canonical field unavailable; vision tags disabled: ${error.message}")
        }

        // Registrations are process-global. Clear the previous OpMode's optional hardware catalog
        // before discovering this robot instance so missing devices cannot inherit stale commands.
        com.areslib.pathing.NamedCommands.clear()

        try {
            val intakeIO = org.firstinspires.ftc.teamcode.hardware.FtcIntakeIO(hardwareMap) {
                base.powerManager.batteryVoltage
            }
            this.intakeIO = intakeIO
            intakeSubsystem = org.firstinspires.ftc.teamcode.subsystems.IntakeSubsystem(intakeIO)
            base.registerSubsystem(intakeSubsystem!!)
        } catch (e: Exception) {
            addTelemetry("Subsystem", "Intake failed to load: ${e.message}")
        }

        try {
            val flywheelIO = org.firstinspires.ftc.teamcode.hardware.FtcFlywheelIO(
                hardwareMap,
                ticksPerRev = FLYWHEEL_TICKS_PER_REV,
                maxRpm = FLYWHEEL_MAX_RPM,
                batteryVoltageSupplier = { base.powerManager.batteryVoltage },
            )
            this.flywheelIO = flywheelIO
            flywheelSubsystem = org.firstinspires.ftc.teamcode.subsystems.FlywheelSubsystem(flywheelIO)
            base.registerSubsystem(flywheelSubsystem!!)
        } catch (e: Exception) {
            addTelemetry("Subsystem", "Flywheel failed to load: ${e.message}")
        }

        // Optional hardware is discovered only by its canonical RC configuration name. A typo is
        // reported as missing instead of silently binding a different servo through fuzzy aliases.
        val primaryIO = runCatching {
            requireNotNull(
                hardwareMap.get(com.qualcomm.robotcore.hardware.Servo::class.java, "indicator")
            ) { "Optional FTC servo 'indicator' was not discovered" }
            com.areslib.ftc.hardware.FtcIndicatorLightIO(hardwareMap, "indicator")
        }.getOrNull()
        if (primaryIO != null) {
            base.registerSubsystem(org.firstinspires.ftc.teamcode.subsystems.IndicatorLightSubsystem(primaryIO, "indicator"))
            setIndicatorColor("indicator", com.areslib.hardware.actuator.IndicatorLightColor.GREEN)
        } else {
            addTelemetry("Subsystem", "Optional 'indicator' servo not configured")
        }

        val secondaryIO = runCatching {
            requireNotNull(
                hardwareMap.get(com.qualcomm.robotcore.hardware.Servo::class.java, "indicator2")
            ) { "Optional FTC servo 'indicator2' was not discovered" }
            com.areslib.ftc.hardware.FtcIndicatorLightIO(hardwareMap, "indicator2")
        }.getOrNull()
        if (secondaryIO != null) {
            base.registerSubsystem(org.firstinspires.ftc.teamcode.subsystems.IndicatorLightSubsystem(secondaryIO, "indicator2"))
            setSecondIndicatorColor(com.areslib.hardware.actuator.IndicatorLightColor.BLUE)
        } else {
            addTelemetry("Subsystem", "Optional 'indicator2' servo not configured")
        }

        FtcAutoCapabilities.registerIndicatorActions(
            primaryAvailable = primaryIO != null,
            secondaryAvailable = secondaryIO != null
        )

        // --- goBILDA Prism RGB LED Driver ("prism") ---
        val prismIOInstance = runCatching<com.areslib.hardware.actuator.PrismDriverIO> {
            requireNotNull(
                hardwareMap.get(com.qualcomm.robotcore.hardware.I2cDeviceSynch::class.java, "prism")
            ) { "Optional FTC I2C device 'prism' was not discovered" }
            com.areslib.ftc.hardware.FtcPrismDriverI2cIO(hardwareMap, "prism")
        }.recoverCatching {
            requireNotNull(
                hardwareMap.get(com.qualcomm.robotcore.hardware.Servo::class.java, "prism")
            ) { "Optional FTC servo 'prism' was not discovered" }
            com.areslib.ftc.hardware.FtcPrismDriverIO(hardwareMap, "prism")
        }.getOrNull()
        if (prismIOInstance != null) {
            prismIO = prismIOInstance
            base.registerSubsystem(org.firstinspires.ftc.teamcode.subsystems.PrismSubsystem(prismIOInstance, "prism"))
            setPrismPreset(com.areslib.hardware.actuator.PrismPwmPreset.RAINBOW_FULL_COLOR)
        } else {
            addTelemetry("Subsystem", "Optional 'prism' device not configured")
        }
        FtcAutoCapabilities.registerPrismActions(prismAvailable = prismIOInstance != null)

        FtcAutoCapabilities.registerMechanismActions(
            intakeAvailable = intakeSubsystem != null,
            flywheelAvailable = flywheelSubsystem != null
        )
        // Simulator game-piece interaction consumes only cached commands that actually reached
        // season IO after interlocks, brownout scaling, and fault latches. Dashboard intent is not
        // authoritative for mechanism state.
        base.simMechanismOutputProvider = object : com.areslib.ftc.sim.FtcSimMechanismStateProvider {
            override val intakeAccepted: Boolean
                get() = base.store.state.superstructure.season.intakeActive
            override val flywheelAccepted: Boolean
                get() = base.store.state.superstructure.season.flywheelActive
            override val intakeApplied: Boolean
                get() = intakeIO?.outputApplied == true
            override val flywheelApplied: Boolean
                get() = flywheelIO?.outputApplied == true
            override val transferApplied: Boolean
                get() = false // The DECODE robot has no independently actuated transfer.
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
        // Check both latches before touching any actuator. A failed instance can only recover
        // through normal OpMode reconstruction.
        val priorFailure = fatalSeasonFailure ?: base.fatalUpdateFailure
        if (priorFailure != null) {
            runCatching { base.safeAll() }
            runCatching { base.safeHardware() }
            throw priorFailure
        }
        try {
            // Refresh all registered IO, update drivetrain/EKF, and compute this frame's power
            // scale exactly once. A thrown shared update skips every season write and its safety
            // stop remains final.
            base.update(gamepad1, gamepad2)

            // Consume the season IO values cached by the shared refresh above.
            val timestamp = com.areslib.util.RobotClock.currentTimeMillis()
            base.readAllSensors(timestamp)

            if (intakeSubsystem?.stalled == true) {
                val seasonState = base.store.state.superstructure.season
                if (seasonState.intakeActive) {
                    base.store.dispatch(com.areslib.action.RobotAction.UpdateSubsystemState(seasonState.copy(intakeActive = false)))
                }
            }
            // Apply the freshly computed brownout/current scale to every season mechanism in the
            // same frame. Mechanism voltage normalization reads the same cached power sample.
            base.writeAllOutputs(base.powerManager.powerScale)

            // Continuously update core Driver Station telemetry.
            telemetryHelper.updateTelemetry()
        } catch (t: Throwable) {
            fatalSeasonFailure = t
            // Clear season intent as diagnostic state and perform best-effort safing. The latch
            // above prevents a later frame from writing these outputs before shared update runs.
            runCatching {
                base.store.dispatch(
                    com.areslib.action.RobotAction.UpdateSubsystemState(SeasonSuperstructureState())
                )
            }
            runCatching { base.safeAll() }
            runCatching { base.safeHardware() }
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

    /** Enables the shared calibration receiver only for a dedicated tuning OpMode. */
    fun enableCalibrationMode() {
        base.sysIdFlywheelIO = flywheelIO
        base.isLiveTuningEnabled = true
        try {
            base.enableCalibrationMode()
        } catch (failure: Throwable) {
            base.sysIdFlywheelIO = null
            base.isLiveTuningEnabled = false
            throw failure
        }
    }

    /** Safes characterization output and removes the season mechanism adapter. */
    fun disableCalibrationMode() {
        try {
            base.disableCalibrationMode()
        } finally {
            base.sysIdFlywheelIO = null
            base.isLiveTuningEnabled = false
        }
    }

    /** Zeroes outputs, closes season subsystems, then always closes shared robot resources. */
    fun close() {
        if (closed) return
        closed = true
        var firstFailure: Throwable? = null
        fun attempt(action: () -> Unit) {
            try {
                action()
            } catch (failure: Throwable) {
                val primary = firstFailure
                if (primary == null) firstFailure = failure
                else if (primary !== failure) primary.addSuppressed(failure)
            }
        }
        attempt(::disableCalibrationMode)
        attempt(base::safeAll)
        base.simMechanismOutputProvider = null
        attempt(base::closeSubsystems)
        attempt(base::close)
        firstFailure?.let { throw it }
    }
}
