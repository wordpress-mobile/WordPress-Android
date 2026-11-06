package org.wordpress.android.fluxc.network.rest.wpapi.post

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.action.PostAction
import org.wordpress.android.fluxc.generated.PostActionBuilder
import org.wordpress.android.fluxc.generated.UploadActionBuilder
import org.wordpress.android.fluxc.model.PostModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.module.FLUXC_SCOPE
import org.wordpress.android.fluxc.network.rest.wpapi.rs.WpAdditionalFieldsBuilder
import org.wordpress.android.fluxc.network.rest.wpapi.rs.WpApiClientProvider
import org.wordpress.android.fluxc.network.rest.wpapi.taxonomy.TaxonomyRsApiRestClient
import org.wordpress.android.fluxc.store.PostStore.DeletedPostPayload
import org.wordpress.android.fluxc.store.PostStore.FetchPostResponsePayload
import org.wordpress.android.fluxc.store.PostStore.FetchPostStatusResponsePayload
import org.wordpress.android.fluxc.store.PostStore.PostDeleteActionType
import org.wordpress.android.fluxc.store.PostStore.PostError
import org.wordpress.android.fluxc.store.PostStore.PostErrorType
import org.wordpress.android.fluxc.store.PostStore.RemotePostPayload
import org.wordpress.android.fluxc.utils.AppLogWrapper
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.DateTimeUtils
import rs.wordpress.api.kotlin.WpApiClient
import rs.wordpress.api.kotlin.WpRequestResult
import uniffi.wp_api.AnyPostWithEditContext
import uniffi.wp_api.PostCommentStatus
import uniffi.wp_api.PostCreateParams
import uniffi.wp_api.PostEndpointType
import uniffi.wp_api.PostRetrieveParams
import uniffi.wp_api.PostUpdateParams
import uniffi.wp_api.RequestExecutionErrorReason
import uniffi.wp_api.SparseAnyPostFieldWithEditContext
import uniffi.wp_api.WpAdditionalFields
import uniffi.wp_api.WpErrorCode
import java.util.Date
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

/**
 * Reads and writes posts and pages over the site's own WP REST API (`wp/v2`) using the
 * WordPress Rust library, for self-hosted sites authenticated with an application password.
 *
 * Those sites used to push every post over XML-RPC, which breaks on hosts that refuse
 * password-authenticated API logins on `xmlrpc.php` even when the credential is an application
 * password — a 2FA plugin is the usual cause (CMM-2502). Media already moved to this transport;
 * this client closes the same gap for the post open/save/delete cycle.
 *
 * The public surface mirrors `PostXMLRPCClient` (same signatures, same dispatched actions) so
 * `PostStore` only has to pick a client.
 */
@Singleton
@Suppress("TooManyFunctions")
class PostRsApiRestClient @Inject constructor(
    @Named(FLUXC_SCOPE) private val scope: CoroutineScope,
    private val dispatcher: Dispatcher,
    private val appLogWrapper: AppLogWrapper,
    private val wpApiClientProvider: WpApiClientProvider,
    private val taxonomyRsApiRestClient: TaxonomyRsApiRestClient,
    private val additionalFieldsBuilder: WpAdditionalFieldsBuilder,
    private val postMapper: RsPostMapper,
) {
    fun fetchPost(post: PostModel, site: SiteModel, origin: PostAction = PostAction.FETCH_POST) {
        scope.launch {
            val client = wpApiClientProvider.getWpApiClient(site)
            val response = client.request { requestBuilder ->
                requestBuilder.posts().retrieveWithEditContext(
                    postEndpointType = post.endpointType(),
                    postId = post.remotePostId,
                    params = PostRetrieveParams()
                )
            }
            val payload = when (response) {
                is WpRequestResult.Success -> {
                    val data = response.response.data
                    // A fetch carries no author-supplied names to reuse, so the response's tag ids
                    // are the only source of truth for this post's tags, and a lookup that fails
                    // has to fail the whole fetch: this payload overwrites the stored row through
                    // PostStore.updatePost, so a row stored with a shorter list would read as "the
                    // author removed those tags" and delete them on the next save. Keeping the
                    // names already held is no safer - everything else in the row, including the
                    // modified dates the conflict pre-check compares, would be refreshed around
                    // them, so stale tags would then be pushed with nothing left to flag them.
                    val tagNames = taxonomyRsApiRestClient.resolveTagNames(site, data.tags ?: emptyList())
                    if (tagNames == null) {
                        FetchPostResponsePayload(post, site).apply {
                            error = PostError(
                                PostErrorType.GENERIC_ERROR,
                                "Failed to resolve the tags of post ${post.remotePostId}"
                            )
                        }
                    } else {
                        FetchPostResponsePayload(toPostModel(data, site, post, tagNames), site)
                    }
                }
                else -> FetchPostResponsePayload(post, site).apply {
                    error = parsePostError(response, "Fetching post ${post.remotePostId}")
                }
            }
            payload.origin = origin
            dispatcher.dispatch(PostActionBuilder.newFetchedPostAction(payload))
        }
    }

    fun fetchPostStatus(post: PostModel, site: SiteModel) {
        scope.launch {
            val client = wpApiClientProvider.getWpApiClient(site)
            val response = client.request { requestBuilder ->
                requestBuilder.posts().filterRetrieveWithEditContext(
                    postEndpointType = post.endpointType(),
                    postId = post.remotePostId,
                    params = PostRetrieveParams(),
                    fields = listOf(SparseAnyPostFieldWithEditContext.STATUS)
                )
            }
            val payload = FetchPostStatusResponsePayload(post, site)
            when (response) {
                is WpRequestResult.Success -> {
                    val status = response.response.data.status
                    if (status != null) {
                        payload.remotePostStatus = postMapper.statusToSlug(status)
                    } else {
                        payload.error = PostError(PostErrorType.INVALID_RESPONSE)
                    }
                }
                else -> payload.error = parsePostError(response, "Fetching status of post ${post.remotePostId}")
            }
            dispatcher.dispatch(PostActionBuilder.newFetchedPostStatusAction(payload))
        }
    }

    fun pushPost(
        post: PostModel,
        site: SiteModel,
        isFirstTimePublish: Boolean,
        shouldSkipConflictResolutionCheck: Boolean,
        lastModifiedForConflictResolution: String?
    ) {
        pushPostInternal(
            post = post,
            site = site,
            isRestoringPost = false,
            isFirstTimePublish = isFirstTimePublish,
            shouldSkipConflictResolutionCheck = shouldSkipConflictResolutionCheck,
            lastModifiedForConflictResolution = lastModifiedForConflictResolution
        )
    }

    fun restorePost(post: PostModel, site: SiteModel) {
        pushPostInternal(
            post = post,
            site = site,
            isRestoringPost = true,
            isFirstTimePublish = false,
            shouldSkipConflictResolutionCheck = true,
            lastModifiedForConflictResolution = ""
        )
    }

    @Suppress("LongParameterList")
    private fun pushPostInternal(
        post: PostModel,
        site: SiteModel,
        isRestoringPost: Boolean,
        isFirstTimePublish: Boolean,
        shouldSkipConflictResolutionCheck: Boolean,
        lastModifiedForConflictResolution: String?
    ) {
        scope.launch {
            val client = wpApiClientProvider.getWpApiClient(site)

            val conflictError = findConflictError(
                client = client,
                post = post,
                shouldSkipConflictResolutionCheck = shouldSkipConflictResolutionCheck,
                lastModifiedForConflictResolution = lastModifiedForConflictResolution
            )
            if (conflictError != null) {
                dispatchPushResult(post, site, isRestoringPost, isFirstTimePublish, conflictError)
                return@launch
            }

            // The author's own tag names, kept before the response overwrites the model: they are
            // what the resolved ids were created from, so there is no need to read them back.
            val authoredTagNames = post.tagNameList
            val tagResolution = if (post.isPage) {
                null
            } else {
                taxonomyRsApiRestClient.resolveTagIds(site, authoredTagNames)
            }
            // A create takes whatever resolved: a post that does not exist yet has no tags to lose.
            // An update must not, while any name is still unresolved - wp/v2 replaces the whole tag
            // set, so a short list would delete the tags missing from it because a lookup failed.
            // An empty list is omitted from the request body, which leaves the site's tags as they
            // are. A name the site refused outright is already left out of the ids and does not
            // hold the update back, so the author's other tag edits still reach the site.
            val createTagIds = tagResolution?.ids.orEmpty()
            val updateTagIds = if (tagResolution?.complete == false) emptyList() else createTagIds

            // Create and update return different response types, so the saved post is pulled out
            // in each branch and the raw result only kept to describe a failure.
            val response: WpRequestResult<*>
            val savedPost: AnyPostWithEditContext?
            if (post.isLocalDraft) {
                val params = buildCreateParams(post, site, createTagIds)
                val result = client.request { requestBuilder ->
                    requestBuilder.posts().create(postEndpointType = post.endpointType(), params = params)
                }
                response = result
                savedPost = (result as? WpRequestResult.Success)?.response?.data
            } else {
                val params = buildUpdateParams(post, updateTagIds)
                val result = client.request { requestBuilder ->
                    requestBuilder.posts().update(
                        postEndpointType = post.endpointType(),
                        postId = post.remotePostId,
                        params = params
                    )
                }
                response = result
                savedPost = (result as? WpRequestResult.Success)?.response?.data
            }

            if (savedPost != null) {
                // Unlike XML-RPC, wp/v2 answers a write with the saved post, so the model can be
                // brought in sync here instead of costing a follow-up fetch.
                //
                // The caller's own model is updated in place, as `PostXMLRPCClient` does on the
                // path this replaces: it carries FluxC's local row id and local-only bookkeeping
                // (`dateLocallyChanged`, `changesConfirmedContentHashcode`, `customFields`, the
                // autosave fields), which a freshly mapped model would drop. Those fields are
                // deliberately left as they are; only server-owned ones are overwritten.
                postMapper.applyTo(
                    target = post,
                    post = savedPost,
                    site = site,
                    tagNames = authoredTagNames,
                    isPage = post.isPage
                )
                post.setIsLocalDraft(false)
                post.setIsLocallyChanged(false)
                dispatchPushResult(post, site, isRestoringPost, isFirstTimePublish, error = null)
            } else {
                val error = parsePostError(response, "Pushing post ${post.remotePostId}")
                dispatchPushResult(post, site, isRestoringPost, isFirstTimePublish, error)
            }
        }
    }

    fun deletePost(post: PostModel, site: SiteModel, postDeleteActionType: PostDeleteActionType) {
        scope.launch {
            val client = wpApiClientProvider.getWpApiClient(site)
            // `wp.deletePost` and WP.com's `/posts/{id}/delete` both trash a live post and
            // permanently delete one that is already trashed. wp/v2 splits that into two routes, and
            // PostStore has already worked out which of the two this is.
            when (postDeleteActionType) {
                PostDeleteActionType.TRASH -> trashPost(client, post, site)
                PostDeleteActionType.DELETE -> deletePostPermanently(client, post, site)
            }
        }
    }

    private suspend fun trashPost(client: WpApiClient, post: PostModel, site: SiteModel) {
        val response = client.request { requestBuilder ->
            requestBuilder.posts().trash(postEndpointType = post.endpointType(), postId = post.remotePostId)
        }
        if (response is WpRequestResult.WpError<*> && response.errorCode is WpErrorCode.TrashNotSupported) {
            // A site that turns the trash off (`EMPTY_TRASH_DAYS = 0`) refuses a non-forced delete,
            // and the UI offers no other way to remove a live post. `wp.deletePost` fell through to
            // a permanent delete on such a site, so retry as one: reported as a DELETE because that
            // is what happened, which also stops PostStore keeping a trashed row for a post that is
            // already gone.
            appLogWrapper.w(
                AppLog.T.POSTS,
                "Trash is disabled on ${site.url}, deleting post ${post.remotePostId} permanently instead"
            )
            deletePostPermanently(client, post, site)
            return
        }
        dispatchDeleteResult(
            post = post,
            site = site,
            postDeleteActionType = PostDeleteActionType.TRASH,
            trashedPost = (response as? WpRequestResult.Success)?.let {
                toPostModel(it.response.data, site, post, post.tagNameList)
            },
            response = response
        )
    }

    private suspend fun deletePostPermanently(client: WpApiClient, post: PostModel, site: SiteModel) {
        val response = client.request { requestBuilder ->
            requestBuilder.posts().delete(postEndpointType = post.endpointType(), postId = post.remotePostId)
        }
        // A permanent delete leaves nothing to store locally — PostStore removes the row —
        // so the returned `previous` post is intentionally dropped.
        dispatchDeleteResult(
            post = post,
            site = site,
            postDeleteActionType = PostDeleteActionType.DELETE,
            trashedPost = null,
            response = response
        )
    }

    /**
     * Detects the case XML-RPC covers with `if_not_modified_since`: the post was edited elsewhere
     * since this copy was loaded, so writing it would silently discard the other edit.
     *
     * wp/v2 has no conditional-update parameter, so this reads the remote `modified_gmt` first and
     * compares. That is a read-then-write check rather than a server-side guarantee: it narrows the
     * race rather than closing it. A failed check returns `null` so a network blip reads as "no
     * conflict" instead of blocking the save behind a conflict dialog the server never asked for.
     */
    @Suppress("ReturnCount")
    private suspend fun findConflictError(
        client: WpApiClient,
        post: PostModel,
        shouldSkipConflictResolutionCheck: Boolean,
        lastModifiedForConflictResolution: String?
    ): PostError? {
        if (shouldSkipConflictResolutionCheck || post.isLocalDraft) return null

        val knownLastModified = lastModifiedForConflictResolution?.takeIf { it.isNotEmpty() } ?: post.lastModified
        val knownDate = DateTimeUtils.dateUTCFromIso8601(knownLastModified) ?: return null

        val response = client.request { requestBuilder ->
            requestBuilder.posts().filterRetrieveWithEditContext(
                postEndpointType = post.endpointType(),
                postId = post.remotePostId,
                params = PostRetrieveParams(),
                fields = listOf(SparseAnyPostFieldWithEditContext.MODIFIED_GMT)
            )
        }
        val remoteDate = when (response) {
            is WpRequestResult.Success -> response.response.data.modifiedGmt
            else -> {
                appLogWrapper.w(
                    AppLog.T.POSTS,
                    "Conflict pre-check for post ${post.remotePostId} failed, pushing anyway: $response"
                )
                null
            }
        } ?: return null

        return if (remoteDate.after(knownDate)) {
            PostError(
                PostErrorType.OLD_REVISION,
                "The remote post was modified at $remoteDate, after the local copy at $knownDate"
            )
        } else {
            null
        }
    }

    /**
     * Note on geolocation: XML-RPC writes `geo_latitude` / `geo_longitude` / `geo_public` as
     * arbitrary `custom_fields`, which wp/v2 has no equivalent for - `meta` only accepts *registered*
     * meta keys and silently drops the rest. Nothing is lost by leaving them out:
     * `PostModel.supportsLocation()` is only `!isPage()`, and the editor's location API has no
     * callers left, so post geotagging is unreachable in the app. Not sending them also means this
     * client will not clobber geo values already on the server.
     */
    private fun buildCreateParams(post: PostModel, site: SiteModel, tagIds: List<Long>) = PostCreateParams(
        dateGmt = post.dateCreatedAsDate(),
        slug = post.slug,
        status = postMapper.statusFromSlug(post.status),
        password = post.password,
        title = post.title,
        content = post.sanitizedContent(),
        excerpt = post.excerpt,
        featuredMedia = post.featuredImageId,
        // First-time publishes take the site's default comment status; later edits leave whatever
        // the post already has, which is what PostXMLRPCClient.pushPostInternal does too.
        commentStatus = commentStatusFromSlug(site.defaultCommentStatus),
        format = if (post.isPage) null else postMapper.formatFromSlug(post.postFormat),
        parent = if (post.isPage) post.parentId else null,
        // An empty list is omitted from the request body, which is exactly what a new post needs:
        // with no `categories` field WordPress assigns the site's default category, whereas an
        // explicit empty array would leave the post uncategorised (PostXMLRPCClient does the same).
        categories = if (post.isPage) emptyList() else post.categoryIdList,
        tags = if (post.isPage) emptyList() else tagIds,
    )

    private fun buildUpdateParams(post: PostModel, tagIds: List<Long>) = PostUpdateParams(
        dateGmt = post.dateCreatedAsDate(),
        slug = post.slug,
        status = postMapper.statusFromSlug(post.status),
        password = post.password,
        title = post.title,
        content = post.sanitizedContent(),
        excerpt = post.excerpt,
        featuredMedia = post.featuredImageId,
        format = if (post.isPage) null else postMapper.formatFromSlug(post.postFormat),
        parent = if (post.isPage) post.parentId else null,
        categories = if (post.isPage) emptyList() else post.categoryIdList,
        tags = if (post.isPage) emptyList() else tagIds,
        additionalFields = clearedTaxonomies(post),
    )

    /**
     * Carries the empty `categories` / `tags` arrays that the typed params drop.
     *
     * wordpress-rs omits an empty list from the request body (`skip_serializing_if =
     * "Vec::is_empty"`), and wp/v2 reads an absent field as "leave this alone". On an edit that
     * would make clearing every category or tag a no-op, where XML-RPC sends an explicit empty
     * array so the removal sticks (see `PostXMLRPCClient.postModelToContentStruct`).
     * `additionalFields` is flattened into the same JSON object, so it can express what the typed
     * field cannot. Pages carry no taxonomies, matching the XML-RPC client.
     */
    private fun clearedTaxonomies(post: PostModel): WpAdditionalFields? {
        if (post.isPage) return null
        val clearedKeys = buildList {
            if (post.categoryIdList.isEmpty()) add(FIELD_CATEGORIES)
            // Keyed on the author's own names rather than on the resolved ids: a name that could
            // not be resolved leaves those empty too, and clearing the post's tags because a
            // lookup failed would delete server-side data the author never asked to remove. This
            // is also what keeps an incomplete resolution harmless - the names are still there, so
            // nothing is cleared, and `updateTagIds` omits the field instead.
            if (post.tagNameList.isEmpty()) add(FIELD_TAGS)
        }
        return if (clearedKeys.isEmpty()) null else additionalFieldsBuilder.emptyArrays(clearedKeys)
    }

    /**
     * Builds a [PostModel] from a server response, keeping the local row and site this post belongs
     * to. Mirrors `PostRestClient`, which also copies the local id onto the parsed post.
     *
     * [tagNames] is the caller's, because the right source differs per path: a save already holds
     * the author's names, having just resolved ids for them, while a fetch has to turn the
     * response's ids back into names.
     */
    private fun toPostModel(
        data: AnyPostWithEditContext,
        site: SiteModel,
        localPost: PostModel,
        tagNames: List<String>
    ): PostModel =
        postMapper.applyTo(
            target = PostModel(),
            post = data,
            site = site,
            tagNames = tagNames,
            isPage = localPost.isPage
        ).apply {
            setId(localPost.id)
        }

    private fun dispatchPushResult(
        post: PostModel,
        site: SiteModel,
        isRestoringPost: Boolean,
        isFirstTimePublish: Boolean,
        error: PostError?
    ) {
        val payload = RemotePostPayload(post, site)
        payload.isFirstTimePublish = isFirstTimePublish
        payload.error = error
        if (isRestoringPost) {
            dispatcher.dispatch(PostActionBuilder.newRestoredPostAction(payload))
        } else {
            dispatcher.dispatch(UploadActionBuilder.newPushedPostAction(payload))
        }
    }

    private fun dispatchDeleteResult(
        post: PostModel,
        site: SiteModel,
        postDeleteActionType: PostDeleteActionType,
        trashedPost: PostModel?,
        response: WpRequestResult<*>
    ) {
        val payload = if (response is WpRequestResult.Success) {
            DeletedPostPayload(post, site, postDeleteActionType, trashedPost)
        } else {
            DeletedPostPayload(
                post,
                site,
                postDeleteActionType,
                parsePostError(response, "Deleting post ${post.remotePostId}")
            )
        }
        dispatcher.dispatch(PostActionBuilder.newDeletedPostAction(payload))
    }

    @Suppress("UseCheckOrError") // Allow to throw IllegalStateException
    private fun parsePostError(response: WpRequestResult<*>, context: String): PostError = when (response) {
        is WpRequestResult.Success ->
            throw IllegalStateException("Success post response should not be parsed as an error")

        is WpRequestResult.ResponseParsingError<*> ->
            buildPostError(PostErrorType.INVALID_RESPONSE, "Failed to parse response", context, response)

        is WpRequestResult.SiteUrlParsingError<*> ->
            buildPostError(PostErrorType.GENERIC_ERROR, "Invalid site URL", context, response)

        // Only reachable from media uploads, which this client never performs.
        is WpRequestResult.MediaFileNotFound<*>,
        is WpRequestResult.MediaFileUnreadable<*> ->
            buildPostError(PostErrorType.GENERIC_ERROR, "Unexpected media failure", context, response)

        is WpRequestResult.InvalidHttpStatusCode<*> ->
            buildPostError(
                postErrorTypeFromHttpStatus(response.statusCode.toInt()),
                "Post request failed with HTTP status ${response.statusCode}",
                context,
                response
            )

        is WpRequestResult.WpError<*> ->
            buildPostError(
                postErrorTypeFromWpError(response.errorCode, response.statusCode.toInt()),
                response.errorMessage,
                context,
                response
            )

        is WpRequestResult.RequestExecutionFailed<*> ->
            buildPostError(
                postErrorTypeFromExecutionReason(response.reason),
                "Post request could not be completed",
                context,
                response
            )

        is WpRequestResult.UnknownError<*> ->
            buildPostError(
                postErrorTypeFromHttpStatus(response.statusCode.toInt()),
                "Post request failed with HTTP status ${response.statusCode}",
                context,
                response
            )
    }

    private fun buildPostError(
        type: PostErrorType,
        message: String,
        context: String,
        response: WpRequestResult<*>
    ): PostError {
        appLogWrapper.e(AppLog.T.POSTS, "$context failed: $type - $message ($response)")
        return PostError(type, message)
    }

    /**
     * A 404 is deliberately not mapped to [PostErrorType.UNKNOWN_POST]: on its own it cannot tell a
     * deleted post from a blocked or missing route, and only the latter is plausible once
     * [postErrorTypeFromWpError] has already recognised `rest_post_invalid_id`. Claiming "this post
     * no longer exists" about a post that is fine is the more misleading of the two answers, and it
     * would also suppress the retry wording a generic failure gets.
     */
    private fun postErrorTypeFromHttpStatus(statusCode: Int): PostErrorType = when (statusCode) {
        HTTP_UNAUTHORIZED, HTTP_FORBIDDEN -> PostErrorType.UNAUTHORIZED
        else -> PostErrorType.GENERIC_ERROR
    }

    /**
     * wp/v2 names the post-specific failures in the error code, so prefer it over the status: a
     * `rest_post_invalid_id` arrives as a 404 but so does a missing route, and the app only wants to
     * offer "this post is gone" for the former. Anything this doesn't recognise falls back to the
     * HTTP status, which is all the XML-RPC client ever had to go on.
     */
    private fun postErrorTypeFromWpError(errorCode: WpErrorCode, statusCode: Int): PostErrorType = when (errorCode) {
        is WpErrorCode.PostInvalidId,
        is WpErrorCode.PostInvalidType -> PostErrorType.UNKNOWN_POST
        is WpErrorCode.Unauthorized,
        is WpErrorCode.Forbidden,
        is WpErrorCode.CannotEdit,
        is WpErrorCode.CannotDelete,
        is WpErrorCode.CannotReadPost -> PostErrorType.UNAUTHORIZED
        else -> postErrorTypeFromHttpStatus(statusCode)
    }

    /**
     * Maps a transport-level failure (no HTTP response, or a connection/auth problem) to a
     * [PostErrorType]. Intentionally exhaustive (no `else`) so a new [RequestExecutionErrorReason]
     * added upstream surfaces as a compile error here rather than silently becoming a
     * [PostErrorType.GENERIC_ERROR].
     */
    private fun postErrorTypeFromExecutionReason(reason: RequestExecutionErrorReason): PostErrorType = when (reason) {
        is RequestExecutionErrorReason.HttpForbiddenError,
        is RequestExecutionErrorReason.HttpAuthenticationRequiredError,
        is RequestExecutionErrorReason.HttpAuthenticationRejectedError,
        is RequestExecutionErrorReason.MisconfiguredHttpAuthenticationError -> PostErrorType.UNAUTHORIZED
        is RequestExecutionErrorReason.HttpTimeoutError,
        is RequestExecutionErrorReason.DeviceIsOfflineError,
        is RequestExecutionErrorReason.ConnectionError,
        is RequestExecutionErrorReason.InvalidSslError,
        is RequestExecutionErrorReason.NonExistentSiteError,
        is RequestExecutionErrorReason.MisconfiguredRateLimitError,
        is RequestExecutionErrorReason.CancellationError,
        is RequestExecutionErrorReason.HttpError,
        is RequestExecutionErrorReason.GenericError -> PostErrorType.GENERIC_ERROR
    }

    private fun commentStatusFromSlug(slug: String?): PostCommentStatus? = when (slug) {
        COMMENT_STATUS_OPEN -> PostCommentStatus.Open
        COMMENT_STATUS_CLOSED -> PostCommentStatus.Closed
        null, "" -> null
        else -> PostCommentStatus.Custom(slug)
    }

    private fun PostModel.endpointType(): PostEndpointType =
        if (isPage) PostEndpointType.Pages else PostEndpointType.Posts

    private fun PostModel.dateCreatedAsDate(): Date? = DateTimeUtils.dateUTCFromIso8601(dateCreated)

    // Strips the object-replacement character Android inserts after images in the editor, the same
    // way PostXMLRPCClient does before writing the content.
    private fun PostModel.sanitizedContent(): String = content.replace(OBJECT_REPLACEMENT_CHARACTER, "")

    companion object {
        private const val HTTP_UNAUTHORIZED = 401
        private const val HTTP_FORBIDDEN = 403

        private const val COMMENT_STATUS_OPEN = "open"
        private const val COMMENT_STATUS_CLOSED = "closed"

        private const val FIELD_CATEGORIES = "categories"
        private const val FIELD_TAGS = "tags"

        private const val OBJECT_REPLACEMENT_CHARACTER = "￼"
    }
}
