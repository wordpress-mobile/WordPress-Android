package org.wordpress.android

import android.content.Context
import com.yarolegovich.wellsql.WellSql
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WellSqlInitializer @Inject constructor(@ApplicationContext private val context: Context) {
    fun init() {
        val wellSqlConfig = WPWellSqlConfig(context)
        WellSql.init(wellSqlConfig)
    }
}
