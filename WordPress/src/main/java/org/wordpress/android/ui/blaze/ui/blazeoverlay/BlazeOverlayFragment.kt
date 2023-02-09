package org.wordpress.android.ui.blaze.ui.blazeoverlay

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import dagger.hilt.android.AndroidEntryPoint
import org.wordpress.android.ui.compose.theme.AppTheme
import org.wordpress.android.ui.compose.utils.LocaleAwareComposable
import org.wordpress.android.util.LocaleManager


@AndroidEntryPoint
class BlazeOverlayFragment : Fragment() {
    companion object {
        fun newInstance() = BlazeOverlayFragment()
    }

    private val viewModel: BlazeActivityViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = ComposeView(requireContext()).apply {
        setContent {
            AppTheme {
                val userLanguage by viewModel.refreshAppLanguage.observeAsState("")

                LocaleAwareComposable(
                    locale = LocaleManager.languageLocale(userLanguage),
                    onLocaleChange = viewModel::setAppLanguage
                ) {
                    // Create composable view
                }
            }
        }
    }
}
