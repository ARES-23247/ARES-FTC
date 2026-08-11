package org.firstinspires.ftc.teamcode.dsl

import com.areslib.action.RobotAction
import com.areslib.ftc.FtcKeyboardListener
import com.areslib.ftc.input.FtcButtonIndex
import com.areslib.ftc.input.FtcInputFrameAdapter
import com.areslib.input.ControllerBindingRuntime
import com.areslib.input.InputFrame
import com.areslib.math.geometry.Pose2d
import com.areslib.math.geometry.Rotation2d
import com.areslib.math.geometry.Translation2d
import com.areslib.pathing.CommandKey
import com.areslib.pathing.NamedCommands
import com.areslib.pathing.Path
import com.areslib.pathing.PathEvent
import com.areslib.pathing.PathPlannerParser
import com.areslib.routine.AutonomousCatalogEntry
import com.areslib.routine.RoutineDriveStep
import com.areslib.routine.RoutineDocument
import com.areslib.routine.RoutineManager
import com.areslib.routine.RoutinePose
import com.areslib.sequencer.FollowPathTask
import com.areslib.sequencer.ParallelDeadlineGroup
import com.areslib.sequencer.SequentialTaskGroup
import com.areslib.sequencer.Task
import com.areslib.sequencer.TaskExecutor
import com.areslib.state.RobotState
import com.areslib.telemetry.GamepadState
import com.areslib.util.RobotClock
import com.qualcomm.robotcore.hardware.Gamepad
import org.firstinspires.ftc.teamcode.generated.GeneratedAresProject
import org.firstinspires.ftc.teamcode.generated.GeneratedAresProjectCapabilities
import org.firstinspires.ftc.teamcode.generated.GeneratedAresProjectControlTaskSink
import org.firstinspires.ftc.teamcode.opmodes.AresRobot
import kotlin.math.hypot

/** Robot-side adapter for the deterministic Kotlin emitted from the checked-in `.ares` project. */
internal class FtcGeneratedProjectRuntime(
    private val robot: AresRobot,
    private val autonomousEntry: AutonomousCatalogEntry? = null,
    private val selectedAlliance: com.areslib.state.Alliance = robot.base.store.state.drive.alliance,
) : GeneratedAresProjectCapabilities {
    private val directTaskExecutor = TaskExecutor()

    val routineManager = RoutineManager(
        bindings = GeneratedAresProject.runtimeBindings(this),
        stateProvider = { robot.base.store.state },
        dispatch = robot.base.store::dispatch,
    ).also { manager ->
        manager.replaceDocuments(GeneratedAresProject.routines.values)
    }

    /** Runs generated routine tasks and controller-submitted one-shot tasks once per robot frame. */
    fun updateTasks() {
        val state = robot.base.store.state
        val actions = directTaskExecutor.update(state, RobotClock.currentTimeMillis())
        for (action in actions) robot.base.store.dispatch(action)
        routineManager.update()
    }

    /** Disable/stop safety hook. Cleanup actions are dispatched before lifecycle cancellation. */
    fun cancelAll(reason: String) {
        val actions = directTaskExecutor.cancelAll(robot.base.store.state)
        for (action in actions) robot.base.store.dispatch(action)
        routineManager.cancelAll(reason)
    }

    fun createControls(schemeId: String): Map<String, ControllerBindingRuntime> =
        GeneratedAresProject.createControllerRuntimes(
            schemeId = schemeId,
            registry = this,
            routineManager = routineManager,
            taskSink = GeneratedAresProjectControlTaskSink { _, task -> directTaskExecutor.addTask(task) },
        )

    override fun createDriveTask(step: RoutineDriveStep): Task {
        val target = autonomousEntry?.let { resolveFtcAutonomousPose(it, selectedAlliance, step.target) }
            ?: step.target.toPose2d()
        require(isFtcRobotPoseWithinField(target, ftcFieldEnvelopeForRobot(robot))) {
            "Drive target (${target.x}, ${target.y}) leaves the FTC field with the configured robot footprint"
        }
        val preset = FtcMotionPreset.fromKey(step.motionPresetKey)
        return FtcRoutineDriveTask(robot, step, target, preset)
    }

    override fun actionSetIndicatorColorBLUE() = command("SetIndicatorColor_BLUE")
    override fun actionSetIndicatorColorCYAN() = command("SetIndicatorColor_CYAN")
    override fun actionSetIndicatorColorGREEN() = command("SetIndicatorColor_GREEN")
    override fun actionSetIndicatorColorOFF() = command("SetIndicatorColor_OFF")
    override fun actionSetIndicatorColorORANGE() = command("SetIndicatorColor_ORANGE")
    override fun actionSetIndicatorColorPURPLE() = command("SetIndicatorColor_PURPLE")
    override fun actionSetIndicatorColorRAINBOW() = command("SetIndicatorColor_RAINBOW")
    override fun actionSetIndicatorColorRED() = command("SetIndicatorColor_RED")
    override fun actionSetIndicatorColorVIOLET() = command("SetIndicatorColor_VIOLET")
    override fun actionSetIndicatorColorWHITE() = command("SetIndicatorColor_WHITE")
    override fun actionSetIndicatorColorYELLOW() = command("SetIndicatorColor_YELLOW")
    override fun actionSetSecondIndicatorColorBLUE() = command("SetSecondIndicatorColor_BLUE")
    override fun actionSetSecondIndicatorColorCYAN() = command("SetSecondIndicatorColor_CYAN")
    override fun actionSetSecondIndicatorColorGREEN() = command("SetSecondIndicatorColor_GREEN")
    override fun actionSetSecondIndicatorColorOFF() = command("SetSecondIndicatorColor_OFF")
    override fun actionSetSecondIndicatorColorORANGE() = command("SetSecondIndicatorColor_ORANGE")
    override fun actionSetSecondIndicatorColorPURPLE() = command("SetSecondIndicatorColor_PURPLE")
    override fun actionSetSecondIndicatorColorRAINBOW() = command("SetSecondIndicatorColor_RAINBOW")
    override fun actionSetSecondIndicatorColorRED() = command("SetSecondIndicatorColor_RED")
    override fun actionSetSecondIndicatorColorVIOLET() = command("SetSecondIndicatorColor_VIOLET")
    override fun actionSetSecondIndicatorColorWHITE() = command("SetSecondIndicatorColor_WHITE")
    override fun actionSetSecondIndicatorColorYELLOW() = command("SetSecondIndicatorColor_YELLOW")
    override fun actionFlywheelPrepare() = command("flywheel.prepare")
    override fun actionFlywheelStop() = command("flywheel.stop")
    override fun actionIntakeCollect() = command("intake.collect")
    override fun actionIntakeStop() = command("intake.stop")

    private fun command(key: String): Task = requireNotNull(
        NamedCommands.create(CommandKey(key), RobotClock.currentTimeMillis()),
    ) { "Generated action '$key' was not registered by the FTC robot" }
}

/**
 * Preallocated FTC controller bridge for one generated scheme.
 *
 * Saved schemes use semantic slots. The robot accepts the novice-facing `driver`/`operator`
 * names plus explicit gamepad aliases; unsupported slots fail during INIT instead of being
 * silently ignored. Vendor buttons remain neutral unless the FTC keyboard/overlay bridge supplies
 * them -- raw Flydigi indexes are intentionally never guessed here.
 */
internal class FtcGeneratedControllerRunner(
    gamepad1: Gamepad,
    gamepad2: Gamepad,
    runtimes: Map<String, ControllerBindingRuntime>,
) {
    private val driverOverlay = GamepadState()
    private val operatorOverlay = GamepadState()
    private val driverKeyboard = GamepadState()
    private val operatorKeyboard = GamepadState()
    private val keyboardListener = FtcKeyboardListener()
    private val driverFrame = InputFrame(buttonCapacity = FtcButtonIndex.COUNT)
    private val operatorFrame = InputFrame(buttonCapacity = FtcButtonIndex.COUNT)
    private val driverAdapter = FtcInputFrameAdapter(gamepad1, driverOverlay)
    private val operatorAdapter = FtcInputFrameAdapter(gamepad2, operatorOverlay)
    private var driverRuntime: ControllerBindingRuntime? = null
    private var operatorRuntime: ControllerBindingRuntime? = null

    init {
        runtimes.forEach { (slot, runtime) ->
            when (slot.lowercase()) {
                "driver", "gamepad1", "controller1", "0" -> {
                    require(driverRuntime == null) { "Control scheme maps more than one slot to gamepad1" }
                    driverRuntime = runtime
                }
                "operator", "gamepad2", "controller2", "1" -> {
                    require(operatorRuntime == null) { "Control scheme maps more than one slot to gamepad2" }
                    operatorRuntime = runtime
                }
                else -> error(
                    "FTC control slot '$slot' is unsupported; use driver/gamepad1 or operator/gamepad2",
                )
            }
        }
    }

    /** Merges the current SDK snapshot and persistent keyboard overlay, then samples both slots. */
    fun update(driverState: GamepadState, operatorState: GamepadState) {
        val now = RobotClock.nanoTime()
        driverRuntime?.let { runtime ->
            mergeExtendedButtons(driverState, driverKeyboard, driverOverlay)
            driverAdapter.sampleInto(driverFrame, now)
            runtime.update(driverFrame, now)
        }
        operatorRuntime?.let { runtime ->
            mergeExtendedButtons(operatorState, operatorKeyboard, operatorOverlay)
            operatorAdapter.sampleInto(operatorFrame, now)
            runtime.update(operatorFrame, now)
        }
    }

    /** Forwards Android F1-F12 key-down events when the host Activity exposes them. */
    fun onKeyDown(controllerSlot: String, keyCode: Int): Boolean =
        keyboardListener.onKeyDown(keyCode, keyboardState(controllerSlot))

    /** Forwards Android F1-F12 key-up events when the host Activity exposes them. */
    fun onKeyUp(controllerSlot: String, keyCode: Int): Boolean =
        keyboardListener.onKeyUp(keyCode, keyboardState(controllerSlot))

    fun cancel() {
        driverRuntime?.cancel()
        operatorRuntime?.cancel()
    }

    private fun keyboardState(controllerSlot: String): GamepadState = when (controllerSlot.lowercase()) {
        "driver", "gamepad1", "controller1", "0" -> driverKeyboard
        "operator", "gamepad2", "controller2", "1" -> operatorKeyboard
        else -> throw IllegalArgumentException("Unknown FTC controller slot '$controllerSlot'")
    }

    private fun mergeExtendedButtons(
        snapshot: GamepadState,
        keyboard: GamepadState,
        destination: GamepadState,
    ) {
        destination.c = snapshot.c
        destination.z = snapshot.z
        destination.m1 = snapshot.m1
        destination.m2 = snapshot.m2
        destination.m3 = snapshot.m3
        destination.m4 = snapshot.m4
        destination.f1 = snapshot.f1 || keyboard.f1
        destination.f2 = snapshot.f2 || keyboard.f2
        destination.f3 = snapshot.f3 || keyboard.f3
        destination.f4 = snapshot.f4 || keyboard.f4
        destination.f5 = snapshot.f5 || keyboard.f5
        destination.f6 = snapshot.f6 || keyboard.f6
        destination.f7 = snapshot.f7 || keyboard.f7
        destination.f8 = snapshot.f8 || keyboard.f8
        destination.f9 = snapshot.f9 || keyboard.f9
        destination.f10 = snapshot.f10 || keyboard.f10
        destination.f11 = snapshot.f11 || keyboard.f11
        destination.f12 = snapshot.f12 || keyboard.f12
    }
}

private enum class FtcMotionPreset(val speedScale: Double, val accelerationScale: Double) {
    SAFE(0.40, 0.45),
    BALANCED(0.65, 0.70),
    FAST(0.85, 0.90),
    ADAPTIVE(0.55, 0.60);

    companion object {
        fun fromKey(key: String): FtcMotionPreset = when (key.lowercase()) {
            "safe" -> SAFE
            "balanced" -> BALANCED
            "fast" -> FAST
            "adaptive" -> ADAPTIVE
            else -> throw IllegalArgumentException("Unknown FTC motion preset '$key'")
        }
    }
}

/** Builds a fresh direct spline from the estimator pose when the drive node actually starts. */
private class FtcRoutineDriveTask(
    private val robot: AresRobot,
    private val step: RoutineDriveStep,
    private val target: Pose2d,
    private val preset: FtcMotionPreset,
) : Task {
    override val name: String = "FTC drive to (%.2f, %.2f)".format(target.x, target.y)
    private var delegate: Task? = null

    override fun initialize(state: RobotState): List<RobotAction> {
        super.initialize(state)
        val start = state.drive.poseEstimator.estimatedPose
        val maximumVelocity = robot.base.mecanumIO.maxWheelSpeedMetersPerSecond * preset.speedScale
        val maximumAcceleration = state.tuning.pathAccelerationLimit * preset.accelerationScale
        val generatedPath = if (hypot(target.x - start.x, target.y - start.y) < 1e-6) {
            Path(emptyList())
        } else {
            PathPlannerParser.generatePath(
                points = listOf(Translation2d(start.x, start.y), Translation2d(target.x, target.y)),
                startHeading = start.heading,
                endHeading = target.heading,
                maxVelocityMps = maximumVelocity,
                maxAccelerationMps2 = maximumAcceleration,
            )
        }
        val totalDistance = generatedPath.points.lastOrNull()?.distanceMeters ?: 0.0
        val events = step.markers.map { marker ->
            PathEvent(marker.actionKey, marker.progress * totalDistance)
        }
        val pathTask = FollowPathTask(
            follower = robot.base.pathFollower,
            path = generatedPath.copy(events = events),
            mirrorForAlliance = false,
        )
        val duringTasks = step.duringActionKeys.map { NamedCommands.task(CommandKey(it)) }
        val arrivalTasks = step.arrivalActionKeys.map { NamedCommands.task(CommandKey(it)) }
        val compiled = composeFtcDriveLifecycle(pathTask, duringTasks, arrivalTasks)
        delegate = compiled
        return compiled.initialize(state)
    }

    override fun isCompleted(state: RobotState, elapsedMs: Long): Boolean =
        delegate?.isCompleted(state, elapsedMs) ?: true

    override fun execute(state: RobotState, elapsedMs: Long): List<RobotAction> =
        delegate?.execute(state, elapsedMs).orEmpty()

    override fun end(state: RobotState, interrupted: Boolean): List<RobotAction> {
        val actions = delegate?.end(state, interrupted).orEmpty()
        super.end(state, interrupted)
        return actions
    }

    override fun releaseRuntimeState() {
        delegate?.releaseRuntimeState()
        delegate = null
        super.releaseRuntimeState()
    }
}

private fun RoutinePose.toPose2d(): Pose2d = Pose2d(xMeters, yMeters, Rotation2d(headingRadians))

/** Drive owns the deadline; unfinished during-actions are interrupted before arrival actions start. */
internal fun composeFtcDriveLifecycle(
    driveTask: Task,
    duringTasks: List<Task>,
    arrivalTasks: List<Task>,
): Task {
    val driveWithDuring = if (duringTasks.isEmpty()) {
        driveTask
    } else {
        ParallelDeadlineGroup(deadline = driveTask, otherTasks = duringTasks)
    }
    return SequentialTaskGroup(listOf(driveWithDuring) + arrivalTasks)
}

/** Center-origin FTC field and rectangular robot-footprint geometry used by runtime preflight. */
internal data class FtcFieldEnvelope(
    val fieldWidthMeters: Double,
    val fieldHeightMeters: Double,
    val robotLengthMeters: Double,
    val robotWidthMeters: Double,
) {
    init {
        require(fieldWidthMeters.isFinite() && fieldWidthMeters > 0.0)
        require(fieldHeightMeters.isFinite() && fieldHeightMeters > 0.0)
        require(robotLengthMeters.isFinite() && robotLengthMeters > 0.0)
        require(robotWidthMeters.isFinite() && robotWidthMeters > 0.0)
    }
}

/** Resolves the active FTC field plus the season robot's configured mecanum footprint. */
internal fun ftcFieldEnvelopeForRobot(robot: AresRobot): FtcFieldEnvelope {
    val field = com.areslib.state.RobotFieldManager.activeConfig
    require(field.fieldType == com.areslib.state.FieldType.FTC) {
        "Active field config '${field.name}' is not an FTC field"
    }
    return FtcFieldEnvelope(
        fieldWidthMeters = field.resolvedWidthMeters,
        fieldHeightMeters = field.resolvedHeightMeters,
        robotLengthMeters = robot.base.wheelBaseMeters,
        robotWidthMeters = robot.base.trackWidthMeters,
    )
}

/** Returns true only when every corner of the rotated robot lies within the FTC field. */
internal fun isFtcRobotPoseWithinField(pose: Pose2d, envelope: FtcFieldEnvelope): Boolean {
    if (!pose.x.isFinite() || !pose.y.isFinite() || !pose.heading.radians.isFinite()) return false
    val heading = pose.heading.radians
    val absCos = kotlin.math.abs(kotlin.math.cos(heading))
    val absSin = kotlin.math.abs(kotlin.math.sin(heading))
    val xExtent = absCos * envelope.robotLengthMeters * 0.5 +
        absSin * envelope.robotWidthMeters * 0.5
    val yExtent = absSin * envelope.robotLengthMeters * 0.5 +
        absCos * envelope.robotWidthMeters * 0.5
    val halfFieldX = envelope.fieldWidthMeters * 0.5
    val halfFieldY = envelope.fieldHeightMeters * 0.5
    return pose.x - xExtent >= -halfFieldX && pose.x + xExtent <= halfFieldX &&
        pose.y - yExtent >= -halfFieldY && pose.y + yExtent <= halfFieldY
}

/** Validates the selected start and every reachable drive target before hardware pose reset. */
internal fun validateFtcAutonomousBounds(
    entry: AutonomousCatalogEntry,
    routines: Map<String, RoutineDocument>,
    envelope: FtcFieldEnvelope,
    selectedAlliance: com.areslib.state.Alliance,
): List<String> {
    val errors = mutableListOf<String>()
    val start = resolveFtcAutonomousPose(entry, selectedAlliance)
    if (!isFtcRobotPoseWithinField(start, envelope)) {
        errors += "starting pose leaves the FTC field with the configured robot footprint"
    }
    val visited = mutableSetOf<String>()
    fun visitRoutine(routineId: String) {
        if (!visited.add(routineId)) return
        val routine = routines[routineId]
        if (routine == null) {
            errors += "routine '$routineId' does not exist"
            return
        }
        fun visitStep(step: com.areslib.routine.RoutineStep, path: String) {
            step.drive?.target?.let { target ->
                val selectedTarget = resolveFtcAutonomousPose(entry, selectedAlliance, target)
                if (!isFtcRobotPoseWithinField(selectedTarget, envelope)) {
                    errors += "$path drive target leaves the FTC field"
                }
            }
            step.routineId?.let(::visitRoutine)
            step.deadline?.let { visitStep(it, "$path.deadline") }
            step.children.forEachIndexed { index, child -> visitStep(child, "$path.children[$index]") }
            step.elseChildren.forEachIndexed { index, child -> visitStep(child, "$path.elseChildren[$index]") }
        }
        routine.steps.forEachIndexed { index, step -> visitStep(step, "steps[$index]") }
    }
    visitRoutine(entry.routineId)
    return errors.distinct()
}
