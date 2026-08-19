package org.wordpress.android.ui.sitecreation.domains

import androidx.annotation.ColorRes
import androidx.annotation.StringRes
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.wordpress.android.R
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.networking.restapi.WpComApiClientProvider
import rs.wordpress.api.kotlin.WpComApiClient
import rs.wordpress.api.kotlin.WpRequestResult
import rs.wordpress.api.kotlin.toLogErrorString
import uniffi.wp_api.DomainSuggestion
import uniffi.wp_api.Product
import uniffi.wp_api.ProductTypeFilter
import uniffi.wp_api.ProductsParams
import org.wordpress.android.models.networkresource.ListState
import org.wordpress.android.models.networkresource.ListState.Error
import org.wordpress.android.models.networkresource.ListState.Loading
import org.wordpress.android.models.networkresource.ListState.Ready
import org.wordpress.android.models.networkresource.ListState.Success
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.sitecreation.domains.SiteCreationDomainsViewModel.DomainSuggestionsQuery.UserQuery
import org.wordpress.android.ui.sitecreation.domains.SiteCreationDomainsViewModel.DomainsUiState.CreateSiteButtonState
import org.wordpress.android.ui.sitecreation.domains.SiteCreationDomainsViewModel.DomainsUiState.DomainsUiContentState
import org.wordpress.android.ui.sitecreation.domains.SiteCreationDomainsViewModel.ListItemUiState.New
import org.wordpress.android.ui.sitecreation.domains.SiteCreationDomainsViewModel.ListItemUiState.New.DomainUiState.Cost
import org.wordpress.android.ui.sitecreation.domains.SiteCreationDomainsViewModel.ListItemUiState.New.DomainUiState.Tag
import org.wordpress.android.ui.sitecreation.domains.SiteCreationDomainsViewModel.ListItemUiState.Old
import org.wordpress.android.ui.sitecreation.misc.SiteCreationErrorType
import org.wordpress.android.ui.sitecreation.misc.SiteCreationHeaderUiState
import org.wordpress.android.ui.sitecreation.misc.SiteCreationSearchInputUiState
import org.wordpress.android.ui.sitecreation.misc.SiteCreationTracker
import org.wordpress.android.ui.sitecreation.usecases.FETCH_DOMAINS_VENDOR_DOT
import org.wordpress.android.ui.sitecreation.usecases.FETCH_DOMAINS_VENDOR_MOBILE
import org.wordpress.android.ui.sitecreation.usecases.FetchDomainsResult
import org.wordpress.android.ui.sitecreation.usecases.FetchDomainsUseCase
import org.wordpress.android.ui.utils.UiString
import org.wordpress.android.ui.utils.UiString.UiStringRes
import org.wordpress.android.ui.utils.UiString.UiStringText
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.NetworkUtilsWrapper
import org.wordpress.android.util.config.PlansInSiteCreationFeatureConfig
import org.wordpress.android.viewmodel.SingleLiveEvent
import javax.inject.Inject
import javax.inject.Named
import kotlin.coroutines.CoroutineContext
import kotlin.properties.Delegates

private const val THROTTLE_DELAY = 500L
private const val ERROR_CONTEXT = "domains"

@HiltViewModel
class SiteCreationDomainsViewModel @Inject constructor(
    private val networkUtils: NetworkUtilsWrapper,
    private val domainSanitizer: SiteCreationDomainSanitizer,
    private val fetchDomainsUseCase: FetchDomainsUseCase,
    private val wpComApiClientProvider: WpComApiClientProvider,
    private val accountStore: AccountStore,
    private val plansInSiteCreationFeatureConfig: PlansInSiteCreationFeatureConfig,
    private val tracker: SiteCreationTracker,
    @Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher,
    @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher
) : ViewModel(), CoroutineScope {
    private val job = Job()
    private var fetchDomainsJob: Job? = null
    override val coroutineContext: CoroutineContext
        get() = bgDispatcher + job
    private var isStarted = false

    private val _uiState: MutableLiveData<DomainsUiState> = MutableLiveData()
    val uiState: LiveData<DomainsUiState> = _uiState

    private var wpComApiClient: WpComApiClient? = null
    private var products: List<Product>? = null
    private var currentQuery: DomainSuggestionsQuery? = null
    private var listState: ListState<DomainModel> = ListState.Init()
    private var selectedDomain by Delegates.observable<DomainModel?>(null) { _, old, new ->
        if (old != new) {
            updateUiStateToContent(currentQuery, listState)
        }
    }

    private val _createSiteBtnClicked = SingleLiveEvent<DomainModel>()
    val createSiteBtnClicked: LiveData<DomainModel> = _createSiteBtnClicked

    private val _clearBtnClicked = SingleLiveEvent<Unit?>()
    val clearBtnClicked = _clearBtnClicked

    private val _onHelpClicked = SingleLiveEvent<Unit?>()
    val onHelpClicked: LiveData<Unit?> = _onHelpClicked

    @Synchronized
    private fun getOrCreateClient(): WpComApiClient {
        val token = requireNotNull(accountStore.accessToken) {
            "WP.com access token is required"
        }
        return wpComApiClient
            ?: wpComApiClientProvider.getWpComApiClient(token)
                .also { wpComApiClient = it }
    }

    fun start() {
        if (isStarted) return
        isStarted = true
        tracker.trackDomainsAccessed()
        resetUiState()
        if (plansInSiteCreationFeatureConfig.isEnabled()) fetchAndCacheProducts()
    }

    private fun fetchAndCacheProducts() {
        launch {
            val params = ProductsParams(
                productType = ProductTypeFilter.Domains
            )
            val result = getOrCreateClient()
                .request { it.products().list(params).data }
            if (result is WpRequestResult.Success) {
                products = result.response.values.toList()
            } else {
                AppLog.e(
                    AppLog.T.DOMAIN_REGISTRATION,
                    "Error while fetching domain products: ${result.toLogErrorString()}"
                )
            }
        }
    }

    fun onCreateSiteBtnClicked() {
        selectedDomain?.let { domain ->
            tracker.trackDomainSelected(domain.domainName, currentQuery?.value.orEmpty(), domain.cost, domain.isFree)
            _createSiteBtnClicked.value = domain
        } // selectedDomain is null if the query has been asynchronously updated and the domain list has been changed.
    }

    fun onClearTextBtnClicked() = _clearBtnClicked.call()

    fun onHelpClicked() = _onHelpClicked.call()

    fun onQueryChanged(query: String) = updateQuery(UserQuery(query))

    private fun updateQuery(query: DomainSuggestionsQuery?) {
        currentQuery = query
        selectedDomain = null
        fetchDomainsJob?.cancel() // cancel any previous requests
        if (query != null && query.value.isNotBlank()) {
            fetchDomains(query)
        } else {
            resetUiState()
        }
    }

    private fun resetUiState() = updateUiStateToContent(null, Ready(emptyList()))

    private fun fetchDomains(query: DomainSuggestionsQuery) {
        if (networkUtils.isNetworkAvailable()) {
            updateUiStateToContent(query, Loading(Ready(emptyList()), false))
            fetchDomainsJob = launch {
                delay(THROTTLE_DELAY)
                val onlyWordpressCom = !plansInSiteCreationFeatureConfig.isEnabled()
                val vendor = if (onlyWordpressCom) FETCH_DOMAINS_VENDOR_DOT else FETCH_DOMAINS_VENDOR_MOBILE
                val result = fetchDomainsUseCase.fetchDomains(query.value, vendor, onlyWordpressCom)

                withContext(mainDispatcher) {
                    onDomainsFetched(query, result)
                }
            }
        } else {
            tracker.trackErrorShown(ERROR_CONTEXT, SiteCreationErrorType.INTERNET_UNAVAILABLE_ERROR)
            updateUiStateToContent(
                query,
                Error(listState, errorMessageResId = R.string.no_network_message)
            )
        }
    }

    private fun onDomainsFetched(query: DomainSuggestionsQuery, result: FetchDomainsResult) {
        when (result) {
            is FetchDomainsResult.Error -> {
                tracker.trackErrorShown(ERROR_CONTEXT, result.type, result.message)
                updateUiStateToContent(
                    query,
                    Error(
                        listState,
                        errorMessageResId = R.string.site_creation_fetch_suggestions_error_unknown
                    )
                )
            }
            FetchDomainsResult.InvalidQuery -> {
                val emptyListMessage = UiStringRes(
                    R.string.new_site_creation_empty_domain_list_message_invalid_query
                )
                updateUiStateToContent(query, Success(emptyList()), emptyListMessage)
            }
            is FetchDomainsResult.Success -> {
                val domains = result.suggestions.map(::parseSuggestion)
                    .partition { it.domainName.startsWith("${query.value}.") }
                    .toList().flatten()

                val emptyListMessage = UiStringRes(
                    R.string.new_site_creation_empty_domain_list_message
                )

                updateUiStateToContent(query, Success(domains), emptyListMessage)
            }
        }
    }

    private fun parseSuggestion(suggestion: DomainSuggestion): DomainModel = when (suggestion) {
        is DomainSuggestion.Free -> DomainModel(
            domainName = suggestion.v1.domainName,
            isFree = true,
            cost = suggestion.v1.cost,
            productId = 0,
            supportsPrivacy = false,
        )
        is DomainSuggestion.Paid -> DomainModel(
            domainName = suggestion.v1.domainName,
            isFree = false,
            cost = suggestion.v1.cost,
            productId = suggestion.v1.productId.toInt(),
            supportsPrivacy = suggestion.v1.supportsPrivacy,
        )
    }

    private fun updateUiStateToContent(
        query: DomainSuggestionsQuery?,
        state: ListState<DomainModel>,
        emptyListMessage: UiString? = null
    ) {
        listState = state
        val isNonEmptyUserQuery = isNonEmptyUserQuery(query)

        _uiState.value = DomainsUiState(
            headerUiState = createHeaderUiState(
                !isNonEmptyUserQuery
            ),
            searchInputUiState = createSearchInputUiState(
                showProgress = state is Loading,
                showClearButton = isNonEmptyUserQuery,
                showDivider = state.data.isNotEmpty()
            ),
            contentState = createDomainsUiContentState(query, state, emptyListMessage),
            createSiteButtonState = getCreateSiteButtonState()
        )
    }

    private fun getCreateSiteButtonState() = selectedDomain?.run {
        when (plansInSiteCreationFeatureConfig.isEnabled()) {
            true -> if (isFree) CreateSiteButtonState.Free else CreateSiteButtonState.Paid
            else -> CreateSiteButtonState.Old
        }
    }

    private fun createDomainsUiContentState(
        query: DomainSuggestionsQuery?,
        state: ListState<DomainModel>,
        emptyListMessage: UiString?
    ): DomainsUiContentState {
        // Only treat it as an error if the search is user initiated
        val isError = isNonEmptyUserQuery(query) && state is Error

        val items = createSuggestionsUiStates(
            onRetry = { updateQuery(query) },
            query = query?.value,
            data = state.data,
            errorFetchingSuggestions = isError,
            errorResId = if (isError) state.errorMessageResId else null
        )
        return if (items.isEmpty()) {
            if (isNonEmptyUserQuery(query) && (state is Success || state is Ready)) {
                DomainsUiContentState.Empty(emptyListMessage)
            } else DomainsUiContentState.Initial(plansInSiteCreationFeatureConfig.isEnabled())
        } else {
            DomainsUiContentState.VisibleItems(items)
        }
    }

    private fun createSuggestionsUiStates(
        onRetry: () -> Unit,
        query: String?,
        data: List<DomainModel>,
        errorFetchingSuggestions: Boolean,
        @StringRes errorResId: Int?
    ): List<ListItemUiState> {
        val items: ArrayList<ListItemUiState> = ArrayList()
        if (errorFetchingSuggestions) {
            val errorUiState = Old.ErrorItemUiState(
                messageResId = errorResId ?: R.string.site_creation_fetch_suggestions_error_unknown,
                retryButtonResId = R.string.button_retry,
                onClick = onRetry,
            )
            items.add(errorUiState)
        } else {
            query?.let { value ->
                getDomainUnavailableUiState(value, data)?.let {
                    items.add(it)
                }
            }

            val sortedDomains = sortDomains(data)

            sortedDomains.forEachIndexed { index, domain ->
                val itemUiState = createAvailableItemUiState(domain, index)
                items.add(itemUiState)
            }
        }
        return items
    }

    /**
     *  First two paid domains, become Recommended, and Best Alternative
     *  First Free domain listed after two paid domains
     *  Then remaining paid domains
     */
    private fun sortDomains(domains: List<DomainModel>): List<DomainModel> {
        return domains.partition { !it.isFree }.let { (paidDomains, freeDomains) ->
            paidDomains.take(2) + freeDomains + paidDomains.drop(2)
        }
    }

    private fun createAvailableItemUiState(domain: DomainModel, index: Int): ListItemUiState {
        return when (plansInSiteCreationFeatureConfig.isEnabled()) {
            true -> {
                New.DomainUiState(
                    domain.domainName,
                    cost = when {
                        domain.isFree -> Cost.Free
                        else -> Cost.Paid(domain.cost)
                    },
                    isSelected = domain.domainName == selectedDomain?.domainName,
                    onClick = { onDomainSelected(domain) },
                    tags = listOfNotNull(
                        when (index) {
                            0 -> Tag.Recommended
                            1 -> Tag.BestAlternative
                            else -> null
                        },
                    ),
                )
            }

            else -> {
                Old.DomainUiState.AvailableDomain(
                    domainSanitizer.getName(domain.domainName),
                    domainSanitizer.getDomain(domain.domainName),
                    checked = domain == selectedDomain,
                    onClick = { onDomainSelected(domain) }
                )
            }
        }
    }

    @Suppress("ForbiddenComment", "ReturnCount")
    private fun getDomainUnavailableUiState(
        query: String,
        domains: List<DomainModel>
    ): ListItemUiState? {
        if (domains.isEmpty()) return null
        if (plansInSiteCreationFeatureConfig.isEnabled()) return null // TODO: Add FQDN availability check
        val sanitizedQuery = domainSanitizer.sanitizeDomainQuery(query)
        val isDomainUnavailable = domains.none { it.domainName.startsWith("$sanitizedQuery.") }
        return if (isDomainUnavailable) {
            Old.DomainUiState.UnavailableDomain(
                sanitizedQuery,
                ".wordpress.com",
                UiStringRes(R.string.new_site_creation_unavailable_domain)
            )
        } else null
    }

    private fun createHeaderUiState(isVisible: Boolean) = if (!isVisible) null else
        SiteCreationHeaderUiState(
            title = UiStringRes(R.string.new_site_creation_domain_header_title),
            subtitle = UiStringRes(
                if (plansInSiteCreationFeatureConfig.isEnabled()) R.string.site_creation_domain_header_subtitle
                else R.string.new_site_creation_domain_header_subtitle,
            ),
        )

private fun createSearchInputUiState(
        showProgress: Boolean,
        showClearButton: Boolean,
        showDivider: Boolean,
    ): SiteCreationSearchInputUiState {
        val hint = UiStringRes(
            if (plansInSiteCreationFeatureConfig.isEnabled())
                R.string.site_creation_domain_search_input_hint
            else
                R.string.new_site_creation_search_domain_input_hint
        )
        return SiteCreationSearchInputUiState(
            hint = hint,
            showProgress = showProgress,
            showClearButton = showClearButton,
            showDivider = showDivider,
            focusSearch = true
        )
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    fun onDomainSelected(domain: DomainModel) {
        selectedDomain = domain
    }

    private fun isNonEmptyUserQuery(query: DomainSuggestionsQuery?) = query is UserQuery && query.value.isNotBlank()

    data class DomainsUiState(
        val headerUiState: SiteCreationHeaderUiState?,
        val searchInputUiState: SiteCreationSearchInputUiState,
        val contentState: DomainsUiContentState,
        val createSiteButtonState: CreateSiteButtonState?
    ) {
        sealed class DomainsUiContentState(
            val emptyViewVisibility: Boolean,
            val exampleViewVisibility: Boolean,
            val updatedExampleViewVisibility: Boolean,
            val items: List<ListItemUiState>
        ) {
            class Initial(isUpdatedExample: Boolean) : DomainsUiContentState(
                emptyViewVisibility = false,
                exampleViewVisibility = !isUpdatedExample,
                updatedExampleViewVisibility = isUpdatedExample,
                items = emptyList()
            )

            class Empty(val message: UiString?) : DomainsUiContentState(
                emptyViewVisibility = true,
                exampleViewVisibility = false,
                updatedExampleViewVisibility = false,
                items = emptyList()
            )

            class VisibleItems(items: List<ListItemUiState>) : DomainsUiContentState(
                emptyViewVisibility = false,
                exampleViewVisibility = false,
                updatedExampleViewVisibility = false,
                items = items
            )
        }

        sealed class CreateSiteButtonState(@StringRes val stringRes: Int) {
            object Old : CreateSiteButtonState(R.string.site_creation_domain_finish_button)
            object Free : CreateSiteButtonState(R.string.site_creation_domain_button_continue_with_subdomain)
            object Paid : CreateSiteButtonState(R.string.site_creation_domain_button_purchase_domain)
        }
    }

    sealed class ListItemUiState(open val type: Type) {
        enum class Type {
            DOMAIN_V1,
            DOMAIN_V2,
            ERROR_V1,
        }

        sealed class Old(override val type: Type) : ListItemUiState(type) {
            sealed class DomainUiState(
                open val name: String,
                open val domain: String,
                open val checked: Boolean,
                val radioButtonVisibility: Boolean,
                open val subTitle: UiString? = null,
            ) : Old(Type.DOMAIN_V1) {
                data class AvailableDomain(
                    override val name: String,
                    override val domain: String,
                    override val checked: Boolean,
                    val onClick: () -> Unit,
                ) : DomainUiState(name, domain, checked, true)

                data class UnavailableDomain(
                    override val name: String,
                    override val domain: String,
                    override val subTitle: UiString,
                ) : DomainUiState(name, domain, false, false, subTitle)
            }

            data class ErrorItemUiState(
                @StringRes val messageResId: Int,
                @StringRes val retryButtonResId: Int,
                val onClick: () -> Unit,
            ) : Old(Type.ERROR_V1)
        }

        sealed class New(override val type: Type) : ListItemUiState(type) {
            data class DomainUiState(
                val domainName: String,
                val cost: Cost,
                val isSelected: Boolean = false,
                val onClick: () -> Unit,
                val tags: List<Tag> = emptyList(),
            ) : New(Type.DOMAIN_V2) {
                sealed class Tag(
                    @ColorRes val dotColor: Int,
                    @ColorRes val subtitleColor: Int? = null,
                    val subtitle: UiString,
                ) {
                    constructor(@ColorRes color: Int, subtitle: UiString) : this(color, color, subtitle)

                    object Unavailable : Tag(
                        R.color.red_50,
                        UiStringRes(R.string.site_creation_domain_tag_unavailable),
                    )

                    object Recommended : Tag(
                        R.color.jetpack_green_50,
                        UiStringRes(R.string.site_creation_domain_tag_recommended),
                    )

                    object BestAlternative : Tag(
                        R.color.purple_50,
                        UiStringRes(R.string.site_creation_domain_tag_best_alternative),
                    )

                    object Sale : Tag(
                        R.color.yellow_50,
                        UiStringRes(R.string.site_creation_domain_tag_sale)
                    )
                }

                sealed class Cost(val title: UiString) {
                    object Free : Cost(UiStringRes(R.string.free))

                    data class Paid(private val titleCost: String) : Cost(
                        UiStringText(titleCost)
                    ) {
                        val strikeoutTitle = UiStringText(titleCost)
                        val subtitle = UiStringRes(R.string.site_creation_domain_free_with_annual_plan)
                    }

                    data class OnSale(private val titleCost: String, private val strikeoutTitleCost: String) : Cost(
                        UiStringText(titleCost)
                    ) {
                        val strikeoutTitle = UiStringText(strikeoutTitleCost)
                        val subtitle = UiStringRes(R.string.site_creation_domain_cost_sale)
                    }
                }
            }
        }
    }

    /**
     * An organized way to separate user initiated searches from automatic searches so we can handle them differently.
     */
    private sealed class DomainSuggestionsQuery(val value: String) {
        /**
         * User initiated search.
         */
        class UserQuery(value: String) : DomainSuggestionsQuery(value)
    }
}
