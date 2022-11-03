package org.wordpress.android.fluxc.tools

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.runBlocking
import org.mockito.Mockito.lenient
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

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
    lenient().doAnswer {
        return@doAnswer runBlocking {
            flow { it.getArgument<(suspend FlowCollector<Any>.() -> Unit)>(3).invoke(this) }
        }
    }.whenever(coroutineEngine).flowWithDefaultContext(
            any(),
            any(),
            any(),
            any<(suspend FlowCollector<Any>.() -> Unit)>()
    )
    coroutineEngine
}
