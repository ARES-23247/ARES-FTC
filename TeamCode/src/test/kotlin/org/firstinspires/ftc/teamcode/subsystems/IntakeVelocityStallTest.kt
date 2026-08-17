package org.firstinspires.ftc.teamcode.subsystems

import com.areslib.Store
import com.areslib.action.RobotAction
import com.areslib.state.RobotState
import org.firstinspires.ftc.teamcode.dsl.SeasonSuperstructureState
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Dwell semantics of the observation-only velocity-stall detector. */
class IntakeVelocityStallTest {
    private class FakeIntakeIO : com.areslib.hardware.actuator.IntakeIO {
        var amps = 0.0
        var ampsValid = true
        override val rollerCurrentValid: Boolean get() = ampsValid
        override val rollerCurrentAmps: Double get() = amps
        override fun isCurrentReadingValid(readingAmps: Double) = ampsValid
        override val rollerVelocityTicksPerSec: Double get() = 0.0
        override fun setRollerVoltage(volts: Double) {}
        override fun setPivotAngle(degrees: Double) {}
        override fun setPivotVoltage(volts: Double) {}
        override fun refresh() {}
        override fun safe() {}
    }

    private fun activeStore() = Store().apply {
        dispatch(RobotAction.UpdateSubsystemState(SeasonSuperstructureState(intakeActive = true)))
    }

    @Test
    fun `near-zero valid velocity under load suspects a stall after the dwell`() {
        val io = FakeIntakeIO().apply { amps = 4.0 }
        val subsystem = IntakeSubsystem(io) { true to 2.0 }
        val store = activeStore()

        subsystem.readSensors(store, 0L)
        assertFalse(subsystem.velocityStallSuspected)
        subsystem.readSensors(store, 999L)
        assertFalse(subsystem.velocityStallSuspected)
        subsystem.readSensors(store, 1_000L)
        assertTrue(submoduleSuspectedAfterDwell(subsystem))
    }

    private fun submoduleSuspectedAfterDwell(subsystem: IntakeSubsystem) = subsystem.velocityStallSuspected

    @Test
    fun `healthy velocity, invalid velocity, idle current, and inactive intake never suspect`() {
        val io = FakeIntakeIO().apply { amps = 4.0 }
        val store = activeStore()

        IntakeSubsystem(io) { true to 2_000.0 }.let { subsystem ->
            repeat(5) { subsystem.readSensors(store, it * 400L) }
            assertFalse(subsystem.velocityStallSuspected)
        }
        IntakeSubsystem(io) { false to 2.0 }.let { subsystem ->
            repeat(5) { subsystem.readSensors(store, it * 400L) }
            assertFalse(subsystem.velocityStallSuspected)
        }
        io.amps = 0.5
        IntakeSubsystem(io) { true to 2.0 }.let { subsystem ->
            repeat(5) { subsystem.readSensors(store, it * 400L) }
            assertFalse(subsystem.velocityStallSuspected)
        }
        val idleStore = Store()
        IntakeSubsystem(io) { true to 2.0 }.let { subsystem ->
            repeat(5) { subsystem.readSensors(idleStore, it * 400L) }
            assertFalse(subsystem.velocityStallSuspected)
        }
    }

    @Test
    fun `a recovered velocity reading resets the dwell`() {
        val io = FakeIntakeIO().apply { amps = 4.0 }
        var velocity = 2.0
        val subsystem = IntakeSubsystem(io) { true to velocity }
        val store = activeStore()

        subsystem.readSensors(store, 0L)
        subsystem.readSensors(store, 600L)
        velocity = 1_500.0
        subsystem.readSensors(store, 800L)
        subsystem.readSensors(store, 900L)
        velocity = 2.0
        subsystem.readSensors(store, 1_000L)
        assertFalse("intermittent recovery must restart the dwell", subsystem.velocityStallSuspected)
        subsystem.readSensors(store, 2_100L)
        assertTrue("sustained disagreement after the reset still suspects", subsystem.velocityStallSuspected)
    }
}
