package org.firstinspires.ftc.teamcode.config

/**
 * Canonical FTC Robot Controller configuration names and season mechanism constants.
 *
 * Drive code and diagnostics require rear-motor names `rl` and `rr`. Encoder and RPM values
 * describe the currently installed bare goBILDA flywheel motor and must be retuned if it changes.
 */
object HardwareConstants {
    // Drivetrain hardware-map names.
    const val MOTOR_FRONT_LEFT = "fl"
    const val MOTOR_FRONT_RIGHT = "fr"
    const val MOTOR_BACK_LEFT = "rl"
    const val MOTOR_BACK_RIGHT = "rr"
    
    // Localization hardware-map names.
    const val ODOMETRY_PINPOINT = "pinpoint"
    
    const val IMU_BNO055 = "imu"
    const val VISION_LIMELIGHT = "limelight"

    // Encoder ticks per motor revolution and nominal-12-V feedforward reference speed.
    const val FLYWHEEL_TICKS_PER_REV = 28.0
    const val FLYWHEEL_MAX_RPM = 6000.0
}
