package org.wordpress.android.ui.reader

import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.wordpress.android.R
import org.wordpress.android.ui.compose.theme.AppThemeM3
import org.wordpress.android.ui.reader.tracker.ReaderReadingPreferencesTracker
import org.wordpress.android.ui.reader.viewmodels.ReaderReadingPreferencesViewModel
import org.wordpress.android.ui.reader.viewmodels.ReaderReadingPreferencesViewModel.ActionEvent
import org.wordpress.android.ui.reader.views.compose.readingpreferences.ReadingPreferencesScreen
import org.wordpress.android.util.extensions.getSerializableCompat

@AndroidEntryPoint
class ReaderReadingPreferencesDialogFragment : DialogFragment() {
    private val viewModel: ReaderReadingPreferencesViewModel by viewModels()

    override fun getTheme(): Int {
        return R.style.ReaderReadingPreferencesDialogFragment
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.getSerializableCompat<ReaderReadingPreferencesTracker.Source>(
            ARG_SOURCE
        )?.let {
            viewModel.onScreenOpened(it)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = ComposeView(requireContext()).apply {
        setContent {
            AppThemeM3 {
                val readerPreferences by viewModel
                    .currentReadingPreferences.collectAsState()
                val showSaveAction by viewModel
                    .hasUnsavedChanges.collectAsState()
                ReadingPreferencesScreen(
                    currentReadingPreferences = readerPreferences,
                    showSaveAction = showSaveAction,
                    onCloseClick = viewModel::onCloseClick,
                    onSaveClick = viewModel::onSaveClick,
                    onThemeClick = viewModel::onThemeClick,
                    onFontFamilyClick = viewModel::onFontFamilyClick,
                    onFontSizeClick = viewModel::onFontSizeClick,
                )
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        observeActionEvents()
    }

    override fun onDismiss(dialog: DialogInterface) {
        viewModel.onScreenClosed()
        super.onDismiss(dialog)
    }

    private fun observeActionEvents() {
        viewModel.actionEvents.onEach {
            when (it) {
                is ActionEvent.Close -> dismiss()
                is ActionEvent.SaveAndClose -> {
                    (activity as? ReaderPostPagerActivity)
                        ?.recreatePages()
                    dismiss()
                }
            }
        }.launchIn(viewLifecycleOwner.lifecycleScope)
    }

    companion object {
        private const val TAG = "READER_READING_PREFERENCES_FRAGMENT"
        private const val ARG_SOURCE = "source"

        fun show(
            fm: FragmentManager,
            source: ReaderReadingPreferencesTracker.Source,
        ): ReaderReadingPreferencesDialogFragment =
            ReaderReadingPreferencesDialogFragment().apply {
                arguments = Bundle().apply {
                    putSerializable(ARG_SOURCE, source)
                }
            }.also {
                it.show(fm, TAG)
            }
    }
}
