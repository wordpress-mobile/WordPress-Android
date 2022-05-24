package org.wordpress.android.ui.mysite

import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.page.PageModel
import org.wordpress.android.fluxc.store.PageStore
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HomePageDataLoader @Inject constructor(private val pageStore: PageStore) {
    suspend fun loadHomepage(site: SiteModel): PageModel? {
        pageStore.requestPagesFromServer(site, false)
        return pageStore.getPageByRemoteId(site.pageOnFront, site)
    }
}
