package org.wordpress.android.fluxc.network.rest.wpcom.activity

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
