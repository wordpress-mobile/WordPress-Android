package org.wordpress.android.ui.subscribers

import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.wordpress.android.R
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.fluxc.utils.AppLogWrapper
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.modules.IO_THREAD
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.ToastUtilsWrapper
import org.wordpress.android.viewmodel.ScopedViewModel
import rs.wordpress.api.kotlin.WpComApiClient
import rs.wordpress.api.kotlin.WpRequestResult
import uniffi.wp_api.AddSubscribersParams
import uniffi.wp_api.WpAuthentication
import uniffi.wp_api.WpAuthenticationProvider
import javax.inject.Inject
import javax.inject.Named

@HiltViewModel
class AddSubscribersViewModel @Inject constructor(
    @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher,
    @Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher,
    private val appLogWrapper: AppLogWrapper,
    private val toastUtilsWrapper: ToastUtilsWrapper,
) : ScopedViewModel(bgDispatcher) {
    @Inject
    @Named(IO_THREAD)
    lateinit var ioDispatcher: CoroutineDispatcher

    @Inject
    lateinit var selectedSiteRepository: SelectedSiteRepository

    @Inject
    lateinit var accountStore: AccountStore

    private val _showProgress = MutableStateFlow(false)
    val showProgress = _showProgress.asStateFlow()

    private val wpComApiClient: WpComApiClient by lazy {
        WpComApiClient(
            WpAuthenticationProvider.staticWithAuth(
                WpAuthentication.Bearer(token = accountStore.accessToken!!)
            ),
            interceptors = emptyList()
        )
    }

    private fun siteId(): Long {
        return selectedSiteRepository.getSelectedSite()?.siteId ?: 0L
    }

    fun onSubmitClick(
        emails: List<String>,
        onSuccess: () -> Unit
    ) {
        launch(bgDispatcher) {
            val result = addSubscribers(emails)
            withContext(mainDispatcher) {
                if (result.isSuccess) {
                    toastUtilsWrapper.showToast(R.string.subscribers_add_success)
                    onSuccess()
                } else {
                    toastUtilsWrapper.showToast(R.string.subscribers_add_failed)
                }
            }
        }
    }

    private suspend fun addSubscribers(emails: List<String>) = runCatching {
        withContext(ioDispatcher) {
            val params = AddSubscribersParams(
                emails = emails
            )

            _showProgress.value = true
            try {
                val response = wpComApiClient.request { requestBuilder ->
                    requestBuilder.subscribers().addSubscribers(
                        wpComSiteId = siteId().toULong(),
                        params = params
                    )
                }

                when (response) {
                    is WpRequestResult.Success -> {
                        // the backend may return HTTP 200 even when no subscribers were added, so verify there's
                        // a valid uploadId before assuming success
                        if (response.response.data.uploadId == 0.toULong()) {
                            appLogWrapper.d(AppLog.T.MAIN, "No subscribers added")
                            Result.failure(Exception("No subscribers added"))
                        } else {
                            appLogWrapper.d(AppLog.T.MAIN, "Successfully added ${emails.size} subscribers")
                            Result.success(true)
                        }
                    }

                    else -> {
                        val error = (response as? WpRequestResult.WpError)?.errorMessage
                        appLogWrapper.e(AppLog.T.MAIN, "Failed to add subscriber: $response")
                        Result.failure(Exception(error))
                    }
                }
            } finally {
                _showProgress.value = false
            }
        }
    }
}
