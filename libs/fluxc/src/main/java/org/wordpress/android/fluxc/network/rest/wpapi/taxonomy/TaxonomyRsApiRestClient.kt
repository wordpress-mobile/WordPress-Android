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
import uniffi.wp_api.CategoryCreateParams
import uniffi.wp_api.CategoryDeleteResponse
import uniffi.wp_api.CategoryListParams
import uniffi.wp_api.CategoryUpdateParams
import uniffi.wp_api.CategoryWithEditContext
import uniffi.wp_api.TagCreateParams
import uniffi.wp_api.TagDeleteResponse
import uniffi.wp_api.TagListParams
import uniffi.wp_api.TagUpdateParams
import uniffi.wp_api.TagWithEditContext
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
            val client = wpApiClientProvider.getWpApiClient(site)

            when (term.taxonomy) {
                DEFAULT_TAXONOMY_CATEGORY -> deleteCategory(client, term, site)
                DEFAULT_TAXONOMY_TAG -> deleteTag(client, term, site)
                else -> {} // TODO We are not supporting any other taxonomy yet
            }
        }
    }

    private suspend fun deleteCategory(
        client: WpApiClient,
        term: TermModel,
        site: SiteModel
    ) {
        val categoriesResponse = client.request { requestBuilder ->
            requestBuilder.categories().delete(categoryId = term.id.toLong())
        }
        handleDeleteResponse(
            response = categoriesResponse,
            termType = "category",
            term = term,
            site = site,
            taxonomy = DEFAULT_TAXONOMY_CATEGORY,
            extractData = { it.response.data },
            checkDeleted = { data -> (data as CategoryDeleteResponse).deleted }
        )
    }

    private suspend fun deleteTag(
        client: WpApiClient,
        term: TermModel,
        site: SiteModel
    ) {
        val tagsResponse = client.request { requestBuilder ->
            requestBuilder.tags().delete(tagId = term.id.toLong())
        }
        handleDeleteResponse(
            response = tagsResponse,
            termType = "tag",
            term = term,
            site = site,
            taxonomy = DEFAULT_TAXONOMY_TAG,
            extractData = { it.response.data },
            checkDeleted = { data -> (data as TagDeleteResponse).deleted }
        )
    }

    @Suppress("LongParameterList")
    private inline fun <T> handleDeleteResponse(
        response: WpRequestResult<T>,
        termType: String,
        term: TermModel,
        site: SiteModel,
        taxonomy: String,
        extractData: (WpRequestResult.Success<T>) -> Any,
        checkDeleted: (Any) -> Boolean
    ) {
        when (response) {
            is WpRequestResult.Success -> {
                val data = extractData(response)
                appLogWrapper.d(AppLog.T.POSTS, "Deleted $termType: ${term.name} - ${checkDeleted(data)}")
                if (checkDeleted(data)) {
                    val termModel = createTermModelForDelete(term, site, taxonomy)
                    notifyTermDeleted(RemoteTermPayload(termModel, site))
                } else {
                    notifyFailedDeleting(termType, site, term)
                }
            }
            else -> {
                notifyFailedDeleting(termType, site, term)
            }
        }
    }

    private fun notifyFailedDeleting(termType: String, site: SiteModel, term: TermModel) {
        appLogWrapper.e(AppLog.T.POSTS, "Failed deleting $termType")
        val payload = RemoteTermPayload(term, site)
        payload.error = TaxonomyError(TaxonomyErrorType.GENERIC_ERROR, "")
        notifyTermDeleted(payload)
    }

    fun createTerm(site: SiteModel, term: TermModel) {
        scope.launch {
            val client = wpApiClientProvider.getWpApiClient(site)

            when (term.taxonomy) {
                DEFAULT_TAXONOMY_CATEGORY -> createCategory(client, term, site)
                DEFAULT_TAXONOMY_TAG -> createTag(client, term, site)
                else -> {} // TODO We are not supporting any other taxonomy yet
            }
        }
    }

    private suspend fun createCategory(
        client: WpApiClient,
        term: TermModel,
        site: SiteModel
    ) {
        val categoriesResponse = client.request { requestBuilder ->
            requestBuilder.categories().create(
                CategoryCreateParams(
                    name = term.name,
                    description = term.description,
                    slug = term.slug,
                    parent = term.parentRemoteId
                )
            )
        }

        handleCreateResponse(
            response = categoriesResponse,
            termType = "category",
            term = term,
            site = site,
            extractData = { it.response.data },
            createTermModel = { data ->
                val category = data as CategoryWithEditContext
                TermModel(
                    category.id.toInt(),
                    site.id,
                    category.id,
                    DEFAULT_TAXONOMY_CATEGORY,
                    category.name,
                    category.slug,
                    category.description,
                    category.parent,
                    category.count.toInt()
                )
            }
        )
    }

    private suspend fun createTag(
        client: WpApiClient,
        term: TermModel,
        site: SiteModel
    ) {
        val tagResponse = client.request { requestBuilder ->
            requestBuilder.tags().create(
                TagCreateParams(
                    name = term.name,
                    description = term.description,
                    slug = term.slug,
                )
            )
        }
        handleCreateResponse(
            response = tagResponse,
            termType = "tag",
            term = term,
            site = site,
            extractData = { it.response.data },
            createTermModel = { data ->
                val tag = data as TagWithEditContext
                TermModel(
                    tag.id.toInt(),
                    site.id,
                    tag.id,
                    DEFAULT_TAXONOMY_TAG,
                    tag.name,
                    tag.slug,
                    tag.description,
                    0,
                    tag.count.toInt()
                )
            }
        )
    }

    @Suppress("LongParameterList")
    private inline fun <T> handleCreateResponse(
        response: WpRequestResult<T>,
        termType: String,
        term: TermModel,
        site: SiteModel,
        extractData: (WpRequestResult.Success<T>) -> Any,
        createTermModel: (Any) -> TermModel
    ) {
        when (response) {
            is WpRequestResult.Success -> {
                val data = extractData(response)
                val name = when (data) {
                    is CategoryWithEditContext -> data.name
                    is TagWithEditContext -> data.name
                    else -> "unknown"
                }
                appLogWrapper.d(AppLog.T.POSTS, "Created $termType: $name")
                val payload = RemoteTermPayload(createTermModel(data), site)
                notifyTermCreated(payload)
            }
            else -> {
                notifyFailedOperation(
                    operation = "creating",
                    termType = termType,
                    term = term,
                    site = site,
                    errorDetails = response.toString(),
                    notifier = ::notifyTermCreated
                )
            }
        }
    }

    fun updateTerm(site: SiteModel, term: TermModel) {
        scope.launch {
            if (term.remoteTermId < 0) {
                appLogWrapper.e(AppLog.T.POSTS, "Failed updating term: $term - id <= 0")
                val payload = RemoteTermPayload(term, site)
                payload.error = TaxonomyError(TaxonomyErrorType.GENERIC_ERROR, "")
                notifyTermCreated(payload) // FluxC uses notifyTermCreated for updates
                return@launch
            }

            val client = wpApiClientProvider.getWpApiClient(site)

            when (term.taxonomy) {
                DEFAULT_TAXONOMY_CATEGORY -> updateCategory(client, term, site)
                DEFAULT_TAXONOMY_TAG -> updateTag(client, term, site)
                else -> {} // TODO We are not supporting any other taxonomy yet
            }
        }
    }

    private suspend fun updateCategory(
        client: WpApiClient,
        term: TermModel,
        site: SiteModel
    ) {
        val categoriesResponse = client.request { requestBuilder ->
            requestBuilder.categories().update(
                categoryId = term.remoteTermId,
                params = CategoryUpdateParams(
                    name = term.name,
                    description = term.description,
                    slug = term.slug,
                    parent = term.parentRemoteId
                )
            )
        }
        handleUpdateResponse(
            response = categoriesResponse,
            termType = "category",
            term = term,
            site = site,
            extractData = { it.response.data },
            createTermModel = { data ->
                val category = data as CategoryWithEditContext
                TermModel(
                    category.id.toInt(),
                    site.id,
                    category.id,
                    DEFAULT_TAXONOMY_CATEGORY,
                    category.name,
                    category.slug,
                    category.description,
                    category.parent,
                    category.count.toInt()
                )
            }
        )
    }

    private suspend fun updateTag(
        client: WpApiClient,
        term: TermModel,
        site: SiteModel
    ) {
        val tagResponse = client.request { requestBuilder ->
            requestBuilder.tags().update(
                tagId = term.remoteTermId,
                params = TagUpdateParams(
                    name = term.name,
                    description = term.description,
                    slug = term.slug,
                )
            )
        }
        handleUpdateResponse(
            response = tagResponse,
            termType = "tag",
            term = term,
            site = site,
            extractData = { it.response.data },
            createTermModel = { data ->
                val tag = data as TagWithEditContext
                TermModel(
                    tag.id.toInt(),
                    site.id,
                    tag.id,
                    DEFAULT_TAXONOMY_TAG,
                    tag.name,
                    tag.slug,
                    tag.description,
                    0,
                    tag.count.toInt()
                )
            }
        )
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
        when (response) {
            is WpRequestResult.Success -> {
                val data = extractData(response)
                val name = when (data) {
                    is CategoryWithEditContext -> data.name
                    is TagWithEditContext -> data.name
                    else -> "unknown"
                }
                appLogWrapper.d(AppLog.T.POSTS, "${termType.replaceFirstChar { it.uppercase() }} updated: $name")
                val payload = RemoteTermPayload(createTermModel(data), site)
                notifyTermCreated(payload) // FluxC uses notifyTermCreated for updates
            }
            else -> {
                notifyFailedOperation(
                    operation = "updating",
                    termType = termType,
                    term = term,
                    site = site,
                    errorDetails = response.toString(),
                    notifier = ::notifyTermCreated
                )
            }
        }
    }

    fun fetchTerms(site: SiteModel, taxonomyName: String) {
        scope.launch {
            val client = wpApiClientProvider.getWpApiClient(site)

            when (taxonomyName) {
                DEFAULT_TAXONOMY_CATEGORY -> fetchCategories(client, site)
                DEFAULT_TAXONOMY_TAG -> fetchTags(client, site)
                else -> {} // TODO We are not supporting any other taxonomy yet
            }
        }
    }

    private suspend fun fetchCategories(
        client: WpApiClient,
        site: SiteModel
    ) {
        val categoriesResponse = client.request { requestBuilder ->
            requestBuilder.categories().listWithEditContext(
                CategoryListParams()
            )
        }
        handleFetchResponse(
            response = categoriesResponse,
            termType = "categories",
            taxonomy = DEFAULT_TAXONOMY_CATEGORY,
            site = site,
            extractData = { it.response.data },
            createTermModels = { data ->
                (data as List<*>).map { category ->
                    val cat = category as CategoryWithEditContext
                    TermModel(
                        cat.id.toInt(),
                        site.id,
                        cat.id,
                        DEFAULT_TAXONOMY_CATEGORY,
                        cat.name,
                        cat.slug,
                        cat.description,
                        cat.parent,
                        cat.count.toInt()
                    )
                }
            }
        )
    }

    private suspend fun fetchTags(
        client: WpApiClient,
        site: SiteModel
    ) {
        val tagsResponse = client.request { requestBuilder ->
            requestBuilder.tags().listWithEditContext(
                TagListParams()
            )
        }
        handleFetchResponse(
            response = tagsResponse,
            termType = "tags",
            taxonomy = DEFAULT_TAXONOMY_TAG,
            site = site,
            extractData = { it.response.data },
            createTermModels = { data ->
                (data as List<*>).map { tag ->
                    val t = tag as TagWithEditContext
                    TermModel(
                        t.id.toInt(),
                        site.id,
                        t.id,
                        DEFAULT_TAXONOMY_TAG,
                        t.name,
                        t.slug,
                        t.description,
                        0,
                        t.count.toInt()
                    )
                }
            }
        )
    }

    @Suppress("LongParameterList")
    private inline fun <T> handleFetchResponse(
        response: WpRequestResult<T>,
        termType: String,
        taxonomy: String,
        site: SiteModel,
        extractData: (WpRequestResult.Success<T>) -> Any,
        createTermModels: (Any) -> List<TermModel>
    ) {
        val termsResponsePayload = when (response) {
            is WpRequestResult.Success -> {
                val data = extractData(response)
                val dataList = data as List<*>
                appLogWrapper.d(AppLog.T.POSTS, "Fetched $termType list: ${dataList.size}")
                createTermsResponsePayload(
                    createTermModels(data),
                    site,
                    taxonomy
                )
            }
            else -> {
                appLogWrapper.e(AppLog.T.POSTS, "Fetch $termType list failed: $response")
                createErrorResponsePayload(taxonomy)
            }
        }
        notifyTermsFetched(termsResponsePayload)
    }

    private fun notifyTermsFetched(
        payload: FetchTermsResponsePayload,
    ) {
        dispatcher.dispatch(TaxonomyActionBuilder.newFetchedTermsAction(payload))
    }

    private fun notifyTermCreated(
        payload: RemoteTermPayload,
    ) {
        dispatcher.dispatch(TaxonomyActionBuilder.newPushedTermAction(payload))
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


    private fun createTermModelForDelete(term: TermModel, site: SiteModel, taxonomy: String): TermModel {
        return TermModel(
            term.id,
            site.id,
            term.id.toLong(),
            taxonomy,
            term.name,
            term.slug,
            term.description,
            term.parentRemoteId,
            term.postCount
        )
    }

    private fun createTermsResponsePayload(
        terms: List<TermModel>,
        site: SiteModel,
        taxonomyName: String
    ): FetchTermsResponsePayload = FetchTermsResponsePayload(
        TermsModel(terms),
        site,
        taxonomyName
    )
}
