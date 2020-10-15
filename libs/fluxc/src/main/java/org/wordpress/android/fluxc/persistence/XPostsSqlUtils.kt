package org.wordpress.android.fluxc.persistence

import com.wellsql.generated.XPostSitesTable
import com.wellsql.generated.XPostsTable
import com.yarolegovich.wellsql.WellSql
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.XPostModel
import org.wordpress.android.fluxc.model.XPostSiteModel
import javax.inject.Inject

class XPostsSqlUtils @Inject constructor() {
    fun insertOrUpdateXPost(xPostSites: List<XPostSiteModel>, site: SiteModel) {
        WellSql.insert(xPostSites)
                .asSingleTransaction(true)
                .execute()

        val xPostModels = xPostSites.map {
            XPostModel().apply {
                sourceSiteId = site.id
                targetSiteId = it.blogId
            }
        }
        WellSql.insert(xPostModels)
                .asSingleTransaction(true)
                .execute()
    }

    fun selectXPostsForSite(site: SiteModel): List<XPostSiteModel> {
        val xPostSiteIds = WellSql.select(XPostModel::class.java)
                .where()
                .equals(XPostsTable.SOURCE_SITE_ID, site.id)
                .endWhere()
                .asModel
                .map {
                    it.targetSiteId
                }

        return if (xPostSiteIds.isEmpty()) {
            emptyList()
        } else {
            WellSql.select(XPostSiteModel::class.java)
                    .where()
                    .isIn(XPostSitesTable.BLOG_ID, xPostSiteIds)
                    .endWhere()
                    .asModel
        }
    }
}
