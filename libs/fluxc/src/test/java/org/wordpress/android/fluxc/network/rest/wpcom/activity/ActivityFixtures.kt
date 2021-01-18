package org.wordpress.android.fluxc.network.rest.wpcom.activity

import org.wordpress.android.fluxc.model.activity.RewindStatusModel
import org.wordpress.android.fluxc.tools.FormattableContent
import java.util.Date

val ACTIVITY_RESPONSE = ActivityLogRestClient.ActivitiesResponse.ActivityResponse("activity",
        FormattableContent(text = "text"),
        "name",
        ActivityLogRestClient.ActivitiesResponse.Actor("author",
                "John Smith",
                10,
                15,
                ActivityLogRestClient.ActivitiesResponse.Icon("jpg",
                        "dog.jpg",
                        100,
                        100),
                "admin"),
        "create a blog",
        Date(),
        ActivityLogRestClient.ActivitiesResponse.Generator(10.3f, 123),
        false,
        "10.0",
        "gridicon.jpg",
        "OK",
        "activity123")
val ACTIVITY_RESPONSE_PAGE = ActivityLogRestClient.ActivitiesResponse.Page(listOf(ACTIVITY_RESPONSE))
val REWIND_RESPONSE = ActivityLogRestClient.RewindStatusResponse.Rewind(rewind_id = "123",
        status = RewindStatusModel.Rewind.Status.RUNNING.value,
        progress = null,
        reason = null,
        site_id = null,
        restore_id = 5,
        message = null,
        currentEntry = null)
val REWIND_STATUS_RESPONSE = ActivityLogRestClient.RewindStatusResponse(
        state = RewindStatusModel.State.ACTIVE.value,
        reason = "reason",
        last_updated = Date(),
        can_autoconfigure = true,
        credentials = listOf(),
        rewind = REWIND_RESPONSE,
        message = "Starting",
        currentEntry = null)
val BACKUP_DOWNLOAD_STATUS_RESPONSE = ActivityLogRestClient.BackupDownloadStatusResponse(
        downloadId = 0,
        rewindId = "rewindId",
        backupPoint = Date(),
        startedAt = Date(),
        progress = 35,
        downloadCount = null,
        validUntil = null,
        url = null)
