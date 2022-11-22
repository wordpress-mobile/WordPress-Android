package org.wordpress.android.localcontentmigration

import android.content.ContentResolver
import android.database.Cursor
import android.net.Uri
import org.wordpress.android.localcontentmigration.LocalMigrationError.ProviderError.NullCursor
import org.wordpress.android.localcontentmigration.LocalMigrationError.ProviderError.NullValueFromQuery
import org.wordpress.android.localcontentmigration.LocalMigrationError.ProviderError.ParsingException
import org.wordpress.android.localcontentmigration.LocalMigrationResult.Failure
import org.wordpress.android.localcontentmigration.LocalMigrationResult.Success
import org.wordpress.android.provider.query.QueryResult
import org.wordpress.android.util.publicdata.WordPressPublicData
import org.wordpress.android.viewmodel.ContextProvider
import javax.inject.Inject

@PublishedApi internal const val CONTENT_SCHEME = "content"

@PublishedApi internal fun ContentResolver.query(
    builder: Uri.Builder,
    entityType: LocalContentEntity,
    entityId: Int?,
) : Cursor? {
    val entityPath = entityType.getPathForContent(entityId)
    builder.appendEncodedPath(entityPath)
    return query(builder.build(), arrayOf(), "", arrayOf(), "")
}


class LocalMigrationContentResolver @Inject constructor(
    @PublishedApi internal val contextProvider: ContextProvider,
    @PublishedApi internal val wordPressPublicData: WordPressPublicData,
    @PublishedApi internal val queryResult: QueryResult,
){
    inline fun <reified T : LocalContentEntityData> getDataForEntityType(
        entityType: LocalContentEntity,
        entityId: Int? = null
    ) = getResultForEntityType<T>(entityType, entityId).let {
        when (it) {
            is Success -> it.value
            is Failure -> error(it.error)
        }
    }

    @PublishedApi internal inline fun <reified T : LocalContentEntityData> Cursor.getValue() =
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
        with (contextProvider.getContext().contentResolver) {
            val cursor = query(uriBuilder, entityType, entityId)
            if (cursor == null) Failure(NullCursor(entityType))
            else runCatching {
                val value = cursor.getValue<T>()
                if (value == null) Failure(NullValueFromQuery(entityType))
                else Success(value)
            }.getOrDefault(Failure(ParsingException(entityType)))
        }
    }
}
