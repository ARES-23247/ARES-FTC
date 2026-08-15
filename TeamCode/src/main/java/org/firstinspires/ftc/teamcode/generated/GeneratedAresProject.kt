@file:Suppress("MagicNumber", "LongMethod")

package org.firstinspires.ftc.teamcode.generated

import com.areslib.codegen.CapabilityArgumentReader
import com.areslib.routine.AutonomousCatalogEntry
import com.areslib.routine.RoutineDocument
import com.areslib.routine.RoutineDriveMarker
import com.areslib.routine.RoutineDriveStep
import com.areslib.routine.RoutinePose
import com.areslib.routine.RoutineRuntimeBindings
import com.areslib.routine.RoutineStep
import com.areslib.routine.RoutineStepKind
import com.areslib.routine.RoutineManager
import com.areslib.input.ControllerBindingRuntime
import com.areslib.sequencer.Task
import com.areslib.state.RobotState

/** Stable robot boundary for capabilities referenced by generated project documents. */
interface GeneratedAresProjectCapabilities {
    /** Creates a hand-authored or season action by its catalog key, or null when unavailable. */
    fun createActionTask(actionKey: String, arguments: Map<String, String>): Task? = null

    /** Creates a hand-authored condition predicate by its catalog key, or null when unavailable. */
    fun createCondition(conditionKey: String, arguments: Map<String, String>): ((RobotState) -> Boolean)? = null

    /** Platform trajectory adapter; returning null rejects a drive step safely. */
    fun createDriveTask(step: RoutineDriveStep): Task? = null
}

/** Robot scheduler boundary used by generated direct-action controller bindings. */
fun interface GeneratedAresProjectControlTaskSink {
    fun submit(bindingId: String, task: Task)
}

/** Generated from the project's checked-in ARES documents. Do not edit by hand. */
object GeneratedAresProject {
    const val GENERATOR_VERSION: Int = 7
    const val CATALOG_SHA256: String = "754f20f33b603a25bea1ed471a2132814f119bb37c63dbf0f496f40c40bdee6a"
    const val CONTENT_SHA256: String = "6835a1b0948a49f065c66458e4bbe6ef5df119b1cb9c7bcec4150ff973cdd108"
    const val SOURCE_SHA256: String = "45e4111293cbf537956064bd11785bd49f6db7e2a3560053ffd6cdec5a1e4ca2"

    const val PROJECT_ID: String = "team23247-gobilda"
    const val PROJECT_LEAGUE: String = "FTC"
    const val COORDINATE_CONVENTION: String = "CENTER_ORIGIN_CCW"
    const val ROBOT_LENGTH_METERS: Double = 0.4572
    const val ROBOT_WIDTH_METERS: Double = 0.4572
    const val FIELD_LENGTH_METERS: Double = 3.6576
    const val FIELD_WIDTH_METERS: Double = 3.6576

    val knownActionKeys: Set<String> = setOf("SetIndicatorColor_BLUE", "SetIndicatorColor_CYAN", "SetIndicatorColor_GREEN", "SetIndicatorColor_OFF", "SetIndicatorColor_ORANGE", "SetIndicatorColor_PURPLE", "SetIndicatorColor_RAINBOW", "SetIndicatorColor_RED", "SetIndicatorColor_VIOLET", "SetIndicatorColor_WHITE", "SetIndicatorColor_YELLOW", "SetPrismPreset_EMERGENCY_LIGHTS", "SetPrismPreset_FTC_TIMER", "SetPrismPreset_RAINBOW_FULL_COLOR", "SetPrismPreset_SOLID_BLUE", "SetPrismPreset_SOLID_CYAN", "SetPrismPreset_SOLID_GREEN", "SetPrismPreset_SOLID_OFF", "SetPrismPreset_SOLID_ORANGE", "SetPrismPreset_SOLID_PURPLE", "SetPrismPreset_SOLID_RED", "SetPrismPreset_SOLID_WHITE", "SetPrismPreset_SOLID_YELLOW", "SetSecondIndicatorColor_BLUE", "SetSecondIndicatorColor_CYAN", "SetSecondIndicatorColor_GREEN", "SetSecondIndicatorColor_OFF", "SetSecondIndicatorColor_ORANGE", "SetSecondIndicatorColor_PURPLE", "SetSecondIndicatorColor_RAINBOW", "SetSecondIndicatorColor_RED", "SetSecondIndicatorColor_VIOLET", "SetSecondIndicatorColor_WHITE", "SetSecondIndicatorColor_YELLOW", "flywheel.prepare", "flywheel.stop", "intake.collect", "intake.stop")
    val knownConditionKeys: Set<String> = emptySet()

    val routines: Map<String, RoutineDocument> = linkedMapOf(
        "test-auto" to RoutineDocument(
            schemaVersion = 2,
            documentId = "test-auto",
            revision = 1,
            parentContentHash = null,
            name = "Test Auto",
            description = "Small mechanism-only routine used to verify autonomous execution.",
            steps = listOf(
                RoutineStep(
                    kind = RoutineStepKind.ACTION,
                    stepId = "step-stop-intake",
                    actionKey = "intake.stop",
                ),
                RoutineStep(
                    kind = RoutineStepKind.WAIT,
                    stepId = "step-settle",
                    durationSeconds = 0.25,
                ),
            ),
        ),
        "test-path" to RoutineDocument(
            schemaVersion = 2,
            documentId = "test-path",
            revision = 1,
            parentContentHash = null,
            name = "Test Path",
            description = "Short safe drive used by simulation and novice onboarding.",
            steps = listOf(
                RoutineStep(
                    kind = RoutineStepKind.DRIVE_TO,
                    stepId = "step-drive-forward",
                    drive = RoutineDriveStep(
                        target = RoutinePose(
                            xMeters = 0.5,
                            yMeters = 0.0,
                            headingRadians = 0.0,
                        ),
                        motionPresetKey = "safe",
                        preferredEngineKey = null,
                    ),
                ),
            ),
        ),
    )

    val autonomousEntries: List<AutonomousCatalogEntry> = listOf(
        AutonomousCatalogEntry(
            entryId = "test-path",
            displayName = "Test Path",
            description = "Drive forward half a meter.",
            routineId = "test-path",
            startingPose = RoutinePose(
                xMeters = 0.0,
                yMeters = 0.0,
                headingRadians = 0.0,
            ),
            authoredAlliance = com.areslib.routine.RoutineAlliance.RED,
            mirrorForOppositeAlliance = true,
            sortOrder = 0,
            enabled = true,
        ),
        AutonomousCatalogEntry(
            entryId = "test-auto",
            displayName = "Test Auto",
            description = "Verify the mechanism action runner.",
            routineId = "test-auto",
            startingPose = RoutinePose(
                xMeters = 0.0,
                yMeters = 0.0,
                headingRadians = 0.0,
            ),
            authoredAlliance = com.areslib.routine.RoutineAlliance.RED,
            mirrorForOppositeAlliance = true,
            sortOrder = 1,
            enabled = true,
        ),
    )
    val DEFAULT_AUTONOMOUS_ENTRY_ID: String? = "test-path"

    fun runtimeBindings(registry: GeneratedAresProjectCapabilities): RoutineRuntimeBindings =
        RoutineRuntimeBindings(
            createActionTask = { key, arguments ->
                when (key) {
                    "SetIndicatorColor_BLUE" -> {
                        CapabilityArgumentReader(
                            capabilityKey = "SetIndicatorColor_BLUE",
                            arguments = arguments,
                            allowedKeys = emptySet(),
                        )
                        registry.createActionTask(key, arguments)
                    }
                    "SetIndicatorColor_CYAN" -> {
                        CapabilityArgumentReader(
                            capabilityKey = "SetIndicatorColor_CYAN",
                            arguments = arguments,
                            allowedKeys = emptySet(),
                        )
                        registry.createActionTask(key, arguments)
                    }
                    "SetIndicatorColor_GREEN" -> {
                        CapabilityArgumentReader(
                            capabilityKey = "SetIndicatorColor_GREEN",
                            arguments = arguments,
                            allowedKeys = emptySet(),
                        )
                        registry.createActionTask(key, arguments)
                    }
                    "SetIndicatorColor_OFF" -> {
                        CapabilityArgumentReader(
                            capabilityKey = "SetIndicatorColor_OFF",
                            arguments = arguments,
                            allowedKeys = emptySet(),
                        )
                        registry.createActionTask(key, arguments)
                    }
                    "SetIndicatorColor_ORANGE" -> {
                        CapabilityArgumentReader(
                            capabilityKey = "SetIndicatorColor_ORANGE",
                            arguments = arguments,
                            allowedKeys = emptySet(),
                        )
                        registry.createActionTask(key, arguments)
                    }
                    "SetIndicatorColor_PURPLE" -> {
                        CapabilityArgumentReader(
                            capabilityKey = "SetIndicatorColor_PURPLE",
                            arguments = arguments,
                            allowedKeys = emptySet(),
                        )
                        registry.createActionTask(key, arguments)
                    }
                    "SetIndicatorColor_RAINBOW" -> {
                        CapabilityArgumentReader(
                            capabilityKey = "SetIndicatorColor_RAINBOW",
                            arguments = arguments,
                            allowedKeys = emptySet(),
                        )
                        registry.createActionTask(key, arguments)
                    }
                    "SetIndicatorColor_RED" -> {
                        CapabilityArgumentReader(
                            capabilityKey = "SetIndicatorColor_RED",
                            arguments = arguments,
                            allowedKeys = emptySet(),
                        )
                        registry.createActionTask(key, arguments)
                    }
                    "SetIndicatorColor_VIOLET" -> {
                        CapabilityArgumentReader(
                            capabilityKey = "SetIndicatorColor_VIOLET",
                            arguments = arguments,
                            allowedKeys = emptySet(),
                        )
                        registry.createActionTask(key, arguments)
                    }
                    "SetIndicatorColor_WHITE" -> {
                        CapabilityArgumentReader(
                            capabilityKey = "SetIndicatorColor_WHITE",
                            arguments = arguments,
                            allowedKeys = emptySet(),
                        )
                        registry.createActionTask(key, arguments)
                    }
                    "SetIndicatorColor_YELLOW" -> {
                        CapabilityArgumentReader(
                            capabilityKey = "SetIndicatorColor_YELLOW",
                            arguments = arguments,
                            allowedKeys = emptySet(),
                        )
                        registry.createActionTask(key, arguments)
                    }
                    "SetPrismPreset_EMERGENCY_LIGHTS" -> {
                        CapabilityArgumentReader(
                            capabilityKey = "SetPrismPreset_EMERGENCY_LIGHTS",
                            arguments = arguments,
                            allowedKeys = emptySet(),
                        )
                        registry.createActionTask(key, arguments)
                    }
                    "SetPrismPreset_FTC_TIMER" -> {
                        CapabilityArgumentReader(
                            capabilityKey = "SetPrismPreset_FTC_TIMER",
                            arguments = arguments,
                            allowedKeys = emptySet(),
                        )
                        registry.createActionTask(key, arguments)
                    }
                    "SetPrismPreset_RAINBOW_FULL_COLOR" -> {
                        CapabilityArgumentReader(
                            capabilityKey = "SetPrismPreset_RAINBOW_FULL_COLOR",
                            arguments = arguments,
                            allowedKeys = emptySet(),
                        )
                        registry.createActionTask(key, arguments)
                    }
                    "SetPrismPreset_SOLID_BLUE" -> {
                        CapabilityArgumentReader(
                            capabilityKey = "SetPrismPreset_SOLID_BLUE",
                            arguments = arguments,
                            allowedKeys = emptySet(),
                        )
                        registry.createActionTask(key, arguments)
                    }
                    "SetPrismPreset_SOLID_CYAN" -> {
                        CapabilityArgumentReader(
                            capabilityKey = "SetPrismPreset_SOLID_CYAN",
                            arguments = arguments,
                            allowedKeys = emptySet(),
                        )
                        registry.createActionTask(key, arguments)
                    }
                    "SetPrismPreset_SOLID_GREEN" -> {
                        CapabilityArgumentReader(
                            capabilityKey = "SetPrismPreset_SOLID_GREEN",
                            arguments = arguments,
                            allowedKeys = emptySet(),
                        )
                        registry.createActionTask(key, arguments)
                    }
                    "SetPrismPreset_SOLID_OFF" -> {
                        CapabilityArgumentReader(
                            capabilityKey = "SetPrismPreset_SOLID_OFF",
                            arguments = arguments,
                            allowedKeys = emptySet(),
                        )
                        registry.createActionTask(key, arguments)
                    }
                    "SetPrismPreset_SOLID_ORANGE" -> {
                        CapabilityArgumentReader(
                            capabilityKey = "SetPrismPreset_SOLID_ORANGE",
                            arguments = arguments,
                            allowedKeys = emptySet(),
                        )
                        registry.createActionTask(key, arguments)
                    }
                    "SetPrismPreset_SOLID_PURPLE" -> {
                        CapabilityArgumentReader(
                            capabilityKey = "SetPrismPreset_SOLID_PURPLE",
                            arguments = arguments,
                            allowedKeys = emptySet(),
                        )
                        registry.createActionTask(key, arguments)
                    }
                    "SetPrismPreset_SOLID_RED" -> {
                        CapabilityArgumentReader(
                            capabilityKey = "SetPrismPreset_SOLID_RED",
                            arguments = arguments,
                            allowedKeys = emptySet(),
                        )
                        registry.createActionTask(key, arguments)
                    }
                    "SetPrismPreset_SOLID_WHITE" -> {
                        CapabilityArgumentReader(
                            capabilityKey = "SetPrismPreset_SOLID_WHITE",
                            arguments = arguments,
                            allowedKeys = emptySet(),
                        )
                        registry.createActionTask(key, arguments)
                    }
                    "SetPrismPreset_SOLID_YELLOW" -> {
                        CapabilityArgumentReader(
                            capabilityKey = "SetPrismPreset_SOLID_YELLOW",
                            arguments = arguments,
                            allowedKeys = emptySet(),
                        )
                        registry.createActionTask(key, arguments)
                    }
                    "SetSecondIndicatorColor_BLUE" -> {
                        CapabilityArgumentReader(
                            capabilityKey = "SetSecondIndicatorColor_BLUE",
                            arguments = arguments,
                            allowedKeys = emptySet(),
                        )
                        registry.createActionTask(key, arguments)
                    }
                    "SetSecondIndicatorColor_CYAN" -> {
                        CapabilityArgumentReader(
                            capabilityKey = "SetSecondIndicatorColor_CYAN",
                            arguments = arguments,
                            allowedKeys = emptySet(),
                        )
                        registry.createActionTask(key, arguments)
                    }
                    "SetSecondIndicatorColor_GREEN" -> {
                        CapabilityArgumentReader(
                            capabilityKey = "SetSecondIndicatorColor_GREEN",
                            arguments = arguments,
                            allowedKeys = emptySet(),
                        )
                        registry.createActionTask(key, arguments)
                    }
                    "SetSecondIndicatorColor_OFF" -> {
                        CapabilityArgumentReader(
                            capabilityKey = "SetSecondIndicatorColor_OFF",
                            arguments = arguments,
                            allowedKeys = emptySet(),
                        )
                        registry.createActionTask(key, arguments)
                    }
                    "SetSecondIndicatorColor_ORANGE" -> {
                        CapabilityArgumentReader(
                            capabilityKey = "SetSecondIndicatorColor_ORANGE",
                            arguments = arguments,
                            allowedKeys = emptySet(),
                        )
                        registry.createActionTask(key, arguments)
                    }
                    "SetSecondIndicatorColor_PURPLE" -> {
                        CapabilityArgumentReader(
                            capabilityKey = "SetSecondIndicatorColor_PURPLE",
                            arguments = arguments,
                            allowedKeys = emptySet(),
                        )
                        registry.createActionTask(key, arguments)
                    }
                    "SetSecondIndicatorColor_RAINBOW" -> {
                        CapabilityArgumentReader(
                            capabilityKey = "SetSecondIndicatorColor_RAINBOW",
                            arguments = arguments,
                            allowedKeys = emptySet(),
                        )
                        registry.createActionTask(key, arguments)
                    }
                    "SetSecondIndicatorColor_RED" -> {
                        CapabilityArgumentReader(
                            capabilityKey = "SetSecondIndicatorColor_RED",
                            arguments = arguments,
                            allowedKeys = emptySet(),
                        )
                        registry.createActionTask(key, arguments)
                    }
                    "SetSecondIndicatorColor_VIOLET" -> {
                        CapabilityArgumentReader(
                            capabilityKey = "SetSecondIndicatorColor_VIOLET",
                            arguments = arguments,
                            allowedKeys = emptySet(),
                        )
                        registry.createActionTask(key, arguments)
                    }
                    "SetSecondIndicatorColor_WHITE" -> {
                        CapabilityArgumentReader(
                            capabilityKey = "SetSecondIndicatorColor_WHITE",
                            arguments = arguments,
                            allowedKeys = emptySet(),
                        )
                        registry.createActionTask(key, arguments)
                    }
                    "SetSecondIndicatorColor_YELLOW" -> {
                        CapabilityArgumentReader(
                            capabilityKey = "SetSecondIndicatorColor_YELLOW",
                            arguments = arguments,
                            allowedKeys = emptySet(),
                        )
                        registry.createActionTask(key, arguments)
                    }
                    "flywheel.prepare" -> {
                        CapabilityArgumentReader(
                            capabilityKey = "flywheel.prepare",
                            arguments = arguments,
                            allowedKeys = emptySet(),
                        )
                        registry.createActionTask(key, arguments)
                    }
                    "flywheel.stop" -> {
                        CapabilityArgumentReader(
                            capabilityKey = "flywheel.stop",
                            arguments = arguments,
                            allowedKeys = emptySet(),
                        )
                        registry.createActionTask(key, arguments)
                    }
                    "intake.collect" -> {
                        CapabilityArgumentReader(
                            capabilityKey = "intake.collect",
                            arguments = arguments,
                            allowedKeys = emptySet(),
                        )
                        registry.createActionTask(key, arguments)
                    }
                    "intake.stop" -> {
                        CapabilityArgumentReader(
                            capabilityKey = "intake.stop",
                            arguments = arguments,
                            allowedKeys = emptySet(),
                        )
                        registry.createActionTask(key, arguments)
                    }
                    else -> null
                }
            },
            createCondition = { _, _ -> null },
            createDriveTask = registry::createDriveTask,
            isActionKnown = knownActionKeys::contains,
            isConditionKnown = knownConditionKeys::contains,
            resourcesForAction = { key ->
                when (key) {
                    "SetIndicatorColor_BLUE" -> setOf("indicator.primary")
                    "SetIndicatorColor_CYAN" -> setOf("indicator.primary")
                    "SetIndicatorColor_GREEN" -> setOf("indicator.primary")
                    "SetIndicatorColor_OFF" -> setOf("indicator.primary")
                    "SetIndicatorColor_ORANGE" -> setOf("indicator.primary")
                    "SetIndicatorColor_PURPLE" -> setOf("indicator.primary")
                    "SetIndicatorColor_RAINBOW" -> setOf("indicator.primary")
                    "SetIndicatorColor_RED" -> setOf("indicator.primary")
                    "SetIndicatorColor_VIOLET" -> setOf("indicator.primary")
                    "SetIndicatorColor_WHITE" -> setOf("indicator.primary")
                    "SetIndicatorColor_YELLOW" -> setOf("indicator.primary")
                    "SetPrismPreset_EMERGENCY_LIGHTS" -> setOf("prism")
                    "SetPrismPreset_FTC_TIMER" -> setOf("prism")
                    "SetPrismPreset_RAINBOW_FULL_COLOR" -> setOf("prism")
                    "SetPrismPreset_SOLID_BLUE" -> setOf("prism")
                    "SetPrismPreset_SOLID_CYAN" -> setOf("prism")
                    "SetPrismPreset_SOLID_GREEN" -> setOf("prism")
                    "SetPrismPreset_SOLID_OFF" -> setOf("prism")
                    "SetPrismPreset_SOLID_ORANGE" -> setOf("prism")
                    "SetPrismPreset_SOLID_PURPLE" -> setOf("prism")
                    "SetPrismPreset_SOLID_RED" -> setOf("prism")
                    "SetPrismPreset_SOLID_WHITE" -> setOf("prism")
                    "SetPrismPreset_SOLID_YELLOW" -> setOf("prism")
                    "SetSecondIndicatorColor_BLUE" -> setOf("indicator.secondary")
                    "SetSecondIndicatorColor_CYAN" -> setOf("indicator.secondary")
                    "SetSecondIndicatorColor_GREEN" -> setOf("indicator.secondary")
                    "SetSecondIndicatorColor_OFF" -> setOf("indicator.secondary")
                    "SetSecondIndicatorColor_ORANGE" -> setOf("indicator.secondary")
                    "SetSecondIndicatorColor_PURPLE" -> setOf("indicator.secondary")
                    "SetSecondIndicatorColor_RAINBOW" -> setOf("indicator.secondary")
                    "SetSecondIndicatorColor_RED" -> setOf("indicator.secondary")
                    "SetSecondIndicatorColor_VIOLET" -> setOf("indicator.secondary")
                    "SetSecondIndicatorColor_WHITE" -> setOf("indicator.secondary")
                    "SetSecondIndicatorColor_YELLOW" -> setOf("indicator.secondary")
                    "flywheel.prepare" -> setOf("flywheel", "intake")
                    "flywheel.stop" -> setOf("flywheel")
                    "intake.collect" -> setOf("flywheel", "intake")
                    "intake.stop" -> setOf("intake")
                    else -> emptySet()
                }
            },
        )

    val knownControlSchemeIds: Set<String> = emptySet()
    val DEFAULT_CONTROL_SCHEME_ID: String? = null

    /**
     * Builds one allocation-free update runtime per zero-based Driver Station port. Suppressing chords are
     * ordered before constituent buttons and raise their effective press debounce to the chord
     * window, preventing a near-simultaneous chord from leaking a single-button action.
     */
    @Suppress("UNUSED_PARAMETER")
    fun createControllerRuntimes(
        schemeId: String?,
        registry: GeneratedAresProjectCapabilities,
        routineManager: RoutineManager,
        taskSink: GeneratedAresProjectControlTaskSink,
    ): Map<Int, ControllerBindingRuntime> {
        require(schemeId == null) { "This project has no generated control scheme" }
        return emptyMap()
    }
}
