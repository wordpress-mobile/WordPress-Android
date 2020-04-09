package org.wordpress.android.fluxc.tools

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.runBlocking
import org.mockito.Mockito.lenient

fun initCoroutineEngine() = runBlocking {
    val coroutineEngine = mock<CoroutineEngine>()
    lenient().doAnswer {
        return@doAnswer runBlocking {
            it.getArgument<(suspend CoroutineScope.() -> Any)>(3).invoke(this)
        }
    }.whenever(coroutineEngine).withDefaultContext(
            any(),
            any(),
            any(),
            any<(suspend CoroutineScope.() -> Any)>()
    )
    lenient().doAnswer {
        it.getArgument<(() -> Any)>(3).invoke()
    }.whenever(coroutineEngine).run(
            any(),
            any(),
            any(),
            any<(() -> Any)>()
    )
    lenient().doAnswer {
        runBlocking {
            it.getArgument<(suspend CoroutineScope.() -> Any)>(3).invoke(this)
        }
        return@doAnswer mock<Job>()
    }.whenever(coroutineEngine).launch(
            any(),
            any(),
            any(),
            any<(suspend CoroutineScope.() -> Any)>()
    )
    coroutineEngine
}
