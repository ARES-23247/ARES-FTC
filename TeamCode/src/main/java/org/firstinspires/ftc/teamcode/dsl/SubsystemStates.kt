package org.firstinspires.ftc.teamcode.dsl

import com.areslib.state.SubsystemState
import com.areslib.state.SuperstructureState

data class IntakeState(val intakeActive: Boolean = false) : SubsystemState

data class FlywheelState(
    val flywheelActive: Boolean = false,
    val flywheelTargetRPM: Double = 0.0,
    var currentRPM: Double = 0.0
) : SubsystemState

// Extension properties to keep state access compatible with old code:
val SuperstructureState.intakeActive: Boolean
    get() = if (has(IntakeState::class.java)) get(IntakeState::class.java).intakeActive else false

val SuperstructureState.flywheelActive: Boolean
    get() = if (has(FlywheelState::class.java)) get(FlywheelState::class.java).flywheelActive else false

val SuperstructureState.flywheelTargetRPM: Double
    get() = if (has(FlywheelState::class.java)) get(FlywheelState::class.java).flywheelTargetRPM else 0.0

val SuperstructureState.currentRPM: Double
    get() = if (has(FlywheelState::class.java)) get(FlywheelState::class.java).currentRPM else 0.0
