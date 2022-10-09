package org.wordpress.android.ui.bloggingprompts

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import org.wordpress.android.R
import org.wordpress.android.databinding.BloggingPromptsListFragmentBinding
import org.wordpress.android.ui.ViewPagerFragment
import org.wordpress.android.ui.bloggingprompts.BloggingPromptsUiUtils.gone
import org.wordpress.android.ui.bloggingprompts.BloggingPromptsUiUtils.showImage
import org.wordpress.android.ui.bloggingprompts.BloggingPromptsUiUtils.showText
import org.wordpress.android.ui.bloggingprompts.BloggingPromptsUiUtils.visible
import org.wordpress.android.util.extensions.exhaustive
import java.util.concurrent.TimeUnit

class BloggingPromptsListFragment : ViewPagerFragment() {
    private lateinit var binding: BloggingPromptsListFragmentBinding

    override fun getScrollableViewForUniqueIdProvision(): View = binding.tempText

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = BloggingPromptsListFragmentBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val section = arguments?.getSerializable(LIST_TYPE) as? PromptSection
                ?: PromptSection.ALL
        binding.tempText.text = section.name
        showContent()

        // DUMMY LOGIC
        showLoading()
        binding.root.postDelayed(Runnable {
            if (!isAdded) return@Runnable
            when (section) {
                PromptSection.ALL -> showEmpty()
                PromptSection.NOT_ANSWERED -> showError()
                PromptSection.ANSWERED -> showNoConnection()
            }.exhaustive
        }, TimeUnit.SECONDS.toMillis(2))
    }

    private fun showContent() {
        with(binding) {
            tempText.visible()
            actionableEmptyView.gone()
        }
    }

    private fun showEmpty() {
        with(binding) {
            tempText.gone()
            with(actionableEmptyView) {
                visible()
                image.showImage(R.drawable.img_illustration_empty_results_216dp)
                title.showText(R.string.blogging_prompts_state_empty_title)
                subtitle.gone()
            }
        }
    }

    private fun showError() {
        with(binding) {
            tempText.gone()
            with(actionableEmptyView) {
                visible()
                image.showImage(R.drawable.img_illustration_empty_results_216dp)
                title.showText(R.string.blogging_prompts_state_error_title)
                subtitle.showText(R.string.blogging_prompts_state_error_subtitle)
            }
        }
    }

    private fun showNoConnection() {
        with(binding) {
            tempText.gone()
            with(actionableEmptyView) {
                visible()
                image.showImage(R.drawable.img_illustration_cloud_off_152dp)
                title.showText(R.string.blogging_prompts_state_no_connection_title)
                subtitle.showText(R.string.blogging_prompts_state_no_connection_subtitle)
            }
        }
    }

    fun showLoading() {
        with(binding) {
            tempText.gone()
            with(actionableEmptyView) {
                visible()
                image.gone()
                title.showText(R.string.blogging_prompts_state_loading_title)
                subtitle.gone()
            }
        }
    }

    companion object {
        const val LIST_TYPE = "type_key"

        fun newInstance(section: PromptSection): BloggingPromptsListFragment {
            val fragment = BloggingPromptsListFragment()
            val bundle = Bundle()
            bundle.putSerializable(LIST_TYPE, section)
            fragment.arguments = bundle
            return fragment
        }
    }
}
