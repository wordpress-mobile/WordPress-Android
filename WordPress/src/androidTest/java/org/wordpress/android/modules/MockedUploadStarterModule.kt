package org.wordpress.android.modules

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import dagger.Module
import dagger.Provides
import kotlinx.coroutines.Job
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.ui.uploads.LocalDraftUploadStarter
import javax.inject.Singleton

class FakeLocalDraftUploadStarter : LocalDraftUploadStarter(
        context = mock(),
        postStore = mock(),
        siteStore = mock(),
        bgDispatcher = mock(),
        ioDispatcher = mock(),
        uploadServiceFacade = mock(),
        networkUtilsWrapper = mock(),
        connectionStatus = mock()
) {
    val counter = mutableMapOf<Function<Any>, Int>().withDefault { 0 }

    fun inc(function: Function<Any>) {
        counter[function] = counter.getValue(function) + 1
    }

    override fun queueUploadFromAllSites(): Job {
        inc(::queueUploadFromAllSites)
        return Job()
    }

    override fun queueUploadFromSite(site: SiteModel): Job {
        inc(::queueUploadFromSite)
        return Job()
    }
}

@Module
class MockedUploadStarterModule {
    @Provides
    @Singleton
    fun provideUploadStarter(): LocalDraftUploadStarter {
        return FakeLocalDraftUploadStarter()
    }

    @Provides
    @Singleton
    fun provideSiteStore(): SiteStore {
        return mock {
            on { getSiteByLocalId(any()) } doReturn SiteModel()
        }
    }
}
