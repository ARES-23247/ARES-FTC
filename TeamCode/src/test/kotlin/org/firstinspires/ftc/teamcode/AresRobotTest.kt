package org.firstinspires.ftc.teamcode

import org.firstinspires.ftc.teamcode.config.TunerConstants
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertNotNull
import org.junit.Test

class AresRobotTest {

    @Test
    fun testTunerConstantsBaseline() {
        // Verify kinematics dimensions
        assertTrue("Track width should be positive", TunerConstants.TRACK_WIDTH_METERS > 0.0)
        assertTrue("Wheelbase should be positive", TunerConstants.WHEEL_BASE_METERS > 0.0)

        // Verify PID coefficients
        assertTrue("Path translation Kp should be positive", TunerConstants.PATH_TRANSLATION_KP > 0.0)
        assertTrue("Path rotation Kp should be positive", TunerConstants.PATH_ROTATION_KP > 0.0)
        assertEquals(0.0, TunerConstants.PATH_TRANSLATION_KI, 1e-6)

        // Verify vision rejection parameters
        assertTrue("Max distance should be reasonable", TunerConstants.VISION_MAX_DISTANCE_METERS in 1.0..15.0)
        assertTrue("Max ambiguity should be reasonable", TunerConstants.VISION_MAX_AMBIGUITY in 0.0..1.0)
        assertTrue("Mahalanobis threshold should be reasonable", TunerConstants.VISION_MAHALANOBIS_THRESHOLD > 0.0)
    }

    @Test
    fun testAresTeleOpBuilder() {
        val builder = org.firstinspires.ftc.teamcode.dsl.AresTeleOpBuilder()
        var initCalled = false
        var loopCalled = false

        builder.onInit { _, _ -> initCalled = true }
        builder.onLoop { _, _, _ -> loopCalled = true }

        assertNotNull("onInitBlock should be configured", builder.onInitBlock)
        assertNotNull("onLoopBlock should be configured", builder.onLoopBlock)
    }

    @Test
    fun testTestPathAutoMetadata() {
        assertEquals("TestPath", TestPathAuto.pathName)
    }
}
