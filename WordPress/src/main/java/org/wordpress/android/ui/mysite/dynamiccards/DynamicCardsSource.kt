package org.wordpress.android.ui.mysite.dynamiccards

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineScope
import org.wordpress.android.ui.mysite.MySiteSource.SiteIndependentSource
import org.wordpress.android.ui.mysite.MySiteUiState.PartialState.DynamicCards
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.util.merge
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DynamicCardsSource
@Inject constructor(
    private val appPrefsWrapper: AppPrefsWrapper
) : SiteIndependentSource<DynamicCards> {
    private val pinnedItem = MutableLiveData<DynamicCardType>()
    private val hiddenItems = MutableLiveData<Set<DynamicCardType>>()

    init {
        hiddenItems.value = setOf()
        pinnedItem.value = appPrefsWrapper.getPinnedDynamicCardType()
    }

    override fun buildSource(coroutineScope: CoroutineScope): LiveData<DynamicCards> {
        return merge(pinnedItem, hiddenItems) { pinnedItem, hiddenItems ->
            val filteredCards = DynamicCardType.values()
                    .filter { hiddenItems.isNullOrEmpty() || !hiddenItems.contains(it) }
            val indexOfPinnedItem = if (pinnedItem != null) {
                filteredCards.indexOf(pinnedItem)
            } else {
                -1
            }
            val updatedCards = if (indexOfPinnedItem > -1 && pinnedItem != null) {
                val pinnedCards = filteredCards.toMutableList()
                pinnedCards.removeAt(indexOfPinnedItem)
                pinnedCards.add(0, pinnedItem)
                pinnedCards
            } else {
                filteredCards
            }
            DynamicCards(
                    pinnedItem,
                    updatedCards
            )
        }
    }

    fun pinItem(dynamicCardType: DynamicCardType) {
        val currentlyPinnedItem = appPrefsWrapper.getPinnedDynamicCardType()
        if (currentlyPinnedItem == dynamicCardType) {
            appPrefsWrapper.unpinDynamicCardType()
            pinnedItem.value = null
        } else {
            appPrefsWrapper.pinDynamicCardType(dynamicCardType)
            pinnedItem.value = dynamicCardType
        }
    }

    fun hideItem(dynamicCardType: DynamicCardType) {
        val hiddenSetOfCards = hiddenItems.value?.toMutableSet() ?: mutableSetOf()
        hiddenSetOfCards.add(dynamicCardType)
        hiddenItems.value = hiddenSetOfCards
    }
}
