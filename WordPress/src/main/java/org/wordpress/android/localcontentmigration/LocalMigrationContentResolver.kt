package org.wordpress.android.localcontentmigration

import android.content.ContentResolver
import android.database.Cursor
import android.net.Uri
import org.wordpress.android.localcontentmigration.LocalMigrationError.ProviderError.CursorException
import org.wordpress.android.localcontentmigration.LocalMigrationError.ProviderError.NullCursor
import org.wordpress.android.localcontentmigration.LocalMigrationError.ProviderError.NullValueFromQuery
import org.wordpress.android.localcontentmigration.LocalMigrationError.ProviderError.ParsingException
import org.wordpress.android.localcontentmigration.LocalMigrationResult.Failure
import org.wordpress.android.localcontentmigration.LocalMigrationResult.Success
import org.wordpress.android.provider.query.QueryResult
import org.wordpress.android.util.publicdata.WordPressPublicData
import org.wordpress.android.viewmodel.ContextProvider
import javax.inject.Inject

@PublishedApi
internal const val CONTENT_SCHEME = "content"

@PublishedApi
internal fun ContentResolver.query(
    builder: Uri.Builder,
    entityType: LocalContentEntity,
    entityId: Int?,
): Cursor? {
    val entityPath = entityType.getPathForContent(entityId)
    builder.appendEncodedPath(entityPath)
    return query(builder.build(), arrayOf(), "", arrayOf(), "")
}


class LocalMigrationContentResolver @Inject constructor(
    @PublishedApi internal val contextProvider: ContextProvider,
    @PublishedApi internal val wordPressPublicData: WordPressPublicData,
    @PublishedApi internal val queryResult: QueryResult,
) {
    @PublishedApi
    internal inline fun <reified T : LocalContentEntityData> Cursor.getValue() =
        queryResult.getValue<T>(this)

    inline fun <reified T : LocalContentEntityData> getResultForEntityType(
        entityType: LocalContentEntity,
        entityId: Int? = null
    ) = wordPressPublicData.currentPackageId().let { packageId ->
        Uri.Builder().apply {
            scheme(CONTENT_SCHEME)
            authority("${packageId}.${LocalMigrationContentProvider::class.simpleName}")
        }
    }.let { uriBuilder ->
        with(contextProvider.getContext().contentResolver) {
            val cursor = runCatching {
                query(uriBuilder, entityType, entityId)
            }.getOrElse { failure ->
                return@with Failure(CursorException(entityType, failure))
            } ?: return@with Failure(NullCursor(entityType))

            runCatching {
                cursor.getValue<T>()?.let { Success(it) } ?: Failure(NullValueFromQuery(entityType))
            }.getOrElse {  failure ->
                Failure(ParsingException(entityType, failure))
            }
        }
    }
}
