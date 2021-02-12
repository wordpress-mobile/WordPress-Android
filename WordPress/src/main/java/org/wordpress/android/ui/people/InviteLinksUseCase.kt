package org.wordpress.android.ui.people

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.flow
import org.wordpress.android.R.string
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.ui.people.InviteLinksApiCallsProvider.InviteLinksCallResult.Failure
import org.wordpress.android.ui.people.InviteLinksApiCallsProvider.InviteLinksCallResult.Success
import org.wordpress.android.ui.people.InviteLinksApiCallsProvider.InviteLinksItem
import org.wordpress.android.ui.people.InviteLinksUseCase.InviteLinksState.InviteLinksData
import org.wordpress.android.ui.people.InviteLinksUseCase.InviteLinksState.InviteLinksError
import org.wordpress.android.ui.people.InviteLinksUseCase.InviteLinksState.InviteLinksLoading
import org.wordpress.android.ui.people.InviteLinksUseCase.InviteLinksState.UserNotAuthenticated
import org.wordpress.android.ui.people.InviteLinksUseCase.UseCaseScenarioContext.GENERATING_LINKS
import org.wordpress.android.ui.people.InviteLinksUseCase.UseCaseScenarioContext.INITIALIZING
import org.wordpress.android.ui.people.InviteLinksUseCase.UseCaseScenarioContext.MANAGING_AVAILABLE_LINKS
import org.wordpress.android.ui.utils.UiString
import org.wordpress.android.ui.utils.UiString.UiStringRes
import org.wordpress.android.ui.utils.UiString.UiStringText
import org.wordpress.android.util.NetworkUtilsWrapper
import javax.inject.Inject

class InviteLinksUseCase @Inject constructor(
    private val networkUtilsWrapper: NetworkUtilsWrapper,
    private val inviteLinksApiCallsProvider: InviteLinksApiCallsProvider,
    private val accountStore: AccountStore
) {
    suspend fun getInviteLinksStatus(
        blogId: Long,
        scenarioContext: UseCaseScenarioContext
    ): Flow<InviteLinksState> = flow {
        makeChecksAndApplyStrategy(this, blogId, ::getInvitesStrategy, scenarioContext)
    }

    suspend fun generateLinks(blogId: Long): Flow<InviteLinksState> = flow {
        makeChecksAndApplyStrategy(this, blogId, ::generateInvitesStrategy, GENERATING_LINKS)
    }

    suspend fun disableLinks(blogId: Long): Flow<InviteLinksState> = flow {
        makeChecksAndApplyStrategy(this, blogId, ::disableInvitesStrategy, MANAGING_AVAILABLE_LINKS)
    }

    private suspend fun makeChecksAndApplyStrategy(
        flow: FlowCollector<InviteLinksState>,
        blogId: Long,
        strategy: suspend (
            flow: FlowCollector<InviteLinksState>,
            blogId: Long,
            scenarioContext: UseCaseScenarioContext
        ) -> Unit,
        scenarioContext: UseCaseScenarioContext
    ) {
        if (!accountStore.hasAccessToken()) {
            flow.emit(UserNotAuthenticated)
        } else {
            flow.emit(InviteLinksLoading(scenarioContext))
            delay(600) // Pretty arbitrary amount to allow the progress bar to appear to the user
            if (!networkUtilsWrapper.isNetworkAvailable()) {
                flow.emit(InviteLinksError(scenarioContext, UiStringRes(string.error_network_connection)))
            } else {
                strategy.invoke(flow, blogId, scenarioContext)
            }
        }
    }

    private suspend fun getInvitesStrategy(
        flow: FlowCollector<InviteLinksState>,
        blogId: Long,
        scenarioContext: UseCaseScenarioContext
    ) {
        when (val status = inviteLinksApiCallsProvider.getInviteLinksStatus(blogId)) {
            is Success -> flow.emit(InviteLinksData(scenarioContext, status.links))
            is Failure -> flow.emit(InviteLinksError(scenarioContext, UiStringText(status.error)))
        }
    }

    private suspend fun generateInvitesStrategy(
        flow: FlowCollector<InviteLinksState>,
        blogId: Long,
        scenarioContext: UseCaseScenarioContext
    ) {
        when (val statusGenerate = inviteLinksApiCallsProvider.generateLinks(blogId)) {
            is Success -> {
                when (val statusGetInvites = inviteLinksApiCallsProvider.getInviteLinksStatus((blogId))) {
                    is Success -> {
                        flow.emit(InviteLinksData(scenarioContext, statusGetInvites.links))
                    }
                    is Failure -> {
                        flow.emit(InviteLinksError(scenarioContext, UiStringText(statusGetInvites.error)))
                    }
                }
            }
            is Failure -> flow.emit(InviteLinksError(scenarioContext, UiStringText(statusGenerate.error)))
        }
    }

    private suspend fun disableInvitesStrategy(
        flow: FlowCollector<InviteLinksState>,
        blogId: Long,
        scenarioContext: UseCaseScenarioContext
    ) {
        when (val status = inviteLinksApiCallsProvider.disableLinks(blogId)) {
            is Success -> flow.emit(InviteLinksData(scenarioContext, listOf()))
            is Failure -> flow.emit(InviteLinksError(scenarioContext, UiStringText(status.error)))
        }
    }

    enum class UseCaseScenarioContext {
        INITIALIZING,
        GENERATING_LINKS,
        MANAGING_AVAILABLE_LINKS
    }

    sealed class InviteLinksState(open val scenarioContext: UseCaseScenarioContext = INITIALIZING) {
        object InviteLinksNotAllowed : InviteLinksState(INITIALIZING)

        object UserNotAuthenticated : InviteLinksState(INITIALIZING)

        data class InviteLinksLoading(override val scenarioContext: UseCaseScenarioContext) : InviteLinksState()

        data class InviteLinksData(
            override val scenarioContext: UseCaseScenarioContext,
            val links: List<InviteLinksItem>
        ) : InviteLinksState()

        data class InviteLinksUserChangedRole(
            val selectedRole: InviteLinksItem
        ) : InviteLinksState(MANAGING_AVAILABLE_LINKS)

        data class InviteLinksError(
            override val scenarioContext: UseCaseScenarioContext,
            val error: UiString
        ) : InviteLinksState()
    }
}
