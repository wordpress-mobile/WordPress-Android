package org.wordpress.android.ui.activitylog

import android.arch.lifecycle.LiveData
import org.wordpress.android.fluxc.model.SiteModel

interface RewindService {
    val rewindProgress: LiveData<Int>
    fun isRewindAvailable(site: SiteModel)
    fun rewind(rewindId: String, site: SiteModel)
    fun attach()
    fun detach()
}
