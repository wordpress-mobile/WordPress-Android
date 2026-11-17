package org.wordpress.android.ui.newstats.util

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Rule
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnit
import org.mockito.junit.MockitoRule
import org.mockito.quality.Strictness
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.wordpress.android.BaseUnitTest

/**
 * Base class for the New Stats tests whose subject formats a date.
 *
 * Every stats date is written in the reader's locale's own field order, which [statsDateFormatter]
 * reads out of ICU through `android.text.format.DateFormat`. That is a framework call, so under the
 * plain JVM runner — where `returnDefaultValues` hands back a null pattern — it cannot run at all.
 * Robolectric supplies the real ICU data, which is also what makes a "11 Aug" assertion mean
 * something: it is the string the device would actually render.
 *
 * Robolectric replaces [BaseUnitTest]'s Mockito runner, so `@Mock` fields are opened by a rule
 * instead. The rule is lenient to match the `MockitoJUnitRunner.Silent` these tests ran under: they
 * stub more than any single case uses.
 */
@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
@Config(application = android.app.Application::class)
@Suppress("UnnecessaryAbstractClass")
abstract class StatsDateUnitTest(
    testDispatcher: TestDispatcher = UnconfinedTestDispatcher()
) : BaseUnitTest(testDispatcher) {
    @Rule
    @JvmField
    val mockitoRule: MockitoRule = MockitoJUnit.rule().strictness(Strictness.LENIENT)
}
