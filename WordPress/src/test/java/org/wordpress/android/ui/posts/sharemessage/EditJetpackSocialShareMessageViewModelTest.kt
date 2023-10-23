package org.wordpress.android.ui.posts.sharemessage

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.R
import org.wordpress.android.ui.posts.sharemessage.EditJetpackSocialShareMessageViewModel.ActionEvent
import org.wordpress.android.ui.posts.sharemessage.EditJetpackSocialShareMessageViewModel.UiState
import org.wordpress.android.util.StringProvider

@ExperimentalCoroutinesApi
class EditJetpackSocialShareMessageViewModelTest : BaseUnitTest() {
    private val stringProvider: StringProvider = mock()
    private val classToTest = EditJetpackSocialShareMessageViewModel(
        stringProvider = stringProvider,
    )
    private val shareMessageMaxLength = 255
    private val title = "Title"
    val description = "Description"

    @Test
    fun `Should emit Loaded with starting share message when start is called`() {
        mockLoadedTitle()
        mockLoadedDescription()

        val shareMessage = "Share"
        classToTest.start(shareMessage)

        val actual = classToTest.uiState.value
        val expected = UiState.Loaded(
            appBarLabel = title,
            currentShareMessage = shareMessage,
            shareMessageMaxLength = shareMessageMaxLength,
            customizeMessageDescription = description,
            onBackClick = classToTest::onBackClick,
        )
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `Should emit Loaded with updated share message when start is called and share message was changed`() {
        mockLoadedTitle()
        mockLoadedDescription()

        val updatedShareMessage = "Updated share message"
        classToTest.updateShareMessage(updatedShareMessage)
        classToTest.start("Share")

        val actual = classToTest.uiState.value
        val expected = UiState.Loaded(
            appBarLabel = title,
            currentShareMessage = updatedShareMessage,
            shareMessageMaxLength = shareMessageMaxLength,
            customizeMessageDescription = description,
            onBackClick = classToTest::onBackClick,
        )
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `Should emit FinishActivity with starting share message when onBackClick is called`() = test {
        mockLoadedTitle()
        mockLoadedDescription()

        val startingShareMessage = "Starting share message"
        classToTest.start(startingShareMessage)
        classToTest.onBackClick()

        val result = ArrayList<ActionEvent>()
        val job = launch {
            classToTest.actionEvents.collectLatest {
                result.add(it)
            }
        }
        val actual = result.first()
        val expected = ActionEvent.FinishActivity(startingShareMessage)
        assertThat(actual).isEqualTo(expected)
        job.cancel()
    }

    @Test
    fun `Should emit FinishActivity with updated share message when onBackClick is called`() = test {
        val updatedShareMessage = "Updated share message"
        classToTest.updateShareMessage(updatedShareMessage)

        classToTest.onBackClick()

        val result = ArrayList<ActionEvent>()
        val job = launch {
            classToTest.actionEvents.collectLatest {
                result.add(it)
            }
        }
        val actual = result.first()
        val expected = ActionEvent.FinishActivity(updatedShareMessage)
        assertThat(actual).isEqualTo(expected)
        job.cancel()
    }

    @Test
    fun `Should get string post_settings_jetpack_social_share_message_title when start is called`() {
        mockLoadedTitle()
        mockLoadedDescription()

        classToTest.start("Message")

        verify(stringProvider).getString(R.string.post_settings_jetpack_social_share_message_title)
    }

    @Test
    fun `Should get string post_settings_jetpack_social_share_message_description when start is called`() {
        mockLoadedTitle()
        mockLoadedDescription()

        classToTest.start("Message")

        verify(stringProvider).getString(R.string.post_settings_jetpack_social_share_message_description)
    }

    private fun mockLoadedDescription() {
        whenever(stringProvider.getString(R.string.post_settings_jetpack_social_share_message_description))
            .thenReturn(description)
    }

    private fun mockLoadedTitle() {
        whenever(stringProvider.getString(R.string.post_settings_jetpack_social_share_message_title))
            .thenReturn(title)
    }
}
