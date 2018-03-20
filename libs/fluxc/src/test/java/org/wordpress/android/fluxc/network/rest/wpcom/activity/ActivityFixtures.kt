package org.wordpress.android.fluxc.network.rest.wpcom.activity

import org.wordpress.android.fluxc.model.activity.RewindStatusModel
import java.util.Date

val ACTIVITY_RESPONSE = ActivityRestClient.ActivitiesResponse.ActivityResponse("activity",
        ActivityRestClient.ActivitiesResponse.Content("text"),
        "name",
        ActivityRestClient.ActivitiesResponse.Actor("author",
                "John Smith",
                10,
                15,
                ActivityRestClient.ActivitiesResponse.Icon("jpg",
                        "dog.jpg",
                        100,
                        100),
                "admin"),
        "create a blog",
        Date(),
        ActivityRestClient.ActivitiesResponse.Generator(10.3f, 123),
        false,
        10f,
        "gridicon.jpg",
        "OK",
        "activity123",
        false)
val ACTIVITY_RESPONSE_PAGE = ActivityRestClient.ActivitiesResponse.Page(listOf(ACTIVITY_RESPONSE))
val RESTORE_RESPONSE = ActivityRestClient.RewindStatusResponse.RestoreStatusResponse(rewind_id = "123",
        status = RewindStatusModel.RestoreStatus.Status.RUNNING.value,
        progress = 10,
        message = "running",
        error_code = null,
        reason = "nit")
val REWIND_RESPONSE = ActivityRestClient.RewindStatusResponse("reason",
        RewindStatusModel.State.ACTIVE.value,
        Date(),
        RESTORE_RESPONSE)
