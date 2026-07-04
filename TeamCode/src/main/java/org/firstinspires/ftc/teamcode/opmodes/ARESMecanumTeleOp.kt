package org.firstinspires.ftc.teamcode.opmodes

import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode
import com.qualcomm.robotcore.eventloop.opmode.TeleOp
import com.areslib.ftc.FtcMecanumRobot
import com.areslib.action.RobotAction
import com.areslib.ftc.toState
import com.areslib.state.Alliance
import org.firstinspires.ftc.teamcode.telemetry.ZulipLogUploader
import org.firstinspires.ftc.teamcode.telemetry.LimelightProxy

/**
 * A highly optimized, modern FTC TeleOp demonstrating the new student facade layout.
 * Optimized specifically for a 4-motor mecanum drivetrain, with a GoBilda Pinpoint 
 * connected to native I2C 1, and a GoBilda Floodgate connected to native Analog Port 1.
 */
@TeleOp(name = "Direct Mecanum Drivetrain", group = "ARES")
class ARESMecanumTeleOp : LinearOpMode() {

    // Configure active alliance (BLUE by default, change to RED for red alliance matches)
    private val alliance = Alliance.RED

    // Configure active field layout (SQUARE_STANDARD or DIAMOND)
    private val fieldLayout = com.areslib.math.FieldLayout.SQUARE_STANDARD

    override fun runOpMode() {
        // Configure the EKF with the tag positions of the selected field layout
        com.areslib.math.PoseEstimator.activeTags = com.areslib.math.FieldLayouts.getTagsForLayout(fieldLayout)

        telemetry.addData("Status", "Initializing...")
        telemetry.update()

        // 1. Initialize the standard direct mecanum robot facade
        val robot = FtcMecanumRobot(
            hardwareMap = hardwareMap,
            flName = "fl",
            frName = "fr",
            blName = "rl",
            brName = "rr",
            pinpointName = "pinpoint",
            limelightName = "limelight",
            localTelemetry = telemetry,
            flDirection = com.qualcomm.robotcore.hardware.DcMotorSimple.Direction.FORWARD,
            blDirection = com.qualcomm.robotcore.hardware.DcMotorSimple.Direction.FORWARD,
            frDirection = com.qualcomm.robotcore.hardware.DcMotorSimple.Direction.REVERSE,
            brDirection = com.qualcomm.robotcore.hardware.DcMotorSimple.Direction.REVERSE
        )

        // Calibrate static friction feedforward (kS) to overcome physical drivetrain deadband
        robot.mecanumIO.kS = 0.05

        // Initialize Zulip telemetry uploader dynamically from the Control Hub's properties file
        val uploader = ZulipLogUploader.createAutoConfigured()

        // Ensure Limelight Proxy is started for wireless tuning over Wi-Fi
        org.firstinspires.ftc.teamcode.telemetry.LimelightProxyAutoStart.start()

        // Dispatch alliance choice to core Redux store
        robot.store.dispatch(RobotAction.SetAlliance(alliance))

        // Set initial pose/heading offset at center (0,0)
        val initialX = 0.0
        val initialHeading = 0.0
        robot.pinpointIO?.initialize(
            com.areslib.math.Pose2d(
                x = initialX,
                y = 0.0,
                heading = com.areslib.math.Rotation2d(initialHeading)
            ),
            resetHardware = true
        )

        // Define declarative bindings for documentation (ARES-Analytics parsing)
        val driver = com.areslib.telemetry.AresGamepad()
        driver.leftStick.label("Field-centric Translation (X/Y)")
        driver.rightStickX.label("Robot Rotation")
        driver.b.label("Auto-Align to Tag 1")
        driver.y.label("Reset Field Centric Pose")

        try {
            // Pre-Match Auto-Alignment Init Loop:
            // Runs continuously while waiting for the match to start. If the Limelight sees AprilTags,
            // the EKF will read the position and automatically snap the robot's starting coordinate
            // to the true physical field coordinate. Telemetry displays the live EKF pose.
            while (opModeInInit()) {
                robot.update(null, null)
                telemetry.addData("Status", "Ready! Aligning via Limelight...")
                telemetry.addData("Alliance", alliance)
                telemetry.addData("EKF Pose (X, Y, Deg)", String.format("(%.2f, %.2f) %.1f°",
                    robot.store.state.drive.poseEstimator.estimatedPose.x,
                    robot.store.state.drive.poseEstimator.estimatedPose.y,
                    Math.toDegrees(robot.store.state.drive.poseEstimator.estimatedPose.heading.radians)
                ))
                telemetry.addData("Vision Status", robot.visionTracker.lastVisionStatus)
                telemetry.update()
                sleep(20)
            }
            if (isStopRequested) return

            // Mark initialization complete to enable active-play vision outlier filtering
            robot.visionTracker.isInInit = false

            // Stop the proxy during active play to free up threads, socket buffers, and CPU cycles
            org.firstinspires.ftc.teamcode.telemetry.LimelightProxyAutoStart.stop()

            // State variables for vision noise filtering (persists across loop frames)
            var hasPrevFiltered = false
            var prevRawYaw = 0.0
            var prevErrX = 0.0
            var prevErrY = 0.0
            var prevErrHeading = 0.0
            var prevErrHeadingForD = 0.0
            var prevLoopTimeMs = System.currentTimeMillis()

            // Tag search state: when tag is lost, rotate toward last known direction
            var lastKnownSearchDirection = 0.0 // +1.0 = rotate CCW, -1.0 = rotate CW
            var tagLostTimestampMs = 0L
            var wasTrackingTag = false

            while (opModeIsActive()) {
                // 1. Check if AprilTag ID 1 is currently visible (independent of button B)
                val now = com.areslib.util.RobotClock.currentTimeMillis()
                val tag1MeasurementTele = robot.store.state.vision.measurements.firstOrNull {
                    it.tagId == 1 && (now - it.timestampMs) < 1000L
                }

                // Always log Tag 1 tracking status to telemetry so driver can diagnose visibility and values
                if (tag1MeasurementTele != null) {
                    val targetSpace = tag1MeasurementTele.robotPoseTargetSpace
                    val ageMs = now - tag1MeasurementTele.timestampMs
                    telemetry.addData("Tag 1 Tracking", String.format("VISIBLE (Age: %dms)", ageMs))
                    telemetry.addData("Tag 1 Raw Z (Dist)", String.format("%.3fm (abs: %.3fm, %.2f ft)", 
                        targetSpace.z, kotlin.math.abs(targetSpace.z), kotlin.math.abs(targetSpace.z) * 3.28084))
                    telemetry.addData("Tag 1 Raw X (Lat)", String.format("%.3fm", targetSpace.x))
                    telemetry.addData("Tag 1 Raw Yaw", String.format("%.1f°", Math.toDegrees(targetSpace.rotation.y)))
                } else {
                    telemetry.addData("Tag 1 Tracking", "NOT VISIBLE")
                }

                if (gamepad1.b) {
                    // Require reasonably fresh data (<= 250ms) for active closed-loop control
                    val activeMeasurement = robot.store.state.vision.measurements.firstOrNull {
                        it.tagId == 1 && (now - it.timestampMs) < 250L
                    }

                    if (activeMeasurement != null) {
                        // Tag reacquired — reset search state
                        tagLostTimestampMs = 0L
                        
                        val robotPoseTargetSpace = activeMeasurement.robotPoseTargetSpace
                        
                        // target-space coordinates (Limelight: Z forward, X right)
                        // We define errors in target-relative alignment space:
                        // errorForwardT: positive if too close (needs to drive backward)
                        // errorLeftT: positive if robot is to the right of tag (needs to drive left)
                        val distanceZ = kotlin.math.abs(robotPoseTargetSpace.z)
                        val targetDistanceMeters = 2.4384 // 8 feet
                        val errorForwardT = distanceZ - targetDistanceMeters
                        val errorLeftT = robotPoseTargetSpace.x
                        
                        // In target-space: Z+ is outward from tag, Y+ is up.
                        // Yaw (robot turning left/right) = rotation around Y axis = rotation.y
                        // Negated to match the controller's sign convention (positive = CCW)
                        val robotYaw = -robotPoseTargetSpace.rotation.y
                        val wrappedYaw = com.areslib.math.InputMath.wrapAngle(robotYaw)
                        
                        // 1. Yaw rate-of-change sanity check (reject PnP flips/jumps > 15 degrees per frame)
                        val maxHeadingChange = Math.toRadians(15.0)
                        val sanitizedYaw = if (hasPrevFiltered) {
                            val diff = com.areslib.math.InputMath.wrapAngle(wrappedYaw - prevRawYaw)
                            if (kotlin.math.abs(diff) > maxHeadingChange) {
                                prevRawYaw
                            } else {
                                wrappedYaw
                            }
                        } else {
                            wrappedYaw
                        }
                        prevRawYaw = sanitizedYaw
                        
                        val phi = sanitizedYaw
                        // Rotate translation errors into robot-centric frame using the correct -phi rotation matrix
                        val errX = errorForwardT * kotlin.math.cos(phi) + errorLeftT * kotlin.math.sin(phi)
                        val errY = -errorForwardT * kotlin.math.sin(phi) + errorLeftT * kotlin.math.cos(phi)
                        
                        // Heading goal: rotate to keep the tag centered in the camera FOV,
                        // while also correcting yaw misalignment. The pointing target adjusts
                        // heading based on lateral offset so the robot tracks a moving tag.
                        val pointingTarget = kotlin.math.atan2(errorLeftT, distanceZ)
                        val errHeading = com.areslib.math.InputMath.wrapAngle(pointingTarget - phi)
                        
                        // 2. Low-pass filters to smooth out high-frequency vision noise
                        // Lower alpha = heavier smoothing (0.0 = frozen, 1.0 = no filtering)
                        val alphaTranslation = 0.4
                        val alphaHeading = 0.5
                        
                        val errXFiltered = if (hasPrevFiltered) {
                            alphaTranslation * errX + (1.0 - alphaTranslation) * prevErrX
                        } else {
                            errX
                        }
                        val errYFiltered = if (hasPrevFiltered) {
                            alphaTranslation * errY + (1.0 - alphaTranslation) * prevErrY
                        } else {
                            errY
                        }
                        val errHeadingFiltered = if (hasPrevFiltered) {
                            val diff = com.areslib.math.InputMath.wrapAngle(errHeading - prevErrHeading)
                            com.areslib.math.InputMath.wrapAngle(prevErrHeading + alphaHeading * diff)
                        } else {
                            errHeading
                        }
                        
                        prevErrX = errXFiltered
                        prevErrY = errYFiltered
                        prevErrHeading = errHeadingFiltered
                        hasPrevFiltered = true
                        
                        val kP_translation = 1.0
                        val kP_rotation = 1.1
                        val kD_rotation = 0.35 // Derivative damping to resist rapid heading swings
                        
                        // 3. Apply deadbands to prevent limit-cycle oscillations (jittering)
                        val translationDeadband = 0.04 // 4 cm
                        val headingErrorDeadband = Math.toRadians(1.0) // 1 degree
                        
                        // Speed-limit translation commands to keep the tag in the camera's FOV
                        val ctrlX = if (kotlin.math.abs(errXFiltered) > translationDeadband) {
                            (errXFiltered * kP_translation).coerceIn(-0.5, 0.5)
                        } else {
                            0.0
                        }
                        
                        val ctrlY = if (kotlin.math.abs(errYFiltered) > translationDeadband) {
                            (errYFiltered * kP_translation).coerceIn(-0.3, 0.3)
                        } else {
                            0.0
                        }
                        
                        val kS_rotational = 0.06 // Feedforward to overcome rotation static scrubbing friction
                        
                        // Compute derivative term: rate of heading error change
                        val nowMs = System.currentTimeMillis()
                        val dtSec = ((nowMs - prevLoopTimeMs).coerceIn(1, 200)) / 1000.0
                        prevLoopTimeMs = nowMs
                        val headingErrorRate = if (hasPrevFiltered) {
                            com.areslib.math.InputMath.wrapAngle(errHeadingFiltered - prevErrHeadingForD) / dtSec
                        } else {
                            0.0
                        }
                        prevErrHeadingForD = errHeadingFiltered
                        
                        val ctrlOmega = if (kotlin.math.abs(errHeadingFiltered) > headingErrorDeadband) {
                            val sign = kotlin.math.sign(errHeadingFiltered)
                            val pTerm = errHeadingFiltered * kP_rotation
                            val dTerm = headingErrorRate * kD_rotation
                            (pTerm + dTerm + sign * kS_rotational).coerceIn(-0.65, 0.65)
                        } else {
                            0.0
                        }
                        
                        robot.store.dispatch(
                            RobotAction.JoystickDriveIntent(
                                targetXVelocity = ctrlX,
                                targetYVelocity = ctrlY,
                                targetAngularVelocity = ctrlOmega,
                                isFieldCentric = false
                            )
                        )
                        telemetry.addData("Alignment Mode", "ACTIVE (Tag 1 Seen & B Held!)")
                        telemetry.addData("Calculated Error (Filtered)", String.format("ErrX: %.3fm, ErrY: %.3fm, ErrH: %.1f°",
                            errXFiltered, errYFiltered, Math.toDegrees(errHeadingFiltered)
                        ))
                        telemetry.addData("Command Outputs", String.format("CtrlX: %.3f, CtrlY: %.3f, CtrlOmega: %.3f",
                            ctrlX, ctrlY, ctrlOmega
                        ))

                        // Update search direction: use the ACTUAL rotation command as the
                        // most reliable predictor. If the robot was rotating CW to track
                        // the tag, the tag is moving CW, so search CW when lost.
                        if (kotlin.math.abs(ctrlOmega) > 0.02) {
                            lastKnownSearchDirection = kotlin.math.sign(ctrlOmega)
                        } else if (kotlin.math.abs(ctrlX) > 0.02) {
                            // If strafing but not rotating, tag is off to one side
                            lastKnownSearchDirection = -kotlin.math.sign(ctrlX)
                        }
                        wasTrackingTag = true
                    } else {
                        hasPrevFiltered = false
                        
                        // Tag is not visible while B is held — initiate search rotation
                        if (tagLostTimestampMs == 0L) {
                            // First frame of tag loss (or first frame with B and no tag ever):
                            tagLostTimestampMs = System.currentTimeMillis()
                            // If we never tracked, pick CW as the initial search direction
                            if (!wasTrackingTag) {
                                lastKnownSearchDirection = -1.0 // start CW
                            }
                        }
                        
                        val firstSweepMs = 1200L  // sweep predicted direction for 1.2s
                        val secondSweepMs = 2400L // reverse for 2.4s (1.2s back + 1.2s new ground)
                        val totalSearchMs = firstSweepMs + secondSweepMs
                        val timeSinceLost = System.currentTimeMillis() - tagLostTimestampMs
                        val searchSpeed = 0.85 // fast aggressive rotation for brief search
                        
                        if (timeSinceLost < totalSearchMs) {
                            // Active search: sweep predicted direction, then reverse for longer
                            val currentDirection = if (timeSinceLost < firstSweepMs) {
                                lastKnownSearchDirection
                            } else {
                                // Reverse for longer to cover the other side
                                -lastKnownSearchDirection
                            }
                            val searchOmega = currentDirection * searchSpeed
                            robot.store.dispatch(
                                RobotAction.JoystickDriveIntent(
                                    targetXVelocity = 0.0,
                                    targetYVelocity = 0.0,
                                    targetAngularVelocity = searchOmega,
                                    isFieldCentric = false
                                )
                            )
                            val sweepNum = if (timeSinceLost < firstSweepMs) 1 else 2
                            val sweepRemaining = if (timeSinceLost < firstSweepMs) {
                                (firstSweepMs - timeSinceLost) / 1000.0
                            } else {
                                (totalSearchMs - timeSinceLost) / 1000.0
                            }
                            val dirLabel = if (currentDirection > 0) "← CCW" else "→ CW"
                            telemetry.addData("Alignment Mode", 
                                String.format("SEARCHING %s [Sweep %d/2] (%.1fs)",
                                    dirLabel, sweepNum, sweepRemaining))
                        } else {
                            // Both sweeps exhausted — stop
                            robot.store.dispatch(
                                RobotAction.JoystickDriveIntent(
                                    targetXVelocity = 0.0,
                                    targetYVelocity = 0.0,
                                    targetAngularVelocity = 0.0,
                                    isFieldCentric = false
                                )
                            )
                            telemetry.addData("Alignment Mode", "SEARCH TIMEOUT (Release B to reset)")
                        }
                    }
                } else {
                    // B released — reset search state for next time
                    wasTrackingTag = false
                    tagLostTimestampMs = 0L
                    hasPrevFiltered = false
                    
                    // Dispatch gamepad stick inputs as pure Redux intents
                    // (Note: joystick y is inverted by default in standard gamepads)
                    val joystickForward = -gamepad1.left_stick_y.toDouble()
                    val joystickLeft = -gamepad1.left_stick_x.toDouble()

                    // Align driver controls with the field coordinate system:
                    // EKF X is the alliance-to-alliance axis (+X towards Blue, -X towards Red).
                    // EKF Y is the audience-to-back-wall axis (+Y towards Back Wall, -Y towards Audience Wall).
                    val (driveX, driveY) = when (fieldLayout) {
                        com.areslib.math.FieldLayout.DIAMOND -> {
                            if (alliance == Alliance.RED) {
                                Pair(-joystickLeft, joystickForward) // Red on -Y wall faces +Y
                            } else {
                                Pair(-joystickForward, -joystickLeft) // Blue on +X wall faces -X
                            }
                        }

                        com.areslib.math.FieldLayout.SQUARE_STANDARD -> {
                            if (alliance == Alliance.RED) {
                                Pair(-joystickLeft, joystickForward) // Square: Red is on -Y wall, facing +Y.
                            } else {
                                Pair(joystickLeft, -joystickForward) // Square: Blue is on +Y wall, facing -Y.
                            }
                        }
                    }

                    robot.store.dispatch(
                        RobotAction.JoystickDriveIntent(
                            targetXVelocity = driveX,
                            targetYVelocity = driveY,
                            targetAngularVelocity = -gamepad1.right_stick_x.toDouble(),
                            isFieldCentric = true
                        )
                    )
                    telemetry.addData("Alignment Mode", "INACTIVE (Manual Drive)")
                }

                // 3. One coordinated call: polls hardware, computes kinematics, executes stall checks, and logs data
                val g1State = gamepad1.toState()
                driver.update(g1State)
                robot.update(g1State, gamepad2.toState())

                // 4. Optional: Reset field-centric heading/pose if Triangle (Y) button is pressed.
                // WARNING: ONLY press this when the robot is physically lined up flat against the starting wall!
                // Do NOT press in the middle of the field, as it forces starting wall coordinates.
                if (gamepad1.y) {
                    robot.pinpointIO?.initialize(
                        com.areslib.math.Pose2d(
                            x = initialX,
                            y = 0.0,
                            heading = com.areslib.math.Rotation2d(initialHeading)
                        )
                    )
                }
            }
        } finally {
            robot.close()
            // Automatically restart the proxy for wireless tuning post-match
            org.firstinspires.ftc.teamcode.telemetry.LimelightProxyAutoStart.start()
            // Upload telemetry logs asynchronously to Zulip in the background
            uploader.checkAndUpload()
        }
    }
}
