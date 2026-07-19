package org.firstinspires.ftc.teamcode.dsl

import com.areslib.state.SubsystemState
import com.areslib.state.SuperstructureState

// No custom states needed anymore.
val SuperstructureState.currentRPM: Double
    get() = 0.0
