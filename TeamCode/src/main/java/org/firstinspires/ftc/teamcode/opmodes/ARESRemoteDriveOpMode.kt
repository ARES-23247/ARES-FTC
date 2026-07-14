package org.firstinspires.ftc.teamcode.opmodes

import com.qualcomm.robotcore.eventloop.opmode.TeleOp
import org.firstinspires.ftc.teamcode.dsl.AresTeleOpBase
import com.areslib.action.RobotAction

@TeleOp(name = "ARES Remote Drive (NT4)", group = "ARES")
class ARESRemoteDriveOpMode : AresTeleOpBase() {

    private var lastHeartbeatTime = 0L
    private var lastHeartbeatVal = 0L

    override fun define() = aresTeleOp {
        
        onInit { robot, telemetry ->
            robot.base.mecanumIO.slewRateLimit = null // Disable slew limits for direct remote tracking
            telemetry.addData("Status", "Remote NT4 client drive mode initialized.")
        }
        
        onLoop { robot, driver, telemetry ->
            val nt4 = robot.base.telemetryManager.nt4
            val currentHeartbeat = nt4.getNumber("ARES/Input/heartbeat", 0.0).toLong()
            val now = com.areslib.util.RobotClock.currentTimeMillis()

            if (currentHeartbeat != lastHeartbeatVal) {
                lastHeartbeatVal = currentHeartbeat
                lastHeartbeatTime = now
            }

            // Command watchdog: If heartbeat hasn't changed in 1000ms, stop robot
            if (now - lastHeartbeatTime < 1000L) {
                val vx = nt4.getNumber("ARES/Input/vx", 0.0)
                val vy = nt4.getNumber("ARES/Input/vy", 0.0)
                val omega = nt4.getNumber("ARES/Input/omega", 0.0)

                robot.base.mecanumDrive.fieldRelativeDrive(
                    vx = vx,
                    vy = vy,
                    omega = omega,
                    useHeadingLock = false
                )
                
                // Parse commands
                val cmdStr = nt4.getString("ARES/Input/command", "")
                if (cmdStr.isNotEmpty()) {
                    nt4.putString("ARES/Input/command", "") // Clear command immediately
                    val parts = cmdStr.split("\\s+".toRegex()).filter { it.isNotEmpty() }
                    val cmdName = parts.firstOrNull()
                    when (cmdName) {
                        "reset" -> {
                            val x = parts.getOrNull(1)?.toDoubleOrNull() ?: 0.0
                            val y = parts.getOrNull(2)?.toDoubleOrNull() ?: 0.0
                            val h = parts.getOrNull(3)?.toDoubleOrNull() ?: 0.0
                            println("[RemoteDrive] Resetting EKF pose to: ($x, $y) at $h rad")
                            robot.base.store.dispatch(RobotAction.PoseUpdate(
                                xMeters = x,
                                yMeters = y,
                                headingRadians = h,
                                timestampMs = now
                            ))
                        }
                        "intake" -> {
                            val state = parts.getOrNull(1) == "on"
                            println("[RemoteDrive] Setting intake active = $state")
                            robot.setIntakeActive(state)
                        }
                        "flywheel" -> {
                            val rpm = parts.getOrNull(1)?.toDoubleOrNull()
                            if (rpm != null) {
                                println("[RemoteDrive] Setting flywheel active with RPM = $rpm")
                                robot.setFlywheelActive(true, rpm)
                            } else {
                                println("[RemoteDrive] Setting flywheel inactive")
                                robot.setFlywheelActive(false)
                            }
                        }
                    }
                }

                telemetry.addData("Status", "DRIVING")
                telemetry.addData("Inputs", "vx=%.2f, vy=%.2f, omega=%.2f".format(vx, vy, omega))
            } else {
                robot.base.mecanumDrive.fieldRelativeDrive(0.0, 0.0, 0.0, false)
                telemetry.addData("Status", "DISCONNECTED / STALE HEARTBEAT")
            }
        }
    }
}
