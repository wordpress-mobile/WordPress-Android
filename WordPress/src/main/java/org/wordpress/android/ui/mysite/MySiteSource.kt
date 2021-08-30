package org.wordpress.android.ui.mysite

import androidx.lifecycle.LiveData
import kotlinx.coroutines.CoroutineScope
import org.wordpress.android.ui.mysite.MySiteUiState.PartialState

interface MySiteSource<T : PartialState> {
    fun buildSource(coroutineScope: CoroutineScope, siteLocalId: Int): LiveData<T>
    interface SiteIndependentSource<T : PartialState> : MySiteSource<T> {
        fun buildSource(coroutineScope: CoroutineScope): LiveData<T>
        override fun buildSource(
            coroutineScope: CoroutineScope,
            siteLocalId: Int
        ): LiveData<T> = buildSource(coroutineScope)
    }
}
