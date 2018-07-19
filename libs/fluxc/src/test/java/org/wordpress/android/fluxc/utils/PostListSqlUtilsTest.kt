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
        val testSite = SiteModel()
        testSite.id = 123
        val postCount = 20
        val listType = ListType.POSTS_ALL

        /**
         * 1. Since a [PostListModel] requires a [ListModel] in the DB due to the foreign key restriction, a test list
         * will be inserted in the DB.
         * 2. A list of test [PostListModel]s will be generated and inserted in the DB
         * 3. Verify that the [PostListModel] instances are inserted correctly
         */
        val testList = insertTestList(testSite, listType)
        val postList = generatePostList(testList, postCount)
        postListSqlUtils.insertPostList(postList)
        assertEquals(postListSqlUtils.getPostList(testList.id)?.size, postCount)
    }

    @Test
    fun testDeletePostList() {
        val testSite = SiteModel()
        testSite.id = 123
        val listType = ListType.POSTS_ALL
        val postCount = 20

        /**
         * 1. Since a [PostListModel] requires a [ListModel] in the DB due to the foreign key restriction, a test list
         * will be inserted in the DB.
         * 2. A list of test [PostListModel]s will be generated and inserted in the DB
         * 3. Verify that the [PostListModel] instances are inserted correctly
         */
        val testList = insertTestList(testSite, listType)
        val postList = generatePostList(testList, postCount)
        postListSqlUtils.insertPostList(postList)
        assertEquals(postListSqlUtils.getPostList(testList.id)?.size, postCount)

        /**
         * 1. Delete the inserted list
         * 2. Verify that deleting the list also deletes the inserted [PostListModel]s due to foreign key restriction
         */
        listSqlUtils.deleteList(testSite, listType)
        assertEquals(postListSqlUtils.getPostList(testList.id)?.size, 0)
    }

    /**
     * Creates and inserts a [ListModel] for a random test site. It also verifies that the list is inserted correctly.
     */
    private fun insertTestList(siteModel: SiteModel, listType: ListType): ListModel {
        listSqlUtils.insertOrUpdateList(siteModel, listType)
        val list = listSqlUtils.getList(siteModel, listType)
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
