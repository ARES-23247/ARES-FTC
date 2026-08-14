package org.firstinspires.ftc.teamcode

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.firstinspires.ftc.teamcode.generated.drivebase.GeneratedAresDrivebaseConfig

/**
 * Focused contract tests for generated canonical drivebase configuration.
 */
class AresRobotTest {
    @Test
    fun testCanonicalDrivebaseConfiguration() {
        assertEquals("fl", GeneratedAresDrivebaseConfig.Components.FTC_MOTOR_FL.HARDWARE_ID)
        assertEquals("fr", GeneratedAresDrivebaseConfig.Components.FTC_MOTOR_FR.HARDWARE_ID)
        assertEquals("rl", GeneratedAresDrivebaseConfig.Components.FTC_MOTOR_RL.HARDWARE_ID)
        assertEquals("rr", GeneratedAresDrivebaseConfig.Components.FTC_MOTOR_RR.HARDWARE_ID)
        assertEquals("pinpoint", GeneratedAresDrivebaseConfig.Components.FTC_LOCALIZATION_PINPOINT.HARDWARE_ID)
        assertEquals("imu", GeneratedAresDrivebaseConfig.Components.FTC_LOCALIZATION_IMU.HARDWARE_ID)
        assertEquals("limelight", GeneratedAresDrivebaseConfig.Components.FTC_LOCALIZATION_LIMELIGHT.HARDWARE_ID)
        assertEquals("mecanum", GeneratedAresDrivebaseConfig.Components.FTC_DRIVEBASE.HARDWARE_ID)

        // Motor inversion polarities
        assertFalse(GeneratedAresDrivebaseConfig.Components.FTC_MOTOR_FL.INVERTED)
        assertTrue(GeneratedAresDrivebaseConfig.Components.FTC_MOTOR_FR.INVERTED)
        assertFalse(GeneratedAresDrivebaseConfig.Components.FTC_MOTOR_RL.INVERTED)
        assertTrue(GeneratedAresDrivebaseConfig.Components.FTC_MOTOR_RR.INVERTED)

        // Geometry dimensions and CCW positive standard
        assertEquals(0.096, GeneratedAresDrivebaseConfig.WHEEL_DIAMETER_METERS, 1e-6)
        assertEquals(0.45, GeneratedAresDrivebaseConfig.TRACK_WIDTH_METERS, 1e-6)
        assertEquals(0.45, GeneratedAresDrivebaseConfig.WHEEL_BASE_METERS, 1e-6)
        assertTrue(GeneratedAresDrivebaseConfig.Localization.HEADING_CCW_POSITIVE)
    }
}
