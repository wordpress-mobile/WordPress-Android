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
import org.wordpress.android.fluxc.store.TaxonomyStore
import org.wordpress.android.fluxc.store.TaxonomyStore.DEFAULT_TAXONOMY_CATEGORY
import org.wordpress.android.fluxc.store.TaxonomyStore.DEFAULT_TAXONOMY_TAG
import org.wordpress.android.fluxc.store.TaxonomyStore.FetchTermsResponsePayload
import org.wordpress.android.fluxc.store.TaxonomyStore.RemoteTermPayload
import org.wordpress.android.fluxc.store.TaxonomyStore.TaxonomyError
import org.wordpress.android.fluxc.store.TaxonomyStore.TaxonomyErrorType
import org.wordpress.android.fluxc.utils.AppLogWrapper
import org.wordpress.android.util.AppLog
import rs.wordpress.api.kotlin.WpRequestResult
import uniffi.wp_api.CategoryCreateParams
import uniffi.wp_api.CategoryListParams
import uniffi.wp_api.CategoryUpdateParams
import uniffi.wp_api.TagCreateParams
import uniffi.wp_api.TagListParams
import uniffi.wp_api.TagUpdateParams
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
        when (term.taxonomy) {
            DEFAULT_TAXONOMY_CATEGORY -> deleteCategory(site, term)
            DEFAULT_TAXONOMY_TAG -> deleteTag(site, term)
            else -> {} // TODO We are not supporting any other taxonomy yet
        }
    }

    private fun deleteCategory(site: SiteModel, term: TermModel) {
        scope.launch {
            val client = wpApiClientProvider.getWpApiClient(site)

            val categoriesResponse = client.request { requestBuilder ->
                requestBuilder.categories().delete(categoryId = term.id.toLong())
            }

            when (categoriesResponse) {
                is WpRequestResult.Success -> {
                    val category = categoriesResponse.response.data
                    appLogWrapper.d(AppLog.T.POSTS, "Deleted category: ${term.name} - ${category.deleted}")
                    if (category.deleted) {
                        val termModel = createTermModelForDelete(term, site, TaxonomyStore.DEFAULT_TAXONOMY_CATEGORY)
                        notifyTermDeleted(RemoteTermPayload(termModel, site))
                    } else {
                        notifyFailedDeleting("category", site, term)
                    }
                }
                else -> {
                    notifyFailedDeleting("category", site, term)
                }
            }
        }
    }

    private fun deleteTag(site: SiteModel, term: TermModel) {
        scope.launch {
            val client = wpApiClientProvider.getWpApiClient(site)

            val tagsResponse = client.request { requestBuilder ->
                requestBuilder.tags().delete(tagId = term.id.toLong())
            }

            when (tagsResponse) {
                is WpRequestResult.Success -> {
                    val tag = tagsResponse.response.data
                    appLogWrapper.d(AppLog.T.POSTS, "Deleted tag: ${term.name} - ${tag.deleted}")
                    if (tag.deleted) {
                        val termModel = createTermModelForDelete(term, site, TaxonomyStore.DEFAULT_TAXONOMY_TAG)
                        notifyTermDeleted(RemoteTermPayload(termModel, site))
                    } else {
                        notifyFailedDeleting("tag", site, term)
                    }
                }
                else -> {
                    notifyFailedDeleting("tag", site, term)
                }
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
                DEFAULT_TAXONOMY_CATEGORY -> {
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
                            val category = data as uniffi.wp_api.CategoryWithEditContext
                            TermModel(
                                category.id.toInt(),
                                site.id,
                                category.id,
                                TaxonomyStore.DEFAULT_TAXONOMY_CATEGORY,
                                category.name,
                                category.slug,
                                category.description,
                                category.parent,
                                category.count.toInt()
                            )
                        }
                    )
                }

                DEFAULT_TAXONOMY_TAG -> {
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
                            val tag = data as uniffi.wp_api.TagWithEditContext
                            TermModel(
                                tag.id.toInt(),
                                site.id,
                                tag.id,
                                TaxonomyStore.DEFAULT_TAXONOMY_TAG,
                                tag.name,
                                tag.slug,
                                tag.description,
                                0,
                                tag.count.toInt()
                            )
                        }
                    )
                }

                else -> {} // TODO We are not supporting any other taxonomy yet
            }
        }
    }

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
                    is uniffi.wp_api.CategoryWithEditContext -> data.name
                    is uniffi.wp_api.TagWithEditContext -> data.name
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
                DEFAULT_TAXONOMY_CATEGORY -> {
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
                            val category = data as uniffi.wp_api.CategoryWithEditContext
                            TermModel(
                                category.id.toInt(),
                                site.id,
                                category.id,
                                TaxonomyStore.DEFAULT_TAXONOMY_CATEGORY,
                                category.name,
                                category.slug,
                                category.description,
                                category.parent,
                                category.count.toInt()
                            )
                        }
                    )
                }

                DEFAULT_TAXONOMY_TAG -> {
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
                            val tag = data as uniffi.wp_api.TagWithEditContext
                            TermModel(
                                tag.id.toInt(),
                                site.id,
                                tag.id,
                                TaxonomyStore.DEFAULT_TAXONOMY_TAG,
                                tag.name,
                                tag.slug,
                                tag.description,
                                0,
                                tag.count.toInt()
                            )
                        }
                    )
                }

                else -> {} // TODO We are not supporting any other taxonomy yet
            }
        }
    }

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
                    is uniffi.wp_api.CategoryWithEditContext -> data.name
                    is uniffi.wp_api.TagWithEditContext -> data.name
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
        when (taxonomyName) {
            DEFAULT_TAXONOMY_CATEGORY -> fetchCategories(site)
            DEFAULT_TAXONOMY_TAG -> fetchTags(site)
            else -> {} // TODO We are not supporting any other taxonomy yet
        }
    }

    private fun fetchCategories(site: SiteModel) {
        scope.launch {
            val client = wpApiClientProvider.getWpApiClient(site)

            val categoriesResponse = client.request { requestBuilder ->
                requestBuilder.categories().listWithEditContext(
                    CategoryListParams()
                )
            }

            val termsResponsePayload = when (categoriesResponse) {
                is WpRequestResult.Success -> {
                    appLogWrapper.d(AppLog.T.POSTS, "Fetched categories list: ${categoriesResponse.response.data.size}")
                    createTermsResponsePayload(
                        categoriesResponse.response.data.map { category ->
                            TermModel(
                                category.id.toInt(),
                                site.id,
                                category.id,
                                TaxonomyStore.DEFAULT_TAXONOMY_CATEGORY,
                                category.name,
                                category.slug,
                                category.description,
                                0,
                                category.count.toInt()
                            )
                        },
                        site,
                        TaxonomyStore.DEFAULT_TAXONOMY_CATEGORY
                    )
                }

                else -> {
                    appLogWrapper.e(AppLog.T.POSTS, "Fetch categories list failed: $categoriesResponse")
                    createErrorResponsePayload(TaxonomyStore.DEFAULT_TAXONOMY_CATEGORY)
                }
            }
            notifyTermsFetched(termsResponsePayload)
        }
    }

    private fun fetchTags(site: SiteModel) {
        scope.launch {
            val client = wpApiClientProvider.getWpApiClient(site)

            val tagsResponse = client.request { requestBuilder ->
                requestBuilder.tags().listWithEditContext(
                    TagListParams()
                )
            }

            val termsResponsePayload = when (tagsResponse) {
                is WpRequestResult.Success -> {
                    appLogWrapper.d(AppLog.T.POSTS, "Fetched tags list: ${tagsResponse.response.data.size}")
                    createTermsResponsePayload(
                        tagsResponse.response.data.map { tag ->
                            TermModel(
                                tag.id.toInt(),
                                site.id,
                                tag.id,
                                TaxonomyStore.DEFAULT_TAXONOMY_TAG,
                                tag.name,
                                tag.slug,
                                tag.description,
                                0,
                                tag.count.toInt()
                            )
                        },
                        site,
                        TaxonomyStore.DEFAULT_TAXONOMY_TAG
                    )
                }

                else -> {
                    appLogWrapper.e(AppLog.T.POSTS, "Fetch tags list failed: $tagsResponse")
                    createErrorResponsePayload(TaxonomyStore.DEFAULT_TAXONOMY_TAG)
                }
            }
            notifyTermsFetched(termsResponsePayload)
        }
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
