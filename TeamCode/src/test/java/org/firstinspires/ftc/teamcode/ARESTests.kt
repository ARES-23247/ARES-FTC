package org.firstinspires.ftc.teamcode

import org.junit.Test
import org.junit.Assert.*
import org.firstinspires.ftc.teamcode.dsl.DEFAULT_SEASON_STATE
import org.firstinspires.ftc.teamcode.dsl.SeasonSuperstructureState
import org.firstinspires.ftc.teamcode.dsl.season
import com.areslib.state.SuperstructureState
/**
 * Documentation for ARESTests
 */

class ARESTests {
    /**
     * Documentation for testDefaultSeasonStateSingleton
     */

    @Test
    fun testDefaultSeasonStateSingleton() {
        /**
         * Documentation for state1
         */
        val state1 = SuperstructureState()
        /**
         * Documentation for season1
         */
        val season1 = state1.season
        /**
         * Documentation for state2
         */
        
        val state2 = SuperstructureState()
        /**
         * Documentation for season2
         */
        val season2 = state2.season
        
        assertSame("Should return the same exact instance to avoid GC allocations", season1, season2)
        assertSame("Should be DEFAULT_SEASON_STATE", DEFAULT_SEASON_STATE, season1)
    }
}
