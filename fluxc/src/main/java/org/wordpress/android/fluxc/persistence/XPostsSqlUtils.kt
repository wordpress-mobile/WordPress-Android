package org.wordpress.android.fluxc.persistence

import com.wellsql.generated.XPostSitesTable
import com.wellsql.generated.XPostsTable
import com.yarolegovich.wellsql.WellSql
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.XPostModel
import org.wordpress.android.fluxc.model.XPostSiteModel
import javax.inject.Inject

class XPostsSqlUtils @Inject constructor() {
    fun persistNoXpostsForSite(site: SiteModel) {
        setXPostsForSite(emptyList(), site)
    }

    /**
     * If an empty list is being set, then insert a single [XPostModel.noXPostModel] entry in
     * the XPosts table (that should be the only entry for that site, see [hasNoXPosts]). This
     * allows us to distinguish between a site that has no XPosts (which will have the single
     * [hasNoXPosts] row), and a site for which we have not persisted the XPosts yet (which will
     * have no matching rows).
     */
    fun setXPostsForSite(xPostSites: List<XPostSiteModel>, site: SiteModel) {
        deleteXpostsForSite(site)

        if (xPostSites.isEmpty()) {
            WellSql.insert(XPostModel.noXPostModel(site))
                    .execute()
        } else {
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
    }

    /**
     * This function has three possible return paths:
     *
     * 1. Because we persist a specific entry in the XPost table to represent no XPosts (see
     * [setXPostsForSite]), an empty xpost list actually indicates that we do
     * not know whether the site has any XPosts, so we return null.
     *
     * 2. Only if the specific "no xposts" entry exists for the given [site] do we know the site does not
     * have any xposts and we should return an empty list.
     *
     * 3. Otherwise, we return the persisted list of XPosts.
     */
    fun selectXPostsForSite(site: SiteModel): List<XPostSiteModel>? {
        val xPosts = WellSql.select(XPostModel::class.java)
                .where()
                .equals(XPostsTable.SOURCE_SITE_ID, site.id)
                .endWhere()
                .asModel

        return when {
            xPosts.isEmpty() -> null
            hasNoXPosts(xPosts) -> emptyList()
            else -> {
                val xPostSiteIds = xPosts.map { it.targetSiteId }
                WellSql.select(XPostSiteModel::class.java)
                        .where()
                        .isIn(XPostSitesTable.BLOG_ID, xPostSiteIds)
                        .endWhere()
                        .asModel
            }
        }
    }

    /**
     * If the only XPost for a site is a "no xposts" entry, we know there are no available xposts.
     */
    private fun hasNoXPosts(xPostsForSite: List<XPostModel>) =
            xPostsForSite.size == 1 && XPostModel.isNoXPostsEntry(xPostsForSite.first())

    /**
     * Only deleting the XPostModel entries.
     */
    private fun deleteXpostsForSite(site: SiteModel) {
        WellSql.delete(XPostModel::class.java)
                .where()
                .equals(XPostsTable.SOURCE_SITE_ID, site.id)
                .endWhere()
                .execute()
    }
}
