package org.wordpress.android

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import com.yarolegovich.wellsql.WellTableManager
import org.wordpress.android.fluxc.persistence.WellSqlConfig
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T
import org.wordpress.android.util.ToastUtils
import org.wordpress.android.util.ToastUtils.Duration

class WPWellSqlConfig(context: Context?) : WellSqlConfig(context) {
    /**
     * Detect when the database is downgraded, and if this is a debug user recreate all the tables and show
     * a toast alerting to the downgrade. The sole purpose of this is to avoid the hassle of devs switching
     * branches and having to clear storage and login again due to a version downgrade.
     */
    override fun onDowngrade(db: SQLiteDatabase?, helper: WellTableManager?, oldVersion: Int, newVersion: Int) {
        if (BuildConfig.DEBUG) {
            // note: don't call super() here because it throws an exception
            AppLog.w(T.DB, "Resetting database due to downgrade from version $oldVersion to $newVersion")
            ToastUtils.showToast(
                    context,
                    "Database downgraded, recreating tables and loading sites",
                    Duration.LONG
            )
            reset(helper)
        } else {
            super.onDowngrade(db, helper, oldVersion, newVersion)
        }
    }

    // TODO: remove this
    override fun getDbVersion(): Int {
        return 9991
    }
}
