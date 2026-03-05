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
import uniffi.wp_api.TermEndpointType
import java.text.DateFormat
import java.util.Date
import javax.inject.Inject

@HiltViewModel
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
            else -> throw PostFetchException(
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
            _uiState.value = _uiState.value.copy(
                categoryNames = names
            )
        }
        resolveTermNames(
            post.tags,
            TermEndpointType.Tags
        ) { names ->
            _uiState.value = _uiState.value.copy(
                tagNames = names
            )
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
            statusLabel = formatStatusLabel(post.status),
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
            formatLabel = formatPostFormatLabel(post.format),
            slug = post.slug,
            excerpt = post.excerpt?.raw ?: "",
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
                _uiState.value = _uiState.value.copy(
                    authorName = if (name != null) {
                        FieldState.Loaded(name)
                    } else {
                        FieldState.Error(fieldError)
                    }
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                AppLog.e(
                    AppLog.T.POSTS,
                    "Failed to resolve author",
                    e
                )
                _uiState.value = _uiState.value.copy(
                    authorName = FieldState.Error(fieldError)
                )
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
                _uiState.value = _uiState.value.copy(
                    featuredImage = if (url != null) {
                        FieldState.Loaded(url)
                    } else {
                        FieldState.Error(fieldError)
                    }
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                AppLog.e(
                    AppLog.T.POSTS,
                    "Failed to resolve featured image",
                    e
                )
                _uiState.value = _uiState.value.copy(
                    featuredImage = FieldState.Error(fieldError)
                )
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

    private fun formatStatusLabel(status: PostStatus?): String {
        val resId = when (status) {
            is PostStatus.Publish ->
                R.string.post_status_post_published
            is PostStatus.Draft -> R.string.post_status_draft
            is PostStatus.Pending ->
                R.string.post_status_pending_review
            is PostStatus.Private ->
                R.string.post_status_post_private
            is PostStatus.Future ->
                R.string.post_status_post_scheduled
            is PostStatus.Trash ->
                R.string.post_status_post_trashed
            else -> return ""
        }
        return resourceProvider.getString(resId)
    }

    private fun formatDate(dateGmt: Date): String {
        return DateFormat.getDateTimeInstance(
            DateFormat.MEDIUM,
            DateFormat.SHORT
        ).format(dateGmt)
    }

    private fun formatPostFormatLabel(
        format: PostFormat?
    ): String {
        return when (format) {
            is PostFormat.Standard -> "Standard"
            is PostFormat.Aside -> "Aside"
            is PostFormat.Chat -> "Chat"
            is PostFormat.Gallery -> "Gallery"
            is PostFormat.Link -> "Link"
            is PostFormat.Image -> "Image"
            is PostFormat.Quote -> "Quote"
            is PostFormat.Status -> "Status"
            is PostFormat.Video -> "Video"
            is PostFormat.Audio -> "Audio"
            is PostFormat.Custom -> format.v1
            null -> ""
        }
    }

    private class PostFetchException(message: String?) :
        Exception(message ?: "Failed to fetch post")

    companion object {
        const val EXTRA_POST_ID = "extra_post_id"
    }
}
