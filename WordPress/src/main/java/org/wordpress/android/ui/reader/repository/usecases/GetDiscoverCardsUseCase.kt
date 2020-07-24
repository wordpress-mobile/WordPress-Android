package org.wordpress.android.ui.reader.repository.usecases

import dagger.Reusable
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import org.wordpress.android.datasets.ReaderDiscoverCardsTable
import org.wordpress.android.datasets.ReaderPostTable
import org.wordpress.android.models.ReaderTag
import org.wordpress.android.models.ReaderTagList
import org.wordpress.android.models.ReaderTagType.DEFAULT
import org.wordpress.android.models.discover.ReaderDiscoverCard
import org.wordpress.android.models.discover.ReaderDiscoverCard.InterestsYouMayLikeCard
import org.wordpress.android.models.discover.ReaderDiscoverCard.ReaderPostCard
import org.wordpress.android.models.discover.ReaderDiscoverCards
import org.wordpress.android.modules.IO_THREAD
import org.wordpress.android.ui.reader.ReaderConstants
import org.wordpress.android.util.JSONUtils
import javax.inject.Inject
import javax.inject.Named

@Reusable
class GetDiscoverCardsUseCase @Inject constructor(@Named(IO_THREAD) private val ioDispatcher: CoroutineDispatcher) :
        ReaderRepositoryDispatchingUseCase(ioDispatcher) {
    suspend fun get(): ReaderDiscoverCards =
            withContext(coroutineContext) {
                val cardJsonList = ReaderDiscoverCardsTable.loadDiscoverCardsJsons()
                val cards: ArrayList<ReaderDiscoverCard> = arrayListOf()

                if (cardJsonList.isNotEmpty()) {
                    // TODO malinjir concat the arrays into a big single array
                    val jsonObjects = cardJsonList.map { JSONArray(it) }[0]

                    for (i in 0 until jsonObjects.length()) {
                        val cardJson = jsonObjects.getJSONObject(i)
                        when (cardJson.getString(ReaderConstants.JSON_CARD_TYPE)) {
                            ReaderConstants.JSON_CARD_INTERESTS_YOU_MAY_LIKE -> {
                                val interests = parseInterestTagsList(cardJson)
                                cards.add(InterestsYouMayLikeCard(interests))
                            }
                            ReaderConstants.JSON_CARD_POST -> {
                                // TODO we might want to load the data in batch
                                val postDataJson = cardJson.getJSONObject(ReaderConstants.JSON_CARD_DATA)
                                val postId = postDataJson.optLong(ReaderConstants.POST_ID)
                                val blogId = postDataJson.optLong(ReaderConstants.POST_SITE_ID)
                                val post = ReaderPostTable.getBlogPost(blogId, postId, false)
                                cards.add(ReaderPostCard(post))
                            }
                        }
                    }
                }
                return@withContext ReaderDiscoverCards(cards)
            }

    // TODO malinjir copied from ReaderDiscoverLogic
    private fun parseInterestTagsList(jsonObject: JSONObject?): ReaderTagList {
        val interestTags = ReaderTagList()
        if (jsonObject == null) {
            return interestTags
        }
        val jsonInterests = jsonObject.optJSONArray(ReaderConstants.JSON_CARD_DATA) ?: return interestTags
        for (i in 0 until jsonInterests.length()) {
            interestTags.add(parseInterestTag(jsonInterests.optJSONObject(i)))
        }
        return interestTags
    }

    // TODO malinjir copied from ReaderDiscoverLogic
    private fun parseInterestTag(jsonInterest: JSONObject): ReaderTag {
        val tagTitle = JSONUtils.getStringDecoded(jsonInterest, ReaderConstants.JSON_TAG_TITLE)
        val tagSlug = JSONUtils.getStringDecoded(jsonInterest, ReaderConstants.JSON_TAG_SLUG)
        return ReaderTag(tagSlug, tagTitle, tagTitle, "", DEFAULT)
    }
}