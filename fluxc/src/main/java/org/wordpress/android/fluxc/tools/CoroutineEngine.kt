package org.wordpress.android.fluxc.tools

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.wordpress.android.fluxc.utils.AppLogWrapper
import org.wordpress.android.util.AppLog
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

class CoroutineEngine
@Inject constructor(
    private val context: CoroutineContext,
    private val appLog: AppLogWrapper
) : CoroutineScope {
    private val job = Job()
    override val coroutineContext: CoroutineContext
        get() = job + context

    suspend fun <RESULT_TYPE> withDefaultContext(
        tag: AppLog.T,
        caller: Any,
        loggedMessage: String,
        block: suspend CoroutineScope.() -> RESULT_TYPE
    ): RESULT_TYPE {
        appLog.d(tag, "${caller.javaClass.simpleName}: $loggedMessage")
        return withContext(coroutineContext, block)
    }

    fun <RESULT_TYPE> run(tag: AppLog.T, caller: Any, loggedMessage: String, block: () -> RESULT_TYPE): RESULT_TYPE {
        appLog.d(tag, "${caller.javaClass.simpleName}: $loggedMessage")
        return block()
    }

    fun <RESULT_TYPE> launch(
        tag: AppLog.T,
        caller: Any,
        loggedMessage: String,
        block: suspend CoroutineScope.() -> RESULT_TYPE
    ): Job {
        appLog.d(tag, "${caller.javaClass.simpleName}: $loggedMessage")
        return launch(coroutineContext) {
            block(this)
        }
    }
}
