package org.wordpress.android.fluxc.persistence

import com.yarolegovich.wellsql.WellSql
import org.wordpress.android.fluxc.model.vertical.VerticalSegmentModel
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VerticalSqlUtils @Inject constructor() {
    // TODO: Add documentation
    fun insertSegments(segmentList: List<VerticalSegmentModel>) {
        WellSql.insert(segmentList).asSingleTransaction(true).execute()
    }

    // TODO: Add documentation
    // TODO: Do we need to order them in any way?
    fun getSegments(): List<VerticalSegmentModel> {
        return WellSql.select(VerticalSegmentModel::class.java).asModel
    }
}
