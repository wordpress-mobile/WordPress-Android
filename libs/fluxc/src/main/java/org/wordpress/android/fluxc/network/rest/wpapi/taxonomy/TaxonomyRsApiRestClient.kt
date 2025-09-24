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
import uniffi.wp_api.TagListParams
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
    fun createPostCategory(site: SiteModel, term: TermModel) {
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

    fun fetchPostCategories(site: SiteModel) {
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

    fun fetchPostTags(site: SiteModel) {
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
