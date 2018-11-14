package org.wordpress.android.fluxc.utils

import java.util.Date
import javax.inject.Inject

class CurrentTimeProvider
@Inject constructor() {
    val currentDate = Date()
}
