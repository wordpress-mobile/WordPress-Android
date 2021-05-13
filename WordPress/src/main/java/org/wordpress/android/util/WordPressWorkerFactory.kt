package org.wordpress.android.util

import androidx.work.DelegatingWorkerFactory
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.push.local.LocalPushHandlerFactory
import org.wordpress.android.push.local.LocalPushScheduleWorker
import org.wordpress.android.ui.uploads.UploadStarter
import javax.inject.Inject

class WordPressWorkerFactory
@Inject constructor(
    uploadStarter: UploadStarter,
    siteStore: SiteStore,
    localPushHandlerFactory: LocalPushHandlerFactory
) : DelegatingWorkerFactory() {
    init {
        addFactory(UploadWorker.Factory(uploadStarter, siteStore))
        addFactory(LocalPushScheduleWorker.Factory(localPushHandlerFactory))
    }
}

