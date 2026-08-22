package org.firstinspires.ftc.teamcode.dsl

import com.areslib.action.RobotAction
import com.areslib.ftc.FtcMecanumRobot
import com.areslib.ftc.photon.AresFtcRuntimeOptions
import com.areslib.ftc.photon.AresFtcRuntimeOptionsProvider
import com.areslib.routine.RoutineRequestResult
import com.areslib.routine.RoutineStartPolicy
import com.areslib.state.Alliance
import com.areslib.state.RoutineExecutionStatus
import com.areslib.telemetry.RobotStatusTracker
import com.areslib.util.PoseStorage
import com.areslib.util.RobotClock
import com.qualcomm.robotcore.eventloop.opmode.OpMode
import org.firstinspires.ftc.teamcode.generated.GeneratedAresProject
import org.firstinspires.ftc.teamcode.config.AresRuntimePolicy
import org.firstinspires.ftc.teamcode.opmodes.AresRobot

/**
 * Generated-project FTC autonomous lifecycle with an offline, INIT-time Driver Station selector.
 *
 * The checked-in [GeneratedAresProject] is the only autonomous source at runtime: no robot or
 * network connection is needed while authoring, and a stale generated file is rejected by the
 * Gradle verification task before the APK is built. During INIT, D-pad left/right selects a
 * routine and X toggles alliance. START seeds the selected catalog pose, invokes its neutral
 * routine through the shared `RoutineManager`, and enforces fail-closed cancellation on timeout,
 * error, stop, or mode transition.
 *
 * FTC hub transport is selected by the reviewed generated project policy before initialization.
 */
abstract class AresAutoBase : OpMode(), AresFtcRuntimeOptionsProvider {
    final override val aresFtcRuntimeOptions: AresFtcRuntimeOptions
        get() = AresRuntimePolicy.options

    private companion object {
        const val DEFAULT_MAXIMUM_RUNTIME_SECONDS = 29.5
        const val OVERRUN_THRESHOLD_MS = 30L
        const val LOOP_TELEMETRY_PERIOD_MS = 100L
    }

    /** Optional fixed entry used by a narrow validation OpMode; null enables driver selection. */
    protected open val lockedAutonomousEntryId: String? = null

    /** Optional fixed alliance used by a narrow validation OpMode; null enables INIT toggling. */
    protected open val lockedAutonomousAlliance: Alliance? = null

    /** Competition-safe hard deadline. FTC autonomous cannot exceed 30 seconds. */
    protected open val maximumRuntimeSeconds: Double = DEFAULT_MAXIMUM_RUNTIME_SECONDS

    private var robot: AresRobot? = null
    private var generatedRuntime: FtcGeneratedProjectRuntime? = null
    private lateinit var selector: FtcAutonomousSelector
    private var configurationError: String? = null
    private var hardwareError: String? = null
    private var deadlineMs = 0L
    private var started = false
    private var finished = false
    private var poseIsUsable = true
    private var closed = false
    private var loopCount = 0L
    private var overrunCount = 0L
    private var lastDashboardRequest: String? = null
    private var lastPublishedSelection: String? = null
    private var lastPublishedStatus: String? = null
    private var activeExecutionId: Long? = null
    private var successfulCompletion = false
    private var lastLoopTelemetryMs = 0L

    /** Constructs the season facade. Kept overridable for focused simulator tests. */
    protected open fun buildRobot(): AresRobot = AresRobot(hardwareMap, telemetry)

    /** Typed test seam for the shared mecanum safety boundary. */
    protected open fun getMecanumRobot(robot: AresRobot): FtcMecanumRobot = robot.base

    /** Builds hardware and the generated routine registry before the mode can be armed. */
    final override fun init() {
        require(maximumRuntimeSeconds.isFinite() && maximumRuntimeSeconds > 0.0 && maximumRuntimeSeconds <= 30.0) {
            "Autonomous maximum runtime must be finite and in (0, 30] seconds"
        }
        val builtRobot = buildRobot()
        robot = builtRobot
        builtRobot.base.mecanumIO.kS = builtRobot.base.driveFeedforward.kS.takeIf { it > 0.0 } ?: 0.05
        selector = FtcAutonomousSelector(
            entries = GeneratedAresProject.autonomousEntries,
            defaultEntryId = GeneratedAresProject.DEFAULT_AUTONOMOUS_ENTRY_ID,
            initialAlliance = lockedAutonomousAlliance ?: Alliance.RED,
            lockedEntryId = lockedAutonomousEntryId,
            lockedAlliance = lockedAutonomousAlliance,
        )
        configurationError = runCatching {
            rebuildSelectedConfiguration(builtRobot, "Autonomous initialized")
            generatedRuntime?.routineManager?.validateProject()
                ?.firstOrNull { it.severity == com.areslib.routine.RoutineValidationSeverity.ERROR }
                ?.let { error(it.message) }
        }.exceptionOrNull()?.let { it.message ?: it::class.java.simpleName }
        publishNetworkCatalog(builtRobot)
        publishInitStatus()
    }

    /** Keeps hardware diagnostics and selection live while the Driver Station shows INIT. */
    final override fun init_loop() {
        val activeRobot = robot ?: return
        // The simulator's leased drive frame reaches Redux before this callback. During INIT an
        // unlocked autonomous treats that value as authoritative, then rebuilds both pose and
        // runtime for the same alliance. Locked validation modes instead reassert their lock.
        val externalAllianceChanged = synchronizeSelectorAlliance(activeRobot)
        val dashboardRequest = activeRobot.base.telemetryManager.nt4
            .getString("ARES/Input/selectedAuto", "")
            .trim()
        var selectionChanged = externalAllianceChanged
        if (dashboardRequest.isNotEmpty() && dashboardRequest != lastDashboardRequest) {
            lastDashboardRequest = dashboardRequest
            selectionChanged = selector.selectEntry(dashboardRequest) || selectionChanged
        }
        selectionChanged = selector.update(
            left = gamepad1.dpad_left,
            right = gamepad1.dpad_right,
            // A dashboard alliance transition wins over a simultaneous synthetic X edge.
            toggleAlliance = gamepad1.x && !externalAllianceChanged,
        ) || selectionChanged
        if (selectionChanged) {
            configurationError = runCatching {
                rebuildSelectedConfiguration(activeRobot, "Autonomous selection changed during INIT")
            }.exceptionOrNull()?.let { it.message ?: it::class.java.simpleName }
        }
        hardwareError = runCatching { activeRobot.update() }
            .exceptionOrNull()
            ?.let { failure ->
                val detail = failure.message ?: failure::class.java.simpleName
                if (activeRobot.fatalUpdateFailure != null) {
                    "Robot failure latched; restart the OpMode: $detail"
                } else {
                    "Robot initialization failed: $detail"
                }
            }
        publishInitStatus()
    }

    /** Starts the selected generated routine, or blocks and stops safely when preflight failed. */
    final override fun start() {
        started = true
        RobotStatusTracker.activeOpMode = "Auto"
        val activeRobot = robot ?: return
        val startSelectionError = runCatching {
            if (synchronizeSelectorAlliance(activeRobot)) {
                rebuildSelectedConfiguration(activeRobot, "Autonomous alliance changed before START")
                configurationError = null
            }
        }.exceptionOrNull()?.let { it.message ?: it::class.java.simpleName }
        if (startSelectionError != null) configurationError = startSelectionError
        val runtime = generatedRuntime
        val entry = selector.selected
        val finalBoundsError = runCatching { validateSelectedBounds(activeRobot) }
            .exceptionOrNull()
            ?.let { it.message ?: it::class.java.simpleName }
        if (finalBoundsError != null) configurationError = finalBoundsError
        val blockingError = configurationError ?: hardwareError
        if (entry == null || runtime == null || blockingError != null) {
            poseIsUsable = false
            val safetyFailure = runCatching { safeRobot(activeRobot) }.exceptionOrNull()
            val reportedError = listOfNotNull(
                blockingError ?: if (entry == null || runtime == null) {
                    "No enabled autonomous entry exists"
                } else null,
                safetyFailure?.let { "Safety stop failed: ${it.message ?: it::class.java.simpleName}" },
            ).joinToString("; ")
            if (safetyFailure != null) hardwareError = reportedError
            telemetry.addData("AUTO BLOCKED", reportedError)
            publishNetworkStatus("Blocked")
            telemetry.update()
            finished = true
            requestOpModeStop()
            return
        }

        activeRobot.base.store.dispatch(RobotAction.SetAlliance(selector.alliance))
        activeRobot.base.visionTracker.hasInitializedPoseWithVision = true
        when (val request = runtime.routineManager.request(entry.routineId, RoutineStartPolicy.RESTART_EXISTING)) {
            is RoutineRequestResult.Accepted -> {
                activeExecutionId = request.executionId
                deadlineMs = RobotClock.currentTimeMillis() + (maximumRuntimeSeconds * 1_000.0).toLong()
                publishNetworkStatus("Running")
            }
            is RoutineRequestResult.AlreadyRunning -> {
                activeExecutionId = request.executionId
                deadlineMs = RobotClock.currentTimeMillis() + (maximumRuntimeSeconds * 1_000.0).toLong()
                publishNetworkStatus("Running")
            }
            is RoutineRequestResult.Rejected -> {
                configurationError = request.issues.joinToString(separator = "; ") { it.message }
                poseIsUsable = false
                finishActiveRun("Generated routine was rejected")
            }
        }
    }

    /** Advances generated tasks, applies their Redux actions, then runs one hardware frame. */
    final override fun loop() {
        if (finished) return
        val activeRobot = robot ?: return
        val runtime = generatedRuntime ?: return
        // Alliance becomes immutable once START is accepted. Network input may continue to carry
        // a dashboard selection, but it cannot retarget a running generated routine out-of-band.
        if (activeRobot.base.store.state.drive.alliance != selector.alliance) {
            activeRobot.base.store.dispatch(RobotAction.SetAlliance(selector.alliance))
        }
        val loopStartMs = RobotClock.currentTimeMillis()
        if (loopStartMs >= deadlineMs) {
            poseIsUsable = false
            finishActiveRun("Runtime limit reached; outputs stopped")
            return
        }

        try {
            runtime.updateTasks()
            activeRobot.update()
            val executionId = activeExecutionId
                ?: throw IllegalStateException("Autonomous routine has no retained execution ID")
            val terminal = activeRobot.base.store.state.routineState.lastTerminalExecution
            when (classifyFtcAutoTerminal(executionId, terminal)) {
                FtcAutoTerminalDecision.COMPLETED -> {
                    successfulCompletion = true
                    finishActiveRun("Complete")
                }
                FtcAutoTerminalDecision.FAILED,
                FtcAutoTerminalDecision.CANCELLED -> {
                    poseIsUsable = false
                    configurationError = terminal?.message
                        ?: "Routine ${terminal?.status?.name?.lowercase() ?: "failed"}"
                    finishActiveRun("Aborted: $configurationError")
                }
                FtcAutoTerminalDecision.RUNNING -> Unit
            }
            if (finished) return
            if (runtime.routineManager.activeCount == 0 && runtime.routineManager.queuedCount == 0) {
                throw IllegalStateException("Routine ended without a matching terminal lifecycle event")
            }
        } catch (error: Throwable) {
            poseIsUsable = false
            configurationError = error.message ?: error::class.java.simpleName
            finishActiveRun("Aborted: ${configurationError}")
            return
        }

        val nowMs = RobotClock.currentTimeMillis()
        val elapsedMs = nowMs - loopStartMs
        loopCount++
        if (elapsedMs > OVERRUN_THRESHOLD_MS) overrunCount++
        if (nowMs - lastLoopTelemetryMs >= LOOP_TELEMETRY_PERIOD_MS) {
            lastLoopTelemetryMs = nowMs
            val pose = activeRobot.base.drive.odometryPose
            telemetry.addData("Pose X (m)", pose.x)
            telemetry.addData("Pose Y (m)", pose.y)
            telemetry.addData("Heading (deg)", Math.toDegrees(pose.heading.radians))
            telemetry.addData("Loop duration (ms)", elapsedMs)
            telemetry.addData("Loop overruns", overrunCount)
            telemetry.update()
        }
    }

    /** Cancels generated work, persists a usable final pose, and closes owned resources once. */
    final override fun stop() {
        if (closed) return
        closed = true
        val activeRobot = robot
        var firstFailure: Throwable? = null
        fun attempt(action: () -> Unit) {
            try {
                action()
            } catch (failure: Throwable) {
                val primary = firstFailure
                if (primary == null) firstFailure = failure
                else if (primary !== failure) primary.addSuppressed(failure)
            }
        }

        if (!finished) attempt { publishNetworkStatus("Stopped") }
        attempt { generatedRuntime?.cancelAll("FTC OpMode stopped") }
        if (activeRobot != null) attempt { safeRobot(activeRobot) }
        val finalPose = activeRobot?.base?.drive?.odometryPose
        attempt { activeRobot?.close() }
        attempt { com.areslib.ftc.photon.AresPhotonCore.disable() }

        generatedRuntime = null
        robot = null
        val cleanupFailure = firstFailure
        if (cleanupFailure != null) {
            poseIsUsable = false
            successfulCompletion = false
            configurationError = "Autonomous cleanup failed: ${cleanupFailure.message ?: cleanupFailure::class.java.simpleName}"
        }
        if (finalPose != null && cleanupFailure == null &&
            shouldPersistFtcAutoPose(started, successfulCompletion, poseIsUsable, configurationError)
        ) {
            PoseStorage.currentPose = finalPose
            PoseStorage.alliance = selector.alliance
            PoseStorage.hasValidPose = true
        } else {
            PoseStorage.hasValidPose = false
        }
        if (cleanupFailure != null) {
            runCatching {
                telemetry.addData("AUTO CLEANUP FAILED", configurationError)
                telemetry.update()
            }
            throw cleanupFailure
        }
    }

    private fun seedSelectedPose(activeRobot: AresRobot) {
        activeRobot.base.store.dispatch(RobotAction.SetAlliance(selector.alliance))
        val entry = selector.selected ?: return
        activeRobot.base.resetPose(
            resolveFtcAutonomousPose(entry, selector.alliance),
            resetHardware = true,
        )
    }

    private fun synchronizeSelectorAlliance(activeRobot: AresRobot): Boolean {
        val lockedAlliance = lockedAutonomousAlliance
        if (lockedAlliance != null) {
            if (activeRobot.base.store.state.drive.alliance != lockedAlliance) {
                activeRobot.base.store.dispatch(RobotAction.SetAlliance(lockedAlliance))
            }
            return false
        }
        return selector.selectAlliance(activeRobot.base.store.state.drive.alliance)
    }

    private fun rebuildSelectedConfiguration(activeRobot: AresRobot, cancellationReason: String) {
        generatedRuntime?.cancelAll(cancellationReason)
        validateSelectedBounds(activeRobot)
        seedSelectedPose(activeRobot)
        generatedRuntime = FtcGeneratedProjectRuntime(
            robot = activeRobot,
            autonomousEntry = selector.selected,
            selectedAlliance = selector.alliance,
        )
    }

    private fun validateSelectedBounds(activeRobot: AresRobot) {
        val entry = selector.selected ?: return
        val envelope = ftcFieldEnvelopeForRobot(activeRobot)
        val errors = validateFtcAutonomousBounds(
            entry = entry,
            routines = GeneratedAresProject.routines,
            envelope = envelope,
            selectedAlliance = selector.alliance,
        )
        require(errors.isEmpty()) { errors.joinToString(separator = "; ") }
    }

    private fun finishActiveRun(status: String) {
        if (finished) return
        finished = true
        var firstFailure: Throwable? = null
        fun attempt(action: () -> Unit) {
            try {
                action()
            } catch (failure: Throwable) {
                val primary = firstFailure
                if (primary == null) firstFailure = failure
                else if (primary !== failure) primary.addSuppressed(failure)
            }
        }
        attempt { generatedRuntime?.cancelAll(status) }
        robot?.let { activeRobot -> attempt { safeRobot(activeRobot) } }
        val finishFailure = firstFailure
        val reportedStatus = if (finishFailure == null) {
            status
        } else {
            successfulCompletion = false
            poseIsUsable = false
            configurationError = "Autonomous stop failed: ${finishFailure.message ?: finishFailure::class.java.simpleName}"
            "Aborted: $configurationError"
        }
        if (!successfulCompletion) PoseStorage.hasValidPose = false
        telemetry.addData("Auto", reportedStatus)
        publishNetworkStatus(
            when {
                successfulCompletion && finishFailure == null -> "Complete"
                started -> "Failed"
                else -> "Blocked"
            }
        )
        telemetry.update()
        requestOpModeStop()
    }

    /** Stops shared drive hardware and every registered season subsystem. */
    open fun safeRobot(activeRobot: AresRobot) {
        val baseRobot = getMecanumRobot(activeRobot)
        var firstFailure: Throwable? = null
        try {
            baseRobot.safeAll()
        } catch (failure: Throwable) {
            firstFailure = failure
        }
        try {
            baseRobot.safeHardware()
        } catch (failure: Throwable) {
            val primary = firstFailure
            if (primary == null) firstFailure = failure
            else if (primary !== failure) primary.addSuppressed(failure)
        }
        firstFailure?.let { throw it }
    }

    private fun publishInitStatus() {
        val entry = selector.selected
        telemetry.addData("Autonomous", entry?.displayName ?: "Safe do-nothing")
        telemetry.addData("Routine ID", entry?.routineId ?: "none")
        telemetry.addData("Alliance", selector.alliance)
        val error = configurationError ?: hardwareError
        if (error == null && entry != null) {
            telemetry.addData("Status", "READY - press START")
            if (lockedAutonomousEntryId == null) telemetry.addData("Select", "D-pad left/right")
            if (lockedAutonomousAlliance == null) telemetry.addData("Alliance control", "X toggles red/blue")
        } else {
            telemetry.addData("Status", "BLOCKED")
            telemetry.addData("Fix", error ?: "Enable at least one autonomous entry in the project")
        }
        publishNetworkStatus(if (error == null && entry != null) "Ready" else "Blocked")
        telemetry.update()
    }

    private fun publishNetworkCatalog(activeRobot: AresRobot) {
        val enabledIds = GeneratedAresProject.autonomousEntries
            .asSequence()
            .filter { it.enabled }
            .sortedWith(compareBy<com.areslib.routine.AutonomousCatalogEntry> { it.sortOrder }.thenBy { it.entryId })
            .joinToString(separator = ",") { it.entryId }
        val nt4 = activeRobot.base.telemetryManager.nt4
        nt4.putString("ARES/Auto/AvailableDocuments", enabledIds)
        nt4.putString("ARES/Auto/Source", "generated:${GeneratedAresProject.CONTENT_SHA256}")
        nt4.update()
    }

    private fun publishNetworkStatus(status: String) {
        val nt4 = robot?.base?.telemetryManager?.nt4 ?: return
        val selectedId = selector.selected?.entryId.orEmpty()
        if (selectedId == lastPublishedSelection && status == lastPublishedStatus) return
        nt4.putString("ARES/Auto/Selected", selectedId)
        nt4.putString("ARES/Auto/Status", status)
        nt4.update()
        lastPublishedSelection = selectedId
        lastPublishedStatus = status
    }
}

internal enum class FtcAutoTerminalDecision { RUNNING, COMPLETED, FAILED, CANCELLED }

/** Accepts terminal lifecycle evidence only for the exact retained routine execution. */
internal fun classifyFtcAutoTerminal(
    activeExecutionId: Long,
    terminal: com.areslib.state.RoutineExecutionState?,
): FtcAutoTerminalDecision {
    if (terminal?.executionId != activeExecutionId) return FtcAutoTerminalDecision.RUNNING
    return when (terminal.status) {
        RoutineExecutionStatus.COMPLETED -> FtcAutoTerminalDecision.COMPLETED
        RoutineExecutionStatus.FAILED -> FtcAutoTerminalDecision.FAILED
        RoutineExecutionStatus.CANCELLED -> FtcAutoTerminalDecision.CANCELLED
        else -> FtcAutoTerminalDecision.RUNNING
    }
}

/** A pose handoff is legal only after proven success; every abort/failure invalidates storage. */
internal fun shouldPersistFtcAutoPose(
    started: Boolean,
    successfulCompletion: Boolean,
    poseIsUsable: Boolean,
    configurationError: String?,
): Boolean = started && successfulCompletion && poseIsUsable && configurationError == null
