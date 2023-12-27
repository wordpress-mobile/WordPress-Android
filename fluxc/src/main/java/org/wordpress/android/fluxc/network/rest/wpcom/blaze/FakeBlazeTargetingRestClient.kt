package org.wordpress.android.fluxc.network.rest.wpcom.blaze

import kotlinx.coroutines.delay
import org.wordpress.android.fluxc.Payload
import org.wordpress.android.fluxc.model.blaze.BlazeTargetingLocation
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequest.WPComGsonNetworkError
import org.wordpress.android.fluxc.persistence.blaze.BlazeTargetingDeviceEntity
import org.wordpress.android.fluxc.persistence.blaze.BlazeTargetingLanguageEntity
import org.wordpress.android.fluxc.persistence.blaze.BlazeTargetingTopicEntity
import javax.inject.Inject

private const val FAKE_DELAY = 500L

class FakeBlazeTargetingRestClient @Inject constructor() {
    suspend fun fetchBlazeLocations(
        query: String
    ): BlazeTargetingPayload<List<BlazeTargetingLocation>> {
        if (query.length < 3) {
            return BlazeTargetingPayload(emptyList())
        }

        delay(FAKE_DELAY)

        return BlazeTargetingPayload(
            fakeLocations
                .filter { it.name.contains(query, ignoreCase = true) }
        )
    }

    suspend fun fetchBlazeTopics(): BlazeTargetingPayload<List<BlazeTargetingTopicEntity>> {
        delay(FAKE_DELAY)

        return BlazeTargetingPayload(fakeTopics)
    }

    suspend fun fetchBlazeLanguages(): BlazeTargetingPayload<List<BlazeTargetingLanguageEntity>> {
        delay(FAKE_DELAY)

        return BlazeTargetingPayload(fakeLanguages)
    }

    suspend fun fetchBlazeDevices(): BlazeTargetingPayload<List<BlazeTargetingDeviceEntity>> {
        delay(FAKE_DELAY)

        return BlazeTargetingPayload(fakeDevices)
    }
}

data class BlazeTargetingPayload<T>(
    val data: T
) : Payload<WPComGsonNetworkError>()

private val fakeLocations
    get() = listOf(
        BlazeTargetingLocation(
            id = 1,
            name = "United States",
            type = "country",
            parent = null
        ),
        BlazeTargetingLocation(
            id = 2,
            name = "California",
            type = "state",
            parent = BlazeTargetingLocation(
                id = 1,
                name = "United States",
                type = "country",
                parent = null
            )
        ),
        BlazeTargetingLocation(
            id = 3,
            name = "San Francisco",
            type = "city",
            parent = BlazeTargetingLocation(
                id = 2,
                name = "California",
                type = "state",
                parent = BlazeTargetingLocation(
                    id = 1,
                    name = "United States",
                    type = "country",
                    parent = null
                )
            )
        ),
        BlazeTargetingLocation(
            id = 4,
            name = "Los Angeles",
            type = "city",
            parent = BlazeTargetingLocation(
                id = 2,
                name = "California",
                type = "state",
                parent = BlazeTargetingLocation(
                    id = 1,
                    name = "United States",
                    type = "country",
                    parent = null
                )
            )
        ),
        BlazeTargetingLocation(
            id = 5,
            name = "New York",
            type = "state",
            parent = BlazeTargetingLocation(
                id = 1,
                name = "United States",
                type = "country",
                parent = null
            )
        ),
        BlazeTargetingLocation(
            id = 6,
            name = "New York City",
            type = "city",
            parent = BlazeTargetingLocation(
                id = 5,
                name = "New York",
                type = "state",
                parent = BlazeTargetingLocation(
                    id = 1,
                    name = "United States",
                    type = "country",
                    parent = null
                )
            )
        ),
        BlazeTargetingLocation(
            id = 7,
            name = "Chicago",
            type = "city",
            parent = BlazeTargetingLocation(
                id = 5,
                name = "New York",
                type = "state",
                parent = BlazeTargetingLocation(
                    id = 1,
                    name = "United States",
                    type = "country",
                    parent = null
                )
            )
        ),
        BlazeTargetingLocation(
            id = 8,
            name = "Texas",
            type = "state",
            parent = BlazeTargetingLocation(
                id = 1,
                name = "United States",
                type = "country",
                parent = null
            )
        ),
        BlazeTargetingLocation(
            id = 9,
            name = "Houston",
            type = "city",
            parent = BlazeTargetingLocation(
                id = 8,
                name = "Texas",
                type = "state",
                parent = BlazeTargetingLocation(
                    id = 1,
                    name = "United States",
                    type = "country",
                    parent = null
                )
            )
        ),
        BlazeTargetingLocation(
            id = 10,
            name = "Dallas",
            type = "city",
            parent = BlazeTargetingLocation(
                id = 8,
                name = "Texas",
                type = "state",
                parent = BlazeTargetingLocation(
                    id = 1,
                    name = "United States",
                    type = "country",
                    parent = null
                )
            )
        )
    )

private val fakeTopics
    get() = listOf(
        BlazeTargetingTopicEntity(
            id = "IAB1",
            description = "Arts & Entertainment"
        ),
        BlazeTargetingTopicEntity(
            id = "IAB2",
            description = "Automotive"
        ),
        BlazeTargetingTopicEntity(
            id = "IAB3",
            description = "Business"
        ),
        BlazeTargetingTopicEntity(
            id = "IAB4",
            description = "Careers"
        ),
        BlazeTargetingTopicEntity(
            id = "IAB5",
            description = "Education"
        ),
        BlazeTargetingTopicEntity(
            id = "IAB6",
            description = "Family & Parenting"
        ),
        BlazeTargetingTopicEntity(
            id = "IAB7",
            description = "Health & Fitness"
        )
    )

private val fakeLanguages
    get() = listOf(
        BlazeTargetingLanguageEntity(
            id = "en",
            name = "English"
        ),
        BlazeTargetingLanguageEntity(
            id = "es",
            name = "Spanish"
        ),
        BlazeTargetingLanguageEntity(
            id = "fr",
            name = "French"
        ),
        BlazeTargetingLanguageEntity(
            id = "de",
            name = "German"
        ),
        BlazeTargetingLanguageEntity(
            id = "it",
            name = "Italian"
        ),
        BlazeTargetingLanguageEntity(
            id = "ja",
            name = "Japanese"
        ),
        BlazeTargetingLanguageEntity(
            id = "ko",
            name = "Korean"
        ),
        BlazeTargetingLanguageEntity(
            id = "pt",
            name = "Portuguese"
        ),
        BlazeTargetingLanguageEntity(
            id = "ru",
            name = "Russian"
        ),
        BlazeTargetingLanguageEntity(
            id = "zh",
            name = "Chinese"
        )
    )

private val fakeDevices
    get() = listOf(
        BlazeTargetingDeviceEntity(
            id = "mobile",
            name = "Mobile"
        ),
        BlazeTargetingDeviceEntity(
            id = "desktop",
            name = "Desktop"
        )
    )
