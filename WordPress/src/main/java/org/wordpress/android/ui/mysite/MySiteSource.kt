package org.wordpress.android.ui.mysite

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineScope
import org.wordpress.android.ui.mysite.MySiteUiState.PartialState

interface MySiteSource<T : PartialState> {
    fun build(coroutineScope: CoroutineScope, siteLocalId: Int): LiveData<T>

    interface MySiteRefreshSource<T : PartialState> : MySiteSource<T> {
        val refresh: MutableLiveData<Boolean>

        fun refresh() {
            refresh.postValue(true)
        }

        fun isRefreshing() = refresh.value

        fun getState(value: T): T {
            refresh.value = false
            return value
        }

        fun MediatorLiveData<T>.postState(value: T) {
            refresh.postValue(false)
            this@postState.postValue(value)
        }

        fun onRefreshed() {
            refresh.value = false
        }
    }

    interface SiteIndependentSource<T : PartialState> : MySiteRefreshSource<T> {
        fun build(coroutineScope: CoroutineScope): LiveData<T>
        override fun build(
            coroutineScope: CoroutineScope,
            siteLocalId: Int
        ): LiveData<T> = build(coroutineScope)
    }
}
