package org.wordpress.android

import android.content.Context
import com.yarolegovich.wellsql.WellSql
import dagger.hilt.android.qualifiers.ApplicationContext
import org.wordpress.android.fluxc.persistence.WellSqlConfig
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WellSqlInitializer @Inject constructor(@ApplicationContext private val context: Context) {
    init {
        val wellSqlConfig = WPWellSqlConfig(context)
        WellSql.init(wellSqlConfig)
    }
}
