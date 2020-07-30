package org.wordpress.android.ui.reader.services.discover

import android.app.job.JobParameters
import com.wordpress.rest.RestRequest.ErrorListener
import com.wordpress.rest.RestRequest.Listener
import org.greenrobot.eventbus.EventBus
import org.json.JSONArray
import org.json.JSONObject
import org.wordpress.android.WordPress
import org.wordpress.android.datasets.ReaderDiscoverCardsTable
import org.wordpress.android.datasets.ReaderPostTable
import org.wordpress.android.models.ReaderPost
import org.wordpress.android.models.ReaderPostList
import org.wordpress.android.models.ReaderTag
import org.wordpress.android.models.discover.ReaderDiscoverCard
import org.wordpress.android.models.discover.ReaderDiscoverCard.InterestsYouMayLikeCard
import org.wordpress.android.models.discover.ReaderDiscoverCard.ReaderPostCard
import org.wordpress.android.modules.AppComponent
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.ui.reader.ReaderConstants.JSON_CARDS
import org.wordpress.android.ui.reader.ReaderConstants.JSON_CARD_DATA
import org.wordpress.android.ui.reader.ReaderConstants.JSON_CARD_INTERESTS_YOU_MAY_LIKE
import org.wordpress.android.ui.reader.ReaderConstants.JSON_CARD_POST
import org.wordpress.android.ui.reader.ReaderConstants.JSON_CARD_TYPE
import org.wordpress.android.ui.reader.ReaderConstants.POST_ID
import org.wordpress.android.ui.reader.ReaderConstants.POST_PSEUDO_ID
import org.wordpress.android.ui.reader.ReaderConstants.POST_SITE_ID
import org.wordpress.android.ui.reader.ReaderEvents.FetchDiscoverCardsEnded
import org.wordpress.android.ui.reader.actions.ReaderActions.UpdateResult.FAILED
import org.wordpress.android.ui.reader.actions.ReaderActions.UpdateResult.HAS_NEW
import org.wordpress.android.ui.reader.actions.ReaderActions.UpdateResult.UNCHANGED
import org.wordpress.android.ui.reader.actions.ReaderActions.UpdateResultListener
import org.wordpress.android.ui.reader.repository.usecases.ParseDiscoverCardsJsonUseCase
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
class ReaderDiscoverLogic constructor(
    private val completionListener: ServiceCompletionListener,
    appComponent: AppComponent
) {
    init {
        appComponent.inject(this)
    }

    @Inject lateinit var parseDiscoverCardsJsonUseCase: ParseDiscoverCardsJsonUseCase
    @Inject lateinit var appPrefsWrapper: AppPrefsWrapper

    enum class DiscoverTasks {
        REQUEST_MORE, REQUEST_FIRST_PAGE
    }

    private var listenerCompanion: JobParameters? = null

    fun performTasks(task: DiscoverTasks, companion: JobParameters?) {
        listenerCompanion = companion

        when (task) {
            REQUEST_MORE -> {
                requestDataForDiscover(REQUEST_MORE, UpdateResultListener {
                    EventBus.getDefault().post(FetchDiscoverCardsEnded(it != FAILED))
                    completionListener.onCompleted(listenerCompanion)
                })
            }
            REQUEST_FIRST_PAGE -> {
                requestDataForDiscover(REQUEST_FIRST_PAGE, UpdateResultListener {
                    EventBus.getDefault().post(FetchDiscoverCardsEnded(it != FAILED))
                    completionListener.onCompleted(listenerCompanion)
                })
            }
        }
    }

    private fun requestDataForDiscover(taskType: DiscoverTasks, resultListener: UpdateResultListener) {
        val params = HashMap<String, String>()
        // TODO malinjir pass interests the user is following
        params["tags"] = "android"

        when (taskType) {
            REQUEST_FIRST_PAGE -> appPrefsWrapper.readerCardsPageHandle = null
            REQUEST_MORE -> {
                val pageHandle = appPrefsWrapper.readerCardsPageHandle
                if (pageHandle?.isNotEmpty() == true) {
                    params["page_handle"] = pageHandle
                } else {
                    // there are no more pages to load
                    resultListener.onUpdateResult(UNCHANGED)
                    return
                }
            }
        }

        val listener = Listener { jsonObject -> // remember when this tag was updated if newer posts were requested
            handleRequestDiscoverDataResponse(taskType, jsonObject, resultListener)
        }
        val errorListener = ErrorListener { volleyError ->
            AppLog.e(READER, volleyError)
            resultListener.onUpdateResult(FAILED)
        }

        WordPress.getRestClientUtilsV2()["read/tags/cards", params, null, listener, errorListener]
    }

    private fun handleRequestDiscoverDataResponse(
        taskType: DiscoverTasks,
        json: JSONObject?,
        resultListener: UpdateResultListener
    ) {
        // TODO malinjir move to bg thread
        if (json == null) {
            resultListener.onUpdateResult(FAILED)
            return
        }
        if (taskType == REQUEST_FIRST_PAGE) {
            clearCache()
        }
        val fullCardsJson = json.optJSONArray(JSON_CARDS)

        // Parse the json into cards model objects
        val cards = parseCards(fullCardsJson)
        insertPostsIntoDb(cards.filterIsInstance<ReaderPostCard>().map { it.post })

        // Simplify the json. The simplified version is used in the upper layers to load the data from the db.
        val simplifiedCardsJson = createSimplifiedJson(fullCardsJson)
        insertCardsJsonIntoDb(simplifiedCardsJson)

        val nextPageHandle = parseDiscoverCardsJsonUseCase.parseNextPageHandle(json)
        appPrefsWrapper.readerCardsPageHandle = nextPageHandle

        resultListener.onUpdateResult(HAS_NEW)
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
            }
        }
        return cards
    }

    private fun insertPostsIntoDb(posts: List<ReaderPost>) {
        val postList = ReaderPostList()
        postList.addAll(posts)
        ReaderPostTable.addOrUpdatePosts(null, postList)
    }

    /**
     * This method creates a simplified version of the provided json.
     *
     * It for example copies only ids from post object as we don't need to store the gigantic post in the json
     * as it's already stored in the db.
     */
    private fun createSimplifiedJson(cardsJsonArray: JSONArray): JSONArray {
        var index = 0
        val simplifiedJson = JSONArray()
        for (i in 0 until cardsJsonArray.length()) {
            val cardJson = cardsJsonArray.getJSONObject(i)
            when (cardJson.getString(JSON_CARD_TYPE)) {
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

    private fun insertCardsJsonIntoDb(simplifiedCardsJson: JSONArray) {
        ReaderDiscoverCardsTable.addCardsPage(simplifiedCardsJson.toString())
    }

    private fun clearCache() {
        ReaderDiscoverCardsTable.reset()
        ReaderPostTable.deletePostsWithTag(ReaderTag.createDiscoverPostCardsTag())
    }
}
