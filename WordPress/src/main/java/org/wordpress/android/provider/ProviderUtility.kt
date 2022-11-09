package org.wordpress.android.provider

import com.yarolegovich.wellsql.WellSql
import dagger.Reusable
import org.wordpress.android.fluxc.model.QuickStartStatusModel
import org.wordpress.android.fluxc.model.QuickStartTaskModel
import javax.inject.Inject

@Reusable
class ProviderUtility @Inject constructor() {
    fun getAllQuickStartTask(): List<QuickStartTaskModel> {
        return WellSql.select(QuickStartTaskModel::class.java).asModel
    }

    fun getAllQuickStartStatus(): List<QuickStartStatusModel> {
        return WellSql.select(QuickStartStatusModel::class.java).asModel
    }
}
