package org.wordpress.android.ui.bloggingreminders

import com.nhaarman.mockitokotlin2.verify
import kotlinx.coroutines.InternalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.R
import org.wordpress.android.TEST_DISPATCHER
import org.wordpress.android.eventToList
import org.wordpress.android.toList
import org.wordpress.android.ui.bloggingreminders.BloggingRemindersItem.CloseButton
import org.wordpress.android.ui.bloggingreminders.BloggingRemindersItem.Illustration

class BloggingRemindersViewModelTest : BaseUnitTest() {
    @Mock lateinit var bloggingRemindersManager: BloggingRemindersManager
    private lateinit var viewModel: BloggingRemindersViewModel
    private val siteId = 123
    private lateinit var events: MutableList<Boolean>
    private lateinit var uiState: MutableList<List<BloggingRemindersItem>>

    @InternalCoroutinesApi
    @Before
    fun setUp() {
        viewModel = BloggingRemindersViewModel(bloggingRemindersManager, TEST_DISPATCHER)
        events = mutableListOf()
        events = viewModel.isBottomSheetShowing.eventToList()
        uiState = viewModel.uiState.toList()
    }

    @Test
    fun `sets blogging reminders as shown on start`() {
        viewModel.start(siteId)

        verify(bloggingRemindersManager).bloggingRemindersShown(siteId)
    }

    @Test
    fun `shows bottom sheet on start`() {
        viewModel.start(siteId)

        assertThat(events).containsExactly(true)
    }

    @Test
    fun `shows ui state on start`() {
        viewModel.start(siteId)

        val state = uiState.last()

        assertCloseButton(state[0])
        assertIllustration(state[1])
    }

    private fun assertCloseButton(item: BloggingRemindersItem) {
        val closeButton = item as CloseButton
        closeButton.listItemInteraction.click()
        assertThat(events.last()).isFalse()
    }

    private fun assertIllustration(item: BloggingRemindersItem) {
        val illustration = item as Illustration
        // TODO replace this assert with real illustration
        assertThat(illustration.illustration).isEqualTo(R.drawable.img_illustration_cloud_off_152dp)
    }
}
