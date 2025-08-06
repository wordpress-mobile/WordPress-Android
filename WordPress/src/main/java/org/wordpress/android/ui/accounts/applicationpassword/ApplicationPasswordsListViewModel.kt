package org.wordpress.android.ui.accounts.applicationpassword

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import uniffi.wp_api.ApplicationPasswordWithViewContext
import uniffi.wp_api.ApplicationPasswordUuid
import uniffi.wp_api.ApplicationPasswordAppId
import uniffi.wp_api.IpAddress
import javax.inject.Inject

@HiltViewModel
class ApplicationPasswordsListViewModel @Inject constructor() : ViewModel() {
    private val _applicationPasswords = MutableLiveData<List<ApplicationPasswordWithViewContext>>()
    val applicationPasswords: LiveData<List<ApplicationPasswordWithViewContext>> = _applicationPasswords

    fun loadApplicationPasswords() {
        loadDummyApplicationPasswords()
    }
    fun loadDummyApplicationPasswords() {
        val dummyPasswords = listOf(
            ApplicationPasswordWithViewContext(
                uuid = ApplicationPasswordUuid("uuid-1"),
                name = "WordPress Mobile App",
                appId = ApplicationPasswordAppId("wordpress-mobile"),
                created = "2024-01-01T00:00:00Z",
                lastUsed = "Used yesterday",
                lastIp = IpAddress("IP")
            ),
            ApplicationPasswordWithViewContext(
                uuid = ApplicationPasswordUuid("uuid-2"),
                name = "Jetpack Mobile App",
                appId = ApplicationPasswordAppId("jetpack-mobile"),
                created = "2024-01-02T00:00:00Z",
                lastUsed = "Used 3 days ago",
                lastIp = IpAddress("IP")
            ),
            ApplicationPasswordWithViewContext(
                uuid = ApplicationPasswordUuid("uuid-3"),
                name = "Desktop Publisher",
                appId = ApplicationPasswordAppId("desktop-app"),
                created = "2024-01-03T00:00:00Z",
                lastUsed = "Used 2 weeks ago",
                lastIp = IpAddress("IP")
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
                lastIp = IpAddress("IP")
            )
        )
        _applicationPasswords.value = dummyPasswords
    }
}
