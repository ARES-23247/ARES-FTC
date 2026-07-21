package org.firstinspires.ftc.teamcode

import org.junit.Test
import org.junit.Assert.*
import org.firstinspires.ftc.teamcode.dsl.DEFAULT_SEASON_STATE
import org.firstinspires.ftc.teamcode.dsl.SeasonSuperstructureState
import org.firstinspires.ftc.teamcode.dsl.season
import com.areslib.state.SuperstructureState

class ARESTests {

    @Test
    fun testDefaultSeasonStateSingleton() {
        val state1 = SuperstructureState()
        val season1 = state1.season
        
        val state2 = SuperstructureState()
        val season2 = state2.season
        
        assertSame("Should return the same exact instance to avoid GC allocations", season1, season2)
        assertSame("Should be DEFAULT_SEASON_STATE", DEFAULT_SEASON_STATE, season1)
    }
}
