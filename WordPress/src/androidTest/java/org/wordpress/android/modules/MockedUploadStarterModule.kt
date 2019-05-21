package org.wordpress.android.modules

import dagger.Module
import dagger.Provides
import org.mockito.Mockito.mock
import org.wordpress.android.ui.uploads.LocalDraftUploadStarter

@Module
class MockedUploadStarterModule {
    @Provides
    fun provideUploadStarter(): LocalDraftUploadStarter {
        return mock(LocalDraftUploadStarter::class.java)
    }
}
