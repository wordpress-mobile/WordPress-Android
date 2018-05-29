package org.wordpress.android.ui.pages

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import javax.inject.Inject

class PagesViewModel
@Inject constructor() : ViewModel() {
    private val mutableSearchExpanded: MutableLiveData<Boolean> = MutableLiveData()
    val searchExpanded: LiveData<Boolean> = mutableSearchExpanded
    fun onSearchTextSubmit(query: String?): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    fun onSearchTextChange(newText: String?): Boolean {
        return true
    }

    fun searchOpen() {
        mutableSearchExpanded.postValue(true)
    }

    fun searchClose() {
        mutableSearchExpanded.postValue(false)
    }
}
