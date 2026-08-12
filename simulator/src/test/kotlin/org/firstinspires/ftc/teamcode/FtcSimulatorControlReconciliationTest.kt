package org.firstinspires.ftc.teamcode

import com.areslib.action.RobotAction
import com.areslib.ftc.FtcMecanumRobot
import com.areslib.ftc.FtcTeleopDriveFrame
import com.areslib.hardware.HardwareRegistry
import com.areslib.sim.infra.SimGamepadManager
import com.areslib.sim.model.MecanumRobotDouble
import com.areslib.state.Alliance
import com.areslib.telemetry.AresGamepad
import com.areslib.telemetry.GamepadState
import com.areslib.util.RobotClock
import com.qualcomm.robotcore.hardware.Gamepad
import kotlin.math.abs
import org.firstinspires.ftc.teamcode.dsl.season
import org.firstinspires.ftc.teamcode.opmodes.robot.AresDriveController
import org.firstinspires.ftc.teamcode.opmodes.robot.AresSuperstructureController
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FtcSimulatorControlReconciliationTest {
    private var robot: FtcMecanumRobot? = null

    @After
    fun cleanUp() {
        runCatching { robot?.close() }
        robot = null
        HardwareRegistry.clear()
        RobotClock.useSystemTime()
    }

    @Test
    fun `normal teleop applies field and robot frames differently at ninety degree heading`() {
        RobotClock.useMockTime(1_000L)
        val base = buildRobot()
        base.store.dispatch(RobotAction.SetAlliance(Alliance.RED))
        base.store.dispatch(
            RobotAction.PoseUpdate(
                xMeters = 0.0,
                yMeters = 0.0,
                headingRadians = Math.PI / 2.0,
                timestampMs = RobotClock.currentTimeMillis(),
                isReset = true,
            )
        )

        val fieldGamepad = AresGamepad().apply {
            update(GamepadState(leftStickY = -1.0f))
        }
        base.teleopDriveFrame = FtcTeleopDriveFrame.FIELD_RELATIVE
        AresDriveController(base).driveWithGamepad(fieldGamepad, useHeadingLock = false)
        val fieldCommand = base.store.state.drive
        assertTrue(abs(fieldCommand.xVelocityMetersPerSecond) < 1e-8)
        assertTrue(fieldCommand.yVelocityMetersPerSecond < -0.1)

        val robotGamepad = AresGamepad().apply {
            update(GamepadState(leftStickY = -1.0f))
        }
        base.teleopDriveFrame = FtcTeleopDriveFrame.ROBOT_RELATIVE
        AresDriveController(base).driveWithGamepad(robotGamepad, useHeadingLock = false)
        val robotCommand = base.store.state.drive
        assertTrue(robotCommand.xVelocityMetersPerSecond > 0.1)
        assertTrue(abs(robotCommand.yVelocityMetersPerSecond) < 1e-8)
    }

    @Test
    fun `rejected shooter desired toggle retries through real gamepad binding after intake clears`() {
        RobotClock.useMockTime(1_000L)
        val base = buildRobot()
        val controller = AresSuperstructureController(base)
        val manager = SimGamepadManager()
        val sdkDriver = Gamepad()
        val sdkOperator = Gamepad()
        val aresGamepad = AresGamepad().apply {
            leftBumper.onPress("toggle intake", controller::toggleIntake)
            rightBumper.onPress("toggle shooter", controller::toggleShooter)
        }

        fun frame() {
            val accepted = base.store.state.superstructure.season
            manager.observeAcceptedMechanismState(
                intakeAccepted = accepted.intakeActive,
                flywheelAccepted = accepted.flywheelActive,
            )
            manager.writeEffectiveGamepads(sdkDriver, sdkOperator)
            aresGamepad.update(
                GamepadState(
                    leftBumper = sdkDriver.left_bumper,
                    rightBumper = sdkDriver.right_bumper,
                )
            )
            RobotClock.setMockTimeMs(RobotClock.currentTimeMillis() + 20L)
        }

        manager.isIntaking = true
        frame()
        assertTrue(base.store.state.superstructure.season.intakeActive)

        manager.isFlywheelOn = true
        frame()
        assertFalse("Intake interlock must reject the first shooter edge", base.store.state.superstructure.season.flywheelActive)

        manager.isIntaking = false
        frame()
        assertFalse(base.store.state.superstructure.season.intakeActive)

        repeat(15) { frame() }
        frame() // observe the accepted state for UI reconciliation
        assertTrue(base.store.state.superstructure.season.flywheelActive)
        assertTrue(manager.appliedIsFlywheelOn)
        assertTrue(manager.effectiveIsFlywheelOn)
    }

    private fun buildRobot(): FtcMecanumRobot {
        val robotDouble = MecanumRobotDouble()
        return FtcMecanumRobot(
            hardwareMap = robotDouble.hardwareMap,
            pinpointName = "pinpoint",
            limelightName = "limelight",
            imuName = "imu",
            pinpointIsCcwPositive = true,
        ).also { robot = it }
    }
}
