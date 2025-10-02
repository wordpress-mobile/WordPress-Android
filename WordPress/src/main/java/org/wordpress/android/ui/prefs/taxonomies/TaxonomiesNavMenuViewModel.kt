package org.wordpress.android.ui.prefs.taxonomies

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.rest.wpapi.rs.WpApiClientProvider
import org.wordpress.android.fluxc.utils.AppLogWrapper
import org.wordpress.android.util.AppLog
import rs.wordpress.api.kotlin.WpRequestResult
import uniffi.wp_api.TaxonomyListParams
import uniffi.wp_api.TaxonomyTypeDetailsWithEditContext
import javax.inject.Inject

class TaxonomiesNavMenuViewModel @Inject constructor(
    private val wpApiClientProvider: WpApiClientProvider,
    private val appLogWrapper: AppLogWrapper,
) : ViewModel() {
    // LiveData because this is observed from Java
    private val _taxonomies = MutableLiveData<List<TaxonomyTypeDetailsWithEditContext>>()
    val taxonomies: LiveData<List<TaxonomyTypeDetailsWithEditContext>> = _taxonomies

    fun fetchTaxonomies(site: SiteModel) {
        if (!site.isUsingSelfHostedRestApi) {
            appLogWrapper.d(
                AppLog.T.API,
                "Taxonomies - Taxonomies cannot be fetched: Application Password not available"
            )
            return
        }
        viewModelScope.launch {
            val client = wpApiClientProvider.getWpApiClient(site)
            val response = client.request { requestBuilder ->
                requestBuilder.taxonomies().listWithEditContext(TaxonomyListParams())
            }
            when (response) {
                is WpRequestResult.Success -> {
                    val list = response.response.data
                    appLogWrapper.d(AppLog.T.API, "Taxonomies - Fetched taxonomies ${list.taxonomyTypes.size}")
                    val taxonomies = mutableListOf<TaxonomyTypeDetailsWithEditContext>()
                    list.taxonomyTypes.forEach { type ->
                        appLogWrapper.d(AppLog.T.API, "Taxonomies - Taxonomy ${type.value.name}")
                        if (type.value.visibility.showInNavMenus) {
                            taxonomies.add(type.value)
                        }
                    }
                    _taxonomies.value = taxonomies
                }

                else -> {
                    appLogWrapper.e(AppLog.T.API, "Taxonomies - Error fetching taxonomies")
                }
            }
        }
    }
}
