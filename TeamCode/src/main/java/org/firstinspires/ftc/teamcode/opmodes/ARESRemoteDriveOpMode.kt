package org.firstinspires.ftc.teamcode.opmodes

import com.qualcomm.robotcore.eventloop.opmode.TeleOp
import com.areslib.util.RobotClock
import org.firstinspires.ftc.teamcode.dsl.AresTeleOpBase

/**
 * Accepts field-relative drive commands from the local ARES NT4 client.
 *
 * `ARES/Input/heartbeat` must change at least once per second. A stale heartbeat, parse error,
 * or networking exception commands zero velocity with heading lock disabled. Topic names are
 * canonical and omit a leading slash. The optional `reset x y headingRadians` command resets
 * the EKF pose. Non-finite motion values fail closed, and malformed reset commands are rejected
 * atomically rather than partially defaulting a pose to the field origin.
 */
@TeleOp(name = "ARES Remote Drive (NT4)", group = "ARES")
class ARESRemoteDriveOpMode : AresTeleOpBase() {

    private var lastHeartbeatTime = 0L
    private var lastHeartbeatVal = 0L
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
                val currentHeartbeat = nt4.getNumber("ARES/Input/heartbeat", Double.NaN)
                    .takeIf { it.isFinite() }
                    ?.toLong()
                val now = RobotClock.currentTimeMillis()

                if (currentHeartbeat != null && currentHeartbeat != lastHeartbeatVal) {
                    lastHeartbeatVal = currentHeartbeat
                    lastHeartbeatTime = now
                }

                // A frozen publisher value is treated as a disconnected controller.
                if (now - lastHeartbeatTime < 1000L) {
                    val vx = finiteClamped(nt4.getNumber("ARES/Input/vx", 0.0), robot.base.drive.maxSpeedMps)
                    val vy = finiteClamped(nt4.getNumber("ARES/Input/vy", 0.0), robot.base.drive.maxSpeedMps)
                    val omega = finiteClamped(
                        nt4.getNumber("ARES/Input/omega", 0.0),
                        robot.base.drive.maxAngularSpeedRadiansPerSecond
                    )

                    robot.base.drive.joystickDrive(vx, vy, omega, isFieldCentric = true)
                    
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
                    robot.addTelemetry("vx", vx)
                    robot.addTelemetry("vy", vy)
                    robot.addTelemetry("omega", omega)
                } else {
                    robot.base.drive.joystickDrive(0.0, 0.0, 0.0, isFieldCentric = true)
                    robot.addTelemetry("Status", "DISCONNECTED / STALE HEARTBEAT")
                }
            } catch (e: Exception) {
                robot.base.drive.joystickDrive(0.0, 0.0, 0.0, isFieldCentric = true)
                robot.addTelemetry("Status", "WATCHDOG ERROR: ${e.message}")
            }
        }
    }

    private fun finiteClamped(value: Double, magnitudeLimit: Double): Double {
        val safeLimit = magnitudeLimit.takeIf { it.isFinite() && it > 0.0 } ?: return 0.0
        return value.takeIf { it.isFinite() }?.coerceIn(-safeLimit, safeLimit) ?: 0.0
    }

    private fun parseResetPose(command: String): com.areslib.math.geometry.Pose2d? {
        val parts = command.trim().split(commandWhitespace)
        if (parts.size != 4 || parts[0] != "reset") return null
        val x = parts[1].toDoubleOrNull()?.takeIf { it.isFinite() } ?: return null
        val y = parts[2].toDoubleOrNull()?.takeIf { it.isFinite() } ?: return null
        val heading = parts[3].toDoubleOrNull()?.takeIf { it.isFinite() } ?: return null
        return com.areslib.math.geometry.Pose2d(x, y, com.areslib.math.geometry.Rotation2d(heading))
    }
}
