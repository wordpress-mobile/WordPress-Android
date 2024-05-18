package org.wordpress.android.ui.audiorecorder

import android.os.Bundle
import android.util.Log
import android.view.ViewGroup
import androidx.compose.material.*
import androidx.compose.ui.platform.ComposeView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import android.view.LayoutInflater
import android.view.View
import androidx.lifecycle.ViewModelProvider
import org.wordpress.android.WordPress
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.util.extensions.getSerializableCompat
import javax.inject.Inject

class AudioRecorderBottomSheetDialogFragment : BottomSheetDialogFragment() {
    @Inject
    internal lateinit var viewModelFactory: ViewModelProvider.Factory
    private lateinit var viewModel: AudioRecorderViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (requireNotNull(activity).application as WordPress).component().inject(this)
        viewModel = ViewModelProvider(this, viewModelFactory)[AudioRecorderViewModel::class.java]
        // todo: annmarie - this is not correct - could use a start method, but for the POC will do
        // todo: annmarie - using requireNotNull will crash the app, handle this nicer!!
        val site = requireNotNull(arguments?.getSerializableCompat<SiteModel>(SITE))
        viewModel.setSite(site)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                MaterialTheme {
                    AudioRecorderBottomSheetContent(viewModel)
                }
            }
        }
    }

    companion object {
        const val TAG = "audio_recorder_bottom_sheet_fragment_tag"
        const val SITE = "audio_recorder_bottom_sheet_site_model"
        const val IS_PAGE = "audio_recorder_bottom_sheet_is_page"

        @JvmStatic
        fun newInstance(site: SiteModel, isPage: Boolean) : AudioRecorderBottomSheetDialogFragment {
            Log.i("AudioRecorderBottomSheetDialog", "***=> In newInstance with ${site.name}")
            val frag = AudioRecorderBottomSheetDialogFragment().apply {
                arguments = Bundle().apply {
                    putSerializable(SITE, site)
                    putBoolean(IS_PAGE, isPage)
                }
            }
            return frag
        }
    }
}
