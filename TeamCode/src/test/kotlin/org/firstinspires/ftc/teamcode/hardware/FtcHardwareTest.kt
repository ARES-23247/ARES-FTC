package org.firstinspires.ftc.teamcode.hardware

import com.qualcomm.robotcore.hardware.DcMotorEx
import com.qualcomm.robotcore.hardware.HardwareMap
import com.qualcomm.robotcore.hardware.Servo
import com.qualcomm.robotcore.hardware.VoltageSensor
import org.firstinspires.ftc.robotcore.external.navigation.CurrentUnit
import com.areslib.hardware.HardwareRegistry
import com.areslib.hardware.CurrentSourceSampler
import com.areslib.hardware.actuator.FlywheelIO
import com.areslib.state.RobotState
import com.areslib.state.SuperstructureState
import org.firstinspires.ftc.teamcode.dsl.SeasonSuperstructureState
import org.firstinspires.ftc.teamcode.subsystems.FlywheelSubsystem
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.AdditionalMatchers
import org.mockito.Mockito
import com.areslib.control.tuning.PIDFCoefficients
import com.areslib.control.tuning.SimpleFeedforwardCoeffs

class FtcHardwareTest {

    @After
    fun tearDown() {
        HardwareRegistry.clear()
    }

    private data class HardwareFixture(
        val hardwareMap: HardwareMap,
        val voltageSensor: VoltageSensor
    )

    private fun createMockHardwareMap(
        motorName: String,
        mockMotor: DcMotorEx,
        voltage: Double = 12.0
    ): HardwareFixture {
        val hardwareMap = Mockito.mock(HardwareMap::class.java)
        Mockito.`when`(hardwareMap.get(DcMotorEx::class.java, motorName)).thenReturn(mockMotor)

        val mockVoltageSensor = Mockito.mock(VoltageSensor::class.java)
        Mockito.`when`(mockVoltageSensor.voltage).thenReturn(voltage)

        @Suppress("UNCHECKED_CAST")
        val deviceMapping = Mockito.mock(HardwareMap.DeviceMapping::class.java) as HardwareMap.DeviceMapping<VoltageSensor>
        Mockito.`when`(deviceMapping.iterator()).thenAnswer { listOf(mockVoltageSensor).iterator() }

        val mockServo = Mockito.mock(Servo::class.java)
        @Suppress("UNCHECKED_CAST")
        val servoMapping = Mockito.mock(HardwareMap.DeviceMapping::class.java) as HardwareMap.DeviceMapping<Servo>
        Mockito.`when`(servoMapping.get(Mockito.anyString())).thenReturn(mockServo)

        runCatching {
            HardwareMap::class.java.getField("voltageSensor").set(hardwareMap, deviceMapping)
            HardwareMap::class.java.getField("servo").set(hardwareMap, servoMapping)
        }

        return HardwareFixture(hardwareMap, mockVoltageSensor)
    }

    @Test
    fun testFtcFlywheelIO() {
        val mockMotor = Mockito.mock(DcMotorEx::class.java)
        val hardwareMap = createMockHardwareMap("shooter", mockMotor).hardwareMap
        Mockito.`when`(mockMotor.velocity).thenReturn(1400.0)

        val io = FtcFlywheelIO(hardwareMap)
        io.setVelocityRpm(3000.0)
        assertTrue(io.outputApplied)
        Mockito.verify(mockMotor).velocity = (3000.0 / 60.0) * 28.0

        io.setAppliedVoltage(6.0)
        Mockito.verify(mockMotor).power = 0.5

        // The base calls this immediately after clearing the REV bulk cache.
        HardwareRegistry.refreshAll()
        assertEquals(3000.0, io.velocityRpm, 1e-6)

        assertEquals(0.0, io.currentAmps, 1e-6)
        assertEquals(0.0, io.tempCelsius, 1e-6)

        io.close()
        assertFalse(io.outputApplied)
        Mockito.verify(mockMotor, Mockito.atLeastOnce()).power = 0.0
    }

    @Test
    fun testFtcIntakeIO() {
        val mockMotor = Mockito.mock(DcMotorEx::class.java)
        val hardwareMap = createMockHardwareMap("intake", mockMotor).hardwareMap

        val io = FtcIntakeIO(hardwareMap)
        io.setRollerVoltage(12.0)
        assertTrue(io.outputApplied)
        Mockito.verify(mockMotor).power = 1.0

        HardwareRegistry.refreshAll()
        Mockito.verify(mockMotor, Mockito.atLeastOnce()).velocity

        // A drivetrain crash invokes HardwareRegistry.safeAll(). Season motors
        // must be registered so that crash safety stops them too.
        HardwareRegistry.safeAll()
        assertFalse(io.outputApplied)
        Mockito.verify(mockMotor, Mockito.atLeastOnce()).power = 0.0

        io.close()
        Mockito.verify(mockMotor, Mockito.atLeastOnce()).power = 0.0
    }

    @Test
    fun intakeWriteFaultLatchesUntilASeparateNeutralCommandSucceeds() {
        val mockMotor = Mockito.mock(DcMotorEx::class.java)
        val hardwareMap = createMockHardwareMap("intake", mockMotor).hardwareMap
        val attemptedPowers = mutableListOf<Double>()
        var firstNonzero = true
        Mockito.doAnswer { invocation ->
            val power = invocation.getArgument<Double>(0)
            attemptedPowers += power
            if (power != 0.0 && firstNonzero) {
                firstNonzero = false
                throw RuntimeException("hub rejected output")
            }
            null
        }.`when`(mockMotor).power = Mockito.anyDouble()
        val io = FtcIntakeIO(hardwareMap)

        io.setRollerVoltage(12.0)
        assertEquals(listOf(1.0, 0.0), attemptedPowers)
        assertFalse(io.outputApplied)

        io.setRollerVoltage(6.0)
        assertEquals("A faulted nonzero command may only retry neutral", listOf(1.0, 0.0, 0.0), attemptedPowers)
        io.setRollerVoltage(0.0)
        io.setRollerVoltage(6.0)
        assertEquals(listOf(1.0, 0.0, 0.0, 0.0, 0.5), attemptedPowers)
        assertTrue(io.outputApplied)
    }

    @Test
    fun intakeFailedStopBlocksMotionUntilAConfirmedNeutralWrite() {
        val mockMotor = Mockito.mock(DcMotorEx::class.java)
        val hardwareMap = createMockHardwareMap("intake", mockMotor).hardwareMap
        val attemptedPowers = mutableListOf<Double>()
        var rejectNextZero = false
        Mockito.doAnswer { invocation ->
            val power = invocation.getArgument<Double>(0)
            attemptedPowers += power
            if (power == 0.0 && rejectNextZero) {
                rejectNextZero = false
                throw RuntimeException("hub rejected STOP")
            }
            null
        }.`when`(mockMotor).power = Mockito.anyDouble()
        val io = FtcIntakeIO(hardwareMap)

        io.setRollerVoltage(12.0)
        assertTrue(io.outputApplied)
        rejectNextZero = true
        io.setRollerVoltage(0.0)
        assertTrue("A failed STOP cannot claim the previous output was cleared", io.outputApplied)

        io.setRollerVoltage(6.0)
        assertEquals(listOf(1.0, 0.0, 0.0), attemptedPowers)
        assertFalse(io.outputApplied)

        io.setRollerVoltage(0.0)
        io.setRollerVoltage(6.0)
        assertEquals(listOf(1.0, 0.0, 0.0, 0.0, 0.5), attemptedPowers)
        assertTrue(io.outputApplied)
    }

    @Test
    fun defaultZeroFlywheelTuningDoesNotOverwriteHubPidf() {
        val mockMotor = Mockito.mock(DcMotorEx::class.java)
        val io = FtcFlywheelIO(createMockHardwareMap("shooter", mockMotor).hardwareMap)
        Mockito.clearInvocations(mockMotor)

        io.configureVelocityController(PIDFCoefficients(), SimpleFeedforwardCoeffs())
        io.configureVelocityController(PIDFCoefficients(), SimpleFeedforwardCoeffs(kS = 0.2, kA = 0.1))

        Mockito.verify(mockMotor, Mockito.never()).setPIDFCoefficients(
            Mockito.any(com.qualcomm.robotcore.hardware.DcMotor.RunMode::class.java),
            Mockito.any(com.qualcomm.robotcore.hardware.PIDFCoefficients::class.java),
        )

        io.configureVelocityController(
            PIDFCoefficients(kP = 1.0),
            SimpleFeedforwardCoeffs(kV = 1.0),
        )
        Mockito.verify(mockMotor).setPIDFCoefficients(
            Mockito.eq(com.qualcomm.robotcore.hardware.DcMotor.RunMode.RUN_USING_ENCODER),
            Mockito.any(com.qualcomm.robotcore.hardware.PIDFCoefficients::class.java),
        )
    }

    @Test
    fun intakeCurrentValidityRecoversAfterTransientReadFailure() {
        val mockMotor = Mockito.mock(DcMotorEx::class.java)
        val hardwareMap = createMockHardwareMap("intake", mockMotor).hardwareMap
        Mockito.`when`(mockMotor.getCurrent(CurrentUnit.AMPS))
            .thenThrow(RuntimeException("transient read failure"))
            .thenReturn(9.0)

        val io = FtcIntakeIO(hardwareMap)
        io.refresh()
        assertFalse(io.rollerCurrentValid)
        assertEquals(0.0, io.rollerCurrentAmps, 1e-6)

        io.refresh()
        assertTrue(io.rollerCurrentValid)
        assertEquals(9.0, io.rollerCurrentAmps, 1e-6)
    }

    @Test
    fun rollerOnlyIntakeContributesItsValidCurrentToTheSharedSampler() {
        val mockMotor = Mockito.mock(DcMotorEx::class.java)
        val hardwareMap = createMockHardwareMap("intake", mockMotor).hardwareMap
        Mockito.`when`(mockMotor.getCurrent(CurrentUnit.AMPS))
            .thenThrow(RuntimeException("transient read failure"))
            .thenReturn(8.75)
        val io = FtcIntakeIO(hardwareMap)
        val sampler = CurrentSourceSampler()

        io.refresh()
        assertTrue(sampler.sample(listOf(io)).isNaN())
        assertFalse(sampler.hasCompleteCoverage)

        io.refresh()
        assertEquals(8.75, sampler.sample(listOf(io)), 1e-6)
        assertTrue(sampler.hasCompleteCoverage)
        Mockito.verify(mockMotor, Mockito.times(2)).getCurrent(CurrentUnit.AMPS)
    }

    @Test
    fun intakeRejectsNonFiniteCurrentAndRecoversOnNextRefresh() {
        val mockMotor = Mockito.mock(DcMotorEx::class.java)
        val hardwareMap = createMockHardwareMap("intake", mockMotor).hardwareMap
        Mockito.`when`(mockMotor.getCurrent(CurrentUnit.AMPS))
            .thenReturn(Double.NaN)
            .thenReturn(6.5)

        val io = FtcIntakeIO(hardwareMap)
        io.refresh()
        assertFalse("NaN current must not be treated as a trustworthy sample", io.rollerCurrentValid)
        assertEquals(0.0, io.rollerCurrentAmps, 1e-6)

        io.refresh()
        assertTrue("A bad sample must not permanently disable current sensing", io.rollerCurrentValid)
        assertEquals(6.5, io.rollerCurrentAmps, 1e-6)
    }

    @Test
    fun intakeRejectsPhysicallyInvalidNegativeCurrent() {
        val mockMotor = Mockito.mock(DcMotorEx::class.java)
        val hardwareMap = createMockHardwareMap("intake", mockMotor).hardwareMap
        Mockito.`when`(mockMotor.getCurrent(CurrentUnit.AMPS)).thenReturn(-0.25)

        val io = FtcIntakeIO(hardwareMap)
        io.refresh()

        assertFalse(io.rollerCurrentValid)
        assertEquals(0.0, io.rollerCurrentAmps, 1e-6)
    }

    @Test
    fun intakeUsesOnlySharedCachedVoltageAndFallsBackWhenItIsNotFinite() {
        val mockMotor = Mockito.mock(DcMotorEx::class.java)
        val fixture = createMockHardwareMap("intake", mockMotor, voltage = Double.NaN)
        val io = FtcIntakeIO(fixture.hardwareMap) { Double.NaN }

        io.refresh()
        Mockito.verifyNoInteractions(fixture.voltageSensor)
        Mockito.clearInvocations(mockMotor)
        io.setRollerVoltage(6.0)

        Mockito.verify(mockMotor).power = AdditionalMatchers.eq(0.5, 1e-9)
        Mockito.verifyNoInteractions(fixture.voltageSensor)
        Mockito.verify(mockMotor, Mockito.never()).velocity
        Mockito.verify(mockMotor, Mockito.never()).getCurrent(Mockito.any(CurrentUnit::class.java))
    }

    @Test
    fun flywheelVelocityCacheInvalidatesThenRecoversAfterTransientReadFailure() {
        val mockMotor = Mockito.mock(DcMotorEx::class.java)
        val hardwareMap = createMockHardwareMap("shooter", mockMotor).hardwareMap
        Mockito.`when`(mockMotor.velocity)
            .thenReturn(1_400.0)
            .thenThrow(RuntimeException("transient encoder read failure"))
            .thenReturn(700.0)

        val io = FtcFlywheelIO(hardwareMap)
        io.refresh()
        assertTrue(io.velocityValid)
        assertEquals(3_000.0, io.velocityRpm, 1e-6)

        io.refresh()
        assertFalse("A failed refresh must invalidate the sample instead of retaining stale RPM", io.velocityValid)
        assertEquals(0.0, io.velocityRpm, 1e-6)

        io.refresh()
        assertTrue("A later successful read must restore velocity validity", io.velocityValid)
        assertEquals(1_500.0, io.velocityRpm, 1e-6)
    }

    @Test
    fun flywheelCurrentSensingRecoversAfterTransientReadFailure() {
        val mockMotor = Mockito.mock(DcMotorEx::class.java)
        val hardwareMap = createMockHardwareMap("shooter", mockMotor).hardwareMap
        Mockito.`when`(mockMotor.getCurrent(CurrentUnit.AMPS))
            .thenThrow(RuntimeException("transient current read failure"))
            .thenReturn(7.25)

        val io = FtcFlywheelIO(hardwareMap)
        io.refresh()
        assertFalse(io.currentReadingValid)
        assertEquals(0.0, io.currentAmps, 1e-6)

        io.refresh()
        assertTrue(io.currentReadingValid)
        assertEquals("One failed hub transaction must not permanently disable sensing", 7.25, io.currentAmps, 1e-6)
        Mockito.verify(mockMotor, Mockito.times(2)).getCurrent(CurrentUnit.AMPS)
    }

    @Test
    fun flywheelFailedCurrentReadCannotMasqueradeAsAValidZeroInSharedSampler() {
        val mockMotor = Mockito.mock(DcMotorEx::class.java)
        val hardwareMap = createMockHardwareMap("shooter", mockMotor).hardwareMap
        Mockito.`when`(mockMotor.getCurrent(CurrentUnit.AMPS))
            .thenThrow(RuntimeException("transient current read failure"))
            .thenReturn(7.25)
        val io = FtcFlywheelIO(hardwareMap)
        val sampler = CurrentSourceSampler()

        io.refresh()
        assertTrue(sampler.sample(listOf(io)).isNaN())
        assertFalse(sampler.hasCompleteCoverage)

        io.refresh()
        assertEquals(7.25, sampler.sample(listOf(io)), 1e-6)
        assertTrue(sampler.hasCompleteCoverage)
        Mockito.verify(mockMotor, Mockito.times(2)).getCurrent(CurrentUnit.AMPS)
    }

    @Test
    fun flywheelVelocityControlFailureStopsAndRequiresExplicitHealthyZeroBeforeRetry() {
        val mockMotor = Mockito.mock(DcMotorEx::class.java)
        val fixture = createMockHardwareMap("shooter", mockMotor)
        Mockito.`when`(mockMotor.velocity).thenReturn(0.0)
        var velocityWrites = 0
        Mockito.doAnswer {
            velocityWrites++
            if (velocityWrites == 1) throw RuntimeException("velocity control unavailable")
            null
        }.`when`(mockMotor).velocity = Mockito.anyDouble()

        val io = FtcFlywheelIO(fixture.hardwareMap)
        io.refresh()
        Mockito.clearInvocations(mockMotor)
        io.setVelocityRpm(3_000.0)

        Mockito.verify(mockMotor).velocity = (3_000.0 / 60.0) * 28.0
        Mockito.verify(mockMotor).power = 0.0
        Mockito.verify(mockMotor, Mockito.never()).mode = com.qualcomm.robotcore.hardware.DcMotor.RunMode.RUN_WITHOUT_ENCODER
        assertFalse("A failed controller write must invalidate closed-loop availability", io.velocityValid)

        Mockito.clearInvocations(mockMotor)
        io.setVelocityRpm(3_000.0)
        Mockito.verify(mockMotor, Mockito.never()).velocity = Mockito.anyDouble()
        Mockito.verify(mockMotor, Mockito.never()).power = AdditionalMatchers.eq(0.5, 1e-9)

        // A good read alone cannot silently re-arm the failed output path.
        io.refresh()
        assertFalse(io.velocityValid)
        Mockito.clearInvocations(mockMotor)
        io.setVelocityRpm(0.0)
        assertTrue("A healthy sample plus explicit zero command should re-arm closed-loop control", io.velocityValid)

        Mockito.clearInvocations(mockMotor)
        io.setVelocityRpm(3_000.0)
        Mockito.verify(mockMotor).velocity = (3_000.0 / 60.0) * 28.0
        Mockito.verify(mockMotor, Mockito.never()).power = AdditionalMatchers.eq(0.5, 1e-9)
    }

    @Test
    fun flywheelFaultCannotBeBypassedBySwitchingToRawVoltageBeforeNeutralRecovery() {
        val mockMotor = Mockito.mock(DcMotorEx::class.java)
        val fixture = createMockHardwareMap("shooter", mockMotor)
        Mockito.`when`(mockMotor.velocity).thenReturn(0.0)
        Mockito.doThrow(RuntimeException("velocity control unavailable"))
            .`when`(mockMotor).velocity = Mockito.anyDouble()
        val io = FtcFlywheelIO(fixture.hardwareMap)
        io.refresh()

        io.setVelocityRpm(3_000.0)
        Mockito.clearInvocations(mockMotor)
        io.setAppliedVoltage(6.0)

        assertFalse(io.outputApplied)
        Mockito.verify(mockMotor, Mockito.never()).power = Mockito.anyDouble()

        Mockito.clearInvocations(mockMotor)
        io.setAppliedVoltage(0.0)
        assertTrue("A fresh sample plus an explicit zero command should re-arm all output paths", io.velocityValid)
        io.setAppliedVoltage(6.0)
        Mockito.verify(mockMotor).power = AdditionalMatchers.eq(0.5, 1e-9)
    }

    @Test
    fun flywheelReducedEffortRequiresFeedbackAndCommandsActualZeroWhenFeedbackIsInvalid() {
        val mockMotor = Mockito.mock(DcMotorEx::class.java)
        val fixture = createMockHardwareMap("shooter", mockMotor)
        Mockito.`when`(mockMotor.velocity)
            .thenReturn(0.0)
            .thenThrow(RuntimeException("encoder unavailable"))

        val io = FtcFlywheelIO(fixture.hardwareMap)
        io.refresh()
        Mockito.clearInvocations(mockMotor)
        io.setVelocityRpm(3_500.0, 0.45)
        Mockito.verify(mockMotor).power = AdditionalMatchers.eq(0.45, 1e-9)

        io.refresh()
        assertFalse(io.velocityValid)
        Mockito.clearInvocations(mockMotor)
        io.setVelocityRpm(3_500.0, 0.45)
        Mockito.verify(mockMotor).power = 0.0
        Mockito.verify(mockMotor, Mockito.never()).velocity = Mockito.anyDouble()
    }

    @Test
    fun flywheelHardStopIsWrittenAfterVelocityCommandEvenWhenPreviousRawPowerWasZero() {
        val mockMotor = Mockito.mock(DcMotorEx::class.java)
        val hardwareMap = createMockHardwareMap("shooter", mockMotor).hardwareMap
        val io = FtcFlywheelIO(hardwareMap)

        io.setAppliedVoltage(0.0)
        io.setVelocityRpm(3_000.0)
        Mockito.clearInvocations(mockMotor)

        io.setAppliedVoltage(0.0)

        Mockito.verify(mockMotor).power = 0.0
        Mockito.verify(mockMotor, Mockito.never()).velocity = Mockito.anyDouble()
    }

    @Test
    fun flywheelEmergencyScaleStopsClosedLoopVelocity() {
        val io = Mockito.mock(FlywheelIO::class.java)
        val subsystem = FlywheelSubsystem(io)
        val state = RobotState(
            superstructure = SuperstructureState(
                custom = SeasonSuperstructureState(
                    flywheelActive = true,
                    flywheelTargetRPM = 3_500.0
                )
            )
        )

        subsystem.writeOutputs(state, 0.0)

        Mockito.verify(io).setAppliedVoltage(0.0)
        Mockito.verify(io, Mockito.never()).setVelocityRpm(Mockito.anyDouble(), Mockito.anyDouble())
    }

    @Test
    fun flywheelPartialScalePreservesTargetAndLimitsEffort() {
        val io = Mockito.mock(FlywheelIO::class.java)
        val subsystem = FlywheelSubsystem(io)
        val state = RobotState(
            superstructure = SuperstructureState(
                custom = SeasonSuperstructureState(
                    flywheelActive = true,
                    flywheelTargetRPM = 3_500.0
                )
            )
        )

        subsystem.writeOutputs(state, 0.45)

        Mockito.verify(io).setVelocityRpm(3_500.0, 0.45)
    }

    @Test
    fun simultaneousIntakeAndFlywheelIntentFailsClosedAtBothOutputBoundaries() {
        val intake = Mockito.mock(com.areslib.hardware.actuator.IntakeIO::class.java)
        val flywheel = Mockito.mock(FlywheelIO::class.java)
        val state = RobotState(
            superstructure = SuperstructureState(
                custom = SeasonSuperstructureState(
                    intakeActive = true,
                    flywheelActive = true,
                    flywheelTargetRPM = 3_500.0,
                )
            )
        )

        org.firstinspires.ftc.teamcode.subsystems.IntakeSubsystem(intake).writeOutputs(state, 1.0)
        FlywheelSubsystem(flywheel).writeOutputs(state, 1.0)

        Mockito.verify(intake).setRollerVoltage(0.0)
        Mockito.verify(flywheel).setAppliedVoltage(0.0)
        Mockito.verify(flywheel, Mockito.never()).setVelocityRpm(Mockito.anyDouble(), Mockito.anyDouble())
    }

    @Test
    fun flywheelZeroOrNegativeTargetRpmZerosAppliedVoltage() {
        val io = Mockito.mock(FlywheelIO::class.java)
        val subsystem = FlywheelSubsystem(io)
        val stateZero = RobotState(
            superstructure = SuperstructureState(
                custom = SeasonSuperstructureState(
                    flywheelActive = true,
                    flywheelTargetRPM = 0.0
                )
            )
        )
        val stateNegative = RobotState(
            superstructure = SuperstructureState(
                custom = SeasonSuperstructureState(
                    flywheelActive = true,
                    flywheelTargetRPM = -500.0
                )
            )
        )

        subsystem.writeOutputs(stateZero, 1.0)
        subsystem.writeOutputs(stateNegative, 1.0)

        Mockito.verify(io, Mockito.times(2)).setAppliedVoltage(0.0)
        Mockito.verify(io, Mockito.never()).setVelocityRpm(Mockito.anyDouble(), Mockito.anyDouble())
    }
}
