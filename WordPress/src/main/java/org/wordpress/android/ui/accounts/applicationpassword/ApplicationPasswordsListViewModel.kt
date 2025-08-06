package org.wordpress.android.ui.accounts.applicationpassword

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.Calendar
import javax.inject.Inject

@HiltViewModel
class ApplicationPasswordsListViewModel @Inject constructor() : ViewModel() {
    private val _applicationPasswords = MutableLiveData<List<ApplicationPasswordWithViewContext>>()
    val applicationPasswords: LiveData<List<ApplicationPasswordWithViewContext>> = _applicationPasswords

    fun loadApplicationPasswords() {
        loadDummyApplicationPasswords()
    }
    fun loadDummyApplicationPasswords() {
        val dummyPasswords = listOf(
            ApplicationPasswordWithViewContext(
                uuid = "uuid-1",
                name = "WordPress Mobile App",
                lastUsed = Calendar.getInstance().apply {
                    add(Calendar.DAY_OF_MONTH, -1)
                }.time
            ),
            ApplicationPasswordWithViewContext(
                uuid = "uuid-2",
                name = "Jetpack Mobile App",
                lastUsed = Calendar.getInstance().apply {
                    add(Calendar.DAY_OF_MONTH, -3)
                }.time
            ),
            ApplicationPasswordWithViewContext(
                uuid = "uuid-3",
                name = "Desktop Publisher",
                lastUsed = Calendar.getInstance().apply {
                    add(Calendar.WEEK_OF_YEAR, -2)
                }.time
            ),
            ApplicationPasswordWithViewContext(
                uuid = "uuid-4",
                name = "Third Party Integration",
                lastUsed = null
            ),
            ApplicationPasswordWithViewContext(
                uuid = "uuid-5",
                name = "Legacy API Client",
                lastUsed = Calendar.getInstance().apply {
                    add(Calendar.MONTH, -6)
                }.time
            )
        )
        _applicationPasswords.value = dummyPasswords
    }
}
