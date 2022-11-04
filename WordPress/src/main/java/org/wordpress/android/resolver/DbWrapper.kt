package org.wordpress.android.resolver

import com.yarolegovich.wellsql.WellSql
import dagger.Reusable
import javax.inject.Inject

@Reusable
class DbWrapper @Inject constructor() {
    fun giveMeWritableDb() = WellSql.giveMeWritableDb()
}
