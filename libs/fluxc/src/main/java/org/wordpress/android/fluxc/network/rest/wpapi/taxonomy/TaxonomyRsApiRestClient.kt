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
                requestBuilder.categories().delete(
                    categoryId = term.id.toLong()
                )
            }

            when (categoriesResponse) {
                is WpRequestResult.Success -> {
                    val category = categoriesResponse.response.data
                    appLogWrapper.d(AppLog.T.POSTS, "Deleted category: ${term.name} - ${category.deleted}")
                    if (category.deleted) {
                        val payload = RemoteTermPayload(
                            TermModel(
                                term.id,
                                site.id,
                                term.id.toLong(),
                                TaxonomyStore.DEFAULT_TAXONOMY_CATEGORY,
                                term.name,
                                term.slug,
                                term.description,
                                term.parentRemoteId,
                                term.postCount
                            ),
                            site
                        )
                        notifyTermDeleted(payload)
                    } else {
                        notifyFailedDeletingCategory(site, term)
                    }
                }

                else -> {
                    notifyFailedDeletingCategory(site, term)
                }
            }
        }
    }

    private fun notifyFailedDeletingCategory(site: SiteModel, term: TermModel) {
        appLogWrapper.e(AppLog.T.POSTS, "Failed deleting category")
        val payload = RemoteTermPayload(term, site)
        payload.error = TaxonomyError(TaxonomyErrorType.GENERIC_ERROR, "")
        notifyTermDeleted(payload)
    }

    private fun deleteTag(site: SiteModel, term: TermModel) {
        scope.launch {
            val client = wpApiClientProvider.getWpApiClient(site)

            val tagsResponse = client.request { requestBuilder ->
                requestBuilder.tags().delete(
                    tagId = term.id.toLong()
                )
            }

            when (tagsResponse) {
                is WpRequestResult.Success -> {
                    val category = tagsResponse.response.data
                    appLogWrapper.d(AppLog.T.POSTS, "Deleted tag: ${term.name} - ${category.deleted}")
                    if (category.deleted) {
                        val payload = RemoteTermPayload(
                            TermModel(
                                term.id,
                                site.id,
                                term.id.toLong(),
                                TaxonomyStore.DEFAULT_TAXONOMY_TAG,
                                term.name,
                                term.slug,
                                term.description,
                                term.parentRemoteId,
                                term.postCount
                            ),
                            site
                        )
                        notifyTermDeleted(payload)
                    } else {
                        notifyFailedDeletingTag(site, term)
                    }
                }

                else -> {
                    notifyFailedDeletingTag(site, term)
                }
            }
        }
    }

    private fun notifyFailedDeletingTag(site: SiteModel, term: TermModel) {
        appLogWrapper.e(AppLog.T.POSTS, "Failed deleting tag")
        val payload = RemoteTermPayload(term, site)
        payload.error = TaxonomyError(TaxonomyErrorType.GENERIC_ERROR, "")
        notifyTermDeleted(payload)
    }

    fun createTerm(site: SiteModel, term: TermModel) {
        when (term.taxonomy) {
            DEFAULT_TAXONOMY_CATEGORY -> createCategory(site, term)
            DEFAULT_TAXONOMY_TAG -> createTag(site, term)
            else -> {} // TODO We are not supporting any other taxonomy yet
        }
    }

    private fun createCategory(site: SiteModel, term: TermModel) {
        scope.launch {
            val client = wpApiClientProvider.getWpApiClient(site)

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

            when (categoriesResponse) {
                is WpRequestResult.Success -> {
                    val category = categoriesResponse.response.data
                    appLogWrapper.d(AppLog.T.POSTS, "Created category: ${category.name}")
                    val payload = RemoteTermPayload(
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
                        ),
                        site
                    )
                    notifyTermCreated(payload)
                }

                else -> {
                    appLogWrapper.e(AppLog.T.POSTS, "Failed creating category: $categoriesResponse")
                    val payload = RemoteTermPayload(term, site)
                    payload.error = TaxonomyError(TaxonomyErrorType.GENERIC_ERROR, "")
                    notifyTermCreated(payload)
                }
            }
        }
    }

    private fun createTag(site: SiteModel, term: TermModel) {
        scope.launch {
            val client = wpApiClientProvider.getWpApiClient(site)

            val tagResponse = client.request { requestBuilder ->
                requestBuilder.tags().create(
                    TagCreateParams(
                        name = term.name,
                        description = term.description,
                        slug = term.slug,
                    )
                )
            }

            when (tagResponse) {
                is WpRequestResult.Success -> {
                    val tag = tagResponse.response.data
                    appLogWrapper.d(AppLog.T.POSTS, "Created tag: ${tag.name}")
                    val payload = RemoteTermPayload(
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
                        ),
                        site
                    )
                    notifyTermCreated(payload)
                }

                else -> {
                    appLogWrapper.e(AppLog.T.POSTS, "Failed creating tag: $tagResponse")
                    val payload = RemoteTermPayload(term, site)
                    payload.error = TaxonomyError(TaxonomyErrorType.GENERIC_ERROR, "")
                    notifyTermCreated(payload)
                }
            }
        }
    }

    fun updateTerm(site: SiteModel, term: TermModel) {
        when (term.taxonomy) {
            DEFAULT_TAXONOMY_CATEGORY -> updateCategory(site, term)
            DEFAULT_TAXONOMY_TAG -> updateTag(site, term)
            else -> {} // TODO We are not supporting any other taxonomy yet
        }
    }

    private fun updateCategory(site: SiteModel, term: TermModel) {
        scope.launch {
            if (term.remoteTermId < 0) {
                appLogWrapper.e(AppLog.T.POSTS, "Failed updating category: $term - id <= 0")
                val payload = RemoteTermPayload(term, site)
                payload.error = TaxonomyError(TaxonomyErrorType.GENERIC_ERROR, "")
                notifyTermCreated(payload) // FluxC uses notifyTermCreated for updates
            }

            val client = wpApiClientProvider.getWpApiClient(site)

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

            when (categoriesResponse) {
                is WpRequestResult.Success -> {
                    val category = categoriesResponse.response.data
                    appLogWrapper.d(AppLog.T.POSTS, "Category updated: ${category.name}")
                    val payload = RemoteTermPayload(
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
                        ),
                        site
                    )
                    notifyTermCreated(payload) // FluxC uses notifyTermCreated for updates
                }

                else -> {
                    appLogWrapper.e(AppLog.T.POSTS, "Failed updating category: $categoriesResponse")
                    val payload = RemoteTermPayload(term, site)
                    payload.error = TaxonomyError(TaxonomyErrorType.GENERIC_ERROR, "")
                    notifyTermCreated(payload) // FluxC uses notifyTermCreated for updates
                }
            }
        }
    }

    private fun updateTag(site: SiteModel, term: TermModel) {
        scope.launch {
            if (term.remoteTermId < 0) {
                appLogWrapper.e(AppLog.T.POSTS, "Failed updating tag: $term - id <= 0")
                val payload = RemoteTermPayload(term, site)
                payload.error = TaxonomyError(TaxonomyErrorType.GENERIC_ERROR, "")
                notifyTermCreated(payload) // FluxC uses notifyTermCreated for updates
            }

            val client = wpApiClientProvider.getWpApiClient(site)

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

            when (tagResponse) {
                is WpRequestResult.Success -> {
                    val tag = tagResponse.response.data
                    appLogWrapper.d(AppLog.T.POSTS, "Tag updated: ${tag.name}")
                    val payload = RemoteTermPayload(
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
                        ),
                        site
                    )
                    notifyTermCreated(payload) // FluxC uses notifyTermCreated for updates
                }

                else -> {
                    appLogWrapper.e(AppLog.T.POSTS, "Failed updating tag: $tagResponse")
                    val payload = RemoteTermPayload(term, site)
                    payload.error = TaxonomyError(TaxonomyErrorType.GENERIC_ERROR, "")
                    notifyTermCreated(payload) // FluxC uses notifyTermCreated for updates
                }
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
                    FetchTermsResponsePayload(
                        TaxonomyError(TaxonomyErrorType.GENERIC_ERROR, ""),
                        TaxonomyStore.DEFAULT_TAXONOMY_CATEGORY
                    )
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
                                DEFAULT_TAXONOMY_TAG,
                                tag.name,
                                tag.slug,
                                tag.description,
                                0,
                                tag.count.toInt()
                            )
                        },
                        site,
                        DEFAULT_TAXONOMY_TAG
                    )
                }

                else -> {
                    appLogWrapper.e(AppLog.T.POSTS, "Fetch tags list failed: $tagsResponse")
                    FetchTermsResponsePayload(
                        TaxonomyError(TaxonomyErrorType.GENERIC_ERROR, ""),
                        DEFAULT_TAXONOMY_TAG
                    )
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
