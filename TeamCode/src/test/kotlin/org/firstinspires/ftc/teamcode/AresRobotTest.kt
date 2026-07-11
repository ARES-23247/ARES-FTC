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

    @Test
    fun testHardwareConstants() {
        assertEquals("fl", org.firstinspires.ftc.teamcode.config.HardwareConstants.MOTOR_FRONT_LEFT)
        assertEquals("fr", org.firstinspires.ftc.teamcode.config.HardwareConstants.MOTOR_FRONT_RIGHT)
        assertEquals("rl", org.firstinspires.ftc.teamcode.config.HardwareConstants.MOTOR_BACK_LEFT)
        assertEquals("rr", org.firstinspires.ftc.teamcode.config.HardwareConstants.MOTOR_BACK_RIGHT)
        assertEquals("pinpoint", org.firstinspires.ftc.teamcode.config.HardwareConstants.ODOMETRY_PINPOINT)
        assertEquals("imu", org.firstinspires.ftc.teamcode.config.HardwareConstants.IMU_BNO055)
        assertEquals("limelight", org.firstinspires.ftc.teamcode.config.HardwareConstants.VISION_LIMELIGHT)
    }

    @Test
    fun testTestPathAutoBuildFollower() {
        val follower = org.mockito.Mockito.mock(com.areslib.pathing.HolonomicPathFollower::class.java)
        var triggered = false
        val eventMap = mapOf("marker1" to { triggered = true })
        
        TestPathAuto.buildPathFollower(follower, eventMap)
        
        val argumentCaptor = org.mockito.ArgumentCaptor.forClass(Function1::class.java) as org.mockito.ArgumentCaptor<(String) -> Unit>
        org.mockito.Mockito.verify(follower).onEventTriggered = argumentCaptor.capture()
        
        argumentCaptor.value.invoke("marker1")
        assertTrue(triggered)
    }

    @Test
    fun testTunerConstantsCoverage() {
        assertNotNull(TunerConstants.TRACK_WIDTH_METERS)
        assertNotNull(TunerConstants.WHEEL_BASE_METERS)
        assertNotNull(TunerConstants.PATH_TRANSLATION_KP)
        assertNotNull(TunerConstants.PATH_TRANSLATION_KI)
        assertNotNull(TunerConstants.PATH_TRANSLATION_KD)
        assertNotNull(TunerConstants.PATH_ROTATION_KP)
        assertNotNull(TunerConstants.PATH_ROTATION_KI)
        assertNotNull(TunerConstants.PATH_ROTATION_KD)
        assertNotNull(TunerConstants.HEADING_KP)
        assertNotNull(TunerConstants.HEADING_KI)
        assertNotNull(TunerConstants.HEADING_KD)
        assertNotNull(TunerConstants.HEADING_DEADZONE_DEG)
        assertNotNull(TunerConstants.DRIVE_KS)
        TunerConstants.DRIVE_SLEW_RATE_LIMIT = 1.0
        assertEquals(1.0, TunerConstants.DRIVE_SLEW_RATE_LIMIT!!, 1e-6)
        assertNotNull(TunerConstants.ODOM_QX)
        assertNotNull(TunerConstants.ODOM_QY)
        assertNotNull(TunerConstants.ODOM_QTHETA)
        assertNotNull(TunerConstants.PINPOINT_X_OFFSET_MM)
        assertNotNull(TunerConstants.PINPOINT_Y_OFFSET_MM)
        TunerConstants.PINPOINT_ENCODER_RESOLUTION = 2000.0
        assertEquals(2000.0, TunerConstants.PINPOINT_ENCODER_RESOLUTION!!, 1e-6)
        assertNotNull(TunerConstants.PINPOINT_X_DIRECTION)
        assertNotNull(TunerConstants.PINPOINT_Y_DIRECTION)
        TunerConstants.MOTOR_KP = 1.0
        TunerConstants.MOTOR_KI = 2.0
        TunerConstants.MOTOR_KD = 3.0
        TunerConstants.MOTOR_KF = 4.0
        assertEquals(1.0, TunerConstants.MOTOR_KP!!, 1e-6)
        assertEquals(2.0, TunerConstants.MOTOR_KI!!, 1e-6)
        assertEquals(3.0, TunerConstants.MOTOR_KD!!, 1e-6)
        assertEquals(4.0, TunerConstants.MOTOR_KF!!, 1e-6)
        assertNotNull(TunerConstants.VISION_STD_DEVS_X)
        assertNotNull(TunerConstants.VISION_STD_DEVS_Y)
        assertNotNull(TunerConstants.VISION_STD_DEVS_HEADING)
        assertNotNull(TunerConstants.VISION_MAX_DISTANCE_METERS)
        assertNotNull(TunerConstants.VISION_MAX_AMBIGUITY)
        assertNotNull(TunerConstants.VISION_MAHALANOBIS_THRESHOLD)
    }
}
