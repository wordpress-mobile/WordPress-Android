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
import org.wordpress.android.fluxc.persistence.SiteSqlUtils
import org.wordpress.android.fluxc.persistence.WellSqlConfig
import org.wordpress.android.fluxc.site.SiteUtils.generateSelfHostedNonJPSite
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
        val testSite = generateAndInsertSelfHostedNonJPTestSite()
        val postCount = 20 // value doesn't matter
        val listType = ListType.POSTS_ALL

        /**
         * 1. Since a [PostListModel] requires a [ListModel] in the DB due to the foreign key restriction, a test list
         * will be inserted in the DB.
         * 2. A list of test [PostListModel]s will be generated and inserted in the DB
         * 3. Verify that the [PostListModel] instances are inserted correctly
         */
        val testList = insertTestList(testSite.id, listType)
        val postList = generatePostList(testList, postCount)
        postListSqlUtils.insertPostList(postList)
        assertEquals(postCount, postListSqlUtils.getPostList(testList.id).size)
    }

    @Test
    fun testListIdForeignKeyCascadeDelete() {
        val testSite = generateAndInsertSelfHostedNonJPTestSite()
        val postCount = 20 // value doesn't matter
        val listType = ListType.POSTS_ALL

        /**
         * 1. Since a [PostListModel] requires a [ListModel] in the DB due to the foreign key restriction, a test list
         * will be inserted in the DB.
         * 2. A list of test [PostListModel]s will be generated and inserted in the DB
         * 3. Verify that the [PostListModel] instances are inserted correctly
         */
        val testList = insertTestList(testSite.id, listType)
        val postList = generatePostList(testList, postCount)
        postListSqlUtils.insertPostList(postList)
        assertEquals(postCount, postListSqlUtils.getPostList(testList.id).size)

        /**
         * 1. Delete the inserted list
         * 2. Verify that deleting the list also deletes the inserted [PostListModel]s due to foreign key restriction
         */
        listSqlUtils.deleteList(testSite.id, listType)
        assertEquals(0, postListSqlUtils.getPostList(testList.id).size)
    }

    @Test
    fun testDeletePost() {
        val testSite = generateAndInsertSelfHostedNonJPTestSite()
        val testPostId = 1245 // value doesn't matter

        /**
         * 1. Insert a test list for every list type. There should at least be 2 list types for a good test.
         * 2. Generate a [PostListModel] for every list type with the same id and insert it
         * 3. Verify that the [PostListModel] was inserted correctly
         */
        val testLists = ListType.values().map { insertTestList(testSite.id, it) }
        val postList = testLists.map { generatePostListModel(it.id, testPostId) }
        postListSqlUtils.insertPostList(postList)
        testLists.forEach { list ->
            assertEquals(1, postListSqlUtils.getPostList(list.id).size)
        }

        /**
         * 1. Delete [PostListModel]s for which [PostListModel.postId] == `testPostId`
         * 2. Verify that [PostListModel]s from every list is deleted
         */
        postListSqlUtils.deletePost(testLists.map { it.id }, testPostId)
        testLists.forEach {
            assertEquals(0, postListSqlUtils.getPostList(it.id).size)
        }
    }

    @Test
    fun insertDuplicatePostListModel() {
        val testSite = generateAndInsertSelfHostedNonJPTestSite()
        val testPostId = 1245 // value doesn't matter
        val listType = ListType.POSTS_ALL

        /**
         * 1. Since a [PostListModel] requires a [ListModel] in the DB due to the foreign key restriction, a test list
         * will be inserted in the DB.
         * 2. Generate 2 [PostListModel]s with the exact same values and insert the first one in the DB
         * 3. Verify that first [PostListModel] is inserted correctly
         */
        val testList = insertTestList(testSite.id, listType)
        val postListModel = generatePostListModel(testList.id, testPostId)
        val postListModel2 = generatePostListModel(testList.id, testPostId)
        postListSqlUtils.insertPostList(listOf(postListModel))
        val insertedPostList = postListSqlUtils.getPostList(testList.id)
        assertEquals(1, insertedPostList.size)

        /**
         * 1. Insert the second [PostListModel] in the DB
         * 2. Verify that no new record is created and the list size is the same.
         * 3. Verify that the [PostListModel.id] has not changed
         */
        postListSqlUtils.insertPostList(listOf(postListModel2))
        val updatedPostList = postListSqlUtils.getPostList(testList.id)
        assertEquals(1, updatedPostList.size)
        assertEquals(insertedPostList[0].id, updatedPostList[0].id)
    }

    /**
     * Creates and inserts a [ListModel] for the given site. It also verifies that the list is inserted correctly.
     */
    private fun insertTestList(localSiteId: Int, listType: ListType): ListModel {
        listSqlUtils.insertOrUpdateList(localSiteId, listType)
        val list = listSqlUtils.getList(localSiteId, listType)
        assertNotNull(list)
        return list!!
    }

    /**
     * Helper function that creates a list of [PostListModel] to be used in tests.
     */
    private fun generatePostList(listModel: ListModel, count: Int): List<PostListModel> =
            (1..count).map { generatePostListModel(listModel.id, it) }

    /**
     * Helper function that generates a [PostListModel] instance.
     */
    private fun generatePostListModel(
        listId: Int,
        postId: Int
    ): PostListModel {
        val postListModel = PostListModel()
        postListModel.listId = listId
        postListModel.postId = postId
        return postListModel
    }

    /**
     * Helper function that generates a self-hosted test site and inserts it into the DB. Since we have a FK restriction
     * for [ListModel.localSiteId] we need to do this before we can insert [ListModel] instances.
     */
    private fun generateAndInsertSelfHostedNonJPTestSite(): SiteModel {
        val site = generateSelfHostedNonJPSite()
        SiteSqlUtils.insertOrUpdateSite(site)
        return site
    }
}
