package org.wordpress.android.ui.people

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import org.wordpress.android.R.string
import org.wordpress.android.analytics.AnalyticsTracker.Stat
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.models.InvitePeopleUtils
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.ui.pages.SnackbarMessageHolder
import org.wordpress.android.ui.people.AnalyticsInviteLinksActionResult.ERROR
import org.wordpress.android.ui.people.AnalyticsInviteLinksActionResult.SUCCEEDED
import org.wordpress.android.ui.people.AnalyticsInviteLinksGenericError.NO_ROLE_DATA_MATCHED
import org.wordpress.android.ui.people.InviteLinksApiCallsProvider.InviteLinksItem
import org.wordpress.android.ui.people.InviteLinksUiStateType.GET_STATUS_RETRY
import org.wordpress.android.ui.people.InviteLinksUiStateType.HIDDEN
import org.wordpress.android.ui.people.InviteLinksUiStateType.LINKS_AVAILABLE
import org.wordpress.android.ui.people.InviteLinksUiStateType.LINKS_GENERATE
import org.wordpress.android.ui.people.InviteLinksUiStateType.LOADING
import org.wordpress.android.ui.people.InviteLinksUseCase.InviteLinksState
import org.wordpress.android.ui.people.InviteLinksUseCase.InviteLinksState.InviteLinksData
import org.wordpress.android.ui.people.InviteLinksUseCase.InviteLinksState.InviteLinksError
import org.wordpress.android.ui.people.InviteLinksUseCase.InviteLinksState.InviteLinksLoading
import org.wordpress.android.ui.people.InviteLinksUseCase.InviteLinksState.InviteLinksNotAllowed
import org.wordpress.android.ui.people.InviteLinksUseCase.InviteLinksState.InviteLinksUserChangedRole
import org.wordpress.android.ui.people.InviteLinksUseCase.InviteLinksState.UserNotAuthenticated
import org.wordpress.android.ui.people.InviteLinksUseCase.UseCaseScenarioContext.GENERATING_LINKS
import org.wordpress.android.ui.people.InviteLinksUseCase.UseCaseScenarioContext.INITIALIZING
import org.wordpress.android.ui.people.InviteLinksUseCase.UseCaseScenarioContext.MANAGING_AVAILABLE_LINKS
import org.wordpress.android.ui.utils.UiString.UiStringText
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T
import org.wordpress.android.util.DateTimeUtilsWrapper
import org.wordpress.android.util.analytics.AnalyticsUtilsWrapper
import org.wordpress.android.util.map
import org.wordpress.android.viewmodel.ContextProvider
import org.wordpress.android.viewmodel.Event
import org.wordpress.android.viewmodel.ScopedViewModel
import java.text.SimpleDateFormat
import javax.inject.Inject
import javax.inject.Named

class PeopleInviteViewModel @Inject constructor(
    private val inviteLinksHandler: InviteLinksHandler,
    @Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher,
    private val dateTimeUtilsWrapper: DateTimeUtilsWrapper,
    private val contextProvider: ContextProvider,
    private val invitePeopleUtils: InvitePeopleUtils,
    private val analyticsUtilsWrapper: AnalyticsUtilsWrapper
) : ScopedViewModel(bgDispatcher) {
    private var isStarted = false
    private var inviteLinksRequestJob: Job? = null
    private lateinit var siteModel: SiteModel
    private val inviteLinksData: MutableList<InviteLinksItem> = mutableListOf()
    private var inviteLinksSelectedRole: InviteLinksUiItem = InviteLinksUiItem.getEmptyItem()

    private val _snackbarEvents = MediatorLiveData<Event<SnackbarMessageHolder>>()
    val snackbarEvents: LiveData<Event<SnackbarMessageHolder>> = _snackbarEvents

    private val _inviteLinksState = MediatorLiveData<InviteLinksState>()
    val inviteLinksUiState: LiveData<InviteLinksUiState> =
        _inviteLinksState.map { state -> buildInviteLinksUiState(state) }

    private val _shareLink = MutableLiveData<Event<InviteLinksItem>>()
    val shareLink: LiveData<Event<InviteLinksItem>> = _shareLink

    private val _showSelectLinksRoleDialog = MutableLiveData<Event<Array<String>>>()
    val showSelectLinksRoleDialog: LiveData<Event<Array<String>>> = _showSelectLinksRoleDialog

    fun start(siteModel: SiteModel) {
        if (isStarted) return
        isStarted = true

        this.siteModel = siteModel

        _snackbarEvents.addSource(inviteLinksHandler.snackbarEvents) { event ->
            _snackbarEvents.value = event
        }

        _inviteLinksState.addSource(inviteLinksHandler.inviteLinksState) { event ->
            _inviteLinksState.value = event
        }

        if (siteModel.isWpForTeamsSite) {
            _inviteLinksState.value = InviteLinksLoading(INITIALIZING)
            inviteLinksRequestJob?.cancel()
            inviteLinksRequestJob = launch(bgDispatcher) {
                inviteLinksHandler.handleInviteLinksStatusRequest(
                    siteModel.siteId,
                    INITIALIZING
                )
            }
        } else {
            _inviteLinksState.value = InviteLinksNotAllowed
        }
    }

    fun onGenerateLinksButtonClicked() {
        inviteLinksRequestJob?.cancel()
        inviteLinksRequestJob = launch(bgDispatcher) {
            inviteLinksHandler.handleGenerateLinks(siteModel.siteId)
        }
    }

    fun onDisableLinksButtonClicked() {
        inviteLinksRequestJob?.cancel()
        inviteLinksRequestJob = launch(bgDispatcher) {
            inviteLinksHandler.handleDisableLinks(siteModel.siteId)
        }
    }

    fun onRetryButtonClicked() {
        inviteLinksRequestJob?.cancel()
        inviteLinksRequestJob = launch(bgDispatcher) {
            inviteLinksHandler.handleInviteLinksStatusRequest(siteModel.siteId, INITIALIZING)
        }
    }

    fun onLinksRoleClicked() {
        val roles = invitePeopleUtils.getInviteLinksRoleDisplayNames(inviteLinksData, siteModel)

        if (roles.count() > 0) {
            _showSelectLinksRoleDialog.value = Event(roles.toTypedArray())
        } else {
            AppLog.d(T.PEOPLE, "Could not get roles for site [${siteModel.siteId}]")
            _snackbarEvents.value = Event(
                SnackbarMessageHolder(
                    UiStringText(
                        contextProvider.getContext()
                            .getString(string.invite_links_cannot_get_roles_error)
                    )
                )
            )
        }
    }

    fun onLinksRoleSelected(roleDisplayName: String) {
        val selectedRole = invitePeopleUtils.getInviteLinkDataFromRoleDisplayName(
            inviteLinksData,
            siteModel,
            roleDisplayName
        )

        selectedRole?.let {
            _inviteLinksState.value = InviteLinksUserChangedRole(it)
        } ?: run {
            AppLog.d(
                T.PEOPLE,
                "Could not get data for site [${siteModel.siteId}] " +
                        "role [$roleDisplayName] from ${inviteLinksData.map { it.role }}"
            )
            _snackbarEvents.value = Event(
                SnackbarMessageHolder(
                    UiStringText(
                        contextProvider.getContext()
                            .getString(string.invite_links_cannot_get_role_data_error, roleDisplayName)
                    )
                )
            )
        }
    }

    fun onShareButtonClicked(roleDisplayName: String) {
        val selectedRole = invitePeopleUtils.getInviteLinkDataFromRoleDisplayName(
            inviteLinksData,
            siteModel,
            roleDisplayName
        )

        val properties = mutableMapOf<String, Any?>()

        selectedRole?.let {
            _shareLink.value = Event(it)
            properties.addInviteLinksActionResult(SUCCEEDED)
            properties.addInviteLinksSharedRole(it.role)
        } ?: run {
            AppLog.d(
                T.PEOPLE,
                "Could not share link for site [${siteModel.siteId}] " +
                        "role [$roleDisplayName] from ${inviteLinksData.map { it.role }}"
            )
            _snackbarEvents.value = Event(
                SnackbarMessageHolder(
                    UiStringText(
                        contextProvider.getContext()
                            .getString(string.invite_links_cannot_get_role_data_error, roleDisplayName)
                    )
                )
            )
            properties.addInviteLinksActionResult(ERROR, NO_ROLE_DATA_MATCHED.errorMessage)
            properties.addInviteLinksSharedRole(roleDisplayName)
        }

        analyticsUtilsWrapper.trackInviteLinksAction(Stat.INVITE_LINKS_SHARE, siteModel, properties)
    }

    private fun buildInviteLinksUiState(inviteLinksState: InviteLinksState): InviteLinksUiState {
        val formatter = SimpleDateFormat.getDateInstance()
        var links: List<InviteLinksUiItem> = invitePeopleUtils.getMappedLinksUiItems(inviteLinksData, siteModel)

        if (inviteLinksState is InviteLinksData) {
            inviteLinksData.clear()
            inviteLinksData.addAll(inviteLinksState.links)

            links = invitePeopleUtils.getMappedLinksUiItems(inviteLinksData, siteModel)

            inviteLinksSelectedRole = links.firstOrNull() ?: InviteLinksUiItem.getEmptyItem()
        } else if (
            inviteLinksState.scenarioContext != MANAGING_AVAILABLE_LINKS &&
            inviteLinksState is InviteLinksError
        ) {
            inviteLinksData.clear()
            links = listOf()
            inviteLinksSelectedRole = InviteLinksUiItem.getEmptyItem()
        }

        if (inviteLinksState is InviteLinksUserChangedRole) {
            val roleData = inviteLinksState.selectedRole

            inviteLinksSelectedRole = InviteLinksUiItem(
                roleName = roleData.role,
                roleDisplayName = invitePeopleUtils.getDisplayNameForRole(siteModel, roleData.role),
                expiryDate = formatter.format(dateTimeUtilsWrapper.dateFromTimestamp(roleData.expiry))
            )
        }

        val uiStateType = mapUiStateType(inviteLinksState, links)

        val loadAndRetryUiState = if (inviteLinksState.scenarioContext != INITIALIZING ||
            listOf(LINKS_AVAILABLE, LINKS_GENERATE).contains(uiStateType)
        ) {
            LoadAndRetryUiState.HIDDEN
        } else {
            if (uiStateType == GET_STATUS_RETRY) {
                LoadAndRetryUiState.RETRY
            } else {
                LoadAndRetryUiState.LOADING
            }
        }

        return InviteLinksUiState(
            type = uiStateType,
            isLinksSectionVisible = uiStateType != HIDDEN,
            loadAndRetryUiState = loadAndRetryUiState,
            isShimmerSectionVisible = loadAndRetryUiState == LoadAndRetryUiState.HIDDEN,
            isRoleSelectionAllowed = links.count() > 1,
            links = links,
            inviteLinksSelectedRole = inviteLinksSelectedRole,
            enableManageLinksActions = links.count() > 0
        )
    }

    private fun mapUiStateType(
        inviteLinksState: InviteLinksState,
        links: List<InviteLinksUiItem>
    ): InviteLinksUiStateType {
        return when (inviteLinksState) {
            InviteLinksNotAllowed, UserNotAuthenticated -> HIDDEN
            is InviteLinksData -> if (links.count() > 0) LINKS_AVAILABLE else LINKS_GENERATE
            is InviteLinksError -> when (inviteLinksState.scenarioContext) {
                INITIALIZING -> GET_STATUS_RETRY
                GENERATING_LINKS -> LINKS_GENERATE
                MANAGING_AVAILABLE_LINKS -> LINKS_AVAILABLE
            }
            is InviteLinksLoading -> LOADING
            is InviteLinksUserChangedRole -> LINKS_AVAILABLE
        }
    }

    override fun onCleared() {
        super.onCleared()
        inviteLinksRequestJob?.cancel()
    }
}
