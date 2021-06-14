package org.wordpress.android.modules

import android.content.Context
import android.util.Base64
import com.automattic.android.tracks.crashlogging.CrashLogging
import com.automattic.android.tracks.crashlogging.CrashLoggingDataProvider
import com.automattic.android.tracks.crashlogging.CrashLoggingProvider
import com.goterl.lazysodium.utils.Key
import dagger.Binds
import dagger.Module
import dagger.Provides
import org.wordpress.android.BuildConfig
import org.wordpress.android.fluxc.model.encryptedlogging.EncryptedLoggingKey
import org.wordpress.android.util.crashlogging.WPCrashLoggingDataProvider
import javax.inject.Singleton

@Module
abstract class CrashLoggingModule {
    companion object {
        @Provides
        @Singleton
        fun provideCrashLogging(context: Context, crashLoggingDataProvider: CrashLoggingDataProvider): CrashLogging {
            return CrashLoggingProvider.createInstance(context, crashLoggingDataProvider)
        }

        @Provides
        fun provideEncryptedLoggingKey(): EncryptedLoggingKey {
            return EncryptedLoggingKey(Key.fromBytes(Base64.decode(BuildConfig.ENCRYPTED_LOGGING_KEY, Base64.DEFAULT)))
        }
    }

    @Binds
    abstract fun bindCrashLoggingDataProvider(dataProvider: WPCrashLoggingDataProvider): CrashLoggingDataProvider
}
