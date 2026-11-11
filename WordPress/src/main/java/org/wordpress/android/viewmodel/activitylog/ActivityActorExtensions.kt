package org.wordpress.android.viewmodel.activitylog

import org.wordpress.android.R
import org.wordpress.android.fluxc.model.activity.ActivityLogModel
import org.wordpress.android.viewmodel.ResourceProvider

fun ActivityLogModel.ActivityActor.formattedMcpMetadata(
    resourceProvider: ResourceProvider
): String? {
    val client = mcpClient
    return if (isMCPAgent && !client.isNullOrEmpty()) {
        resourceProvider.getString(
            R.string.activity_log_mcp_agent_label, client
        )
    } else {
        null
    }
}
