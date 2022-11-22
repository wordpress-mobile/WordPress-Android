package org.wordpress.android.localcontentmigration

import org.wordpress.android.localcontentmigration.LocalMigrationResult.Failure
import org.wordpress.android.localcontentmigration.LocalMigrationResult.Success

sealed class LocalMigrationResult<out T: LocalContentEntityData, out E: LocalMigrationError> {
    data class Success<T: LocalContentEntityData>(val value: T): LocalMigrationResult<T, Nothing>()
    data class Failure<E: LocalMigrationError>(val error: E): LocalMigrationResult<Nothing, E>()
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
