package org.firstinspires.ftc.teamcode

import com.areslib.Store
import com.areslib.ftc.FtcMecanumRobot
import com.areslib.hardware.HardwareRegistry
import com.areslib.hardware.actuator.IndicatorLightIO
import com.areslib.hardware.actuator.IntakeIO
import com.areslib.reducer.rootReducer
import com.areslib.state.RobotState
import com.areslib.state.Alliance
import com.areslib.state.SuperstructureState
import org.firstinspires.ftc.teamcode.dsl.AresAutoBase
import org.firstinspires.ftc.teamcode.dsl.SeasonSuperstructureState
import org.firstinspires.ftc.teamcode.opmodes.AresRobot
import org.firstinspires.ftc.teamcode.subsystems.IndicatorLightSubsystem
import org.firstinspires.ftc.teamcode.subsystems.IntakeSubsystem
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito

class SeasonLifecycleSafetyTest {

    @Before
    fun clearRegistryBeforeTest() {
        HardwareRegistry.clear()
    }

    @After
    fun clearRegistryAfterTest() {
        HardwareRegistry.clear()
    }

    @Test
    fun closeLifecycleOrdersZeroOutputRegistrySafetyAndIdempotentIoClose() {
        val events = mutableListOf<String>()
        val io = RecordingIntakeIO(events)
        val subsystem = IntakeSubsystem(io)
        val robot = com.areslib.subsystem.AresRobot(
            initialState = RobotState(
                superstructure = SuperstructureState(
                    custom = SeasonSuperstructureState(intakeActive = true)
                )
            )
        )
        robot.registerSubsystem(subsystem)
        HardwareRegistry.registerDevice("test-intake", io)

        robot.safeAll()
        robot.closeSubsystems()
        HardwareRegistry.closeAll()

        assertEquals(
            listOf("roller:0.0", "registry-safe", "close", "close"),
            events
        )
    }

    @Test
    fun invalidCurrentCannotLatchIntakeStallAndValidSamplesCanRecover() {
        val io = RecordingIntakeIO(mutableListOf()).apply {
            rollerCurrentAmpsValue = 9.0
            rollerCurrentValidValue = true
        }
        val subsystem = IntakeSubsystem(io)
        val store = Store(RobotState(), ::rootReducer)

        subsystem.readSensors(store, 1_000L)
        subsystem.readSensors(store, 1_251L)
        assertTrue("A sustained valid overcurrent should latch the stall", subsystem.stalled)

        io.rollerCurrentValidValue = false
        subsystem.readSensors(store, 1_252L)
        assertFalse("An invalid sample must clear stale overcurrent state", subsystem.stalled)

        io.rollerCurrentValidValue = true
        subsystem.readSensors(store, 2_000L)
        assertFalse("Recovery starts a new dwell window instead of reusing the old timestamp", subsystem.stalled)
        subsystem.readSensors(store, 2_251L)
        assertTrue(subsystem.stalled)
    }

    @Test
    fun missingOptionalIndicatorStateProducesNoHardwareWriteButStillCloses() {
        val events = mutableListOf<String>()
        val io = RecordingIndicatorIO(events)
        val subsystem = IndicatorLightSubsystem(io, "indicator2")
        val defaultState = RobotState()
        val store = Store(defaultState, ::rootReducer)

        subsystem.readSensors(store, 0L)
        subsystem.writeOutputs(defaultState, 1.0)
        assertTrue("An absent optional light target must be a no-op", events.isEmpty())

        subsystem.close()
        assertEquals(listOf("close"), events)
    }

    @Test
    fun autonomousAbortSafetyStopsSeasonOutputsBeforePlatformHardware() {
        val wrapper = Mockito.mock(AresRobot::class.java)
        val base = Mockito.mock(FtcMecanumRobot::class.java)
        val auto = object : AresAutoBase() {
            override fun defineAuto() = auto {
                aresAuto("safety-fixture")
                alliance(Alliance.RED)
            }
            override fun buildRobot(): AresRobot = wrapper
            override fun getMecanumRobot(robot: AresRobot): FtcMecanumRobot = base
        }

        auto.safeRobot(wrapper)

        val order = Mockito.inOrder(base)
        order.verify(base).safeAll()
        order.verify(base).safeHardware()
        order.verifyNoMoreInteractions()
    }

    private class RecordingIntakeIO(
        private val events: MutableList<String>
    ) : IntakeIO, AutoCloseable {
        var rollerCurrentAmpsValue = 0.0
        var rollerCurrentValidValue = true

        override val rollerCurrentAmps: Double
            get() = rollerCurrentAmpsValue

        override val rollerCurrentValid: Boolean
            get() = rollerCurrentValidValue

        override fun setPivotAngle(degrees: Double) = Unit

        override fun setPivotVoltage(volts: Double) = Unit

        override fun setRollerVoltage(volts: Double) {
            events += "roller:$volts"
        }

        override fun safe() {
            events += "registry-safe"
        }

        override fun close() {
            events += "close"
        }
    }

    private class RecordingIndicatorIO(
        private val events: MutableList<String>
    ) : IndicatorLightIO, AutoCloseable {
        override val currentPosition: Double = 0.0

        override fun setPosition(position: Double) {
            events += "position:$position"
        }

        override fun close() {
            events += "close"
        }
    }
}
