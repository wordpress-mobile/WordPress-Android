package org.wordpress.android.ui.reader.discover.interests

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import org.wordpress.android.WordPress
import org.wordpress.android.ui.compose.theme.AppThemeM3
import org.wordpress.android.ui.pages.SnackbarMessageHolder
import org.wordpress.android.ui.reader.viewmodels.ReaderViewModel
import org.wordpress.android.ui.reader.views.compose.interests.ReaderInterestsScreen
import org.wordpress.android.ui.utils.UiHelpers
import org.wordpress.android.util.PerAppLocaleManager
import org.wordpress.android.util.extensions.getSerializableExtraCompat
import org.wordpress.android.viewmodel.observeEvent
import javax.inject.Inject

class ReaderInterestsFragment : Fragment() {
    @Inject
    lateinit var uiHelpers: UiHelpers

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory
    private lateinit var viewModel: ReaderInterestsViewModel

    @Inject
    lateinit var perAppLocaleManager: PerAppLocaleManager

    private val snackbarHostState = SnackbarHostState()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (requireActivity().application as WordPress).component().inject(this)
        viewModel = ViewModelProvider(this, viewModelFactory)[ReaderInterestsViewModel::class.java]
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val entryPoint = entryPoint()
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                val uiState by viewModel.uiState.observeAsState()
                AppThemeM3 {
                    uiState?.let { state ->
                        ReaderInterestsScreen(
                            uiState = state,
                            snackbarHostState = snackbarHostState,
                            showBackButton = entryPoint == EntryPoint.DISCOVER,
                            onBackClick = viewModel::onBackButtonClick,
                            onInterestToggled = viewModel::onInterestAtIndexToggled,
                            onDoneClick = viewModel::onDoneButtonClick,
                            onRetryClick = viewModel::onRetryButtonClick
                        )
                    }
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val entryPoint = entryPoint()
        val parentViewModel = if (entryPoint == EntryPoint.DISCOVER) {
            ViewModelProvider(requireParentFragment())[ReaderViewModel::class.java]
        } else {
            null
        }

        viewModel.snackbarEvents.observeEvent(viewLifecycleOwner) { showSnackbar(it) }

        viewModel.closeReaderInterests.observeEvent(viewLifecycleOwner) {
            requireActivity().finish()
        }

        viewModel.start(
            perAppLocaleManager.getCurrentLocaleLanguageCode(),
            parentViewModel,
            entryPoint
        )
    }

    private fun entryPoint() =
        requireActivity().intent.getSerializableExtraCompat(READER_INTEREST_ENTRY_POINT) ?: EntryPoint.DISCOVER

    private fun showSnackbar(holder: SnackbarMessageHolder) {
        val message = uiHelpers.getTextOfUiString(requireContext(), holder.message).toString()
        val actionLabel = holder.buttonTitle?.let { uiHelpers.getTextOfUiString(requireContext(), it).toString() }
        viewLifecycleOwner.lifecycleScope.launch {
            val result = snackbarHostState.showSnackbar(message, actionLabel, duration = SnackbarDuration.Long)
            if (result == SnackbarResult.ActionPerformed) {
                holder.buttonAction.invoke()
            }
        }
    }

    companion object {
        const val TAG = "reader_interests_fragment_tag"
        const val READER_INTEREST_ENTRY_POINT = "reader_interest_entry_point"
    }

    enum class EntryPoint {
        DISCOVER,
        SETTINGS,
    }
}
