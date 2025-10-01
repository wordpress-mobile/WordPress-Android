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
import rs.wordpress.api.kotlin.WpApiClient
import rs.wordpress.api.kotlin.WpRequestResult
import uniffi.wp_api.TermCreateParams
import uniffi.wp_api.TermEndpointType
import uniffi.wp_api.TermListParams
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
                termEndpointType = TermEndpointType.Tags,
                termId = term.id.toLong()
            )
        }
        when (termResponse) {
            is WpRequestResult.Success -> {
                appLogWrapper.d(AppLog.T.POSTS, "Deleting $taxonomyName: ${term.name} - ${termResponse.response.data.deleted}")
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
                    parent = if (term.parentRemoteId > 0) term.parentRemoteId else null
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
//        scope.launch {
//            if (term.remoteTermId < 0) {
//                appLogWrapper.e(AppLog.T.POSTS, "Failed updating term: $term - id <= 0")
//                val payload = RemoteTermPayload(term, site)
//                payload.error = TaxonomyError(TaxonomyErrorType.GENERIC_ERROR, "")
//                notifyTermCreated(payload) // FluxC uses notifyTermCreated for updates
//                return@launch
//            }
//
//            val client = wpApiClientProvider.getWpApiClient(site)
//
//            when (term.taxonomy) {
//                DEFAULT_TAXONOMY_CATEGORY -> updateCategory(client, term, site)
//                DEFAULT_TAXONOMY_TAG -> updateTag(client, term, site)
//                else -> {} // TODO We are not supporting any other taxonomy yet
//            }
//        }
    }

    private suspend fun updateCategory(
        client: WpApiClient,
        term: TermModel,
        site: SiteModel
    ) {
//        val categoriesResponse = client.request { requestBuilder ->
//            requestBuilder.categories().update(
//                categoryId = term.remoteTermId,
//                params = CategoryUpdateParams(
//                    name = term.name,
//                    description = term.description,
//                    slug = term.slug,
//                    parent = term.parentRemoteId
//                )
//            )
//        }
//        handleUpdateResponse(
//            response = categoriesResponse,
//            termType = "category",
//            term = term,
//            site = site,
//            extractData = { it.response.data },
//            createTermModel = { data ->
//                val category = data as CategoryWithEditContext
//                TermModel(
//                    category.id.toInt(),
//                    site.id,
//                    category.id,
//                    DEFAULT_TAXONOMY_CATEGORY,
//                    category.name,
//                    category.slug,
//                    category.description,
//                    category.parent,
//                    category.count.toInt()
//                )
//            }
//        )
    }

    private suspend fun updateTag(
        client: WpApiClient,
        term: TermModel,
        site: SiteModel
    ) {
//        val tagResponse = client.request { requestBuilder ->
//            requestBuilder.tags().update(
//                tagId = term.remoteTermId,
//                params = TagUpdateParams(
//                    name = term.name,
//                    description = term.description,
//                    slug = term.slug,
//                )
//            )
//        }
//        handleUpdateResponse(
//            response = tagResponse,
//            termType = "tag",
//            term = term,
//            site = site,
//            extractData = { it.response.data },
//            createTermModel = { data ->
//                val tag = data as TagWithEditContext
//                TermModel(
//                    tag.id.toInt(),
//                    site.id,
//                    tag.id,
//                    DEFAULT_TAXONOMY_TAG,
//                    tag.name,
//                    tag.slug,
//                    tag.description,
//                    0,
//                    tag.count.toInt()
//                )
//            }
//        )
    }

    @Suppress("LongParameterList")
    private inline fun <T> handleUpdateResponse(
        response: WpRequestResult<T>,
        termType: String,
        term: TermModel,
        site: SiteModel,
        extractData: (WpRequestResult.Success<T>) -> Any,
        createTermModel: (Any) -> TermModel
    ) {
//        when (response) {
//            is WpRequestResult.Success -> {
//                val data = extractData(response)
//                val name = when (data) {
//                    is CategoryWithEditContext -> data.name
//                    is TagWithEditContext -> data.name
//                    else -> "unknown"
//                }
//                appLogWrapper.d(AppLog.T.POSTS, "${termType.replaceFirstChar { it.uppercase() }} updated: $name")
//                val payload = RemoteTermPayload(createTermModel(data), site)
//                notifyTermCreated(payload) // FluxC uses notifyTermCreated for updates
//            }
//            else -> {
//                notifyFailedOperation(
//                    operation = "updating",
//                    termType = termType,
//                    term = term,
//                    site = site,
//                    errorDetails = response.toString(),
//                    notifier = ::notifyTermCreated
//                )
//            }
//        }
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
                createTermsResponsePayload(
                    terms = termsResponse.response.data.map { term ->
                        TermModel(
                            term.id.toInt(),
                            site.id,
                            term.id,
                            taxonomyName,
                            term.name,
                            term.slug,
                            term.description,
                            term.parent ?: 0,
                            term.count.toInt()
                        )
                    },
                    site,
                    taxonomyName
                )
            }
            else -> {
                appLogWrapper.e(AppLog.T.POSTS, "Fetch $termEndpointType list failed: $termsResponse")
                createErrorResponsePayload(taxonomyName)
            }
        }
        notifyTermsFetched(termsResponsePayload)
    }

    private fun notifyTermsFetched(
        payload: FetchTermsResponsePayload,
    ) {
        dispatcher.dispatch(TaxonomyActionBuilder.newFetchedTermsAction(payload))
    }

    private fun notifyTermDeleted(
        payload: RemoteTermPayload,
    ) {
        dispatcher.dispatch(TaxonomyActionBuilder.newDeletedTermAction(payload))
    }

    @Suppress("LongParameterList")
    private fun notifyFailedOperation(
        operation: String,
        termType: String,
        term: TermModel,
        site: SiteModel,
        errorDetails: String,
        notifier: (RemoteTermPayload) -> Unit
    ) {
        appLogWrapper.e(AppLog.T.POSTS, "Failed $operation $termType: $errorDetails")
        val payload = RemoteTermPayload(term, site)
        payload.error = TaxonomyError(TaxonomyErrorType.GENERIC_ERROR, "")
        notifier(payload)
    }

    private fun createErrorResponsePayload(taxonomyName: String): FetchTermsResponsePayload =
        FetchTermsResponsePayload(
            TaxonomyError(TaxonomyErrorType.GENERIC_ERROR, ""),
            taxonomyName
        )


    private fun createTermsResponsePayload(
        terms: List<TermModel>,
        site: SiteModel,
        taxonomyName: String
    ): FetchTermsResponsePayload = FetchTermsResponsePayload(
        TermsModel(terms),
        site,
        taxonomyName
    )

    private fun TermEndpointType.toTaxonomyName(): String = when (this) {
        TermEndpointType.Categories -> DEFAULT_TAXONOMY_CATEGORY
        TermEndpointType.Tags -> DEFAULT_TAXONOMY_TAG
        is TermEndpointType.Custom -> this.v1
    }
}
