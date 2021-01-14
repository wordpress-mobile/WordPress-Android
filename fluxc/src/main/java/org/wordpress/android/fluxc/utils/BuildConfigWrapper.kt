package org.wordpress.android.fluxc.utils

import dagger.Reusable
import org.wordpress.android.fluxc.BuildConfig
import javax.inject.Inject

@Reusable
class BuildConfigWrapper @Inject constructor() {
    fun isDebug() = BuildConfig.DEBUG
}
