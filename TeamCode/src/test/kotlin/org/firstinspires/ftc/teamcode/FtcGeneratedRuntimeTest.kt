package org.firstinspires.ftc.teamcode

import com.areslib.action.RobotAction
import com.areslib.ftc.input.FtcButtonIndex
import com.areslib.input.BindingReleaseReason
import com.areslib.input.ControllerBindingRuntime
import com.areslib.input.DigitalBinding
import com.areslib.input.DigitalBindingListener
import com.areslib.input.RawButtonSource
import com.areslib.math.geometry.Pose2d
import com.areslib.math.geometry.Rotation2d
import com.areslib.routine.AutonomousCatalogEntry
import com.areslib.routine.RoutineDocument
import com.areslib.routine.RoutineDriveStep
import com.areslib.routine.RoutinePose
import com.areslib.routine.RoutineStep
import com.areslib.sequencer.Task
import com.areslib.sequencer.TaskExecutor
import com.areslib.state.Alliance
import com.areslib.state.RobotState
import com.areslib.telemetry.GamepadState
import com.qualcomm.robotcore.hardware.Gamepad
import org.firstinspires.ftc.teamcode.dsl.FtcFieldEnvelope
import org.firstinspires.ftc.teamcode.dsl.FtcGeneratedControllerRunner
import org.firstinspires.ftc.teamcode.dsl.composeFtcDriveLifecycle
import org.firstinspires.ftc.teamcode.dsl.isFtcRobotPoseWithinField
import org.firstinspires.ftc.teamcode.dsl.validateFtcAutonomousBounds
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FtcGeneratedRuntimeTest {
    @Test
    fun `extended snapshot and keyboard buttons reach generated bindings`() {
        val m1 = RecordingBindingListener()
        val f1 = RecordingBindingListener()
        val runtime = ControllerBindingRuntime(
            digitalBindings = listOf(
                DigitalBinding(RawButtonSource(FtcButtonIndex.M1), listener = m1),
                DigitalBinding(RawButtonSource(FtcButtonIndex.F1), listener = f1),
            ),
            nanoTime = { 1L },
        )
        val runner = FtcGeneratedControllerRunner(
            gamepad1 = Gamepad().apply { id = 0 },
            gamepad2 = Gamepad().apply { id = 1 },
            runtimes = mapOf("driver" to runtime),
        )

        // A newly connected controller must report neutral once before bindings arm.
        runner.update(GamepadState(), GamepadState())
        runner.update(GamepadState(m1 = true), GamepadState())
        assertEquals(1, m1.presses)
        assertEquals(0, f1.presses)

        assertTrue(runner.onKeyDown("driver", 131))
        runner.update(GamepadState(m1 = true), GamepadState())
        assertEquals(1, f1.presses)
        assertTrue(runner.onKeyUp("driver", 131))
        runner.update(GamepadState(), GamepadState())
        assertEquals(1, f1.releases)
        assertFalse(runner.onKeyDown("driver", 99))
    }

    @Test
    fun `drive is deadline for during actions and arrival starts afterward`() {
        val drive = RecordingTask("drive", completeAtMs = 10L)
        val during = RecordingTask("during", completeAtMs = Long.MAX_VALUE)
        val arrival = RecordingTask("arrival", completeAtMs = 0L)
        val executor = TaskExecutor().apply {
            addTask(composeFtcDriveLifecycle(drive, listOf(during), listOf(arrival)))
        }
        val state = RobotState()

        executor.update(state, 0L)
        assertEquals(1, drive.initializations)
        assertEquals(1, during.initializations)
        assertEquals(0, arrival.initializations)

        executor.update(state, 10L)
        assertEquals(false, drive.lastInterrupted)
        assertEquals(true, during.lastInterrupted)
        assertEquals(1, arrival.initializations)
        assertEquals(0, executor.size)
    }

    @Test
    fun `rotated robot footprint must remain entirely inside field`() {
        val envelope = FtcFieldEnvelope(
            fieldWidthMeters = 3.0,
            fieldHeightMeters = 3.0,
            robotLengthMeters = 1.0,
            robotWidthMeters = 0.4,
        )

        assertTrue(isFtcRobotPoseWithinField(Pose2d(1.0, 0.0, Rotation2d(0.0)), envelope))
        assertFalse(isFtcRobotPoseWithinField(Pose2d(1.01, 0.0, Rotation2d(0.0)), envelope))
        assertTrue(isFtcRobotPoseWithinField(Pose2d(1.25, 0.0, Rotation2d(Math.PI / 2.0)), envelope))
        assertFalse(isFtcRobotPoseWithinField(Pose2d(1.31, 0.0, Rotation2d(Math.PI / 2.0)), envelope))
    }

    @Test
    fun `bounds preflight traverses called routines and rejects unsafe goals`() {
        val entry = AutonomousCatalogEntry(
            entryId = "match",
            displayName = "Match",
            routineId = "root",
            startingPose = RoutinePose(0.0, 0.0, 0.0),
        )
        val routines = mapOf(
            "root" to RoutineDocument(
                documentId = "root",
                name = "Root",
                steps = listOf(RoutineStep.call("nested")),
            ),
            "nested" to RoutineDocument(
                documentId = "nested",
                name = "Nested",
                steps = listOf(
                    RoutineStep.driveTo(
                        RoutineDriveStep(RoutinePose(1.7, 0.0, 0.0)),
                    ),
                ),
            ),
        )
        val errors = validateFtcAutonomousBounds(
            entry = entry,
            routines = routines,
            envelope = FtcFieldEnvelope(3.6576, 3.6576, 0.45, 0.45),
            selectedAlliance = Alliance.RED,
        )

        assertEquals(1, errors.size)
        assertTrue(errors.single().contains("drive target leaves"))
    }

    private class RecordingBindingListener : DigitalBindingListener {
        var presses = 0
        var releases = 0
        override fun onPress() {
            presses++
        }
        override fun onRelease(heldForNanos: Long, reason: BindingReleaseReason) {
            releases++
        }
    }

    private class RecordingTask(
        override val name: String,
        private val completeAtMs: Long,
    ) : Task {
        var initializations = 0
        var lastInterrupted: Boolean? = null

        override fun initialize(state: RobotState): List<RobotAction> {
            initializations++
            return super.initialize(state)
        }

        override fun isCompleted(state: RobotState, elapsedMs: Long): Boolean = elapsedMs >= completeAtMs

        override fun end(state: RobotState, interrupted: Boolean): List<RobotAction> {
            lastInterrupted = interrupted
            return super.end(state, interrupted)
        }
    }
}
