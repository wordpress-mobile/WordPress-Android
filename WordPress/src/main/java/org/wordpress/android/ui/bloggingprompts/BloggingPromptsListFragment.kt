package org.wordpress.android.ui.bloggingprompts

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import org.wordpress.android.databinding.BloggingPromptsListFragmentBinding
import org.wordpress.android.ui.ViewPagerFragment

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
