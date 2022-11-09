package org.wordpress.android.userflags

import org.wordpress.android.fluxc.model.QuickStartStatusModel
import org.wordpress.android.fluxc.model.QuickStartTaskModel

data class UserFlagsData(
    val flags: Map<String, Any?>,
    val quickStartTaskList: List<QuickStartTaskModel>,
    val quickStartStatusList: List<QuickStartStatusModel>
)
