package org.vicpoo.gasolineproject.core.sync

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.vicpoo.gasolineproject.core.common.AppScope
import org.vicpoo.gasolineproject.testing.FakeSessionReader
import org.vicpoo.gasolineproject.testing.testSession
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class BackgroundSyncCoordinatorTest {

    private class FakeScheduler : BackgroundSyncScheduler {
        val calls = mutableListOf<String>()
        override fun schedulePeriodic() { calls += "periodic" }
        override fun requestWhenOnline() { calls += "online" }
        override fun cancel() { calls += "cancel" }
    }

    @Test
    fun schedulesWhileLoggedIn_andCancelsOnLogout() = runTest {
        val sessions = FakeSessionReader(initial = null)
        val scheduler = FakeScheduler()
        val scope = AppScope(backgroundScope + UnconfinedTestDispatcher(testScheduler))

        BackgroundSyncCoordinator(sessions, scheduler, scope)
        assertEquals(listOf("cancel"), scheduler.calls)

        sessions.state.value = testSession()
        assertEquals(listOf("cancel", "periodic", "online"), scheduler.calls)

        sessions.state.value = null
        assertEquals(listOf("cancel", "periodic", "online", "cancel"), scheduler.calls)
    }
}
