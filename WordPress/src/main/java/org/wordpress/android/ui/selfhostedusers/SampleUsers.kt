package org.wordpress.android.ui.selfhostedusers

import uniffi.wp_api.UserWithEditContext

/**
 * This is a temporary object to supply a list of users for the self-hosted user list.
 * It will be removed once the network request to retrieve users is implemented.
 */
object SampleUsers {
    private val sampleUserList = ArrayList<UserWithEditContext>()

    private val sampleUser1 = UserWithEditContext(
        id = 1,
        username = "@sampleUser",
        avatarUrls = emptyMap(),
        capabilities = emptyMap(),
        description = "User description",
        email = "email@exmaple.com",
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
    )

    // TODO remove the avatar url
    private val sampleUser2 = UserWithEditContext(
        id = 2,
        username = "@sampleUserWithALongUserName",
        avatarUrls = mapOf("sampleUserTwo" to "https://nickbradbury.com/wp-content/uploads/2022/03/1394-2.jpg"),
        capabilities = emptyMap(),
        description = "User description",
        email = "email@exmaple.com",
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
        id = 1,
        username = "@sampleUser",
        avatarUrls = emptyMap(),
        capabilities = emptyMap(),
        description = "User description",
        email = "email@exmaple.com",
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
        if (sampleUserList.isEmpty()) {
            repeat(25) {
                sampleUserList.add(sampleUser1)
                sampleUserList.add(sampleUser2)
                sampleUserList.add(sampleUser3)
            }
        }
        return sampleUserList
    }
}
