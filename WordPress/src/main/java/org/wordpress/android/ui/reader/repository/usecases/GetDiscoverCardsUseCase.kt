package org.wordpress.android.ui.reader.repository.usecases

import dagger.Reusable
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import org.wordpress.android.datasets.ReaderDiscoverCardsTable
import org.wordpress.android.datasets.ReaderPostTable
import org.wordpress.android.models.discover.ReaderDiscoverCard
import org.wordpress.android.models.discover.ReaderDiscoverCard.InterestsYouMayLikeCard
import org.wordpress.android.models.discover.ReaderDiscoverCard.ReaderPostCard
import org.wordpress.android.models.discover.ReaderDiscoverCards
import org.wordpress.android.modules.IO_THREAD
import org.wordpress.android.ui.reader.ReaderConstants
import javax.inject.Inject
import javax.inject.Named

@Reusable
class GetDiscoverCardsUseCase @Inject constructor(
    private val parseDiscoverCardsJsonUseCase: ParseDiscoverCardsJsonUseCase,
    @Named(IO_THREAD) private val ioDispatcher: CoroutineDispatcher
) : ReaderRepositoryDispatchingUseCase(ioDispatcher) {
    suspend fun get(): ReaderDiscoverCards =
            withContext(coroutineContext) {
                val cardJsonList = ReaderDiscoverCardsTable.loadDiscoverCardsJsons()
                val cards: ArrayList<ReaderDiscoverCard> = arrayListOf()

                if (cardJsonList.isNotEmpty()) {
                    val jsonObjects = parseDiscoverCardsJsonUseCase.convertListOfJsonArraysIntoSingleJsonArray(cardJsonList)

                    for (i in 0 until jsonObjects.length()) {
                        val cardJson = jsonObjects.getJSONObject(i)
                        when (cardJson.getString(ReaderConstants.JSON_CARD_TYPE)) {
                            ReaderConstants.JSON_CARD_INTERESTS_YOU_MAY_LIKE -> {
                                val interests = parseDiscoverCardsJsonUseCase.parseInterestCard(cardJson)
                                cards.add(InterestsYouMayLikeCard(interests))
                            }
                            ReaderConstants.JSON_CARD_POST -> {
                                // TODO we might want to load the data in batch
                                val (blogId, postId) = parseDiscoverCardsJsonUseCase.parseSimplifiedPostCard(cardJson)
                                val post = ReaderPostTable.getBlogPost(blogId, postId, false)
                                cards.add(ReaderPostCard(post))
                            }
                        }
                    }
                }
                return@withContext ReaderDiscoverCards(cards)
            }
}
