package io.sellmair.pacemaker.wear

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performScrollToIndex
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WearGroupContentTest {
    @get:Rule val compose = createComposeRule()

    private fun openGroup(count: Int) {
        val label = fixtureString(R.string.group_count, count)
        compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText(label))
        compose.onNodeWithText(label).performClick()
    }

    @Test
    fun emptyGroupCanCloseWithoutWorkoutAction() {
        var toggles = 0
        compose.setContent {
            WearUiFixture {
                WearAppContent(wearFixtureState().copy(group = emptyList()), WearFixtureNow, false, { toggles++ })
            }
        }
        openGroup(0)
        compose.onNodeWithText(fixtureString(R.string.wear_group_empty)).assertExists()
        compose.onNodeWithText(fixtureString(R.string.close)).performClick()
        compose.onNodeWithText(fixtureString(R.string.group_count, 0)).assertIsDisplayed()
        compose.runOnIdle { assertEquals(0, toggles) }
    }

    @Test
    fun duplicateRowsDoNotCrashAndCapSummaryCountsUniqueIdentities() {
        val members = listOf(
            WearGroupMember("critical", "Critical member", 160, 150, WearFixtureNow),
            WearGroupMember("critical", "Old duplicate", 220, 140, WearFixtureNow - 16_000L),
        ) + (1..12).map { WearGroupMember("member-$it", "Member $it", 120, 145, WearFixtureNow) }
        compose.setContent {
            WearUiFixture { WearAppContent(wearFixtureState().copy(group = members), WearFixtureNow, false, {}) }
        }
        openGroup(13)
        compose.onNodeWithText(fixtureString(R.string.wear_group_summary, 12, 13)).assertExists()
        compose.onNodeWithText("Critical member").assertExists()
        compose.onNodeWithText(fixtureString(R.string.wear_member_bpm_limit, 160, 150)).assertExists()
        compose.captureWearFixture("group-capped")
    }

    @Test
    fun retainedStaleMemberShowsNoReadingAndItsOwnLimit() {
        val members = listOf(
            WearGroupMember("old", "Old member", 220, 140, WearFixtureNow - 15_001L),
            WearGroupMember("fresh", "Fresh member", 160, 150, WearFixtureNow),
        )
        compose.setContent {
            WearUiFixture { WearAppContent(wearFixtureState().copy(group = members), WearFixtureNow, false, {}) }
        }
        openGroup(2)
        compose.onNodeWithText("Fresh member").assertExists()
        compose.onNodeWithText(fixtureString(R.string.wear_member_bpm_limit, 160, 150)).assertExists()
        compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText(fixtureString(R.string.wear_member_no_reading_limit, 140)))
        compose.onNode(hasScrollToIndexAction()).performScrollToIndex(3)
        compose.onNodeWithText(fixtureString(R.string.wear_member_no_reading_limit, 140)).assertIsDisplayed()
        compose.onNodeWithText("220", substring = true).assertDoesNotExist()
        compose.captureWearFixture("group-stale")
    }
}
