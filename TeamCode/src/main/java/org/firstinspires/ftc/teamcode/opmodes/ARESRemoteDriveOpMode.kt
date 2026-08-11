package org.firstinspires.ftc.teamcode.opmodes

import com.qualcomm.robotcore.eventloop.opmode.TeleOp
import com.areslib.networktables.NT4Server
import com.areslib.util.RobotClock
import org.firstinspires.ftc.teamcode.dsl.AresTeleOpBase

/**
 * Accepts field-relative drive commands from the local ARES NT4 client.
 *
 * Motion is accepted only from the atomic `ARES/Input/driveFrame` double-array topic. Each frame
 * carries a publisher session, sequence, timestamp, and all three axes. A new session or expired
 * lease must first publish a complete neutral frame; only a later sequence may command motion.
 * Legacy scalar heartbeat/axis topics remain fail-closed and cannot arm this OpMode. The optional
 * `reset x y headingRadians` command resets the EKF pose after the atomic handshake. Non-finite or
 * malformed input, stale/replayed sequences, and networking exceptions command zero velocity.
 */
@TeleOp(name = "ARES Remote Drive (NT4)", group = "ARES")
class ARESRemoteDriveOpMode : AresTeleOpBase() {
    private val driveFrameGate = RemoteDriveFrameGate()
    private val commandWhitespace = Regex("\\s+")

    override fun define() = teleOp {

        setup {
            // Remote commands already arrive as a time series; do not add joystick EMA slew limiting.
            robot.base.mecanumIO.slewRateLimit = null
            robot.addTelemetry("Status", "Remote NT4 client drive mode initialized.")
        }

        everyLoop {
            try {
                val nt4 = robot.base.telemetryManager.nt4
                val now = RobotClock.currentTimeMillis()
                val encodedFrame = try {
                    NT4Server.getDoubleArray(DRIVE_FRAME_TOPIC, EMPTY_DRIVE_FRAME)
                } catch (_: Exception) {
                    null
                }
                val frameFresh = driveFrameGate.observe(
                    encodedFrame = encodedFrame,
                    timestampMs = now,
                    maxTranslationMps = robot.base.drive.maxSpeedMps,
                    maxOmegaRadiansPerSecond = robot.base.drive.maxAngularSpeedRadiansPerSecond
                )

                if (frameFresh && driveFrameGate.motionAuthorized) {
                    // Commands are single-consumer: clear the topic before executing one.
                    val cmdStr = nt4.getString("ARES/Input/command", "")
                    if (cmdStr.isNotEmpty()) {
                        nt4.putString("ARES/Input/command", "") // Clear command immediately
                        val spaceIdx = cmdStr.indexOf(' ')
                        val cmdName = if (spaceIdx > 0) cmdStr.substring(0, spaceIdx) else cmdStr
                        when (cmdName) {
                            "reset" -> {
                                val pose = parseResetPose(cmdStr)
                                if (pose == null) {
                                    robot.addTelemetry("Command Error", "Expected: reset <x> <y> <headingRadians>")
                                } else {
                                    println("[RemoteDrive] Resetting EKF pose to: (${pose.x}, ${pose.y}) at ${pose.heading.radians} rad")
                                    robot.base.resetPose(pose)
                                }
                            }
                        }
                    }

                    robot.addTelemetry("Status", "DRIVING")
                    robot.addTelemetry("vx", driveFrameGate.vx)
                    robot.addTelemetry("vy", driveFrameGate.vy)
                    robot.addTelemetry("omega", driveFrameGate.omega)

                    // Apply motion last, after every network read, command operation, and telemetry
                    // write in this loop has completed. Any earlier exception reaches hard zero.
                    robot.base.drive.joystickDrive(
                        driveFrameGate.vx,
                        driveFrameGate.vy,
                        driveFrameGate.omega,
                        isFieldCentric = true
                    )
                } else if (frameFresh) {
                    // A retained command string must not survive into the first post-handshake
                    // motion frame. Clear it while holding drivetrain output at neutral.
                    nt4.putString("ARES/Input/command", "")
                    robot.base.drive.joystickDrive(0.0, 0.0, 0.0, isFieldCentric = true)
                    robot.addTelemetry("Status", "ATOMIC NEUTRAL HANDSHAKE ACCEPTED")
                } else {
                    robot.base.drive.joystickDrive(0.0, 0.0, 0.0, isFieldCentric = true)
                    robot.addTelemetry("Status", "DISCONNECTED / WAITING FOR NEUTRAL DRIVE FRAME")
                }
            } catch (e: Exception) {
                robot.base.drive.joystickDrive(0.0, 0.0, 0.0, isFieldCentric = true)
                robot.addTelemetry("Status", "WATCHDOG ERROR: ${e.message}")
            }
        }
    }

    private fun parseResetPose(command: String): com.areslib.math.geometry.Pose2d? {
        val parts = command.trim().split(commandWhitespace)
        if (parts.size != 4 || parts[0] != "reset") return null
        val x = parts[1].toDoubleOrNull()?.takeIf { it.isFinite() } ?: return null
        val y = parts[2].toDoubleOrNull()?.takeIf { it.isFinite() } ?: return null
        val heading = parts[3].toDoubleOrNull()?.takeIf { it.isFinite() } ?: return null
        return com.areslib.math.geometry.Pose2d(x, y, com.areslib.math.geometry.Rotation2d(heading))
    }

    private companion object {
        const val DRIVE_FRAME_TOPIC = "ARES/Input/driveFrame"
        val EMPTY_DRIVE_FRAME = DoubleArray(0)
    }
}

/**
 * Fail-closed state machine for atomic remote-drive frames.
 *
 * Payload: `[version, sessionNonce, sequence, clientTimestampMs, vx, vy, omega]`. Version is `1`;
 * session, sequence, and timestamp are exact non-negative integers within JavaScript's safe integer
 * range. Session changes, malformed frames, read failures, clock rollback, and lease expiry disarm
 * motion. The first accepted sequence in each arming cycle must be neutral and never moves the
 * robot; a later sequence from that same session authorizes complete-frame motion.
 */
internal class RemoteDriveFrameGate(
    private val timeoutMs: Long = 1_000L
) {
    private var hasSession = false
    private var sessionNonce = 0L
    private var lastSequence = -1L
    private var lastAcceptedTimeMs = 0L
    private var neutralHandshakeComplete = false

    var vx: Double = 0.0
        private set
    var vy: Double = 0.0
        private set
    var omega: Double = 0.0
        private set
    var motionAuthorized: Boolean = false
        private set

    init {
        require(timeoutMs > 0L) { "timeoutMs must be positive" }
    }

    fun observe(
        encodedFrame: DoubleArray?,
        timestampMs: Long,
        maxTranslationMps: Double,
        maxOmegaRadiansPerSecond: Double
    ): Boolean {
        if (!isFresh(timestampMs)) disarmForHandshake(clearSession = false)
        if (encodedFrame == null || encodedFrame.size != FRAME_SIZE ||
            !maxTranslationMps.isFinite() || maxTranslationMps <= 0.0 ||
            !maxOmegaRadiansPerSecond.isFinite() || maxOmegaRadiansPerSecond <= 0.0
        ) {
            disarmForHandshake(clearSession = true)
            return false
        }

        val version = encodedFrame[VERSION_INDEX]
        val parsedSession = exactNonNegativeLong(encodedFrame[SESSION_INDEX])
        val parsedSequence = exactNonNegativeLong(encodedFrame[SEQUENCE_INDEX])
        val parsedClientTimestamp = exactNonNegativeLong(encodedFrame[CLIENT_TIMESTAMP_INDEX])
        val candidateVx = encodedFrame[VX_INDEX]
        val candidateVy = encodedFrame[VY_INDEX]
        val candidateOmega = encodedFrame[OMEGA_INDEX]
        if (version != PROTOCOL_VERSION || parsedSession == null || parsedSequence == null ||
            parsedClientTimestamp == null || !candidateVx.isFinite() || !candidateVy.isFinite() ||
            !candidateOmega.isFinite() || kotlin.math.abs(candidateVx) > maxTranslationMps ||
            kotlin.math.abs(candidateVy) > maxTranslationMps ||
            kotlin.math.abs(candidateOmega) > maxOmegaRadiansPerSecond
        ) {
            disarmForHandshake(clearSession = false)
            return false
        }

        if (!hasSession || parsedSession != sessionNonce) {
            disarmForHandshake(clearSession = false)
            hasSession = true
            sessionNonce = parsedSession
            lastSequence = parsedSequence
            return acceptNeutralHandshake(candidateVx, candidateVy, candidateOmega, timestampMs)
        }

        if (parsedSequence < lastSequence) {
            disarmForHandshake(clearSession = false)
            return false
        }
        // Polling an unchanged atomic topic may return the same committed sequence many times.
        // It may hold the last coherent command only within the receiver-side freshness lease.
        if (parsedSequence == lastSequence) return isFresh(timestampMs)
        lastSequence = parsedSequence

        if (!neutralHandshakeComplete) {
            return acceptNeutralHandshake(candidateVx, candidateVy, candidateOmega, timestampMs)
        }

        vx = candidateVx
        vy = candidateVy
        omega = candidateOmega
        motionAuthorized = true
        lastAcceptedTimeMs = timestampMs
        return true
    }

    private fun acceptNeutralHandshake(
        candidateVx: Double,
        candidateVy: Double,
        candidateOmega: Double,
        timestampMs: Long
    ): Boolean {
        if (kotlin.math.abs(candidateVx) > NEUTRAL_EPSILON ||
            kotlin.math.abs(candidateVy) > NEUTRAL_EPSILON ||
            kotlin.math.abs(candidateOmega) > NEUTRAL_EPSILON
        ) {
            return false
        }
        neutralHandshakeComplete = true
        motionAuthorized = false
        vx = 0.0
        vy = 0.0
        omega = 0.0
        lastAcceptedTimeMs = timestampMs
        return true
    }

    private fun isFresh(timestampMs: Long): Boolean {
        if (!neutralHandshakeComplete) return false
        val elapsedMs = timestampMs - lastAcceptedTimeMs
        return elapsedMs >= 0L && elapsedMs < timeoutMs
    }

    private fun disarmForHandshake(clearSession: Boolean) {
        neutralHandshakeComplete = false
        motionAuthorized = false
        vx = 0.0
        vy = 0.0
        omega = 0.0
        lastAcceptedTimeMs = 0L
        if (clearSession) {
            hasSession = false
            sessionNonce = 0L
            lastSequence = -1L
        }
    }

    private fun exactNonNegativeLong(value: Double): Long? {
        if (!value.isFinite() || value < 0.0 || value > MAX_SAFE_INTEGER_AS_DOUBLE) return null
        val parsed = value.toLong()
        return parsed.takeIf { it.toDouble() == value }
    }

    private companion object {
        const val FRAME_SIZE = 7
        const val VERSION_INDEX = 0
        const val SESSION_INDEX = 1
        const val SEQUENCE_INDEX = 2
        const val CLIENT_TIMESTAMP_INDEX = 3
        const val VX_INDEX = 4
        const val VY_INDEX = 5
        const val OMEGA_INDEX = 6
        const val PROTOCOL_VERSION = 1.0
        const val MAX_SAFE_INTEGER_AS_DOUBLE = 9_007_199_254_740_991.0
        const val NEUTRAL_EPSILON = 1e-6
    }
}
