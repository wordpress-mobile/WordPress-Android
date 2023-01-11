package org.wordpress.android.ui.reader.repository.usecases

import dagger.Reusable
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import org.wordpress.android.datasets.ReaderBlogTableWrapper
import org.wordpress.android.datasets.ReaderDiscoverCardsTableWrapper
import org.wordpress.android.datasets.wrappers.ReaderPostTableWrapper
import org.wordpress.android.fluxc.utils.AppLogWrapper
import org.wordpress.android.models.discover.ReaderDiscoverCard
import org.wordpress.android.models.discover.ReaderDiscoverCard.InterestsYouMayLikeCard
import org.wordpress.android.models.discover.ReaderDiscoverCard.ReaderPostCard
import org.wordpress.android.models.discover.ReaderDiscoverCard.ReaderRecommendedBlogsCard
import org.wordpress.android.models.discover.ReaderDiscoverCard.WelcomeBannerCard
import org.wordpress.android.models.discover.ReaderDiscoverCards
import org.wordpress.android.modules.IO_THREAD
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.ui.reader.ReaderConstants
import org.wordpress.android.util.AppLog.T.READER
import javax.inject.Inject
import javax.inject.Named

@Reusable
class GetDiscoverCardsUseCase @Inject constructor(
    private val parseDiscoverCardsJsonUseCase: ParseDiscoverCardsJsonUseCase,
    private val readerDiscoverCardsTableWrapper: ReaderDiscoverCardsTableWrapper,
    private val readerPostTableWrapper: ReaderPostTableWrapper,
    private val readerBlogTableWrapper: ReaderBlogTableWrapper,
    private val appLogWrapper: AppLogWrapper,
    private val appPrefsWrapper: AppPrefsWrapper,
    @Named(IO_THREAD) private val ioDispatcher: CoroutineDispatcher
) {
    suspend fun get(): ReaderDiscoverCards = withContext(ioDispatcher) {
        val cardJsonList = readerDiscoverCardsTableWrapper.loadDiscoverCardsJsons()
        val cards: ArrayList<ReaderDiscoverCard> = arrayListOf()

        if (cardJsonList.isNotEmpty()) {
            val jsonObjects = parseDiscoverCardsJsonUseCase.convertListOfJsonArraysIntoSingleJsonArray(
                cardJsonList
            )

            forLoop@ for (i in 0 until jsonObjects.length()) {
                val cardJson = jsonObjects.getJSONObject(i)
                when (cardJson.getString(ReaderConstants.JSON_CARD_TYPE)) {
                    ReaderConstants.JSON_CARD_INTERESTS_YOU_MAY_LIKE -> {
                        val interests = parseDiscoverCardsJsonUseCase.parseInterestCard(cardJson)
                        cards.add(InterestsYouMayLikeCard(interests))
                    }
                    ReaderConstants.JSON_CARD_POST -> {
                        // TODO we might want to load the data in batch
                        val (blogId, postId) = parseDiscoverCardsJsonUseCase.parseSimplifiedPostCard(cardJson)
                        val post = readerPostTableWrapper.getBlogPost(blogId, postId, false)
                        if (post != null) {
                            cards.add(ReaderPostCard(post))
                        } else {
                            appLogWrapper.d(READER, "Post from /cards json not found in ReaderDatabase")
                            continue@forLoop
                        }
                    }
                    ReaderConstants.JSON_CARD_RECOMMENDED_BLOGS -> {
                        cardJson?.let {
                            val recommendedBlogs = parseDiscoverCardsJsonUseCase.parseSimplifiedRecommendedBlogsCard(it)
                                .mapNotNull { (blogId, feedId) ->
                                    readerBlogTableWrapper.getReaderBlog(blogId, feedId)
                                }
                            cards.add(ReaderRecommendedBlogsCard(recommendedBlogs))
                        }
                    }
                }
            }

            if (cards.isNotEmpty() && !appPrefsWrapper.readerDiscoverWelcomeBannerShown) {
                cards.add(0, WelcomeBannerCard)
            }
        }
        return@withContext ReaderDiscoverCards(cards)
    }
}
