package org.wordpress.android.ui.jetpackconnection

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.fluxc.utils.AppLogWrapper
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.VersionUtils.checkMinimalVersion
import org.wordpress.android.viewmodel.ScopedViewModel
import uniffi.wp_api.JetpackConnectionClient
import uniffi.wp_api.PluginCreateParams
import uniffi.wp_api.PluginStatus
import uniffi.wp_api.PluginWpOrgDirectorySlug
import uniffi.wp_api.WpAuthentication
import javax.inject.Inject
import javax.inject.Named

class JetpackConnectionViewModel @Inject constructor(
    @Named(UI_THREAD) mainDispatcher: CoroutineDispatcher,
    private val jetpackConnectionClient: JetpackConnectionClient,
    private val selectedSiteRepository: SelectedSiteRepository,
    private val accountStore: AccountStore,
    private val appLogWrapper: AppLogWrapper,
) : ScopedViewModel(mainDispatcher) {
    private val _currentStep = MutableStateFlow<ConnectionStep?>(null)
    val currentStep = _currentStep

    private val _stepStatuses = MutableStateFlow(
        mapOf<ConnectionStep, ConnectionStatus>(
            ConnectionStep.LoginWpCom to ConnectionStatus.NotStarted,
            ConnectionStep.InstallJetpack to ConnectionStatus.NotStarted,
            ConnectionStep.ConnectSite to ConnectionStatus.NotStarted,
            ConnectionStep.ConnectWpCom to ConnectionStatus.NotStarted,
            ConnectionStep.Finalize to ConnectionStatus.NotStarted
        )
    )
    val stepStatuses = _stepStatuses

    private fun updateStepStatus(step: ConnectionStep, status: ConnectionStatus) {
        _stepStatuses.value = _stepStatuses.value.toMutableMap().apply {
            this[step] = status
        }
    }

    fun setCurrentStep(step: ConnectionStep) {
        appLogWrapper.d(AppLog.T.API, "$TAG: Setting current step to $step")
        _currentStep.value = step
        updateStepStatus(step, ConnectionStatus.InProgress)
    }

    fun onCloseClick() {
        // TODO
    }

    private suspend fun networkRequest() {
        when (currentStep.value) {
            ConnectionStep.LoginWpCom -> {
                // TODO
            }

            ConnectionStep.ConnectSite -> {
                jetpackConnectionClient.connectSite(
                    from = getSiteId().toString()
                )
            }

            ConnectionStep.InstallJetpack -> {
                installJetpackPlugin()
            }

            ConnectionStep.ConnectWpCom -> {
                jetpackConnectionClient.connectUser(
                    wpComAuthentication = WpAuthentication.Bearer(token = accountStore.accessToken!!),
                    from = getSiteId().toString()
                )
            }

            ConnectionStep.Finalize -> {
                // TODO
            }

            null -> {
                // noop
            }
        }
    }

    // TODO
    private fun installJetpackPlugin() {
        val params = PluginCreateParams(
            slug = PluginWpOrgDirectorySlug("jetpack"),
            status = PluginStatus.ACTIVE,
        )
    }

    private fun getSiteId() = getSite().siteId

    private fun getSite() = selectedSiteRepository.getSelectedSite()!!

    sealed class ConnectionStep {
        data object LoginWpCom : ConnectionStep()
        data object InstallJetpack : ConnectionStep()
        data object ConnectSite : ConnectionStep()
        data object ConnectWpCom : ConnectionStep()
        data object Finalize : ConnectionStep()
    }

    sealed class ConnectionStatus {
        data object NotStarted : ConnectionStatus()
        data object InProgress : ConnectionStatus()
        data object Completed : ConnectionStatus()
    }

    companion object {
        /**
         * Requirements:
         * - Self-hosted site, and
         * - The site is authenticated with application password, and
         * - the site isn't already connected to Jetpack, and
         * - Jetpack is not installed or the installed jetpack version is 14.2 or above
         */
        fun canInitiateJetpackConnection(site: SiteModel): Boolean {
            if (site.isSelfHostedAdmin && site.isApplicationPasswordsSupported) {
                return if (site.applicationPasswordsAuthorizeUrl.isNullOrEmpty()) {
                    false
                } else if (site.wpApiRestUrl.isNullOrEmpty()) {
                    false
                } else if (site.isJetpackConnected) {
                    false
                } else if (site.isJetpackInstalled) {
                    checkMinimalVersion(site.jetpackVersion, LIMIT_VERSION)
                } else {
                    true
                }
            }
            return false
        }

        private const val TAG = "JetpackConnectionViewModel"
        private const val LIMIT_VERSION = "14.2"
    }
}
