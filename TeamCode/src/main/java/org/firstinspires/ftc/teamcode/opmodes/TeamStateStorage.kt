package org.firstinspires.ftc.teamcode.opmodes

/** Process-local season state passed between consecutive OpModes; cleared by an RC restart. */
object TeamStateStorage {
    /** Last observed lift height in the mechanism's configured distance unit. */
    var liftHeight: Double = 0.0
}
