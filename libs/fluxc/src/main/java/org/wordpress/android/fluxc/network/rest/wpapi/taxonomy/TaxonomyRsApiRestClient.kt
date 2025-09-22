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
import org.wordpress.android.fluxc.store.TaxonomyStore.FetchTermsResponsePayload
import org.wordpress.android.fluxc.store.TaxonomyStore.TaxonomyError
import org.wordpress.android.fluxc.store.TaxonomyStore.TaxonomyErrorType
import org.wordpress.android.fluxc.utils.AppLogWrapper
import org.wordpress.android.util.AppLog
import rs.wordpress.api.kotlin.WpRequestResult
import uniffi.wp_api.TagListParams
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
    fun fetchPostTags(site: SiteModel, taxonomyName: String) {
        scope.launch {
            val client = wpApiClientProvider.getWpApiClient(site)

            val mediaResponse = client.request { requestBuilder ->
                requestBuilder.tags().listWithEditContext(
                    TagListParams()
                )
            }

            val termsResponsePayload = when (mediaResponse) {
                is WpRequestResult.Success -> {
                    appLogWrapper.d(AppLog.T.POSTS, "Fetched tags list: ${mediaResponse.response.data.size}")
                    mediaResponse.response.data.toFetchTermsResponsePayload(site, taxonomyName)
                }

                else -> {
                    appLogWrapper.e(AppLog.T.POSTS, "Fetch tags list failed: $mediaResponse")
                    FetchTermsResponsePayload(
                        TaxonomyError(TaxonomyErrorType.GENERIC_ERROR, ""),
                        taxonomyName
                    )
                }
            }
            notifyTagsFetched(termsResponsePayload)
        }
    }

    private fun notifyTagsFetched(
        payload: FetchTermsResponsePayload,
    ) {
        dispatcher.dispatch(TaxonomyActionBuilder.newFetchedTermsAction(payload))
    }

    private fun List<TagWithEditContext>.toFetchTermsResponsePayload(
        site: SiteModel,
        taxonomyName: String
    ): FetchTermsResponsePayload = FetchTermsResponsePayload(
        TermsModel(
            this.map {
                TermModel(
                    it.id.toInt(),
                    site.id,
                    it.id,
                    taxonomyName,
                    it.name,
                    it.slug,
                    it.description,
                    0,
                    it.count.toInt()
                )
            }
        ),
        site,
        taxonomyName
    )
}
