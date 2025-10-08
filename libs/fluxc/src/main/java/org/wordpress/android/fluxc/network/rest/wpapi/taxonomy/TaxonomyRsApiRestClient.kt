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
                termId = term.id.toLong()
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
                        term.id.toLong(),
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
                    notifyFailedDeleting(taxonomyName, site, term)
                }
            }
            else -> {
                notifyFailedDeleting(taxonomyName, site, term)
            }
        }
    }

    private fun notifyFailedDeleting(taxonomyName: String, site: SiteModel, term: TermModel) {
        appLogWrapper.e(AppLog.T.POSTS, "Failed deleting $taxonomyName")
        val payload = RemoteTermPayload(term, site)
        payload.error = TaxonomyError(TaxonomyErrorType.GENERIC_ERROR, "")
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
                appLogWrapper.e(AppLog.T.POSTS, "Failed creating $taxonomyName: ${term.name} - $termResponse")
                val payload = RemoteTermPayload(term, site)
                payload.error = TaxonomyError(TaxonomyErrorType.GENERIC_ERROR, "")
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
                appLogWrapper.e(AppLog.T.POSTS, "Failed updating ${term.name}: $termResponse")
                val payload = RemoteTermPayload(term, site)
                payload.error = TaxonomyError(TaxonomyErrorType.GENERIC_ERROR, "")
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
        val termsResponse = client.request { requestBuilder ->
            requestBuilder.terms().listWithEditContext(
                termEndpointType = termEndpointType,
                params = TermListParams()
            )
        }
        val termsResponsePayload = when (termsResponse) {
            is WpRequestResult.Success -> {
                appLogWrapper.d(AppLog.T.POSTS, "Fetched $taxonomyName list: ${termsResponse.response.data.size}")
                FetchTermsResponsePayload(
                    TermsModel(
                        termsResponse.response.data.map { term ->
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
                            )
                        },
                    ),
                    site,
                    taxonomyName
                )
            }
            else -> {
                appLogWrapper.e(AppLog.T.POSTS, "Fetch $termEndpointType list failed: $termsResponse")
                FetchTermsResponsePayload(
                    TaxonomyError(TaxonomyErrorType.GENERIC_ERROR, ""),
                    taxonomyName
                )
            }
        }
        dispatcher.dispatch(TaxonomyActionBuilder.newFetchedTermsAction(termsResponsePayload))
    }


    private fun TermEndpointType.toTaxonomyName(): String = when (this) {
        TermEndpointType.Categories -> DEFAULT_TAXONOMY_CATEGORY
        TermEndpointType.Tags -> DEFAULT_TAXONOMY_TAG
        is TermEndpointType.Custom -> this.v1
    }
}
