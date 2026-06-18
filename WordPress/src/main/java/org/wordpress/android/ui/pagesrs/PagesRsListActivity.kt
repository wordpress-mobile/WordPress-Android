package org.wordpress.android.ui.pagesrs

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.ui.ActivityLauncher
import org.wordpress.android.ui.PagePostCreationSourcesDetail.PAGE_FROM_PAGES_LIST
import org.wordpress.android.ui.blaze.BlazeFlowSource
import org.wordpress.android.ui.compose.theme.AppThemeM3
import org.wordpress.android.ui.jetpackoverlay.JetpackFeatureRemovalPhaseHelper
import org.wordpress.android.ui.main.BaseAppCompatActivity
import org.wordpress.android.ui.mlp.ModalLayoutPickerFragment
import org.wordpress.android.ui.mlp.ModalLayoutPickerFragment.Companion.MODAL_LAYOUT_PICKER_TAG
import org.wordpress.android.ui.pagesrs.screens.PagesRsListScreen
import org.wordpress.android.util.ToastUtils
import org.wordpress.android.util.extensions.clipboardManager
import org.wordpress.android.util.extensions.setContent
import org.wordpress.android.viewmodel.mlp.ModalLayoutPickerViewModel
import org.wordpress.android.viewmodel.observeEvent
import javax.inject.Inject

@AndroidEntryPoint
class PagesRsListActivity : BaseAppCompatActivity() {
    @Inject lateinit var viewModelFactory: ViewModelProvider.Factory
    @Inject lateinit var jetpackFeatureRemovalPhaseHelper: JetpackFeatureRemovalPhaseHelper

    private val viewModel: PagesRsListViewModel by viewModels()
    private lateinit var mlpViewModel: ModalLayoutPickerViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mlpViewModel = ViewModelProvider(this, viewModelFactory)[ModalLayoutPickerViewModel::class.java]
        observeEvents()
        observeMlp()

        setContent {
            val tabStates by viewModel.tabStates.collectAsState()
            val isSearchActive by viewModel.isSearchActive.collectAsState()
            val isOpeningPage by viewModel.isOpeningPage.collectAsState()
            val searchQuery by viewModel.searchQuery.collectAsState()
            val authorFilter by viewModel.authorFilter.collectAsState()
            val pendingConfirmation by viewModel.pendingConfirmation.collectAsState()
            val parentPicker by viewModel.parentPicker.collectAsState()
            AppThemeM3 {
                PagesRsListScreen(
                    tabStates = tabStates,
                    isSearchActive = isSearchActive,
                    isOpeningPage = isOpeningPage,
                    searchQuery = searchQuery,
                    authorFilter = authorFilter,
                    isAuthorFilterSupported = viewModel.isAuthorFilterSupported,
                    avatarUrl = viewModel.avatarUrl,
                    confirmationDialog = PageRsConfirmationDialogState(
                        pending = pendingConfirmation,
                        onConfirm = viewModel::onConfirmPendingAction,
                        onDismiss = viewModel::onDismissPendingAction
                    ),
                    parentPicker = parentPicker,
                    snackbarMessages = viewModel.snackbarMessages,
                    onSearchOpen = viewModel::onSearchOpen,
                    onSearchQueryChanged = viewModel::onSearchQueryChanged,
                    onSearchClose = viewModel::onSearchClose,
                    onAuthorFilterChanged = viewModel::onAuthorFilterChanged,
                    onInitTab = viewModel::initTab,
                    onTabChanged = viewModel::onTabChanged,
                    onRefreshTab = { tab -> viewModel.refreshTab(tab, isUserRefresh = true) },
                    onLoadMore = viewModel::loadMorePages,
                    onNavigateBack = { onBackPressedDispatcher.onBackPressed() },
                    onPageClick = viewModel::openPage,
                    onPageMenuAction = viewModel::onPageMenuAction,
                    onParentSelected = viewModel::onParentSelected,
                    onParentSearchChanged = viewModel::onParentSearchChanged,
                    onLoadMoreParents = viewModel::onLoadMoreParents,
                    onParentPickerDismissed = viewModel::onParentPickerDismissed,
                    onAddNewPage = viewModel::onAddNewPage
                )
            }
        }
    }

    private fun observeEvents() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.events.collect { event -> handleEvent(event) }
            }
        }
    }

    private fun handleEvent(event: PageRsListEvent) {
        when (event) {
            is PageRsListEvent.EditPage ->
                ActivityLauncher.editPostOrPageForResult(this, event.site, event.page)
            is PageRsListEvent.CreateNewPage -> startCreatePageFlow()
            is PageRsListEvent.ViewPage -> ActivityLauncher.openUrlExternal(this, event.url)
            is PageRsListEvent.SharePage ->
                ActivityLauncher.openShareIntent(this, event.url, event.title)
            is PageRsListEvent.CopyPageUrl -> copyUrlToClipboard(event.url)
            is PageRsListEvent.PromoteWithBlaze ->
                ActivityLauncher.openPromoteWithBlaze(this, event.page, BlazeFlowSource.PAGES_LIST)
            is PageRsListEvent.ShowToast -> ToastUtils.showToast(this, event.messageResId)
            is PageRsListEvent.Finish -> finish()
        }
    }

    private fun copyUrlToClipboard(url: String) {
        clipboardManager?.setPrimaryClip(ClipData.newPlainText(CLIPBOARD_URL_LABEL, url))
        // Android 13+ shows its own confirmation UI when the clipboard changes.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            ToastUtils.showToast(this, R.string.media_edit_copy_url_toast)
        }
    }

    private fun startCreatePageFlow() {
        if (mlpViewModel.canShowModalLayoutPicker() &&
            jetpackFeatureRemovalPhaseHelper.shouldShowTemplateSelectionInPages()
        ) {
            mlpViewModel.createPageFlowTriggered()
        } else {
            val site = viewModel.site ?: return
            launchNewPageEditor(site, title = "", template = null)
        }
    }

    private fun launchNewPageEditor(site: SiteModel, title: String, template: String?) {
        ActivityLauncher.addNewPageForResult(
            this, site, title, "", template, PAGE_FROM_PAGES_LIST
        )
    }

    private fun observeMlp() {
        mlpViewModel.onCreateNewPageRequested.observe(this) { request ->
            val site = viewModel.site ?: return@observe
            launchNewPageEditor(site, request.title, request.template)
        }
        mlpViewModel.isModalLayoutPickerShowing.observeEvent(this) { isShowing ->
            val fm = supportFragmentManager
            val existing = fm.findFragmentByTag(MODAL_LAYOUT_PICKER_TAG) as ModalLayoutPickerFragment?
            if (isShowing && existing == null) {
                ModalLayoutPickerFragment().show(fm, MODAL_LAYOUT_PICKER_TAG)
            } else if (!isShowing && existing != null) {
                existing.dismiss()
            }
        }
    }

    companion object {
        private const val CLIPBOARD_URL_LABEL = "Page URL"

        fun createIntent(context: Context) = Intent(context, PagesRsListActivity::class.java)
    }
}
