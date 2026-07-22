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
                    /**
                     * Documentation for parts
                     */
                    val parts = cmdStr.split(' ').filter { it.isNotBlank() }
                    /**
                     * Documentation for cmdName
                     */
                    val cmdName = parts.firstOrNull()
                    when (cmdName) {
                        "reset" -> {
                            /**
                             * Documentation for x
                             */
                            val x = parts.getOrNull(1)?.toDoubleOrNull() ?: 0.0
                            /**
                             * Documentation for y
                             */
                            val y = parts.getOrNull(2)?.toDoubleOrNull() ?: 0.0
                            /**
                             * Documentation for h
                             */
                            val h = parts.getOrNull(3)?.toDoubleOrNull() ?: 0.0
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
        }
    }
}
