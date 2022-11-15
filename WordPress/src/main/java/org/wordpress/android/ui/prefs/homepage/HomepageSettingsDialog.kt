package org.wordpress.android.ui.prefs.homepage

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.Gravity
import android.view.Menu
import android.view.View
import android.widget.Button
import android.widget.PopupMenu
import android.widget.RadioGroup
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.databinding.SiteSettingsHomepageDialogBinding
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.ui.utils.UiHelpers
import org.wordpress.android.util.extensions.getColorResIdFromAttribute
import org.wordpress.android.viewmodel.observeEvent
import javax.inject.Inject

class HomepageSettingsDialog : DialogFragment() {
    @Inject lateinit var viewModelFactory: ViewModelProvider.Factory
    @Inject lateinit var uiHelpers: UiHelpers
    private lateinit var viewModel: HomepageSettingsViewModel
    private var siteId: Int? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        var isClassicBlog: Boolean? = null
        var pageOnFrontId: Long? = null
        var pageForPostsId: Long? = null
        (arguments ?: savedInstanceState)?.let { bundle ->
            siteId = bundle.getInt(KEY_SITE_ID)
            isClassicBlog = bundle.get(KEY_IS_CLASSIC_BLOG)?.let { it as Boolean }
            pageOnFrontId = bundle.get(KEY_PAGE_ON_FRONT)?.let { it as Long }
            pageForPostsId = bundle.get(KEY_PAGE_FOR_POSTS)?.let { it as Long }
        } ?: throw IllegalArgumentException("Site has to be initialized")
        val builder = MaterialAlertDialogBuilder(requireActivity())
        builder.setPositiveButton(R.string.site_settings_accept_homepage) { _, _ -> }
        builder.setNegativeButton(R.string.cancel) { _, _ -> }
        with(SiteSettingsHomepageDialogBinding.inflate(requireActivity().layoutInflater)) {
            homepageSettingsRadioGroup.setOnCheckedChangeListener { _, checkedId ->
                when (checkedId) {
                    R.id.classic_blog -> viewModel.classicBlogSelected()
                    R.id.static_homepage -> viewModel.staticHomepageSelected()
                }
            }
            builder.setView(root)

            viewModel = ViewModelProvider(this@HomepageSettingsDialog, viewModelFactory)
                    .get(HomepageSettingsViewModel::class.java)
            viewModel.uiState.observe(this@HomepageSettingsDialog) { uiState ->
                uiState?.let {
                    loadingPages.visibility = if (uiState.isLoading) View.VISIBLE else View.GONE
                    enablePositiveButton(uiState.isSaveEnabled)
                    if (uiState.error != null) {
                        loadingError.visibility = View.VISIBLE
                        loadingError.setText(uiState.error)
                    } else {
                        loadingError.visibility = View.GONE
                        loadingError.text = null
                    }
                    isClassicBlogState(uiState)
                }
            }
        }
        viewModel.dismissDialogEvent.observeEvent(this, {
            requireDialog().dismiss()
        })
        viewModel.start(requireNotNull(siteId), isClassicBlog, pageForPostsId, pageOnFrontId)
        return builder.create()
    }

    private fun SiteSettingsHomepageDialogBinding.isClassicBlogState(
        uiState: HomepageSettingsUiState
    ) {
        when (uiState.isClassicBlogState) {
            true -> {
                homepageSettingsRadioGroup.checkIfNotChecked(R.id.classic_blog)
                dropdownContainer.visibility = View.GONE
            }
            false -> {
                homepageSettingsRadioGroup.checkIfNotChecked(R.id.static_homepage)
                if (uiState.pageForPostsState != null && uiState.pageOnFrontState != null) {
                    dropdownContainer.visibility = View.VISIBLE
                    setupDropdownItem(
                            uiState.pageOnFrontState,
                            selectedHomepage,
                            viewModel::onPageOnFrontDialogOpened,
                            viewModel::onPageOnFrontSelected
                    )
                    setupDropdownItem(
                            uiState.pageForPostsState,
                            selectedPostsPage,
                            viewModel::onPageForPostsDialogOpened,
                            viewModel::onPageForPostsSelected
                    )
                } else {
                    dropdownContainer.visibility = View.GONE
                }
            }
        }
    }

    private fun enablePositiveButton(enabled: Boolean) {
        val button = (dialog as AlertDialog).getButton(AlertDialog.BUTTON_POSITIVE)
        button.isEnabled = enabled
        val textColor = if (enabled) {
            requireContext().getColorResIdFromAttribute(R.attr.colorPrimary)
        } else {
            R.color.material_on_surface_disabled
        }
        button.setTextColor(ContextCompat.getColor(requireContext(), textColor))
    }

    override fun onResume() {
        super.onResume()
        val d = dialog as AlertDialog?
        if (d != null) {
            val positiveButton = d.getButton(Dialog.BUTTON_POSITIVE) as Button
            positiveButton.setOnClickListener {
                viewModel.onAcceptClicked()
            }
            val negativeButton = d.getButton(Dialog.BUTTON_NEGATIVE) as Button
            negativeButton.setOnClickListener {
                viewModel.onDismissClicked()
            }
        }
    }

    private fun setupDropdownItem(
        selectorUiModel: HomepageSettingsSelectorUiState,
        textView: TextView,
        onClickAction: () -> Unit,
        onPageSelectedAction: (id: Int) -> Boolean
    ) {
        uiHelpers.setTextOrHide(textView, selectorUiModel.selectedItem)
        textView.isSelected = selectorUiModel.isHighlighted
        if (selectorUiModel.isExpanded) {
            val popupMenu = PopupMenu(requireContext(), textView, Gravity.END)
            for (page in selectorUiModel.data) {
                popupMenu.menu.add(Menu.NONE, page.id, Menu.NONE, page.title)
            }
            popupMenu.setOnDismissListener {
                viewModel.onDialogHidden()
            }
            popupMenu.setOnMenuItemClickListener { menuItem ->
                onPageSelectedAction(menuItem.itemId)
            }
            popupMenu.show()
        } else {
            textView.setOnClickListener {
                onClickAction()
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putSerializable(KEY_SITE_ID, siteId)
        viewModel.isClassicBlog()?.let {
            outState.putBoolean(KEY_IS_CLASSIC_BLOG, it)
        }
        viewModel.getSelectedPageOnFrontId()?.let {
            outState.putLong(KEY_PAGE_ON_FRONT, it)
        }
        viewModel.getSelectedPageForPostsId()?.let {
            outState.putLong(KEY_PAGE_FOR_POSTS, it)
        }
    }

    private fun RadioGroup.checkIfNotChecked(id: Int) {
        if (checkedRadioButtonId != id) {
            check(id)
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        (requireActivity().application as WordPress).component().inject(this)
    }

    companion object {
        private const val KEY_SITE_ID = "site_id"
        private const val KEY_IS_CLASSIC_BLOG = "is_classic_blog"
        private const val KEY_PAGE_ON_FRONT = "page_on_front"
        private const val KEY_PAGE_FOR_POSTS = "page_for_posts"
        fun newInstance(siteModel: SiteModel): HomepageSettingsDialog {
            val args = Bundle()
            args.putInt(KEY_SITE_ID, siteModel.id)
            val dialog = HomepageSettingsDialog()
            dialog.arguments = args
            return dialog
        }
    }
}
