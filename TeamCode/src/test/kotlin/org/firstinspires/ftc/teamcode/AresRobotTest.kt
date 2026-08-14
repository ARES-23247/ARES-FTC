package org.firstinspires.ftc.teamcode

import org.firstinspires.ftc.teamcode.generated.drivebase.GeneratedAresDrivebaseConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Focused contract tests for generated canonical drivebase configuration and peripheral hardware IDs.
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
    }

    @Test
    fun testDrivetrainMotorHardwareIdConfigurations() {
        // Front-left drive motor
        assertEquals("fl", GeneratedAresDrivebaseConfig.Components.FTC_MOTOR_FL.HARDWARE_ID)
        assertEquals("ftc.motor.fl", GeneratedAresDrivebaseConfig.Components.FTC_MOTOR_FL.UID)
        assertEquals("DRIVE_MOTOR", GeneratedAresDrivebaseConfig.Components.FTC_MOTOR_FL.ROLE)
        assertEquals("REV Control Hub DcMotorEx", GeneratedAresDrivebaseConfig.Components.FTC_MOTOR_FL.CONTROLLER_MODEL)
        assertEquals("Integrated quadrature encoder", GeneratedAresDrivebaseConfig.Components.FTC_MOTOR_FL.ENCODER_MODEL)
        assertTrue(GeneratedAresDrivebaseConfig.Components.FTC_MOTOR_FL.CURRENT_MEASUREMENT_REQUIRED)
        assertTrue(GeneratedAresDrivebaseConfig.Components.FTC_MOTOR_FL.CURRENT_MEASUREMENT_AVAILABLE)
        assertFalse(GeneratedAresDrivebaseConfig.Components.FTC_MOTOR_FL.INVERTED)

        // Front-right drive motor
        assertEquals("fr", GeneratedAresDrivebaseConfig.Components.FTC_MOTOR_FR.HARDWARE_ID)
        assertEquals("ftc.motor.fr", GeneratedAresDrivebaseConfig.Components.FTC_MOTOR_FR.UID)
        assertEquals("DRIVE_MOTOR", GeneratedAresDrivebaseConfig.Components.FTC_MOTOR_FR.ROLE)
        assertEquals("REV Control Hub DcMotorEx", GeneratedAresDrivebaseConfig.Components.FTC_MOTOR_FR.CONTROLLER_MODEL)
        assertEquals("Integrated quadrature encoder", GeneratedAresDrivebaseConfig.Components.FTC_MOTOR_FR.ENCODER_MODEL)
        assertTrue(GeneratedAresDrivebaseConfig.Components.FTC_MOTOR_FR.CURRENT_MEASUREMENT_REQUIRED)
        assertTrue(GeneratedAresDrivebaseConfig.Components.FTC_MOTOR_FR.CURRENT_MEASUREMENT_AVAILABLE)
        assertTrue(GeneratedAresDrivebaseConfig.Components.FTC_MOTOR_FR.INVERTED)

        // Rear-left drive motor
        assertEquals("rl", GeneratedAresDrivebaseConfig.Components.FTC_MOTOR_RL.HARDWARE_ID)
        assertEquals("ftc.motor.rl", GeneratedAresDrivebaseConfig.Components.FTC_MOTOR_RL.UID)
        assertEquals("DRIVE_MOTOR", GeneratedAresDrivebaseConfig.Components.FTC_MOTOR_RL.ROLE)
        assertEquals("REV Control Hub DcMotorEx", GeneratedAresDrivebaseConfig.Components.FTC_MOTOR_RL.CONTROLLER_MODEL)
        assertEquals("Integrated quadrature encoder", GeneratedAresDrivebaseConfig.Components.FTC_MOTOR_RL.ENCODER_MODEL)
        assertTrue(GeneratedAresDrivebaseConfig.Components.FTC_MOTOR_RL.CURRENT_MEASUREMENT_REQUIRED)
        assertTrue(GeneratedAresDrivebaseConfig.Components.FTC_MOTOR_RL.CURRENT_MEASUREMENT_AVAILABLE)
        assertFalse(GeneratedAresDrivebaseConfig.Components.FTC_MOTOR_RL.INVERTED)

        // Rear-right drive motor
        assertEquals("rr", GeneratedAresDrivebaseConfig.Components.FTC_MOTOR_RR.HARDWARE_ID)
        assertEquals("ftc.motor.rr", GeneratedAresDrivebaseConfig.Components.FTC_MOTOR_RR.UID)
        assertEquals("DRIVE_MOTOR", GeneratedAresDrivebaseConfig.Components.FTC_MOTOR_RR.ROLE)
        assertEquals("REV Control Hub DcMotorEx", GeneratedAresDrivebaseConfig.Components.FTC_MOTOR_RR.CONTROLLER_MODEL)
        assertEquals("Integrated quadrature encoder", GeneratedAresDrivebaseConfig.Components.FTC_MOTOR_RR.ENCODER_MODEL)
        assertTrue(GeneratedAresDrivebaseConfig.Components.FTC_MOTOR_RR.CURRENT_MEASUREMENT_REQUIRED)
        assertTrue(GeneratedAresDrivebaseConfig.Components.FTC_MOTOR_RR.CURRENT_MEASUREMENT_AVAILABLE)
        assertTrue(GeneratedAresDrivebaseConfig.Components.FTC_MOTOR_RR.INVERTED)
    }

    @Test
    fun testPinpointOdometryHardwareIdAndLocalizationConfiguration() {
        assertEquals("pinpoint", GeneratedAresDrivebaseConfig.Components.FTC_LOCALIZATION_PINPOINT.HARDWARE_ID)
        assertEquals("ftc.localization.pinpoint", GeneratedAresDrivebaseConfig.Components.FTC_LOCALIZATION_PINPOINT.UID)
        assertEquals("ODOMETRY_SENSOR", GeneratedAresDrivebaseConfig.Components.FTC_LOCALIZATION_PINPOINT.ROLE)
        assertEquals("goBILDA Pinpoint", GeneratedAresDrivebaseConfig.Components.FTC_LOCALIZATION_PINPOINT.CONTROLLER_MODEL)

        assertEquals("ftc.localization.pinpoint-source", GeneratedAresDrivebaseConfig.Localization.PRIMARY_ODOMETRY.UID)
        assertEquals("PINPOINT", GeneratedAresDrivebaseConfig.Localization.PRIMARY_ODOMETRY.KIND)
        assertEquals(listOf("ftc.localization.pinpoint"), GeneratedAresDrivebaseConfig.Localization.PRIMARY_ODOMETRY.COMPONENT_UIDS)
        assertEquals("com.areslib.ftc.drivetrain.PinpointIO", GeneratedAresDrivebaseConfig.Localization.PRIMARY_ODOMETRY.IMPLEMENTATION_CLASS)

        assertEquals("ftc.localization.pinpoint-source", GeneratedAresDrivebaseConfig.Localization.HEADING_SOURCE_UID)
        assertTrue(GeneratedAresDrivebaseConfig.Localization.HEADING_CCW_POSITIVE)
        assertTrue(GeneratedAresDrivebaseConfig.Localization.CACHED_INPUTS_REQUIRED)
    }

    @Test
    fun testImuHardwareIdAndConfiguration() {
        assertEquals("imu", GeneratedAresDrivebaseConfig.Components.FTC_LOCALIZATION_IMU.HARDWARE_ID)
        assertEquals("ftc.localization.imu", GeneratedAresDrivebaseConfig.Components.FTC_LOCALIZATION_IMU.UID)
        assertEquals("GYRO", GeneratedAresDrivebaseConfig.Components.FTC_LOCALIZATION_IMU.ROLE)
        assertEquals("REV Control Hub IMU", GeneratedAresDrivebaseConfig.Components.FTC_LOCALIZATION_IMU.CONTROLLER_MODEL)
    }

    @Test
    fun testLimelightHardwareIdAndVisionConfiguration() {
        assertEquals("limelight", GeneratedAresDrivebaseConfig.Components.FTC_LOCALIZATION_LIMELIGHT.HARDWARE_ID)
        assertEquals("ftc.localization.limelight", GeneratedAresDrivebaseConfig.Components.FTC_LOCALIZATION_LIMELIGHT.UID)
        assertEquals("OTHER", GeneratedAresDrivebaseConfig.Components.FTC_LOCALIZATION_LIMELIGHT.ROLE)

        assertEquals("ftc.localization.limelight-source", GeneratedAresDrivebaseConfig.Localization.VISION_FTC_LOCALIZATION_LIMELIGHT_SOURCE.UID)
        assertEquals("EXTERNAL", GeneratedAresDrivebaseConfig.Localization.VISION_FTC_LOCALIZATION_LIMELIGHT_SOURCE.KIND)
        assertEquals(listOf("ftc.localization.limelight"), GeneratedAresDrivebaseConfig.Localization.VISION_FTC_LOCALIZATION_LIMELIGHT_SOURCE.COMPONENT_UIDS)
        assertEquals("com.areslib.ftc.vision.FtcLimelightIO", GeneratedAresDrivebaseConfig.Localization.VISION_FTC_LOCALIZATION_LIMELIGHT_SOURCE.IMPLEMENTATION_CLASS)
    }

    @Test
    fun testPeripheralNamingUniquenessAndCompleteness() {
        val hardwareIds = listOf(
            GeneratedAresDrivebaseConfig.Components.FTC_MOTOR_FL.HARDWARE_ID,
            GeneratedAresDrivebaseConfig.Components.FTC_MOTOR_FR.HARDWARE_ID,
            GeneratedAresDrivebaseConfig.Components.FTC_MOTOR_RL.HARDWARE_ID,
            GeneratedAresDrivebaseConfig.Components.FTC_MOTOR_RR.HARDWARE_ID,
            GeneratedAresDrivebaseConfig.Components.FTC_LOCALIZATION_PINPOINT.HARDWARE_ID,
            GeneratedAresDrivebaseConfig.Components.FTC_LOCALIZATION_IMU.HARDWARE_ID,
            GeneratedAresDrivebaseConfig.Components.FTC_LOCALIZATION_LIMELIGHT.HARDWARE_ID,
            GeneratedAresDrivebaseConfig.Components.FTC_DRIVEBASE.HARDWARE_ID,
        )

        assertEquals("Expected 8 unique component hardware IDs", 8, hardwareIds.toSet().size)
        assertEquals(
            setOf("fl", "fr", "rl", "rr", "pinpoint", "imu", "limelight", "mecanum"),
            hardwareIds.toSet()
        )
    }
}
