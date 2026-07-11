package org.firstinspires.ftc.teamcode.opmodes

import com.areslib.ftc.FtcMecanumRobot
import com.qualcomm.robotcore.hardware.HardwareMap
import org.firstinspires.ftc.robotcore.external.Telemetry
import com.areslib.hardware.HardwareRegistry
import com.areslib.action.RobotAction

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
        blName = "rl",
        brName = "rr",
        localTelemetry = localTelemetry,
        trackWidthMeters = org.firstinspires.ftc.teamcode.config.TunerConstants.TRACK_WIDTH_METERS,
        wheelBaseMeters = org.firstinspires.ftc.teamcode.config.TunerConstants.WHEEL_BASE_METERS,
        headingKp = org.firstinspires.ftc.teamcode.config.TunerConstants.HEADING_KP,
        headingKi = org.firstinspires.ftc.teamcode.config.TunerConstants.HEADING_KI,
        headingKd = org.firstinspires.ftc.teamcode.config.TunerConstants.HEADING_KD,
        headingDeadzoneDeg = org.firstinspires.ftc.teamcode.config.TunerConstants.HEADING_DEADZONE_DEG,
        driveKs = org.firstinspires.ftc.teamcode.config.TunerConstants.DRIVE_KS,
        driveSlewRateLimit = org.firstinspires.ftc.teamcode.config.TunerConstants.DRIVE_SLEW_RATE_LIMIT,
        odomQx = org.firstinspires.ftc.teamcode.config.TunerConstants.ODOM_QX,
        odomQy = org.firstinspires.ftc.teamcode.config.TunerConstants.ODOM_QY,
        odomQtheta = org.firstinspires.ftc.teamcode.config.TunerConstants.ODOM_QTHETA,
        pinpointXOffsetMm = org.firstinspires.ftc.teamcode.config.TunerConstants.PINPOINT_X_OFFSET_MM,
        pinpointYOffsetMm = org.firstinspires.ftc.teamcode.config.TunerConstants.PINPOINT_Y_OFFSET_MM,
        pinpointEncoderResolution = org.firstinspires.ftc.teamcode.config.TunerConstants.PINPOINT_ENCODER_RESOLUTION,
        pinpointXDirection = org.firstinspires.ftc.teamcode.config.TunerConstants.PINPOINT_X_DIRECTION,
        pinpointYDirection = org.firstinspires.ftc.teamcode.config.TunerConstants.PINPOINT_Y_DIRECTION,
        motorKp = org.firstinspires.ftc.teamcode.config.TunerConstants.MOTOR_KP,
        motorKi = org.firstinspires.ftc.teamcode.config.TunerConstants.MOTOR_KI,
        motorKd = org.firstinspires.ftc.teamcode.config.TunerConstants.MOTOR_KD,
        motorKf = org.firstinspires.ftc.teamcode.config.TunerConstants.MOTOR_KF,
        visionStdDevs = com.areslib.math.Vector3(
            org.firstinspires.ftc.teamcode.config.TunerConstants.VISION_STD_DEVS_X,
            org.firstinspires.ftc.teamcode.config.TunerConstants.VISION_STD_DEVS_Y,
            org.firstinspires.ftc.teamcode.config.TunerConstants.VISION_STD_DEVS_HEADING
        ),
        visionFilterConfig = com.areslib.hardware.vision.VisionFilterConfig(
            maxDistanceMeters = org.firstinspires.ftc.teamcode.config.TunerConstants.VISION_MAX_DISTANCE_METERS,
            maxAmbiguity = org.firstinspires.ftc.teamcode.config.TunerConstants.VISION_MAX_AMBIGUITY,
            mahalanobisThreshold = org.firstinspires.ftc.teamcode.config.TunerConstants.VISION_MAHALANOBIS_THRESHOLD
        )
    )
    
    val intakeIO = org.firstinspires.ftc.teamcode.hardware.FtcIntakeIO(hardwareMap)
    val flywheelIO = org.firstinspires.ftc.teamcode.hardware.FtcFlywheelIO(hardwareMap)

    init {
        HardwareRegistry.registerDevice("Superstructure/Intake", intakeIO)
        HardwareRegistry.registerDevice("Superstructure/Flywheel", flywheelIO)
        HardwareRegistry.registerCloseable(intakeIO)
        HardwareRegistry.registerCloseable(flywheelIO)
    }

    fun update() {
        val timestamp = com.areslib.util.RobotClock.currentTimeMillis()
        
        // 1. Update drivebase sensors, EKF, and kinematics
        base.update()

        // 2. Read flywheel sensor RPM and update Redux store
        val currentRPM = flywheelIO.velocityRpm
        base.store.dispatch(RobotAction.UpdateFlywheelRPM(currentRPM, timestamp))

        // 3. Command actuators based on latest Redux target state
        val state = base.store.state

        if (state.superstructure.intakeActive) {
            intakeIO.setRollerVoltage(12.0)
        } else {
            intakeIO.setRollerVoltage(0.0)
        }

        if (state.superstructure.flywheelActive) {
            flywheelIO.setVelocityRpm(state.superstructure.flywheelTargetRPM)
        } else {
            flywheelIO.setVelocityRpm(0.0)
        }
    }

    fun driveFieldCentric(x: Double, y: Double, rotation: Double) {
        base.driveFieldCentric(x, y, rotation)
    }

    fun driveRobotCentric(x: Double, y: Double, rotation: Double) {
        base.driveRobotCentric(x, y, rotation)
    }

    fun close() {
        base.close()
        intakeIO.close()
        flywheelIO.close()
    }
}
