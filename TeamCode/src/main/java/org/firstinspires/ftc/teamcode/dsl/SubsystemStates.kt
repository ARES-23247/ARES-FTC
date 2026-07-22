package org.firstinspires.ftc.teamcode.dsl

import com.areslib.state.SubsystemState
import com.areslib.state.SuperstructureState

data class SeasonSuperstructureState(
    /**
     * Documentation for intakeActive
     */
    val intakeActive: Boolean = false,
    /**
     * Documentation for flywheelActive
     */
    val flywheelActive: Boolean = false,
    /**
     * Documentation for flywheelTargetRPM
     */
    val flywheelTargetRPM: Double = 0.0,
    /**
     * Documentation for flywheelCurrentRPM
     */
    val flywheelCurrentRPM: Double = 0.0
) : SubsystemState
/**
 * Documentation for DEFAULT_SEASON_STATE
 */

val DEFAULT_SEASON_STATE = SeasonSuperstructureState()
/**
 * Documentation for SuperstructureState
 */

val SuperstructureState.season: SeasonSuperstructureState
    get() = (this.custom as? SeasonSuperstructureState) ?: DEFAULT_SEASON_STATE
