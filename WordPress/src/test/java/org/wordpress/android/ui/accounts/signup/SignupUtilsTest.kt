package org.wordpress.android.ui.accounts.signup

import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
class SignupUtilsTest {
    private lateinit var signupUtils: SignupUtils

    @Before
    fun setUp() {
        this.signupUtils = SignupUtils()
    }

    @Test
    fun `maps simple email to display name`() {
        assertEmailMappedToDisplayName("displayname@email.com", "Displayname")
    }

    @Test
    fun `maps longer email to display name`() {
        assertEmailMappedToDisplayName("firstname.lastname@email.com", "Firstname Lastname")
    }

    @Test
    fun `maps single character short email to display name`() {
        assertEmailMappedToDisplayName("a@email.com", "A")
    }

    @Test
    fun `maps single character longer email to display name`() {
        assertEmailMappedToDisplayName("a.b.c@email.com", "A B C")
    }

    @Test
    fun `maps invalid email only with domain to null`() {
        assertEmailMappedToDisplayName("@email.com", null)
    }

    @Test
    fun `maps invalid email ending with numbers in the middle`() {
        assertEmailMappedToDisplayName("username.12.a@email.com", "Username A")
    }

    @Test
    fun `maps invalid email without domain to just display name`() {
        assertEmailMappedToDisplayName("displayname", "Displayname")
    }

    @Test
    fun `creates username from simple email`() {
        assertEmailMappedToUsername("username@email.com", "username")
    }

    @Test
    fun `creates username from longer email`() {
        assertEmailMappedToUsername("firstname.lastname@email.com", "firstnamelastname")
    }

    @Test
    fun `creates username from single character short email`() {
        assertEmailMappedToUsername("a@email.com", "a")
    }

    @Test
    fun `creates username from single character longer email`() {
        assertEmailMappedToUsername("a.b.c@email.com", "abc")
    }

    @Test
    fun `returns null username from invalid email only with domain`() {
        assertEmailMappedToUsername("@email.com", "")
    }

    @Test
    fun `creates username from invalid email without domain`() {
        assertEmailMappedToUsername("username", "username")
    }

    private fun assertEmailMappedToDisplayName(email: String, expectedDisplayName: String? = null) {
        val displayName = signupUtils.createDisplayNameFromEmail(email)
        assertThat(displayName).isEqualTo(expectedDisplayName)
    }

    private fun assertEmailMappedToUsername(email: String, expectedUsername: String? = null) {
        val username = signupUtils.createUsernameFromEmail(email)
        assertThat(username).isEqualTo(expectedUsername)
    }
}
