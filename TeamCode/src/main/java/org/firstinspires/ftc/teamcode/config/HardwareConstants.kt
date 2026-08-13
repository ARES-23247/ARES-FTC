package org.firstinspires.ftc.teamcode.config

/**
 * Season mechanism constants that are not part of the canonical drivebase contract.
 *
 * Drive/localization names, geometry, polarity, and gains live in `.ares/drivetrains` and
 * `.ares/tuning`; generated typed configuration is their only runtime source.
 */
object HardwareConstants {
    // Encoder ticks per motor revolution and nominal-12-V feedforward reference speed.
    const val FLYWHEEL_TICKS_PER_REV = 28.0
    const val FLYWHEEL_MAX_RPM = 6000.0
}
