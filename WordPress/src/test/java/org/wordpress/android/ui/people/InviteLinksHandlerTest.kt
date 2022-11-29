package org.wordpress.android.ui.people

import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.flow.flow
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.TEST_DISPATCHER
import org.wordpress.android.test
import org.wordpress.android.ui.pages.SnackbarMessageHolder
import org.wordpress.android.ui.people.InviteLinksUseCase.InviteLinksState
import org.wordpress.android.ui.people.InviteLinksUseCase.InviteLinksState.InviteLinksData
import org.wordpress.android.ui.people.InviteLinksUseCase.InviteLinksState.InviteLinksError
import org.wordpress.android.ui.people.InviteLinksUseCase.UseCaseScenarioContext.GENERATING_LINKS
import org.wordpress.android.ui.people.InviteLinksUseCase.UseCaseScenarioContext.INITIALIZING
import org.wordpress.android.ui.people.InviteLinksUseCase.UseCaseScenarioContext.MANAGING_AVAILABLE_LINKS
import org.wordpress.android.ui.utils.UiString.UiStringText

@InternalCoroutinesApi
class InviteLinksHandlerTest : BaseUnitTest() {
    @Mock lateinit var inviteLinksUseCase: InviteLinksUseCase

    private lateinit var inviteLinksHandler: InviteLinksHandler
    private var uiState: InviteLinksState? = null
    private var holder: SnackbarMessageHolder? = null
    private val blogId = 100L

    @Before
    fun setUp() {
        inviteLinksHandler = InviteLinksHandler(
                inviteLinksUseCase,
                TEST_DISPATCHER
        )
        setupObservers()
    }

    @Test
    fun `manageState populates snackbar on Failure`() = test {
        val errorMessage = UiStringText("error message")

        val state = InviteLinksError(
                scenarioContext = INITIALIZING,
                error = errorMessage
        )

        whenever(
                inviteLinksUseCase.getInviteLinksStatus(blogId, INITIALIZING)
        ).thenReturn(flow { emit(state) })

        inviteLinksHandler.handleInviteLinksStatusRequest(blogId, INITIALIZING)

        requireNotNull(uiState).let {
            assertThat(it).isEqualTo(state)
        }

        requireNotNull(holder).let {
            assertThat(it.message).isEqualTo(errorMessage)
        }
    }

    @Test
    fun `handleInviteLinksStatusRequest collects expected state`() = test {
        val state = InviteLinksData(
                scenarioContext = INITIALIZING,
                links = listOf(mock(), mock())
        )

        whenever(
                inviteLinksUseCase.getInviteLinksStatus(blogId, INITIALIZING)
        ).thenReturn(flow { emit(state) })

        inviteLinksHandler.handleInviteLinksStatusRequest(blogId, INITIALIZING)

        requireNotNull(uiState).let {
            assertThat(it).isEqualTo(state)
        }

        assertThat(holder).isNull()
    }

    @Test
    fun `handleGenerateLinks collects expected state`() = test {
        val state = InviteLinksData(
                scenarioContext = GENERATING_LINKS,
                links = listOf(mock(), mock())
        )

        whenever(inviteLinksUseCase.generateLinks(blogId)).thenReturn(flow { emit(state) })

        inviteLinksHandler.handleGenerateLinks(blogId)

        requireNotNull(uiState).let {
            assertThat(it).isEqualTo(state)
        }

        assertThat(holder).isNull()
    }

    @Test
    fun `handleDisableLinks collects expected state`() = test {
        val state = InviteLinksData(
                scenarioContext = MANAGING_AVAILABLE_LINKS,
                links = listOf()
        )

        whenever(inviteLinksUseCase.disableLinks(blogId)).thenReturn(flow { emit(state) })

        inviteLinksHandler.handleDisableLinks(blogId)

        requireNotNull(uiState).let {
            assertThat(it).isEqualTo(state)
        }

        assertThat(holder).isNull()
    }

    private fun setupObservers() {
        uiState = null

        inviteLinksHandler.inviteLinksState.observeForever {
            uiState = it
        }

        holder = null
        inviteLinksHandler.snackbarEvents.observeForever { event ->
            event.applyIfNotHandled {
                holder = this
            }
        }
    }
}
