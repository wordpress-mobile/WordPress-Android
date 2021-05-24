package org.wordpress.android.ui.prefs

import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.viewmodel.ContextProvider
import javax.inject.Inject

class SiteSettingsInterfaceWrapper(private val siteSettingsInterface: SiteSettingsInterface) {
    val localSiteId: Int
        get() = siteSettingsInterface.localSiteId

    var title: String
        get() = siteSettingsInterface.title
        set(value) {
            siteSettingsInterface.title = value
        }

    fun setSiteIconMediaId(mediaId: Int) = siteSettingsInterface.setSiteIconMediaId(mediaId)

    fun saveSettings() = siteSettingsInterface.saveSettings()
    fun clear() {
        siteSettingsInterface.clear()
    }

    fun init(fetchRemote: Boolean) {
        siteSettingsInterface.init(fetchRemote)
    }

    class Factory
    @Inject constructor(private val contextProvider: ContextProvider) {
        fun build(
            site: SiteModel,
            onSaveError: ((error: Exception?) -> Unit)? = null,
            onFetchError: ((error: Exception?) -> Unit)? = null,
            onSettingsUpdated: (() -> Unit)? = null,
            onSettingsSaved: (() -> Unit)? = null,
            onCredentialsValidated: ((error: Exception?) -> Unit)? = null
        ): SiteSettingsInterfaceWrapper? {
            val siteSettingsInterface = SiteSettingsInterface.getInterface(
                    contextProvider.getContext(),
                    site,
                    object : SiteSettingsInterface.SiteSettingsListener {
                        override fun onSaveError(error: Exception?) {
                            onSaveError?.invoke(error)
                        }

                        override fun onFetchError(error: Exception?) {
                            onFetchError?.invoke(error)
                        }

                        override fun onSettingsUpdated() {
                            onSettingsUpdated?.invoke()
                        }

                        override fun onSettingsSaved() {
                            onSettingsSaved?.invoke()
                        }

                        override fun onCredentialsValidated(error: Exception?) {
                            onCredentialsValidated?.invoke(error)
                        }
                    })
            return siteSettingsInterface?.let {
                SiteSettingsInterfaceWrapper(
                        it
                )
            }
        }
    }
}
