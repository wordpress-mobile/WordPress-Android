package org.wordpress.android.ui.people

import android.content.Context
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.InternalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.ArgumentMatchers.anyList
import org.mockito.ArgumentMatchers.anyLong
import org.mockito.Mock
import org.mockito.kotlin.eq
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.TEST_DISPATCHER
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.models.InvitePeopleUtils
import org.wordpress.android.ui.pages.SnackbarMessageHolder
import org.wordpress.android.ui.people.InviteLinksApiCallsProvider.InviteLinksItem
import org.wordpress.android.ui.people.InviteLinksUiStateType.HIDDEN
import org.wordpress.android.ui.people.InviteLinksUiStateType.LOADING
import org.wordpress.android.ui.people.InviteLinksUseCase.InviteLinksState
import org.wordpress.android.ui.people.InviteLinksUseCase.InviteLinksState.InviteLinksData
import org.wordpress.android.ui.people.InviteLinksUseCase.UseCaseScenarioContext.INITIALIZING
import org.wordpress.android.ui.utils.UiString.UiStringText
import org.wordpress.android.util.DateTimeUtilsWrapper
import org.wordpress.android.util.analytics.AnalyticsUtilsWrapper
import org.wordpress.android.viewmodel.ContextProvider
import org.wordpress.android.viewmodel.Event

@InternalCoroutinesApi
class PeopleInviteViewModelTest : BaseUnitTest() {
    @Mock lateinit var inviteLinksHandler: InviteLinksHandler
    @Mock lateinit var dateTimeUtilsWrapper: DateTimeUtilsWrapper
    @Mock lateinit var contextProvider: ContextProvider
    @Mock lateinit var invitePeopleUtils: InvitePeopleUtils
    @Mock lateinit var analyticsUtilsWrapper: AnalyticsUtilsWrapper
    @Mock lateinit var siteModel: SiteModel
    @Mock lateinit var context: Context
    @Mock lateinit var inviteLinksItem: InviteLinksItem

    private lateinit var viewModel: PeopleInviteViewModel
    private val blogId = 100L

    private val snackbarEvents = MutableLiveData<Event<SnackbarMessageHolder>>()
    private val inviteLinksState = MutableLiveData<InviteLinksState>()

    private var holder: SnackbarMessageHolder? = null
    private var uiState: InviteLinksUiState? = null
    private var sharedLink: InviteLinksItem? = null
    private var rolesForSelectDialog: Array<String>? = null

    @Before
    fun setUp() {
        whenever(inviteLinksHandler.snackbarEvents).thenReturn(snackbarEvents)
        whenever(inviteLinksHandler.inviteLinksState).thenReturn(inviteLinksState)
        whenever(siteModel.siteId).thenReturn(blogId)
        whenever(siteModel.isWpForTeamsSite).thenReturn(true)
        whenever(contextProvider.getContext()).thenReturn(context)

        viewModel = PeopleInviteViewModel(
                inviteLinksHandler,
                TEST_DISPATCHER,
                dateTimeUtilsWrapper,
                contextProvider,
                invitePeopleUtils,
                analyticsUtilsWrapper
        )
    }

    @Test
    fun `View Model has expected state when starting and site is WP for teams site`() = test {
        setupObvserversAndStart()

        requireNotNull(uiState).let {
            assertThat(it).isEqualTo(getInviteLinksUiState())
        }

        verify(
                inviteLinksHandler, times(1)
        ).handleInviteLinksStatusRequest(anyLong(), eq(INITIALIZING))
    }

    @Test
    fun `View Model has expected state when starting and site is NOT WP for teams site`() = test {
        whenever(siteModel.isWpForTeamsSite).thenReturn(false)

        setupObvserversAndStart()

        requireNotNull(uiState).let {
            assertThat(it).isEqualTo(getInviteLinksUiState(type = HIDDEN, isLinksSectionVisible = false))
        }
    }

    @Test
    fun `selecting links role opens role dialog`() {
        val roles = listOf("administrator", "contributor")

        whenever(invitePeopleUtils.getInviteLinksRoleDisplayNames(anyList(), eq(siteModel))).thenReturn(roles)

        setupObvserversAndStart()

        viewModel.onLinksRoleClicked()

        requireNotNull(rolesForSelectDialog).let {
            assertThat(it).isEqualTo(roles.toTypedArray())
        }
    }

    @Test
    fun `selecting links role gives error if role cannot be found`() {
        val roles = listOf<String>()
        val message = "snackbar message"

        whenever(invitePeopleUtils.getInviteLinksRoleDisplayNames(anyList(), eq(siteModel))).thenReturn(roles)
        whenever(context.getString(anyInt())).thenReturn(message)

        setupObvserversAndStart()

        viewModel.onLinksRoleClicked()

        assertThat(rolesForSelectDialog).isNull()
        requireNotNull(holder).let {
            assertThat(it.message).isEqualTo(UiStringText(message))
        }
    }

    @Test
    fun `expected link is shared when data is found`() {
        val role = "viewer"

        whenever(inviteLinksItem.role).thenReturn(role)
        whenever(
                invitePeopleUtils.getInviteLinkDataFromRoleDisplayName(anyList(), eq(siteModel), eq(role))
        ).thenReturn(inviteLinksItem)

        setupObvserversAndStart()

        viewModel.onShareButtonClicked(role)

        requireNotNull(sharedLink).let {
            assertThat(it).isEqualTo(inviteLinksItem)
        }
    }

    @Test
    fun `snackbar is shown when link to shared data is not found`() {
        val role = "viewer"
        val message = "snackbar message"

        whenever(inviteLinksItem.role).thenReturn(role)
        whenever(
                invitePeopleUtils.getInviteLinkDataFromRoleDisplayName(anyList(), eq(siteModel), eq(role))
        ).thenReturn(null)
        whenever(context.getString(anyInt(), eq(role))).thenReturn(message)

        setupObvserversAndStart()
        inviteLinksState.value = InviteLinksData(INITIALIZING, listOf(inviteLinksItem))

        viewModel.onShareButtonClicked(role)

        assertThat(sharedLink).isNull()
        requireNotNull(holder).let {
            assertThat(it.message).isEqualTo(UiStringText(message))
        }
    }

    private fun setupObvserversAndStart() {
        viewModel.inviteLinksUiState.observeForever {
            uiState = it
        }

        viewModel.shareLink.observeForever {
            it.applyIfNotHandled {
                sharedLink = this
            }
        }

        viewModel.snackbarEvents.observeForever {
            it.applyIfNotHandled {
                holder = this
            }
        }

        viewModel.showSelectLinksRoleDialog.observeForever {
            it.applyIfNotHandled {
                rolesForSelectDialog = this
            }
        }

        viewModel.start(siteModel)
    }

    companion object {
        fun getInviteLinksUiState(
            type: InviteLinksUiStateType = LOADING,
            isLinksSectionVisible: Boolean = true,
            loadAndRetryUiState: LoadAndRetryUiState = LoadAndRetryUiState.LOADING,
            isShimmerSectionVisible: Boolean = false,
            isRoleSelectionAllowed: Boolean = false,
            links: List<InviteLinksUiItem> = listOf(),
            inviteLinksSelectedRole: InviteLinksUiItem = InviteLinksUiItem.getEmptyItem(),
            enableManageLinksActions: Boolean = false,
            startShimmer: Boolean = isShimmerSectionVisible && type == LOADING,
            isActionButtonsEnabled: Boolean = !startShimmer
        ): InviteLinksUiState {
            return InviteLinksUiState(
                    type = type,
                    isLinksSectionVisible = isLinksSectionVisible,
                    loadAndRetryUiState = loadAndRetryUiState,
                    isShimmerSectionVisible = isShimmerSectionVisible,
                    isRoleSelectionAllowed = isRoleSelectionAllowed,
                    links = links,
                    inviteLinksSelectedRole = inviteLinksSelectedRole,
                    enableManageLinksActions = enableManageLinksActions,
                    startShimmer = startShimmer,
                    isActionButtonsEnabled = isActionButtonsEnabled
            )
        }
    }
}
