package org.wordpress.android

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner

@Suppress("UnnecessaryAbstractClass")
@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
abstract class BaseUnitTest(testDispatcher: TestDispatcher = UnconfinedTestDispatcher()) {
    @Rule @JvmField val rule = InstantTaskExecutorRule()
    @Rule @JvmField val coroutinesTestRule = CoroutineTestRule(testDispatcher)

    protected fun testDispatcher() = coroutinesTestRule.testDispatcher

    protected fun testScope() = TestScope(testDispatcher())

    protected fun test(block: suspend TestScope.() -> Unit) = runTest(testDispatcher()) { block() }

    protected fun advanceUntilIdle() = testScope().advanceUntilIdle()
}
