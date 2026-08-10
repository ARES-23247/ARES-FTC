package org.firstinspires.ftc.teamcode.opmodes

import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode
import com.qualcomm.robotcore.eventloop.opmode.TeleOp

/**
 * Hardware-free diagnostic that never constructs the ARES robot facade.
 * If this mode initializes while a normal mode hangs, investigate configured hardware/I2C startup
 * rather than the Driver Station lifecycle. It commands no actuator.
 */
@TeleOp(name = "AAA Blank Null OpMode", group = "Diagnostics")
class NullOpMode : LinearOpMode() {

    override fun runOpMode() {
        telemetry.addData("Status", "Initialized Successfully!")
        telemetry.addData("Diagnosis", "If you see this, Case A is true (I2C/Pinpoint Software Hang).")
        telemetry.update()

        waitForStart()

        while (opModeIsActive()) {
            telemetry.addData("Status", "Running...")
            telemetry.update()
            idle()
        }
    }
}
