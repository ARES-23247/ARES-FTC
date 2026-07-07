package org.firstinspires.ftc.teamcode.opmodes

import com.qualcomm.robotcore.eventloop.opmode.Autonomous
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode
import org.firstinspires.ftc.teamcode.TestPathAuto
import com.areslib.pathing.HolonomicPathFollower
import com.areslib.util.RobotClock

@Autonomous(name = "Auto: TestPath", group = "ARES")
class AutoTestPathOpMode : LinearOpMode() {
    override fun runOpMode() {
        val robot = AresRobot(hardwareMap, telemetry)

        val drivetrain = object : com.areslib.subsystem.DrivetrainSubsystem {
            override fun setChassisSpeeds(vx: Double, vy: Double, omega: Double) {
                robot.base.mecanumDrive.robotRelativeDrive(vx, vy, omega)
            }
            override fun getEstimatedPose(): com.areslib.math.Pose2d {
                return robot.base.store.state.drive.poseEstimator.estimatedPose
            }
            override fun readSensors(store: com.areslib.subsystem.Store, timestampMs: Long) {}
            override fun writeOutputs(state: com.areslib.state.RobotState, scale: Double) {}
        }
        val follower = HolonomicPathFollower(drivetrain)

        TestPathAuto.buildPathFollower(
            follower,
            eventMap = mapOf(
                "intake_on" to { robot.intake(1.0) },
                "intake_off" to { robot.intake(0.0) },
                "shoot" to { robot.shoot(2000.0) }
            )
        )

        telemetry.addData("Status", "Initialized TestPath")
        telemetry.update()

        waitForStart()

        if (isStopRequested) return

        // Note: For a real path, you would wrap this in a RobotSequence
        // or call a trajectory sampler. This is just a compilation fix.
        while (opModeIsActive()) {
            robot.update()
            
            telemetry.addData("Pose", robot.base.store.state.drive.poseEstimator.estimatedPose)
            telemetry.update()
        }

        robot.stopAll()
        robot.close()
    }
}
