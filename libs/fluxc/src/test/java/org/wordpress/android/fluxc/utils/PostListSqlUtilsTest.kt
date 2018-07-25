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
import org.wordpress.android.fluxc.site.SiteUtils.generateJetpackSiteOverXMLRPC
import org.wordpress.android.fluxc.site.SiteUtils.generateSelfHostedNonJPSite
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

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
        val postList = generatePostList(testList, testSite.id, postCount)
        postListSqlUtils.insertPostList(postList)
        assertEquals(postCount, postListSqlUtils.getPostList(testList.id).size)
    }

    @Test
    fun testDeletePostList() {
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
        val postList = generatePostList(testList, testSite.id, postCount)
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
        val postList = testLists.map { generatePostListModel(it.id, testSite.id, testPostId) }
        postListSqlUtils.insertPostList(postList)
        testLists.forEach { list ->
            assertEquals(1, postListSqlUtils.getPostList(list.id).size)
        }

        /**
         * 1. Delete [PostListModel]s for which [PostListModel.postId] == `testPostId`
         * 2. Verify that [PostListModel]s from every list is deleted
         */
        postListSqlUtils.deletePost(testSite.id, testPostId)
        testLists.forEach {
            assertEquals(0, postListSqlUtils.getPostList(it.id).size)
        }
    }

    /**
     * This is a test to ensure that if there are multiple [PostListModel]s with the same `postId`, only the one with
     * the correct `siteId` will be deleted.
     */
    @Test
    fun testDeletePostOnlyForTheCorrectSite() {
        /**
         * 1. Generate and insert 2 different test sites
         * 2. Insert a [ListModel] for each test site
         * 3. Insert a [PostListModel] for each test list with the same postId
         */
        val testSite1 = generateAndInsertSelfHostedNonJPTestSite()
        val testSite2 = generateAndInsertJetpackSiteOverXMLRPCTestSite()
        val testPostId = 1245 // value doesn't matter
        val listType = ListType.POSTS_ALL

        val testLists = listOf(testSite1, testSite2).map { insertTestList(it.id, listType) }
        val postList = testLists.map {
            generatePostListModel(it.id, it.localSiteId, testPostId)
        }
        postListSqlUtils.insertPostList(postList)

        /**
         * 1. Verify that for each list, a single post is inserted
         * 2. Verify that inserted post has the expected id
         */
        testLists.forEach {
            val insertedPostList = postListSqlUtils.getPostList(it.id)
            assertEquals(1, insertedPostList.size)
            assertEquals(testPostId, insertedPostList[0].postId)
        }

        /**
         * 1. Delete the post for the first site
         * 2. Verify that it's deleted from the first list which corresponds to the first list
         * 3. Verify that it's NOT deleted from the second list
         */
        postListSqlUtils.deletePost(testSite1.id, testPostId)
        assertEquals(0, postListSqlUtils.getPostList(testLists[0].id).size)
        assertEquals(1, postListSqlUtils.getPostList(testLists[1].id).size)
    }

    @Test
    fun insertDuplicatePostListModel() {
        val testSite = generateAndInsertSelfHostedNonJPTestSite()
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
        val testList = insertTestList(testSite.id, listType)
        val postListModel = generatePostListModel(testList.id, testSite.id, testPostId, date1)
        postListSqlUtils.insertPostList(listOf(postListModel))
        val insertedPostList = postListSqlUtils.getPostList(testList.id)
        assertEquals(date1, insertedPostList.firstOrNull()?.date)

        /**
         * 1. Update the `date` of `postListModel` to a different date: `date2`
         * 2. Insert the updated `postListModel` to DB
         * 3. Verify that no new record is created and the list size is the same.
         * 4. Verify that the `date` is correctly updated after the second insertion.
         */
        postListModel.date = date2
        postListSqlUtils.insertPostList(listOf(postListModel))
        val updatedPostList = postListSqlUtils.getPostList(testList.id)
        assertEquals(insertedPostList.size, updatedPostList.size)
        assertEquals(date2, updatedPostList.firstOrNull()?.date)
    }

    @Test
    fun testListIdForeignKeyCascadeDelete() {
        val postCount = 20 // value doesn't matter
        val listType = ListType.POSTS_ALL
        /**
         * 1. Generate and insert a self-hosted test site
         * 2. Verify that the site is inserted
         * 3. Generate and insert a [ListModel] for the test site
         * 4. Generate a list of [PostListModel]s for the test list
         * 5. Verify that the [PostListModel] was inserted correctly
         */
        val testSite = generateAndInsertSelfHostedNonJPTestSite()
        assertFalse(SiteSqlUtils.getSitesAccessedViaXMLRPC().asModel.isEmpty())
        val testList = insertTestList(testSite.id, listType)
        val postList = generatePostList(testList, testSite.id, postCount)
        postListSqlUtils.insertPostList(postList)
        assertEquals(postCount, postListSqlUtils.getPostList(testList.id).size)

        /**
         * 1. Delete the test list
         * 2. Verify that test list is deleted
         * 3. Verify that [PostListModel]s for the test list are deleted
         */
        listSqlUtils.deleteList(testSite.id, listType)
        assertNull(listSqlUtils.getList(testSite.id, listType))
        assertTrue(postListSqlUtils.getPostList(testList.id).isEmpty())
    }

    @Test
    fun testLocalSiteIdForeignKeyCascadeDelete() {
        val postCount = 20 // value doesn't matter
        val listType = ListType.POSTS_ALL
        /**
         * 1. Generate and insert a self-hosted test site
         * 2. Verify that the site is inserted
         * 3. Generate and insert a [ListModel] for the test site
         * 4. Generate a list of [PostListModel]s for the test list
         * 5. Verify that the [PostListModel] was inserted correctly
         */
        val testSite = generateAndInsertSelfHostedNonJPTestSite()
        assertFalse(SiteSqlUtils.getSitesAccessedViaXMLRPC().asModel.isEmpty())
        val testList = insertTestList(testSite.id, listType)
        val postList = generatePostList(testList, testSite.id, postCount)
        postListSqlUtils.insertPostList(postList)
        assertEquals(postCount, postListSqlUtils.getPostList(testList.id).size)

        /**
         * 1. Delete the test site
         * 2. Verify that test site is deleted
         * 3. Verify that [PostListModel]s for the test site are deleted
         */
        SiteSqlUtils.deleteSite(testSite)
        assertTrue(SiteSqlUtils.getSitesAccessedViaXMLRPC().asModel.isEmpty())
        assertTrue(postListSqlUtils.getPostList(testList.id).isEmpty())
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
     * The [PostListModel.date] will be the same date for all [PostListModel]s.
     */
    private fun generatePostList(listModel: ListModel, localSiteId: Int, count: Int): List<PostListModel> =
            (1..count).map { generatePostListModel(listModel.id, localSiteId, it) }

    /**
     * Helper function that generates a [PostListModel] instance.
     */
    private fun generatePostListModel(
        listId: Int,
        localSiteId: Int,
        postId: Int,
        date: String = "1955-11-05T14:15:00Z" // just a random valid date since most test don't care about it
    ): PostListModel {
        val postListModel = PostListModel()
        postListModel.listId = listId
        postListModel.localSiteId = localSiteId
        postListModel.postId = postId
        postListModel.date = date
        return postListModel
    }

    /**
     * Helper function that generates a self-hosted test site and inserts it into the DB. Since we have a FK restriction
     * for [PostListModel.localSiteId] we need to do this before we can insert [PostListModel] instances.
     */
    private fun generateAndInsertSelfHostedNonJPTestSite(): SiteModel {
        val site = generateSelfHostedNonJPSite()
        SiteSqlUtils.insertOrUpdateSite(site)
        return site
    }

    /**
     * Helper function that generates a self-hosted test site with Jetpack and inserts it into the DB. Since we have a
     * FK restriction for [PostListModel.localSiteId] we need to do this before we can insert [PostListModel] instances.
     */
    private fun generateAndInsertJetpackSiteOverXMLRPCTestSite(): SiteModel {
        val site = generateJetpackSiteOverXMLRPC()
        SiteSqlUtils.insertOrUpdateSite(site)
        return site
    }
}
