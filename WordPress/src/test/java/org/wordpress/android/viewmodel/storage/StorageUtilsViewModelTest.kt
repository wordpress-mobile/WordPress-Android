package org.wordpress.android.viewmodel.storage

import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Observer
import com.nhaarman.mockitokotlin2.anyOrNull
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import kotlinx.coroutines.InternalCoroutinesApi
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.TEST_DISPATCHER
import org.wordpress.android.util.StorageUtilsProvider
import org.wordpress.android.util.StorageUtilsProvider.Source
import org.wordpress.android.viewmodel.Event

@InternalCoroutinesApi
class StorageUtilsViewModelTest : BaseUnitTest() {
    @Mock lateinit var storageUtilsProvider: StorageUtilsProvider
    @Mock lateinit var checkStorageWarningObserver: Observer<Event<Unit>>
    @Mock lateinit var fm: FragmentManager
    @Mock lateinit var source: Source

    private lateinit var viewModel: StorageUtilsViewModel

    @Before
    fun setUp() {
        viewModel = StorageUtilsViewModel(
                TEST_DISPATCHER,
                storageUtilsProvider
        )
    }

    @Test
    fun `onStorageWarningInit emits checkStorageWarning event`() {
        viewModel.checkStorageWarning.observeForever(checkStorageWarningObserver)

        viewModel.onStorageWarningInit()

        verify(checkStorageWarningObserver).onChanged(anyOrNull())
    }

    @Test
    fun `onStorageWarningCheck triggers notifyOnLowStorageSpace`() {
        viewModel.onStorageWarningCheck(fm, source)
        verify(storageUtilsProvider, times(1)).notifyOnLowStorageSpace(fm, source)
    }
}
