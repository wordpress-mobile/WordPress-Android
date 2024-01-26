package org.wordpress.android.fluxc.network.rest.wpcom.blaze

import kotlinx.coroutines.delay
import org.wordpress.android.fluxc.Payload
import org.wordpress.android.fluxc.model.blaze.BlazeTargetingLocation
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequest.WPComGsonNetworkError
import org.wordpress.android.fluxc.persistence.blaze.BlazeCampaignsDao.BlazeAdSuggestionEntity
import org.wordpress.android.fluxc.persistence.blaze.BlazeTargetingDeviceEntity
import org.wordpress.android.fluxc.persistence.blaze.BlazeTargetingLanguageEntity
import org.wordpress.android.fluxc.persistence.blaze.BlazeTargetingTopicEntity
import javax.inject.Inject

private const val FAKE_DELAY = 500L

class FakeBlazeTargetingRestClient @Inject constructor() {
    @Suppress("MagicNumber", "UNUSED_PARAMETER")
    suspend fun fetchBlazeLocations(
        query: String,
        locale: String
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

    suspend fun fetchBlazeTopics(locale: String): BlazeTargetingPayload<List<BlazeTargetingTopicEntity>> {
        delay(FAKE_DELAY)

        return BlazeTargetingPayload(generateFakeTopics(locale))
    }

    suspend fun fetchBlazeLanguages(locale: String): BlazeTargetingPayload<List<BlazeTargetingLanguageEntity>> {
        delay(FAKE_DELAY)

        return BlazeTargetingPayload(generateFakeLanguages(locale))
    }

    suspend fun fetchBlazeDevices(locale: String): BlazeTargetingPayload<List<BlazeTargetingDeviceEntity>> {
        delay(FAKE_DELAY)

        return BlazeTargetingPayload(generateFakeDevices(locale))
    }

    suspend fun fetchBlazeAdSuggestions(
        siteId: Long,
        productId: Long
    ): BlazeTargetingPayload<List<BlazeAdSuggestionEntity>> {
        delay(FAKE_DELAY)

        return BlazeTargetingPayload(generateFakeAdSuggestions(siteId, productId))
    }

    @Suppress("MagicNumber")
    private fun generateFakeAdSuggestions(
        siteId: Long,
        productId: Long
    ): List<BlazeAdSuggestionEntity> {
        val adSuggestions = mutableListOf<BlazeAdSuggestionEntity>()

        for (i in 1..3) {
            adSuggestions.add(
                BlazeAdSuggestionEntity(
                    id = i.toString(),
                    siteId = siteId,
                    productId = productId,
                    tagLine = "Suggested tag line $i",
                    description = "Suggested description $i"
                )
            )
        }

        return adSuggestions
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

private fun generateFakeTopics(locale: String) = listOf(
    BlazeTargetingTopicEntity(
        id = "IAB1",
        description = "Arts & Entertainment",
        locale = locale
    ),
    BlazeTargetingTopicEntity(
        id = "IAB2",
        description = "Automotive",
        locale = locale
    ),
    BlazeTargetingTopicEntity(
        id = "IAB3",
        description = "Business",
        locale = locale
    ),
    BlazeTargetingTopicEntity(
        id = "IAB4",
        description = "Careers",
        locale = locale
    ),
    BlazeTargetingTopicEntity(
        id = "IAB5",
        description = "Education",
        locale = locale
    ),
    BlazeTargetingTopicEntity(
        id = "IAB6",
        description = "Family & Parenting",
        locale = locale
    ),
    BlazeTargetingTopicEntity(
        id = "IAB7",
        description = "Health & Fitness",
        locale = locale
    )
)

private fun generateFakeLanguages(locale: String) = listOf(
    BlazeTargetingLanguageEntity(
        id = "en",
        name = "English",
        locale = locale
    ),
    BlazeTargetingLanguageEntity(
        id = "es",
        name = "Spanish",
        locale = locale
    ),
    BlazeTargetingLanguageEntity(
        id = "fr",
        name = "French",
        locale = locale
    ),
    BlazeTargetingLanguageEntity(
        id = "de",
        name = "German",
        locale = locale
    ),
    BlazeTargetingLanguageEntity(
        id = "it",
        name = "Italian",
        locale = locale
    ),
    BlazeTargetingLanguageEntity(
        id = "ja",
        name = "Japanese",
        locale = locale
    ),
    BlazeTargetingLanguageEntity(
        id = "ko",
        name = "Korean",
        locale = locale
    ),
    BlazeTargetingLanguageEntity(
        id = "pt",
        name = "Portuguese",
        locale = locale
    ),
    BlazeTargetingLanguageEntity(
        id = "ru",
        name = "Russian",
        locale = locale
    ),
    BlazeTargetingLanguageEntity(
        id = "zh",
        name = "Chinese",
        locale = locale
    )
)

private fun generateFakeDevices(locale: String) = listOf(
    BlazeTargetingDeviceEntity(
        id = "mobile",
        name = "Mobile",
        locale = locale
    ),
    BlazeTargetingDeviceEntity(
        id = "desktop",
        name = "Desktop",
        locale = locale
    )
)
