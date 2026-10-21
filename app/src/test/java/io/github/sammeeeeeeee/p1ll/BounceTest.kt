package io.github.sammeeeeeeee.p1ll

import android.app.Application
import androidx.compose.foundation.LocalOverscrollFactory
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import io.github.sammeeeeeeee.p1ll.ui.components.BounceOverscrollFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class)
class BounceTest {
    @get:Rule
    val compose = createComposeRule()

    private fun list() = compose.setContent {
        CompositionLocalProvider(LocalOverscrollFactory provides BounceOverscrollFactory) {
            LazyColumn(Modifier.fillMaxSize().testTag("list")) {
                items(60) { Text("Row $it", Modifier.height(48.dp).testTag("row$it")) }
            }
        }
    }

    private fun topOfFirst() = compose.onNodeWithTag("row0").getBoundsInRoot().top.value

    @Test
    fun pullingPastTheTopFollowsTheFingerThenSpringsBack() {
        list()
        compose.onNodeWithTag("list").performTouchInput {
            down(center)
            moveBy(Offset(0f, 60f))
            moveBy(Offset(0f, 240f))
        }
        compose.waitForIdle()
        assertTrue("content should move down while pulled", topOfFirst() > 4f)
        compose.onNodeWithTag("list").performTouchInput { up() }
        compose.mainClock.advanceTimeBy(3_000)
        compose.waitForIdle()
        assertEquals(0f, topOfFirst(), 0.5f)
    }

    @Test
    fun scrollingStillScrolls() {
        list()
        compose.onNodeWithTag("list").performTouchInput {
            down(center)
            moveBy(Offset(0f, -40f))
            moveBy(Offset(0f, -300f))
            up()
        }
        compose.mainClock.advanceTimeBy(3_000)
        compose.waitForIdle()
        val gone = compose.onAllNodesWithTag("row0").fetchSemanticsNodes().isEmpty()
        assertTrue("first row should have scrolled up", gone || topOfFirst() < -100f)
    }
}
