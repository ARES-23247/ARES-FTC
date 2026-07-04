package org.firstinspires.ftc.teamcode.opmodes

import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode
import com.qualcomm.robotcore.eventloop.opmode.TeleOp
import com.qualcomm.robotcore.hardware.DcMotorEx

@TeleOp(name = "ARES Drivetrain Diagnostic", group = "ARES")
class ARESMecanumDiagnostic : LinearOpMode() {

    override fun runOpMode() {
        telemetry.addData("Status", "Initializing raw motors...")
        telemetry.update()

        val fl = try { hardwareMap.get(DcMotorEx::class.java, "fl") } catch (_: Exception) { null }
        val fr = try { hardwareMap.get(DcMotorEx::class.java, "fr") } catch (_: Exception) { null }
        val rl = try { hardwareMap.get(DcMotorEx::class.java, "rl") } catch (_: Exception) { null }
        val rr = try { hardwareMap.get(DcMotorEx::class.java, "rr") } catch (_: Exception) { null }

        telemetry.addData("Status", "Ready. Press Play.")
        telemetry.addData("FL Motor (\"fl\")", if (fl != null) "FOUND" else "MISSING")
        telemetry.addData("FR Motor (\"fr\")", if (fr != null) "FOUND" else "MISSING")
        telemetry.addData("RL Motor (\"rl\")", if (rl != null) "FOUND" else "MISSING")
        telemetry.addData("RR Motor (\"rr\")", if (rr != null) "FOUND" else "MISSING")
        telemetry.update()

        waitForStart()

        while (opModeIsActive()) {
            val flPower = if (gamepad1.a) 0.4 else 0.0  // Cross / A
            val frPower = if (gamepad1.b) 0.4 else 0.0  // Circle / B
            val rlPower = if (gamepad1.x) 0.4 else 0.0  // Square / X
            val rrPower = if (gamepad1.y) 0.4 else 0.0  // Triangle / Y

            fl?.power = flPower
            fr?.power = frPower
            rl?.power = rlPower
            rr?.power = rrPower

            telemetry.addData("--- Raw Motor Controls ---", "")
            telemetry.addData("Hold Cross / A", "Spin FL (\"fl\") -> Speed: ${flPower}")
            telemetry.addData("Hold Circle / B", "Spin FR (\"fr\") -> Speed: ${frPower}")
            telemetry.addData("Hold Square / X", "Spin RL (\"rl\") -> Speed: ${rlPower}")
            telemetry.addData("Hold Triangle / Y", "Spin RR (\"rr\") -> Speed: ${rrPower}")
            telemetry.update()
        }

        fl?.power = 0.0
        fr?.power = 0.0
        rl?.power = 0.0
        rr?.power = 0.0
    }
}
