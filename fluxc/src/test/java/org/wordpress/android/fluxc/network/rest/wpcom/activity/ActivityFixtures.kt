package org.wordpress.android.fluxc.network.rest.wpcom.activity

import org.wordpress.android.fluxc.model.activity.RewindStatusModel
import java.util.Date

val ACTIVITY_RESPONSE = ActivityLogRestClient.ActivitiesResponse.ActivityResponse("activity",
        ActivityLogRestClient.ActivitiesResponse.Content("text"),
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
        "activity123",
        false)
val ACTIVITY_RESPONSE_PAGE = ActivityLogRestClient.ActivitiesResponse.Page(listOf(ACTIVITY_RESPONSE))
val RESTORE_RESPONSE = ActivityLogRestClient.RewindStatusResponse.RestoreStatusResponse(rewind_id = "123",
        status = RewindStatusModel.RestoreStatus.Status.RUNNING.value,
        progress = 10,
        message = "running",
        error_code = null,
        reason = "nit")
val REWIND_RESPONSE = ActivityLogRestClient.RewindStatusResponse("reason",
        RewindStatusModel.State.ACTIVE.value,
        Date(),
        RESTORE_RESPONSE)
