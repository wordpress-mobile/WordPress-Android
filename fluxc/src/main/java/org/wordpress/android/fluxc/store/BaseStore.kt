package org.wordpress.android.fluxc.store

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.withContext
import org.wordpress.android.fluxc.store.BaseStore.LoggerFactory.Logger
import org.wordpress.android.fluxc.utils.AppLogWrapper
import org.wordpress.android.util.AppLog.T
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

open class BaseStore(private val logger: Logger) {
    protected suspend fun <RESULT_TYPE> withContext(
        context: CoroutineContext,
        loggedMessage: String,
        block: suspend CoroutineScope.() -> RESULT_TYPE
    ): RESULT_TYPE {
        logger.log(this.javaClass.simpleName, loggedMessage)
        return withContext(context, block)
    }

    protected fun <RESULT_TYPE> withLog(loggedMessage: String, block: () -> RESULT_TYPE): RESULT_TYPE {
        logger.log(this.javaClass.simpleName, loggedMessage)
        return block()
    }

    class LoggerFactory
    @Inject constructor(private val appLog: AppLogWrapper) {
        fun build(tag: T): Logger {
            return Logger(tag, appLog)
        }

        class Logger(
            private val tag: T,
            private val appLog: AppLogWrapper
        ) {
            fun log(className: String, message: String) {
                return appLog.d(tag, "$className: $message")
            }
        }
    }
}


