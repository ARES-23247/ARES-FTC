package org.firstinspires.ftc.teamcode

import com.areslib.Store
import com.areslib.action.RobotAction
import com.areslib.ftc.FtcMecanumRobot
import com.areslib.reducer.rootReducer
import com.areslib.state.Alliance
import com.areslib.state.DriveState
import com.areslib.state.RobotState
import com.areslib.state.SuperstructureState
import org.firstinspires.ftc.teamcode.dsl.SeasonSuperstructureState
import org.firstinspires.ftc.teamcode.dsl.season
import org.firstinspires.ftc.teamcode.opmodes.robot.AresSuperstructureController
import com.areslib.util.RobotClock
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito

/**
 * Tests for AresSuperstructureController verifying toggle dispatches and state transitions.
 * Uses a real Store with real reducers to avoid Mockito deep-stub property chain issues.
 */
class AresSuperstructureControllerTest {

    @Before
    fun useDeterministicClock() {
        RobotClock.useMockTime(1_000L)
    }

    @After
    fun restoreSystemClock() {
        RobotClock.useSystemTime()
    }

    private fun createRobotWithStore(
        alliance: Alliance = Alliance.RED,
        flywheelActive: Boolean = false,
        intakeActive: Boolean = false
    ): FtcMecanumRobot {
        val initialState = RobotState(
            drive = DriveState(alliance = alliance),
            superstructure = SuperstructureState(
                custom = SeasonSuperstructureState(
                    flywheelActive = flywheelActive,
                    intakeActive = intakeActive,
                )
            )
        )
        val store = Store(initialState, ::rootReducer)
        val robot = Mockito.mock(FtcMecanumRobot::class.java, Mockito.RETURNS_DEEP_STUBS)
        Mockito.`when`(robot.store).thenReturn(store)
        return robot
    }

    @Test
    fun testToggleIntakeTurnsIntakeOn() {
        val robot = createRobotWithStore(intakeActive = false)
        val controller = AresSuperstructureController(robot)

        controller.toggleIntake()

        val season = robot.store.state.superstructure.season
        assertTrue("Intake should be turned on", season.intakeActive)
    }

    @Test
    fun testToggleIntakeTurnsIntakeOff() {
        val robot = createRobotWithStore(intakeActive = true)
        val controller = AresSuperstructureController(robot)

        controller.toggleIntake()

        val season = robot.store.state.superstructure.season
        assertFalse("Intake should be turned off", season.intakeActive)
    }

    @Test
    fun intakeStopInsideDebounceWindowIsAlwaysHonored() {
        val robot = createRobotWithStore(intakeActive = false)
        val controller = AresSuperstructureController(robot)

        controller.toggleIntake()
        assertTrue(robot.store.state.superstructure.season.intakeActive)

        RobotClock.setMockTimeMs(1_199L)
        controller.toggleIntake()

        assertFalse("Stopping intake must never be delayed by debounce", robot.store.state.superstructure.season.intakeActive)
    }

    @Test
    fun toggleIntakeStartIsDebounced() {
        val robot = createRobotWithStore(intakeActive = true)
        val controller = AresSuperstructureController(robot)

        // Stop intake at t = 1000ms
        controller.toggleIntake()
        assertFalse(robot.store.state.superstructure.season.intakeActive)

        // Rapid start attempt within 200ms debounce window should be ignored
        RobotClock.setMockTimeMs(1_150L)
        controller.toggleIntake()
        assertFalse("Rapid start attempt within 200ms debounce window must be ignored", robot.store.state.superstructure.season.intakeActive)

        // Advancing clock past 200ms allows intake to start
        RobotClock.setMockTimeMs(1_250L)
        controller.toggleIntake()
        assertTrue("Intake should start after debounce window elapses", robot.store.state.superstructure.season.intakeActive)
    }

    @Test
    fun testToggleShooterTurnsShooterOn() {
        val robot = createRobotWithStore(flywheelActive = false)
        val controller = AresSuperstructureController(robot)

        controller.toggleShooter()

        val season = robot.store.state.superstructure.season
        assertTrue("Shooter should be turned on", season.flywheelActive)
    }

    @Test
    fun testToggleShooterTurnsShooterOff() {
        val robot = createRobotWithStore(flywheelActive = true)
        val controller = AresSuperstructureController(robot)

        controller.toggleShooter()

        val season = robot.store.state.superstructure.season
        assertFalse("Shooter should be turned off", season.flywheelActive)
        assertEquals("Target RPM should be 0.0 when off", 0.0, season.flywheelTargetRPM, 1e-4)
    }

    @Test
    fun shooterCannotStartWhileIntakeIsActive() {
        val robot = createRobotWithStore(flywheelActive = false, intakeActive = true)
        val controller = AresSuperstructureController(robot)

        controller.toggleShooter()

        val season = robot.store.state.superstructure.season
        assertFalse("Shooter interlock must reject startup while intake is active", season.flywheelActive)
        assertEquals(0.0, season.flywheelTargetRPM, 1e-9)
        assertTrue("Rejected shooter intent must not alter the intake", season.intakeActive)
    }

    @Test
    fun shooterStopInsideDebounceWindowIsAlwaysHonored() {
        val robot = createRobotWithStore(flywheelActive = false)
        val controller = AresSuperstructureController(robot)

        controller.toggleShooter()
        assertTrue(robot.store.state.superstructure.season.flywheelActive)

        RobotClock.setMockTimeMs(1_199L)
        controller.toggleShooter()

        assertFalse("Stopping must never be delayed by debounce", robot.store.state.superstructure.season.flywheelActive)
        assertEquals(0.0, robot.store.state.superstructure.season.flywheelTargetRPM, 0.0)
    }

    @Test
    fun startingIntakeAtomicallyStopsShooter() {
        val robot = createRobotWithStore(flywheelActive = true)
        val controller = AresSuperstructureController(robot)

        controller.toggleIntake()

        val season = robot.store.state.superstructure.season
        assertTrue(season.intakeActive)
        assertFalse(season.flywheelActive)
        assertEquals(0.0, season.flywheelTargetRPM, 0.0)
    }

    @Test
    fun staleShooterTargetCanAlwaysBeClearedWhileIntakeIsActive() {
        val robot = createRobotWithStore(intakeActive = true)
        val stale = robot.store.state.superstructure.season.copy(
            flywheelActive = false,
            flywheelTargetRPM = 3_500.0,
        )
        robot.store.dispatch(RobotAction.UpdateSubsystemState(stale))
        val controller = AresSuperstructureController(robot)

        controller.toggleShooter()

        val season = robot.store.state.superstructure.season
        assertTrue(season.intakeActive)
        assertFalse(season.flywheelActive)
        assertEquals(0.0, season.flywheelTargetRPM, 0.0)
    }

    @Test
    fun testToggleAllianceRedToBlue() {
        val robot = createRobotWithStore(alliance = Alliance.RED)
        val controller = AresSuperstructureController(robot)

        controller.toggleAlliance()

        assertEquals("Alliance should be toggled to Blue", Alliance.BLUE, robot.store.state.drive.alliance)
    }

    @Test
    fun testToggleAllianceBlueToRed() {
        val robot = createRobotWithStore(alliance = Alliance.BLUE)
        val controller = AresSuperstructureController(robot)

        controller.toggleAlliance()

        assertEquals("Alliance should be toggled to Red", Alliance.RED, robot.store.state.drive.alliance)
    }

    @Test
    fun testZeroDefaultStateProducesZeroOutputs() {
        val robot = createRobotWithStore(flywheelActive = false, intakeActive = false)

        val season = robot.store.state.superstructure.season
        assertFalse(season.intakeActive)
        assertFalse(season.flywheelActive)
        assertEquals(0.0, season.flywheelTargetRPM, 1e-4)
    }

    @Test
    fun testMotorVoltageOutputsBounded() {
        val robot = createRobotWithStore(flywheelActive = false)
        val controller = AresSuperstructureController(robot)

        controller.toggleShooter()

        val season = robot.store.state.superstructure.season
        assertTrue("RPM should not exceed physical limits", season.flywheelTargetRPM <= 6000.0)
    }

    @Test
    fun testStateMachineTransitions() {
        val robot = createRobotWithStore(flywheelActive = false)
        val controller = AresSuperstructureController(robot)

        // From idle to active
        controller.toggleShooter()
        val state1 = robot.store.state.superstructure.season
        assertTrue(state1.flywheelActive)

        RobotClock.setMockTimeMs(1_250L)

        // From active back to idle
        controller.toggleShooter()
        val state2 = robot.store.state.superstructure.season
        assertFalse(state2.flywheelActive)
        assertEquals(0.0, state2.flywheelTargetRPM, 1e-4)
    }

    @Test
    fun testToggleAllianceDebounceBlocksRapidSuccessiveCalls() {
        val robot = createRobotWithStore(alliance = Alliance.RED)
        val controller = AresSuperstructureController(robot)

        // First call toggles RED -> BLUE
        controller.toggleAlliance()
        assertEquals(Alliance.BLUE, robot.store.state.drive.alliance)

        // Second call at same timestamp should be debounced and ignored
        controller.toggleAlliance()
        assertEquals(Alliance.BLUE, robot.store.state.drive.alliance)

        // Advancing clock past 200ms allows toggle BLUE -> RED
        RobotClock.setMockTimeMs(1_300L)
        controller.toggleAlliance()
        assertEquals(Alliance.RED, robot.store.state.drive.alliance)
    }
}
