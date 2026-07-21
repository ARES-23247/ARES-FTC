package org.firstinspires.ftc.teamcode.dsl

import com.areslib.state.SubsystemState
import com.areslib.state.SuperstructureState

data class SeasonSuperstructureState(
    val intakeActive: Boolean = false,
    val flywheelActive: Boolean = false,
    val flywheelTargetRPM: Double = 0.0,
    val flywheelCurrentRPM: Double = 0.0
) : SubsystemState

val DEFAULT_SEASON_STATE = SeasonSuperstructureState()

val SuperstructureState.season: SeasonSuperstructureState
    get() = (this.custom as? SeasonSuperstructureState) ?: DEFAULT_SEASON_STATE
