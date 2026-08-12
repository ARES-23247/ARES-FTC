package org.firstinspires.ftc.teamcode

import com.areslib.pathing.CommandKey
import com.areslib.pathing.NamedCommands
import com.areslib.action.RobotAction
import com.areslib.hardware.actuator.PrismPwmPreset
import org.firstinspires.ftc.teamcode.dsl.FtcAutoCapabilities
import org.firstinspires.ftc.teamcode.dsl.requireFtcDriveActionsAvailable
import com.areslib.routine.RoutineDriveMarker
import com.areslib.routine.RoutineDriveStep
import com.areslib.routine.RoutinePose
import com.areslib.routine.AutonomousCatalogEntry
import com.areslib.routine.RoutineDocument
import com.areslib.routine.RoutineStep
import com.areslib.state.Alliance
import org.firstinspires.ftc.teamcode.dsl.FtcFieldEnvelope
import org.firstinspires.ftc.teamcode.dsl.validateFtcAutonomousBounds
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FtcAutoCapabilitiesTest {
    @Before
    fun clearBefore() = NamedCommands.clear()

    @After
    fun clearAfter() = NamedCommands.clear()

    @Test
    fun `live capabilities contain only discovered mechanism and indicator hardware`() {
        FtcAutoCapabilities.registerMechanismActions(intakeAvailable = false, flywheelAvailable = false)
        FtcAutoCapabilities.registerIndicatorActions(primaryAvailable = false, secondaryAvailable = false)
        assertFalse(NamedCommands.contains(CommandKey("intake.collect")))
        assertFalse(NamedCommands.contains(CommandKey("flywheel.prepare")))
        assertFalse(NamedCommands.contains(CommandKey("SetIndicatorColor_GREEN")))

        FtcAutoCapabilities.registerMechanismActions(intakeAvailable = true, flywheelAvailable = false)
        FtcAutoCapabilities.registerIndicatorActions(primaryAvailable = false, secondaryAvailable = true)
        assertTrue(NamedCommands.contains(CommandKey("intake.collect")))
        assertTrue(NamedCommands.contains(CommandKey("intake.stop")))
        assertFalse(NamedCommands.contains(CommandKey("flywheel.prepare")))
        assertFalse(NamedCommands.contains(CommandKey("SetIndicatorColor_GREEN")))
        assertTrue(NamedCommands.contains(CommandKey("SetSecondIndicatorColor_GREEN")))
    }

    @Test
    fun `Prism capabilities are hardware gated and dispatch immutable Redux actions`() {
        val rainbowKey = CommandKey("SetPrismPreset_RAINBOW_FULL_COLOR")
        FtcAutoCapabilities.registerPrismActions(prismAvailable = false)
        assertFalse(NamedCommands.contains(rainbowKey))

        FtcAutoCapabilities.registerPrismActions(prismAvailable = true)
        assertTrue(NamedCommands.contains(rainbowKey))
        assertTrue(NamedCommands.contains(CommandKey("SetPrismPreset_SOLID_OFF")))

        val task = requireNotNull(NamedCommands.create(rainbowKey, 0L))
        val action = task.initialize(com.areslib.state.RobotState()).single() as RobotAction.SetPrismDriver
        assertEquals("prism", action.name)
        assertEquals(PrismPwmPreset.RAINBOW_FULL_COLOR.pulseWidthUs, action.pulseWidthUs)
    }

    @Test
    fun `drive marker is rejected before motion when its hardware capability is absent`() {
        val step = RoutineDriveStep(
            target = RoutinePose(0.5, 0.0, 0.0),
            markers = listOf(RoutineDriveMarker(0.5, "SetIndicatorColor_GREEN")),
        )
        org.junit.Assert.assertThrows(IllegalArgumentException::class.java) {
            requireFtcDriveActionsAvailable(step)
        }

        FtcAutoCapabilities.registerIndicatorActions(primaryAvailable = true, secondaryAvailable = false)
        requireFtcDriveActionsAvailable(step)
    }

    @Test
    fun `init preflight reports direct action unavailable on discovered hardware`() {
        val entry = AutonomousCatalogEntry(
            entryId = "test",
            displayName = "test",
            routineId = "test",
            startingPose = RoutinePose(0.0, 0.0, 0.0),
        )
        val routine = RoutineDocument(
            documentId = "test",
            name = "test",
            steps = listOf(RoutineStep.action("intake.stop")),
        )
        fun errors() = validateFtcAutonomousBounds(
            entry = entry,
            routines = mapOf("test" to routine),
            envelope = FtcFieldEnvelope(4.0, 4.0, 0.4, 0.4),
            selectedAlliance = Alliance.RED,
            obstacles = emptyList(),
        )

        assertTrue(errors().single().contains("unavailable on discovered FTC hardware"))
        FtcAutoCapabilities.registerMechanismActions(intakeAvailable = true, flywheelAvailable = false)
        assertTrue(errors().isEmpty())
    }

    @Test
    fun `init preflight rejects unsupported FTC drive preset and engine`() {
        val entry = entryFor("test")
        val routine = RoutineDocument(
            documentId = "test",
            name = "test",
            steps = listOf(
                RoutineStep.driveTo(
                    RoutineDriveStep(
                        target = RoutinePose(0.5, 0.0, 0.0),
                        motionPresetKey = "warp",
                        preferredEngineKey = "engine-that-ftc-does-not-implement",
                    )
                )
            ),
        )

        val errors = preflight(entry, routine)
        assertTrue(errors.any { it.contains("Unknown FTC motion preset") })
        assertTrue(errors.any { it.contains("preferred trajectory engine") })
    }

    @Test
    fun `init preflight rejects drives that a race or deadline can interrupt`() {
        val drive = RoutineStep.driveTo(RoutineDriveStep(target = RoutinePose(0.5, 0.0, 0.0)))
        val following = RoutineStep.driveTo(RoutineDriveStep(target = RoutinePose(0.8, 0.0, 0.0)))
        val raceRoutine = RoutineDocument(
            documentId = "race",
            name = "race",
            steps = listOf(
                RoutineStep.firstToFinish(listOf(RoutineStep.wait(0.1), drive)),
                following,
            ),
        )
        val deadlineRoutine = RoutineDocument(
            documentId = "deadline",
            name = "deadline",
            steps = listOf(
                RoutineStep.deadline(RoutineStep.wait(0.1), listOf(drive)),
                following,
            ),
        )

        assertTrue(preflight(entryFor("race"), raceRoutine).any { it.contains("indeterminate pose") })
        assertTrue(preflight(entryFor("deadline"), deadlineRoutine).any { it.contains("interrupted by its deadline") })
    }

    private fun entryFor(id: String) = AutonomousCatalogEntry(
        entryId = id,
        displayName = id,
        routineId = id,
        startingPose = RoutinePose(0.0, 0.0, 0.0),
    )

    private fun preflight(entry: AutonomousCatalogEntry, routine: RoutineDocument): List<String> =
        validateFtcAutonomousBounds(
            entry = entry,
            routines = mapOf(routine.documentId to routine),
            envelope = FtcFieldEnvelope(4.0, 4.0, 0.4, 0.4),
            selectedAlliance = Alliance.RED,
            obstacles = emptyList(),
        )
}
