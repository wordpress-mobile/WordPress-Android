package org.wordpress.android.fluxc.persistence

import com.wellsql.generated.CommentModelTable
import com.yarolegovich.wellsql.SelectQuery
import com.yarolegovich.wellsql.WellSql
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.activity.ActivityModel
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ActivitySqlUtils @Inject constructor() {
    fun getActivitiesForSite(site: SiteModel?, @SelectQuery.Order order: Int): List<ActivityModel> {
        return if (site == null) {
            listOf()
        } else {
            activitiesQueryForSite(site).orderBy(CommentModelTable.DATE_PUBLISHED, order).asModel.map { it.build() }
        }
    }

    private fun activitiesQueryForSite(site: SiteModel): SelectQuery<ActivityModel.Builder> {
        val selectQueryBuilder = WellSql.select(ActivityModel.Builder::class.java)
                .where().beginGroup()
                .equals(CommentModelTable.LOCAL_SITE_ID, site.id)
        return selectQueryBuilder.endGroup().endWhere()
    }
}
