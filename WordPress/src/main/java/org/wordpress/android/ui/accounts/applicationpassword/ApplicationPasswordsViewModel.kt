package org.wordpress.android.ui.accounts.applicationpassword

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.rest.wpapi.rs.WpApiClientProvider
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.fluxc.utils.AppLogWrapper
import org.wordpress.android.modules.IO_THREAD
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.dataview.DataViewDropdownItem
import org.wordpress.android.ui.dataview.DataViewFieldType
import org.wordpress.android.ui.dataview.DataViewItem
import org.wordpress.android.ui.dataview.DataViewItemField
import org.wordpress.android.ui.dataview.DataViewViewModel
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.NetworkUtilsWrapper
import rs.wordpress.api.kotlin.WpApiClient
import rs.wordpress.api.kotlin.WpRequestResult
import uniffi.wp_api.ApplicationPasswordAppId
import uniffi.wp_api.ApplicationPasswordUuid
import uniffi.wp_api.ApplicationPasswordWithViewContext
import uniffi.wp_api.IpAddress
import uniffi.wp_api.WpApiParamOrder
import uniffi.wp_api.parseAsWpSiteHealthTestsRequestFilterBackgroundUpdatesResponse
import javax.inject.Inject
import javax.inject.Named

@HiltViewModel
class ApplicationPasswordsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val wpApiClientProvider: WpApiClientProvider,
    private val appLogWrapper: AppLogWrapper,
    private val selectedSiteRepository: SelectedSiteRepository,
    private val accountStore: AccountStore,
    @Named(UI_THREAD) mainDispatcher: CoroutineDispatcher,
    sharedPrefs: SharedPreferences,
    networkUtilsWrapper: NetworkUtilsWrapper,
    @Named(IO_THREAD) ioDispatcher: CoroutineDispatcher,
) : DataViewViewModel(
    mainDispatcher = mainDispatcher,
    appLogWrapper = appLogWrapper,
    sharedPrefs = sharedPrefs,
    networkUtilsWrapper = networkUtilsWrapper,
    selectedSiteRepository = selectedSiteRepository,
    accountStore = accountStore,
    ioDispatcher = ioDispatcher
) {
    override fun getSupportedSorts(): List<DataViewDropdownItem> = listOf(
        DataViewDropdownItem(id = SORT_BY_NAME_ID, titleRes = R.string.application_password_name_sort),
        DataViewDropdownItem(id = SORT_BY_CREATED_ID, titleRes = R.string.application_password_created_sort),
        DataViewDropdownItem(id = SORT_BY_LAST_USED_ID, titleRes = R.string.application_password_last_used_sort)
    )

    override suspend fun performNetworkRequest(
        page: Int,
        searchQuery: String,
        filter: DataViewDropdownItem?,
        sortOrder: WpApiParamOrder,
        sortBy: DataViewDropdownItem?,
    ): List<DataViewItem> = withContext(ioDispatcher) {
        val selectedSite = selectedSiteRepository.getSelectedSite()

        if (selectedSite == null) {
            val error = "No selected site to get Application Passwords"
            appLogWrapper.e(AppLog.T.API, error)
            onError(error)
            return@withContext emptyList()
        }

        val allApplicationPasswords = getApplicationPasswordsList(selectedSite)

        // Filter by search query
        val filteredPasswords = if (searchQuery.isBlank()) {
            allApplicationPasswords
        } else {
            allApplicationPasswords.filter { applicationPassword ->
                applicationPassword.name.contains(searchQuery, ignoreCase = true)
            }
        }

        // Sort the results
        val sortedPasswords = when (sortBy?.id) {
            SORT_BY_NAME_ID -> { // Sort by name
                if (sortOrder == WpApiParamOrder.ASC) {
                    filteredPasswords.sortedBy { it.name }
                } else {
                    filteredPasswords.sortedByDescending { it.name }
                }
            }
            SORT_BY_CREATED_ID -> { // Sort by created date
                if (sortOrder == WpApiParamOrder.ASC) {
                    filteredPasswords.sortedBy { it.created }
                } else {
                    filteredPasswords.sortedByDescending { it.created }
                }
            }
            SORT_BY_LAST_USED_ID -> { // Sort by last used
                if (sortOrder == WpApiParamOrder.ASC) {
                    filteredPasswords.sortedBy { it.lastUsed ?: "" }
                } else {
                    filteredPasswords.sortedByDescending { it.lastUsed ?: "" }
                }
            }
            else -> filteredPasswords
        }

        // Convert to DataViewItems and return
        sortedPasswords.map { password ->
            convertToDataViewItem(password)
        }
    }

    fun getApplicationPassword(uuid: String): ApplicationPasswordWithViewContext? {
        val item = uiState.value.items.firstOrNull {
            (it.data as? ApplicationPasswordWithViewContext)?.uuid?.uuid == uuid
        }
        return item?.data as? ApplicationPasswordWithViewContext
    }

    private fun convertToDataViewItem(applicationPassword: ApplicationPasswordWithViewContext): DataViewItem {
        return DataViewItem(
            id = applicationPassword.uuid.uuid.hashCode().toLong(),
            image = null, // No image for application passwords
            title = applicationPassword.name,
            fields = listOf(
                DataViewItemField(
                    value = formatLastUsed(applicationPassword.lastUsed),
                    valueType = DataViewFieldType.TEXT,
                    weight = 1f
                ),
                DataViewItemField(
                    value = applicationPassword.created,
                    valueType = DataViewFieldType.DATE,
                    weight = 0.8f
                )
            ),
            data = applicationPassword // Store the original object for click handling
        )
    }

    private fun formatLastUsed(lastUsed: String?): String {
        return if (lastUsed.isNullOrEmpty()) {
            context.resources.getString(R.string.application_password_never_used)
        } else {
            lastUsed
        }
    }

    private suspend fun getApplicationPasswordsList(site: SiteModel): List<ApplicationPasswordWithViewContext> {
        val wpApiClient = wpApiClientProvider.getWpApiClient(site)

        val currentUserId = getCurrentUserId(wpApiClient)
        return if (currentUserId == null) {
            emptyList()
        } else {
            getAllApplicationPasswords(currentUserId, wpApiClient)
        }
    }

    private suspend fun getCurrentUserId(wpApiClient: WpApiClient):Long? {
        val userIdResponse = wpApiClient.request { requestBuilder ->
            requestBuilder.users().retrieveMeWithViewContext()
        }
        return when (userIdResponse) {
            is WpRequestResult.Success -> {
                userIdResponse.response.data.id
            }

            else -> {
                val error = "Error getting current user Id"
                appLogWrapper.e(AppLog.T.API, error)
                onError(error)
                null
            }
        }
    }

    private suspend fun getAllApplicationPasswords(userId: Long, wpApiClient: WpApiClient): List<ApplicationPasswordWithViewContext> {
        val currentApplicationPasswordResponse = wpApiClient.request { requestBuilder ->
            requestBuilder.applicationPasswords().listWithViewContext(userId)
        }

        return when (currentApplicationPasswordResponse) {
            is WpRequestResult.Success -> {
                currentApplicationPasswordResponse.response.data
            }

            else -> {
                val error = "Error getting current user Id"
                appLogWrapper.e(AppLog.T.API, error)
                onError(error)
                emptyList()
            }
        }
    }

    companion object {
        private const val SORT_BY_NAME_ID = 1L
        private const val SORT_BY_CREATED_ID = 2L
        private const val SORT_BY_LAST_USED_ID = 3L
    }
}
