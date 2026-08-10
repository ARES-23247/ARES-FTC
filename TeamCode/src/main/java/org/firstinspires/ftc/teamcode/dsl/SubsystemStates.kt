package org.firstinspires.ftc.teamcode.dsl

import com.areslib.state.SubsystemState
import com.areslib.state.SuperstructureState

/**
 * Immutable DECODE mechanism state stored in [SuperstructureState.custom].
 *
 * Targets express commanded intent; measured values are observations dispatched by subsystem
 * reads. RPM is motor revolutions per minute. Lift values retain the mechanism's configured
 * distance unit until physical lift IO defines a canonical unit.
 */
data class SeasonSuperstructureState(
    /** Whether the intake roller is commanded on. */
    val intakeActive: Boolean = false,
    /** Whether the flywheel controller should hold its target speed. */
    val flywheelActive: Boolean = false,
    /** Commanded flywheel speed in motor RPM. */
    val flywheelTargetRPM: Double = 0.0,
    /** Most recently accepted flywheel observation in motor RPM. */
    val flywheelCurrentRPM: Double = 0.0,
    /** Reserved lift target; no physical lift IO is currently registered. */
    val liftTargetHeight: Double = 0.0,
    /** Process-local lift observation carried from Auto to TeleOp. */
    val liftHeight: Double = 0.0
) : SubsystemState

/**
 * Shared allocation-free fallback when generic superstructure state has no DECODE extension.
 */
val DEFAULT_SEASON_STATE = SeasonSuperstructureState()

/**
 * Returns the typed season extension or [DEFAULT_SEASON_STATE] without allocating.
 */
val SuperstructureState.season: SeasonSuperstructureState
    get() = (this.custom as? SeasonSuperstructureState) ?: DEFAULT_SEASON_STATE
