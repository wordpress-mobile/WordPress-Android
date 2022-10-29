package org.wordpress.android.ui.main.jetpack.migration

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class JetpackMigrationViewModel @Inject constructor(
) : ViewModel() {
    fun onAccountInfoLoaded() {
        // TODO add logic to handle account info loaded in db
    }

    fun onSiteListLoaded() {
        // TODO add logic to handle site list loaded in db
    }
}
