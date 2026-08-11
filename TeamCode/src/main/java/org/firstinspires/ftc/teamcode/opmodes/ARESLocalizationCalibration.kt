package org.firstinspires.ftc.teamcode.opmodes

import com.areslib.hardware.HardwareRegistry
import com.areslib.math.estimation.LocalizationCalibrationCheckpoint
import com.areslib.math.estimation.LocalizationCalibrationPlatform
import com.areslib.math.estimation.LocalizationCalibrationRecorder
import com.areslib.math.estimation.LocalizationCalibrationSample
import com.areslib.math.estimation.LocalizationCalibrationTestType
import com.areslib.math.geometry.Pose2d
import com.areslib.math.geometry.Rotation2d
import com.areslib.util.RobotClock
import com.qualcomm.robotcore.eventloop.opmode.TeleOp
import org.firstinspires.ftc.teamcode.dsl.AresTeleOpBase

/**
 * Driver-operated localization calibration collector.
 *
 * Controls:
 * - B: cycle test type
 * - D-pad: adjust surveyed X/Y by 5 cm
 * - Bumpers: adjust surveyed heading by 5 degrees
 * - Back: zero surveyed pose
 * - Start: seed robot localization to surveyed pose
 * - A: toggle stationary/combined frame recording
 * - X/Y: record surveyed route START/END checkpoints
 */
@TeleOp(name = "ARES Localization Calibration", group = "Tuning")
class ARESLocalizationCalibration : AresTeleOpBase() {
    private var testType = LocalizationCalibrationTestType.VISION_STATIONARY
    private var truthX = 0.0
    private var truthY = 0.0
    private var truthHeading = 0.0
    private var runId = 1
    private var continuousRecording = false
    private var recorder: LocalizationCalibrationRecorder? = null
    private var pendingCheckpoint = LocalizationCalibrationCheckpoint.NONE
    private var pendingRunId = 0
    private var lastRecordedVisionTimestampMs = Long.MIN_VALUE

    override fun define() = teleOp {
        setup {
            recorder = LocalizationCalibrationRecorder(LocalizationCalibrationPlatform.FTC).also {
                HardwareRegistry.registerCloseable(it)
            }
            robot.base.isLiveTuningEnabled = false
            robot.addTelemetry("Calibration", "Use surveyed poses; do not use Limelight as truth")
        }

        controls {
            driver.b.onPress("Cycle localization calibration test") {
                val values = LocalizationCalibrationTestType.entries
                testType = values[(testType.ordinal + 1) % values.size]
                continuousRecording = false
            }
            driver.dpadRight.onPress("Increase surveyed X by 5 cm") { truthX += 0.05 }
            driver.dpadLeft.onPress("Decrease surveyed X by 5 cm") { truthX -= 0.05 }
            driver.dpadUp.onPress("Increase surveyed Y by 5 cm") { truthY += 0.05 }
            driver.dpadDown.onPress("Decrease surveyed Y by 5 cm") { truthY -= 0.05 }
            driver.rightBumper.onPress("Increase surveyed heading by 5 degrees") {
                truthHeading = com.areslib.math.wrapAngle(truthHeading + Math.toRadians(5.0))
            }
            driver.leftBumper.onPress("Decrease surveyed heading by 5 degrees") {
                truthHeading = com.areslib.math.wrapAngle(truthHeading - Math.toRadians(5.0))
            }
            driver.back.onPress("Zero surveyed pose") {
                truthX = 0.0
                truthY = 0.0
                truthHeading = 0.0
            }
            driver.start.onPress("Seed localization to surveyed pose") {
                robot.base.resetPose(Pose2d(truthX, truthY, Rotation2d(truthHeading)))
            }
            driver.a.onPress("Toggle stationary calibration recording") {
                continuousRecording = !continuousRecording
            }
            driver.x.onPress("Record surveyed route start") {
                robot.base.resetPose(Pose2d(truthX, truthY, Rotation2d(truthHeading)))
                pendingRunId = runId
                pendingCheckpoint = LocalizationCalibrationCheckpoint.START
            }
            driver.y.onPress("Record surveyed route end") {
                pendingRunId = runId
                pendingCheckpoint = LocalizationCalibrationCheckpoint.END
                runId++
            }
        }

        everyLoop {
            robot.driveWithGamepad(driver, useHeadingLock = true)
            val checkpoint = pendingCheckpoint
            if (checkpoint != LocalizationCalibrationCheckpoint.NONE) {
                record(robot, checkpoint, pendingRunId, truthValid = true)
                pendingCheckpoint = LocalizationCalibrationCheckpoint.NONE
            }

            if (continuousRecording &&
                (testType == LocalizationCalibrationTestType.VISION_STATIONARY ||
                    testType == LocalizationCalibrationTestType.COMBINED_VALIDATION)) {
                var newestVisionTimestamp = Long.MIN_VALUE
                for (measurement in robot.base.visionTracker.visionInputs.measurements) {
                    if (measurement.timestampMs > newestVisionTimestamp) {
                        newestVisionTimestamp = measurement.timestampMs
                    }
                }
                if (newestVisionTimestamp > lastRecordedVisionTimestampMs) {
                    record(robot, LocalizationCalibrationCheckpoint.NONE, runId, truthValid = true)
                    lastRecordedVisionTimestampMs = newestVisionTimestamp
                }
            }

            robot.addTelemetry("Cal/Test", testType.name)
            robot.addTelemetry("Cal/Run", runId)
            robot.addTelemetry("Cal/Recording", continuousRecording)
            robot.addTelemetry("Cal/Truth", "%.2f, %.2f, %.1f deg".format(truthX, truthY, Math.toDegrees(truthHeading)))
            robot.addTelemetry("Cal/Dropped", recorder?.droppedSampleCount ?: 0L)
        }
    }

    private fun record(
        robot: AresRobot,
        checkpoint: LocalizationCalibrationCheckpoint,
        sampleRunId: Int,
        truthValid: Boolean
    ) {
        recorder?.record(
            LocalizationCalibrationSample.capture(
                timestampMs = RobotClock.currentTimeMillis(),
                platform = LocalizationCalibrationPlatform.FTC,
                testType = testType,
                runId = sampleRunId,
                state = robot.base.store.state,
                measurements = robot.base.visionTracker.visionInputs.measurements,
                checkpoint = checkpoint,
                truthValid = truthValid,
                truthX = truthX,
                truthY = truthY,
                truthHeading = truthHeading
            )
        )
    }
}
