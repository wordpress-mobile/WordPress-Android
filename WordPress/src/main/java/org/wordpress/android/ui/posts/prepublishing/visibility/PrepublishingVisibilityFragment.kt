package org.wordpress.android.ui.posts.prepublishing.visibility

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.prepublishing_visibility_fragment.*
import org.wordpress.android.R
import org.wordpress.android.R.string
import org.wordpress.android.WordPress
import org.wordpress.android.ui.posts.EditPostRepository
import org.wordpress.android.ui.posts.EditPostSettingsFragment.EditPostActivityHook
import org.wordpress.android.ui.posts.PostSettingsInputDialogFragment
import javax.inject.Inject

class PrepublishingVisibilityFragment : Fragment() {
    @Inject internal lateinit var viewModelFactory: ViewModelProvider.Factory
    private lateinit var viewModel: PrepublishingVisibilityViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (requireNotNull(activity).application as WordPress).component().inject(this)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.prepublishing_visibility_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        visibility_recycler_view.layoutManager = LinearLayoutManager(requireActivity())
        visibility_recycler_view.adapter = PrepublishingVisibilityAdapter(requireActivity())

        initViewModel()
    }

    private fun initViewModel() {
        viewModel = ViewModelProviders.of(this, viewModelFactory)
                .get(PrepublishingVisibilityViewModel::class.java)

        viewModel.uiState.observe(this, Observer { uiState ->
            (visibility_recycler_view.adapter as PrepublishingVisibilityAdapter).update(uiState)
        })

        viewModel.showPasswordDialog.observe(this, Observer { event ->
            event?.applyIfNotHandled {
                showPostPasswordDialog()
            }
        })

        viewModel.start(getEditPostRepository())
    }

    private fun getEditPostRepository(): EditPostRepository {
        val editPostActivityHook = requireNotNull(getEditPostActivityHook()) {
            "This is possibly null because it's " +
                    "called during config changes."
        }

        return editPostActivityHook.editPostRepository
    }

    private fun showPostPasswordDialog() {
        val dialog = PostSettingsInputDialogFragment.newInstance(
                getEditPostRepository().password, getString(string.password),
                getString(string.post_settings_password_dialog_hint), false
        )
        dialog.setPostSettingsInputDialogListener { input -> viewModel.onPostPasswordChanged(input) }

        fragmentManager?.let {
            dialog.show(it, null)
        }
    }

    private fun getEditPostActivityHook(): EditPostActivityHook? {
        val activity = activity ?: return null
        return if (activity is EditPostActivityHook) {
            activity
        } else {
            throw RuntimeException("$activity must implement EditPostActivityHook")
        }
    }

    companion object {
        const val TAG = "prepublishing_visibility_fragment_tag"
        fun newInstance() = PrepublishingVisibilityFragment()
    }
}
