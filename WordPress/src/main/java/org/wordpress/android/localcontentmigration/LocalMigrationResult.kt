package org.wordpress.android.localcontentmigration

import android.os.Parcelable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.parcelize.Parcelize
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.localcontentmigration.LocalContentEntityData.AccessTokenData
import org.wordpress.android.localcontentmigration.LocalContentEntityData.EmptyData
import org.wordpress.android.localcontentmigration.LocalContentEntityData.SitesData
import org.wordpress.android.localcontentmigration.LocalMigrationResult.Companion.EmptyResult
import org.wordpress.android.localcontentmigration.LocalMigrationResult.Failure
import org.wordpress.android.localcontentmigration.LocalMigrationResult.Success
import org.wordpress.android.localcontentmigration.LocalMigrationState.Initial
import org.wordpress.android.localcontentmigration.LocalMigrationState.Migrating

sealed class LocalMigrationResult<out T: LocalContentEntityData, out E: LocalMigrationError> {
    data class Success<T: LocalContentEntityData>(val value: T): LocalMigrationResult<T, Nothing>()
    data class Failure<E: LocalMigrationError>(val error: E): LocalMigrationResult<Nothing, E>()
    companion object {
        val EmptyResult = Success(EmptyData)
    }
}
fun <T: LocalContentEntityData, U: LocalContentEntityData, E: LocalMigrationError> LocalMigrationResult<T, E>
        .thenWith(next: (T) -> LocalMigrationResult<U, E>) = when (this) {
    is Success -> next(this.value)
    is Failure -> this
}

fun <T: LocalContentEntityData> LocalMigrationResult<LocalContentEntityData, LocalMigrationError>
        .then(next: () -> LocalMigrationResult<T, LocalMigrationError>) = when (this) {
    is Success -> next()
    is Failure -> this
}

fun <E: LocalMigrationError> LocalMigrationResult<LocalContentEntityData, E>.orElse(
    handleError: (E) -> LocalMigrationResult<LocalContentEntityData, LocalMigrationError>
) = when (this) {
    is Success -> this
    is Failure -> handleError(this.error)
}

fun <T: LocalContentEntityData, E: LocalMigrationError> LocalMigrationResult<T, E>
        .otherwise(handleError: (E) -> Unit) = when (this) {
    is Success -> Unit
    is Failure -> handleError(this.error)
}

/**
 * This function folds all items in the iterable using the provided transform function, returning early on the first
 * failure. When all items are transformed into successful results, an empty successful result is returned. References
 * to data in any intermediate successful results are intentionally discarded to allow earlier garbage collection,
 * freeing up memory for large collections.
 *
 * @param transform A function which accepts an element from the collection and returns a local migration result.
 */
inline fun <T: Any?,  U: LocalContentEntityData, E: LocalMigrationError> Iterable<T>.foldAllToSingleResult(
    transform: (T) -> LocalMigrationResult<U, E>,
) = fold(EmptyResult) { current: LocalMigrationResult<LocalContentEntityData, LocalMigrationError>, item ->
    when (val result = transform(item)) {
        is Failure -> return result
        else -> current
    }
}

fun LocalMigrationResult<LocalContentEntityData, LocalMigrationError>.emitTo(
    flow: MutableStateFlow<LocalMigrationState>,
) = this.also {
    when (this) {
        is Success -> emitDataToFlow(this.value, flow)
        is Failure -> Failure(this.error)
    }
}

private fun emitDataToFlow(data: LocalContentEntityData, flow: MutableStateFlow<LocalMigrationState>) {
    if (flow.value is Initial) {
        when (data) {
            is AccessTokenData -> flow.value = Migrating(avatarUrl = data.avatarUrl)
            is SitesData -> flow.value = Migrating(sites = data.sites)
            else -> Unit
        }
    } else {
        when (data) {
            is AccessTokenData -> (flow.value as? Migrating)?.let { currentState ->
                flow.value = currentState.copy(avatarUrl = data.avatarUrl)
            }
            is SitesData -> (flow.value as? Migrating)?.let { currentState ->
                flow.value = currentState.copy(sites = data.sites)
            }
            else -> Unit
        }
    }
}

sealed class LocalMigrationState {
    object Initial: LocalMigrationState()
    data class Migrating(
        val avatarUrl: String = "",
        val sites: List<SiteModel> = emptyList(),
    ): LocalMigrationState()
    sealed class Finished: LocalMigrationState() {
        object Successful: Finished()
        data class Failure(val error: LocalMigrationError): Finished()
    }
    sealed class SingleStep: LocalMigrationState(), Parcelable {
        @Parcelize
        object DeleteSingleStep: SingleStep()
        @Parcelize
        object UpdateWPSingleStep: SingleStep()
    }
}
