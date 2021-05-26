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
import org.wordpress.android.ui.bloggingreminders.BloggingRemindersItem.PrimaryButton
import org.wordpress.android.ui.bloggingreminders.BloggingRemindersItem.Text
import org.wordpress.android.ui.bloggingreminders.BloggingRemindersItem.Title
import org.wordpress.android.ui.utils.UiString.UiStringRes
import org.wordpress.android.ui.utils.UiString.UiStringText

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
        assertTitle(state[2])
        assertText(state[3])
        assertPrimaryButton(state[4])
    }

    private fun assertCloseButton(item: BloggingRemindersItem) {
        val closeButton = item as CloseButton
        closeButton.onClick.click()
        assertThat(events.last()).isFalse()
    }

    private fun assertIllustration(item: BloggingRemindersItem) {
        val illustration = item as Illustration
        // TODO replace this assert with real illustration
        assertThat(illustration.illustration).isEqualTo(R.drawable.img_illustration_cloud_off_152dp)
    }

    private fun assertTitle(item: BloggingRemindersItem) {
        val title = item as Title
        // TODO replace this assert with real copy
        assertThat((title.text as UiStringText).text).isEqualTo("Set your blogging goals!")
    }

    private fun assertText(item: BloggingRemindersItem) {
        val title = item as Text
        // TODO replace this assert with real copy
        assertThat((title.text as UiStringText).text).isEqualTo("Well done on your first post! Keep it going.")
    }

    private fun assertPrimaryButton(item: BloggingRemindersItem) {
        val closeButton = item as PrimaryButton
        assertThat((closeButton.text as UiStringRes).stringRes).isEqualTo(R.string.get_started)
    }
}
