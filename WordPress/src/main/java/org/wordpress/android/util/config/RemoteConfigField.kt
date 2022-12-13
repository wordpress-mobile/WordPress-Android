package org.wordpress.android.util.config

open class RemoteConfigField<T>(val appConfig: AppConfig, val remoteField: String, val defaultValue: T) {
    @Suppress("UseCheckOrError")
    inline fun <reified T> getValue() : T  {
        val remoteFieldValue = appConfig.getRemoteFieldConfigValue(remoteField)
        return when (T::class) {
            Int::class -> remoteFieldValue.toInt() as T
            String::class -> remoteFieldValue as T
            Long::class -> remoteFieldValue.toLong() as T
            // add other types here if need
            else -> throw IllegalStateException("Unknown Generic Type")
        }
    }
}
