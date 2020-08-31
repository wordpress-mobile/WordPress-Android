package org.wordpress.android.fluxc.media

import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.fluxc.store.MediaStore.MediaError
import org.wordpress.android.fluxc.store.MediaStore.MediaErrorType.BAD_REQUEST
import org.wordpress.android.fluxc.store.MediaStore.MediaErrorType.GENERIC_ERROR

@RunWith(MockitoJUnitRunner::class)
class MediaErrorTest {
    private lateinit var mediaError: MediaError

    @Before
    fun setUp() {
        mediaError = MediaError(GENERIC_ERROR)
    }

    @Test
    fun `user message empty on API user message empty`() {
        val userMessage = mediaError.apiUserMessageIfAvailable

        assertThat(userMessage).isNullOrEmpty()
    }

    @Test
    fun `user message extracted on BAD_REQUEST and API user message available`() {
        mediaError.type = BAD_REQUEST
        mediaError.message = "rest_upload_user_quota_exceeded|You have used your space quota. " +
                "Please delete files before uploading. Back"

        val userMessage = mediaError.apiUserMessageIfAvailable

        assertThat(userMessage).isNotEmpty()
        assertThat(userMessage).isEqualTo("You have used your space quota. " +
                "Please delete files before uploading.")
    }

    @Test
    fun `user message not extracted on media error type different from BAD_REQUEST`() {
        mediaError.message = "rest_upload_user_quota_exceeded|You have used your space quota. " +
                "Please delete files before uploading. Back"

        val userMessage = mediaError.apiUserMessageIfAvailable

        assertThat(userMessage).isNotEmpty()
        assertThat(userMessage).isEqualTo("rest_upload_user_quota_exceeded|You have used your space quota. " +
                "Please delete files before uploading. Back")
    }
}
