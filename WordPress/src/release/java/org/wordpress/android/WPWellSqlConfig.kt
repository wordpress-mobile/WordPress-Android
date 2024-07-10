package org.wordpress.android

import android.content.Context
import org.wordpress.android.fluxc.persistence.WellSqlConfig

class WPWellSqlConfig(context: Context) : WellSqlConfig(context) {
    /**
     * Increase the cursor window size to 20MB for devices running API 28 and above. This should reduce the
     * number of SQLiteBlobTooBigExceptions. Note that this is only called on API 28 and
     * above since earlier versions don't allow adjusting the cursor window size.
     */
    @Suppress("MagicNumber")
    override fun getCursorWindowSize() = (1024L * 1024L * 20L)
}
