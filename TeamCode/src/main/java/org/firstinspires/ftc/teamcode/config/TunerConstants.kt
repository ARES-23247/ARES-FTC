package org.firstinspires.ftc.teamcode.config

import com.qualcomm.hardware.gobilda.GoBildaPinpointDriver.EncoderDirection

object TunerConstants {
    // Drivetrain Kinematics
    var TRACK_WIDTH_METERS = 0.45
    var WHEEL_BASE_METERS = 0.45
    
    // Drivetrain Path-following Translation PID
    var PATH_TRANSLATION_KP = 2.0
    var PATH_TRANSLATION_KI = 0.0
    var PATH_TRANSLATION_KD = 0.02
    
    // Drivetrain Path-following Rotation PID
    var PATH_ROTATION_KP = 2.5
    var PATH_ROTATION_KI = 0.0
    var PATH_ROTATION_KD = 0.05
    
    // Drive Feedback (Heading Lock PID)
    var HEADING_KP = 4.5
    var HEADING_KI = 0.0
    var HEADING_KD = 0.25
    var HEADING_DEADZONE_DEG = 0.5
    
    // Drivetrain Feedforward (Static friction) & Slew Acceleration Limits
    var DRIVE_KS = 0.0
    var DRIVE_SLEW_RATE_LIMIT: Double? = null
    
    // EKF Odometry Process Noise Covariance (Q diagonal values)
    var ODOM_QX = 0.01
    var ODOM_QY = 0.01
    var ODOM_QTHETA = 0.01
    
    // GoBilda Pinpoint Odometry Pod Physical Offsets (mm) & Configuration
    var PINPOINT_X_OFFSET_MM = 0.0
    var PINPOINT_Y_OFFSET_MM = 0.0
    var PINPOINT_ENCODER_RESOLUTION: Double? = null
    var PINPOINT_X_DIRECTION = EncoderDirection.FORWARD
    var PINPOINT_Y_DIRECTION = EncoderDirection.FORWARD
    
    // Motor Velocity Closed-Loop PIDF (Qualcomm DcMotorEx SDK level)
    var MOTOR_KP: Double? = null
    var MOTOR_KI: Double? = null
    var MOTOR_KD: Double? = null
    var MOTOR_KF: Double? = null
    
    // Vision Filtering baseline standard deviations (X meters, Y meters, Heading radians)
    var VISION_STD_DEVS_X = 0.05
    var VISION_STD_DEVS_Y = 0.05
    var VISION_STD_DEVS_HEADING = 0.1
    
    // Vision Outlier Rejection Thresholds
    var VISION_MAX_DISTANCE_METERS = 6.0
    var VISION_MAX_AMBIGUITY = 0.2
    var VISION_MAHALANOBIS_THRESHOLD = 12.0
}
