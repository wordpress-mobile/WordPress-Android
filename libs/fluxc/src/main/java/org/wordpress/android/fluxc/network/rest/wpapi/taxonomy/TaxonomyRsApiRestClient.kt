package org.wordpress.android.fluxc.network.rest.wpapi.taxonomy

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.apache.commons.text.StringEscapeUtils
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
     * losing one tag is a better outcome than losing the post edit that carried it.
     */
    suspend fun resolveTagIds(site: SiteModel, tagNames: List<String>): List<Long> {
        val wanted = tagNames.map { it.trim() }.filter { it.isNotEmpty() }.distinct()
        if (wanted.isEmpty()) return emptyList()

        val client = wpApiClientProvider.getWpApiClient(site)
        return wanted.mapNotNull { name ->
            findTagIdByName(client, name, site) ?: createTagReturningId(client, name, site)
        }
    }

    private suspend fun findTagIdByName(
        client: WpApiClient,
        name: String,
        site: SiteModel
    ): Long? {
        val response = client.request { requestBuilder ->
            requestBuilder.terms().listWithViewContext(
                termEndpointType = TermEndpointType.Tags,
                params = TermListParams(search = name.htmlEncodedLikeWordPress(), perPage = TAG_SEARCH_PER_PAGE)
            )
        }
        return when (response) {
            is WpRequestResult.Success ->
                // `search` is a substring match, so "news" also returns "newsletter". Only an
                // exact name is the tag the author typed, compared with both sides decoded.
                response.response.data.firstOrNull {
                    StringEscapeUtils.unescapeHtml4(it.name).equals(name, ignoreCase = true)
                }?.id
            else -> {
                appLogWrapper.e(
                    AppLog.T.POSTS,
                    "Tag lookup for '$name' on ${site.url} failed: ${response.toTaxonomyError().toLogString()}"
                )
                null
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

    private suspend fun createTagReturningId(
        client: WpApiClient,
        name: String,
        site: SiteModel
    ): Long? {
        val response = client.request { requestBuilder ->
            requestBuilder.terms().create(
                termEndpointType = TermEndpointType.Tags,
                params = TermCreateParams(name = name)
            )
        }
        return when (response) {
            is WpRequestResult.Success -> response.response.data.id
            else -> {
                appLogWrapper.e(
                    AppLog.T.POSTS,
                    "Creating tag '$name' on ${site.url} failed: ${response.toTaxonomyError().toLogString()}"
                )
                null
            }
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
     */
    suspend fun resolveTagNames(site: SiteModel, tagIds: List<Long>): List<String> {
        if (tagIds.isEmpty()) return emptyList()

        val client = wpApiClientProvider.getWpApiClient(site)
        val namesById = mutableMapOf<Long, String>()
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
                    params = null
                }
            }
        }
        return tagIds.mapNotNull { namesById[it] }
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
        private const val HTTP_UNAUTHORIZED = 401u
        private const val HTTP_FORBIDDEN = 403u
    }
}
