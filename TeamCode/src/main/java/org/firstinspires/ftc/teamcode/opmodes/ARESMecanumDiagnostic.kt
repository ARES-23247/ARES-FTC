package org.firstinspires.ftc.teamcode.opmodes

import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode
import com.qualcomm.robotcore.eventloop.opmode.TeleOp
import com.qualcomm.robotcore.hardware.DcMotorEx

/**
 * Restrained-hardware drivetrain wiring diagnostic.
 *
 * Each face button commands exactly one raw motor at 40% output. Production names are
 * `fl`, `fr`, `rl`, and `rr`; rear `bl`/`br` aliases are accepted only to diagnose legacy
 * configurations. The `finally` block zeros every discovered motor on stop or failure.
 */
@TeleOp(name = "ARES Drivetrain Diagnostic", group = "ARES")
class ARESMecanumDiagnostic : LinearOpMode() {

    private fun findMotor(vararg names: String): DcMotorEx? {
        for (name in names) {
            try {
                return hardwareMap.get(DcMotorEx::class.java, name)
            } catch (_: Exception) {
                // Try the next diagnostic alias.
            }
        }
        return null
    }

    override fun runOpMode() {
        telemetry.addData("Status", "Initializing raw motors...")
        telemetry.update()
        val fl = findMotor("fl")
        val fr = findMotor("fr")
        val rl = findMotor("rl", "bl")
        val rr = findMotor("rr", "br")

        telemetry.addData("Status", "Ready. Press Play.")
        telemetry.addData("FL Motor (\"fl\")", if (fl != null) "FOUND" else "MISSING")
        telemetry.addData("FR Motor (\"fr\")", if (fr != null) "FOUND" else "MISSING")
        telemetry.addData("RL Motor (\"rl\")", if (rl != null) "FOUND" else "MISSING")
        telemetry.addData("RR Motor (\"rr\")", if (rr != null) "FOUND" else "MISSING")
        telemetry.update()

        waitForStart()

        try {
            while (opModeIsActive()) {
                val flPower = if (gamepad1.a) 0.4 else 0.0 // Cross / A
                val frPower = if (gamepad1.b) 0.4 else 0.0 // Circle / B
                val rlPower = if (gamepad1.x) 0.4 else 0.0 // Square / X
                val rrPower = if (gamepad1.y) 0.4 else 0.0 // Triangle / Y

                fl?.power = flPower
                fr?.power = frPower
                rl?.power = rlPower
                rr?.power = rrPower

                telemetry.addData("--- Raw Motor Controls ---", "")
                telemetry.addData("Hold Cross/A (FL)", flPower)
                telemetry.addData("Hold Circle/B (FR)", frPower)
                telemetry.addData("Hold Square/X (RL)", rlPower)
                telemetry.addData("Hold Triangle/Y (RR)", rrPower)
                telemetry.update()
                sleep(20)
            }
        } finally {
            fl?.power = 0.0
            fr?.power = 0.0
            rl?.power = 0.0
            rr?.power = 0.0
        }
    }
}
