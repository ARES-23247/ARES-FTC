package org.firstinspires.ftc.teamcode.dsl

import com.areslib.action.RobotAction
import com.areslib.pathing.CommandKey
import com.areslib.pathing.NamedCommandDescriptor
import com.areslib.pathing.NamedCommands
import com.areslib.sequencer.StateActionTask

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

    private fun registerStateAction(
        descriptor: NamedCommandDescriptor,
        actionFactory: (com.areslib.state.RobotState) -> RobotAction
    ) {
        NamedCommands.register(descriptor) {
            StateActionTask(descriptor.displayName, actionFactory)
        }
    }

    private const val MAX_FLYWHEEL_RPM = 6000.0
}
