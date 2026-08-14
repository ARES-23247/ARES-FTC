package org.firstinspires.ftc.teamcode

import com.areslib.Store
import com.areslib.action.RobotAction
import com.areslib.ftc.FtcMecanumRobot
import com.areslib.hardware.HardwareRegistry
import com.areslib.hardware.actuator.FlywheelIO
import com.areslib.hardware.actuator.IndicatorLightIO
import com.areslib.hardware.actuator.IntakeIO
import com.areslib.hardware.actuator.PrismPwmPreset
import com.areslib.reducer.rootReducer
import com.areslib.state.RobotState
import com.areslib.state.SuperstructureState
import org.firstinspires.ftc.teamcode.dsl.AresAutoBase
import org.firstinspires.ftc.teamcode.dsl.SeasonSuperstructureState
import org.firstinspires.ftc.teamcode.opmodes.AresRobot
import org.firstinspires.ftc.teamcode.subsystems.IndicatorLightSubsystem
import org.firstinspires.ftc.teamcode.subsystems.IntakeSubsystem
import org.firstinspires.ftc.teamcode.subsystems.FlywheelSubsystem
import org.firstinspires.ftc.teamcode.subsystems.PrismSubsystem
import org.firstinspires.ftc.teamcode.subsystems.MockPrismDriverIO
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
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
    fun sustainedOvercurrentLatchesIntakeUntilDisabledWithValidRecovery() {
        val io = RecordingIntakeIO(mutableListOf()).apply {
            rollerCurrentAmpsValue = 9.0
            rollerCurrentValidValue = true
        }
        val subsystem = IntakeSubsystem(io)
        val store = seasonStore(intakeActive = true)

        subsystem.readSensors(store, 1_000L)
        subsystem.readSensors(store, 1_250L)
        assertTrue("A sustained valid overcurrent should latch the stall", subsystem.stalled)

        io.rollerCurrentAmpsValue = 0.0
        subsystem.readSensors(store, 1_300L)
        assertTrue("A valid low-current sample must not restart an actively commanded intake", subsystem.stalled)

        setIntakeActive(store, false)
        subsystem.readSensors(store, 1_301L)
        assertTrue("Recovery requires a bounded valid dwell", subsystem.stalled)
        subsystem.readSensors(store, 1_401L)
        assertFalse("Disabled plus sustained valid low current should clear the latch", subsystem.stalled)
    }

    @Test
    fun invalidIntakeCurrentUsesBoundedGraceThenFailsClosedAndRequiresExplicitRecovery() {
        val events = mutableListOf<String>()
        val io = RecordingIntakeIO(events).apply {
            rollerCurrentValidValue = false
        }
        val subsystem = IntakeSubsystem(io)
        val store = seasonStore(intakeActive = true)

        subsystem.readSensors(store, 2_000L)
        subsystem.writeOutputs(store.state, 1.0)
        assertFalse("One bad sample is inside the bounded grace interval", subsystem.stalled)
        assertEquals("roller:12.0", events.last())

        subsystem.readSensors(store, 2_099L)
        assertFalse(subsystem.stalled)
        subsystem.readSensors(store, 2_100L)
        assertTrue("Persistent invalid current must latch fail-closed", subsystem.stalled)
        subsystem.writeOutputs(store.state, 1.0)
        assertEquals("A latched sensor fault must command actual zero output", "roller:0.0", events.last())

        io.rollerCurrentValidValue = true
        io.rollerCurrentAmpsValue = 0.0
        subsystem.readSensors(store, 2_200L)
        assertTrue("Valid feedback alone cannot clear while the operator still commands intake", subsystem.stalled)

        setIntakeActive(store, false)
        subsystem.readSensors(store, 2_201L)
        subsystem.readSensors(store, 2_301L)
        assertFalse(subsystem.stalled)
    }

    @Test
    fun flywheelInvalidFeedbackLatchesAfterGraceStopsVoltageAndRecoversOnlyWhileDisabled() {
        val io = RecordingFlywheelIO().apply { velocityValidValue = false }
        val subsystem = FlywheelSubsystem(io)
        val store = seasonStore(flywheelActive = true)

        subsystem.readSensors(store, 3_000L)
        subsystem.writeOutputs(store.state, 1.0)
        assertFalse(subsystem.feedbackFaultLatched)
        assertEquals(1, io.velocityCommands.size)

        subsystem.readSensors(store, 3_149L)
        assertFalse(subsystem.feedbackFaultLatched)
        subsystem.readSensors(store, 3_150L)
        assertTrue("Persistent invalid feedback must latch after the bounded grace", subsystem.feedbackFaultLatched)
        subsystem.writeOutputs(store.state, 1.0)
        assertEquals(listOf(0.0), io.voltageCommands)

        io.velocityValidValue = true
        subsystem.readSensors(store, 3_200L)
        assertTrue("An active flywheel must not auto-restart after feedback recovery", subsystem.feedbackFaultLatched)

        setFlywheelActive(store, false)
        subsystem.readSensors(store, 3_201L)
        subsystem.readSensors(store, 3_301L)
        assertFalse(subsystem.feedbackFaultLatched)
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
    fun indicatorLightSubsystemWritesDispatchedServoPosition() {
        val events = mutableListOf<String>()
        val io = RecordingIndicatorIO(events)
        val subsystem = IndicatorLightSubsystem(io, "indicator")
        val store = Store(RobotState(), ::rootReducer)
        val greenPos = com.areslib.hardware.actuator.IndicatorLightColor.GREEN.position
        store.dispatch(RobotAction.SetIndicatorLight("indicator", greenPos))

        subsystem.readSensors(store, 100L)
        subsystem.writeOutputs(store.state, 1.0)

        assertEquals(listOf("position:$greenPos"), events)
        subsystem.close()
    }

    @Test
    fun prismLifecycleAppliesReduxPresetWithBoundedBrightnessAndCloses() {
        val io = MockPrismDriverIO()
        val subsystem = PrismSubsystem(io, configuredMaxBrightness = 80)
        val store = Store(RobotState(), ::rootReducer)
        store.dispatch(RobotAction.SetPrismDriver("prism", 1005))

        subsystem.readSensors(store, 0L)
        subsystem.writeOutputs(store.state, 0.5)

        assertEquals(1005, io.currentPulseWidthUs)
        assertEquals(40, io.maxBrightnessPercent)
        subsystem.close()
        assertTrue(io.isClosed)
        assertEquals(
            "Mock cleanup must match the FTC adapter's visible red safety indication",
            PrismPwmPreset.SOLID_RED.pulseWidthUs,
            io.currentPulseWidthUs,
        )
    }

    @Test
    fun prismSubsystemClampsEffortScaleToUnitRange() {
        val io = MockPrismDriverIO()
        val subsystem = PrismSubsystem(io, configuredMaxBrightness = 100)
        val store = Store(RobotState(), ::rootReducer)
        store.dispatch(RobotAction.SetPrismDriver("prism", 1200))

        subsystem.readSensors(store, 0L)
        subsystem.writeOutputs(store.state, 1.5)
        assertEquals(100, io.maxBrightnessPercent)

        subsystem.writeOutputs(store.state, -0.5)
        assertEquals(0, io.maxBrightnessPercent)
        subsystem.close()
    }

    @Test
    fun autonomousAbortSafetyStopsSeasonOutputsBeforePlatformHardware() {
        val wrapper = Mockito.mock(AresRobot::class.java)
        val base = Mockito.mock(FtcMecanumRobot::class.java)
        val auto = object : AresAutoBase() {
            override fun buildRobot(): AresRobot = wrapper
            override fun getMecanumRobot(robot: AresRobot): FtcMecanumRobot = base
        }

        auto.safeRobot(wrapper)

        val order = Mockito.inOrder(base)
        order.verify(base).safeAll()
        order.verify(base).safeHardware()
        order.verifyNoMoreInteractions()
    }

    @Test
    fun autonomousAbortSafetyStillStopsPlatformWhenSeasonSafetyThrows() {
        val wrapper = Mockito.mock(AresRobot::class.java)
        val base = Mockito.mock(FtcMecanumRobot::class.java)
        val failure = IllegalStateException("season output fault")
        Mockito.doThrow(failure).`when`(base).safeAll()
        val auto = object : AresAutoBase() {
            override fun buildRobot(): AresRobot = wrapper
            override fun getMecanumRobot(robot: AresRobot): FtcMecanumRobot = base
        }

        val thrown = org.junit.Assert.assertThrows(IllegalStateException::class.java) {
            auto.safeRobot(wrapper)
        }

        assertSame(failure, thrown)
        val order = Mockito.inOrder(base)
        order.verify(base).safeAll()
        order.verify(base).safeHardware()
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

    private class RecordingFlywheelIO : FlywheelIO {
        var velocityRpmValue = 0.0
        var velocityValidValue = true
        val velocityCommands = mutableListOf<Pair<Double, Double>>()
        val voltageCommands = mutableListOf<Double>()

        override val velocityRpm: Double
            get() = velocityRpmValue

        override val velocityValid: Boolean
            get() = velocityValidValue

        override fun setVelocityRpm(rpm: Double) {
            velocityCommands += rpm to 1.0
        }

        override fun setVelocityRpm(rpm: Double, maxEffortScale: Double) {
            velocityCommands += rpm to maxEffortScale
        }

        override fun setAppliedVoltage(volts: Double) {
            voltageCommands += volts
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

    private fun seasonStore(
        intakeActive: Boolean = false,
        flywheelActive: Boolean = false
    ): Store = Store(
        RobotState(
            superstructure = SuperstructureState(
                custom = SeasonSuperstructureState(
                    intakeActive = intakeActive,
                    flywheelActive = flywheelActive,
                    flywheelTargetRPM = if (flywheelActive) 3_500.0 else 0.0
                )
            )
        ),
        ::rootReducer
    )

    private fun setIntakeActive(store: Store, active: Boolean) {
        val season = store.state.superstructure.custom as SeasonSuperstructureState
        store.dispatch(RobotAction.UpdateSubsystemState(season.copy(intakeActive = active)))
    }

    private fun setFlywheelActive(store: Store, active: Boolean) {
        val season = store.state.superstructure.custom as SeasonSuperstructureState
        store.dispatch(RobotAction.UpdateSubsystemState(season.copy(flywheelActive = active)))
    }
}
