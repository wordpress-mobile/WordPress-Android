package org.wordpress.android.ui.activitylog.list.filter

import kotlinx.coroutines.InternalCoroutinesApi
import org.junit.Before
import org.junit.Test
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.TEST_DISPATCHER

@InternalCoroutinesApi
class ActivityLogTypeFilterViewModelTest : BaseUnitTest() {
    private lateinit var viewModel: ActivityLogTypeFilterViewModel

    @Before
    fun setUp() {
        viewModel = ActivityLogTypeFilterViewModel(TEST_DISPATCHER)
    }

    @Test
    fun `skeleton test`() {
        // Skeleton test.
    }
}
