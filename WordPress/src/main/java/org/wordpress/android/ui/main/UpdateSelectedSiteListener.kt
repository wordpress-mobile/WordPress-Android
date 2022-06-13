package org.wordpress.android.ui.main

import android.content.Intent

interface UpdateSelectedSiteListener {
    fun onUpdateSelectedSiteResult(resultCode: Int, data: Intent?)
}
