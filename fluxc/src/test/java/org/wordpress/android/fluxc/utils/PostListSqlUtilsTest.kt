package org.wordpress.android.fluxc.utils

import com.yarolegovich.wellsql.WellSql
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.wordpress.android.fluxc.model.ListModel
import org.wordpress.android.fluxc.model.ListModel.ListType
import org.wordpress.android.fluxc.model.PostListModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.persistence.ListSqlUtils
import org.wordpress.android.fluxc.persistence.PostListSqlUtils
import org.wordpress.android.fluxc.persistence.WellSqlConfig
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@RunWith(RobolectricTestRunner::class)
class PostListSqlUtilsTest {
    private lateinit var listSqlUtils: ListSqlUtils
    private lateinit var postListSqlUtils: PostListSqlUtils

    @Before
    fun setUp() {
        val appContext = RuntimeEnvironment.application.applicationContext
        val config = WellSqlConfig(appContext)
        WellSql.init(config)
        config.reset()

        listSqlUtils = ListSqlUtils()
        postListSqlUtils = PostListSqlUtils()
    }

    @Test
    fun testInsertOrUpdatePostList() {
        /**
         * 1. Since a [PostListModel] requires a [ListModel] in the DB due to the foreign key restriction, a test list
         * will be inserted in the DB.
         * 2. A list of test [PostListModel]s will be generated and inserted in the DB
         */
        val postCount = 20
        val testList = insertTestList()
        val postList = generatePostList(testList, postCount)
        postListSqlUtils.insertPostList(postList)

        /**
         * Verify that the [PostListModel] instances are inserted correctly
         */
        assertEquals(postListSqlUtils.getPostList(testList.id)?.size, postCount)
    }

    /**
     * Creates and inserts a [ListModel] for a random test site. It also verifies that the list is inserted correctly.
     */
    private fun insertTestList(): ListModel {
        val testSite = SiteModel()
        testSite.id = 123
        val listType = ListType.POSTS_ALL
        listSqlUtils.insertOrUpdateList(testSite, listType)
        val list = listSqlUtils.getList(testSite, listType)
        assertNotNull(list)
        return list!!
    }

    /**
     * Helper function that creates a list of [PostListModel] to be used in tests.
     * The [PostListModel.date] will be the same date for all [PostListModel]s.
     */
    private fun generatePostList(listModel: ListModel, count: Int): List<PostListModel> {
        val postList = ArrayList<PostListModel>()
        val testDate = "1955-11-05T14:15:00Z"
        (1..count).forEach { index ->
            val postListModel = PostListModel()
            postListModel.listId = listModel.id
            postListModel.postId = index
            postListModel.date = testDate
            postList.add(postListModel)
        }
        return postList
    }
}
