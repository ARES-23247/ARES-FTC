package org.firstinspires.ftc.teamcode.dsl

import com.areslib.state.SubsystemState
import com.areslib.state.SuperstructureState

data class IntakeState(val intakeActive: Boolean = false) : SubsystemState

data class FlywheelState(
    val flywheelActive: Boolean = false,
    val flywheelTargetRPM: Double = 0.0,
    val currentRPM: Double = 0.0
) : SubsystemState

// Extension properties to keep state access compatible with old code:
val SuperstructureState.intakeActive: Boolean
    get() = (states[IntakeState::class.java] as? IntakeState)?.intakeActive ?: false

val SuperstructureState.flywheelActive: Boolean
    get() = (states[FlywheelState::class.java] as? FlywheelState)?.flywheelActive ?: false

val SuperstructureState.flywheelTargetRPM: Double
    get() = (states[FlywheelState::class.java] as? FlywheelState)?.flywheelTargetRPM ?: 0.0

val SuperstructureState.currentRPM: Double
    get() = (states[FlywheelState::class.java] as? FlywheelState)?.currentRPM ?: 0.0
