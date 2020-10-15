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
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
class XPostSqlUtilsTest {
    private lateinit var xPostSqlUtils: XPostsSqlUtils
    private val xPostSite = XPostSiteModel().apply { blogId = 10 }

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
    }

    @Test
    fun `test inserting and retrieving an xpost`() {
        val site = SiteModel().apply { id = 100 }
        WellSql.insert(site).execute()

        xPostSqlUtils.insertOrUpdateXPost(listOf(xPostSite), site)

        assertEquals(listOf(xPostSite), xPostSqlUtils.selectXPostsForSite(site))
    }

    @Test
    fun `test insert xpost for different site`() {
        val siteWithXpost = SiteModel().apply { id = 100 }
        WellSql.insert(siteWithXpost).execute()

        val otherSite = SiteModel().apply { id = 101 }
        WellSql.insert(otherSite).execute()

        xPostSqlUtils.insertOrUpdateXPost(listOf(xPostSite), siteWithXpost)

        assertTrue(xPostSqlUtils.selectXPostsForSite(otherSite).isEmpty())
    }
}
