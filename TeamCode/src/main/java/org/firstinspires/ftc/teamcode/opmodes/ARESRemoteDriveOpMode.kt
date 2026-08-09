package org.firstinspires.ftc.teamcode.opmodes

import com.qualcomm.robotcore.eventloop.opmode.TeleOp
import org.firstinspires.ftc.teamcode.dsl.AresTeleOpBase
import com.areslib.action.RobotAction
/**
 * Documentation for ARESRemoteDriveOpMode
 */

@TeleOp(name = "ARES Remote Drive (NT4)", group = "ARES")
class ARESRemoteDriveOpMode : AresTeleOpBase() {

    private var lastHeartbeatTime = 0L
    private var lastHeartbeatVal = 0L

    override fun define() = aresTeleOp {
        
        onInit { robot, telemetry ->
            robot.base.mecanumIO.slewRateLimit = null // Disable slew limits for direct remote tracking
            robot.addTelemetry("Status", "Remote NT4 client drive mode initialized.")
        }
        
        onLoop { robot, driver, telemetry ->
            try {
                /**
                 * Documentation for nt4
                 */
                val nt4 = robot.base.telemetryManager.nt4
                /**
                 * Documentation for currentHeartbeat
                 */
                val currentHeartbeat = nt4.getNumber("ARES/Input/heartbeat", 0.0).toLong()
                /**
                 * Documentation for now
                 */
                val now = com.areslib.util.RobotClock.currentTimeMillis()

                if (currentHeartbeat != lastHeartbeatVal) {
                    lastHeartbeatVal = currentHeartbeat
                    lastHeartbeatTime = now
                }

                // Command watchdog: If heartbeat hasn't changed in 1000ms, stop robot
                if (now - lastHeartbeatTime < 1000L) {
                    /**
                     * Documentation for vx
                     */
                    val vx = nt4.getNumber("ARES/Input/vx", 0.0)
                    /**
                     * Documentation for vy
                     */
                    val vy = nt4.getNumber("ARES/Input/vy", 0.0)
                    /**
                     * Documentation for omega
                     */
                    val omega = nt4.getNumber("ARES/Input/omega", 0.0)

                    robot.driveFieldCentric(vx, vy, omega)
                    
                    // Parse commands
                    /**
                     * Documentation for cmdStr
                     */
                    val cmdStr = nt4.getString("ARES/Input/command", "")
                    if (cmdStr.isNotEmpty()) {
                        nt4.putString("ARES/Input/command", "") // Clear command immediately
                        val spaceIdx = cmdStr.indexOf(' ')
                        val cmdName = if (spaceIdx > 0) cmdStr.substring(0, spaceIdx) else cmdStr
                        when (cmdName) {
                            "reset" -> {
                                var parseStr = if (spaceIdx > 0) cmdStr.substring(spaceIdx + 1) else ""
                                var idx1 = parseStr.indexOf(' ')
                                val x = if (idx1 > 0) parseStr.substring(0, idx1).toDoubleOrNull() ?: 0.0 else parseStr.toDoubleOrNull() ?: 0.0
                                parseStr = if (idx1 > 0) parseStr.substring(idx1 + 1) else ""
                                var idx2 = parseStr.indexOf(' ')
                                val y = if (idx2 > 0) parseStr.substring(0, idx2).toDoubleOrNull() ?: 0.0 else parseStr.toDoubleOrNull() ?: 0.0
                                parseStr = if (idx2 > 0) parseStr.substring(idx2 + 1) else ""
                                val h = parseStr.toDoubleOrNull() ?: 0.0
                                println("[RemoteDrive] Resetting EKF pose to: ($x, $y) at $h rad")
                                robot.base.store.dispatch(RobotAction.PoseUpdate(
                                    xMeters = x,
                                    yMeters = y,
                                    headingRadians = h,
                                    timestampMs = now
                                ))
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
