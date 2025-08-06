package org.wordpress.android.ui.accounts.applicationpassword

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import org.wordpress.android.R
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
import org.wordpress.android.util.NetworkUtilsWrapper
import uniffi.wp_api.ApplicationPasswordAppId
import uniffi.wp_api.ApplicationPasswordUuid
import uniffi.wp_api.ApplicationPasswordWithViewContext
import uniffi.wp_api.IpAddress
import uniffi.wp_api.WpApiParamOrder
import javax.inject.Inject
import javax.inject.Named

@HiltViewModel
class ApplicationPasswordsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    @Named(UI_THREAD) mainDispatcher: CoroutineDispatcher,
    appLogWrapper: AppLogWrapper,
    sharedPrefs: SharedPreferences,
    networkUtilsWrapper: NetworkUtilsWrapper,
    selectedSiteRepository: SelectedSiteRepository,
    accountStore: AccountStore,
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
        DataViewDropdownItem(id = 1L, titleRes = R.string.application_password_name_sort),
        DataViewDropdownItem(id = 2L, titleRes = R.string.application_password_created_sort),
        DataViewDropdownItem(id = 3L, titleRes = R.string.application_password_last_used_sort)
    )

    override suspend fun performNetworkRequest(
        page: Int,
        searchQuery: String,
        filter: DataViewDropdownItem?,
        sortOrder: WpApiParamOrder,
        sortBy: DataViewDropdownItem?,
    ): List<DataViewItem> = withContext(ioDispatcher) {
        val allApplicationPasswords = createDummyApplicationPasswords()

        // Filter by search query
        val filteredPasswords = if (searchQuery.isBlank()) {
            allApplicationPasswords
        } else {
            allApplicationPasswords.filter { password ->
                password.name.contains(searchQuery, ignoreCase = true) ||
                password.appId.toString().contains(searchQuery, ignoreCase = true)
            }
        }

        // Sort the results
        val sortedPasswords = when (sortBy?.id) {
            1L -> { // Sort by name
                if (sortOrder == WpApiParamOrder.ASC) {
                    filteredPasswords.sortedBy { it.name }
                } else {
                    filteredPasswords.sortedByDescending { it.name }
                }
            }
            2L -> { // Sort by created date
                if (sortOrder == WpApiParamOrder.ASC) {
                    filteredPasswords.sortedBy { it.created }
                } else {
                    filteredPasswords.sortedByDescending { it.created }
                }
            }
            3L -> { // Sort by last used
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

    private fun createDummyApplicationPasswords(): List<ApplicationPasswordWithViewContext> {
        return listOf(
            ApplicationPasswordWithViewContext(
                uuid = ApplicationPasswordUuid("uuid-1"),
                name = "WordPress Mobile App",
                appId = ApplicationPasswordAppId("wordpress-mobile"),
                created = "2024-01-01T00:00:00Z",
                lastUsed = "Used yesterday",
                lastIp = IpAddress("192.168.1.1")
            ),
            ApplicationPasswordWithViewContext(
                uuid = ApplicationPasswordUuid("uuid-2"),
                name = "Jetpack Mobile App",
                appId = ApplicationPasswordAppId("jetpack-mobile"),
                created = "2024-01-02T00:00:00Z",
                lastUsed = "Used 3 days ago",
                lastIp = IpAddress("192.168.1.2")
            ),
            ApplicationPasswordWithViewContext(
                uuid = ApplicationPasswordUuid("uuid-3"),
                name = "Desktop Publisher",
                appId = ApplicationPasswordAppId("desktop-app"),
                created = "2024-01-03T00:00:00Z",
                lastUsed = "Used 2 weeks ago",
                lastIp = IpAddress("192.168.1.3")
            ),
            ApplicationPasswordWithViewContext(
                uuid = ApplicationPasswordUuid("uuid-4"),
                name = "Third Party Integration",
                appId = ApplicationPasswordAppId("third-party"),
                created = "2024-01-04T00:00:00Z",
                lastUsed = null,
                lastIp = null
            ),
            ApplicationPasswordWithViewContext(
                uuid = ApplicationPasswordUuid("uuid-5"),
                name = "Legacy API Client",
                appId = ApplicationPasswordAppId("legacy-client"),
                created = "2024-01-05T00:00:00Z",
                lastUsed = "Used 6 months ago",
                lastIp = IpAddress("192.168.1.4")
            ),
            ApplicationPasswordWithViewContext(
                uuid = ApplicationPasswordUuid("uuid-6"),
                name = "Development Testing Tool",
                appId = ApplicationPasswordAppId("dev-tool"),
                created = "2024-01-06T00:00:00Z",
                lastUsed = "Used today",
                lastIp = IpAddress("192.168.1.5")
            )
        )
    }
}
