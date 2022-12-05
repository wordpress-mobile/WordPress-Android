package org.wordpress.android.viewmodel.storage

import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Observer
import kotlinx.coroutines.InternalCoroutinesApi
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
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
    fun `view model emits checkStorageWarning event when it is the first start`() {
        viewModel.checkStorageWarning.observeForever(checkStorageWarningObserver)
        viewModel.start(true)

        verify(checkStorageWarningObserver, times(1)).onChanged(anyOrNull())
    }

    @Test
    fun `view model does not emit checkStorageWarning event when it is not the first start`() {
        viewModel.checkStorageWarning.observeForever(checkStorageWarningObserver)
        viewModel.start(false)

        verify(checkStorageWarningObserver, times(0)).onChanged(anyOrNull())
    }

    @Test
    fun `onStorageWarningCheck triggers notifyOnLowStorageSpace`() {
        viewModel.onStorageWarningCheck(fm, source)
        verify(storageUtilsProvider, times(1)).notifyOnLowStorageSpace(fm, source)
    }
}
