package org.wordpress.android

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.graphics.Color
import android.graphics.PorterDuff
import android.view.Gravity
import android.widget.TextView
import android.widget.Toast
import android.widget.Toast.LENGTH_LONG
import com.yarolegovich.wellsql.WellTableManager
import org.wordpress.android.fluxc.persistence.WellSqlConfig
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T

class WPWellSqlConfig(context: Context) : WellSqlConfig(context) {
    /**
     * Detect when the database is downgraded in debug builds, and if the build flag is set recreate all the tables
     * and show a toast alerting to the downgrade. The sole purpose of this is to avoid the hassle of devs switching
     * branches and having to clear storage and login again due to a version downgrade.
     */
    override fun onDowngrade(db: SQLiteDatabase?, helper: WellTableManager?, oldVersion: Int, newVersion: Int) {
        if (BuildConfig.RESET_DB_ON_DOWNGRADE.toBoolean()) {
            // note: don't call super() here because it throws an exception
            AppLog.w(T.DB, "Resetting database due to downgrade from version $oldVersion to $newVersion")

            val toast = Toast.makeText(context, "Database downgraded, recreating tables and loading sites", LENGTH_LONG)
            toast.view?.let { view ->
                view.background.setColorFilter(Color.RED, PorterDuff.Mode.SRC_IN)
                view.findViewById<TextView>(android.R.id.message)?.setTextColor(Color.WHITE)
            }
            toast.setGravity(Gravity.CENTER, 0, 0)
            toast.show()

            helper?.let { reset(it) }
        } else {
            super.onDowngrade(db, helper, oldVersion, newVersion)
        }
    }

    /**
     * Increase the cursor window size to 5MB for devices running API 28 and above. This should reduce the
     * number of SQLiteBlobTooBigExceptions. Note that this is only called on API 28 and
     * above since earlier versions don't allow adjusting the cursor window size.
     */
    override fun getCursorWindowSize() = (1024L * 1024L * 5L)
}
