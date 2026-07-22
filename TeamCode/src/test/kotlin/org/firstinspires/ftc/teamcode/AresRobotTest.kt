package org.firstinspires.ftc.teamcode

import org.firstinspires.ftc.teamcode.opmodes.TestPathAuto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertNotNull
import org.junit.Test
/**
 * Documentation for AresRobotTest
 */

class AresRobotTest {
    /**
     * Documentation for testTestPathAutoMetadata
     */





    @Test
    fun testTestPathAutoMetadata() {
        assertEquals("TestPath", TestPathAuto.pathName)
    }
    /**
     * Documentation for testHardwareConstants
     */

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
    /**
     * Documentation for testTestPathAutoBuildFollower
     */

    @Test
    fun testTestPathAutoBuildFollower() {
        /**
         * Documentation for follower
         */
        val follower = org.mockito.Mockito.mock(com.areslib.pathing.HolonomicPathFollower::class.java)
        /**
         * Documentation for triggered
         */
        var triggered = false
        /**
         * Documentation for eventMap
         */
        val eventMap = mapOf("marker1" to { triggered = true })
        
        TestPathAuto.buildPathFollower(follower, eventMap)
        /**
         * Documentation for argumentCaptor
         */
        
        val argumentCaptor = org.mockito.ArgumentCaptor.forClass(Function1::class.java) as org.mockito.ArgumentCaptor<(String) -> Unit>
        org.mockito.Mockito.verify(follower).onEventTriggered = argumentCaptor.capture()
        
        argumentCaptor.value.invoke("marker1")
        assertTrue(triggered)
    }


}
