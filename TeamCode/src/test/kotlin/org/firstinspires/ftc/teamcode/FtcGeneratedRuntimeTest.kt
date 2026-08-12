package org.firstinspires.ftc.teamcode

import com.areslib.action.RobotAction
import com.areslib.math.geometry.Pose2d
import com.areslib.math.geometry.Rotation2d
import com.areslib.routine.AutonomousCatalogEntry
import com.areslib.routine.RoutineDocument
import com.areslib.routine.RoutineDriveStep
import com.areslib.routine.RoutinePose
import com.areslib.routine.RoutineStep
import com.areslib.sequencer.Task
import com.areslib.sequencer.TaskExecutor
import com.areslib.sequencer.TaskStateMachine
import com.areslib.sequencer.TaskStatus
import com.areslib.state.Alliance
import com.areslib.state.DriveState
import com.areslib.state.ObstacleType
import com.areslib.state.RobotFieldObstacle
import com.areslib.state.RobotState
import com.areslib.math.estimation.PoseEstimatorState
import org.firstinspires.ftc.teamcode.dsl.FtcDelegateStatusBridge
import org.firstinspires.ftc.teamcode.dsl.FtcDriveMotionKind
import org.firstinspires.ftc.teamcode.dsl.FtcFieldEnvelope
import org.firstinspires.ftc.teamcode.dsl.FtcRotateToHeadingTask
import org.firstinspires.ftc.teamcode.dsl.classifyFtcDriveMotion
import org.firstinspires.ftc.teamcode.dsl.composeFtcDriveLifecycle
import org.firstinspires.ftc.teamcode.dsl.isFtcRobotPoseWithinField
import org.firstinspires.ftc.teamcode.dsl.isFtcRobotSweepCollisionFree
import org.firstinspires.ftc.teamcode.dsl.validateFtcAutonomousBounds
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FtcGeneratedRuntimeTest {
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
            obstacles = emptyList(),
        )

        assertEquals(1, errors.size)
        assertTrue(errors.single().contains("drive target leaves"))
    }

    @Test
    fun `same pose is immediate while same position with new heading rotates`() {
        val start = Pose2d(0.2, -0.3, Rotation2d(0.25))

        assertEquals(FtcDriveMotionKind.IMMEDIATE, classifyFtcDriveMotion(start, start))
        assertEquals(
            FtcDriveMotionKind.ROTATE,
            classifyFtcDriveMotion(start, Pose2d(start.x, start.y, Rotation2d(1.0))),
        )
        assertEquals(
            FtcDriveMotionKind.TRANSLATE,
            classifyFtcDriveMotion(start, Pose2d(start.x + 0.1, start.y, start.heading)),
        )
    }

    @Test
    fun `heading-only task commands CCW rotation and always emits a zero ending`() {
        val task = FtcRotateToHeadingTask(targetHeadingRadians = 1.0, maxOmegaRadiansPerSecond = 2.0)
        val state = RobotState(
            drive = DriveState(
                poseEstimator = PoseEstimatorState(estimatedPoseHeading = 0.25),
                measuredAngularVelocityRadiansPerSecond = 0.0,
            )
        )
        task.initialize(state)

        val moving = task.execute(state, 0L).single() as RobotAction.JoystickDriveIntent
        assertEquals(0.0, moving.targetXVelocity, 0.0)
        assertEquals(0.0, moving.targetYVelocity, 0.0)
        assertTrue(moving.targetAngularVelocity > 0.0)
        val stopped = task.end(state, interrupted = true).single() as RobotAction.JoystickDriveIntent
        assertEquals(0.0, stopped.targetAngularVelocity, 0.0)
    }

    @Test
    fun `swept footprint rejects an obstacle between safe endpoints`() {
        val envelope = FtcFieldEnvelope(4.0, 4.0, 0.4, 0.4)
        val start = Pose2d(-1.0, 0.0, Rotation2d())
        val end = Pose2d(1.0, 0.0, Rotation2d())
        val obstacle = RobotFieldObstacle(
            id = "center",
            x = 0.0,
            y = 0.0,
            width = 0.2,
            height = 0.8,
            isBlocking = true,
            obstacleType = ObstacleType.BLOCKING,
        )

        assertTrue(isFtcRobotPoseWithinField(start, envelope))
        assertTrue(isFtcRobotPoseWithinField(end, envelope))
        assertFalse(isFtcRobotSweepCollisionFree(start, end, envelope, listOf(obstacle)))
        assertTrue(isFtcRobotSweepCollisionFree(start, end, envelope, emptyList()))
    }

    @Test
    fun `drive wrapper mirrors first failed or cancelled child status once`() {
        val state = RobotState()
        val failedOwner = RecordingTask("failed-owner", Long.MAX_VALUE)
        val failedChild = RecordingTask("failed-child", Long.MAX_VALUE)
        failedOwner.initialize(state)
        failedChild.initialize(state)
        val failedBridge = FtcDelegateStatusBridge(failedOwner)
        TaskStateMachine.markFailed(failedChild)
        assertEquals(TaskStatus.FAILED, failedBridge.propagate(failedChild))
        assertEquals(TaskStatus.FAILED, TaskStateMachine.getStatus(failedOwner))
        TaskStateMachine.transitionTo(failedChild, TaskStatus.CANCELLED)
        failedBridge.propagate(failedChild)
        assertEquals("A later observation cannot replace the first terminal propagation", TaskStatus.FAILED, failedBridge.terminalStatus)
        assertEquals(TaskStatus.FAILED, TaskStateMachine.getStatus(failedOwner))

        val cancelledOwner = RecordingTask("cancelled-owner", Long.MAX_VALUE)
        val cancelledChild = RecordingTask("cancelled-child", Long.MAX_VALUE)
        cancelledOwner.initialize(state)
        cancelledChild.initialize(state)
        TaskStateMachine.transitionTo(cancelledChild, TaskStatus.CANCELLED)
        val cancelledBridge = FtcDelegateStatusBridge(cancelledOwner)
        assertEquals(TaskStatus.CANCELLED, cancelledBridge.propagate(cancelledChild))
        assertEquals(TaskStatus.CANCELLED, TaskStateMachine.getStatus(cancelledOwner))
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
