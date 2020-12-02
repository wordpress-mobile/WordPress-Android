package org.wordpress.android.fluxc.persistance

import com.yarolegovich.wellsql.WellSql
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.wordpress.android.fluxc.SingleStoreWellSqlConfigForTests
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.XPostModel
import org.wordpress.android.fluxc.model.XPostSiteModel
import org.wordpress.android.fluxc.persistence.XPostsSqlUtils
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
class XPostSqlUtilsTest {
    private lateinit var xPostSqlUtils: XPostsSqlUtils
    private val xPostSiteModel = XPostSiteModel().apply { blogId = 10 }
    private val site = SiteModel().apply { id = 100 }

    @Before
    fun setUp() {
        val appContext = RuntimeEnvironment.application.applicationContext
        val config = SingleStoreWellSqlConfigForTests(
                appContext,
                listOf(
                        SiteModel::class.java,
                        XPostSiteModel::class.java,
                        XPostModel::class.java))
        WellSql.init(config)
        config.reset()

        xPostSqlUtils = XPostsSqlUtils()
        WellSql.insert(site).execute()
    }

    @Test
    fun `sets xposts for a site`() {
        xPostSqlUtils.setXPostsForSite(listOf(xPostSiteModel), site)

        assertEquals(listOf(xPostSiteModel), xPostSqlUtils.selectXPostsForSite(site))
    }

    @Test
    fun `setting xposts for a site deletes previous xposts`() {
        xPostSqlUtils.setXPostsForSite(listOf(xPostSiteModel), site)
        xPostSqlUtils.setXPostsForSite(emptyList(), site)
        assertTrue(xPostSqlUtils.selectXPostsForSite(site)!!.isEmpty())
    }

    @Test
    fun `selectXPostsForSite returns null if no xposts ever set`() {
        assertNull(xPostSqlUtils.selectXPostsForSite(site))
    }

    @Test
    fun `selectXPostsForSite returns empty list if empty list of xposts previously set`() {
        xPostSqlUtils.setXPostsForSite(emptyList(), site)
        assertTrue(xPostSqlUtils.selectXPostsForSite(site)!!.isEmpty())
    }

    @Test
    fun `inserting and retrieving an xpost`() {
        xPostSqlUtils.setXPostsForSite(listOf(xPostSiteModel), site)
        assertEquals(listOf(xPostSiteModel), xPostSqlUtils.selectXPostsForSite(site))
    }

    @Test
    fun `inserting an xpost target does not affect that xpost target for other sites`() {
        val siteWithNoXposts = SiteModel().apply { site.id + 1 }
        WellSql.insert(siteWithNoXposts).execute()

        xPostSqlUtils.setXPostsForSite(listOf(xPostSiteModel), site)

        assertNull(xPostSqlUtils.selectXPostsForSite(siteWithNoXposts))
    }

    @Test
    fun `can insert same xpost for two sites`() {
        val otherSite = SiteModel().apply { site.id + 1 }
        WellSql.insert(otherSite).execute()

        xPostSqlUtils.setXPostsForSite(listOf(xPostSiteModel), site)
        xPostSqlUtils.setXPostsForSite(listOf(xPostSiteModel), otherSite)
        xPostSqlUtils.setXPostsForSite(emptyList(), otherSite)

        assertTrue(xPostSqlUtils.selectXPostsForSite(site)!!.isNotEmpty())
    }
}
