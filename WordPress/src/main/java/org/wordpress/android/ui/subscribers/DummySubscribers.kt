package org.wordpress.android.ui.subscribers

import uniffi.wp_api.IndividualSubscriberStats
import uniffi.wp_api.Subscriber
import uniffi.wp_api.SubscriberCountry
import java.util.Date

/**
 * Returns a list of "dummy" [Subscriber]s for use in screenshots promoting this feature
 * (we can't use real data because that would expose emails and other private info)
 */
@Suppress("Unused")
object DummySubscribers {
    private val dummyNames = listOf(
        "Brian Allen", "Lisa Anderson", "Robert Brown", "Michael Chen", "Lauren Clark",
        "Jennifer Davis", "Rachel Green", "Ryan Harris", "Matthew Jackson", "Sarah Johnson",
        "Melissa King", "Christopher Lee", "Kevin Lewis", "Jessica Martinez", "Amanda Miller",
        "Thomas Moore", "Nicole Robinson", "David Rodriguez", "Daniel Taylor", "Emma Thompson",
        "Steven Walker", "Ashley White", "James Wilson", "Maria Garcia", "Stephanie Young"
    )

    private val dummyEmails = listOf(
        "brian.allen@example.com", "lisa.anderson@example.com", "robert.brown@example.com",
        "michael.chen@example.com", "lauren.clark@example.com", "jennifer.davis@example.com",
        "rachel.green@example.com", "ryan.harris@example.com", "matthew.jackson@example.com",
        "sarah.johnson@example.com", "melissa.king@example.com", "christopher.lee@example.com",
        "kevin.lewis@example.com", "jessica.martinez@example.com", "amanda.miller@example.com",
        "thomas.moore@example.com", "nicole.robinson@example.com", "david.rodriguez@example.com",
        "daniel.taylor@example.com", "emma.thompson@example.com", "steven.walker@example.com",
        "ashley.white@example.com", "james.wilson@example.com", "maria.garcia@example.com",
        "stephanie.young@example.com"
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
        Date(1694044800000L), // 2023-09-07
        Date(1698624000000L), // 2023-10-30
        Date(1699833600000L), // 2023-11-14
        Date(1701734400000L), // 2023-12-05
        Date(1705708800000L), // 2024-01-20
        Date(1709164800000L), // 2024-02-28
        Date(1710547200000L), // 2024-03-16
        Date(1712620800000L), // 2024-04-09
        Date(1716422400000L), // 2024-05-23
        Date(1718150400000L), // 2024-06-11
        Date(1720051200000L), // 2024-07-04
        Date(1724025600000L), // 2024-08-19
        Date(1726185600000L), // 2024-09-13
        Date(1729987200000L), // 2024-10-27
        Date(1731369600000L), // 2024-11-08
        Date(1733097600000L), // 2024-12-02
        Date(1736812800000L)  // 2025-01-14
    )

    private val subscriptionStatuses = listOf(
        "Subscribed", "Not subscribed", "Not sending"
    )

    private val profileImageUrls = listOf(
        "https://randomuser.me/api/portraits/men/1.jpg",
        "https://randomuser.me/api/portraits/women/2.jpg",
        "https://randomuser.me/api/portraits/men/3.jpg",
        "https://randomuser.me/api/portraits/women/4.jpg",
        "https://randomuser.me/api/portraits/women/5.jpg",
        "https://randomuser.me/api/portraits/men/6.jpg",
        "https://randomuser.me/api/portraits/women/7.jpg",
        "https://randomuser.me/api/portraits/men/8.jpg",
        "https://randomuser.me/api/portraits/men/9.jpg",
        "https://randomuser.me/api/portraits/women/10.jpg",
        "https://randomuser.me/api/portraits/women/11.jpg",
        "https://randomuser.me/api/portraits/men/12.jpg",
        "https://randomuser.me/api/portraits/men/13.jpg",
        "https://randomuser.me/api/portraits/women/14.jpg",
        "https://randomuser.me/api/portraits/women/15.jpg",
        "https://randomuser.me/api/portraits/men/16.jpg",
        "https://randomuser.me/api/portraits/women/17.jpg",
        "https://randomuser.me/api/portraits/men/18.jpg",
        "https://randomuser.me/api/portraits/women/19.jpg",
        "https://randomuser.me/api/portraits/men/20.jpg",
        "https://randomuser.me/api/portraits/men/21.jpg",
        "https://randomuser.me/api/portraits/women/22.jpg",
        "https://randomuser.me/api/portraits/men/23.jpg",
        "https://randomuser.me/api/portraits/women/24.jpg",
        "https://randomuser.me/api/portraits/women/25.jpg"
    )

    private val countries = listOf(
        SubscriberCountry("US", "United States"),
        SubscriberCountry("CA", "Canada"),
        SubscriberCountry("UK", "United Kingdom"),
        SubscriberCountry("AU", "Australia"),
        SubscriberCountry("DE", "Germany"),
        SubscriberCountry("FR", "France"),
        SubscriberCountry("JP", "Japan"),
        SubscriberCountry("BR", "Brazil"),
        SubscriberCountry("IN", "India"),
        SubscriberCountry("IT", "Italy"),
        SubscriberCountry("ES", "Spain"),
        SubscriberCountry("MX", "Mexico"),
        SubscriberCountry("NL", "Netherlands"),
        SubscriberCountry("SE", "Sweden"),
        SubscriberCountry("NO", "Norway"),
        SubscriberCountry("KR", "South Korea"),
        SubscriberCountry("CN", "China"),
        SubscriberCountry("RU", "Russia"),
        SubscriberCountry("ZA", "South Africa"),
        SubscriberCountry("AR", "Argentina"),
        SubscriberCountry("CL", "Chile"),
        SubscriberCountry("PL", "Poland"),
        SubscriberCountry("PT", "Portugal"),
        SubscriberCountry("GR", "Greece"),
        SubscriberCountry("TR", "Turkey")
    )

    private val websiteUrls = listOf(
        "https://brianallen.example.com", "https://lisa-anderson.example.com", "https://robertbrown.example.com",
        "https://michaelchen.example.com", "https://laurenclark.example.com", "https://jenniferdavis.example.com",
        "https://rachel-green.example.com", "https://ryanharris.example.com", "https://matthewjackson.example.com",
        "https://sarahjohnson.example.com", "https://melissaking.example.com", "https://christopherlee.example.com",
        "https://kevinlewis.example.com", "https://jessicamartinez.example.com", "https://amandamiller.example.com",
        "https://thomasmoore.example.com", "https://nicolerobinson.example.com", "https://davidrodriguez.example.com",
        "https://danieltaylor.example.com", "https://emmathompson.example.com", "https://stevenwalker.example.com",
        "https://ashleywhite.example.com", "https://jameswilson.example.com", "https://mariagarcia.example.com",
        "https://stephanieyoung.example.com"
    )

    fun getDummySubscribers(count: Int = 25): List<Subscriber> {
        val subscribers = mutableListOf<Subscriber>()
        repeat(count) { index ->
            val nameIndex = index % dummyNames.size
            val emailIndex = index % dummyEmails.size
            val dateIndex = index % dummyDates.size
            val statusIndex = index % subscriptionStatuses.size
            val imageIndex = index % profileImageUrls.size
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
                avatar = profileImageUrls[imageIndex],
                country = countries[countryIndex],
                plans = emptyList()
            )
            subscribers.add(subscriber)
        }
        return subscribers
    }

    @Suppress("Unused", "MagicNumber")
    fun getDummySubscriberStats(): IndividualSubscriberStats {
        return IndividualSubscriberStats(
            emailsSent = 1201u,
            uniqueOpens = 858u,
            uniqueClicks = 687u,
            blogRegistrationDate = Date(1673740800000L).toString(),
        )
    }

    private const val EMAIL_SUBSCRIBER_STATUS_INDEX = 0
}
