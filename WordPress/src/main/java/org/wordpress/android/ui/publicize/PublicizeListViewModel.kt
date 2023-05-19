package org.wordpress.android.ui.publicize

import kotlinx.coroutines.CoroutineDispatcher
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.ui.publicize.services.PublicizeUpdateServicesV2
import org.wordpress.android.viewmodel.ScopedViewModel
import javax.inject.Inject
import javax.inject.Named

class PublicizeListViewModel @Inject constructor(
    private val publicizeUpdateServicesV2: PublicizeUpdateServicesV2,
    @Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher,
) : ScopedViewModel(bgDispatcher) {

    fun onSiteAvailable(siteModel: SiteModel) {
        launch {
            publicizeUpdateServicesV2.execute(siteModel)
        }
    }
}
