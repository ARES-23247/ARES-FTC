package org.firstinspires.ftc.teamcode.dsl

import com.areslib.action.RobotAction
import com.areslib.hardware.actuator.IndicatorLightColor
import com.areslib.pathing.CommandKey
import com.areslib.pathing.NamedCommandDescriptor
import com.areslib.pathing.NamedCommands
import com.areslib.sequencer.StateActionTask
import com.areslib.sequencer.Task
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

    private val mechanismDescriptors = listOf(
        INTAKE_COLLECT,
        INTAKE_STOP,
        FLYWHEEL_PREPARE,
        FLYWHEEL_STOP
    )
    private val primaryIndicatorDescriptors = IndicatorLightColor.entries.associateWith { color ->
        indicatorDescriptor(color, primary = true)
    }
    private val secondaryIndicatorDescriptors = IndicatorLightColor.entries.associateWith { color ->
        indicatorDescriptor(color, primary = false)
    }

    /** Complete runtime catalog that must match `ares/auto-capabilities.json`. */
    val descriptors: List<NamedCommandDescriptor> = mechanismDescriptors +
        primaryIndicatorDescriptors.values + secondaryIndicatorDescriptors.values

    /** Registers fresh task factories. Calling this again safely replaces prior registrations. */
    fun register() {
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
        registerStateAction(
            FLYWHEEL_PREPARE
        ) { state ->
            val targetRpm = state.tuning.flywheelTargetRpmPreset
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

    /**
     * Registers indicator actions after optional hardware discovery is complete.
     *
     * Missing lights retain their advertised command as a safe no-op, so an auto remains portable
     * between the competition robot and simulator configurations with fewer optional devices.
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
        NamedCommands.register(descriptor) {
            object : Task {
                override val name: String = descriptor.displayName
                override fun initialize(state: RobotState): List<RobotAction> =
                    if (available) {
                        listOf(RobotAction.SetIndicatorLight(hardwareName, color.position))
                    } else {
                        emptyList()
                    }

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
}
