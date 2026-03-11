package org.wordpress.android.ui.postsrs

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import org.wordpress.android.R
import org.wordpress.android.ui.compose.theme.AppThemeM3
import org.wordpress.android.ui.main.BaseAppCompatActivity
import org.wordpress.android.ui.mediapicker.MediaPickerActivity
import org.wordpress.android.ui.mediapicker.MediaPickerSetup
import org.wordpress.android.ui.mediapicker.MediaType
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import org.wordpress.android.ui.photopicker.MediaPickerConstants
import org.wordpress.android.ui.postsrs.screens.PostRsSettingsScreen
import org.wordpress.android.ui.postsrs.terms.TermSelectionActivity
import org.wordpress.android.ui.postsrs.terms.TermSelectionViewModel
import org.wordpress.android.util.ToastUtils
import org.wordpress.android.util.extensions.setContent
import javax.inject.Inject

@AndroidEntryPoint
class PostRsSettingsActivity : BaseAppCompatActivity() {
    private val viewModel: PostRsSettingsViewModel by viewModels()

    @Inject
    lateinit var selectedSiteRepository: SelectedSiteRepository

    private lateinit var mediaPickerLauncher:
        ActivityResultLauncher<Intent>

    private lateinit var categorySelectionLauncher:
        ActivityResultLauncher<Intent>

    private lateinit var tagSelectionLauncher:
        ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        registerMediaPickerLauncher()
        registerTermSelectionLaunchers()

        val controller = WindowInsetsControllerCompat(
            window, window.decorView
        )
        controller.hide(WindowInsetsCompat.Type.statusBars())
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat
                .BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        observeEvents()

        setContent {
            val uiState by viewModel.uiState.collectAsState()
            AppThemeM3 {
                PostRsSettingsScreen(
                    uiState = uiState,
                    onNavigateBack = viewModel::onBackClicked,
                    onRetry = viewModel::retry,
                    onRetryField = viewModel::retryField,
                    onStatusClicked = viewModel::onStatusClicked,
                    onStatusSelected = viewModel::onStatusSelected,
                    onPasswordClicked = viewModel::onPasswordClicked,
                    onPasswordSet = viewModel::onPasswordSet,
                    onStickyToggled = viewModel::onStickyToggled,
                    onSlugClicked = viewModel::onSlugClicked,
                    onSlugSet = viewModel::onSlugSet,
                    onExcerptClicked = viewModel::onExcerptClicked,
                    onExcerptSet = viewModel::onExcerptSet,
                    onFormatClicked = viewModel::onFormatClicked,
                    onFormatSelected = viewModel::onFormatSelected,
                    onDateClicked = viewModel::onDateClicked,
                    onDateSelected = viewModel::onDateSelected,
                    onTimeSelected = viewModel::onTimeSelected,
                    onAuthorClicked = viewModel::onAuthorClicked,
                    onAuthorSelected = viewModel::onAuthorSelected,
                    onCategoriesClicked =
                        viewModel::onCategoriesClicked,
                    onTagsClicked =
                        viewModel::onTagsClicked,
                    onFeaturedImageClicked =
                        viewModel::onFeaturedImageClicked,
                    onFeaturedImageRemoved =
                        viewModel::onFeaturedImageRemoved,
                    onLoadMoreAuthors = viewModel::loadMoreAuthors,
                    onSaveClicked = viewModel::onSaveClicked,
                    onDismissDialog = viewModel::onDismissDialog,
                    onDiscardConfirmed = viewModel::onDiscardConfirmed,
                )
            }
        }
    }

    private fun observeEvents() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.events.collect { event ->
                    when (event) {
                        is PostRsSettingsEvent.Finish -> finish()
                        is PostRsSettingsEvent.FinishWithChanges -> {
                            setResult(RESULT_OK)
                            finish()
                        }
                        is PostRsSettingsEvent.LaunchMediaPicker ->
                            launchMediaPicker()
                        is PostRsSettingsEvent
                            .LaunchCategorySelection ->
                            launchTermSelection(
                                categorySelectionLauncher,
                                isCategories = true,
                                event.selectedIds,
                            )
                        is PostRsSettingsEvent
                            .LaunchTagSelection ->
                            launchTermSelection(
                                tagSelectionLauncher,
                                isCategories = false,
                                event.selectedIds,
                            )
                        is PostRsSettingsEvent.ShowSnackbar ->
                            ToastUtils.showToast(
                                this@PostRsSettingsActivity,
                                event.message
                            )
                    }
                }
            }
        }
    }

    private fun registerMediaPickerLauncher() {
        mediaPickerLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == RESULT_OK) {
                val mediaId = result.data?.getLongExtra(
                    MediaPickerConstants.EXTRA_MEDIA_ID, 0L
                ) ?: 0L
                if (mediaId > 0L) {
                    viewModel.onFeaturedImageSelected(mediaId)
                }
            }
        }
    }

    private fun registerTermSelectionLaunchers() {
        categorySelectionLauncher =
            registerTermLauncher(
                viewModel::onCategoriesSelected
            )
        tagSelectionLauncher =
            registerTermLauncher(
                viewModel::onTagsSelected
            )
    }

    private fun registerTermLauncher(
        onResult: (LongArray) -> Unit,
    ) = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val ids = result.data?.getLongArrayExtra(
                TermSelectionViewModel
                    .RESULT_SELECTED_IDS
            ) ?: return@registerForActivityResult
            onResult(ids)
        }
    }

    private fun launchTermSelection(
        launcher: ActivityResultLauncher<Intent>,
        isCategories: Boolean,
        selectedIds: List<Long>,
    ) {
        launcher.launch(
            TermSelectionActivity.createIntent(
                this,
                isCategories = isCategories,
                selectedIds = selectedIds.toLongArray()
            )
        )
    }

    private fun launchMediaPicker() {
        val site =
            selectedSiteRepository.getSelectedSite()
                ?: return
        val setup = MediaPickerSetup(
            primaryDataSource =
                MediaPickerSetup.DataSource.WP_LIBRARY,
            availableDataSources = emptySet(),
            canMultiselect = false,
            requiresPhotosVideosPermissions = false,
            requiresMusicAudioPermissions = false,
            allowedTypes = setOf(MediaType.IMAGE),
            cameraSetup =
                MediaPickerSetup.CameraSetup.HIDDEN,
            systemPickerEnabled = false,
            editingEnabled = false,
            queueResults = false,
            defaultSearchView = false,
            title = R.string.photo_picker_title
        )
        val intent = MediaPickerActivity.buildIntent(
            this, setup, site
        )
        mediaPickerLauncher.launch(intent)
    }

    companion object {
        fun createIntent(context: Context, postId: Long): Intent {
            return Intent(context, PostRsSettingsActivity::class.java)
                .putExtra(PostRsSettingsViewModel.EXTRA_POST_ID, postId)
        }
    }
}
