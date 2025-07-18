package org.wordpress.android.ui.subscribers

import org.wordpress.android.R
import uniffi.wp_api.IndividualSubscriberStats
import uniffi.wp_api.Subscriber
import uniffi.wp_api.SubscriberCountry
import java.util.Date
import kotlin.random.Random

/**
 * Returns a list of "dummy" [Subscriber]s for use in screenshots promoting this feature
 * (we can't use real data because that would expose emails and other private info)
 */
@Suppress("Unused")
object DummySubscribers {
    private val dummyNames = listOf(
        "Brian Allen",
        "Robert Brown",
        "Lisa Anderson",
        "Michael Chen",
        "Lauren Clark",
        "Jennifer Davis",
        "David Green",
        "Ryan Harris"
    )

    private val dummyEmails = listOf(
        "brian.allen@example.com",
        "robert.brown@example.com",
        "lisa.anderson@example.com",
        "michael.chen@example.com",
        "lauren.clark@example.com",
        "jennifer.davis@example.com",
        "david.green@example.com",
        "ryan.harris@example.com"
    )

    private val dummyDates = listOf(
        Date(1673740800000L), // 2023-01-15
        Date(1675814400000L), // 2023-02-08
        Date(1679443200000L), // 2023-03-22
        Date(1681084800000L), // 2023-04-10
        Date(1683072000000L), // 2023-05-03
        Date(1687046400000L), // 2023-06-18
        Date(1690243200000L), // 2023-07-25
        Date(1691884800000L), // 2023-08-12
        Date(1687046400000L), // 2023-06-18
    )

    private val subscriptionStatuses = listOf(
        "Subscribed",
        "Not subscribed",
        "Not sending"
    )

    private val profileImageDrawables = listOf(
        R.drawable.dummy_profile_1,
        R.drawable.dummy_profile_2,
        R.drawable.dummy_profile_3,
        R.drawable.dummy_profile_4,
        R.drawable.dummy_profile_5,
        R.drawable.dummy_profile_6,
        R.drawable.dummy_profile_7,
        R.drawable.dummy_profile_8
    )

    private val countries = listOf(
        SubscriberCountry("US", "United States"),
        SubscriberCountry("CA", "Canada"),
        SubscriberCountry("UK", "United Kingdom"),
        SubscriberCountry("AU", "Australia"),
        SubscriberCountry("DE", "Germany"),
        SubscriberCountry("FR", "France"),
        SubscriberCountry("JP", "Japan"),
        SubscriberCountry("BR", "Brazil")
    )

    private val websiteUrls = listOf(
        "https://brianallen.example.com",
        "https://robertbrown.example.com",
        "https://lisa-anderson.example.com",
        "https://michaelchen.example.com",
        "https://laurenclark.example.com",
        "https://jenniferdavis.example.com",
        "https://david-green.example.com",
        "https://ryanharris.example.com"
    )

    fun getDummySubscribers(count: Int = 20): List<Subscriber> {
        val subscribers = mutableListOf<Subscriber>()
        repeat(count) { index ->
            val nameIndex = index % dummyNames.size
            val emailIndex = index % dummyEmails.size
            val dateIndex = index % dummyDates.size
            val statusIndex = index % subscriptionStatuses.size
            val imageIndex = index % profileImageDrawables.size
            val countryIndex = index % countries.size
            val urlIndex = index % websiteUrls.size

            val subscriber = Subscriber(
                userId = (index + 1).toLong(),
                subscriptionId = (index + 1001).toULong(),
                displayName = dummyNames[nameIndex],
                emailAddress = dummyEmails[emailIndex],
                isEmailSubscriber = statusIndex == EMAIL_SUBSCRIBER_STATUS_INDEX,
                url = websiteUrls[urlIndex],
                dateSubscribed = dummyDates[dateIndex],
                subscriptionStatus = subscriptionStatuses[statusIndex],
                avatar = "drawable:${profileImageDrawables[imageIndex]}",
                country = countries[countryIndex],
                plans = emptyList()
            )
            subscribers.add(subscriber)
        }

        return subscribers
    }

    @Suppress("Unused", "MagicNumber")
    fun getDummySubscriberStats(): IndividualSubscriberStats {
        val random = Random.nextInt(from = 500, until = 1000)
        return IndividualSubscriberStats(
            emailsSent = random.toULong(),
            uniqueOpens = (random - 200).toULong(),
            uniqueClicks = (random - 243).toULong(),
            blogRegistrationDate = Date(1673740800000L).toString(),
        )
    }

    private const val EMAIL_SUBSCRIBER_STATUS_INDEX = 0
}
