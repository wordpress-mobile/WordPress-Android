package org.wordpress.android.fluxc.network.rest.wpapi.taxonomy

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.apache.commons.text.StringEscapeUtils
import org.json.JSONException
import org.json.JSONObject
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.TaxonomyActionBuilder
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.TermModel
import org.wordpress.android.fluxc.model.TermsModel
import org.wordpress.android.fluxc.module.FLUXC_SCOPE
import org.wordpress.android.fluxc.network.rest.wpapi.rs.WpApiClientProvider
import org.wordpress.android.fluxc.store.TaxonomyStore.DEFAULT_TAXONOMY_CATEGORY
import org.wordpress.android.fluxc.store.TaxonomyStore.DEFAULT_TAXONOMY_TAG
import org.wordpress.android.fluxc.store.TaxonomyStore.FetchTermsResponsePayload
import org.wordpress.android.fluxc.store.TaxonomyStore.RemoteTermPayload
import org.wordpress.android.fluxc.store.TaxonomyStore.TaxonomyError
import org.wordpress.android.fluxc.store.TaxonomyStore.TaxonomyErrorType
import org.wordpress.android.fluxc.utils.AppLogWrapper
import org.wordpress.android.util.AppLog
import rs.wordpress.api.kotlin.WpApiClient
import rs.wordpress.api.kotlin.WpRequestResult
import uniffi.wp_api.AnyTermWithViewContext
import uniffi.wp_api.TermCreateParams
import uniffi.wp_api.TermEndpointType
import uniffi.wp_api.TermListParams
import uniffi.wp_api.TermUpdateParams
import uniffi.wp_api.WpErrorCode
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class TaxonomyRsApiRestClient @Inject constructor(
    @Named(FLUXC_SCOPE) private val scope: CoroutineScope,
    private val dispatcher: Dispatcher,
    private val appLogWrapper: AppLogWrapper,
    private val wpApiClientProvider: WpApiClientProvider,
) {
    fun deleteTerm(site: SiteModel, term: TermModel) {
        scope.launch {
            when (term.taxonomy) {
                DEFAULT_TAXONOMY_CATEGORY -> deleteTerm(TermEndpointType.Categories, term, site)
                DEFAULT_TAXONOMY_TAG -> deleteTerm(TermEndpointType.Tags, term, site)
                else -> {} // TODO We are not supporting any other taxonomy yet
            }
        }
    }

    private suspend fun deleteTerm(
        termEndpointType: TermEndpointType,
        term: TermModel,
        site: SiteModel
    ) {
        val client = wpApiClientProvider.getWpApiClient(site)
        val taxonomyName = termEndpointType.toTaxonomyName()
        val termResponse = client.request { requestBuilder ->
            requestBuilder.terms().delete(
                termEndpointType = termEndpointType,
                termId = term.remoteTermId
            )
        }
        when (termResponse) {
            is WpRequestResult.Success -> {
                appLogWrapper.d(
                    AppLog.T.POSTS,
                    "Deleting $taxonomyName: ${term.name} - ${termResponse.response.data.deleted}"
                )
                if (termResponse.response.data.deleted) {
                    val termModel = TermModel(
                        term.id,
                        site.id,
                        term.remoteTermId,
                        taxonomyName,
                        term.name,
                        term.slug,
                        term.description,
                        term.parentRemoteId,
                        term.isHierarchical,
                        term.postCount
                    )
                    notifyTermDeleted(RemoteTermPayload(termModel, site))
                } else {
                    // The request itself succeeded, so there is no error to map: the API simply
                    // reported the term as not deleted.
                    appLogWrapper.e(AppLog.T.POSTS, "Failed deleting $taxonomyName: API reported it as not deleted")
                    notifyFailedDeleting(site, term, TaxonomyError(TaxonomyErrorType.GENERIC_ERROR))
                }
            }
            else -> {
                val error = termResponse.toTaxonomyError()
                appLogWrapper.e(AppLog.T.POSTS, "Failed deleting $taxonomyName: ${error.toLogString()}")
                notifyFailedDeleting(site, term, error)
            }
        }
    }

    private fun notifyFailedDeleting(
        site: SiteModel,
        term: TermModel,
        error: TaxonomyError
    ) {
        val payload = RemoteTermPayload(term, site)
        payload.error = error
        notifyTermDeleted(payload)
    }

    private fun notifyTermDeleted(
        payload: RemoteTermPayload,
    ) {
        dispatcher.dispatch(TaxonomyActionBuilder.newDeletedTermAction(payload))
    }

    fun createTerm(site: SiteModel, term: TermModel) {
        scope.launch {
            when (term.taxonomy) {
                DEFAULT_TAXONOMY_CATEGORY -> createTerm(TermEndpointType.Categories, term, site)
                DEFAULT_TAXONOMY_TAG -> createTerm(TermEndpointType.Tags, term, site)
                else -> {} // TODO We are not supporting any other taxonomy yet
            }
        }
    }

    private suspend fun createTerm(
        termEndpointType: TermEndpointType,
        term: TermModel,
        site: SiteModel
    ) {
        val client = wpApiClientProvider.getWpApiClient(site)
        val taxonomyName = termEndpointType.toTaxonomyName()
        val termResponse = client.request { requestBuilder ->
            requestBuilder.terms().create(
                termEndpointType = termEndpointType,
                TermCreateParams(
                    name = term.name,
                    description = term.description,
                    slug = term.slug,
                    // Right now, this is the only way we have to know if it's hierarchical
                    parent = if (termEndpointType == TermEndpointType.Categories) term.parentRemoteId else null
                )
            )
        }

        when (termResponse) {
            is WpRequestResult.Success -> {
                val term = termResponse.response.data
                appLogWrapper.d(AppLog.T.POSTS, "Created $taxonomyName: ${term.name}")
                val payload = RemoteTermPayload(
                    TermModel(
                        NEW_LOCAL_TERM_ID,
                        site.id,
                        term.id,
                        taxonomyName,
                        term.name,
                        term.slug,
                        term.description,
                        term.parent ?: 0,
                        term.parent != null,
                        term.count.toInt()
                    ),
                    site
                )
                dispatcher.dispatch(TaxonomyActionBuilder.newPushedTermAction(payload))
            }
            else -> {
                val error = termResponse.toTaxonomyError()
                appLogWrapper.e(
                    AppLog.T.POSTS,
                    "Failed creating $taxonomyName: ${term.name} - ${error.toLogString()}"
                )
                val payload = RemoteTermPayload(term, site)
                payload.error = error
                dispatcher.dispatch(TaxonomyActionBuilder.newPushedTermAction(payload))
            }
        }
    }

    fun updateTerm(site: SiteModel, term: TermModel) {
        scope.launch {
            if (term.remoteTermId < 0) {
                appLogWrapper.e(AppLog.T.POSTS, "Failed updating term: $term - id <= 0")
                val payload = RemoteTermPayload(term, site)
                payload.error = TaxonomyError(TaxonomyErrorType.GENERIC_ERROR, "")
                notifyTermUpdated(payload)
                return@launch
            }

            when (term.taxonomy) {
                DEFAULT_TAXONOMY_CATEGORY -> updateTerm(TermEndpointType.Categories, term, site)
                DEFAULT_TAXONOMY_TAG -> updateTerm(TermEndpointType.Tags, term, site)
                else -> {} // TODO We are not supporting any other taxonomy yet
            }
        }
    }

    private suspend fun updateTerm(
        termEndpointType: TermEndpointType,
        term: TermModel,
        site: SiteModel
    ) {
        val client = wpApiClientProvider.getWpApiClient(site)
        val taxonomyName = termEndpointType.toTaxonomyName()
        val termResponse = client.request { requestBuilder ->
            requestBuilder.terms().update(
                termEndpointType = termEndpointType,
                termId = term.remoteTermId,
                params = TermUpdateParams(
                    name = term.name,
                    description = term.description,
                    slug = term.slug,
                    parent = if (term.isHierarchical) term.parentRemoteId else null
                )
            )
        }
        when (termResponse) {
            is WpRequestResult.Success -> {
                val term = termResponse.response.data
                appLogWrapper.d(AppLog.T.POSTS, "Updated $taxonomyName: ${term.name}")
                val payload = RemoteTermPayload(
                    TermModel(
                        NEW_LOCAL_TERM_ID,
                        site.id,
                        term.id,
                        taxonomyName,
                        term.name,
                        term.slug,
                        term.description,
                        term.parent ?: 0,
                        term.parent != null,
                        term.count.toInt()
                    ),
                    site
                )
                notifyTermUpdated(payload)
            }
            else -> {
                val error = termResponse.toTaxonomyError()
                appLogWrapper.e(AppLog.T.POSTS, "Failed updating ${term.name}: ${error.toLogString()}")
                val payload = RemoteTermPayload(term, site)
                payload.error = error
                notifyTermUpdated(payload)
            }
        }
    }

    private fun notifyTermUpdated(
        payload: RemoteTermPayload,
    ) {
        // FluxC uses notifyTermCreated for updates
        dispatcher.dispatch(TaxonomyActionBuilder.newPushedTermAction(payload))
    }

    fun fetchTerms(site: SiteModel, taxonomyName: String) {
        scope.launch {
            when (taxonomyName) {
                DEFAULT_TAXONOMY_CATEGORY -> fetchTerms(TermEndpointType.Categories, site)
                DEFAULT_TAXONOMY_TAG -> fetchTerms(TermEndpointType.Tags, site)
                else -> {} // TODO We are not supporting any other taxonomy yet
            }
        }
    }

    private suspend fun fetchTerms(
        termEndpointType: TermEndpointType,
        site: SiteModel
    ) {
        val client = wpApiClientProvider.getWpApiClient(site)
        val taxonomyName = termEndpointType.toTaxonomyName()
        // Listing terms is a read, so it is requested in the view context. The edit context is
        // gated on the taxonomy's edit_terms capability (manage_categories / manage_post_tags),
        // which Authors and Contributors lack, and it returns no field this client maps.
        // The REST API defaults to 10 terms per page, so we request a larger page size and
        // follow the pagination params until all terms have been fetched (see CMM-2122).
        val allTerms = mutableListOf<AnyTermWithViewContext>()
        var params: TermListParams? = TermListParams(perPage = TERMS_PER_PAGE)
        var failure: WpRequestResult<*>? = null
        while (params != null) {
            val currentParams = params
            val termsResponse = client.request { requestBuilder ->
                requestBuilder.terms().listWithViewContext(
                    termEndpointType = termEndpointType,
                    params = currentParams
                )
            }
            when (termsResponse) {
                is WpRequestResult.Success -> {
                    allTerms.addAll(termsResponse.response.data)
                    params = termsResponse.response.nextPageParams
                }
                else -> {
                    // Keep any terms already fetched from earlier pages so a transient failure
                    // mid-pagination degrades gracefully instead of dropping the whole list.
                    appLogWrapper.e(
                        AppLog.T.POSTS,
                        "Fetch $termEndpointType list failed: ${termsResponse.toTaxonomyError().toLogString()}"
                    )
                    failure = termsResponse
                    break
                }
            }
        }
        // Only surface the error (leaving the cached list untouched) when nothing at all could be
        // fetched; otherwise persist whatever pages we managed to retrieve.
        if (failure != null && allTerms.isEmpty()) {
            dispatcher.dispatch(
                TaxonomyActionBuilder.newFetchedTermsAction(
                    FetchTermsResponsePayload(failure.toTaxonomyError(), taxonomyName)
                )
            )
            return
        }
        appLogWrapper.d(AppLog.T.POSTS, "Fetched $taxonomyName list: ${allTerms.size} (complete=${failure == null})")
        val termsResponsePayload = FetchTermsResponsePayload(
            TermsModel(allTerms.map { it.toTermModel(site, taxonomyName) }),
            site,
            taxonomyName,
            failure == null
        )
        dispatcher.dispatch(TaxonomyActionBuilder.newFetchedTermsAction(termsResponsePayload))
    }

    /**
     * Turns tag *names* into tag ids, creating the ones the site doesn't have yet.
     *
     * XML-RPC accepts tags by name (`terms_names`) and lets WordPress create the missing ones
     * server-side; wp/v2 only accepts term ids, so the REST post client has to do that resolution
     * itself before it can write a post. Returned in the order the names were given, with
     * duplicates and blanks dropped.
     *
     * A name that can neither be found nor created is skipped rather than failing the whole call:
     * losing one tag is a better outcome than losing the post edit that carried it. The result
     * reports whether every name resolved, because the ids alone cannot say so and an update that
     * sent an incomplete set would delete the tags missing from it - see [TagResolution].
     */
    suspend fun resolveTagIds(site: SiteModel, tagNames: List<String>): TagResolution {
        val wanted = tagNames.map { it.trim() }.filter { it.isNotEmpty() }.distinct()
        if (wanted.isEmpty()) return TagResolution(emptyList(), complete = true)

        val client = wpApiClientProvider.getWpApiClient(site)
        val outcomes = wanted.map { name -> resolveTagId(client, name, site) }
        return TagResolution(
            ids = outcomes.filterIsInstance<TagOutcome.Resolved>().map { it.id },
            complete = outcomes.none { it is TagOutcome.Unknown }
        )
    }

    /**
     * The ids [resolveTagIds] could resolve, and whether that is all of them.
     *
     * [complete] is `false` only while a name's fate is still open - a lookup or a create that
     * failed for a reason a retry might clear. The ids are still usable for a post being created,
     * which has no tags to lose, but an update must not send them: wp/v2 replaces a post's whole
     * tag set, so a short list removes the tags that are missing from it.
     *
     * A name the site refuses outright, such as a create rejected because the author's role lacks
     * `manage_categories`, is left out of the ids and leaves [complete] `true`. That answer will
     * not change on a retry, so treating it as still open would discard the author's other tag
     * edits on every save from then on, not just this one.
     */
    data class TagResolution(val ids: List<Long>, val complete: Boolean)

    private suspend fun resolveTagId(client: WpApiClient, name: String, site: SiteModel): TagOutcome =
        when (val lookup = findTagIdByName(client, name, site)) {
            is TagLookup.Found -> TagOutcome.Resolved(lookup.id)
            // A lookup that failed says nothing about whether the site has the tag, so creating
            // one now would be as likely to duplicate a tag it already holds as to add a new one.
            TagLookup.Failed -> TagOutcome.Unknown
            TagLookup.Missing -> createTag(client, name, site)
        }

    /** What became of one name: an id, a name the site will not give one, or no answer yet. */
    private sealed interface TagOutcome {
        data class Resolved(val id: Long) : TagOutcome
        data object Unavailable : TagOutcome
        data object Unknown : TagOutcome
    }

    /** Whether the site has a tag by this name, kept apart from having failed to ask. */
    private sealed interface TagLookup {
        data class Found(val id: Long) : TagLookup
        data object Missing : TagLookup
        data object Failed : TagLookup
    }

    private suspend fun findTagIdByName(
        client: WpApiClient,
        name: String,
        site: SiteModel
    ): TagLookup {
        val response = client.request { requestBuilder ->
            requestBuilder.terms().listWithViewContext(
                termEndpointType = TermEndpointType.Tags,
                params = TermListParams(search = name.htmlEncodedLikeWordPress(), perPage = TAG_SEARCH_PER_PAGE)
            )
        }
        return when (response) {
            is WpRequestResult.Success -> {
                // `search` is a substring match, so "news" also returns "newsletter". Only an
                // exact name is the tag the author typed, compared with both sides decoded.
                val match = response.response.data.firstOrNull {
                    StringEscapeUtils.unescapeHtml4(it.name).equals(name, ignoreCase = true)
                }
                match?.let { TagLookup.Found(it.id) } ?: TagLookup.Missing
            }
            else -> {
                appLogWrapper.e(
                    AppLog.T.POSTS,
                    "Tag lookup for '$name' on ${site.url} failed: ${response.toTaxonomyError().toLogString()}"
                )
                TagLookup.Failed
            }
        }
    }

    /**
     * Encodes a term name the way WordPress stores one, so a `search` can match it.
     *
     * `pre_term_name` runs the name through `_wp_specialchars` with `ENT_NOQUOTES`, so a tag typed
     * as `AT&T` is stored - and returned by wp/v2 - as `AT&amp;T`, while quotes and accented
     * characters are left alone. `search` is a `LIKE` against that stored text, so searching for the
     * decoded name finds nothing at all. Deliberately not `escapeHtml4`, which would also turn
     * `Hermès` into `Herm&egrave;s` and miss the tag for the opposite reason.
     */
    private fun String.htmlEncodedLikeWordPress(): String =
        replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")

    private suspend fun createTag(
        client: WpApiClient,
        name: String,
        site: SiteModel
    ): TagOutcome {
        val response = client.request { requestBuilder ->
            requestBuilder.terms().create(
                termEndpointType = TermEndpointType.Tags,
                params = TermCreateParams(name = name)
            )
        }
        val id = when (response) {
            is WpRequestResult.Success -> response.response.data.id
            is WpRequestResult.WpError<*> -> response.existingTermId()
            else -> null
        }
        if (id != null) return TagOutcome.Resolved(id)

        val error = response.toTaxonomyError()
        appLogWrapper.e(AppLog.T.POSTS, "Creating tag '$name' on ${site.url} failed: ${error.toLogString()}")
        // A site that turned the create down on the author's permissions has given its final
        // answer, so only that name is dropped. Any other failure might be the connection, and
        // could resolve on a retry, so it holds the whole tag set back instead.
        return if (error.type == TaxonomyErrorType.UNAUTHORIZED) TagOutcome.Unavailable else TagOutcome.Unknown
    }

    /**
     * The id a `term_exists` error carries, or `null` for any other error.
     *
     * `wp_insert_term` refuses a name it already holds, case-insensitively, and puts the existing
     * term's id in the error data - so the tag is recoverable, and dropping the error would drop
     * the tag from the post instead. A name [findTagIdByName] missed can still hit this, because
     * the two compare different things: the lookup is a `LIKE` against one page of `search`
     * results, while the create compares the submitted name after WordPress has normalised it.
     * A name typed with a doubled space resolves this way - `_sanitize_text_fields` collapses the
     * run of whitespace, so `my  tag` is refused as the already-stored `my tag` - and so does an
     * exact name sitting past the single page `search` reads.
     *
     * Note this is a name conflict, not a slug conflict: the slug branch of `wp_insert_term` only
     * applies when the create supplies a slug, which this one does not, or to a hierarchical
     * taxonomy, which tags are not. A name whose *slug* collides is created as a separate term.
     *
     * 0.10.0 has no [WpErrorCode] variant for `term_exists`, so it arrives as a
     * [WpErrorCode.CustomException] holding the raw code, and exposes no typed error data: reading
     * the response body is the only way to the id.
     */
    private fun WpRequestResult.WpError<*>.existingTermId(): Long? {
        if ((errorCode as? WpErrorCode.CustomException)?.v1 != TERM_EXISTS_CODE) return null
        return try {
            JSONObject(response).optJSONObject("data")?.optLong("term_id")?.takeIf { it > 0 }
        } catch (e: JSONException) {
            appLogWrapper.e(
                AppLog.T.POSTS,
                "A $TERM_EXISTS_CODE error carried no readable term id: ${e.message}"
            )
            null
        }
    }

    /**
     * Turns tag *ids* back into names, which is the form `PostModel.getTagNameList` stores.
     *
     * Returned in the order the ids were given; an id the site no longer has is skipped.
     * Paginates because the REST API caps how many terms one page returns, so a post carrying more
     * than a page of tags would otherwise lose every name past the first page (same reasoning as
     * CMM-2122).
     *
     * Names come back decoded, which is the form the tag picker shows and [resolveTagIds] expects;
     * storing the encoded `AT&amp;T` would re-encode it on the next save.
     *
     * Returns `null` if any page failed, rather than the names it did manage to read. A partial
     * list is indistinguishable from the real one, and wp/v2 replaces a post's whole tag set on
     * save, so storing one would quietly delete the tags missing from it the next time the post is
     * written. The caller is expected to keep whatever names it already holds instead.
     */
    suspend fun resolveTagNames(site: SiteModel, tagIds: List<Long>): List<String>? {
        if (tagIds.isEmpty()) return emptyList()

        val client = wpApiClientProvider.getWpApiClient(site)
        val namesById = mutableMapOf<Long, String>()
        var failed = false
        var params: TermListParams? = TermListParams(include = tagIds, perPage = TERMS_PER_PAGE)
        while (params != null) {
            val currentParams = params
            val response = client.request { requestBuilder ->
                requestBuilder.terms().listWithViewContext(
                    termEndpointType = TermEndpointType.Tags,
                    params = currentParams
                )
            }
            when (response) {
                is WpRequestResult.Success -> {
                    response.response.data.forEach {
                        namesById[it.id] = StringEscapeUtils.unescapeHtml4(it.name)
                    }
                    params = response.response.nextPageParams
                }
                else -> {
                    appLogWrapper.e(
                        AppLog.T.POSTS,
                        "Tag name lookup on ${site.url} failed: ${response.toTaxonomyError().toLogString()}"
                    )
                    failed = true
                    params = null
                }
            }
        }
        return if (failed) null else tagIds.mapNotNull { namesById[it] }
    }

    private fun AnyTermWithViewContext.toTermModel(site: SiteModel, taxonomyName: String) = TermModel(
        NEW_LOCAL_TERM_ID,
        site.id,
        id,
        taxonomyName,
        name,
        slug,
        description,
        parent ?: 0,
        parent != null,
        count.toInt()
    )


    private fun TermEndpointType.toTaxonomyName(): String = when (this) {
        TermEndpointType.Categories -> DEFAULT_TAXONOMY_CATEGORY
        TermEndpointType.Tags -> DEFAULT_TAXONOMY_TAG
        is TermEndpointType.Custom -> this.v1
    }

    /**
     * Keeps the permission fidelity TaxonomyXMLRPCClient already has as the wp-rs path replaces
     * it: a 401 (authentication required or rejected) and a 403 (the account lacks the capability)
     * both land on the single permission failure [TaxonomyErrorType] carries, rather than being
     * reported as a failed request.
     */
    private fun WpRequestResult<*>.toTaxonomyError(): TaxonomyError {
        val statusCode = when (this) {
            is WpRequestResult.WpError -> statusCode
            is WpRequestResult.InvalidHttpStatusCode -> statusCode
            is WpRequestResult.RequestExecutionFailed -> statusCode
            is WpRequestResult.UnknownError -> statusCode
            else -> null
        }
        val type = if (statusCode == HTTP_UNAUTHORIZED || statusCode == HTTP_FORBIDDEN) {
            TaxonomyErrorType.UNAUTHORIZED
        } else {
            TaxonomyErrorType.GENERIC_ERROR
        }
        return TaxonomyError(type, (this as? WpRequestResult.WpError)?.errorMessage.orEmpty())
    }

    /**
     * Describes a failure for the log. The result itself is not interpolated because its toString()
     * writes the whole response body.
     */
    private fun TaxonomyError.toLogString(): String =
        if (message.isEmpty()) type.toString() else "$type - $message"

    companion object {
        // TermModel.id is the local row id, not the remote term id. Leaving it at 0 lets TaxonomySqlUtils match
        // the row by remote id + site + taxonomy; passing the remote id instead matched and overwrote whichever
        // row happened to have that local id, even one belonging to another site or taxonomy.
        private const val NEW_LOCAL_TERM_ID = 0
        private const val TERMS_PER_PAGE = 100u
        // `search` is a substring match, so a short tag name can return many rows; this only has
        // to be wide enough to contain the exact match among them.
        private const val TAG_SEARCH_PER_PAGE = 100u
        private const val TERM_EXISTS_CODE = "term_exists"
        private const val HTTP_UNAUTHORIZED = 401u
        private const val HTTP_FORBIDDEN = 403u
    }
}
