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
        testSite.id = 123 // value doesn't matter
        val postCount = 20 // value doesn't matter
        val listType = ListType.POSTS_ALL

        /**
         * 1. Since a [PostListModel] requires a [ListModel] in the DB due to the foreign key restriction, a test list
         * will be inserted in the DB.
         * 2. A list of test [PostListModel]s will be generated and inserted in the DB
         * 3. Verify that the [PostListModel] instances are inserted correctly
         */
        val testList = insertTestList(testSite, listType)
        val postList = generatePostList(testList, postCount)
        postListSqlUtils.insertPostList(testList.id, postList)
        assertEquals(postCount, postListSqlUtils.getPostList(testList.id)?.size)
    }

    @Test
    fun testDeletePostList() {
        val testSite = SiteModel()
        testSite.id = 123 // value doesn't matter
        val postCount = 20 // value doesn't matter
        val listType = ListType.POSTS_ALL

        /**
         * 1. Since a [PostListModel] requires a [ListModel] in the DB due to the foreign key restriction, a test list
         * will be inserted in the DB.
         * 2. A list of test [PostListModel]s will be generated and inserted in the DB
         * 3. Verify that the [PostListModel] instances are inserted correctly
         */
        val testList = insertTestList(testSite, listType)
        val postList = generatePostList(testList, postCount)
        postListSqlUtils.insertPostList(testList.id, postList)
        assertEquals(postCount, postListSqlUtils.getPostList(testList.id)?.size)

        /**
         * 1. Delete the inserted list
         * 2. Verify that deleting the list also deletes the inserted [PostListModel]s due to foreign key restriction
         */
        listSqlUtils.deleteList(testSite, listType)
        assertEquals(0, postListSqlUtils.getPostList(testList.id)?.size)
    }

    @Test
    fun testDeletePost() {
        val testSite = SiteModel()
        testSite.id = 123 // value doesn't matter
        val testPostId = 1245 // value doesn't matter

        /**
         * 1. Insert a test list for every list type. There should at least be 2 list types for a good test.
         * 2. Generate a [PostListModel] for every list type with the same id and insert it
         * 3. Verify that the [PostListModel] was inserted correctly
         */
        val testLists = ListType.values().map { insertTestList(testSite, it) }
        testLists.forEach { list ->
            postListSqlUtils.insertPostList(list.id, arrayListOf(generatePostListModel(list.id, testPostId)))
            assertEquals(1, postListSqlUtils.getPostList(list.id)?.size)
        }

        /**
         * 1. Delete [PostListModel]s for which [PostListModel.postId] == `testPostId`
         * 2. Verify that [PostListModel]s from every list is deleted
         */
        postListSqlUtils.deletePost(testPostId)
        testLists.forEach {
            assertEquals(0, postListSqlUtils.getPostList(it.id)?.size)
        }
    }

    @Test
    fun insertDuplicatePostListModel() {
        val testSite = SiteModel()
        testSite.id = 123 // value doesn't matter
        val testPostId = 1245 // value doesn't matter
        val listType = ListType.POSTS_ALL
        val date1 = "1955-11-05T14:15:00Z"
        val date2 = "1955-11-05T14:25:00Z"

        /**
         * 1. Since a [PostListModel] requires a [ListModel] in the DB due to the foreign key restriction, a test list
         * will be inserted in the DB.
         * 2. Generate a [PostListModel] for `date1` and insert it in the DB
         * 3. Verify that it's inserted correctly and [PostListModel.date] equals to `date1`
         */
        val testList = insertTestList(testSite, listType)
        val postListModel = generatePostListModel(testList.id, testPostId, date1)
        postListSqlUtils.insertPostList(testList.id, arrayListOf(postListModel))
        val insertedPostList = postListSqlUtils.getPostList(testList.id)
        assertEquals(date1, insertedPostList?.firstOrNull()?.date)

        /**
         * 1. Update the `date` of `postListModel` to a different date: `date2`
         * 2. Insert the updated `postListModel` to DB
         * 3. Verify that no new record is created and the list size is the same.
         * 4. Verify that the `date` is correctly updated after the second insertion.
         */
        postListModel.date = date2
        postListSqlUtils.insertPostList(testList.id, arrayListOf(postListModel))
        val updatedPostList = postListSqlUtils.getPostList(testList.id)
        assertEquals(insertedPostList?.size, updatedPostList?.size)
        assertEquals(date2, updatedPostList?.firstOrNull()?.date)
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
    private fun generatePostList(listModel: ListModel, count: Int): List<PostListModel> =
            (1..count).map { generatePostListModel(listModel.id, it) }

    /**
     * Helper function that generates a [PostListModel] instance.
     */
    private fun generatePostListModel(
        listId: Int,
        postId: Int,
        date: String = "1955-11-05T14:15:00Z" // just a random valid date since most test don't care about it
    ): PostListModel {
        val postListModel = PostListModel()
        postListModel.listId = listId
        postListModel.postId = postId
        postListModel.date = date
        return postListModel
    }
}
