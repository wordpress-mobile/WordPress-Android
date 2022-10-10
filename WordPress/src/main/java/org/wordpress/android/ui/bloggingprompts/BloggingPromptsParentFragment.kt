package org.wordpress.android.ui.bloggingprompts

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.google.android.material.tabs.TabLayout.OnTabSelectedListener
import com.google.android.material.tabs.TabLayout.Tab
import com.google.android.material.tabs.TabLayoutMediator
import org.wordpress.android.R
import org.wordpress.android.databinding.BloggingPromptsParentFragmentBinding
import org.wordpress.android.ui.bloggingprompts.PromptSection.ALL
import org.wordpress.android.ui.bloggingprompts.PromptSection.ANSWERED
import org.wordpress.android.ui.bloggingprompts.PromptSection.NOT_ANSWERED

class BloggingPromptsParentFragment : Fragment() {
    private lateinit var binding: BloggingPromptsParentFragmentBinding

    private val viewModel: BloggingPromptsParentViewModel by activityViewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = BloggingPromptsParentFragmentBinding.inflate(inflater)
        setupToolbar(binding)
        setupTabLayout(binding)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.onOpen()
    }

    private fun setupToolbar(binding: BloggingPromptsParentFragmentBinding) {
        with(binding) {
            with(activity as AppCompatActivity) {
                setSupportActionBar(toolbar)

                title = getString(R.string.blogging_prompts_title)
                supportActionBar?.setDisplayShowTitleEnabled(true)
                supportActionBar?.setDisplayHomeAsUpEnabled(true)
            }
        }
    }

    private fun setupTabLayout(binding: BloggingPromptsParentFragmentBinding) {
        with(binding) {
            val adapter = BloggingPromptsPagerAdapter(this@BloggingPromptsParentFragment)
            promptPager.adapter = adapter
            TabLayoutMediator(tabLayout, promptPager) { tab, position ->
                tab.text = adapter.getTabTitle(position)
            }.attach()
            tabLayout.addOnTabSelectedListener(SelectedTabListener(viewModel))
        }
    }

    companion object {
        const val LIST_TYPE = "type_key"

        fun newInstance(section: PromptSection): BloggingPromptsParentFragment {
            val fragment = BloggingPromptsParentFragment()
            val bundle = Bundle()
            bundle.putSerializable(LIST_TYPE, section)
            fragment.arguments = bundle
            return fragment
        }
    }
}

private val promptsSections = listOf(ALL, ANSWERED, NOT_ANSWERED)

enum class PromptSection(@StringRes val titleRes: Int) {
    ALL(R.string.blogging_prompts_tab_all),
    ANSWERED(R.string.blogging_prompts_tab_answered),
    NOT_ANSWERED(R.string.blogging_prompts_tab_not_answered)
}

class BloggingPromptsPagerAdapter(private val parent: BloggingPromptsParentFragment) : FragmentStateAdapter(parent) {
    override fun getItemCount(): Int = promptsSections.size

    override fun createFragment(position: Int): Fragment {
        return BloggingPromptsListFragment.newInstance(promptsSections[position])
    }

    fun getTabTitle(position: Int): CharSequence {
        return parent.context?.getString(promptsSections[position].titleRes).orEmpty()
    }
}

private class SelectedTabListener(val viewModel: BloggingPromptsParentViewModel) : OnTabSelectedListener {
    override fun onTabReselected(tab: Tab?) = Unit

    override fun onTabUnselected(tab: Tab?) = Unit

    override fun onTabSelected(tab: Tab) {
        viewModel.onSectionSelected(promptsSections[tab.position])
    }
}
