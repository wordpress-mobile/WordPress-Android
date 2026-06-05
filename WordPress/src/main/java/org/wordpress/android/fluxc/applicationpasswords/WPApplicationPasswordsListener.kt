package org.wordpress.android.fluxc.applicationpasswords

import com.automattic.android.tracks.crashlogging.CrashLogging
import dagger.Lazy
import org.wordpress.android.fluxc.network.rest.wpapi.applicationpasswords.ApplicationPasswordsListener
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.crashlogging.sendReportWithTag
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WPApplicationPasswordsListener @Inject constructor(
    private val crashLogging: Lazy<CrashLogging>
) : ApplicationPasswordsListener {
    override fun onKeystoreError(error: Throwable) {
        crashLogging.get().sendReportWithTag(error, AppLog.T.MAIN)
    }
}
