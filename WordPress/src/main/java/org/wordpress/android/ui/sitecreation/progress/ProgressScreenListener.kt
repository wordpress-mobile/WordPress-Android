package org.wordpress.android.ui.sitecreation.progress

import org.wordpress.android.ui.sitecreation.SiteCreationResult

interface ProgressScreenListener {
    fun onProgressStopped(result: SiteCreationResult)
    fun onProgressCompleted(result: SiteCreationResult)
}
