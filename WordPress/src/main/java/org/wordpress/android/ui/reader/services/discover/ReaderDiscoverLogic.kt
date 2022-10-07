package org.wordpress.android.ui.reader.services.discover

import android.app.job.JobParameters
import com.wordpress.rest.RestRequest.ErrorListener
import com.wordpress.rest.RestRequest.Listener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus
import org.json.JSONArray
import org.json.JSONObject
import org.wordpress.android.WordPress
import org.wordpress.android.datasets.ReaderBlogTable
import org.wordpress.android.datasets.ReaderBlogTableWrapper
import org.wordpress.android.datasets.ReaderDiscoverCardsTable
import org.wordpress.android.datasets.ReaderPostTable
import org.wordpress.android.datasets.wrappers.ReaderTagTableWrapper
import org.wordpress.android.models.ReaderBlog
import org.wordpress.android.models.ReaderPost
import org.wordpress.android.models.ReaderPostList
import org.wordpress.android.models.ReaderTag
import org.wordpress.android.models.discover.ReaderDiscoverCard
import org.wordpress.android.models.discover.ReaderDiscoverCard.InterestsYouMayLikeCard
import org.wordpress.android.models.discover.ReaderDiscoverCard.ReaderPostCard
import org.wordpress.android.models.discover.ReaderDiscoverCard.ReaderRecommendedBlogsCard
import org.wordpress.android.modules.AppComponent
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.ui.reader.ReaderConstants.JSON_CARDS
import org.wordpress.android.ui.reader.ReaderConstants.JSON_CARD_DATA
import org.wordpress.android.ui.reader.ReaderConstants.JSON_CARD_INTERESTS_YOU_MAY_LIKE
import org.wordpress.android.ui.reader.ReaderConstants.JSON_CARD_POST
import org.wordpress.android.ui.reader.ReaderConstants.JSON_CARD_RECOMMENDED_BLOGS
import org.wordpress.android.ui.reader.ReaderConstants.JSON_CARD_TYPE
import org.wordpress.android.ui.reader.ReaderConstants.POST_ID
import org.wordpress.android.ui.reader.ReaderConstants.POST_PSEUDO_ID
import org.wordpress.android.ui.reader.ReaderConstants.POST_SITE_ID
import org.wordpress.android.ui.reader.ReaderConstants.RECOMMENDED_BLOG_ID
import org.wordpress.android.ui.reader.ReaderConstants.RECOMMENDED_FEED_ID
import org.wordpress.android.ui.reader.ReaderEvents.FetchDiscoverCardsEnded
import org.wordpress.android.ui.reader.actions.ReaderActions.UpdateResult.FAILED
import org.wordpress.android.ui.reader.actions.ReaderActions.UpdateResult.HAS_NEW
import org.wordpress.android.ui.reader.actions.ReaderActions.UpdateResult.UNCHANGED
import org.wordpress.android.ui.reader.actions.ReaderActions.UpdateResultListener
import org.wordpress.android.ui.reader.repository.usecases.GetDiscoverCardsUseCase
import org.wordpress.android.ui.reader.repository.usecases.ParseDiscoverCardsJsonUseCase
import org.wordpress.android.ui.reader.repository.usecases.tags.GetFollowedTagsUseCase
import org.wordpress.android.ui.reader.services.ServiceCompletionListener
import org.wordpress.android.ui.reader.services.discover.ReaderDiscoverLogic.DiscoverTasks.REQUEST_FIRST_PAGE
import org.wordpress.android.ui.reader.services.discover.ReaderDiscoverLogic.DiscoverTasks.REQUEST_MORE
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T.READER
import java.util.HashMap
import javax.inject.Inject

/**
 * This class contains logic related to fetching data for the discover tab in the Reader.
 */
class ReaderDiscoverLogic(
    private val completionListener: ServiceCompletionListener,
    private val coroutineScope: CoroutineScope,
    appComponent: AppComponent
) {
    init {
        appComponent.inject(this)
    }

    @Inject lateinit var parseDiscoverCardsJsonUseCase: ParseDiscoverCardsJsonUseCase
    @Inject lateinit var appPrefsWrapper: AppPrefsWrapper
    @Inject lateinit var readerTagTableWrapper: ReaderTagTableWrapper
    @Inject lateinit var getFollowedTagsUseCase: GetFollowedTagsUseCase
    @Inject lateinit var readerBlogTableWrapper: ReaderBlogTableWrapper
    @Inject lateinit var getDiscoverCardsUseCase: GetDiscoverCardsUseCase

    enum class DiscoverTasks {
        REQUEST_MORE, REQUEST_FIRST_PAGE
    }

    private var listenerCompanion: JobParameters? = null

    fun performTasks(task: DiscoverTasks, companion: JobParameters?) {
        listenerCompanion = companion
        requestDataForDiscover(task, UpdateResultListener {
            EventBus.getDefault().post(FetchDiscoverCardsEnded(task, it))
            completionListener.onCompleted(listenerCompanion)
        })
    }

    private fun requestDataForDiscover(taskType: DiscoverTasks, resultListener: UpdateResultListener) {
        coroutineScope.launch {
            val params = HashMap<String, String>()
            params["tags"] = getFollowedTagsUseCase.get().joinToString { it.tagSlug }

            when (taskType) {
                REQUEST_FIRST_PAGE -> {
                    appPrefsWrapper.readerCardsPageHandle = null
                    /* "refresh" parameter is used to tell the server to return a different set of data so we don't
                    present a static content to the user. We need to include it only for the first page (when
                    page_handle is empty). The server will take care of including the "refresh" parameter within the
                    page_handle so the user will stay on the same shard while they’re paging through the cards. */
                    params["refresh"] = appPrefsWrapper.getReaderCardsRefreshCounter().toString()
                    appPrefsWrapper.incrementReaderCardsRefreshCounter()
                }
                REQUEST_MORE -> {
                    val pageHandle = appPrefsWrapper.readerCardsPageHandle
                    if (pageHandle?.isNotEmpty() == true) {
                        params["page_handle"] = pageHandle
                    } else {
                        // there are no more pages to load
                        resultListener.onUpdateResult(UNCHANGED)
                        return@launch
                    }
                }
            }

            val listener = Listener { jsonObject ->
                coroutineScope.launch {
                    handleRequestDiscoverDataResponse(taskType, jsonObject, resultListener)
                }
            }
            val errorListener = ErrorListener { volleyError ->
                AppLog.e(READER, volleyError)
                resultListener.onUpdateResult(FAILED)
            }
            WordPress.getRestClientUtilsV2()["read/tags/cards", params, null, listener, errorListener]
        }
    }

    private suspend fun handleRequestDiscoverDataResponse(
        taskType: DiscoverTasks,
        json: JSONObject?,
        resultListener: UpdateResultListener
    ) {
        if (json == null) {
            resultListener.onUpdateResult(FAILED)
            return
        }
        if (taskType == REQUEST_FIRST_PAGE) {
            clearCache()
        }
        json.optJSONArray(JSON_CARDS)?.let { fullCardsJson ->
            // Parse the json into cards model objects
            val cards = parseCards(fullCardsJson)
            insertPostsIntoDb(cards.filterIsInstance<ReaderPostCard>().map { it.post })
            insertBlogsIntoDb(cards.filterIsInstance<ReaderRecommendedBlogsCard>().map { it.blogs }.flatten())

            // Simplify the json. The simplified version is used in the upper layers to load the data from the db.
            val simplifiedCardsJson = createSimplifiedJson(fullCardsJson)
            insertCardsJsonIntoDb(simplifiedCardsJson)

            val nextPageHandle = parseDiscoverCardsJsonUseCase.parseNextPageHandle(json)
            appPrefsWrapper.readerCardsPageHandle = nextPageHandle

            if (cards.isEmpty()) {
                readerTagTableWrapper.clearTagLastUpdated(ReaderTag.createDiscoverPostCardsTag())
            } else {
                readerTagTableWrapper.setTagLastUpdated(ReaderTag.createDiscoverPostCardsTag())
            }

            resultListener.onUpdateResult(HAS_NEW)
        }
    }

    private fun parseCards(cardsJsonArray: JSONArray): ArrayList<ReaderDiscoverCard> {
        val cards: ArrayList<ReaderDiscoverCard> = arrayListOf()
        for (i in 0 until cardsJsonArray.length()) {
            val cardJson = cardsJsonArray.getJSONObject(i)
            when (cardJson.getString(JSON_CARD_TYPE)) {
                JSON_CARD_INTERESTS_YOU_MAY_LIKE -> {
                    val interests = parseDiscoverCardsJsonUseCase.parseInterestCard(cardJson)
                    cards.add(InterestsYouMayLikeCard(interests))
                }
                JSON_CARD_POST -> {
                    val post = parseDiscoverCardsJsonUseCase.parsePostCard(cardJson)
                    cards.add(ReaderPostCard(post))
                }
                JSON_CARD_RECOMMENDED_BLOGS -> {
                    cardJson?.let {
                        val recommendedBlogs = parseDiscoverCardsJsonUseCase.parseRecommendedBlogsCard(it)
                        cards.add(ReaderRecommendedBlogsCard(recommendedBlogs))
                    }
                }
            }
        }
        return cards
    }

    private fun insertPostsIntoDb(posts: List<ReaderPost>) {
        val postList = ReaderPostList()
        postList.addAll(posts)
        ReaderPostTable.addOrUpdatePosts(ReaderTag.createDiscoverPostCardsTag(), postList)
    }

    private fun insertBlogsIntoDb(blogs: List<ReaderBlog>) {
        blogs.forEach { blog ->
            ReaderBlogTable.addOrUpdateBlog(blog)
        }
    }

    /**
     * This method creates a simplified version of the provided json.
     *
     * It for example copies only ids from post object as we don't need to store the gigantic post in the json
     * as it's already stored in the db.
     */
    @Suppress("NestedBlockDepth")
    private fun createSimplifiedJson(cardsJsonArray: JSONArray): JSONArray {
        var index = 0
        val simplifiedJson = JSONArray()
        for (i in 0 until cardsJsonArray.length()) {
            val cardJson = cardsJsonArray.getJSONObject(i)
            when (cardJson.getString(JSON_CARD_TYPE)) {
                JSON_CARD_RECOMMENDED_BLOGS -> {
                    cardJson.optJSONArray(JSON_CARD_DATA)?.let { recommendedBlogsCardJson ->
                        if (recommendedBlogsCardJson.length() > 0) {
                            simplifiedJson.put(index++, createSimplifiedRecommendedBlogsCardJson(cardJson))
                        }
                    }
                }
                JSON_CARD_INTERESTS_YOU_MAY_LIKE -> {
                    simplifiedJson.put(index++, cardJson)
                }
                JSON_CARD_POST -> {
                    simplifiedJson.put(index++, createSimplifiedPostJson(cardJson))
                }
            }
        }
        return simplifiedJson
    }

    /**
     * Create a simplified copy of the json - keeps only postId, siteId and pseudoId.
     */
    private fun createSimplifiedPostJson(originalCardJson: JSONObject): JSONObject {
        val originalPostData = originalCardJson.getJSONObject(JSON_CARD_DATA)

        val simplifiedPostData = JSONObject()
        // copy only fields which uniquely identify this post
        simplifiedPostData.put(POST_ID, originalPostData.get(POST_ID))
        simplifiedPostData.put(POST_SITE_ID, originalPostData.get(POST_SITE_ID))
        simplifiedPostData.put(POST_PSEUDO_ID, originalPostData.get(POST_PSEUDO_ID))

        val simplifiedCardJson = JSONObject()
        simplifiedCardJson.put(JSON_CARD_TYPE, originalCardJson.getString(JSON_CARD_TYPE))
        simplifiedCardJson.put(JSON_CARD_DATA, simplifiedPostData)
        return simplifiedCardJson
    }

    @Suppress("NestedBlockDepth")
    private fun createSimplifiedRecommendedBlogsCardJson(originalCardJson: JSONObject): JSONObject {
        return JSONObject().apply {
            JSONArray().apply {
                originalCardJson.optJSONArray(JSON_CARD_DATA)?.let { jsonRecommendedBlogs ->
                    for (i in 0 until jsonRecommendedBlogs.length()) {
                        JSONObject().apply {
                            val jsonRecommendedBlog = jsonRecommendedBlogs.getJSONObject(i)
                            put(RECOMMENDED_BLOG_ID, jsonRecommendedBlog.optLong(RECOMMENDED_BLOG_ID))
                            put(RECOMMENDED_FEED_ID, jsonRecommendedBlog.optLong(RECOMMENDED_FEED_ID))
                        }.let {
                            put(it)
                        }
                    }
                }
            }.let { simplifiedRecommendedBlogsJson ->
                put(JSON_CARD_TYPE, originalCardJson.getString(JSON_CARD_TYPE))
                put(JSON_CARD_DATA, simplifiedRecommendedBlogsJson)
            }
        }
    }

    private fun insertCardsJsonIntoDb(simplifiedCardsJson: JSONArray) {
        ReaderDiscoverCardsTable.addCardsPage(simplifiedCardsJson.toString())
    }

    private suspend fun clearCache() {
        val blogIds = getRecommendedBlogsToBeDeleted().map { it.blogId }
        ReaderBlogTable.deleteBlogsWithIds(blogIds)

        ReaderDiscoverCardsTable.clear()
        ReaderPostTable.deletePostsWithTag(ReaderTag.createDiscoverPostCardsTag())
    }

    private suspend fun getRecommendedBlogsToBeDeleted(): List<ReaderBlog> {
        val discoverCards = getDiscoverCardsUseCase.get()

        val blogsToBeDeleted = ArrayList<ReaderBlog>()
        discoverCards.cards.filterIsInstance<ReaderRecommendedBlogsCard>().forEach {
            it.blogs.forEach { blog ->
                if (!blog.isFollowing) {
                    blogsToBeDeleted.add(blog)
                }
            }
        }

        return blogsToBeDeleted
    }
}
