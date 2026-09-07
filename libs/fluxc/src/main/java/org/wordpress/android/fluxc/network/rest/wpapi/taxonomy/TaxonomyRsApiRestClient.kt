package org.wordpress.android.fluxc.network.rest.wpapi.taxonomy

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
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
                        term.id.toInt(),
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
                        term.id.toInt(),
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

    private fun AnyTermWithViewContext.toTermModel(site: SiteModel, taxonomyName: String) = TermModel(
        id.toInt(),
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
        private const val TERMS_PER_PAGE = 100u
        private const val HTTP_UNAUTHORIZED = 401u
        private const val HTTP_FORBIDDEN = 403u
    }
}
