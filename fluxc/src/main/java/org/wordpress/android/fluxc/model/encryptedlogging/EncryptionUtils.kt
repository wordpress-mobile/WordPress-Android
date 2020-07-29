package org.wordpress.android.fluxc.model.encryptedlogging

import com.goterl.lazycode.lazysodium.LazySodiumAndroid
import com.goterl.lazycode.lazysodium.SodiumAndroid

/**
 * Convenience helpers for Encrypted Logging
 */
class EncryptionUtils {
    companion object {
        /**
         * Use a single shared instance of the Sodium library.
         *
         * The initialization is inexpensive, but verbose, so this is just syntactic sugar.
         */
        @JvmStatic
        val sodium = LazySodiumAndroid(SodiumAndroid())
    }
}
