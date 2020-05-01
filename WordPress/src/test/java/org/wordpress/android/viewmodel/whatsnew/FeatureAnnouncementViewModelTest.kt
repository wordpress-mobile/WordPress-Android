package org.wordpress.android.viewmodel.whatsnew

import androidx.lifecycle.Observer
import com.nhaarman.mockitokotlin2.anyOrNull
import com.nhaarman.mockitokotlin2.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.WordPress
import org.wordpress.android.ui.whatsnew.FeatureAnnouncementViewModel
import org.wordpress.android.ui.whatsnew.FeatureAnnouncementViewModel.FeatureAnnouncementUiModel
import org.wordpress.android.util.NoDelayCoroutineDispatcher

class FeatureAnnouncementViewModelTest : BaseUnitTest() {
    private lateinit var viewModel: FeatureAnnouncementViewModel
    @Mock lateinit var onDialogClosedObserver: Observer<Unit>

    private val uiModelResults = mutableListOf<FeatureAnnouncementUiModel>()

    @Before
    fun setUp() {
        WordPress.versionName = "1.0"
        uiModelResults.clear()

        viewModel = FeatureAnnouncementViewModel(NoDelayCoroutineDispatcher())
        viewModel.uiModel.observeForever { if (it != null) uiModelResults.add(it) }
        viewModel.onDialogClosed.observeForever(onDialogClosedObserver)

        viewModel.start()
    }

    @Test
    fun `progress is visible after start`() {
        assertThat(uiModelResults.isNotEmpty()).isEqualTo(true)
        assertThat(uiModelResults[0].isProgressVisible).isEqualTo(true)
    }

    @Test
    fun `pressing close button closes the dialog`() {
        viewModel.onCloseDialogButtonPressed()
        verify(onDialogClosedObserver).onChanged(anyOrNull())
    }
}
