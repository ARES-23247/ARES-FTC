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
 * the EKF pose; malformed/missing numeric fields intentionally default to zero.
 */
@TeleOp(name = "ARES Remote Drive (NT4)", group = "ARES")
class ARESRemoteDriveOpMode : AresTeleOpBase() {

    private var lastHeartbeatTime = 0L
    private var lastHeartbeatVal = 0L

    override fun define() = aresTeleOp {
        
        onInit { robot, _ ->
            // Remote commands already arrive as a time series; do not add joystick EMA slew limiting.
            robot.base.mecanumIO.slewRateLimit = null
            robot.addTelemetry("Status", "Remote NT4 client drive mode initialized.")
        }
        
        onLoop { robot, _, _ ->
            try {
                val nt4 = robot.base.telemetryManager.nt4
                val currentHeartbeat = nt4.getNumber("ARES/Input/heartbeat", 0.0).toLong()
                val now = RobotClock.currentTimeMillis()

                if (currentHeartbeat != lastHeartbeatVal) {
                    lastHeartbeatVal = currentHeartbeat
                    lastHeartbeatTime = now
                }

                // A frozen publisher value is treated as a disconnected controller.
                if (now - lastHeartbeatTime < 1000L) {
                    val vx = nt4.getNumber("ARES/Input/vx", 0.0)
                    val vy = nt4.getNumber("ARES/Input/vy", 0.0)
                    val omega = nt4.getNumber("ARES/Input/omega", 0.0)

                    robot.driveFieldCentric(vx, vy, omega)
                    
                    // Commands are single-consumer: clear the topic before executing one.
                    val cmdStr = nt4.getString("ARES/Input/command", "")
                    if (cmdStr.isNotEmpty()) {
                        nt4.putString("ARES/Input/command", "") // Clear command immediately
                        val spaceIdx = cmdStr.indexOf(' ')
                        val cmdName = if (spaceIdx > 0) cmdStr.substring(0, spaceIdx) else cmdStr
                        when (cmdName) {
                            "reset" -> {
                                var parseStr = if (spaceIdx > 0) cmdStr.substring(spaceIdx + 1) else ""
                                val idx1 = parseStr.indexOf(' ')
                                val x = if (idx1 > 0) parseStr.substring(0, idx1).toDoubleOrNull() ?: 0.0 else parseStr.toDoubleOrNull() ?: 0.0
                                parseStr = if (idx1 > 0) parseStr.substring(idx1 + 1) else ""
                                val idx2 = parseStr.indexOf(' ')
                                val y = if (idx2 > 0) parseStr.substring(0, idx2).toDoubleOrNull() ?: 0.0 else parseStr.toDoubleOrNull() ?: 0.0
                                parseStr = if (idx2 > 0) parseStr.substring(idx2 + 1) else ""
                                val h = parseStr.toDoubleOrNull() ?: 0.0
                                println("[RemoteDrive] Resetting EKF pose to: ($x, $y) at $h rad")
                                robot.base.resetPose(
                                    com.areslib.math.geometry.Pose2d(
                                        x, y,
                                        com.areslib.math.geometry.Rotation2d(h)
                                    )
                                )
                            }
                        }
                    }

                    robot.addTelemetry("Status", "DRIVING")
                    robot.addTelemetry("vx", vx)
                    robot.addTelemetry("vy", vy)
                    robot.addTelemetry("omega", omega)
                } else {
                    robot.base.mecanumDrive.fieldRelativeDrive(0.0, 0.0, 0.0, false)
                    robot.addTelemetry("Status", "DISCONNECTED / STALE HEARTBEAT")
                }
            } catch (e: Exception) {
                robot.base.mecanumDrive.fieldRelativeDrive(0.0, 0.0, 0.0, false)
                robot.addTelemetry("Status", "WATCHDOG ERROR: ${e.message}")
            }
        }
    }
}
