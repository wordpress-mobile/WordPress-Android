package org.wordpress.android.ui.main.jetpack.migration

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class JetpackMigrationViewModel @Inject constructor(
) : ViewModel() {
    fun onAccountInfoLoaded() {
        // TODO update UI when account info is loaded to db
    }

    fun onSiteListLoaded() {
        // TODO update UI when site list is loaded to db
    }
}
