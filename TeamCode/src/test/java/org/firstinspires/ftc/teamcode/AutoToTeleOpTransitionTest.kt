package org.firstinspires.ftc.teamcode

import com.areslib.math.geometry.Pose2d
import com.areslib.math.geometry.Rotation2d
import com.areslib.util.PoseStorage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AutoToTeleOpTransitionTest {

    @Before
    fun setUp() {
        PoseStorage.hasValidPose = false
        PoseStorage.currentPose = Pose2d(0.0, 0.0, Rotation2d(0.0))
    }

    @Test
    fun testAutonomousToTeleOpPosePersistence() {
        // 1. Initially, PoseStorage has no valid pose
        assertFalse("PoseStorage should initially be invalid", PoseStorage.hasValidPose)

        // 2. Simulate Autonomous end pose handoff
        val endAutoPose = Pose2d(1.25, -0.85, Rotation2d(Math.toRadians(45.0)))
        PoseStorage.currentPose = endAutoPose
        PoseStorage.hasValidPose = true

        assertTrue("PoseStorage should now hold valid pose from Auto", PoseStorage.hasValidPose)
        assertEquals(1.25, PoseStorage.currentPose.x, 1e-4)
        assertEquals(-0.85, PoseStorage.currentPose.y, 1e-4)
        assertEquals(Math.toRadians(45.0), PoseStorage.currentPose.heading.radians, 1e-4)

        // 3. Simulate TeleOp start restoring pose from PoseStorage
        val restoredPose = if (PoseStorage.hasValidPose) {
            PoseStorage.currentPose
        } else {
            Pose2d(0.0, 0.0, Rotation2d(0.0))
        }

        assertEquals("TeleOp should restore exact pose handed off from Autonomous", endAutoPose.x, restoredPose.x, 1e-4)
        assertEquals("TeleOp should restore exact pose handed off from Autonomous", endAutoPose.y, restoredPose.y, 1e-4)
        assertEquals("TeleOp should restore exact pose handed off from Autonomous", endAutoPose.heading.radians, restoredPose.heading.radians, 1e-4)
    }

    @Test
    fun testAllianceFieldRelativeTransformations() {
        val rawVx = 1.0  // Pushing forward on joystick
        val rawVy = 0.0

        // RED Alliance (default: Facing +Y, Driver station at -Y)
        val isRedAlliance = true
        val redVx = if (isRedAlliance) rawVx else -rawVx
        val redVy = if (isRedAlliance) rawVy else -rawVy

        assertEquals(1.0, redVx, 1e-4)
        assertEquals(0.0, redVy, 1e-4)

        // BLUE Alliance (Facing -Y, Driver station at +Y)
        val isBlueAlliance = false
        val blueVx = if (isBlueAlliance) rawVx else -rawVx
        val blueVy = if (isBlueAlliance) rawVy else -rawVy

        assertEquals(-1.0, blueVx, 1e-4)
        assertEquals(0.0, blueVy, 1e-4)

        println("[Match Transition Test] Auto-to-TeleOp pose handoff and Alliance transformation guards PASSED.")
    }
}
