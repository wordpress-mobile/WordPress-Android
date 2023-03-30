package org.wordpress.android.util.config

open class RemoteConfigField<T : Any>(val appConfig: AppConfig, val remoteField: String) {
    @Suppress("UseCheckOrError")
    inline fun <reified R : T> getValue(): R {
        val remoteFieldValue = appConfig.getRemoteFieldConfigValue(remoteField)
        return when (R::class) {
            Int::class -> remoteFieldValue.toInt() as R
            String::class -> remoteFieldValue as R
            Long::class -> remoteFieldValue.toLong() as R
            // add other types here if need
            else -> throw IllegalStateException("Unknown Generic Type")
        }
    }
}
