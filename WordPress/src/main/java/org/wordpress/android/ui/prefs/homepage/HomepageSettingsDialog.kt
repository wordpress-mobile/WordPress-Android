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
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.android.synthetic.main.site_settings_homepage_dialog.view.*
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.ui.utils.UiHelpers
import org.wordpress.android.util.getColorResIdFromAttribute
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
        val view = View.inflate(
                activity,
                R.layout.site_settings_homepage_dialog,
                null
        )
        view.apply {
            homepage_settings_radio_group.setOnCheckedChangeListener { _, checkedId ->
                when (checkedId) {
                    R.id.classic_blog -> viewModel.classicBlogSelected()
                    R.id.static_homepage -> viewModel.staticHomepageSelected()
                }
            }
        }
        val builder = MaterialAlertDialogBuilder(activity)
        builder.setPositiveButton(R.string.site_settings_accept_homepage) { _, _ -> }
        builder.setNegativeButton(R.string.cancel) { _, _ -> }
        builder.setView(view)

        viewModel = ViewModelProviders.of(this, viewModelFactory)
                .get(HomepageSettingsViewModel::class.java)
        viewModel.uiState.observe(this, Observer { uiState ->
            uiState?.let {
                view.apply {
                    loading_pages.visibility = if (uiState.isLoading) View.VISIBLE else View.GONE
                    if (uiState.isDisabled) {
                        enablePositiveButton(false)
                        homepage_settings_radio_group.isEnabled = false
                        selected_homepage.isEnabled = false
                        selected_posts_page.isEnabled = false
                    } else {
                        enablePositiveButton(true)
                        homepage_settings_radio_group.isEnabled = true
                        selected_homepage.isEnabled = true
                        selected_posts_page.isEnabled = true
                    }
                    if (uiState.error != null) {
                        loading_error.visibility = View.VISIBLE
                        loading_error.setText(uiState.error)
                    } else {
                        loading_error.visibility = View.GONE
                        loading_error.text = null
                    }
                    when (uiState.isClassicBlogState) {
                        true -> {
                            homepage_settings_radio_group.checkIfNotChecked(R.id.classic_blog)
                            dropdown_container.visibility = View.GONE
                            enablePositiveButton(true)
                        }
                        false -> {
                            homepage_settings_radio_group.checkIfNotChecked(R.id.static_homepage)
                            if (uiState.pageForPostsState != null && uiState.pageOnFrontState != null) {
                                enablePositiveButton(true)
                                dropdown_container.visibility = View.VISIBLE
                                setupDropdownItem(
                                        uiState.pageOnFrontState,
                                        selected_homepage,
                                        viewModel::onPageOnFrontDialogOpened,
                                        viewModel::onPageOnFrontSelected
                                )
                                setupDropdownItem(
                                        uiState.pageForPostsState,
                                        selected_posts_page,
                                        viewModel::onPageForPostsDialogOpened,
                                        viewModel::onPageForPostsSelected
                                )
                            } else {
                                enablePositiveButton(false)
                                dropdown_container.visibility = View.GONE
                            }
                        }
                    }
                }
            }
        })
        viewModel.dismissDialogEvent.observe(this, Observer {
            it?.applyIfNotHandled {
                requireDialog().dismiss()
            }
        })
        viewModel.start(requireNotNull(siteId), isClassicBlog, pageForPostsId, pageOnFrontId)
        return builder.create()
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
