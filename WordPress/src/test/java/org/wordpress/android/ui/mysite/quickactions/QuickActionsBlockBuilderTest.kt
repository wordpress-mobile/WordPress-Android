package org.wordpress.android.ui.mysite.quickactions

import kotlinx.coroutines.InternalCoroutinesApi
import org.assertj.core.api.Assertions
import org.junit.Before
import org.junit.Test
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.R.string
import org.wordpress.android.ui.mysite.MySiteItem.QuickActionsBlock
import org.wordpress.android.ui.utils.UiString.UiStringRes

@InternalCoroutinesApi
class QuickActionsBlockBuilderTest : BaseUnitTest() {
    private lateinit var builder: QuickActionsBlockBuilder

    private val onStatsClick: () -> Unit = {}
    private val onPostsClick: () -> Unit = {}
    private val onPagesClick: () -> Unit = {}
    private val onMediaClick: () -> Unit = {}
    private val onRemoveMenuItemClick: () -> Unit = {}

    @Before
    fun setUp() {
        builder = QuickActionsBlockBuilder()
    }

    /* TITLE */

    @Test
    fun `when toolbar is built, then title exists`() {
        val quickActionsBlock = buildQuickActionsBlock()

        Assertions.assertThat(quickActionsBlock.title).isEqualTo(UiStringRes(string.my_site_quick_actions_title))
    }

    /* REMOVE MENU ITEM */

    @Test
    fun `when block is built, then remove menu item click is set on the block`() {
        val quickActionsBlock = buildQuickActionsBlock()

        Assertions.assertThat(quickActionsBlock.onRemoveMenuItemClick).isNotNull
    }

    /* ACTION CLICKS */
    @Test
    fun `when block is built, then action item click are set on the block`() {
        val quickActionsBlock = buildQuickActionsBlock()

        Assertions.assertThat(quickActionsBlock.onStatsClick).isNotNull
        Assertions.assertThat(quickActionsBlock.onPagesClick).isNotNull
        Assertions.assertThat(quickActionsBlock.onPostsClick).isNotNull
        Assertions.assertThat(quickActionsBlock.onMediaClick).isNotNull
    }

    private fun buildQuickActionsBlock(
        showPages: Boolean = true,
        showStatsFocusPoint: Boolean = false,
        showPagesFocusPoint: Boolean = false
    ): QuickActionsBlock {
        return builder.build(
                onRemoveMenuItemClick,
                onStatsClick,
                onPagesClick,
                onPostsClick,
                onMediaClick,
                showPages,
                showStatsFocusPoint,
                showPagesFocusPoint
        )
    }
}
