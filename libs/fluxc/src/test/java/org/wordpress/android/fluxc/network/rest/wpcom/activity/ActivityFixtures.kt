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
        "activity123")
val ACTIVITY_RESPONSE_PAGE = ActivityLogRestClient.ActivitiesResponse.Page(listOf(ACTIVITY_RESPONSE))
val REWIND_RESPONSE = ActivityLogRestClient.RewindStatusResponse.Rewind(rewindId = "123",
        status = RewindStatusModel.Rewind.Status.RUNNING.value,
        progress = 10,
        reason = "nit",
        startedAt = Date())
val REWIND_STATUS_RESPONSE = ActivityLogRestClient.RewindStatusResponse(
        state = RewindStatusModel.State.ACTIVE.value,
        reason = "reason",
        last_updated = Date(),
        can_autoconfigure = true,
        credentials = listOf(),
        rewind = REWIND_RESPONSE)
