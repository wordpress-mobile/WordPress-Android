package org.wordpress.android.ui.bloggingprompts.onboarding

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.button.MaterialButton
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.databinding.BloggingPromptsOnboardingDialogFragmentBinding
import org.wordpress.android.ui.ActivityLauncher
import org.wordpress.android.ui.bloggingprompts.onboarding.BloggingPromptsOnboardingAction.OpenEditor
import org.wordpress.android.util.extensions.exhaustive
import org.wordpress.android.util.extensions.setStatusBarAsSurfaceColor
import javax.inject.Inject

class BloggingPromptsOnboardingDialogFragment : DialogFragment() {
    @Inject lateinit var viewModelFactory: ViewModelProvider.Factory
    private lateinit var viewModel: BloggingPromptsOnboardingViewModel

    companion object {
        const val TAG = "BLOGGING_PROMPTS_ONBOARDING_DIALOG_FRAGMENT"

        @JvmStatic
        fun newInstance(): BloggingPromptsOnboardingDialogFragment = BloggingPromptsOnboardingDialogFragment()
    }

    override fun getTheme(): Int {
        return R.style.BloggingPromptsOnboardingDialogFragment
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        viewModel = ViewModelProvider(this, viewModelFactory)
                .get(BloggingPromptsOnboardingViewModel::class.java)
        dialog.setStatusBarAsSurfaceColor()
        return dialog
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = BloggingPromptsOnboardingDialogFragmentBinding.inflate(inflater).root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val binding = BloggingPromptsOnboardingDialogFragmentBinding.bind(view)
        setupTryNow(binding.tryNow)
        setupActionObserver()
        viewModel.start()
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        (requireActivity().applicationContext as WordPress).component().inject(this)
    }

    private fun setupTryNow(tryNow: MaterialButton) {
        tryNow.setOnClickListener { viewModel.onTryNow() }
    }

    private fun setupActionObserver() {
        viewModel.action.observe(this, { action ->
            when (action) {
                is OpenEditor -> ActivityLauncher.openEditorInNewStack(activity)
            }.exhaustive
        })
    }
}
