package org.wordpress.android.ui.sitecreation.progress

import org.wordpress.android.ui.sitecreation.misc.CreateSiteState

interface ProgressScreenListener {
    fun onProgressScreenDismissed(state: CreateSiteState)
    fun onSiteCreationCompleted(state: CreateSiteState)
}
