package org.firstinspires.ftc.teamcode.dsl

import com.areslib.action.RobotAction
import com.areslib.hardware.actuator.IndicatorLightColor
import com.areslib.hardware.actuator.PrismPwmPreset
import com.areslib.pathing.CommandKey
import com.areslib.pathing.NamedCommandDescriptor
import com.areslib.pathing.NamedCommands
import com.areslib.sequencer.StateActionTask
import com.areslib.sequencer.Task
import com.areslib.sequencer.TaskStateMachine
import com.areslib.state.RobotState

/**
 * FTC actions available to the visual auto editor and native auto compiler.
 *
 * Tasks derive their update from the state observed at execution time, preserving unrelated
 * mechanism fields and avoiding stale state captured during robot initialization.
 */
object FtcAutoCapabilities {
    val INTAKE_COLLECT = NamedCommandDescriptor(
        key = CommandKey("intake.collect"),
        displayName = "Collect game piece",
        description = "Starts the intake and safely stops the flywheel.",
        category = "Intake"
    )
    val INTAKE_STOP = NamedCommandDescriptor(
        key = CommandKey("intake.stop"),
        displayName = "Stop intake",
        description = "Stops the intake roller.",
        category = "Intake"
    )
    val FLYWHEEL_PREPARE = NamedCommandDescriptor(
        key = CommandKey("flywheel.prepare"),
        displayName = "Prepare flywheel",
        description = "Stops the intake and spins the flywheel to the configured match preset.",
        category = "Shooter"
    )
    val FLYWHEEL_STOP = NamedCommandDescriptor(
        key = CommandKey("flywheel.stop"),
        displayName = "Stop flywheel",
        description = "Stops closed-loop flywheel output and clears its speed target.",
        category = "Shooter"
    )
    val DRIVE_RECOVER_NEUTRAL = NamedCommandDescriptor(
        key = CommandKey("drivetrain.recoverNeutral"),
        displayName = "Recover drive after a fault",
        description = "Requires released drive controls, writes neutral to all four motors, then clears the drive fault latch.",
        category = "Drive safety"
    )

    private val primaryIndicatorDescriptors = IndicatorLightColor.entries.associateWith { color ->
        indicatorDescriptor(color, primary = true)
    }
    private val secondaryIndicatorDescriptors = IndicatorLightColor.entries.associateWith { color ->
        indicatorDescriptor(color, primary = false)
    }
    private val prismDescriptors by lazy {
        PRISM_PRESETS.associate { choice ->
            choice.preset to NamedCommandDescriptor(
                key = CommandKey("SetPrismPreset_${choice.preset.name}"),
                displayName = "Prism: ${choice.displayName}",
                description = choice.description,
                category = "Prism"
            )
        }
    }

    /** Registers only mechanism actions backed by hardware discovered for this robot instance. */
    fun registerMechanismActions(intakeAvailable: Boolean, flywheelAvailable: Boolean) {
        if (intakeAvailable) {
            registerStateAction(
                INTAKE_COLLECT
            ) { state ->
                RobotAction.UpdateSubsystemState(
                    state.superstructure.season.copy(
                        intakeActive = true,
                        flywheelActive = false,
                        flywheelTargetRPM = 0.0
                    )
                )
            }
            registerStateAction(
                INTAKE_STOP
            ) { state ->
                RobotAction.UpdateSubsystemState(state.superstructure.season.copy(intakeActive = false))
            }
        }

        if (flywheelAvailable) {
            registerStateAction(
                FLYWHEEL_PREPARE
            ) { state ->
                val targetRpm = state.tuning.subsystem.ftc.flywheelTargetRpmPreset
                    .takeIf(Double::isFinite)
                    ?.coerceIn(0.0, MAX_FLYWHEEL_RPM)
                    ?: 0.0
                RobotAction.UpdateSubsystemState(
                    state.superstructure.season.copy(
                        intakeActive = false,
                        flywheelActive = targetRpm > 0.0,
                        flywheelTargetRPM = targetRpm
                    )
                )
            }
            registerStateAction(
                FLYWHEEL_STOP
            ) { state ->
                RobotAction.UpdateSubsystemState(
                    state.superstructure.season.copy(
                        flywheelActive = false,
                        flywheelTargetRPM = 0.0
                    )
                )
            }
        }
    }

    /** Registers the explicit, neutral-first recovery required by the generated drive safety contract. */
    fun registerDriveRecovery(recoverWithNeutral: () -> Boolean) {
        NamedCommands.register(DRIVE_RECOVER_NEUTRAL) {
            object : Task {
                override val name: String = DRIVE_RECOVER_NEUTRAL.displayName
                private var recovered = false

                override fun initialize(state: RobotState): List<RobotAction> {
                    super.initialize(state)
                    recovered = recoverWithNeutral()
                    if (!recovered) TaskStateMachine.markFailed(this)
                    return emptyList()
                }

                override fun isCompleted(state: RobotState, elapsedMs: Long): Boolean = recovered

                override fun releaseRuntimeState() {
                    recovered = false
                    super.releaseRuntimeState()
                }
            }
        }
    }

    /**
     * Registers indicator actions after optional hardware discovery is complete.
     *
     * Missing lights are absent from the live capability registry. A routine that requires one is
     * rejected instead of falsely completing a no-op action.
     */
    fun registerIndicatorActions(primaryAvailable: Boolean, secondaryAvailable: Boolean) {
        IndicatorLightColor.entries.forEach { color ->
            registerIndicator(
                descriptor = requireNotNull(primaryIndicatorDescriptors[color]),
                hardwareName = "indicator",
                available = primaryAvailable,
                color = color
            )
            registerIndicator(
                descriptor = requireNotNull(secondaryIndicatorDescriptors[color]),
                hardwareName = "indicator2",
                available = secondaryAvailable,
                color = color
            )
        }
    }

    /**
     * Registers a deliberately small, novice-friendly Prism effect catalog.
     *
     * The shared Prism driver supports many specialized pulse-width presets. Controller and
     * autonomous menus expose the common match-safe choices here instead of presenting students
     * with a long list of raw microsecond values. Registration remains hardware-gated: an action
     * is unavailable when the optional `prism` device was not discovered during robot init.
     */
    fun registerPrismActions(prismAvailable: Boolean) {
        if (!prismAvailable) return
        PRISM_PRESETS.forEach { choice ->
            val descriptor = requireNotNull(prismDescriptors[choice.preset])
            NamedCommands.register(descriptor) {
                object : Task {
                    override val name: String = descriptor.displayName

                    override fun initialize(state: RobotState): List<RobotAction> =
                        listOf(RobotAction.SetPrismDriver("prism", choice.preset.pulseWidthUs))

                    override fun isCompleted(state: RobotState, elapsedMs: Long): Boolean = true
                }
            }
        }
    }

    private fun registerStateAction(
        descriptor: NamedCommandDescriptor,
        actionFactory: (com.areslib.state.RobotState) -> RobotAction
    ) {
        NamedCommands.register(descriptor) {
            StateActionTask(descriptor.displayName, actionFactory)
        }
    }

    private fun registerIndicator(
        descriptor: NamedCommandDescriptor,
        hardwareName: String,
        available: Boolean,
        color: IndicatorLightColor
    ) {
        if (!available) return
        NamedCommands.register(descriptor) {
            object : Task {
                override val name: String = descriptor.displayName
                override fun initialize(state: RobotState): List<RobotAction> =
                    listOf(RobotAction.SetIndicatorLight(hardwareName, color.position))

                override fun isCompleted(state: RobotState, elapsedMs: Long): Boolean = true
            }
        }
    }

    private fun indicatorDescriptor(
        color: IndicatorLightColor,
        primary: Boolean
    ): NamedCommandDescriptor {
        val target = if (primary) "Primary" else "Secondary"
        val keyPrefix = if (primary) "SetIndicatorColor" else "SetSecondIndicatorColor"
        val colorName = color.name.lowercase().replaceFirstChar(Char::uppercase)
        val description = if (color == IndicatorLightColor.OFF) {
            "Turns off the ${target.lowercase()} indicator light."
        } else {
            "Sets the ${target.lowercase()} indicator light to ${color.name.lowercase()}."
        }
        return NamedCommandDescriptor(
            key = CommandKey("${keyPrefix}_${color.name}"),
            displayName = "$target light: $colorName",
            description = description,
            category = "$target indicator"
        )
    }

    private const val MAX_FLYWHEEL_RPM = 6000.0

    private data class PrismPresetChoice(
        val preset: PrismPwmPreset,
        val displayName: String,
        val description: String,
    )

    private val PRISM_PRESETS = listOf(
        PrismPresetChoice(PrismPwmPreset.SOLID_OFF, "Off", "Turns the goBILDA Prism LEDs off."),
        PrismPresetChoice(
            PrismPwmPreset.RAINBOW_FULL_COLOR,
            "Full rainbow",
            "Shows a full-color rainbow animation on the goBILDA Prism."
        ),
        PrismPresetChoice(
            PrismPwmPreset.FTC_TIMER,
            "FTC timer",
            "Shows the Prism's built-in FTC match timer animation."
        ),
        PrismPresetChoice(
            PrismPwmPreset.EMERGENCY_LIGHTS,
            "Emergency lights",
            "Shows the Prism's alternating emergency-lights animation."
        ),
        PrismPresetChoice(PrismPwmPreset.SOLID_RED, "Solid red", "Sets the Prism to solid red."),
        PrismPresetChoice(PrismPwmPreset.SOLID_ORANGE, "Solid orange", "Sets the Prism to solid orange."),
        PrismPresetChoice(PrismPwmPreset.SOLID_YELLOW, "Solid yellow", "Sets the Prism to solid yellow."),
        PrismPresetChoice(PrismPwmPreset.SOLID_GREEN, "Solid green", "Sets the Prism to solid green."),
        PrismPresetChoice(PrismPwmPreset.SOLID_CYAN, "Solid cyan", "Sets the Prism to solid cyan."),
        PrismPresetChoice(PrismPwmPreset.SOLID_BLUE, "Solid blue", "Sets the Prism to solid blue."),
        PrismPresetChoice(PrismPwmPreset.SOLID_PURPLE, "Solid purple", "Sets the Prism to solid purple."),
        PrismPresetChoice(PrismPwmPreset.SOLID_WHITE, "Solid white", "Sets the Prism to solid white."),
    )
}
