package org.wordpress.android.ui.selfhostedusers

import uniffi.wp_api.UserWithEditContext

/**
 * This is a temporary object to supply a list of users for the self-hosted user list.
 * It will be removed once the network request to retrieve users is implemented.
 */
@Suppress("MaxLineLength")
object SampleUsers {
    private val sampleUserList = ArrayList<UserWithEditContext>()

    private val sampleUser1 = UserWithEditContext(
        id = 1,
        username = "@sampleUser",
        avatarUrls = emptyMap(),
        capabilities = emptyMap(),
        email = "email@example.com",
        extraCapabilities = emptyMap(),
        firstName = "Sample",
        lastName = "User",
        link = "example.com",
        locale = "en-US",
        name = "Sample User",
        nickname = "User nickname",
        registeredDate = "2023-01-01",
        roles = listOf("admin"),
        slug = "sample-user",
        url = "example.com",
        description = "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Etiam non quam viverra, viverra est vel, interdum felis. Pellentesque interdum libero quis metus pharetra ullamcorper. Morbi nec libero ligula. Quisque consectetur, purus sit amet lobortis porttitor, ligula ex imperdiet massa, in ullamcorper augue odio sit amet metus. In sollicitudin mauris et risus mollis commodo. Aliquam vel vehicula ante, nec blandit erat. Aenean non turpis porttitor orci fringilla fringilla nec ac nunc. Nulla ultrices urna ut ipsum posuere blandit. Phasellus mauris nulla, tincidunt at leo at, auctor interdum felis. Sed pharetra risus a ullamcorper dictum. Suspendisse pharetra justo molestie risus lobortis facilisis.",
    )

    private val sampleUser2 = UserWithEditContext(
        id = 2,
        username = "@sampleUserWithALongUserName",
        avatarUrls = emptyMap(),
        capabilities = emptyMap(),
        description = "User description",
        email = "email@example.com",
        extraCapabilities = emptyMap(),
        firstName = "Sample",
        lastName = "User",
        link = "example.com",
        locale = "en-US",
        name = "Sample User",
        nickname = "User nickname",
        registeredDate = "2023-01-01",
        roles = listOf("contributor"),
        slug = "sample-user",
        url = "example.com",
    )

    private val sampleUser3 = UserWithEditContext(
        id = 3,
        username = "@sampleUser",
        avatarUrls = emptyMap(),
        capabilities = emptyMap(),
        description = "User description",
        email = "email@example.com",
        extraCapabilities = emptyMap(),
        firstName = "Sample",
        lastName = "User",
        link = "example.com",
        locale = "en-US",
        name = "Sample User",
        nickname = "User nickname",
        registeredDate = "2023-01-01",
        roles = listOf("contributor", "editor", "subscriber"),
        slug = "sample-user",
        url = "example.com",
    )

    @Suppress("MagicNumber")
    fun getSampleUsers(): ArrayList<UserWithEditContext> {
        fun addWithId(user: UserWithEditContext) {
            sampleUserList.add(
                user.copy(
                    id = sampleUserList.size,
                    name = "${user.name}${sampleUserList.size}"
                )
            )
        }
        if (sampleUserList.isEmpty()) {
            repeat(25) {
                addWithId(sampleUser1)
                addWithId(sampleUser2)
                addWithId(sampleUser3)
            }
        }
        return sampleUserList
    }
}
