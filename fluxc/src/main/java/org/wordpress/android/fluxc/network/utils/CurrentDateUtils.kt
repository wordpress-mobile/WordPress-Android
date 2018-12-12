package org.wordpress.android.fluxc.network.utils

import java.util.Date
import javax.inject.Inject

class CurrentDateUtils
@Inject constructor() {
    fun getCurrentDate() = Date()
}
