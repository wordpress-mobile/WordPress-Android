package org.wordpress.android.ui.stories.intro

import android.app.Dialog
import android.content.Context
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import kotlinx.android.synthetic.main.stories_intro_dialog_fragment.*
import org.wordpress.android.R
import org.wordpress.android.R.attr
import org.wordpress.android.WordPress
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.ui.ActivityLauncher
import org.wordpress.android.ui.photopicker.MediaPickerLauncher
import org.wordpress.android.util.getColorFromAttribute
import org.wordpress.android.util.isDarkTheme
import javax.inject.Inject

class StoriesIntroDialogFragment : DialogFragment() {
    @Inject lateinit var viewModelFactory: ViewModelProvider.Factory
    @Inject internal lateinit var mediaPickerLauncher: MediaPickerLauncher

    private lateinit var viewModel: StoriesIntroViewModel

    companion object {
        const val TAG = "STORIES_INTRO_DIALOG_FRAGMENT"

        @JvmStatic fun newInstance(site: SiteModel?): StoriesIntroDialogFragment {
            val args = Bundle()
            if (site != null) {
                args.putSerializable(WordPress.SITE, site)
            }
            return StoriesIntroDialogFragment().apply { arguments = args }
        }
    }

    override fun getTheme(): Int {
        return R.style.FeatureAnnouncementDialogFragment
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        viewModel = ViewModelProvider(this, viewModelFactory)
                .get(StoriesIntroViewModel::class.java)

        if (VERSION.SDK_INT >= VERSION_CODES.M) {
            val window: Window? = dialog.window
            window?.let {
                window.statusBarColor = dialog.context.getColorFromAttribute(attr.colorSurface)
                if (!resources.configuration.isDarkTheme()) {
                    window.decorView.systemUiVisibility = window.decorView
                            .systemUiVisibility or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
                }
            }
        }
        return dialog
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.stories_intro_dialog_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val site = requireArguments().getSerializable(WordPress.SITE) as SiteModel

        create_story_intro_button.setOnClickListener { viewModel.onCreateStoryButtonPressed() }
        stories_intro_back_button.setOnClickListener { viewModel.onBackButtonPressed() }

        story_image_first.setOnClickListener { viewModel.onStoryPreviewTapped1() }
        story_image_second.setOnClickListener { viewModel.onStoryPreviewTapped2() }

        viewModel.onCreateButtonClicked.observe(this, Observer {
            activity?.let {
                mediaPickerLauncher.showStoriesPhotoPickerForResultAndTrack(it, site)
            }
            dismiss()
        })

        viewModel.onDialogClosed.observe(this, Observer {
            dismiss()
        })

        viewModel.onStoryOpenRequested.observe(this, Observer { storyUrl ->
            ActivityLauncher.openUrlExternal(context, storyUrl)
        })

        viewModel.start()
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        (requireActivity().applicationContext as WordPress).component().inject(this)
    }
}
