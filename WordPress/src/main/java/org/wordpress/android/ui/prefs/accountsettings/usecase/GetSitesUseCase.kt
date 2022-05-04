package org.wordpress.android.ui.prefs.accountsettings.usecase

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.modules.IO_THREAD
import javax.inject.Inject
import javax.inject.Named

class GetSitesUseCase @Inject constructor(
    @Named(IO_THREAD) private val ioDispatcher: CoroutineDispatcher,
    private val siteStore: SiteStore
) : GetSitesInteractor {
    override suspend fun getSitesAccessedViaWPComRest(): List<SiteModel> = withContext(ioDispatcher) {
        siteStore.sitesAccessedViaWPComRest
    }
}

interface GetSitesInteractor{
    suspend fun getSitesAccessedViaWPComRest(): List<SiteModel>
}
