package org.wordpress.android.fluxc.store.stats

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doAnswer
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.runBlocking
import org.wordpress.android.fluxc.tools.CoroutineEngine
import org.wordpress.android.util.AppLog

fun initCoroutineEngine(coroutineEngine: CoroutineEngine) = runBlocking {
    doAnswer {
        return@doAnswer runBlocking {
            it.getArgument<(suspend CoroutineScope.() -> Any)>(3).invoke(this)
        }
    }.whenever(coroutineEngine).runOnBackground(
            eq(AppLog.T.STATS),
            any(),
            any(),
            any<(suspend CoroutineScope.() -> Any)>()
    )
    doAnswer {
        it.getArgument<(() -> Any)>(3).invoke()
    }.whenever(coroutineEngine).run(
            eq(AppLog.T.STATS),
            any(),
            any(),
            any<(() -> Any)>()
    )
}

fun initCoroutineEngine() = runBlocking {
    val coroutineEngine = mock<CoroutineEngine>()
    initCoroutineEngine(coroutineEngine)
    coroutineEngine
}

