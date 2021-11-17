package org.wordpress.android.ui.mysite

import androidx.lifecycle.LiveData
import kotlinx.coroutines.CoroutineScope
import org.wordpress.android.ui.mysite.MySiteUiState.PartialState

interface MySiteSource<T : PartialState> {
    fun build(coroutineScope: CoroutineScope, siteLocalId: Int): LiveData<T>
    interface SiteIndependentSource<T : PartialState> : MySiteSource<T> {
        fun build(coroutineScope: CoroutineScope): LiveData<T>
        override fun build(
            coroutineScope: CoroutineScope,
            siteLocalId: Int
        ): LiveData<T> = build(coroutineScope)
    }
}
