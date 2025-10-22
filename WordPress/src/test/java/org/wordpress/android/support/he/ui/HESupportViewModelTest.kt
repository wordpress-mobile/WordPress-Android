package org.wordpress.android.support.he.ui

import app.cash.turbine.test
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.fluxc.model.AccountModel
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.support.he.model.SupportConversation
import org.wordpress.android.support.he.model.SupportMessage
import java.util.Date

@ExperimentalCoroutinesApi
class HESupportViewModelTest : BaseUnitTest() {
    @Mock
    lateinit var accountStore: AccountStore

    @Mock
    lateinit var account: AccountModel

    private lateinit var viewModel: HESupportViewModel

    @Before
    fun setUp() {
        viewModel = HESupportViewModel(
            accountStore = accountStore
        )
    }

    // region init() tests

    @Test
    fun `init loads user info when account exists`() {
        // Given
        val displayName = "Test User"
        val email = "test@example.com"
        val avatarUrl = "https://example.com/avatar.jpg"

        whenever(accountStore.account).thenReturn(account)
        whenever(account.displayName).thenReturn(displayName)
        whenever(account.email).thenReturn(email)
        whenever(account.avatarUrl).thenReturn(avatarUrl)

        // When
        viewModel.init()

        // Then
        assertThat(viewModel.userInfo.value.userName).isEqualTo(displayName)
        assertThat(viewModel.userInfo.value.userEmail).isEqualTo(email)
        assertThat(viewModel.userInfo.value.avatarUrl).isEqualTo(avatarUrl)
    }

    @Test
    fun `init uses userName when displayName is empty`() {
        // Given
        val userName = "testuser"
        val email = "test@example.com"

        whenever(accountStore.account).thenReturn(account)
        whenever(account.displayName).thenReturn("")
        whenever(account.userName).thenReturn(userName)
        whenever(account.email).thenReturn(email)
        whenever(account.avatarUrl).thenReturn("")

        // When
        viewModel.init()

        // Then
        assertThat(viewModel.userInfo.value.userName).isEqualTo(userName)
    }

    @Test
    fun `init sets avatarUrl to null when empty`() {
        // Given
        whenever(accountStore.account).thenReturn(account)
        whenever(account.displayName).thenReturn("Test User")
        whenever(account.email).thenReturn("test@example.com")
        whenever(account.avatarUrl).thenReturn("")

        // When
        viewModel.init()

        // Then
        assertThat(viewModel.userInfo.value.avatarUrl).isNull()
    }

    // endregion

    // region onConversationClick() tests

    @Test
    fun `onConversationClick updates selected conversation`() {
        // Given
        val conversation = createTestConversation()

        // When
        viewModel.onConversationClick(conversation)

        // Then
        assertThat(viewModel.selectedConversation.value).isEqualTo(conversation)
    }

    @Test
    fun `onConversationClick emits NavigateToConversationDetail event`() = test {
        // Given
        val conversation = createTestConversation()

        // When
        viewModel.navigationEvents.test {
            viewModel.onConversationClick(conversation)

            // Then
            val event = awaitItem()
            assertThat(event).isInstanceOf(HESupportViewModel.NavigationEvent.NavigateToConversationDetail::class.java)
            val navigateEvent = event as HESupportViewModel.NavigationEvent.NavigateToConversationDetail
            assertThat(navigateEvent.conversation).isEqualTo(conversation)
        }
    }

    // endregion

    // region onBackFromDetailClick() tests

    @Test
    fun `onBackFromDetailClick emits NavigateBack event`() = test {
        // When
        viewModel.navigationEvents.test {
            viewModel.onBackFromDetailClick()

            // Then
            val event = awaitItem()
            assertThat(event).isEqualTo(HESupportViewModel.NavigationEvent.NavigateBack)
        }
    }

    // endregion

    // region onCreateNewConversation() tests

    @Test
    fun `onCreateNewConversation emits NavigateToNewTicket event`() = test {
        // When
        viewModel.navigationEvents.test {
            viewModel.onCreateNewConversation()

            // Then
            val event = awaitItem()
            assertThat(event).isEqualTo(HESupportViewModel.NavigationEvent.NavigateToNewTicket)
        }
    }

    // endregion

    // region onSendNewConversation() tests

    @Test
    fun `onSendNewConversation emits NavigateBack event`() = test {
        // When
        viewModel.navigationEvents.test {
            viewModel.onSendNewConversation()

            // Then
            val event = awaitItem()
            assertThat(event).isEqualTo(HESupportViewModel.NavigationEvent.NavigateBack)
        }
    }

    // endregion

    // region StateFlow initial values tests

    @Test
    fun `conversations is empty before init`() {
        // Then
        assertThat(viewModel.conversations.value).isEmpty()
    }

    @Test
    fun `selectedConversation is null before init`() {
        // Then
        assertThat(viewModel.selectedConversation.value).isNull()
    }

    @Test
    fun `userInfo has correct initial values before init`() {
        // Then
        assertThat(viewModel.userInfo.value.userName).isEmpty()
        assertThat(viewModel.userInfo.value.userEmail).isEmpty()
        assertThat(viewModel.userInfo.value.avatarUrl).isNull()
    }

    // endregion

    // region Navigation event sequence tests

    @Test
    fun `can navigate to detail and back in sequence`() = test {
        // Given
        val conversation = createTestConversation()

        // When
        viewModel.navigationEvents.test {
            viewModel.onConversationClick(conversation)
            val firstEvent = awaitItem()

            viewModel.onBackFromDetailClick()
            val secondEvent = awaitItem()

            // Then
            assertThat(firstEvent)
                .isInstanceOf(HESupportViewModel.NavigationEvent.NavigateToConversationDetail::class.java)
            assertThat(secondEvent).isEqualTo(HESupportViewModel.NavigationEvent.NavigateBack)
        }
    }

    @Test
    fun `can create new ticket and send in sequence`() = test {
        // When
        viewModel.navigationEvents.test {
            viewModel.onCreateNewConversation()
            val firstEvent = awaitItem()

            viewModel.onSendNewConversation()
            val secondEvent = awaitItem()

            // Then
            assertThat(firstEvent).isEqualTo(HESupportViewModel.NavigationEvent.NavigateToNewTicket)
            assertThat(secondEvent).isEqualTo(HESupportViewModel.NavigationEvent.NavigateBack)
        }
    }

    // endregion

    // region Multiple conversation selection tests

    @Test
    fun `selecting different conversations updates selectedConversation`() {
        // Given
        val conversation1 = createTestConversation(id = 1L, title = "First")
        val conversation2 = createTestConversation(id = 2L, title = "Second")

        // When
        viewModel.onConversationClick(conversation1)
        val firstSelection = viewModel.selectedConversation.value

        viewModel.onConversationClick(conversation2)
        val secondSelection = viewModel.selectedConversation.value

        // Then
        assertThat(firstSelection).isEqualTo(conversation1)
        assertThat(secondSelection).isEqualTo(conversation2)
        assertThat(secondSelection).isNotEqualTo(firstSelection)
    }

    // endregion

    // Helper methods

    private fun createTestConversation(
        id: Long = 1L,
        title: String = "Test Conversation",
        description: String = "Test Description"
    ): SupportConversation {
        return SupportConversation(
            id = id,
            title = title,
            description = description,
            lastMessageSentAt = Date(System.currentTimeMillis()),
            messages = listOf(
                SupportMessage(
                    id = 1L,
                    text = "Test message",
                    createdAt = Date(System.currentTimeMillis()),
                    authorName = "Test Author",
                    authorIsUser = true
                )
            )
        )
    }
}
