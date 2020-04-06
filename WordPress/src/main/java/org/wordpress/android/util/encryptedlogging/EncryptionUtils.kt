package org.wordpress.android.util.encryptedlogging

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
        val sodium = LazySodiumAndroid(SodiumAndroid())
    }
}
