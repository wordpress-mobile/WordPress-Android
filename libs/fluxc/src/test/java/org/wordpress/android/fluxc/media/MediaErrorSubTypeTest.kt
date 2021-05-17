package org.wordpress.android.fluxc.media

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.fluxc.store.media.MediaErrorSubType
import org.wordpress.android.fluxc.store.media.MediaErrorSubType.MalformedMediaArgSubType
import org.wordpress.android.fluxc.store.media.MediaErrorSubType.MalformedMediaArgSubType.Type.UNSUPPORTED_MIME_TYPE
import org.wordpress.android.fluxc.store.media.MediaErrorSubType.UndefinedSubType

@RunWith(MockitoJUnitRunner::class)
class MediaErrorSubTypeTest {
    @Test
    fun `UndefinedSubType is got from string`() {
        val result = UndefinedSubType.fromString(UndefinedSubType().toString())
        assertThat(result).isNotNull
        assertThat(result is UndefinedSubType).isTrue
    }

    @Test
    fun `UndefinedSubType is null from wrong string`() {
        val result = UndefinedSubType.fromString(MalformedMediaArgSubType(UNSUPPORTED_MIME_TYPE).toString())
        assertThat(result).isNull()
    }

    @Test
    fun `MalformedMediaArgSubType is got from string`() {
        val result = MalformedMediaArgSubType.fromString(MalformedMediaArgSubType(UNSUPPORTED_MIME_TYPE).toString())
        assertThat(result).isNotNull
        assertThat(result is MalformedMediaArgSubType).isTrue
        assertThat((result as MalformedMediaArgSubType).type).isEqualTo(UNSUPPORTED_MIME_TYPE)
    }

    @Test
    fun `MediaErrorSubType gives correct type from string`() {
        val result = MediaErrorSubType.getSubTypeFromString(MalformedMediaArgSubType(UNSUPPORTED_MIME_TYPE).toString())
        assertThat(result).isNotNull
        assertThat(result is MalformedMediaArgSubType).isTrue
        assertThat((result as MalformedMediaArgSubType).type).isEqualTo(UNSUPPORTED_MIME_TYPE)
    }
}
