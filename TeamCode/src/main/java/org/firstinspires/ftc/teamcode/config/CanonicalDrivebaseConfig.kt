package org.firstinspires.ftc.teamcode.config

import com.areslib.control.tuning.PIDFCoefficients
import com.areslib.control.tuning.SimpleFeedforwardCoeffs
import com.areslib.state.DriveTuningState
import com.areslib.state.EkfProcessNoiseTuningState
import com.areslib.state.FtcDriveTuningState
import com.areslib.state.FtcPinpointTuningState
import com.areslib.state.LocalizationTuningState
import com.areslib.state.TuningState
import com.areslib.tuning.TypedTuningRuntime
import com.qualcomm.hardware.gobilda.GoBildaPinpointDriver
import com.qualcomm.robotcore.hardware.DcMotorSimple
import org.firstinspires.ftc.teamcode.generated.drivebase.GeneratedAresDrivebaseConfig
import org.firstinspires.ftc.teamcode.generated.drivebase.GeneratedAresTuningConfig

/**
 * Student-readable adapter from generated mechanical plumbing to the shared robot constructor.
 *
 * Edit `.ares/drivetrains/gobilda-mecanum.aresdrivetrain` and
 * `.ares/tuning/competition.arestuning`, never the generated Kotlin under `build/`. This adapter
 * deliberately contains mapping code only: physical values and profile values stay declarative.
 */
object CanonicalDrivebaseConfig {
    private val values get() = GeneratedAresTuningConfig.Parameters

    /** True only when a tuning update is fully consumed by [withRuntimeValues]. */
    fun supportsRuntimeParameter(parameterUid: String): Boolean = parameterUid in reduxParameterUids

    val frontLeftDirection: DcMotorSimple.Direction get() = direction(GeneratedAresDrivebaseConfig.Components.FTC_MOTOR_FL.INVERTED)
    val frontRightDirection: DcMotorSimple.Direction get() = direction(GeneratedAresDrivebaseConfig.Components.FTC_MOTOR_FR.INVERTED)
    val rearLeftDirection: DcMotorSimple.Direction get() = direction(GeneratedAresDrivebaseConfig.Components.FTC_MOTOR_RL.INVERTED)
    val rearRightDirection: DcMotorSimple.Direction get() = direction(GeneratedAresDrivebaseConfig.Components.FTC_MOTOR_RR.INVERTED)
    val pinpointXDirection: GoBildaPinpointDriver.EncoderDirection get() = encoderDirection(values.LOCALIZATION_PINPOINTXREVERSED)
    val pinpointYDirection: GoBildaPinpointDriver.EncoderDirection get() = encoderDirection(values.LOCALIZATION_PINPOINTYREVERSED)
    val pinpointEncoderResolution: Double? get() = values.LOCALIZATION_PINPOINTENCODERRESOLUTION.takeIf { it > 0.0 }

    /** Canonical Redux state is installed before controllers and typed tuning transport start. */
    fun initialTuningState(): TuningState = tuningState(TuningState(), null)

    /** Rebuilds only drive/localization slices after a policy-approved typed tuning update. */
    fun withRuntimeValues(current: TuningState, runtime: TypedTuningRuntime): TuningState =
        tuningState(current, runtime)

    private fun tuningState(current: TuningState, runtime: TypedTuningRuntime?): TuningState {
        fun number(uid: String, canonical: Double): Double = runtime?.double(uid) ?: canonical
        val drive = current.drive.copy(
            trackWidthMeters = GeneratedAresDrivebaseConfig.TRACK_WIDTH_METERS,
            wheelBaseMeters = GeneratedAresDrivebaseConfig.WHEEL_BASE_METERS,
            pathTranslationGains = PIDFCoefficients(
                number("ftc.drive.path-translation.kp", values.DRIVE_PATHTRANSLATIONKP),
                0.0,
                number("ftc.drive.path-translation.kd", values.DRIVE_PATHTRANSLATIONKD),
            ),
            pathRotationGains = PIDFCoefficients(
                number("ftc.drive.path-rotation.kp", values.DRIVE_PATHROTATIONKP),
                0.0,
                number("ftc.drive.path-rotation.kd", values.DRIVE_PATHROTATIONKD),
            ),
            headingGains = PIDFCoefficients(
                number("ftc.drive.heading.kp", values.DRIVE_HEADINGKP),
                number("ftc.drive.heading.ki", values.DRIVE_HEADINGKI),
                number("ftc.drive.heading.kd", values.DRIVE_HEADINGKD),
            ),
            headingDeadzoneDeg = number("ftc.drive.heading.deadzone", values.DRIVE_HEADINGDEADZONEDEG),
            driveFeedforward = SimpleFeedforwardCoeffs(
                number("ftc.drive.feedforward.ks", values.DRIVE_FEEDFORWARDKS),
                number("ftc.drive.feedforward.kv", values.DRIVE_FEEDFORWARDKV),
                number("ftc.drive.feedforward.ka", values.DRIVE_FEEDFORWARDKA),
            ),
            pathVelocityScale = number("ftc.drive.path.velocity-scale", values.DRIVE_PATHVELOCITYSCALE),
            pathAccelerationLimit = number("ftc.drive.path.acceleration-limit", values.DRIVE_PATHACCELERATIONLIMIT),
            ftc = FtcDriveTuningState(
                ticksPerMeter = number("ftc.drive.ticks-per-meter", values.DRIVE_TICKSPERMETER),
                motorGains = PIDFCoefficients(
                    number("ftc.drive.motor.kp", values.DRIVE_MOTORKP),
                    number("ftc.drive.motor.ki", values.DRIVE_MOTORKI),
                    number("ftc.drive.motor.kd", values.DRIVE_MOTORKD),
                    number("ftc.drive.motor.kf", values.DRIVE_MOTORKF),
                ),
            ),
        )
        val localization = LocalizationTuningState(
            ekfNoise = EkfProcessNoiseTuningState(
                number("ftc.localization.ekf.qx", values.LOCALIZATION_EKFQX),
                number("ftc.localization.ekf.qy", values.LOCALIZATION_EKFQY),
                number("ftc.localization.ekf.qtheta", values.LOCALIZATION_EKFQTHETA),
            ),
            ftcPinpoint = FtcPinpointTuningState(
                number("ftc.localization.pinpoint.x-offset", values.LOCALIZATION_PINPOINTXOFFSETMM),
                number("ftc.localization.pinpoint.y-offset", values.LOCALIZATION_PINPOINTYOFFSETMM),
                number("ftc.localization.pinpoint.encoder-resolution", values.LOCALIZATION_PINPOINTENCODERRESOLUTION),
            ),
        )
        return current.copy(drive = drive, localization = localization)
    }

    private fun direction(inverted: Boolean) =
        if (inverted) DcMotorSimple.Direction.REVERSE else DcMotorSimple.Direction.FORWARD

    private fun encoderDirection(reversed: Boolean): GoBildaPinpointDriver.EncoderDirection {
        val directions = GoBildaPinpointDriver.EncoderDirection.values()
        // FTC SDK names this second value REVERSED while the desktop mock names it REVERSE.
        // Both preserve the same two-value ordinal contract; this runs only during construction.
        return if (reversed) directions.last() else directions.first()
    }

    private val reduxParameterUids = setOf(
        "ftc.drive.ticks-per-meter",
        "ftc.drive.feedforward.ks",
        "ftc.drive.feedforward.kv",
        "ftc.drive.feedforward.ka",
        "ftc.drive.motor.kp",
        "ftc.drive.motor.ki",
        "ftc.drive.motor.kd",
        "ftc.drive.motor.kf",
        "ftc.drive.heading.kp",
        "ftc.drive.heading.ki",
        "ftc.drive.heading.kd",
        "ftc.drive.heading.deadzone",
        "ftc.drive.path-translation.kp",
        "ftc.drive.path-translation.kd",
        "ftc.drive.path-rotation.kp",
        "ftc.drive.path-rotation.kd",
        "ftc.drive.path.velocity-scale",
        "ftc.drive.path.acceleration-limit",
        "ftc.localization.pinpoint.x-offset",
        "ftc.localization.pinpoint.y-offset",
        "ftc.localization.pinpoint.encoder-resolution",
        "ftc.localization.ekf.qx",
        "ftc.localization.ekf.qy",
        "ftc.localization.ekf.qtheta",
    )
}
