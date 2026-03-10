package org.wordpress.android.ui.postsrs

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.wordpress.android.R
import org.wordpress.android.fluxc.network.rest.wpapi.rs.WpApiClientProvider
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import org.wordpress.android.ui.postsrs.data.PostRsRestClient
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.NetworkUtilsWrapper
import org.wordpress.android.viewmodel.ResourceProvider
import rs.wordpress.api.kotlin.WpRequestResult
import uniffi.wp_api.AnyPostWithEditContext
import uniffi.wp_api.PostEndpointType
import uniffi.wp_api.PostFormat
import uniffi.wp_api.PostRetrieveParams
import uniffi.wp_api.PostStatus
import uniffi.wp_api.PostUpdateParams
import uniffi.wp_api.TermEndpointType
import java.text.DateFormat
import java.util.Date
import javax.inject.Inject

@HiltViewModel
@Suppress("TooManyFunctions")
class PostRsSettingsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    selectedSiteRepository: SelectedSiteRepository,
    private val wpApiClientProvider: WpApiClientProvider,
    private val restClient: PostRsRestClient,
    private val resourceProvider: ResourceProvider,
    private val networkUtilsWrapper: NetworkUtilsWrapper,
) : ViewModel() {
    private val postId: Long = requireNotNull(savedStateHandle[EXTRA_POST_ID]) {
        "Missing $EXTRA_POST_ID in SavedStateHandle"
    }

    private val site = selectedSiteRepository.getSelectedSite()

    private val _uiState = MutableStateFlow(PostRsSettingsUiState())
    val uiState: StateFlow<PostRsSettingsUiState> = _uiState.asStateFlow()

    private val _events = Channel<PostRsSettingsEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    private val fieldError: String
        get() = resourceProvider.getString(
            R.string.post_rs_settings_field_error
        )

    private var lastPost: AnyPostWithEditContext? = null

    init {
        if (site == null) {
            _events.trySend(
                PostRsSettingsEvent.ShowSnackbar(
                    resourceProvider.getString(R.string.blog_not_found)
                )
            )
            _events.trySend(PostRsSettingsEvent.Finish)
        } else {
            loadPost()
        }
    }

    fun retry() {
        loadPost()
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
        _uiState.update {
            it.copy(dialogState = DialogState.FormatDialog)
        }
    }

    fun onFormatSelected(format: PostFormat) {
        val original = _uiState.value.postFormat
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

    fun onDismissDialog() {
        _uiState.update {
            it.copy(dialogState = DialogState.None)
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

    @Suppress("ReturnCount")
    fun onSaveClicked() {
        val site = site ?: return
        val state = _uiState.value
        if (!state.hasChanges || state.isSaving) return

        if (!networkUtilsWrapper.isNetworkAvailable()) {
            _events.trySend(
                PostRsSettingsEvent.ShowSnackbar(
                    resourceProvider.getString(
                        R.string.error_generic_network
                    )
                )
            )
            return
        }

        _uiState.update { it.copy(isSaving = true) }
        savePost(site, state)
    }

    @Suppress("TooGenericExceptionCaught")
    private fun savePost(
        site: org.wordpress.android.fluxc.model.SiteModel,
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
                    meta = null
                )
                withContext(Dispatchers.IO) {
                    val client =
                        wpApiClientProvider.getWpApiClient(site)
                    val response = client.request {
                        it.posts().update(
                            PostEndpointType.Posts,
                            postId,
                            params
                        )
                    }
                    when (response) {
                        is WpRequestResult.Success -> Unit
                        else -> throw PostApiException(
                            (response
                                as? WpRequestResult.WpError<*>)
                                ?.errorMessage
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
                val message = e.message?.takeIf {
                    it.isNotBlank()
                } ?: resourceProvider.getString(
                    R.string.post_rs_settings_save_error
                )
                _events.trySend(
                    PostRsSettingsEvent.ShowSnackbar(message)
                )
            }
        }
    }

    private fun loadPost() {
        val site = site ?: return
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
                val post = fetchPost(site)
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
                    error = resourceProvider.getString(
                        R.string.request_failed_message
                    )
                )
            }
        }
    }

    private suspend fun fetchPost(
        site: org.wordpress.android.fluxc.model.SiteModel
    ): AnyPostWithEditContext = withContext(Dispatchers.IO) {
        val client = wpApiClientProvider.getWpApiClient(site)
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
            else -> throw PostApiException(
                (response as? WpRequestResult.WpError<*>)
                    ?.errorMessage
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

    @Suppress("ComplexCondition")
    private fun mapPostToUiState(
        post: AnyPostWithEditContext
    ): PostRsSettingsUiState {
        return PostRsSettingsUiState(
            isLoading = false,
            postTitle = post.title?.raw?.takeIf { it.isNotBlank() }
                ?: post.title?.rendered ?: "",
            publishDate = formatDate(post.dateGmt),
            password = post.password,
            authorName = if (
                post.author != null && post.author != 0L
            ) {
                FieldState.Loading
            } else {
                FieldState.Empty
            },
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

    private fun formatDate(dateGmt: Date): String {
        return DateFormat.getDateTimeInstance(
            DateFormat.MEDIUM,
            DateFormat.SHORT
        ).format(dateGmt)
    }

    private class PostApiException(message: String?) :
        Exception(message ?: "Post API request failed")

    companion object {
        const val EXTRA_POST_ID = "extra_post_id"
    }
}
