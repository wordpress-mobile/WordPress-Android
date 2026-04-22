package org.wordpress.android.ui.postsrs

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.wordpress.android.R
import org.wordpress.android.analytics.AnalyticsTracker.Stat
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.rest.wpapi.rs.WpApiClientProvider
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import org.wordpress.android.ui.postsrs.data.PostRsRestClient
import org.wordpress.android.ui.postsrs.data.PostRsRestClient.Companion.AUTHORS_PER_PAGE
import org.wordpress.android.ui.posts.trackPostSettings
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.NetworkUtilsWrapper
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import org.wordpress.android.viewmodel.ResourceProvider
import rs.wordpress.api.kotlin.WpRequestResult
import uniffi.wp_api.AnyPostWithEditContext
import uniffi.wp_api.MediaCreateParams
import uniffi.wp_api.PostEndpointType
import uniffi.wp_api.PostFormat
import uniffi.wp_api.PostRetrieveParams
import uniffi.wp_api.PostStatus
import uniffi.wp_api.PostUpdateParams
import uniffi.wp_api.TermEndpointType
import uniffi.wp_api.UserListParams
import java.io.File
import java.text.DateFormat
import java.util.Calendar
import java.util.Date
import java.util.TimeZone
import javax.inject.Inject

@HiltViewModel
@Suppress("TooManyFunctions", "LargeClass")
class PostRsSettingsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    selectedSiteRepository: SelectedSiteRepository,
    private val wpApiClientProvider: WpApiClientProvider,
    private val restClient: PostRsRestClient,
    private val resourceProvider: ResourceProvider,
    private val networkUtilsWrapper: NetworkUtilsWrapper,
    private val uriToFileMapper: UriToFileMapper,
    private val analyticsTracker: AnalyticsTrackerWrapper,
) : ViewModel() {
    private val postId: Long = requireNotNull(savedStateHandle[EXTRA_POST_ID]) {
        "Missing $EXTRA_POST_ID in SavedStateHandle"
    }

    private val site = selectedSiteRepository.getSelectedSite()

    private val apiClient by lazy {
        site?.let { wpApiClientProvider.getWpApiClient(it) }
    }

    private val _uiState = MutableStateFlow(PostRsSettingsUiState())
    val uiState: StateFlow<PostRsSettingsUiState> = _uiState.asStateFlow()

    private val _events = Channel<PostRsSettingsEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    private val _snackbarMessages =
        Channel<SnackbarMessage>(Channel.BUFFERED)
    val snackbarMessages = _snackbarMessages.receiveAsFlow()

    private val fieldError: String
        get() = resourceProvider.getString(
            R.string.post_rs_settings_field_error
        )

    private var lastPost: AnyPostWithEditContext? = null
    private var nextAuthorPageParams: UserListParams? = null
    private var authorSearchJob: Job? = null

    init {
        if (site == null) {
            _events.trySend(PostRsSettingsEvent.Finish)
        } else {
            loadPost()
        }
    }

    fun retry() {
        loadPost()
    }

    @Suppress("TooGenericExceptionCaught")
    fun refreshPost() {
        if (site == null) return
        if (!networkUtilsWrapper.isNetworkAvailable()) {
            _snackbarMessages.trySend(
                SnackbarMessage(
                    resourceProvider.getString(
                        R.string.error_generic_network
                    )
                )
            )
            return
        }
        _uiState.update { it.copy(isRefreshing = true) }
        viewModelScope.launch {
            try {
                val post = fetchPost()
                lastPost = post
                val current = _uiState.value
                _uiState.value = mapPostToUiState(post)
                    .preserveEdits(from = current)
                resolveAsyncFields(post)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                AppLog.e(
                    AppLog.T.POSTS,
                    "Failed to refresh post settings",
                    e
                )
                _uiState.update {
                    it.copy(isRefreshing = false)
                }
                _snackbarMessages.trySend(
                    SnackbarMessage(
                        message = PostRsErrorUtils
                            .friendlyErrorMessage(
                                e = e,
                                resourceProvider =
                                    resourceProvider,
                                networkUtilsWrapper =
                                    networkUtilsWrapper,
                            ),
                        actionLabel = resourceProvider
                            .getString(R.string.retry),
                        onAction = { refreshPost() }
                    )
                )
            }
        }
    }

    fun retryField(field: RetryableField) {
        val post = lastPost ?: return
        when (field) {
            RetryableField.AUTHOR -> {
                _uiState.update {
                    it.copy(authorName = FieldState.Loading)
                }
                resolveAuthor(post.author)
            }
            RetryableField.CATEGORIES -> {
                _uiState.update {
                    it.copy(categoryNames = FieldState.Loading)
                }
                resolveTermNames(
                    post.categories,
                    TermEndpointType.Categories
                ) { names ->
                    _uiState.update {
                        it.copy(categoryNames = names)
                    }
                }
            }
            RetryableField.TAGS -> {
                _uiState.update {
                    it.copy(tagNames = FieldState.Loading)
                }
                resolveTermNames(
                    post.tags,
                    TermEndpointType.Tags
                ) { names ->
                    _uiState.update {
                        it.copy(tagNames = names)
                    }
                }
            }
        }
    }

    fun onStatusClicked() {
        _uiState.update {
            it.copy(dialogState = DialogState.StatusDialog)
        }
    }

    fun onStatusSelected(status: PostStatus) {
        val original = _uiState.value.postStatus
        if (status != original) {
            analyticsTracker.trackPostSettings(
                Stat.EDITOR_POST_VISIBILITY_CHANGED
            )
        }
        _uiState.update {
            it.copy(
                editedStatus = status.takeIf { es ->
                    es != original
                },
                dialogState = DialogState.None
            )
        }
    }

    fun onPasswordClicked() {
        _uiState.update {
            it.copy(dialogState = DialogState.PasswordDialog)
        }
    }

    fun onPasswordSet(password: String) {
        val original = _uiState.value.password ?: ""
        if (password != original) {
            analyticsTracker.trackPostSettings(
                Stat.EDITOR_POST_PASSWORD_CHANGED
            )
        }
        _uiState.update {
            it.copy(
                editedPassword = password.takeIf { ep ->
                    ep != original
                },
                dialogState = DialogState.None
            )
        }
    }

    fun onSlugClicked() {
        _uiState.update {
            it.copy(dialogState = DialogState.SlugDialog)
        }
    }

    fun onSlugSet(slug: String) {
        val original = _uiState.value.slug
        if (slug != original) {
            analyticsTracker.track(Stat.EDITOR_POST_SLUG_CHANGED)
        }
        _uiState.update {
            it.copy(
                editedSlug = slug.takeIf { es ->
                    es != original
                },
                dialogState = DialogState.None
            )
        }
    }

    fun onExcerptClicked() {
        _uiState.update {
            it.copy(dialogState = DialogState.ExcerptDialog)
        }
    }

    fun onExcerptSet(excerpt: String) {
        val original = _uiState.value.excerpt
        if (excerpt != original) {
            analyticsTracker.track(
                Stat.EDITOR_POST_EXCERPT_CHANGED
            )
        }
        _uiState.update {
            it.copy(
                editedExcerpt = excerpt.takeIf { ee ->
                    ee != original
                },
                dialogState = DialogState.None
            )
        }
    }

    fun onFormatClicked() {
        if (!hasFetchedFormats &&
            !_uiState.value.isLoadingFormats
        ) {
            resolveSitePostFormats()
        }
        _uiState.update {
            it.copy(dialogState = DialogState.FormatDialog)
        }
    }

    fun onFormatSelected(format: PostFormat) {
        val original = _uiState.value.postFormat
        if (format != original) {
            analyticsTracker.track(
                Stat.EDITOR_POST_FORMAT_CHANGED
            )
        }
        _uiState.update {
            it.copy(
                editedFormat = format.takeIf { ef ->
                    ef != original
                },
                dialogState = DialogState.None
            )
        }
    }

    fun onStickyToggled() {
        val current = _uiState.value
        val original = current.sticky
        val newValue = !current.effectiveSticky
        _uiState.update {
            it.copy(
                editedSticky = newValue.takeIf { es ->
                    es != original
                }
            )
        }
    }

    fun onDateClicked() {
        _uiState.update {
            it.copy(dialogState = DialogState.DateDialog)
        }
    }

    fun onDateSelected(year: Int, month: Int, dayOfMonth: Int) {
        val current = _uiState.value
        val base = current.effectiveDate ?: Date()
        val cal = Calendar.getInstance().apply {
            time = base
            this[Calendar.YEAR] = year
            this[Calendar.MONTH] = month
            this[Calendar.DAY_OF_MONTH] = dayOfMonth
        }
        val newDate = cal.time
        _uiState.update {
            it.copy(
                editedDate = newDate.takeIf { ed ->
                    ed != current.originalDate
                },
                publishDate = formatDate(newDate),
                dialogState = DialogState.TimeDialog
            )
        }
    }

    fun onTimeSelected(hour: Int, minute: Int) {
        val current = _uiState.value
        val base = current.effectiveDate ?: Date()
        val cal = Calendar.getInstance().apply {
            time = base
            this[Calendar.HOUR_OF_DAY] = hour
            this[Calendar.MINUTE] = minute
            this[Calendar.SECOND] = 0
            this[Calendar.MILLISECOND] = 0
        }
        val newDate = cal.time
        if (newDate != current.originalDate) {
            analyticsTracker.trackPostSettings(
                Stat.EDITOR_POST_SCHEDULE_CHANGED
            )
        }
        _uiState.update {
            it.copy(
                editedDate = newDate.takeIf { ed ->
                    ed != current.originalDate
                },
                publishDate = formatDate(newDate),
                dialogState = DialogState.None
            )
        }
    }

    fun onAuthorClicked() {
        val currentSite = site ?: return
        if (!_uiState.value.canEditAuthor) {
            _snackbarMessages.trySend(
                SnackbarMessage(
                    resourceProvider.getString(
                        R.string
                            .post_rs_settings_author_no_permission
                    )
                )
            )
            return
        }
        if (_uiState.value.siteAuthors.isEmpty()) {
            loadSiteAuthors(currentSite)
        }
        _uiState.update {
            it.copy(dialogState = DialogState.AuthorDialog)
        }
    }

    fun onAuthorSelected(authorId: Long) {
        val current = _uiState.value
        val authorName = current.siteAuthors
            .firstOrNull { it.id == authorId }?.name
        authorSearchJob?.cancel()
        nextAuthorPageParams = null
        _uiState.update {
            it.copy(
                editedAuthor = authorId.takeIf { ea ->
                    ea != current.authorId
                },
                authorName = if (authorName != null) {
                    FieldState.Loaded(authorName)
                } else {
                    it.authorName
                },
                dialogState = DialogState.None,
                authorSearchQuery = "",
                isSearchingAuthors = false,
                siteAuthors = emptyList(),
                canLoadMoreAuthors = false,
            )
        }
    }

    fun onCategoriesClicked() {
        _events.trySend(
            PostRsSettingsEvent.LaunchCategorySelection(
                _uiState.value.effectiveCategoryIds
            )
        )
    }

    fun onTagsClicked() {
        _events.trySend(
            PostRsSettingsEvent.LaunchTagSelection(
                _uiState.value.effectiveTagIds
            )
        )
    }

    fun onCategoriesSelected(ids: LongArray) {
        val originalIds = _uiState.value.categoryIds
        if (ids.toList().sorted() != originalIds.sorted()) {
            analyticsTracker.trackPostSettings(
                Stat.EDITOR_POST_CATEGORIES_ADDED
            )
        }
        onTermsSelected(
            ids = ids,
            originalIds = originalIds,
            endpointType = TermEndpointType.Categories,
            updateEdited = { state, edited ->
                state.copy(editedCategoryIds = edited)
            },
            updateNames = { state, names ->
                state.copy(categoryNames = names)
            }
        )
    }

    fun onTagsSelected(ids: LongArray) {
        val originalIds = _uiState.value.tagIds
        if (ids.toList().sorted() != originalIds.sorted()) {
            analyticsTracker.trackPostSettings(
                Stat.EDITOR_POST_TAGS_CHANGED
            )
        }
        onTermsSelected(
            ids = ids,
            originalIds = originalIds,
            endpointType = TermEndpointType.Tags,
            updateEdited = { state, edited ->
                state.copy(editedTagIds = edited)
            },
            updateNames = { state, names ->
                state.copy(tagNames = names)
            }
        )
    }

    private fun onTermsSelected(
        ids: LongArray,
        originalIds: List<Long>,
        endpointType: TermEndpointType,
        updateEdited: (PostRsSettingsUiState, List<Long>?) ->
            PostRsSettingsUiState,
        updateNames: (PostRsSettingsUiState, FieldState) ->
            PostRsSettingsUiState,
    ) {
        val newIds = ids.toList()
        val edited = newIds.takeIf {
            it.sorted() != originalIds.sorted()
        }
        val namesState = if (newIds.isEmpty()) {
            FieldState.Empty
        } else {
            FieldState.Loading
        }
        _uiState.update {
            updateNames(updateEdited(it, edited), namesState)
        }
        if (newIds.isNotEmpty()) {
            resolveTermNames(
                newIds, endpointType
            ) { names ->
                _uiState.update {
                    updateNames(it, names)
                }
            }
        }
    }

    fun onChooseFromWpMedia() {
        _events.trySend(
            PostRsSettingsEvent.LaunchWpMediaPicker
        )
    }

    fun onChooseFromDevice() {
        _events.trySend(
            PostRsSettingsEvent.LaunchDeviceMediaPicker
        )
    }

    fun onFeaturedImageSelected(mediaId: Long) {
        val current = _uiState.value
        if (mediaId == current.effectiveFeaturedImageId) return
        val edited = mediaId.takeIf { id ->
            id != current.featuredImageId
        }
        _uiState.update {
            it.copy(
                editedFeaturedImageId = edited,
                featuredImage = FieldState.Loading
            )
        }
        resolveFeaturedImage(mediaId)
    }

    fun onFeaturedImageRemoved() {
        val current = _uiState.value
        val edited = 0L.takeIf {
            current.featuredImageId != 0L
        }
        _uiState.update {
            it.copy(
                editedFeaturedImageId = edited,
                featuredImage = FieldState.Empty
            )
        }
    }

    @Suppress("TooGenericExceptionCaught")
    fun onFeaturedImagePickedFromDevice(uri: Uri) {
        _uiState.update {
            it.copy(featuredImage = FieldState.Loading)
        }
        viewModelScope.launch {
            var tempFile: File? = null
            try {
                val mediaId = withContext(Dispatchers.IO) {
                    val displayName = uriToFileMapper
                        .getDisplayName(uri)
                    val file = uriToFileMapper
                        .copyUriToTempFile(uri)
                    tempFile = file
                    uploadMediaAndGetId(
                        file.absolutePath,
                        displayName
                    )
                }
                onFeaturedImageSelected(mediaId)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                AppLog.e(
                    AppLog.T.POSTS,
                    "Failed to upload featured image",
                    e
                )
                _uiState.update {
                    it.copy(
                        featuredImage = FieldState.Error(
                            fieldError
                        )
                    )
                }
                _snackbarMessages.trySend(
                    SnackbarMessage(
                        message = resourceProvider.getString(
                            R.string.error_media_upload
                        ),
                        actionLabel = resourceProvider
                            .getString(R.string.retry),
                        onAction = {
                            onFeaturedImagePickedFromDevice(
                                uri
                            )
                        }
                    )
                )
            } finally {
                if (tempFile?.delete() == false) {
                    AppLog.w(
                        AppLog.T.MEDIA,
                        "Failed to delete temp file"
                    )
                }
            }
        }
    }

    private suspend fun uploadMediaAndGetId(
        filePath: String,
        displayName: String?,
    ): Long {
        val title = displayName
            ?.substringBeforeLast(".")
            ?: File(filePath).nameWithoutExtension
        val client = apiClient
            ?: throw PostApiRequestException(
                ERROR_NO_SITE
            )
        val response = client.request { requestBuilder ->
            requestBuilder.media().create(
                params = MediaCreateParams(
                    title = title,
                    filePath = filePath,
                )
            )
        }
        return when (response) {
            is WpRequestResult.Success ->
                response.response.data.id
            else -> throw PostApiRequestException(
                (response
                    as? WpRequestResult.WpError<*>)
                    ?.errorMessage
                    ?: "Failed to upload media"
            )
        }
    }

    private fun loadSiteAuthors(
        site: SiteModel
    ) {
        viewModelScope.launch {
            @Suppress("TooGenericExceptionCaught")
            try {
                val page = withContext(Dispatchers.IO) {
                    restClient.fetchSiteAuthors(site)
                }
                nextAuthorPageParams = page.nextPageParams
                _uiState.update {
                    it.copy(
                        siteAuthors = page.authors,
                        canLoadMoreAuthors =
                            page.nextPageParams != null,
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                AppLog.e(
                    AppLog.T.POSTS,
                    "Failed to load site authors",
                    e
                )
                _uiState.update {
                    it.copy(
                        dialogState = DialogState.None
                    )
                }
                _snackbarMessages.trySend(
                    SnackbarMessage(
                        e.message?.takeIf {
                            it.isNotBlank()
                        } ?: fieldError
                    )
                )
            }
        }
    }

    @Suppress("ReturnCount")
    fun loadMoreAuthors() {
        val site = site ?: return
        val params = nextAuthorPageParams ?: return
        if (_uiState.value.isLoadingMoreAuthors) return

        _uiState.update {
            it.copy(isLoadingMoreAuthors = true)
        }

        viewModelScope.launch {
            @Suppress("TooGenericExceptionCaught")
            try {
                val page = withContext(Dispatchers.IO) {
                    restClient.fetchSiteAuthors(
                        site, params
                    )
                }
                nextAuthorPageParams = page.nextPageParams
                _uiState.update {
                    it.copy(
                        siteAuthors =
                            it.siteAuthors + page.authors,
                        isLoadingMoreAuthors = false,
                        canLoadMoreAuthors =
                            page.nextPageParams != null,
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                AppLog.e(
                    AppLog.T.POSTS,
                    "Failed to load more authors",
                    e
                )
                _uiState.update {
                    it.copy(isLoadingMoreAuthors = false)
                }
            }
        }
    }

    fun onAuthorSearchQueryChanged(query: String) {
        _uiState.update {
            it.copy(authorSearchQuery = query)
        }
        authorSearchJob?.cancel()
        val trimmed = query.trim()
        if (trimmed.isNotEmpty() &&
            trimmed.length < MIN_AUTHOR_SEARCH_LENGTH
        ) {
            return
        }
        authorSearchJob = viewModelScope.launch {
            delay(AUTHOR_SEARCH_DEBOUNCE_MS)
            _uiState.update {
                it.copy(isSearchingAuthors = true)
            }
            searchAuthors(trimmed)
        }
    }

    @Suppress("TooGenericExceptionCaught")
    private suspend fun searchAuthors(query: String) {
        val currentSite = site ?: return
        try {
            val params = UserListParams(
                search = query.ifEmpty { null },
                perPage = AUTHORS_PER_PAGE,
            )
            val page = withContext(Dispatchers.IO) {
                restClient.fetchSiteAuthors(
                    currentSite, params
                )
            }
            nextAuthorPageParams = page.nextPageParams
            _uiState.update {
                it.copy(
                    siteAuthors = page.authors,
                    isSearchingAuthors = false,
                    canLoadMoreAuthors =
                        page.nextPageParams != null,
                )
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            AppLog.e(
                AppLog.T.POSTS,
                "Failed to search authors",
                e
            )
            _uiState.update {
                it.copy(isSearchingAuthors = false)
            }
            _snackbarMessages.trySend(
                SnackbarMessage(
                    PostRsErrorUtils.friendlyErrorMessage(
                        e = e,
                        resourceProvider =
                            resourceProvider,
                        networkUtilsWrapper =
                            networkUtilsWrapper,
                    )
                )
            )
        }
    }

    fun onDismissDialog() {
        authorSearchJob?.cancel()
        nextAuthorPageParams = null
        _uiState.update {
            it.copy(
                dialogState = DialogState.None,
                authorSearchQuery = "",
                isSearchingAuthors = false,
                siteAuthors = emptyList(),
                canLoadMoreAuthors = false,
            )
        }
    }

    fun onBackClicked() {
        if (_uiState.value.hasChanges) {
            _uiState.update {
                it.copy(dialogState = DialogState.DiscardDialog)
            }
        } else {
            _events.trySend(PostRsSettingsEvent.Finish)
        }
    }

    fun onDiscardConfirmed() {
        _uiState.update {
            it.copy(dialogState = DialogState.None)
        }
        _events.trySend(PostRsSettingsEvent.Finish)
    }

    fun onSaveClicked() {
        val state = _uiState.value
        if (!state.hasChanges || state.isSaving) return

        _uiState.update { it.copy(isSaving = true) }
        savePost(state)
    }

    @Suppress("TooGenericExceptionCaught", "LongMethod", "ThrowsCount")
    private fun savePost(
        state: PostRsSettingsUiState,
    ) {
        viewModelScope.launch {
            try {
                val params = PostUpdateParams(
                    status = state.editedStatus,
                    password = state.editedPassword,
                    sticky = state.editedSticky,
                    slug = state.editedSlug,
                    excerpt = state.editedExcerpt,
                    format = state.editedFormat,
                    dateGmt = state.editedDate,
                    author = state.editedAuthor,
                    featuredMedia =
                        state.editedFeaturedImageId,
                    categories =
                        state.editedCategoryIds
                            ?: emptyList(),
                    tags =
                        state.editedTagIds
                            ?: emptyList(),
                    meta = null
                )
                withContext(Dispatchers.IO) {
                    val client = apiClient
                        ?: throw PostApiRequestException(
                            ERROR_NO_SITE
                        )
                    val response = client.request {
                        it.posts().update(
                            PostEndpointType.Posts,
                            postId,
                            params
                        )
                    }
                    when (response) {
                        is WpRequestResult.Success -> Unit
                        else -> throw PostApiRequestException(
                            (response
                                as? WpRequestResult.WpError<*>)
                                ?.errorMessage
                                ?: "Post API request failed"
                        )
                    }
                }
                _events.trySend(
                    PostRsSettingsEvent.FinishWithChanges
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                AppLog.e(
                    AppLog.T.POSTS,
                    "Failed to save post settings",
                    e
                )
                _uiState.update { it.copy(isSaving = false) }
                _snackbarMessages.trySend(
                    SnackbarMessage(
                        message = PostRsErrorUtils
                            .friendlyErrorMessage(
                                e = e,
                                defaultResId = R.string
                                    .post_rs_settings_save_error,
                                resourceProvider =
                                    resourceProvider,
                                networkUtilsWrapper =
                                    networkUtilsWrapper,
                            ),
                        actionLabel = resourceProvider
                            .getString(R.string.retry),
                        onAction = { onSaveClicked() }
                    )
                )
            }
        }
    }

    private fun loadPost() {
        if (site == null) return
        if (!networkUtilsWrapper.isNetworkAvailable()) {
            _uiState.value = PostRsSettingsUiState(
                isLoading = false,
                error = resourceProvider.getString(
                    R.string.error_generic_network
                )
            )
            return
        }

        _uiState.value = PostRsSettingsUiState(isLoading = true)

        viewModelScope.launch {
            @Suppress("TooGenericExceptionCaught")
            try {
                val post = fetchPost()
                lastPost = post
                _uiState.value = mapPostToUiState(post)
                resolveAsyncFields(post)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                AppLog.e(
                    AppLog.T.POSTS,
                    "Failed to load post settings",
                    e
                )
                _uiState.value = PostRsSettingsUiState(
                    isLoading = false,
                    error = PostRsErrorUtils
                        .friendlyErrorMessage(
                            e = e,
                            resourceProvider =
                                resourceProvider,
                            networkUtilsWrapper =
                                networkUtilsWrapper,
                        )
                )
            }
        }
    }

    private suspend fun fetchPost():
        AnyPostWithEditContext = withContext(Dispatchers.IO) {
        val client = apiClient
            ?: throw PostApiRequestException(ERROR_NO_SITE)
        val response = client.request {
            it.posts().retrieveWithEditContext(
                PostEndpointType.Posts,
                postId,
                PostRetrieveParams()
            )
        }
        when (response) {
            is WpRequestResult.Success ->
                response.response.data
            else -> throw PostApiRequestException(
                (response as? WpRequestResult.WpError<*>)
                    ?.errorMessage
                    ?: "Post API request failed"
            )
        }
    }

    private fun resolveAsyncFields(
        post: AnyPostWithEditContext
    ) {
        resolveAuthor(post.author)
        resolveFeaturedImage(post.featuredMedia)
        resolveTermNames(
            post.categories,
            TermEndpointType.Categories
        ) { names ->
            _uiState.update { it.copy(categoryNames = names) }
        }
        resolveTermNames(
            post.tags,
            TermEndpointType.Tags
        ) { names ->
            _uiState.update { it.copy(tagNames = names) }
        }
    }

    @Suppress("ComplexCondition", "CyclomaticComplexMethod")
    private fun mapPostToUiState(
        post: AnyPostWithEditContext
    ): PostRsSettingsUiState {
        return PostRsSettingsUiState(
            isLoading = false,
            postTitle = post.title?.raw?.takeIf { it.isNotBlank() }
                ?: post.title?.rendered ?: "",
            publishDate = formatDate(post.dateGmt),
            originalDate = post.dateGmt,
            authorId = post.author ?: 0L,
            canEditAuthor =
                site?.hasCapabilityEditOthersPosts == true,
            password = post.password,
            authorName = if (
                post.author != null && post.author != 0L
            ) {
                FieldState.Loading
            } else {
                FieldState.Empty
            },
            categoryIds = post.categories ?: emptyList(),
            tagIds = post.tags ?: emptyList(),
            categoryNames = if (
                !post.categories.isNullOrEmpty()
            ) {
                FieldState.Loading
            } else {
                FieldState.Empty
            },
            tagNames = if (!post.tags.isNullOrEmpty()) {
                FieldState.Loading
            } else {
                FieldState.Empty
            },
            featuredImage = if (
                post.featuredMedia != null &&
                post.featuredMedia != 0L
            ) {
                FieldState.Loading
            } else {
                FieldState.Empty
            },
            featuredImageId = post.featuredMedia ?: 0L,
            sticky = post.sticky ?: false,
            slug = post.slug,
            excerpt = post.excerpt?.raw ?: "",
            postStatus = post.status,
            postFormat = post.format,
        )
    }

    private fun resolveAuthor(authorId: Long?) {
        if (authorId == null || authorId == 0L) return
        val site = site ?: return
        viewModelScope.launch {
            @Suppress("TooGenericExceptionCaught")
            try {
                val names = withContext(Dispatchers.IO) {
                    restClient.fetchUserDisplayNames(
                        site, listOf(authorId)
                    )
                }
                val name = names[authorId]
                _uiState.update {
                    it.copy(
                        authorName = if (name != null) {
                            FieldState.Loaded(name)
                        } else {
                            FieldState.Error(fieldError)
                        }
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                AppLog.e(
                    AppLog.T.POSTS,
                    "Failed to resolve author",
                    e
                )
                _uiState.update {
                    it.copy(
                        authorName = FieldState.Error(fieldError)
                    )
                }
            }
        }
    }

    private fun resolveFeaturedImage(mediaId: Long?) {
        if (mediaId == null || mediaId == 0L) return
        val site = site ?: return
        viewModelScope.launch {
            @Suppress("TooGenericExceptionCaught")
            try {
                val urls = withContext(Dispatchers.IO) {
                    restClient.fetchMediaUrls(
                        site, listOf(mediaId)
                    )
                }
                val url = urls[mediaId]
                _uiState.update {
                    it.copy(
                        featuredImage = if (url != null) {
                            FieldState.Loaded(url)
                        } else {
                            FieldState.Error(fieldError)
                        }
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                AppLog.e(
                    AppLog.T.POSTS,
                    "Failed to resolve featured image",
                    e
                )
                _uiState.update {
                    it.copy(
                        featuredImage = FieldState.Error(fieldError)
                    )
                }
            }
        }
    }

    private fun resolveTermNames(
        ids: List<Long>?,
        endpointType: TermEndpointType,
        update: (FieldState) -> Unit,
    ) {
        if (ids.isNullOrEmpty()) return
        val site = site ?: return
        viewModelScope.launch {
            @Suppress("TooGenericExceptionCaught")
            try {
                val names = withContext(Dispatchers.IO) {
                    restClient.fetchTermNames(
                        site, ids, endpointType
                    )
                }
                val resolved = ids.mapNotNull { names[it] }
                update(
                    if (resolved.isNotEmpty()) {
                        FieldState.Loaded(
                            resolved.joinToString(", ")
                        )
                    } else {
                        FieldState.Error(fieldError)
                    }
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                AppLog.e(
                    AppLog.T.POSTS,
                    "Failed to resolve term names",
                    e
                )
                update(FieldState.Error(fieldError))
            }
        }
    }

    private var hasFetchedFormats = false

    private fun resolveSitePostFormats() {
        val site = site ?: return
        viewModelScope.launch {
            _uiState.update {
                it.copy(isLoadingFormats = true)
            }
            @Suppress("TooGenericExceptionCaught")
            try {
                val formats =
                    withContext(Dispatchers.IO) {
                        restClient.fetchSitePostFormats(site)
                    }
                _uiState.update {
                    it.copy(sitePostFormats = formats)
                }
                hasFetchedFormats = true
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                AppLog.w(
                    AppLog.T.POSTS,
                    "resolveSitePostFormats failed: ${e.message}"
                )
            }
            _uiState.update {
                it.copy(isLoadingFormats = false)
            }
        }
    }

    private fun formatDate(dateGmt: Date): String {
        val fmt = DateFormat.getDateTimeInstance(
            DateFormat.MEDIUM,
            DateFormat.SHORT
        )
        val tz = TimeZone.getDefault()
        val tzLabel = tz.getDisplayName(
            tz.inDaylightTime(dateGmt),
            TimeZone.SHORT
        )
        return "${fmt.format(dateGmt)} $tzLabel"
    }

    private fun PostRsSettingsUiState.preserveEdits(
        from: PostRsSettingsUiState
    ) = copy(
        editedStatus = from.editedStatus,
        editedPassword = from.editedPassword,
        editedSticky = from.editedSticky,
        editedSlug = from.editedSlug,
        editedExcerpt = from.editedExcerpt,
        editedFormat = from.editedFormat,
        editedDate = from.editedDate,
        editedAuthor = from.editedAuthor,
        editedFeaturedImageId = from.editedFeaturedImageId,
        editedCategoryIds = from.editedCategoryIds,
        editedTagIds = from.editedTagIds,
        sitePostFormats = from.sitePostFormats,
    )

    private class PostApiRequestException(message: String) :
        RuntimeException(message)

    companion object {
        const val EXTRA_POST_ID = "extra_post_id"
        private const val AUTHOR_SEARCH_DEBOUNCE_MS = 500L
        private const val MIN_AUTHOR_SEARCH_LENGTH = 3
        private const val ERROR_NO_SITE =
            "No site selected"
    }
}
