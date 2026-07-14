package org.firstinspires.ftc.teamcode.opmodes

import com.areslib.ftc.FtcMecanumRobot
import com.qualcomm.robotcore.hardware.HardwareMap
import org.firstinspires.ftc.robotcore.external.Telemetry
import com.areslib.hardware.HardwareRegistry
import com.areslib.action.RobotAction
import org.firstinspires.ftc.teamcode.dsl.*

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
        rlName = "rl",
        rrName = "rr",
        localTelemetry = localTelemetry,
        trackWidthMeters = org.firstinspires.ftc.teamcode.config.TunerConstants.TRACK_WIDTH_METERS,
        wheelBaseMeters = org.firstinspires.ftc.teamcode.config.TunerConstants.WHEEL_BASE_METERS,
        headingGains = com.areslib.control.tuning.PIDFCoefficients(
            org.firstinspires.ftc.teamcode.config.TunerConstants.HEADING_KP,
            org.firstinspires.ftc.teamcode.config.TunerConstants.HEADING_KI,
            org.firstinspires.ftc.teamcode.config.TunerConstants.HEADING_KD
        ),
        headingDeadzoneDeg = org.firstinspires.ftc.teamcode.config.TunerConstants.HEADING_DEADZONE_DEG,
        driveFeedforward = com.areslib.control.tuning.SimpleFeedforwardCoeffs(
            org.firstinspires.ftc.teamcode.config.TunerConstants.DRIVE_KS
        ),
        driveSlewRateLimit = org.firstinspires.ftc.teamcode.config.TunerConstants.DRIVE_SLEW_RATE_LIMIT,
        pathTranslationGains = com.areslib.control.tuning.PIDFCoefficients(
            org.firstinspires.ftc.teamcode.config.TunerConstants.PATH_TRANSLATION_KP,
            org.firstinspires.ftc.teamcode.config.TunerConstants.PATH_TRANSLATION_KI,
            org.firstinspires.ftc.teamcode.config.TunerConstants.PATH_TRANSLATION_KD
        ),
        pathRotationGains = com.areslib.control.tuning.PIDFCoefficients(
            org.firstinspires.ftc.teamcode.config.TunerConstants.PATH_ROTATION_KP,
            org.firstinspires.ftc.teamcode.config.TunerConstants.PATH_ROTATION_KI,
            org.firstinspires.ftc.teamcode.config.TunerConstants.PATH_ROTATION_KD
        ),
        odomQx = org.firstinspires.ftc.teamcode.config.TunerConstants.ODOM_QX,
        odomQy = org.firstinspires.ftc.teamcode.config.TunerConstants.ODOM_QY,
        odomQtheta = org.firstinspires.ftc.teamcode.config.TunerConstants.ODOM_QTHETA,
        pinpointXOffsetMm = org.firstinspires.ftc.teamcode.config.TunerConstants.PINPOINT_X_OFFSET_MM,
        pinpointYOffsetMm = org.firstinspires.ftc.teamcode.config.TunerConstants.PINPOINT_Y_OFFSET_MM,
        pinpointEncoderResolution = org.firstinspires.ftc.teamcode.config.TunerConstants.PINPOINT_ENCODER_RESOLUTION,
        pinpointXDirection = org.firstinspires.ftc.teamcode.config.TunerConstants.PINPOINT_X_DIRECTION,
        pinpointYDirection = org.firstinspires.ftc.teamcode.config.TunerConstants.PINPOINT_Y_DIRECTION,
        motorGains = com.areslib.control.tuning.PIDFCoefficients(
            org.firstinspires.ftc.teamcode.config.TunerConstants.MOTOR_KP ?: 0.0,
            org.firstinspires.ftc.teamcode.config.TunerConstants.MOTOR_KI ?: 0.0,
            org.firstinspires.ftc.teamcode.config.TunerConstants.MOTOR_KD ?: 0.0,
            org.firstinspires.ftc.teamcode.config.TunerConstants.MOTOR_KF ?: 0.0
        ),
        visionStdDevs = com.areslib.math.geometry.Vector3(
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
        
        // Initialize subsystem states in the Redux store
        base.store.dispatch(RobotAction.UpdateSubsystemState(IntakeState()))
        base.store.dispatch(RobotAction.UpdateSubsystemState(FlywheelState()))
    }

    fun update() {
        val timestamp = com.areslib.util.RobotClock.currentTimeMillis()
        
        // 0. Poll tuning from dashboard
        pollTuning()

        // 1. Update drivebase sensors, EKF, and kinematics
        base.update()

        // 2. Read flywheel sensor RPM and update Redux store
        val currentRPM = flywheelIO.velocityRpm
        val state = base.store.state
        base.store.dispatch(RobotAction.UpdateSubsystemState(
            FlywheelState(
                flywheelActive = state.superstructure.flywheelActive,
                flywheelTargetRPM = state.superstructure.flywheelTargetRPM,
                currentRPM = currentRPM
            )
        ))

        // 3. Command actuators based on latest Redux target state
        val updatedState = base.store.state

        if (updatedState.superstructure.intakeActive) {
            intakeIO.setRollerVoltage(12.0)
        } else {
            intakeIO.setRollerVoltage(0.0)
        }

        if (updatedState.superstructure.flywheelActive) {
            flywheelIO.setVelocityRpm(updatedState.superstructure.flywheelTargetRPM)
        } else {
            flywheelIO.setVelocityRpm(0.0)
        }
    }

    private fun pollTuning() {
        val nt4 = base.telemetryManager.dataLoggingTelemetry
        
        // Only update if the dashboard has sent a tuning flag or something.
        // Actually, NT4 Client caches the last received value. So we can just read them.
        // To avoid constantly dispatching UpdateTuningState, we check if they changed, or just do it.
        // Redux diffing will ignore if the data class is .equals(), so it's cheap to just dispatch.
        // But allocating a new TuningState every frame is against zero-GC!
        // We should check if they changed from current state.
        
        val currentState = base.store.state.tuning
        var changed = false
        
        val trackWidthMeters = nt4.getNumber("Tuning/TrackWidthMeters", currentState.trackWidthMeters)
        val wheelBaseMeters = nt4.getNumber("Tuning/WheelBaseMeters", currentState.wheelBaseMeters)
        val pathTranslationKp = nt4.getNumber("Tuning/PathTranslationKp", currentState.pathTranslationGains.kP)
        val pathTranslationKi = nt4.getNumber("Tuning/PathTranslationKi", currentState.pathTranslationGains.kI)
        val pathTranslationKd = nt4.getNumber("Tuning/PathTranslationKd", currentState.pathTranslationGains.kD)
        val pathRotationKp = nt4.getNumber("Tuning/PathRotationKp", currentState.pathRotationGains.kP)
        val pathRotationKi = nt4.getNumber("Tuning/PathRotationKi", currentState.pathRotationGains.kI)
        val pathRotationKd = nt4.getNumber("Tuning/PathRotationKd", currentState.pathRotationGains.kD)
        val headingKp = nt4.getNumber("Tuning/HeadingKp", currentState.headingGains.kP)
        val headingKi = nt4.getNumber("Tuning/HeadingKi", currentState.headingGains.kI)
        val headingKd = nt4.getNumber("Tuning/HeadingKd", currentState.headingGains.kD)
        val headingDeadzoneDeg = nt4.getNumber("Tuning/HeadingDeadzoneDeg", currentState.headingDeadzoneDeg)
        val driveKs = nt4.getNumber("Tuning/DriveKs", currentState.driveFeedforward.kS)
        val driveSlewRateLimit = nt4.getNumber("Tuning/DriveSlewRateLimit", currentState.driveSlewRateLimit ?: -1.0)
        val motorKp = nt4.getNumber("Tuning/MotorKp", currentState.motorGains?.kP ?: -1.0)
        val motorKi = nt4.getNumber("Tuning/MotorKi", currentState.motorGains?.kI ?: -1.0)
        val motorKd = nt4.getNumber("Tuning/MotorKd", currentState.motorGains?.kD ?: -1.0)
        val motorKf = nt4.getNumber("Tuning/MotorKf", currentState.motorGains?.kF ?: -1.0)
        val visionStdDevsX = nt4.getNumber("Tuning/VisionStdDevsX", currentState.visionStdDevsX)
        val visionStdDevsY = nt4.getNumber("Tuning/VisionStdDevsY", currentState.visionStdDevsY)
        val visionStdDevsHeading = nt4.getNumber("Tuning/VisionStdDevsHeading", currentState.visionStdDevsHeading)
        val visionMaxDistanceMeters = nt4.getNumber("Tuning/VisionMaxDistanceMeters", currentState.visionMaxDistanceMeters)
        val visionMaxAmbiguity = nt4.getNumber("Tuning/VisionMaxAmbiguity", currentState.visionMaxAmbiguity)
        val visionMahalanobisThreshold = nt4.getNumber("Tuning/VisionMahalanobisThreshold", currentState.visionMahalanobisThreshold)
        
        // Fast-path checking
        if (trackWidthMeters != currentState.trackWidthMeters || wheelBaseMeters != currentState.wheelBaseMeters ||
            pathTranslationKp != currentState.pathTranslationGains.kP || pathRotationKp != currentState.pathRotationGains.kP ||
            headingKp != currentState.headingGains.kP || headingKd != currentState.headingGains.kD || 
            visionStdDevsX != currentState.visionStdDevsX || visionMaxAmbiguity != currentState.visionMaxAmbiguity) {
            changed = true
        }

        if (changed) {
            val newTuning = com.areslib.state.TuningState(
                trackWidthMeters = trackWidthMeters,
                wheelBaseMeters = wheelBaseMeters,
                pathTranslationGains = com.areslib.control.tuning.PIDFCoefficients(pathTranslationKp, pathTranslationKi, pathTranslationKd),
                pathRotationGains = com.areslib.control.tuning.PIDFCoefficients(pathRotationKp, pathRotationKi, pathRotationKd),
                headingGains = com.areslib.control.tuning.PIDFCoefficients(headingKp, headingKi, headingKd),
                headingDeadzoneDeg = headingDeadzoneDeg,
                driveFeedforward = com.areslib.control.tuning.SimpleFeedforwardCoeffs(driveKs),
                driveSlewRateLimit = if (driveSlewRateLimit < 0.0) null else driveSlewRateLimit,
                motorGains = if (motorKp < 0.0 && motorKi < 0.0 && motorKd < 0.0 && motorKf < 0.0) null else {
                    com.areslib.control.tuning.PIDFCoefficients(
                        if (motorKp < 0.0) 0.0 else motorKp,
                        if (motorKi < 0.0) 0.0 else motorKi,
                        if (motorKd < 0.0) 0.0 else motorKd,
                        if (motorKf < 0.0) 0.0 else motorKf
                    )
                },
                visionStdDevsX = visionStdDevsX,
                visionStdDevsY = visionStdDevsY,
                visionStdDevsHeading = visionStdDevsHeading,
                visionMaxDistanceMeters = visionMaxDistanceMeters,
                visionMaxAmbiguity = visionMaxAmbiguity,
                visionMahalanobisThreshold = visionMahalanobisThreshold
            )
            base.store.dispatch(RobotAction.UpdateTuningState(newTuning))
        }
    }

    // Action Composition Helpers
    fun setIntakeActive(active: Boolean) {
        val currentState = base.store.state.superstructure
        base.store.dispatch(RobotAction.UpdateSubsystemState(
            IntakeState(intakeActive = active)
        ))
    }

    fun setFlywheelActive(active: Boolean, targetRPM: Double = 3000.0) {
        val currentState = base.store.state.superstructure
        base.store.dispatch(RobotAction.UpdateSubsystemState(
            FlywheelState(
                flywheelActive = active,
                flywheelTargetRPM = targetRPM,
                currentRPM = currentState.currentRPM // Preserve current reading
            )
        ))
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

