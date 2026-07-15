package org.firstinspires.ftc.teamcode.opmodes

import com.areslib.ftc.FtcMecanumRobot
import com.qualcomm.robotcore.hardware.HardwareMap
import org.firstinspires.ftc.robotcore.external.Telemetry
import com.areslib.hardware.HardwareRegistry
import com.areslib.action.RobotAction
import org.firstinspires.ftc.teamcode.dsl.*

/**
 * Team-specific wrapper around the core FtcMecanumRobot.
 * Stripped to drive-only for maximum loop performance.
 * Subsystem IOs (intake, flywheel) can be re-added when physical hardware is present.
 */
class AresRobot(
    val hardwareMap: HardwareMap,
    val localTelemetry: Telemetry? = null
) {
    val base = FtcMecanumRobot(
        hardwareMap = hardwareMap,
        rlName = "rl",
        rrName = "rr",
        flDirection = com.qualcomm.robotcore.hardware.DcMotorSimple.Direction.FORWARD,
        frDirection = com.qualcomm.robotcore.hardware.DcMotorSimple.Direction.REVERSE,
        rlDirection = com.qualcomm.robotcore.hardware.DcMotorSimple.Direction.FORWARD,
        rrDirection = com.qualcomm.robotcore.hardware.DcMotorSimple.Direction.REVERSE,
        pinpointName = org.firstinspires.ftc.teamcode.config.HardwareConstants.ODOMETRY_PINPOINT,
        limelightName = org.firstinspires.ftc.teamcode.config.HardwareConstants.VISION_LIMELIGHT,
        localTelemetry = localTelemetry
    )

    @kotlin.jvm.JvmOverloads
    fun update(
        gamepad1: com.areslib.telemetry.GamepadState? = null,
        gamepad2: com.areslib.telemetry.GamepadState? = null
    ) {
        // Update drivebase sensors, EKF, and kinematics
        base.update(gamepad1, gamepad2)
    }

    fun driveFieldCentric(x: Double, y: Double, rotation: Double) {
        base.driveFieldCentric(x, y, rotation)
    }

    fun driveRobotCentric(x: Double, y: Double, rotation: Double) {
        base.driveRobotCentric(x, y, rotation)
    }

    fun close() {
        base.close()
    }
}
