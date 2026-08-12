package org.firstinspires.ftc.teamcode

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Focused contract tests for canonical hardware-map constants.
 */
class AresRobotTest {
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
}
