package org.wordpress.android.datasets

import dagger.Reusable
import org.wordpress.android.ui.reader.discover.DiscoverSortingType
import javax.inject.Inject

@Reusable
class ReaderDiscoverCardsTableWrapper @Inject constructor() {
    fun loadDiscoverCardsJsons(sortingType: DiscoverSortingType) =
            ReaderDiscoverCardsTable.loadDiscoverCardsJsons(sortingType)
}
