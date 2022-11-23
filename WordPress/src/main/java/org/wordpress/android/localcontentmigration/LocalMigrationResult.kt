package org.wordpress.android.localcontentmigration

import kotlinx.coroutines.flow.MutableStateFlow
import org.wordpress.android.localcontentmigration.LocalContentEntityData.EmptyData
import org.wordpress.android.localcontentmigration.LocalMigrationResult.Companion.EmptyResult
import org.wordpress.android.localcontentmigration.LocalMigrationResult.Failure
import org.wordpress.android.localcontentmigration.LocalMigrationResult.Success

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

fun <T: LocalContentEntityData> LocalMigrationResult<T, LocalMigrationError>.emitTo(flow: MutableStateFlow<T>) =
        when (this) {
            is Success -> {
                flow.value = this.value
                this
            }
            is Failure -> this
        }

fun <R: LocalContentEntityData, T> LocalMigrationResult<R, LocalMigrationError>.emitTo(
    flow: MutableStateFlow<T>,
    transform: (R) -> T,
) = when (this) {
    is Success -> {
        flow.value = transform(this.value)
        this
    }
    is Failure -> this
}
