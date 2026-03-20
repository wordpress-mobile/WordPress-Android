package org.wordpress.android.ui.reader

import android.app.Dialog
import android.content.DialogInterface
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.ComponentDialog
import androidx.activity.addCallback
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.core.view.WindowCompat
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.wordpress.android.R
import org.wordpress.android.ui.WPWebViewActivity
import org.wordpress.android.ui.compose.theme.AppThemeM3
import org.wordpress.android.ui.reader.models.ReaderReadingPreferences
import org.wordpress.android.ui.reader.tracker.ReaderReadingPreferencesTracker
import org.wordpress.android.ui.reader.viewmodels.ReaderReadingPreferencesViewModel
import org.wordpress.android.ui.reader.viewmodels.ReaderReadingPreferencesViewModel.ActionEvent
import org.wordpress.android.ui.reader.views.compose.readingpreferences.ReadingPreferencesScreen
import org.wordpress.android.util.extensions.fillScreen
import org.wordpress.android.util.extensions.getSerializableCompat
import org.wordpress.android.util.extensions.setDialogWindowStatusBarColor

@AndroidEntryPoint
class ReaderReadingPreferencesDialogFragment : BottomSheetDialogFragment() {
    private val viewModel: ReaderReadingPreferencesViewModel by viewModels()

    override fun getTheme(): Int {
        return R.style.ReaderReadingPreferencesDialogFragment
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.getSerializableCompat<ReaderReadingPreferencesTracker.Source>(ARG_SOURCE)?.let {
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
                val readerPreferences by viewModel.currentReadingPreferences.collectAsState()
                ReadingPreferencesScreen(
                    currentReadingPreferences = readerPreferences,
                    onCloseClick = viewModel::onExitActionClick,
                    onThemeClick = viewModel::onThemeClick,
                    onFontFamilyClick = viewModel::onFontFamilyClick,
                    onFontSizeClick = viewModel::onFontSizeClick,
                    onBackgroundColorUpdate = { dialog?.window?.setDialogWindowStatusBarColor(it) },
                )
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        observeActionEvents()
        viewModel.init()
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog =
        super.onCreateDialog(savedInstanceState).apply {
            (this as? BottomSheetDialog)?.apply {
                fillScreen(isDraggable = true)
                // Draw content behind the status bar so the TopAppBar's
                // background color is visible through the transparent
                // status bar (required on SDK 35+ where statusBarColor
                // is ignored).
                window?.let { WindowCompat.setDecorFitsSystemWindows(it, false) }

                behavior.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
                    private var isStatusBarTransparent = false
                    override fun onStateChanged(bottomSheet: View, newState: Int) {
                        if (newState == BottomSheetBehavior.STATE_EXPANDED && isStatusBarTransparent) {
                            isStatusBarTransparent = false
                            val currentTheme = viewModel.currentReadingPreferences.value.theme
                            handleUpdateStatusBarColor(currentTheme)
                        } else if (newState != BottomSheetBehavior.STATE_EXPANDED && !isStatusBarTransparent) {
                            isStatusBarTransparent = true
                            dialog?.window?.setDialogWindowStatusBarColor(Color.TRANSPARENT)
                        }

                        if (newState == BottomSheetBehavior.STATE_HIDDEN) {
                            viewModel.onBottomSheetHidden()
                        }
                    }

                    override fun onSlide(bottomSheet: View, slideOffset: Float) {
                        // no-op
                    }
                })
            }

            (this as ComponentDialog).onBackPressedDispatcher.addCallback(this@ReaderReadingPreferencesDialogFragment) {
                viewModel.onExitActionClick()
            }
        }

    override fun onDismiss(dialog: DialogInterface) {
        if (viewModel.hasUnsavedChanges) {
            viewModel.syncCachedPreferences()
            (activity as? ReaderPostPagerActivity)?.recreateCurrentPage()
        }
        viewModel.onScreenClosed()
        super.onDismiss(dialog)
    }

    private fun observeActionEvents() {
        viewModel.actionEvents.onEach {
            when (it) {
                is ActionEvent.Close -> dismiss()
                is ActionEvent.UpdateStatusBarColor -> handleUpdateStatusBarColor(it.theme)
                is ActionEvent.OpenWebView -> handleOpenWebView(it.url)
            }
        }.launchIn(viewLifecycleOwner.lifecycleScope)
    }

    private fun handleUpdateStatusBarColor(theme: ReaderReadingPreferences.Theme) {
        val context = requireContext()
        val themeValues = ReaderReadingPreferences.ThemeValues.from(context, theme)
        dialog?.window?.setDialogWindowStatusBarColor(themeValues.intBackgroundColor)
    }

    private fun handleOpenWebView(url: String) {
        context?.let { context ->
            WPWebViewActivity.openURL(context, url)
        }
    }

    companion object {
        private const val TAG = "READER_READING_PREFERENCES_FRAGMENT"
        private const val ARG_SOURCE = "source"

        @JvmStatic
        fun newInstance(
            source: ReaderReadingPreferencesTracker.Source,
        ): ReaderReadingPreferencesDialogFragment = ReaderReadingPreferencesDialogFragment().apply {
            arguments = Bundle().apply {
                putSerializable(ARG_SOURCE, source)
            }
        }

        @JvmStatic
        fun show(
            fm: FragmentManager,
            source: ReaderReadingPreferencesTracker.Source,
        ): ReaderReadingPreferencesDialogFragment = newInstance(source).also {
            it.show(fm, TAG)
        }
    }
}
