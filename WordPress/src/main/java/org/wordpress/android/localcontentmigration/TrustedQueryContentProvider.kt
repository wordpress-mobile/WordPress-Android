package org.wordpress.android.localcontentmigration

import android.database.Cursor
import android.net.Uri
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import org.wordpress.android.provider.query.QueryContentProvider
import org.wordpress.android.util.config.JetpackProviderSyncFeatureConfig
import org.wordpress.android.util.publicdata.ClientVerification

abstract class TrustedQueryContentProvider : QueryContentProvider() {
    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface TrustedQueryContentProviderEntryPoint {
        fun jetpackProviderSyncFeatureConfig(): JetpackProviderSyncFeatureConfig
        fun clientVerification(): ClientVerification
    }

    override fun onCreate() = true

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?
    ): Cursor? {
        val context = checkNotNull(context) { "Cannot find context from the provider." }
        with(
            EntryPointAccessors.fromApplication(
                context.applicationContext,
                TrustedQueryContentProviderEntryPoint::class.java
            )
        ) {
            return if (jetpackProviderSyncFeatureConfig().isEnabled() && clientVerification().canTrust(callingPackage))
                query(uri)
            else
                null
        }
    }

    abstract fun query(uri: Uri): Cursor?
}
