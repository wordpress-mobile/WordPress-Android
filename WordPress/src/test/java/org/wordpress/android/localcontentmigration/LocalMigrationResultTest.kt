package org.wordpress.android.localcontentmigration

import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.kotlin.spy
import org.mockito.kotlin.verify
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.localcontentmigration.LocalContentEntity.Sites
import org.wordpress.android.localcontentmigration.LocalContentEntityData.AccessTokenData
import org.wordpress.android.localcontentmigration.LocalContentEntityData.SitesData
import org.wordpress.android.localcontentmigration.LocalMigrationResult.Failure
import org.wordpress.android.localcontentmigration.LocalMigrationResult.Success
import org.wordpress.android.localcontentmigration.LocalMigrationState.Initial
import org.wordpress.android.localcontentmigration.LocalMigrationState.Migrating

class LocalMigrationResultTest {
    @Test
    fun `Should invoke next function with value if Success on thenWith`() {
        val data = AccessTokenData("", "")
        val next: (LocalContentEntityData) -> LocalMigrationResult<LocalContentEntityData, LocalMigrationError> = spy {
            LocalMigrationResult.EmptyResult
        }
        Success(data).thenWith(next)
        verify(next).invoke(data)
    }

    @Test
    fun `Should return Failure if Failure on thenWith`() {
        val expected = Failure(LocalMigrationError.ProviderError.NullCursor(Sites))
        val actual = expected.thenWith {
            LocalMigrationResult.EmptyResult
        }
        assertEquals(expected, actual)
    }

    @Test
    fun `Should invoke next function if Success on then`() {
        val data = AccessTokenData("", "")
        val next: () -> LocalMigrationResult<LocalContentEntityData, LocalMigrationError> = spy {
            LocalMigrationResult.EmptyResult
        }
        Success(data).then(next)
        verify(next).invoke()
    }

    @Test
    fun `Should return Failure if Failure on then`() {
        val expected = Failure(LocalMigrationError.ProviderError.NullValueFromQuery(Sites))
        val actual = expected.then {
            LocalMigrationResult.EmptyResult
        }
        assertEquals(expected, actual)
    }

    @Test
    fun `Should return value if Success on orElse`() {
        val data = AccessTokenData("", "")
        val expected = Success(data)
        val actual = Success(data).orElse {
            LocalMigrationResult.EmptyResult
        }
        assertEquals(expected, actual)
    }

    @Test
    fun `Should invoke handleError function if Failure on orElse`() {
        val error = LocalMigrationError.ProviderError.NullValueFromQuery(Sites)
        val handleError: (LocalMigrationError) -> LocalMigrationResult<LocalContentEntityData, LocalMigrationError> =
            spy { LocalMigrationResult.EmptyResult }
        Failure(error).orElse(handleError)
        verify(handleError).invoke(error)
    }

    @Test
    fun `Should return Unit if Success on otherwise`() {
        val expected = Unit
        val actual = Success(AccessTokenData("", "")).otherwise {}
        assertEquals(expected, actual)
    }

    @Test
    fun `Should invoke handleError function if Failure on otherwise`() {
        val error = LocalMigrationError.ProviderError.NullValueFromQuery(Sites)
        val handleError: (LocalMigrationError) -> Unit = spy { LocalMigrationResult.EmptyResult }
        Failure(error).otherwise(handleError)
        verify(handleError).invoke(error)
    }

    @Test
    fun `Should emitTo correct state if current state IS Initial and data IS AccessTokenData`() {
        val data = AccessTokenData("token", "avatarUrl")
        val mutableStateFlow: MutableStateFlow<LocalMigrationState> = MutableStateFlow(Initial)
        Success(data).emitTo(mutableStateFlow)
        val expected = Migrating(WelcomeScreenData(avatarUrl = data.avatarUrl)).data
        val actual = mutableStateFlow.value.data
        assertEquals(expected, actual)
        assertTrue(mutableStateFlow.value is Migrating)
    }

    @Test
    fun `Should emitTo correct state if current state IS Initial and data IS SitesData`() {
        val sites = listOf(SiteModel(), SiteModel())
        val data = SitesData(sites)
        val mutableStateFlow: MutableStateFlow<LocalMigrationState> = MutableStateFlow(Initial)
        Success(data).emitTo(mutableStateFlow)
        val expected = Migrating(WelcomeScreenData(sites = sites)).data
        val actual = mutableStateFlow.value.data
        assertEquals(expected, actual)
        assertTrue(mutableStateFlow.value is Migrating)
    }

    @Test
    fun `Should emitTo correct state if current state IS NOT Initial and data IS AccessTokenData`() {
        val data = AccessTokenData("token", "avatarUrl")
        val mutableStateFlow: MutableStateFlow<LocalMigrationState> = MutableStateFlow(Migrating(WelcomeScreenData()))
        Success(data).emitTo(mutableStateFlow)
        val expected = Migrating(WelcomeScreenData(avatarUrl = data.avatarUrl)).data
        val actual = mutableStateFlow.value.data
        assertEquals(expected, actual)
        assertTrue(mutableStateFlow.value is Migrating)
    }

    @Test
    fun `Should emitTo correct state if current state IS NOT Initial and data IS SitesData`() {
        val sites = listOf(SiteModel(), SiteModel())
        val data = SitesData(sites)
        val mutableStateFlow: MutableStateFlow<LocalMigrationState> = MutableStateFlow(Migrating(WelcomeScreenData()))
        Success(data).emitTo(mutableStateFlow)
        val expected = Migrating(WelcomeScreenData(sites = sites)).data
        val actual = mutableStateFlow.value.data
        assertEquals(expected, actual)
        assertTrue(mutableStateFlow.value is Migrating)
    }
}
