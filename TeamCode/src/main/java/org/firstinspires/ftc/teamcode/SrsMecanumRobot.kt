package org.firstinspires.ftc.teamcode

import com.qualcomm.robotcore.hardware.HardwareMap
import com.qualcomm.robotcore.hardware.DcMotorEx
import com.qualcomm.robotcore.hardware.VoltageSensor
import com.areslib.subsystem.AresRobot
import com.areslib.ftc.hardware.SrsHubDriver
import com.areslib.ftc.hardware.SrsHubEncoderIO
import com.areslib.ftc.hardware.SrsHubAnalogIO
import com.areslib.ftc.hardware.SrsHubPinpointOdometry
import com.areslib.ftc.hardware.FtcMotor
import com.areslib.ftc.hardware.CompositeMotorIO
import com.areslib.ftc.hardware.FtcFloodgateCurrentSensor
import com.areslib.ftc.hardware.FtcPerformanceManager
import com.areslib.kinematics.MecanumKinematics
import com.areslib.kinematics.MecanumWheelSpeeds
import com.areslib.math.ChassisSpeeds
import com.areslib.action.RobotAction
import com.areslib.telemetry.NT4Telemetry
import com.areslib.telemetry.DataLoggingTelemetry
import com.areslib.telemetry.ARESNetworkStatePublisher
import com.areslib.control.BrownoutGuard
import com.areslib.util.RobotClock

/**
 * Custom Hardware Facade for a Simple Mecanum Wheeled FTC Robot.
 * 
 * Optimized specifically for:
 * - 4x GoBilda 312 RPM Yellow Jacket Motors (Drivetrain)
 * - 104mm GoBilda Gripforce Wheels
 * - Quadrature encoders routed through the SRS Hub (Ports 0, 1, 2, 3)
 * - GoBilda Floodgate Current Switch analog telemetry routed through SRS Hub Analog Port 0
 * - GoBilda Pinpoint Odometry Computer routed through SRS Hub I2C Port 0
 */
class SrsMecanumRobot(
    val hardwareMap: HardwareMap,
    flName: String = "fl",
    frName: String = "fr",
    blName: String = "bl",
    brName: String = "br",
    srsHubName: String = "srsHub",
    private val localTelemetry: Any? = null
) : AresRobot() {

    // 1. Initialize Telemetry & Network Tables State Publishers
    private val nt4 = NT4Telemetry()
    private val dataLoggingTelemetry = DataLoggingTelemetry(nt4)
    private val publisher = ARESNetworkStatePublisher(dataLoggingTelemetry)

    // 2. Fetch the SRS Hub Driver
    val srsHub: SrsHubDriver = hardwareMap.get(SrsHubDriver::class.java, srsHubName)

    // 3. Physical Motors (REV Hub Power + SRS Hub Encoder Feedback)
    private val rawFl = hardwareMap.get(DcMotorEx::class.java, flName)
    private val rawFr = hardwareMap.get(DcMotorEx::class.java, frName)
    private val rawBl = hardwareMap.get(DcMotorEx::class.java, blName)
    private val rawBr = hardwareMap.get(DcMotorEx::class.java, brName)

    val flIO = CompositeMotorIO(FtcMotor(rawFl), SrsHubEncoderIO(srsHub, 0))
    val frIO = CompositeMotorIO(FtcMotor(rawFr), SrsHubEncoderIO(srsHub, 1))
    val blIO = CompositeMotorIO(FtcMotor(rawBl), SrsHubEncoderIO(srsHub, 2))
    val brIO = CompositeMotorIO(FtcMotor(rawBr), SrsHubEncoderIO(srsHub, 3))

    // 4. Pinpoint Odometry routed through SRS Hub I2C Port 0
    val pinpointIO = SrsHubPinpointOdometry(srsHub, port = 0)
    private val pinpointInputs = com.areslib.hardware.OdometryInputs()

    // 5. Floodgate Current Switch routed through SRS Hub Analog Port 0
    val floodgate = FtcFloodgateCurrentSensor(SrsHubAnalogIO(srsHub, port = 0))

    // 6. Kinematics parameters optimized for 104mm GoBilda Gripforce & 312 RPM motors
    // Wheel diameter = 104mm (0.104m) -> radius = 0.052m
    // Free speed of 312 RPM = 5.2 rev/s -> max velocity = 5.2 * pi * 0.104 = 1.70 m/s
    private val maxWheelSpeedMps = 1.70 
    private val kinematics = MecanumKinematics(
        trackWidthMeters = 0.38, // Standard GoBilda Strafer track width
        wheelBaseMeters = 0.38   // Standard GoBilda Strafer wheel base
    )

    // 7. Safety, caching, and loop tuning variables
    val brownoutGuard = BrownoutGuard.ftcDefaults()
    private var lastUpdateTime = 0L
    private var lastVoltageReadTime = 0L
    private var cachedBatteryVoltage = 12.0

    init {
        // Set motor directions (typical mecanum: reverse right side)
        rawFl.direction = com.qualcomm.robotcore.hardware.DcMotorSimple.Direction.FORWARD
        rawBl.direction = com.qualcomm.robotcore.hardware.DcMotorSimple.Direction.FORWARD
        rawFr.direction = com.qualcomm.robotcore.hardware.DcMotorSimple.Direction.REVERSE
        rawBr.direction = com.qualcomm.robotcore.hardware.DcMotorSimple.Direction.REVERSE

        // Initialize Performance Manager (Manual Bulk Caching & Photon detection)
        FtcPerformanceManager.initialize(hardwareMap)
        
        // Start pinpoint pose estimation at origin
        pinpointIO.initialize(com.areslib.math.Pose2d())
        
        // Reset the floodgate energy integration tracker
        floodgate.resetTracker()
    }

    /**
     * Call exactly once at the beginning of each OpMode loop iteration.
     * Executes single atomic register scans and updates the central Redux state.
     */
    fun update(gamepad1: com.areslib.telemetry.GamepadState? = null, gamepad2: com.areslib.telemetry.GamepadState? = null) {
        val timestamp = RobotClock.currentTimeMillis()
        lastUpdateTime = timestamp

        // 1. Core Performance Step: trigger single integrated 256-byte bulk scan of all SRS Hub registers
        FtcPerformanceManager.clearBulkCaches()

        // 2. Read pinpoint inputs from the SRS Hub cache and dispatch to the Redux state tree
        pinpointIO.updateInputs(pinpointInputs)
        store.dispatch(RobotAction.PoseUpdate(
            xMeters = pinpointInputs.posX,
            yMeters = pinpointInputs.posY,
            headingRadians = pinpointInputs.heading,
            timestampMs = timestamp
        ))

        // 3. Map human joystick driver intent into target field-relative speeds
        val vx = store.state.drive.xVelocityMetersPerSecond * maxWheelSpeedMps
        val vy = store.state.drive.yVelocityMetersPerSecond * maxWheelSpeedMps
        val omega = store.state.drive.angularVelocityRadiansPerSecond * maxWheelSpeedMps

        val robotHeading = store.state.drive.poseEstimator.estimatedPose.heading
        val chassisSpeeds = ChassisSpeeds.fromFieldRelativeSpeeds(vx, vy, omega, robotHeading)
        val wheelSpeeds = kinematics.toWheelSpeeds(chassisSpeeds)

        // 4. Rate-limited battery voltage check to compensate for motor sag without blocking loop
        if (timestamp - lastVoltageReadTime > 100 || lastVoltageReadTime == 0L) {
            lastVoltageReadTime = timestamp
            val voltageSensors = hardwareMap.getAll(VoltageSensor::class.java)
            cachedBatteryVoltage = if (voltageSensors.isNotEmpty()) voltageSensors[0].voltage else 12.0
        }
        val batteryVoltage = cachedBatteryVoltage

        // Apply battery-compensated voltage vectors
        val maxVolts = 12.0
        val voltageCompensationFactor = maxVolts / batteryVoltage

        val flPower = ((wheelSpeeds.frontLeftMetersPerSecond / maxWheelSpeedMps) * voltageCompensationFactor).coerceIn(-1.0, 1.0)
        val frPower = ((wheelSpeeds.frontRightMetersPerSecond / maxWheelSpeedMps) * voltageCompensationFactor).coerceIn(-1.0, 1.0)
        val blPower = ((wheelSpeeds.backLeftMetersPerSecond / maxWheelSpeedMps) * voltageCompensationFactor).coerceIn(-1.0, 1.0)
        val brPower = ((wheelSpeeds.backRightMetersPerSecond / maxWheelSpeedMps) * voltageCompensationFactor).coerceIn(-1.0, 1.0)

        // 5. Active safety: Brownout protection and Floodgate thermal current limiter
        brownoutGuard.update(batteryVoltage)
        var effectiveScale = brownoutGuard.powerScale

        // Process GoBilda Floodgate switch analog current updates
        floodgate.update()
        if (floodgate.isOverloadWarning()) {
            val fuseScale = (1.0 - floodgate.fuseThermalLoadPercent / 100.0).coerceIn(0.2, 1.0)
            effectiveScale = minOf(effectiveScale, fuseScale)
        }

        // Apply scaled, deduplicated powers to hardware motor ports
        flIO.powerScale = effectiveScale
        frIO.powerScale = effectiveScale
        blIO.powerScale = effectiveScale
        brIO.powerScale = effectiveScale

        flIO.power = flPower
        frIO.power = frPower
        blIO.power = blPower
        brIO.power = brPower

        // 6. Automatically publish loop data asynchronously to NetworkTables
        publisher.publish(store.state, gamepad1, gamepad2)

        // 7. Human-readable local driver station telemetry
        if (localTelemetry != null) {
            try {
                val addDataMethod = localTelemetry.javaClass.getMethod("addData", String::class.java, Any::class.java)
                val updateMethod = localTelemetry.javaClass.getMethod("update")

                addDataMethod.invoke(localTelemetry, "Current Draw", String.format("%.2f Amps", floodgate.current))
                addDataMethod.invoke(localTelemetry, "Fuse Load", String.format("%.1f%%", floodgate.fuseThermalLoadPercent))
                addDataMethod.invoke(localTelemetry, "Voltage", String.format("%.2f V", batteryVoltage))
                addDataMethod.invoke(localTelemetry, "Pose", String.format("(%.2f, %.2f) %.1f°",
                    store.state.drive.poseEstimator.estimatedPose.x,
                    store.state.drive.poseEstimator.estimatedPose.y,
                    Math.toDegrees(store.state.drive.poseEstimator.estimatedPose.heading.radians)
                ))

                updateMethod.invoke(localTelemetry)
            } catch (_: Exception) {}
        }
    }

    /**
     * Cleanly shuts down data logging and background network channels.
     */
    fun close() {
        dataLoggingTelemetry.close()
    }
}
