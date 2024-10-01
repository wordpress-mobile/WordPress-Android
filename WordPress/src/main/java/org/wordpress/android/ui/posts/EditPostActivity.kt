@file:Suppress("DEPRECATION")
package org.wordpress.android.ui.posts

import android.app.ProgressDialog
import android.content.DialogInterface
import android.content.Intent
import android.content.res.Configuration
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.preference.PreferenceManager
import android.text.Editable
import android.text.TextUtils
import android.view.DragEvent
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.MarginLayoutParams
import android.webkit.MimeTypeMap
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.util.Consumer
import androidx.core.util.Pair
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import androidx.lifecycle.LiveData
import androidx.viewpager.widget.ViewPager.SimpleOnPageChangeListener
import com.automattic.android.tracks.crashlogging.CrashLogging
import com.automattic.android.tracks.crashlogging.JsException
import com.automattic.android.tracks.crashlogging.JsExceptionCallback
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import kotlinx.parcelize.parcelableCreator
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.BuildConfig
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.WordPress.Companion.getContext
import org.wordpress.android.analytics.AnalyticsTracker
import org.wordpress.android.analytics.AnalyticsTracker.Stat
import org.wordpress.android.editor.AztecEditorFragment
import org.wordpress.android.editor.EditorEditMediaListener
import org.wordpress.android.editor.EditorFragmentAbstract
import org.wordpress.android.editor.EditorFragmentAbstract.EditorDragAndDropListener
import org.wordpress.android.editor.EditorFragmentAbstract.EditorFragmentListener
import org.wordpress.android.editor.EditorFragmentAbstract.EditorFragmentNotAddedException
import org.wordpress.android.editor.EditorFragmentActivity
import org.wordpress.android.editor.EditorImageMetaData
import org.wordpress.android.editor.EditorImagePreviewListener
import org.wordpress.android.editor.EditorImageSettingsListener
import org.wordpress.android.editor.EditorMediaUploadListener
import org.wordpress.android.editor.EditorMediaUtils
import org.wordpress.android.editor.EditorThemeUpdateListener
import org.wordpress.android.editor.ExceptionLogger
import org.wordpress.android.editor.gutenberg.DialogVisibility
import org.wordpress.android.editor.gutenberg.GutenbergEditorFragment
import org.wordpress.android.editor.gutenberg.GutenbergNetworkConnectionListener
import org.wordpress.android.editor.gutenberg.GutenbergPropsBuilder
import org.wordpress.android.editor.gutenberg.GutenbergWebViewAuthorizationData
import org.wordpress.android.editor.savedinstance.SavedInstanceDatabase
import org.wordpress.android.editor.savedinstance.SavedInstanceDatabase.Companion.getDatabase
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.action.AccountAction
import org.wordpress.android.fluxc.generated.AccountActionBuilder
import org.wordpress.android.fluxc.generated.EditorThemeActionBuilder
import org.wordpress.android.fluxc.generated.PostActionBuilder
import org.wordpress.android.fluxc.generated.SiteActionBuilder
import org.wordpress.android.fluxc.model.AccountModel
import org.wordpress.android.fluxc.model.CauseOfOnPostChanged
import org.wordpress.android.fluxc.model.EditorTheme
import org.wordpress.android.fluxc.model.EditorThemeSupport
import org.wordpress.android.fluxc.model.MediaModel
import org.wordpress.android.fluxc.model.MediaModel.MediaUploadState
import org.wordpress.android.fluxc.model.PostImmutableModel
import org.wordpress.android.fluxc.model.PostModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.post.PostStatus
import org.wordpress.android.fluxc.network.UserAgent
import org.wordpress.android.fluxc.network.rest.wpcom.site.PrivateAtomicCookie
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.fluxc.store.AccountStore.OnAccountChanged
import org.wordpress.android.fluxc.store.EditorThemeStore
import org.wordpress.android.fluxc.store.EditorThemeStore.FetchEditorThemePayload
import org.wordpress.android.fluxc.store.EditorThemeStore.OnEditorThemeChanged
import org.wordpress.android.fluxc.store.MediaStore
import org.wordpress.android.fluxc.store.MediaStore.MediaError
import org.wordpress.android.fluxc.store.MediaStore.MediaErrorType
import org.wordpress.android.fluxc.store.MediaStore.OnMediaChanged
import org.wordpress.android.fluxc.store.MediaStore.OnMediaListFetched
import org.wordpress.android.fluxc.store.MediaStore.OnMediaUploaded
import org.wordpress.android.fluxc.store.PostStore
import org.wordpress.android.fluxc.store.PostStore.OnPostChanged
import org.wordpress.android.fluxc.store.PostStore.OnPostUploaded
import org.wordpress.android.fluxc.store.PostStore.RemotePostPayload
import org.wordpress.android.fluxc.store.QuickStartStore
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.fluxc.store.SiteStore.OnPrivateAtomicCookieFetched
import org.wordpress.android.fluxc.store.UploadStore
import org.wordpress.android.fluxc.store.bloggingprompts.BloggingPromptsStore
import org.wordpress.android.fluxc.tools.FluxCImageLoader
import org.wordpress.android.imageeditor.preview.PreviewImageFragment
import org.wordpress.android.imageeditor.preview.PreviewImageFragment.Companion.EditImageData.InputData
import org.wordpress.android.networking.ConnectionChangeReceiver.ConnectionChangeEvent
import org.wordpress.android.support.ZendeskHelper
import org.wordpress.android.ui.ActivityId
import org.wordpress.android.ui.ActivityLauncher
import org.wordpress.android.ui.LocaleAwareActivity
import org.wordpress.android.ui.PrivateAtCookieRefreshProgressDialog.Companion.dismissIfNecessary
import org.wordpress.android.ui.PrivateAtCookieRefreshProgressDialog.Companion.isShowing
import org.wordpress.android.ui.PrivateAtCookieRefreshProgressDialog.Companion.showIfNecessary
import org.wordpress.android.ui.PrivateAtCookieRefreshProgressDialog.PrivateAtCookieProgressDialogOnDismissListener
import org.wordpress.android.ui.RequestCodes
import org.wordpress.android.ui.Shortcut
import org.wordpress.android.ui.WPWebViewActivity
import org.wordpress.android.ui.history.HistoryDetailContainerFragment.KEY_REVISION
import org.wordpress.android.ui.history.HistoryListItem.Revision
import org.wordpress.android.ui.jetpackoverlay.JetpackFeatureRemovalPhaseHelper
import org.wordpress.android.ui.media.MediaBrowserActivity
import org.wordpress.android.ui.media.MediaBrowserType
import org.wordpress.android.ui.media.MediaPreviewActivity
import org.wordpress.android.ui.media.MediaSettingsActivity
import org.wordpress.android.ui.pages.SnackbarMessageHolder
import org.wordpress.android.ui.photopicker.MediaPickerConstants
import org.wordpress.android.ui.photopicker.MediaPickerLauncher
import org.wordpress.android.ui.photopicker.PhotoPickerFragment.PhotoPickerIcon
import org.wordpress.android.ui.photopicker.PhotoPickerFragment.PhotoPickerListener
import org.wordpress.android.ui.posts.EditPostCustomerSupportHelper.onContactCustomerSupport
import org.wordpress.android.ui.posts.EditPostCustomerSupportHelper.onGotoCustomerSupportOptions
import org.wordpress.android.ui.posts.EditPostPublishSettingsFragment.Companion.newInstance
import org.wordpress.android.ui.posts.EditPostRepository.UpdatePostResult
import org.wordpress.android.ui.posts.EditPostRepository.UpdatePostResult.Updated
import org.wordpress.android.ui.posts.EditPostSettingsFragment.EditPostActivityHook
import org.wordpress.android.ui.posts.EditPostSettingsFragment.EditPostSettingsCallback
import org.wordpress.android.ui.posts.EditorBloggingPromptsViewModel.EditorLoadedPrompt
import org.wordpress.android.ui.posts.EditorJetpackSocialViewModel.ActionEvent.OpenEditShareMessage
import org.wordpress.android.ui.posts.EditorJetpackSocialViewModel.ActionEvent.OpenSocialConnectionsList
import org.wordpress.android.ui.posts.EditorJetpackSocialViewModel.ActionEvent.OpenSubscribeJetpackSocial
import org.wordpress.android.ui.posts.FeaturedImageHelper.EnqueueFeaturedImageResult
import org.wordpress.android.ui.posts.HistoryListFragment.Companion.newInstance
import org.wordpress.android.ui.posts.HistoryListFragment.HistoryItemClickInterface
import org.wordpress.android.ui.posts.InsertMediaDialog.InsertMediaCallback
import org.wordpress.android.ui.posts.InsertMediaDialog.InsertType
import org.wordpress.android.ui.posts.PostEditorAnalyticsSession.Outcome
import org.wordpress.android.ui.posts.PostSettingsListDialogFragment.OnPostSettingsDialogFragmentListener
import org.wordpress.android.ui.posts.RemotePreviewLogicHelper.PreviewLogicOperationResult
import org.wordpress.android.ui.posts.RemotePreviewLogicHelper.RemotePreviewHelperFunctions
import org.wordpress.android.ui.posts.RemotePreviewLogicHelper.RemotePreviewType
import org.wordpress.android.ui.posts.editor.EditorActionsProvider
import org.wordpress.android.ui.posts.editor.EditorPhotoPicker
import org.wordpress.android.ui.posts.editor.EditorPhotoPickerListener
import org.wordpress.android.ui.posts.editor.EditorTracker
import org.wordpress.android.ui.posts.editor.ImageEditorTracker
import org.wordpress.android.ui.posts.editor.PostLoadingState
import org.wordpress.android.ui.posts.editor.PostLoadingState.Companion.fromInt
import org.wordpress.android.ui.posts.editor.PrimaryEditorAction
import org.wordpress.android.ui.posts.editor.SecondaryEditorAction
import org.wordpress.android.ui.posts.editor.StorePostViewModel
import org.wordpress.android.ui.posts.editor.StorePostViewModel.ActivityFinishState
import org.wordpress.android.ui.posts.editor.StorePostViewModel.UpdateFromEditor
import org.wordpress.android.ui.posts.editor.StorePostViewModel.UpdateFromEditor.PostFields
import org.wordpress.android.ui.posts.editor.XPostsCapabilityChecker
import org.wordpress.android.ui.posts.editor.media.AddExistingMediaSource
import org.wordpress.android.ui.posts.editor.media.EditorMedia
import org.wordpress.android.ui.posts.editor.media.EditorMedia.AddMediaToPostUiState
import org.wordpress.android.ui.posts.editor.media.EditorMediaListener
import org.wordpress.android.ui.posts.prepublishing.PrepublishingBottomSheetFragment
import org.wordpress.android.ui.posts.prepublishing.PrepublishingBottomSheetFragment.Companion.newInstance
import org.wordpress.android.ui.posts.prepublishing.home.usecases.PublishPostImmediatelyUseCase
import org.wordpress.android.ui.posts.prepublishing.listeners.PrepublishingBottomSheetListener
import org.wordpress.android.ui.posts.reactnative.ReactNativeRequestHandler
import org.wordpress.android.ui.posts.services.AztecImageLoader
import org.wordpress.android.ui.posts.services.AztecVideoLoader
import org.wordpress.android.ui.posts.sharemessage.EditJetpackSocialShareMessageActivity
import org.wordpress.android.ui.posts.sharemessage.EditJetpackSocialShareMessageActivity.Companion.createIntent
import org.wordpress.android.ui.prefs.AppPrefs
import org.wordpress.android.ui.prefs.SiteSettingsInterface
import org.wordpress.android.ui.prefs.SiteSettingsInterface.SiteSettingsListener
import org.wordpress.android.ui.reader.utils.ReaderUtilsWrapper
import org.wordpress.android.ui.suggestion.SuggestionActivity
import org.wordpress.android.ui.suggestion.SuggestionType
import org.wordpress.android.ui.uploads.PostEvents.PostMediaCanceled
import org.wordpress.android.ui.uploads.PostEvents.PostOpenedInEditor
import org.wordpress.android.ui.uploads.PostEvents.PostPreviewingInEditor
import org.wordpress.android.ui.uploads.ProgressEvent
import org.wordpress.android.ui.uploads.UploadService
import org.wordpress.android.ui.uploads.UploadService.UploadMediaRetryEvent
import org.wordpress.android.ui.uploads.UploadUtils
import org.wordpress.android.ui.uploads.UploadUtilsWrapper
import org.wordpress.android.ui.utils.AuthenticationUtils
import org.wordpress.android.ui.utils.UiHelpers
import org.wordpress.android.util.ActivityUtils
import org.wordpress.android.util.AniUtils
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AutolinkUtils
import org.wordpress.android.util.BuildConfigWrapper
import org.wordpress.android.util.DateTimeUtilsWrapper
import org.wordpress.android.util.DisplayUtils
import org.wordpress.android.util.FluxCUtils
import org.wordpress.android.util.LocaleManager
import org.wordpress.android.util.LocaleManagerWrapper
import org.wordpress.android.util.MediaUtils
import org.wordpress.android.util.NetworkUtils
import org.wordpress.android.util.PermissionUtils
import org.wordpress.android.util.ReblogUtils
import org.wordpress.android.util.ShortcutUtils
import org.wordpress.android.util.SiteUtils
import org.wordpress.android.util.StorageUtilsProvider
import org.wordpress.android.util.StringUtils
import org.wordpress.android.util.ToastUtils
import org.wordpress.android.util.UrlUtils
import org.wordpress.android.util.WPMediaUtils
import org.wordpress.android.util.WPPermissionUtils
import org.wordpress.android.util.WPUrlUtils
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import org.wordpress.android.util.analytics.AnalyticsUtils
import org.wordpress.android.util.analytics.AnalyticsUtils.BlockEditorEnabledSource
import org.wordpress.android.util.config.ContactSupportFeatureConfig
import org.wordpress.android.util.config.GlobalStyleSupportFeatureConfig
import org.wordpress.android.util.config.PostConflictResolutionFeatureConfig
import org.wordpress.android.util.config.NewGutenbergFeatureConfig
import org.wordpress.android.util.extensions.setLiftOnScrollTargetViewIdAndRequestLayout
import org.wordpress.android.util.helpers.MediaFile
import org.wordpress.android.util.helpers.MediaGallery
import org.wordpress.android.util.image.BlavatarShape
import org.wordpress.android.util.image.ImageManager
import org.wordpress.android.util.image.ImageType
import org.wordpress.android.viewmodel.Event
import org.wordpress.android.viewmodel.helpers.ToastMessageHolder
import org.wordpress.android.viewmodel.storage.StorageUtilsViewModel
import org.wordpress.android.widgets.AppReviewManager.incrementInteractions
import org.wordpress.android.widgets.WPSnackbar.Companion.make
import org.wordpress.android.widgets.WPViewPager
import org.wordpress.gutenberg.GutenbergWebViewPool
import org.wordpress.aztec.AztecExceptionHandler
import org.wordpress.aztec.exceptions.DynamicLayoutGetBlockIndexOutOfBoundsException
import org.wordpress.aztec.util.AztecLog
import org.wordpress.gutenberg.GutenbergView
import java.io.File
import java.util.Locale
import java.util.regex.Matcher
import java.util.regex.Pattern
import javax.inject.Inject
import kotlin.math.max

@Suppress("LargeClass")
class EditPostActivity : LocaleAwareActivity(), EditorFragmentActivity, EditorImageSettingsListener,
    EditorImagePreviewListener, EditorEditMediaListener, EditorDragAndDropListener, EditorFragmentListener,
    ActivityCompat.OnRequestPermissionsResultCallback,
    PhotoPickerListener, EditorPhotoPickerListener, EditorMediaListener, EditPostActivityHook,
    OnPostSettingsDialogFragmentListener, HistoryItemClickInterface, EditPostSettingsCallback,
    PrepublishingBottomSheetListener, PrivateAtCookieProgressDialogOnDismissListener, ExceptionLogger,
    SiteSettingsListener {
    // External Access to the Image Loader
    var aztecImageLoader: AztecImageLoader? = null

    internal enum class RestartEditorOptions {
        NO_RESTART,
        RESTART_SUPPRESS_GUTENBERG,
        RESTART_DONT_SUPPRESS_GUTENBERG
    }

    private var restartEditorOption: RestartEditorOptions = RestartEditorOptions.NO_RESTART
    private var showAztecEditor: Boolean = false
    private var showGutenbergEditor: Boolean = false
    private var pendingVideoPressInfoRequests: MutableList<String>? = null
    private var postEditorAnalyticsSession: PostEditorAnalyticsSession? = null
    private var isConfigChange: Boolean = false

    /**
     * The PagerAdapter that will provide
     * fragments for each of the sections. We use a
     * FragmentPagerAdapter derivative, which will keep every
     * loaded fragment in memory. If this becomes too memory intensive, it
     * may be best to switch to a
     * FragmentStatePagerAdapter.
     */
    private var sectionsPagerAdapter: SectionsPagerAdapter? = null

    /**
     * The ViewPager that will host the section contents.
     */
    var viewPager: WPViewPager? = null
    private var revision: Revision? = null
    private var editorFragment: EditorFragmentAbstract? = null
    private var editPostSettingsFragment: EditPostSettingsFragment? = null
    private var editorMediaUploadListener: EditorMediaUploadListener? = null
    private var editorPhotoPicker: EditorPhotoPicker? = null
    private var progressDialog: ProgressDialog? = null
    private var addingMediaToEditorProgressDialog: ProgressDialog? = null
    private var isNewPost: Boolean = false
    private var isPage: Boolean = false
    private var isLandingEditor: Boolean = false
    private var hasSetPostContent: Boolean = false
    private var postLoadingState: PostLoadingState = PostLoadingState.NONE
    private var isXPostsCapable: Boolean? = null
    private var onGetSuggestionResult: Consumer<String?>? = null
    private var isVoiceContentSet = false
    private var isNewGutenbergEditor = false

    // For opening the context menu after permissions have been granted
    private var menuView: View? = null
    private var appBarLayout: AppBarLayout? = null
    private var toolbar: Toolbar? = null
    private var menuHasUndo: Boolean = false
    private var menuHasRedo: Boolean = false
    private var showPrepublishingBottomSheetHandler: Handler? = null
    private var showPrepublishingBottomSheetRunnable: Runnable? = null
    private var htmlModeMenuStateOn: Boolean = false
    private var updatingPostArea: FrameLayout? = null

    @Inject lateinit var dispatcher: Dispatcher

    @Inject lateinit var userAgent: UserAgent

    @Inject lateinit var accountStore: AccountStore

    @Inject lateinit var siteStore: SiteStore

    @Inject lateinit var postStore: PostStore

    @Inject lateinit var mediaStore: MediaStore

    @Inject lateinit var uploadStore: UploadStore

    @Inject lateinit var editorThemeStore: EditorThemeStore

    @Inject lateinit var imageLoader: FluxCImageLoader

    @Inject lateinit var shortcutUtils: ShortcutUtils

    @Inject lateinit var quickStartStore: QuickStartStore

    @Inject lateinit var imageManager: ImageManager

    @Inject lateinit var uiHelpers: UiHelpers

    @Inject lateinit var remotePreviewLogicHelper: RemotePreviewLogicHelper

    @Inject lateinit var progressDialogHelper: ProgressDialogHelper

    @Inject lateinit var featuredImageHelper: FeaturedImageHelper

    @Inject lateinit var reactNativeRequestHandler: ReactNativeRequestHandler

    @Inject lateinit var editorMedia: EditorMedia

    @Inject lateinit var localeManagerWrapper: LocaleManagerWrapper

    @Inject internal lateinit var editPostRepository: EditPostRepository

    @Inject lateinit var postUtilsWrapper: PostUtilsWrapper

    @Inject lateinit var editorTracker: EditorTracker

    @Inject lateinit var uploadUtilsWrapper: UploadUtilsWrapper

    @Inject lateinit var editorActionsProvider: EditorActionsProvider

    @Inject lateinit var buildConfigWrapper: BuildConfigWrapper

    @Inject lateinit var dateTimeUtils: DateTimeUtilsWrapper

    @Inject lateinit var readerUtilsWrapper: ReaderUtilsWrapper

    @Inject lateinit var privateAtomicCookie: PrivateAtomicCookie

    @Inject lateinit var imageEditorTracker: ImageEditorTracker

    @Inject lateinit var reblogUtils: ReblogUtils

    @Inject lateinit var analyticsTrackerWrapper: AnalyticsTrackerWrapper

    @Inject lateinit var publishPostImmediatelyUseCase: PublishPostImmediatelyUseCase

    @Inject lateinit var xPostsCapabilityChecker: XPostsCapabilityChecker

    @Inject lateinit var crashLogging: CrashLogging

    @Inject lateinit var mediaPickerLauncher: MediaPickerLauncher

    @Inject lateinit var updateFeaturedImageUseCase: UpdateFeaturedImageUseCase

    @Inject lateinit var globalStyleSupportFeatureConfig: GlobalStyleSupportFeatureConfig

    @Inject lateinit var zendeskHelper: ZendeskHelper

    @Inject lateinit var bloggingPromptsStore: BloggingPromptsStore

    @Inject lateinit var jetpackFeatureRemovalPhaseHelper: JetpackFeatureRemovalPhaseHelper

    @Inject lateinit var contactSupportFeatureConfig: ContactSupportFeatureConfig

    @Inject lateinit var postConflictResolutionFeatureConfig: PostConflictResolutionFeatureConfig

    @Inject lateinit var newGutenbergFeatureConfig: NewGutenbergFeatureConfig

    @Inject lateinit var storePostViewModel: StorePostViewModel
    @Inject lateinit var storageUtilsViewModel: StorageUtilsViewModel
    @Inject lateinit var editorBloggingPromptsViewModel: EditorBloggingPromptsViewModel
    @Inject lateinit var editorJetpackSocialViewModel: EditorJetpackSocialViewModel

    private lateinit var siteModel: SiteModel

    private var siteSettings: SiteSettingsInterface? = null
    private var isJetpackSsoEnabled: Boolean = false
    private var networkErrorOnLastMediaFetchAttempt: Boolean = false
    private var editShareMessageActivityResultLauncher: ActivityResultLauncher<Intent>? = null
    private val hideUpdatingPostAreaHandler: Handler = Handler(Looper.getMainLooper())
    private var hideUpdatingPostAreaRunnable: Runnable? = null
    private var updatingPostStartTime: Long = 0L

    private fun newPostSetup(title: String? = null, content: String? = null) {
        isNewPost = true
        if (!siteModel.isVisible) {
            showErrorAndFinish(R.string.error_blog_hidden)
            return
        }
        // Create a new post
        editPostRepository.set {
            val post = postStore.instantiatePostModel(
                siteModel, isPage, title, content,
                PostStatus.DRAFT.toString(), null, null, false
            )
            post
        }
        editPostRepository.savePostSnapshot()
        EventBus.getDefault().postSticky(
            PostOpenedInEditor(editPostRepository.localSiteId, editPostRepository.id)
        )
        shortcutUtils.reportShortcutUsed(Shortcut.CREATE_NEW_POST)
    }

    private fun newPostFromShareAction() {
        if (isMediaTypeIntent(intent, null)) {
            newPostSetup()
            setPostMediaFromShareAction()
        } else {
            val title = intent.getStringExtra(Intent.EXTRA_SUBJECT)
            val text = intent.getStringExtra(Intent.EXTRA_TEXT)
            val content = migrateToGutenbergEditor(AutolinkUtils.autoCreateLinks(text?:""))
            newPostSetup(title, content)
        }
    }

    private fun newReblogPostSetup() {
        val title = intent.getStringExtra(EditPostActivityConstants.EXTRA_REBLOG_POST_TITLE)
        val quote = intent.getStringExtra(EditPostActivityConstants.EXTRA_REBLOG_POST_QUOTE)
        val citation = intent.getStringExtra(EditPostActivityConstants.EXTRA_REBLOG_POST_CITATION)
        val image = intent.getStringExtra(EditPostActivityConstants.EXTRA_REBLOG_POST_IMAGE)
        val content = reblogUtils.reblogContent(image, quote ?: "", title, citation)
        newPostSetup(title, content)
    }

    private fun newPageFromLayoutPickerSetup(title: String?, layoutSlug: String?) {
        val content = siteStore.getBlockLayoutContent(siteModel, layoutSlug ?: "")
        newPostSetup(title, content)
    }

    private fun createPostEditorAnalyticsSessionTracker(
        showGutenbergEditor: Boolean, post: PostImmutableModel?,
        site: SiteModel, isNewPost: Boolean
    ) {
        if (postEditorAnalyticsSession == null) {
            postEditorAnalyticsSession = PostEditorAnalyticsSession(
                if (showGutenbergEditor) PostEditorAnalyticsSession.Editor.GUTENBERG
                else PostEditorAnalyticsSession.Editor.CLASSIC,
                post, site, isNewPost, analyticsTrackerWrapper
            )
        }
    }

    private fun createEditShareMessageActivityResultLauncher() {
        editShareMessageActivityResultLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result: ActivityResult ->
            if (result.resultCode == RESULT_OK) {
                val data: Intent? = result.data
                data?.let { intent ->
                    val shareMessage: String? =
                        intent.getStringExtra(EditJetpackSocialShareMessageActivity.RESULT_UPDATED_SHARE_MESSAGE)
                    shareMessage?.let { message ->
                        editorJetpackSocialViewModel.onJetpackSocialShareMessageChanged(message)
                    }
                }
            }
        }
    }

    @Suppress("LongMethod")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (application as WordPress).component().inject(this)
        setContentView(R.layout.new_edit_post_activity)
        val callback: OnBackPressedCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                handleBackPressed()
            }
        }
        onBackPressedDispatcher.addCallback(this, callback)
        dispatcher.register(this)
        isNewGutenbergEditor = newGutenbergFeatureConfig.isEnabled()

        createEditShareMessageActivityResultLauncher()

        if (!initializeSiteModel(savedInstanceState)) {
            ToastUtils.showToast(this, R.string.blog_not_found, ToastUtils.Duration.SHORT)
            finish()
            return
        }

        isLandingEditor = intent.extras?.getBoolean(EditPostActivityConstants.EXTRA_IS_LANDING_EDITOR) ?: false

        refreshMobileEditorFromSiteSetting()

        // Initialize editor settings and UI components based on the siteModel
        setupEditor()
        setupToolbar()

        val fragmentManager: FragmentManager = supportFragmentManager
        val isRestarting = checkToRestart(intent)

        if (savedInstanceState == null) {
            handleIntentExtras(intent.extras, isRestarting)
        } else {
            retrieveSavedInstanceState(savedInstanceState)
        }

        // Ensure we have a valid post
        if (!editPostRepository.hasPost()) {
            showErrorAndFinish(R.string.post_not_found)
            return
        }
        editorMedia.start(siteModel, this)
        startObserving()
        editorFragment?.let {
            hasSetPostContent = true
            it.setImageLoader(imageLoader)
        }

        // Ensure that this check happens when post is set
        setShowGutenbergEditor(savedInstanceState)

        // ok now we are sure to have both a valid Post and showGutenberg flag, let's start the editing session tracker
        createPostEditorAnalyticsSessionTracker(
            showGutenbergEditor, editPostRepository.getPost(), siteModel,
            isNewPost
        )
        logTemplateSelection()

        // Bump post created analytics only once, first time the editor is opened
        if (isNewPost && (savedInstanceState == null) && !isRestarting) {
            AnalyticsUtils.trackEditorCreatedPost(
                intent.action,
                intent,
                siteStore.getSiteByLocalId(editPostRepository.localSiteId),
                editPostRepository.getPost()
            )
        }
        if (!isNewPost) {
            // if we are opening a Post for which an error notification exists, we need to remove it from the dashboard
            // to prevent the user from tapping RETRY on a Post that is being currently edited
            UploadService.cancelFinalNotification(this, editPostRepository.getPost())
            resetUploadingMediaToFailedIfPostHasNotMediaInProgressOrQueued()
        }
        sectionsPagerAdapter = SectionsPagerAdapter(fragmentManager)

        // we need to make sure AT cookie is available when trying to edit post on private AT site
        if (siteModel.isPrivateWPComAtomic && privateAtomicCookie.isCookieRefreshRequired()) {
            showIfNecessary(fragmentManager)
            dispatcher.dispatch(
                SiteActionBuilder.newFetchPrivateAtomicCookieAction(
                    SiteStore.FetchPrivateAtomicCookiePayload(siteModel.siteId)
                )
            )
        } else {
            setupViewPager()
        }
        ActivityId.trackLastActivity(ActivityId.POST_EDITOR)
        setupPrepublishingBottomSheetRunnable()

        // The check on savedInstanceState should allow to show the dialog only on first start
        // (even in cases when the VM could be re-created like when activity is destroyed in the background)
        storageUtilsViewModel.start(savedInstanceState == null)
        editorJetpackSocialViewModel.start(siteModel, (editPostRepository))
        customizeToolbar()
        updatingPostArea = findViewById(R.id.updating)

        // check if post content needs updating
        if (postConflictResolutionFeatureConfig.isEnabled()) {
            storePostViewModel.checkIfUpdatedPostVersionExists((editPostRepository), siteModel)
        }
    }

    private fun initializeSiteModel(savedInstanceState: Bundle?): Boolean {
        // Initialize siteModel based on intent or savedInstanceState and set it only once
        val tempSiteModel = if (savedInstanceState == null) {
            val site = intent.getSerializableExtra(WordPress.SITE) as SiteModel?
            val siteFromQuickPressBlogId = getSiteModelForExtraQuickPressBlogIdIfRequested(intent.extras)
            siteFromQuickPressBlogId ?: site
        } else {
            savedInstanceState.getSerializable(WordPress.SITE) as SiteModel?
        }
        tempSiteModel?.let { siteModel = it }?: return false

        return true
    }

    private fun isActionSendOrNewMedia(action: String?): Boolean {
        return action == Intent.ACTION_SEND || action == Intent.ACTION_SEND_MULTIPLE || action == NEW_MEDIA_POST
    }

    private fun refreshMobileEditorFromSiteSetting() {
        // Make sure to use the latest fresh info about the site we've in the DB set only the editor setting for now
        siteStore.getSiteByLocalId(siteModel.id)?.let {
                siteModel.mobileEditor = it.mobileEditor
            siteSettings = SiteSettingsInterface.getInterface(this, siteModel, this)
            // initialize settings with locally cached values, fetch remote on first pass
            fetchSiteSettings()
        }
    }
    private fun getSiteModelForExtraQuickPressBlogIdIfRequested(extras: Bundle?): SiteModel? {
        if (extras == null || extras.containsKey(EditPostActivityConstants.EXTRA_POST_LOCAL_ID)) {
            return null
        }

        val isActionSendOrNewMedia = isActionSendOrNewMedia(intent.action)
        val hasQuickPressFlag = extras.containsKey(EditPostActivityConstants.EXTRA_IS_QUICKPRESS)
        val hasQuickPressBlogId = extras.containsKey(EditPostActivityConstants.EXTRA_QUICKPRESS_BLOG_ID)

        // QuickPress might want to use a different blog than the current blog
        return if ((isActionSendOrNewMedia || hasQuickPressFlag) && hasQuickPressBlogId) {
            val localSiteId = intent.getIntExtra(EditPostActivityConstants.EXTRA_QUICKPRESS_BLOG_ID, -1)
            siteStore.getSiteByLocalId(localSiteId)
        } else {
            null
        }
    }

    @Suppress("CyclomaticComplexMethod")
    private fun handleIntentExtras(extras: Bundle?, isRestarting: Boolean) {
        extras ?: return
        val action = intent.action

        if (!extras.containsKey(EditPostActivityConstants.EXTRA_POST_LOCAL_ID) ||
            isActionSendOrNewMedia(intent.action) ||
            extras.containsKey(EditPostActivityConstants.EXTRA_IS_QUICKPRESS)
        ) {
            isPage = extras.getBoolean(EditPostActivityConstants.EXTRA_IS_PAGE)
            if (isPage && !TextUtils.isEmpty(extras.getString(EditPostActivityConstants.EXTRA_PAGE_TITLE))) {
                newPageFromLayoutPickerSetup(
                    extras.getString(EditPostActivityConstants.EXTRA_PAGE_TITLE),
                    extras.getString(EditPostActivityConstants.EXTRA_PAGE_TEMPLATE)
                )
            } else if ((Intent.ACTION_SEND == action)) {
                newPostFromShareAction()
            } else if ((EditPostActivityConstants.ACTION_REBLOG == action)) {
                newReblogPostSetup()
            } else {
                newPostSetup()
            }
        } else {
            editPostRepository.loadPostByLocalPostId(extras.getInt(EditPostActivityConstants.EXTRA_POST_LOCAL_ID))
            // Load post from extra's
            if (editPostRepository.hasPost()) {
                if (extras.getBoolean(EditPostActivityConstants.EXTRA_LOAD_AUTO_SAVE_REVISION)) {
                    editPostRepository.update { postModel: PostModel ->
                        val updateTitle = !TextUtils.isEmpty(postModel.autoSaveTitle)
                        if (updateTitle) {
                            postModel.setTitle(postModel.autoSaveTitle)
                        }
                        val updateContent = !TextUtils.isEmpty(postModel.autoSaveContent)
                        if (updateContent) {
                            postModel.setContent(postModel.autoSaveContent)
                        }
                        val updateExcerpt = !TextUtils.isEmpty(postModel.autoSaveExcerpt)
                        if (updateExcerpt) {
                            postModel.setExcerpt(postModel.autoSaveExcerpt)
                        }
                        updateTitle || updateContent || updateExcerpt
                    }
                    editPostRepository.savePostSnapshot()
                }
                initializePostObject()
            } else if (isRestarting) {
                newPostSetup()
            }
        }

        if (isRestarting && extras.getBoolean(EditPostActivityConstants.EXTRA_IS_NEW_POST)) {
            // editor was on a new post before the switch so, keep that signal.
            // Fixes https://github.com/wordpress-mobile/gutenberg-mobile/issues/2072
            isNewPost = true
        }

        // retrieve Editor session data if switched editors
        if (isRestarting && extras.containsKey(EditPostActivityConstants.STATE_KEY_EDITOR_SESSION_DATA)) {
            postEditorAnalyticsSession = PostEditorAnalyticsSession
                .fromBundle(extras, EditPostActivityConstants.STATE_KEY_EDITOR_SESSION_DATA, analyticsTrackerWrapper)
        }
    }

    private fun retrieveSavedInstanceState(savedInstanceState: Bundle?) {
        savedInstanceState?.let { state ->
            state.getParcelableArrayList<Uri>(EditPostActivityConstants.STATE_KEY_DROPPED_MEDIA_URIS)
                ?.let { parcelableArrayList ->
                    editorMedia.droppedMediaUris = parcelableArrayList
                }

            isNewPost = state.getBoolean(EditPostActivityConstants.STATE_KEY_IS_NEW_POST, false)
            isNewGutenbergEditor = state.getBoolean(EditPostActivityConstants.STATE_KEY_IS_NEW_GUTENBERG, false)
            isVoiceContentSet = state.getBoolean(EditPostActivityConstants.STATE_KEY_IS_VOICE_CONTENT_SET, false)
            updatePostLoadingAndDialogState(
                fromInt(
                    state.getInt(EditPostActivityConstants.STATE_KEY_POST_LOADING_STATE, 0)
                )
            )
            dB?.let {
                revision = it.getParcel(EditPostActivityConstants.STATE_KEY_REVISION, parcelableCreator())
            }
            postEditorAnalyticsSession = PostEditorAnalyticsSession
                .fromBundle(
                    state,
                    EditPostActivityConstants.STATE_KEY_EDITOR_SESSION_DATA,
                    analyticsTrackerWrapper
                )

            // if we have a remote id saved, let's first try that, as the local Id might have changed after FETCH_POSTS
            if (state.containsKey(EditPostActivityConstants.STATE_KEY_POST_REMOTE_ID)) {
                editPostRepository.loadPostByRemotePostId(
                    state.getLong(EditPostActivityConstants.STATE_KEY_POST_REMOTE_ID),
                    siteModel
                )
                initializePostObject()
            } else if (state.containsKey(EditPostActivityConstants.STATE_KEY_POST_LOCAL_ID)) {
                editPostRepository.loadPostByLocalPostId(
                    state.getInt(EditPostActivityConstants.STATE_KEY_POST_LOCAL_ID)
                )
                initializePostObject()
            }

            (supportFragmentManager.getFragment(
                state,
                EditPostActivityConstants.STATE_KEY_EDITOR_FRAGMENT
            ) as EditorFragmentAbstract?)?.let { frag ->
                editorFragment = frag
                if (frag is EditorMediaUploadListener) {
                    editorMediaUploadListener = frag
                }
            }
        }
    }

    private fun setShowGutenbergEditor(savedInstanceState: Bundle?) {
        showGutenbergEditor = if (savedInstanceState == null) {
            val restartEditorOptionName = intent.getStringExtra(EditPostActivityConstants.EXTRA_RESTART_EDITOR)
            val restartEditorOption =  if (restartEditorOptionName == null)
                RestartEditorOptions.RESTART_DONT_SUPPRESS_GUTENBERG
            else RestartEditorOptions.valueOf(restartEditorOptionName)
            (PostUtils.shouldShowGutenbergEditor(isNewPost, editPostRepository.content, siteModel)
                    && restartEditorOption != RestartEditorOptions.RESTART_SUPPRESS_GUTENBERG)
        } else {
            savedInstanceState.getBoolean(EditPostActivityConstants.STATE_KEY_GUTENBERG_IS_SHOWN)
        }
    }

    private fun setupEditor() {
        if (isNewGutenbergEditor) {
            GutenbergWebViewPool.getPreloadedWebView(getContext())
        }
        // Check whether to show the visual editor

        // NOTE: Migrate to 'androidx.preference.PreferenceManager' and 'androidx.preference.Preference'
        //  This migration is not possible at the moment for 'PreferenceManager.setDefaultValues(...)' because it
        //  depends on the migration of 'EditTextPreferenceWithValidation', which is a type of
        //  'android.preference.EditTextPreference', thus a type of 'android.preference.Preference', and as such it will
        //  throw this 'java.lang.ClassCastException': 'org.wordpress.android.ui.prefs.EditTextPreferenceWithValidation
        //  cannot be cast to androidx.preference.Preference'
        PreferenceManager.setDefaultValues(this, R.xml.account_settings, false)
        showAztecEditor = AppPrefs.isAztecEditorEnabled()
        editorPhotoPicker = EditorPhotoPicker(this, this, this, showAztecEditor)

        // TODO when aztec is the only editor, remove this part and set the overlay bottom margin in xml
        if (showAztecEditor) {
            val overlay: View = findViewById(R.id.view_overlay)
            val layoutParams: MarginLayoutParams = overlay.layoutParams as MarginLayoutParams
            layoutParams.bottomMargin = resources.getDimensionPixelOffset(
                org.wordpress.aztec.R.dimen.aztec_format_bar_height
            )
            overlay.layoutParams = layoutParams
        }
    }

    private fun setupToolbar(){
        // Set up the action bar.
        toolbar = findViewById(R.id.toolbar_main)
        setSupportActionBar(toolbar)
        val actionBar: ActionBar? = supportActionBar
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(false)
            actionBar.setDisplayShowTitleEnabled(false)
        }
        appBarLayout = findViewById(R.id.appbar_main)
    }

    private fun showUpdatingPostArea() {
        updatingPostArea?.visibility = View.VISIBLE
        updatingPostStartTime = System.currentTimeMillis()
        // Cancel any pending hide operations to avoid conflicts
        hideUpdatingPostAreaRunnable?.let {
            hideUpdatingPostAreaHandler.removeCallbacks(it)
        }
    }

    private fun hideUpdatingPostArea() {
        val elapsedTime = System.currentTimeMillis() - updatingPostStartTime
        val delay: Long = MIN_UPDATING_POST_DISPLAY_TIME - elapsedTime
        if (delay > 0) {
            // Delay hiding the view if the elapsed time is less than the minimum display time
            hideUpdatingPostAreaWithDelay(delay)
        } else {
            // Hide the view immediately if the minimum display time has been met or exceeded
            updatingPostArea?.visibility = View.GONE
        }
    }

    private fun hideUpdatingPostAreaWithDelay(delay: Long) {
        // Define the runnable only once or ensure it's the same instance if it's already defined
        if (hideUpdatingPostAreaRunnable == null) {
            hideUpdatingPostAreaRunnable = Runnable {
                updatingPostArea?.visibility = View.GONE
            }
        }
        hideUpdatingPostAreaRunnable?.let {
            hideUpdatingPostAreaHandler.postDelayed(it, delay)
        }
    }

    private fun customizeToolbar() {
        toolbar?.let {
            val overflowIcon: Drawable? = ContextCompat.getDrawable(this, R.drawable.more_vertical)
            it.overflowIcon = overflowIcon

            // Custom close button
            val closeHeader: View = it.findViewById(R.id.edit_post_header)
            closeHeader.setOnClickListener { handleBackPressed() }
            // Update site icon if mSite is available, if not it will use the placeholder.
            val siteIconUrl = SiteUtils.getSiteIconUrl(
                siteModel,
                resources.getDimensionPixelSize(R.dimen.blavatar_sz_small)
            )

            val siteIcon: ImageView = it.findViewById(R.id.close_editor_site_icon)
            val blavatarType: ImageType = SiteUtils.getSiteImageType(
                siteModel.isWpForTeamsSite, BlavatarShape.SQUARE_WITH_ROUNDED_CORNERES
            )
            imageManager.loadImageWithCorners(
                siteIcon, blavatarType, siteIconUrl,
                resources.getDimensionPixelSize(R.dimen.edit_post_header_image_corner_radius)
            )
        }
    }

    private fun presentNewPageNoticeIfNeeded() {
        if (!isPage || !isNewPost) {
            return
        }
        val message: String =
            if (editPostRepository.content.isEmpty()) getString(R.string.mlp_notice_blank_page_created) else getString(
                R.string.mlp_notice_page_created
            )
        editorFragment?.showNotice(message)
    }

    private fun fetchSiteSettings() {
        siteSettings?.init(true)
    }

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onPrivateAtomicCookieFetched(event: OnPrivateAtomicCookieFetched) {
        // if the dialog is not showing by the time cookie fetched it means that it was dismissed and content was loaded
        if (isShowing(supportFragmentManager)) {
            setupViewPager()
            dismissIfNecessary(supportFragmentManager)
        }
        if (event.isError) {
            AppLog.e(
                AppLog.T.EDITOR,
                "Failed to load private AT cookie. " + event.error.type + " - " + event.error.message
            )
            make(findViewById(R.id.editor_activity), R.string.media_accessing_failed, Snackbar.LENGTH_LONG)
                .show()
        }
    }

    override fun onCookieProgressDialogCancelled() {
        make(findViewById(R.id.editor_activity), R.string.media_accessing_failed, Snackbar.LENGTH_LONG)
            .show()
        setupViewPager()
    }

    // SiteSettingsListener
    override fun onSaveError(error: Exception?) { /* No Op */ }
    override fun onFetchError(error: Exception?) { /* No Op */ }
    override fun onSettingsUpdated() {
        // Let's hold the value in local variable as listener is too noisy
        val isJetpackSsoEnabled = siteModel.isJetpackConnected && siteSettings?.isJetpackSsoEnabled == true
        if (this.isJetpackSsoEnabled != isJetpackSsoEnabled) {
            this.isJetpackSsoEnabled = isJetpackSsoEnabled
            if (editorFragment is GutenbergEditorFragment) {
                val gutenbergFragment = editorFragment as GutenbergEditorFragment
                gutenbergFragment.setJetpackSsoEnabled(this.isJetpackSsoEnabled)
                gutenbergFragment.updateCapabilities(gutenbergPropsBuilder)
            }
        }
    }

    override fun onSettingsSaved() { /* No Op */ }
    override fun onCredentialsValidated(error: Exception?) { /* No Op */ }
    private fun setupViewPager() {
        // Set up the ViewPager with the sections adapter.
        viewPager = findViewById(R.id.pager)
        viewPager?.adapter = sectionsPagerAdapter
        viewPager?.offscreenPageLimit = OFFSCREEN_PAGE_LIMIT
        viewPager?.setPagingEnabled(false)

        // When swiping between different sections, select the corresponding tab. We can also use ActionBar.Tab#select()
        // to do this if we have a reference to the Tab.
        viewPager?.clearOnPageChangeListeners()
        viewPager?.addOnPageChangeListener(object : SimpleOnPageChangeListener() {
            override fun onPageSelected(position: Int) {
                invalidateOptionsMenu()
                if (position == PAGE_CONTENT) {
                    title = SiteUtils.getSiteNameOrHomeURL(siteModel)
                    appBarLayout?.setLiftOnScrollTargetViewIdAndRequestLayout(View.NO_ID)
                    toolbar?.setBackgroundResource(R.drawable.tab_layout_background)
                } else if (position == PAGE_SETTINGS) {
                    setTitle(if (editPostRepository.isPage) R.string.page_settings else R.string.post_settings)
                    editorPhotoPicker?.hidePhotoPicker()
                    appBarLayout?.liftOnScrollTargetViewId = R.id.settings_fragment_root
                    toolbar?.background = null
                } else if (position == PAGE_PUBLISH_SETTINGS) {
                    setTitle(R.string.publish_date)
                    editorPhotoPicker?.hidePhotoPicker()
                    appBarLayout?.setLiftOnScrollTargetViewIdAndRequestLayout(View.NO_ID)
                    toolbar?.background = null
                } else if (position == PAGE_HISTORY) {
                    setTitle(R.string.history_title)
                    editorPhotoPicker?.hidePhotoPicker()
                    appBarLayout?.liftOnScrollTargetViewId = R.id.empty_recycler_view
                    toolbar?.background = null
                }
            }
        })
    }

    @Suppress("LongMethod")
    private fun startObserving() {
        editorMedia.uiState.observe(this
        ) { uiState: AddMediaToPostUiState? ->
            if (uiState != null) {
                updateAddingMediaToEditorProgressDialogState(uiState.progressDialogUiState)
                if (uiState.editorOverlayVisibility) {
                    showOverlay(false)
                } else {
                    hideOverlay()
                }
            }
        }
        editorMedia.snackBarMessage.observe(this
        ) { event: Event<SnackbarMessageHolder?> ->
            val messageHolder: SnackbarMessageHolder? = event.getContentIfNotHandled()
            if (messageHolder != null) {
                make(
                    findViewById(R.id.editor_activity),
                    uiHelpers.getTextOfUiString(this, messageHolder.message),
                    Snackbar.LENGTH_SHORT
                )
                    .show()
            }
        }
        editorMedia.toastMessage.observe(this) { event: Event<ToastMessageHolder?> ->
            event.getContentIfNotHandled()?.show(this)
        }
        if (!isNewGutenbergEditor) {
            storePostViewModel.onSavePostTriggered.observe(this) { unitEvent: Event<Unit> ->
                unitEvent.applyIfNotHandled {
                    updateAndSavePostAsync()
                }
            }
        }
        storePostViewModel.onFinish.observe(this) { finishEvent ->
            finishEvent.applyIfNotHandled {
                when (this) {
                    ActivityFinishState.SAVED_ONLINE -> saveResult(saved = true, uploadNotStarted = false)
                    ActivityFinishState.SAVED_LOCALLY -> saveResult(saved = true, uploadNotStarted = true)
                    ActivityFinishState.CANCELLED -> saveResult(saved = false, uploadNotStarted = true)
                }
                removePostOpenInEditorStickyEvent()
                editorMedia.definitelyDeleteBackspaceDeletedMediaItemsAsync()
                finish()
            }
        }
        editPostRepository.postChanged.observe(this
        ) { postEvent: Event<PostImmutableModel?> ->
            postEvent.applyIfNotHandled {
                storePostViewModel.savePostToDb(editPostRepository, siteModel)
            }
        }
        storageUtilsViewModel.checkStorageWarning.observe(this
        ) { event: Event<Unit> ->
            event.applyIfNotHandled {
                storageUtilsViewModel.onStorageWarningCheck(
                    supportFragmentManager,
                    StorageUtilsProvider.Source.EDITOR
                )
            }
        }
        editorBloggingPromptsViewModel.onBloggingPromptLoaded.observe(this
        ) { event: Event<EditorLoadedPrompt> ->
            event.applyIfNotHandled {
                editPostRepository.updateAsync({ postModel: PostModel ->
                    postModel.setContent(this.content)
                    postModel.answeredPromptId = this.promptId
                    postModel.setTagNameList(this.tags)
                    true
                }) { _: PostImmutableModel?, _: UpdatePostResult? ->
                    refreshEditorContent()
                }
            }
        }
        editorJetpackSocialViewModel.actionEvents.observe(this
        ) { actionEvent: EditorJetpackSocialViewModel.ActionEvent? ->
            if (actionEvent is OpenEditShareMessage) {
                val intent: Intent = createIntent(
                    this, actionEvent.shareMessage
                )
                editShareMessageActivityResultLauncher?.launch(intent)
            } else if (actionEvent is OpenSocialConnectionsList) {
                ActivityLauncher.viewBlogSharing(this, actionEvent.siteModel)
            } else if (actionEvent is OpenSubscribeJetpackSocial) {
                WPWebViewActivity.openUrlByUsingGlobalWPCOMCredentials(
                    this, actionEvent.url
                )
            }
        }
        storePostViewModel.onPostUpdateUiVisible.observe(this) { isVisible: Boolean ->
            if (isVisible) {
                showUpdatingPostArea()
            } else {
                hideUpdatingPostArea()
            }
        }
        storePostViewModel.onPostUpdateResult.observe(this) { isSuccess: Boolean ->
            if (isSuccess) {
                editPostRepository.loadPostByLocalPostId(editPostRepository.id)
                refreshEditorContent()
            } else {
                ToastUtils.showToast(
                    this@EditPostActivity,
                    getString(R.string.editor_updating_content_failed),
                    ToastUtils.Duration.SHORT
                )
            }
        }
    }

    private fun initializePostObject() {
        if (editPostRepository.hasPost()) {
            editPostRepository.savePostSnapshotWhenEditorOpened()
            editPostRepository.replace { post: PostModel? ->
                UploadService.updatePostWithCurrentlyCompletedUploads(
                    post
                )
            }
            isPage = editPostRepository.isPage
            EventBus.getDefault().postSticky(
                PostOpenedInEditor(
                    editPostRepository.localSiteId,
                    editPostRepository.id
                )
            )
            editorMedia.purgeMediaToPostAssociationsIfNotInPostAnymoreAsync()
        }
    }

    // this method aims at recovering the current state of media items if they're inconsistent within the PostModel.
    private fun resetUploadingMediaToFailedIfPostHasNotMediaInProgressOrQueued() {
        val useAztec = AppPrefs.isAztecEditorEnabled()
        if (!useAztec || UploadService.hasPendingOrInProgressMediaUploadsForPost(editPostRepository.getPost())) {
            return
        }
        editPostRepository.updateAsync({ postModel: PostModel ->
            val oldContent = postModel.content
            if ((!AztecEditorFragment.hasMediaItemsMarkedUploading(this@EditPostActivity, oldContent)
               // we need to make sure items marked failed are still failed or not as well
               && !AztecEditorFragment.hasMediaItemsMarkedFailed(this@EditPostActivity, oldContent))
            ) {
                return@updateAsync false
            }
            val newContent = AztecEditorFragment.resetUploadingMediaToFailed(this@EditPostActivity, oldContent)
            if (!TextUtils.isEmpty(oldContent) && (newContent != null) && (oldContent.compareTo(newContent) != 0)) {
                postModel.setContent(newContent)
                return@updateAsync true
            }
            false
        }, null)
    }

    override fun onResume() {
        super.onResume()
        EventBus.getDefault().register(this)
        reattachUploadingMediaForAztec()

        // Bump editor opened event every time the activity is resumed, to match the EDITOR_CLOSED event onPause
        PostUtils.trackOpenEditorAnalytics(editPostRepository.getPost(), siteModel)
        isConfigChange = false
    }

    private fun reattachUploadingMediaForAztec() {
        editorMediaUploadListener?.let {
            editorMedia.reattachUploadingMediaForAztec(
                (editPostRepository),
                editorFragment is AztecEditorFragment,
                it
            )
        }
    }

    override fun onPause() {
        super.onPause()
        EventBus.getDefault().unregister(this)
        AnalyticsTracker.track(Stat.EDITOR_CLOSED)
    }

    override fun onStop() {
        super.onStop()
        if (aztecImageLoader != null && isFinishing) {
            aztecImageLoader?.clearTargets()
            aztecImageLoader = null
        }
        showPrepublishingBottomSheetRunnable?.let {
            showPrepublishingBottomSheetHandler?.removeCallbacks(it)
        }

        hideUpdatingPostAreaRunnable?.let {
            hideUpdatingPostAreaHandler.removeCallbacks(it)
        }
    }

    override fun onDestroy() {
        if (!isConfigChange && (restartEditorOption == RestartEditorOptions.NO_RESTART)) {
            postEditorAnalyticsSession?.end()
        }
        dispatcher.unregister(this)
        editorMedia.cancelAddMediaToEditorActions()
        removePostOpenInEditorStickyEvent()
        if (editorFragment is AztecEditorFragment) {
            (editorFragment as AztecEditorFragment).disableContentLogOnCrashes()
        }
        reactNativeRequestHandler.destroy()
        super.onDestroy()
    }

    private fun removePostOpenInEditorStickyEvent() {
        val stickyEvent: PostOpenedInEditor? = EventBus.getDefault().getStickyEvent(
            PostOpenedInEditor::class.java
        )
        if (stickyEvent != null) {
            // "Consume" the sticky event
            EventBus.getDefault().removeStickyEvent(stickyEvent)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        // Saves both post objects so we can restore them in onCreate()
        updateAndSavePostAsync()
        outState.putInt(EditPostActivityConstants.STATE_KEY_POST_LOCAL_ID, editPostRepository.id)
        if (!editPostRepository.isLocalDraft) {
            outState.putLong(EditPostActivityConstants.STATE_KEY_POST_REMOTE_ID, editPostRepository.remotePostId)
        }
        outState.putInt(EditPostActivityConstants.STATE_KEY_POST_LOADING_STATE, postLoadingState.value)
        outState.putBoolean(EditPostActivityConstants.STATE_KEY_IS_NEW_POST, isNewPost)
        outState.putBoolean(EditPostActivityConstants.STATE_KEY_IS_VOICE_CONTENT_SET, isVoiceContentSet)
        outState.putBoolean(EditPostActivityConstants.STATE_KEY_IS_NEW_GUTENBERG, isNewGutenbergEditor)
        outState.putBoolean(
            EditPostActivityConstants.STATE_KEY_IS_PHOTO_PICKER_VISIBLE,
            editorPhotoPicker?.isPhotoPickerShowing() ?: false
        )
        outState.putBoolean(EditPostActivityConstants.STATE_KEY_HTML_MODE_ON, htmlModeMenuStateOn)
        outState.putBoolean(EditPostActivityConstants.STATE_KEY_UNDO, menuHasUndo)
        outState.putBoolean(EditPostActivityConstants.STATE_KEY_REDO, menuHasRedo)
        outState.putSerializable(WordPress.SITE, siteModel)
        dB?.addParcel(EditPostActivityConstants.STATE_KEY_REVISION, revision)
        outState.putSerializable(EditPostActivityConstants.STATE_KEY_EDITOR_SESSION_DATA, postEditorAnalyticsSession)
        isConfigChange = true // don't call sessionData.end() in onDestroy() if this is an Android config change
        outState.putBoolean(EditPostActivityConstants.STATE_KEY_GUTENBERG_IS_SHOWN, showGutenbergEditor)
        outState.putParcelableArrayList(
            EditPostActivityConstants.STATE_KEY_DROPPED_MEDIA_URIS, editorMedia.droppedMediaUris
        )

        editorFragment?.let {
            supportFragmentManager.putFragment(outState, EditPostActivityConstants.STATE_KEY_EDITOR_FRAGMENT, it)
        }
        // We must save the media capture path when the activity is destroyed to handle orientation changes during
        // photo capture (see: https://github.com/wordpress-mobile/WordPress-Android/issues/11296)
        outState.putString(EditPostActivityConstants.STATE_KEY_MEDIA_CAPTURE_PATH, mediaCapturePath)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        htmlModeMenuStateOn = savedInstanceState.getBoolean(EditPostActivityConstants.STATE_KEY_HTML_MODE_ON)
        menuHasUndo = savedInstanceState.getBoolean(EditPostActivityConstants.STATE_KEY_UNDO)
        menuHasRedo = savedInstanceState.getBoolean(EditPostActivityConstants.STATE_KEY_REDO)
        if (savedInstanceState.getBoolean(EditPostActivityConstants.STATE_KEY_IS_PHOTO_PICKER_VISIBLE, false)) {
            editorPhotoPicker?.showPhotoPicker(siteModel)
        }

        // Restore media capture path for orientation changes during photo capture
        mediaCapturePath = savedInstanceState.getString(EditPostActivityConstants.STATE_KEY_MEDIA_CAPTURE_PATH, "")
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        editorPhotoPicker?.onOrientationChanged(newConfig.orientation)
    }

    private val primaryAction: PrimaryEditorAction
        get() = editorActionsProvider
            .getPrimaryAction(editPostRepository.status, UploadUtils.userCanPublish(siteModel), isLandingEditor)
    private val primaryActionText: String
        get() {
            return getString(primaryAction.titleResource)
        }
    private val secondaryAction: SecondaryEditorAction
        get() {
            return editorActionsProvider
                .getSecondaryAction(editPostRepository.status, UploadUtils.userCanPublish(siteModel))
        }
    private val secondaryActionText: String?
        get() {
            @StringRes val titleResource = secondaryAction.titleResource
            return if (titleResource != null) getString(titleResource) else null
        }

    @Suppress("SwallowedException")
    private fun shouldSwitchToGutenbergBeVisible(
        editorFragment: EditorFragmentAbstract?,
        site: SiteModel
    ): Boolean {
        // Some guard conditions
        val message: String? = if (!editPostRepository.hasPost())
            "shouldSwitchToGutenbergBeVisible got a null post parameter."
        else if (editorFragment == null)
            "shouldSwitchToGutenbergBeVisible got a null editorFragment parameter."
        else null

        message?.let {
            AppLog.w(AppLog.T.EDITOR, it)
            return false
        }

        // Check whether the content has blocks.
        var hasBlocks = false
        var isEmpty = false
        try {
            val content = editorFragment?.getContent(editPostRepository.content) as String
            hasBlocks = PostUtils.contentContainsGutenbergBlocks(content)
            isEmpty = TextUtils.isEmpty(content)
        } catch (e: EditorFragmentNotAddedException) {
            // legacy exception; just ignore.
        }

        // if content has blocks or empty, offer the switch to Gutenberg. The block editor doesn't have good
        //  "Classic Block" support yet so, don't offer a switch to it if content doesn't have blocks. If the post
        //  is empty but the user hasn't enabled "Use Gutenberg for new posts" in Site Setting,
        //  don't offer the switch.
        return hasBlocks || (SiteUtils.isBlockEditorDefaultForNewPost(site) && isEmpty)
    }

    /*
     * shows/hides the overlay which appears atop the editor, which effectively disables it
     */
    private fun showOverlay(animate: Boolean) {
        val overlay = findViewById<View>(R.id.view_overlay)
        if (animate) {
            AniUtils.fadeIn(overlay, AniUtils.Duration.MEDIUM)
        } else {
            overlay.visibility = View.VISIBLE
        }
    }

    private fun hideOverlay() {
        val overlay = findViewById<View>(R.id.view_overlay)
        overlay.visibility = View.GONE
    }

    override fun onPhotoPickerShown() {
        // animate in the editor overlay
        showOverlay(true)
        if (editorFragment is AztecEditorFragment) {
            (editorFragment as AztecEditorFragment).enableMediaMode(true)
        }
    }

    override fun onPhotoPickerHidden() {
        hideOverlay()
        if (editorFragment is AztecEditorFragment) {
            (editorFragment as AztecEditorFragment).enableMediaMode(false)
        }
    }

    /*
     * called by PhotoPickerFragment when media is selected - may be a single item or a list of items
     */
    override fun onPhotoPickerMediaChosen(uriList: List<Uri>) {
        editorPhotoPicker?.hidePhotoPicker()
        editorMedia.addNewMediaItemsToEditorAsync(uriList, false)
    }

    /*
     * called by PhotoPickerFragment when user clicks an icon to launch the camera, native
     * picker, or WP media picker
     */
    override fun onPhotoPickerIconClicked(icon: PhotoPickerIcon, allowMultipleSelection: Boolean) {
        editorPhotoPicker?.hidePhotoPicker()
        if (!icon.requiresUploadPermission() || WPMediaUtils.currentUserCanUploadMedia(siteModel)) {
            editorPhotoPicker?.allowMultipleSelection = allowMultipleSelection
            when (icon) {
                PhotoPickerIcon.ANDROID_CAPTURE_PHOTO -> launchCamera()
                PhotoPickerIcon.ANDROID_CAPTURE_VIDEO -> launchVideoCamera()
                PhotoPickerIcon.ANDROID_CHOOSE_PHOTO_OR_VIDEO -> WPMediaUtils.launchMediaLibrary(
                    this,
                    allowMultipleSelection
                )

                PhotoPickerIcon.ANDROID_CHOOSE_PHOTO -> launchPictureLibrary()
                PhotoPickerIcon.ANDROID_CHOOSE_VIDEO -> launchVideoLibrary()
                PhotoPickerIcon.WP_MEDIA -> mediaPickerLauncher.viewWPMediaLibraryPickerForResult(
                    this,
                    siteModel, MediaBrowserType.EDITOR_PICKER
                )

                PhotoPickerIcon.STOCK_MEDIA -> {
                    val requestCode =
                        if (allowMultipleSelection) RequestCodes.STOCK_MEDIA_PICKER_MULTI_SELECT
                        else RequestCodes.STOCK_MEDIA_PICKER_SINGLE_SELECT_FOR_GUTENBERG_BLOCK
                    mediaPickerLauncher.showStockMediaPickerForResult(
                        this,
                        siteModel,
                        requestCode,
                        allowMultipleSelection
                    )
                }

                PhotoPickerIcon.GIF -> mediaPickerLauncher.showGifPickerForResult(
                    this,
                    siteModel,
                    allowMultipleSelection
                )
            }
        } else {
            make(
                findViewById(R.id.editor_activity), R.string.media_error_no_permission_upload,
                Snackbar.LENGTH_SHORT
            ).show()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        super.onCreateOptionsMenu(menu)
        menuInflater.inflate(R.menu.edit_post, menu)
        return true
    }

    @Suppress("LongMethod", "CyclomaticComplexMethod", "SwallowedException")
    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        var showMenuItems = true
        viewPager?.let {
            if (it.currentItem > PAGE_CONTENT) {
                showMenuItems = false
            }
        }

        val undoItem = menu.findItem(R.id.menu_undo_action)
        val redoItem = menu.findItem(R.id.menu_redo_action)
        val secondaryAction = menu.findItem(R.id.menu_secondary_action)
        val previewMenuItem = menu.findItem(R.id.menu_preview_post)
        val viewHtmlModeMenuItem = menu.findItem(R.id.menu_html_mode)
        val historyMenuItem = menu.findItem(R.id.menu_history)
        val settingsMenuItem = menu.findItem(R.id.menu_post_settings)
        val helpMenuItem = menu.findItem(R.id.menu_editor_help)
        if (undoItem != null) {
            undoItem.setEnabled(menuHasUndo)
            undoItem.setVisible(!htmlModeMenuStateOn && !isNewGutenbergEditor)
        }
        if (redoItem != null) {
            redoItem.setEnabled(menuHasRedo)
            redoItem.setVisible(!htmlModeMenuStateOn && !isNewGutenbergEditor)
        }
        if (secondaryAction != null && editPostRepository.hasPost()) {
            secondaryAction.setVisible(showMenuItems && this.secondaryAction.isVisible)
            secondaryAction.setTitle(secondaryActionText)
        }
        previewMenuItem?.setVisible(showMenuItems)
        if (viewHtmlModeMenuItem != null) {
            viewHtmlModeMenuItem.setVisible(
                (((editorFragment is AztecEditorFragment)
                        || (editorFragment is GutenbergEditorFragment))) && !isNewGutenbergEditor && showMenuItems
            )
            viewHtmlModeMenuItem.setTitle(
                if (htmlModeMenuStateOn) R.string.menu_visual_mode else R.string.menu_html_mode)
        }
        if (historyMenuItem != null) {
            val hasHistory = !isNewPost && siteModel.isUsingWpComRestApi
            historyMenuItem.setVisible(showMenuItems && hasHistory)
        }
        if (settingsMenuItem != null) {
            settingsMenuItem.setTitle(if (isPage) R.string.page_settings else R.string.post_settings)
            settingsMenuItem.setVisible(showMenuItems)
        }

        // Set text of the primary action button in the ActionBar
        if (editPostRepository.hasPost()) {
            val primaryAction = menu.findItem(R.id.menu_primary_action)
            if (primaryAction != null) {
                primaryAction.setTitle(primaryActionText)
                primaryAction.setVisible(
                    (viewPager != null) && (viewPager?.currentItem != PAGE_HISTORY
                            ) && (viewPager?.currentItem != PAGE_PUBLISH_SETTINGS)
                )
            }
        }
        val switchToGutenbergMenuItem = menu.findItem(R.id.menu_switch_to_gutenberg)

        // The following null checks should basically be redundant but were added to manage
        // an odd behaviour recorded with Android 8.0.0
        // (see https://github.com/wordpress-mobile/WordPress-Android/issues/9748 for more information)
        if (switchToGutenbergMenuItem != null) {
            val switchToGutenbergVisibility =
                if (showGutenbergEditor) false else shouldSwitchToGutenbergBeVisible(editorFragment, siteModel)
            switchToGutenbergMenuItem.setVisible(switchToGutenbergVisibility)
        }
        val contentInfo = menu.findItem(R.id.menu_content_info)
        (editorFragment as? GutenbergEditorFragment)?.let { gutenbergEditorFragment ->
            if (isNewGutenbergEditor) {
                contentInfo.isVisible = false
            } else {
                contentInfo.setOnMenuItemClickListener { _: MenuItem? ->
                    try {
                        gutenbergEditorFragment.showContentInfo()
                    } catch (e: EditorFragmentNotAddedException) {
                        ToastUtils.showToast(
                            getContext(),
                            R.string.toast_content_info_failed
                        )
                    }
                    true
                }
            }
        } ?: run {
            contentInfo.isVisible = false // only show the menu item for Gutenberg
        }

        if (helpMenuItem != null) {
            // Support section will be disabled in WordPress app when Jetpack-powered features are removed.
            // Therefore, we have to update the Help menu item accordingly.
            val showHelpAndSupport = jetpackFeatureRemovalPhaseHelper.shouldShowHelpAndSupportOnEditor()
            val helpMenuTitle = if (showHelpAndSupport) R.string.help_and_support else R.string.help
            helpMenuItem.setTitle(helpMenuTitle)
            if (editorFragment is GutenbergEditorFragment && showMenuItems && !isNewGutenbergEditor) {
                helpMenuItem.setVisible(true)
            } else {
                helpMenuItem.setVisible(false)
            }
        }
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        val allGranted = WPPermissionUtils.setPermissionListAsked(
            this, requestCode, permissions, grantResults, true
        )
        if (allGranted) {
            when (requestCode) {
                WPPermissionUtils.EDITOR_MEDIA_PERMISSION_REQUEST_CODE -> if (menuView != null) {
                    super.openContextMenu(menuView)
                    menuView = null
                }
            }
        }
    }

    private fun handleBackPressed(): Boolean {
        viewPager?.let { pager ->
            when {
                pager.currentItem == PAGE_PUBLISH_SETTINGS -> {
                    pager.currentItem = PAGE_SETTINGS
                    invalidateOptionsMenu()
                }
                pager.currentItem > PAGE_CONTENT -> {
                    if (pager.currentItem == PAGE_SETTINGS) {
                        editorFragment?.setFeaturedImageId(editPostRepository.featuredImageId)
                    }
                    pager.currentItem = PAGE_CONTENT
                    invalidateOptionsMenu()
                }
                editorPhotoPicker?.isPhotoPickerShowing() == true -> {
                    editorPhotoPicker?.hidePhotoPicker()
                }
                else -> {
                    savePostAndOptionallyFinish(doFinish = true, forceSave = false)
                }
            }
        }
        return true
    }

    private val editPostActivityStrategyFunctions: RemotePreviewHelperFunctions
        get() {
            return object : RemotePreviewHelperFunctions {
                override fun notifyUploadInProgress(post: PostImmutableModel): Boolean {
                    return if (UploadService.hasInProgressMediaUploadsForPost(post)) {
                        ToastUtils.showToast(
                            this@EditPostActivity,
                            getString(R.string.editor_toast_uploading_please_wait), ToastUtils.Duration.SHORT
                        )
                        true
                    } else {
                        false
                    }
                }

                override fun notifyEmptyDraft() {
                    ToastUtils.showToast(
                        this@EditPostActivity,
                        getString(R.string.error_preview_empty_draft), ToastUtils.Duration.SHORT
                    )
                }

                override fun startUploading(isRemoteAutoSave: Boolean, post: PostImmutableModel) {
                    if (isRemoteAutoSave) {
                        updatePostLoadingAndDialogState(PostLoadingState.REMOTE_AUTO_SAVING_FOR_PREVIEW, post)
                        savePostAndOptionallyFinish(doFinish = false, forceSave = true)
                    } else {
                        updatePostLoadingAndDialogState(PostLoadingState.UPLOADING_FOR_PREVIEW, post)
                        savePostAndOptionallyFinish(doFinish= false, forceSave = false)
                    }
                }

                override fun notifyEmptyPost() {
                    val message =
                        getString(if (isPage) R.string.error_preview_empty_page else R.string.error_preview_empty_post)
                    ToastUtils.showToast(this@EditPostActivity, message, ToastUtils.Duration.SHORT)
                }
            }
        }

    // Menu actions
    @Suppress("LongMethod", "CyclomaticComplexMethod", "ReturnCount")
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val itemId = item.itemId
        if (itemId == android.R.id.home) {
            return handleBackPressed()
        }
        editorPhotoPicker?.hidePhotoPicker()

        if (itemId == R.id.menu_primary_action) {
            performPrimaryAction()
        } else {
            // Disable other action bar buttons while a media upload is in progress
            // (unnecessary for Aztec since it supports progress reattachment)
            val isMediaOrActionInProgress =
                editorFragment?.isUploadingMedia == true || editorFragment?.isActionInProgress == true
            if ((!(showAztecEditor || showGutenbergEditor) && isMediaOrActionInProgress)) {
                ToastUtils.showToast(this, R.string.editor_toast_uploading_please_wait, ToastUtils.Duration.SHORT)
                return false
            }
            if (itemId == R.id.menu_history) {
                AnalyticsTracker.track(Stat.REVISIONS_LIST_VIEWED)
                ActivityUtils.hideKeyboard(this)
                viewPager?.currentItem = PAGE_HISTORY
            } else if (itemId == R.id.menu_preview_post) {
                if (!showPreview()) {
                    return false
                }
            } else if (itemId == R.id.menu_post_settings) {
                editPostSettingsFragment?.refreshViews()
                ActivityUtils.hideKeyboard(this)
                viewPager?.currentItem = PAGE_SETTINGS
            } else if (itemId == R.id.menu_secondary_action) {
                return performSecondaryAction()
            } else if (itemId == R.id.menu_html_mode) {
                // toggle HTML mode
                if (editorFragment is AztecEditorFragment) {
                    (editorFragment as AztecEditorFragment).onToolbarHtmlButtonClicked()
                } else if (editorFragment is GutenbergEditorFragment) {
                    (editorFragment as GutenbergEditorFragment).onToggleHtmlMode()
                }
            } else if (itemId == R.id.menu_switch_to_gutenberg) {
                // The following boolean check should be always redundant but was added to manage
                // an odd behaviour recorded with Android 8.0.0
                // (see https://github.com/wordpress-mobile/WordPress-Android/issues/9748 for more information)
                if (shouldSwitchToGutenbergBeVisible(editorFragment, siteModel)) {
                    // let's finish this editing instance and start again, but let GB be used
                    restartEditorOption = RestartEditorOptions.RESTART_DONT_SUPPRESS_GUTENBERG
                    postEditorAnalyticsSession?.switchEditor(PostEditorAnalyticsSession.Editor.GUTENBERG)
                    postEditorAnalyticsSession?.setOutcome(Outcome.SAVE)
                    storePostViewModel.finish(ActivityFinishState.SAVED_LOCALLY)
                } else {
                    logWrongMenuState("Wrong state in menu_switch_to_gutenberg: menu should not be visible.")
                }
            } else if (itemId == R.id.menu_editor_help) {
                // Display the editor help page -- option should only be available in the GutenbergEditor
                if (editorFragment is GutenbergEditorFragment) {
                    analyticsTrackerWrapper.track(Stat.EDITOR_HELP_SHOWN, siteModel)
                    (editorFragment as GutenbergEditorFragment).showEditorHelp()
                }
            } else if (itemId == R.id.menu_undo_action) {
                if (editorFragment is GutenbergEditorFragment) {
                    (editorFragment as GutenbergEditorFragment).onUndoPressed()
                }
            } else if (itemId == R.id.menu_redo_action) {
                if (editorFragment is GutenbergEditorFragment) {
                    (editorFragment as GutenbergEditorFragment).onRedoPressed()
                }
            }
        }
        return false
    }

    private fun logWrongMenuState(logMsg: String) {
        AppLog.w(AppLog.T.EDITOR, logMsg)
    }

    private fun showEmptyPostErrorForSecondaryAction() {
        var message =
            getString(if (isPage) R.string.error_publish_empty_page else R.string.error_publish_empty_post)
        if ((secondaryAction === SecondaryEditorAction.SAVE_AS_DRAFT
                    || secondaryAction === SecondaryEditorAction.SAVE)
        ) {
            message = getString(R.string.error_save_empty_draft)
        }
        ToastUtils.showToast(this@EditPostActivity, message, ToastUtils.Duration.SHORT)
    }

    private fun saveAsDraft() {
        editPostSettingsFragment?.updatePostStatus(PostStatus.DRAFT)
        ToastUtils.showToast(
            this@EditPostActivity,
            getString(R.string.editor_post_converted_back_to_draft), ToastUtils.Duration.SHORT
        )
        uploadUtilsWrapper.showSnackbar(
            findViewById(R.id.editor_activity),
            R.string.editor_uploading_post
        )
        savePostAndOptionallyFinish(doFinish = false, forceSave = false)
    }

    @Suppress("UseCheckOrError", "ReturnCount")
    private fun performSecondaryAction(): Boolean {
        if (UploadService.hasInProgressMediaUploadsForPost(editPostRepository.getPost())) {
            ToastUtils.showToast(
                this@EditPostActivity,
                getString(R.string.editor_toast_uploading_please_wait), ToastUtils.Duration.SHORT
            )
            return false
        }
        if (isDiscardable) {
            showEmptyPostErrorForSecondaryAction()
            return false
        }
        when (secondaryAction) {
            SecondaryEditorAction.SAVE_AS_DRAFT -> {
                // Force the new Draft status
                saveAsDraft()
                return true
            }

            SecondaryEditorAction.SAVE -> {
                uploadPost(false)
                return true
            }

            SecondaryEditorAction.PUBLISH_NOW -> {
                analyticsTrackerWrapper.track(Stat.EDITOR_POST_PUBLISH_TAPPED)
                publishPostImmediatelyUseCase.updatePostToPublishImmediately((editPostRepository), isNewPost)
                showPrepublishingNudgeBottomSheet()
                return true
            }

            SecondaryEditorAction.NONE ->
                throw IllegalStateException("Switch in `secondaryAction` shouldn't go through the NONE case")
        }
    }

    private fun refreshEditorContent() {
        hasSetPostContent = false
        fillContentEditorFields()
    }

    private fun setPreviewingInEditorSticky(enable: Boolean, post: PostImmutableModel?) {
        if (enable) {
            if (post != null) {
                EventBus.getDefault().postSticky(
                    PostPreviewingInEditor(post.localSiteId, post.id)
                )
            }
        } else {
            val stickyEvent: PostPreviewingInEditor? = EventBus.getDefault().getStickyEvent(
                PostPreviewingInEditor::class.java
            )
            if (stickyEvent != null) {
                EventBus.getDefault().removeStickyEvent(stickyEvent)
            }
        }
    }

    private fun managePostLoadingStateTransitions(
        postLoadingState: PostLoadingState,
        post: PostImmutableModel?
    ) {
        when (postLoadingState) {
            PostLoadingState.NONE -> setPreviewingInEditorSticky(false, post)
            PostLoadingState.UPLOADING_FOR_PREVIEW,
            PostLoadingState.REMOTE_AUTO_SAVING_FOR_PREVIEW,
            PostLoadingState.PREVIEWING,
            PostLoadingState.REMOTE_AUTO_SAVE_PREVIEW_ERROR -> setPreviewingInEditorSticky(
                true,
                post
            )

            PostLoadingState.LOADING_REVISION -> {}
        }
    }

    private fun updatePostLoadingAndDialogState(postLoadingState: PostLoadingState, post: PostImmutableModel? = null) {
        // We need only transitions, so...
        if (this.postLoadingState === postLoadingState) return
        AppLog.d(
            AppLog.T.POSTS,
            "Editor post loading state machine: transition from ${this.postLoadingState} to $postLoadingState"
        )

        // update the state
        this.postLoadingState = postLoadingState

        // take care of exit actions on state transition
        managePostLoadingStateTransitions(postLoadingState, post)

        // update the progress dialog state
        progressDialog = progressDialogHelper.updateProgressDialogState(
            this,
            progressDialog,
            this.postLoadingState.progressDialogUiState,
            (uiHelpers)
        )
    }

    private fun toggleHtmlModeOnMenu() {
        htmlModeMenuStateOn = !htmlModeMenuStateOn
        trackPostSessionEditorModeSwitch()
        invalidateOptionsMenu()
        showEditorModeSwitchedNotice()
    }

    private fun showEditorModeSwitchedNotice() {
        val message: String = getString(
            if (htmlModeMenuStateOn)
                R.string.menu_html_mode_switched_notice
            else R.string.menu_visual_mode_switched_notice
        )
        editorFragment?.showNotice(message)
    }

    private fun trackPostSessionEditorModeSwitch() {
        val isGutenberg: Boolean = editorFragment is GutenbergEditorFragment
        postEditorAnalyticsSession?.switchEditor(
            if (htmlModeMenuStateOn) PostEditorAnalyticsSession.Editor.HTML
            else
            (
                if (isGutenberg) PostEditorAnalyticsSession.Editor.GUTENBERG
                else PostEditorAnalyticsSession.Editor.CLASSIC
            )
        )
    }

    private fun performPrimaryAction() {
        when (primaryAction) {
            PrimaryEditorAction.PUBLISH_NOW -> {
                analyticsTrackerWrapper.track(Stat.EDITOR_POST_PUBLISH_TAPPED)
                showPrepublishingNudgeBottomSheet()
                return
            }

            PrimaryEditorAction.UPDATE,
            PrimaryEditorAction.CONTINUE,
            PrimaryEditorAction.SCHEDULE,
            PrimaryEditorAction.SUBMIT_FOR_REVIEW -> {
                showPrepublishingNudgeBottomSheet()
                return
            }

            PrimaryEditorAction.SAVE -> uploadPost(false)
        }
    }

    private fun showGutenbergInformativeDialog() {
        // We are no longer showing the dialog, but we are leaving all the surrounding logic because
        // this is going in shortly before release, and we're going to remove all this logic in the
        // very near future.
        AppPrefs.setGutenbergInfoPopupDisplayed(siteModel.url, true)
    }

    private fun showGutenbergRolloutV2InformativeDialog() {
        // We are no longer showing the dialog, but we are leaving all the surrounding logic because
        // this is going in shortly before release, and we're going to remove all this logic in the
        // very near future.
        AppPrefs.setGutenbergInfoPopupDisplayed(siteModel.url, true)
    }

    private fun setGutenbergEnabledIfNeeded() {
        if (AppPrefs.isGutenbergInfoPopupDisplayed(siteModel.url)) {
            return
        }
        val showPopup = AppPrefs.shouldShowGutenbergInfoPopupForTheNewPosts(siteModel.url)
        val showRolloutPopupPhase2 = AppPrefs.shouldShowGutenbergInfoPopupPhase2ForNewPosts(siteModel.url)
        if (TextUtils.isEmpty(siteModel.mobileEditor) && !isNewPost) {
            SiteUtils.enableBlockEditor(dispatcher, siteModel)
            AnalyticsUtils.trackWithSiteDetails(
                Stat.EDITOR_GUTENBERG_ENABLED, siteModel,
                BlockEditorEnabledSource.ON_BLOCK_POST_OPENING.asPropertyMap()
            )
        }
        if (showPopup) {
            showGutenbergInformativeDialog()
        } else if (showRolloutPopupPhase2) {
            showGutenbergRolloutV2InformativeDialog()
        }
    }

    private fun savePostOnline(isFirstTimePublish: Boolean): ActivityFinishState {
        if (editorFragment is GutenbergEditorFragment) {
            (editorFragment as GutenbergEditorFragment).sendToJSPostSaveEvent()
        }
        return storePostViewModel.savePostOnline(isFirstTimePublish, this, (editPostRepository), siteModel)
    }

    private fun onUploadSuccess(media: MediaModel?) {
        if (media != null) {
            // TODO Should this statement check media.getLocalPostId() == mEditPostRepository.getId()?
            if (!media.markedLocallyAsFeatured && editorMediaUploadListener != null) {
                editorMediaUploadListener?.onMediaUploadSucceeded(
                    media.id.toString(),
                    FluxCUtils.mediaFileFromMediaModel(media)
                )
            } else if (media.markedLocallyAsFeatured && media.localPostId == editPostRepository.id) {
                setFeaturedImageId(media.mediaId, imagePicked = false, isGutenbergEditor = false)
            }
        }
    }

    private fun onUploadProgress(media: MediaModel?, progress: Float) {
        val localMediaId = media?.id.toString()
        editorMediaUploadListener?.onMediaUploadProgress(localMediaId, progress)
    }

    private fun launchPictureLibrary() {
        WPMediaUtils.launchPictureLibrary(this, editorPhotoPicker?.allowMultipleSelection == true)
    }

    private fun launchVideoLibrary() {
        WPMediaUtils.launchVideoLibrary(this, editorPhotoPicker?.allowMultipleSelection == true)
    }

    private fun launchVideoCamera() {
        WPMediaUtils.launchVideoCamera(this)
    }

    private fun showErrorAndFinish(errorMessageId: Int) {
        ToastUtils.showToast(this, errorMessageId, ToastUtils.Duration.LONG)
        finish()
    }

    private fun updateAndSavePostAsync() {
        if (editorFragment == null) {
            AppLog.e(AppLog.T.POSTS, "Fragment not initialized")
            return
        }
        storePostViewModel.updatePostObjectWithUIAsync(
            (editPostRepository),
            { oldContent: String -> updateFromEditor(oldContent) },
            null
        )
    }

    private fun updateAndSavePostAsync(listener: OnPostUpdatedFromUIListener?) {
        if (editorFragment == null) {
            AppLog.e(AppLog.T.POSTS, "Fragment not initialized")
            return
        }
        storePostViewModel.updatePostObjectWithUIAsync(
            (editPostRepository), { oldContent: String -> updateFromEditor(oldContent) }
        ) { _: PostImmutableModel?, result: UpdatePostResult ->
            storePostViewModel.isSavingPostOnEditorExit = false
            // Ignore the result as we want to invoke the listener even when the PostModel was up-to-date
            listener?.onPostUpdatedFromUI(result)
        }
    }

    /**
     * This method:
     * 1. Shows and hides the editor's progress dialog;
     * 2. Saves the post via [EditPostActivity.updateAndSavePostAsync];
     * 3. Invokes the listener method parameter
     */
    private fun updateAndSavePostAsyncOnEditorExit(listener: OnPostUpdatedFromUIListener?) {
        if (editorFragment == null) {
            return
        }
        storePostViewModel.isSavingPostOnEditorExit = true
        storePostViewModel.showSavingProgressDialog()
        updateAndSavePostAsync(listener)
    }

    private fun updateFromEditor(oldContent: String): UpdateFromEditor {
        editorFragment?.let {
            return try {
                // To reduce redundant bridge events emitted to the Gutenberg editor, we get title and content at once
                val titleAndContent: Pair<CharSequence, CharSequence> = it.getTitleAndContent(oldContent)
                val title = titleAndContent.first as String
                val content = titleAndContent.second as String
                PostFields(title, content)
            } catch (e: EditorFragmentNotAddedException) {
                AppLog.e(AppLog.T.EDITOR, "Impossible to save the post, we weren't able to update it.")
                UpdateFromEditor.Failed(e)
            }
        }?:run { return UpdateFromEditor.Failed(java.lang.Exception("Impossible to save post, editor frag is null.")) }
    }

    override fun initializeEditorFragment() {
        if (editorFragment is AztecEditorFragment) {
            val aztecEditorFragment = editorFragment as AztecEditorFragment
            aztecEditorFragment.setEditorImageSettingsListener(this@EditPostActivity)
            aztecEditorFragment.setMediaToolbarButtonClickListener(editorPhotoPicker)

            // Here we should set the max width for media, but the default size is already OK. No need
            // to customize it further
            val loadingImagePlaceholder = EditorMediaUtils.getAztecPlaceholderDrawableFromResID(
                this,
                org.wordpress.android.editor.R.drawable.ic_gridicons_image,
                aztecEditorFragment.maxMediaSize
            )
            aztecImageLoader = AztecImageLoader(baseContext, (imageManager), loadingImagePlaceholder)
            aztecEditorFragment.setAztecImageLoader(aztecImageLoader)
            aztecEditorFragment.setLoadingImagePlaceholder(loadingImagePlaceholder)
            val loadingVideoPlaceholder = EditorMediaUtils.getAztecPlaceholderDrawableFromResID(
                this,
                org.wordpress.android.editor.R.drawable.ic_gridicons_video_camera,
                aztecEditorFragment.maxMediaSize
            )
            aztecEditorFragment.setAztecVideoLoader(AztecVideoLoader(baseContext, loadingVideoPlaceholder))
            aztecEditorFragment.setLoadingVideoPlaceholder(loadingVideoPlaceholder)
            if (site.isWPCom && !site.isPrivate) {
                // Add the content reporting for wpcom blogs that are not private
                val exceptionHandler: AztecExceptionHandler.ExceptionHandlerHelper =
                    object : AztecExceptionHandler.ExceptionHandlerHelper {
                        override fun shouldLog(ex: Throwable): Boolean {
                            // Do not log private or password protected post
                            return editPostRepository.hasPost() && editPostRepository.password.isEmpty()
                                    && !editPostRepository.hasStatus(PostStatus.PRIVATE)
                        }
                    }
                aztecEditorFragment.enableContentLogOnCrashes(exceptionHandler)
            }
            if (editPostRepository.hasPost() && AppPrefs
                    .isPostWithHWAccelerationOff(editPostRepository.localSiteId, editPostRepository.id)
            ) {
                // We need to disable HW Acc. on this post
                aztecEditorFragment.disableHWAcceleration()
            }
            aztecEditorFragment.setExternalLogger(object : AztecLog.ExternalLogger {
                // This method handles the custom Exception thrown by Aztec to notify the parent app of the error #8828
                // We don't need to log the error, since it was already logged by Aztec, instead we need to write the
                // prefs to disable HW acceleration for it.
                private fun isError8828(throwable: Throwable): Boolean {
                    return when {
                        throwable !is DynamicLayoutGetBlockIndexOutOfBoundsException ||
                                !editPostRepository.hasPost() -> {
                            false
                        }

                        else -> {
                            AppPrefs.addPostWithHWAccelerationOff(
                                editPostRepository.localSiteId,
                                editPostRepository.id
                            )
                            true
                        }
                    }
                }

                override fun log(message: String) {
                    AppLog.e(AppLog.T.EDITOR, message)
                }

                override fun logException(tr: Throwable) {
                    if (isError8828(tr)) {
                        return
                    }
                    AppLog.e(AppLog.T.EDITOR, tr)
                }

                override fun logException(tr: Throwable, message: String) {
                    if (isError8828(tr)) {
                        return
                    }
                    AppLog.e(AppLog.T.EDITOR, message)
                }
            })
        }
    }

    override fun onImageSettingsRequested(editorImageMetaData: EditorImageMetaData) {
        MediaSettingsActivity.showForResult(this, siteModel, editorImageMetaData)
    }

    override fun onImagePreviewRequested(mediaUrl: String) {
        MediaPreviewActivity.showPreview(this, siteModel, mediaUrl)
    }

    override fun onMediaEditorRequested(mediaUrl: String) {
        val imageUrl = UrlUtils.removeQuery(StringUtils.notNullStr(mediaUrl))

        // We're using a separate cache in WPAndroid and RN's Gutenberg editor so we need to reload the image
        // in the preview screen using WPAndroid's image loader. We create a resized url using Photon service and
        // device's max width to display a smaller image that can load faster and act as a placeholder.
        val displayWidth = max(
            DisplayUtils.getWindowPixelWidth(baseContext),
            DisplayUtils.getWindowPixelHeight(baseContext)
        )
        val margin = resources.getDimensionPixelSize(
            org.wordpress.android.imageeditor.R.dimen.preview_image_view_margin
        )
        val maxWidth = displayWidth - (margin * 2)
        val reducedSizeWidth = (maxWidth * PreviewImageFragment.PREVIEW_IMAGE_REDUCED_SIZE_FACTOR).toInt()
        val resizedImageUrl = readerUtilsWrapper.getResizedImageUrl(
            mediaUrl,
            reducedSizeWidth,
            0,
            !SiteUtils.isPhotonCapable(siteModel),
            siteModel.isWPComAtomic
        )
        val outputFileExtension: String = MimeTypeMap.getFileExtensionFromUrl(imageUrl)
        val inputData: ArrayList<InputData> = ArrayList(1)
        inputData.add(
            InputData(
                imageUrl,
                StringUtils.notNullStr(resizedImageUrl),
                outputFileExtension
            )
        )
        ActivityLauncher.openImageEditor(this, inputData)
    }

    /*
     * user clicked OK on a settings list dialog displayed from the settings fragment - pass the event
     * along to the settings fragment
     */
    override fun onPostSettingsFragmentPositiveButtonClicked(dialog: PostSettingsListDialogFragment) {
        editPostSettingsFragment?.onPostSettingsFragmentPositiveButtonClicked(dialog)
    }

    interface OnPostUpdatedFromUIListener {
        fun onPostUpdatedFromUI(updatePostResult: UpdatePostResult)
    }

    override fun onHistoryItemClicked(revision: Revision, revisions: List<Revision>) {
        AnalyticsTracker.track(Stat.REVISIONS_DETAIL_VIEWED_FROM_LIST)
        this.revision = revision
        val postId = editPostRepository.remotePostId
        this.revision?.let {
            ActivityLauncher.viewHistoryDetailForResult(
                this, it, getRevisionsIds(revisions), postId, siteModel.siteId
            )
        }
    }

    private fun getRevisionsIds(revisions: List<Revision>): LongArray {
        val idsArray = LongArray(revisions.size)
        for (i in revisions.indices) {
            val current: Revision = revisions[i]
            idsArray[i] = current.revisionId
        }
        return idsArray
    }

    private fun loadRevision() {
        updatePostLoadingAndDialogState(PostLoadingState.LOADING_REVISION)
        editPostRepository.saveForUndo()
        editPostRepository.updateAsync({ postModel: PostModel ->
            revision?.postTitle?.let {
                postModel.setTitle(it)
            }
            revision?.postContent?.let {
                postModel.setContent(it)
            }
            true
        }) { _: PostImmutableModel?, result: UpdatePostResult ->
            if (result === Updated) {
                refreshEditorContent()
                viewPager?.let {
                    make(it, getString(R.string.history_loaded_revision), SNACKBAR_DURATION)
                        .setAction(getString(R.string.undo)) { _: View? ->
                            AnalyticsTracker.track(Stat.REVISIONS_LOAD_UNDONE)
                            val payload = RemotePostPayload(editPostRepository.getPostForUndo(), siteModel)
                            dispatcher.dispatch(PostActionBuilder.newFetchPostAction(payload))
                            editPostRepository.undo()
                            refreshEditorContent()
                        }
                        .show()
                }
                updatePostLoadingAndDialogState(PostLoadingState.NONE)
            }
        }
    }

    private fun saveResult(saved: Boolean, uploadNotStarted: Boolean) {
        val i = intent
        i.putExtra(EditPostActivityConstants.EXTRA_UPLOAD_NOT_STARTED, uploadNotStarted)
        i.putExtra(EditPostActivityConstants.EXTRA_HAS_FAILED_MEDIA, hasFailedMedia())
        i.putExtra(EditPostActivityConstants.EXTRA_IS_PAGE, isPage)
        i.putExtra(EditPostActivityConstants.EXTRA_IS_LANDING_EDITOR, isLandingEditor)
        i.putExtra(EditPostActivityConstants.EXTRA_HAS_CHANGES, saved)
        i.putExtra(EditPostActivityConstants.EXTRA_POST_LOCAL_ID, editPostRepository.id)
        i.putExtra(EditPostActivityConstants.EXTRA_POST_REMOTE_ID, editPostRepository.remotePostId)
        i.putExtra(EditPostActivityConstants.EXTRA_RESTART_EDITOR, restartEditorOption.name)
        i.putExtra(EditPostActivityConstants.STATE_KEY_EDITOR_SESSION_DATA, postEditorAnalyticsSession)
        i.putExtra(EditPostActivityConstants.EXTRA_IS_NEW_POST, isNewPost)
        i.putExtra(EditPostActivityConstants.STATE_KEY_IS_NEW_GUTENBERG, isNewGutenbergEditor)
        setResult(RESULT_OK, i)
    }

    private fun setupPrepublishingBottomSheetRunnable() {
        showPrepublishingBottomSheetHandler = Handler()
        showPrepublishingBottomSheetRunnable = Runnable {
            val fragment = supportFragmentManager.findFragmentByTag(
                PrepublishingBottomSheetFragment.TAG
            )
            if (fragment == null) {
                val prepublishingFragment: PrepublishingBottomSheetFragment =
                    newInstance(site, isPage)
                prepublishingFragment.show(supportFragmentManager, PrepublishingBottomSheetFragment.TAG)
            }
        }
    }

    private fun showPrepublishingNudgeBottomSheet() {
        viewPager?.currentItem = PAGE_CONTENT
        ActivityUtils.hideKeyboard(this)
        val delayMs = PREPUBLISHING_NUDGE_BOTTOM_SHEET_DELAY
        showPrepublishingBottomSheetRunnable?.let {
            showPrepublishingBottomSheetHandler?.postDelayed(it, delayMs)
        }
    }

    override fun onSubmitButtonClicked(publishPost: Boolean) {
        uploadPost(publishPost)
        if (publishPost) {
            incrementInteractions(Stat.APP_REVIEWS_EVENT_INCREMENTED_BY_PUBLISHING_POST_OR_PAGE)
        }
    }

    private fun uploadPost(publishPost: Boolean) {
        updateAndSavePostAsyncOnEditorExit(object : OnPostUpdatedFromUIListener {
            override fun onPostUpdatedFromUI(updatePostResult: UpdatePostResult) {
                if (shouldPerformPostUpdateAndPublish()) {
                    performPostUpdateAndPublish(publishPost)
                }
            }
        })
    }
    @Suppress("ReturnCount")
    private fun shouldPerformPostUpdateAndPublish() : Boolean {
        val account: AccountModel = accountStore.account
        // prompt user to verify e-mail before publishing
        if (!account.emailVerified) {
            storePostViewModel.hideSavingProgressDialog()
            val message: String =
                if (TextUtils.isEmpty(account.email)) getString(R.string.editor_confirm_email_prompt_message)
                else String.format(
                    getString(R.string.editor_confirm_email_prompt_message_with_email),
                    account.email
                )
            val builder: AlertDialog.Builder = MaterialAlertDialogBuilder(this)
            builder.setTitle(R.string.editor_confirm_email_prompt_title)
                .setMessage(message)
                .setPositiveButton(android.R.string.ok
                ) { _, _ ->
                    ToastUtils.showToast(
                        this@EditPostActivity,
                        getString(R.string.toast_saving_post_as_draft)
                    )
                    savePostAndOptionallyFinish(doFinish = true, forceSave = false)
                }
                .setNegativeButton(
                    R.string.editor_confirm_email_prompt_negative
                ) { _, _ ->
                    dispatcher
                        .dispatch(AccountActionBuilder.newSendVerificationEmailAction())
                }
            builder.create().show()
            return false
        }

        editPostRepository.getPost()?.let {
            if (!postUtilsWrapper.isPublishable(it)) {
                storePostViewModel.hideSavingProgressDialog()
                // TODO we don't want to show "publish" message when the user clicked on eg. save
                editPostRepository.updateStatusFromPostSnapshotWhenEditorOpened()
                runOnUiThread {
                    val message: String = getString(
                        if (isPage) R.string.error_publish_empty_page else R.string.error_publish_empty_post
                    )
                    ToastUtils.showToast(
                        this@EditPostActivity,
                        message,
                        ToastUtils.Duration.SHORT
                    )
                }
                return false
            }
        }
        return true
    }

    private fun performPostUpdateAndPublish(publishPost: Boolean) {
        storePostViewModel.showSavingProgressDialog()
        val isFirstTimePublish: Boolean = isFirstTimePublish(publishPost)
        editPostRepository.updateAsync( { postModel: PostModel ->
            if (publishPost) {
                // now set status to PUBLISHED - only do this AFTER we have run the isFirstTimePublish() check,
                // otherwise we'd have an incorrect value
                // also re-set the published date in case it was SCHEDULED and they want to publish NOW
                if ((postModel.status == PostStatus.SCHEDULED.toString())) {
                    postModel.setDateCreated(dateTimeUtils.currentTimeInIso8601())
                }
                if (uploadUtilsWrapper.userCanPublish(site)) {
                    postModel.setStatus(PostStatus.PUBLISHED.toString())
                } else {
                    postModel.setStatus(PostStatus.PENDING.toString())
                }
                postEditorAnalyticsSession?.setOutcome(Outcome.PUBLISH)
            } else {
                postEditorAnalyticsSession?.setOutcome(Outcome.SAVE)
            }
            AppLog.d(
                AppLog.T.POSTS,
                "User explicitly confirmed changes. Post Title: " + postModel.title
            )
            // the user explicitly confirmed an intention to upload the post
            postModel.setChangesConfirmedContentHashcode(postModel.contentHashcode())
            true
        } )
        { _: PostImmutableModel?, result: UpdatePostResult ->
            if (result === Updated) {
                val activityFinishState: ActivityFinishState = savePostOnline(isFirstTimePublish)
                storePostViewModel.finish(activityFinishState)
            }
        }
    }

    private fun savePostAndOptionallyFinish(doFinish: Boolean, forceSave: Boolean) {
        if (editorFragment?.isAdded != true) {
            AppLog.e(AppLog.T.POSTS, "Fragment not initialized")
            return
        }
        val lambda: (UpdatePostResult?) -> Unit = { _ ->
            // check if the opened post had some unsaved local changes
            val isFirstTimePublish = isFirstTimePublish(false)

            // if post was modified during this editing session, save it
            val shouldSave = shouldSavePost() || forceSave
            postEditorAnalyticsSession?.setOutcome(Outcome.SAVE)
            var activityFinishState: ActivityFinishState? = ActivityFinishState.CANCELLED
            if (shouldSave) {
                /*
                 * Remote-auto-save isn't supported on self-hosted sites. We can save the post online (as draft)
                 * only when it doesn't exist in the remote yet. When it does exist in the remote, we can upload
                 * it only when the user explicitly confirms the changes - eg. clicks on save/publish/submit. The
                 * user didn't confirm the changes in this code path.
                 */
                val isWpComOrIsLocalDraft: Boolean =
                    siteModel.isUsingWpComRestApi || editPostRepository.isLocalDraft
                activityFinishState = if (isWpComOrIsLocalDraft) {
                    savePostOnline(isFirstTimePublish)
                } else if (forceSave) {
                    savePostOnline(false)
                } else {
                    ActivityFinishState.SAVED_LOCALLY
                }
            }
            // discard post if new & empty
            if (isDiscardable) {
                dispatcher.dispatch(PostActionBuilder.newRemovePostAction(editPostRepository.getEditablePost()))
                postEditorAnalyticsSession?.setOutcome(Outcome.CANCEL)
                activityFinishState = ActivityFinishState.CANCELLED
            }
            if (doFinish) {
                activityFinishState?.let {
                    storePostViewModel.finish(it)
                }
            }
        }

        // Convert the lambda to OnPostUpdatedFromUIListener and pass it to the method
        updateAndSavePostAsyncOnEditorExit(lambdaToListener(lambda))
    }

    // Helper function to convert a lambda to OnPostUpdatedFromUIListener
    private fun lambdaToListener(lambda: (UpdatePostResult) -> Unit): OnPostUpdatedFromUIListener {
        return object : OnPostUpdatedFromUIListener {
            override fun onPostUpdatedFromUI(updatePostResult: UpdatePostResult) {
                lambda(updatePostResult)
            }
        }
    }

    private fun shouldSavePost(): Boolean {
        val hasChanges = editPostRepository.postWasChangedInCurrentSession()
        val isPublishable = editPostRepository.isPostPublishable()
        val existingPostWithChanges = editPostRepository.hasPostSnapshotWhenEditorOpened() && hasChanges
        // if post was modified during this editing session, save it
        return isPublishable && (existingPostWithChanges || isNewPost)
    }

    private val isDiscardable: Boolean
        get() {
            return !editPostRepository.isPostPublishable() && isNewPost
        }

    private fun isFirstTimePublish(publishPost: Boolean): Boolean {
        val originalStatus = editPostRepository.status
        return (((originalStatus == PostStatus.DRAFT || originalStatus == PostStatus.UNKNOWN) && publishPost)
                || (originalStatus == PostStatus.SCHEDULED && publishPost)
                || (originalStatus == PostStatus.PUBLISHED && editPostRepository.isLocalDraft)
                || (originalStatus == PostStatus.PUBLISHED && editPostRepository.remotePostId == 0L))
    }

    /**
     * Can be dropped and replaced by mEditorFragment.hasFailedMediaUploads() when we drop the visual editor.
     * mEditorFragment.isActionInProgress() was added to address a timing issue when adding media and immediately
     * publishing or exiting the visual editor. It's not safe to upload the post in this state.
     * See https://github.com/wordpress-mobile/WordPress-Editor-Android/issues/294
     */
    private fun hasFailedMedia(): Boolean {
        return editorFragment?.hasFailedMediaUploads() == true || editorFragment?.isActionInProgress == true
    }

    /**
     * A [FragmentPagerAdapter] that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    inner class SectionsPagerAdapter internal constructor(fm: FragmentManager) :
        FragmentPagerAdapter(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {
        @Suppress("ReturnCount")
        override fun getItem(position: Int): Fragment {
            return when (position) {
                PAGE_CONTENT -> {
                    if (showGutenbergEditor) {
                        createGutenbergEditorFragment()
                    } else {
                        // If gutenberg editor is not selected, default to Aztec.
                        AztecEditorFragment.newInstance("", "", AppPrefs.isAztecEditorToolbarExpanded())
                    }
                }
                PAGE_SETTINGS -> EditPostSettingsFragment.newInstance()
                PAGE_PUBLISH_SETTINGS -> newInstance()
                PAGE_HISTORY -> newInstance(editPostRepository.id, siteModel)
                else -> throw IllegalArgumentException("Unexpected page type")
            }
        }

        private fun createGutenbergEditorFragment(): GutenbergEditorFragment {
            // Enable gutenberg on the site & show the informative popup upon opening
            // the GB editor the first time when the remote setting value is still null
            setGutenbergEnabledIfNeeded()
            xPostsCapabilityChecker.retrieveCapability(siteModel) { isXpostsCapable ->
                onXpostsSettingsCapability(isXpostsCapable)
            }

            val isWpCom = site.isWPCom || siteModel.isPrivateWPComAtomic || siteModel.isWPComAtomic
            val gutenbergPropsBuilder = gutenbergPropsBuilder
            val gutenbergWebViewAuthorizationData = GutenbergWebViewAuthorizationData(
                siteModel.url,
                isWpCom,
                accountStore.account.userId,
                accountStore.account.userName,
                accountStore.accessToken,
                siteModel.selfHostedSiteId,
                siteModel.username,
                siteModel.password,
                siteModel.isUsingWpComRestApi,
                siteModel.webEditor,
                userAgent.toString(),
                isJetpackSsoEnabled
            )

            val postType = if (editPostRepository.isPage) "page" else "post"
            val siteApiRoot = if (isWpCom) "https://public-api.wordpress.com/" else ""
            val siteId = site.siteId
            val authToken = accountStore.accessToken
            val authHeader = "Bearer $authToken"
            val siteApiNamespace = "sites/$siteId"

            val settings = mutableMapOf<String, Any?>(
                "postId" to editPostRepository.getPost()?.remotePostId?.toInt(),
                "postType" to postType,
                "postTitle" to editPostRepository.getPost()?.title,
                "postContent" to editPostRepository.getPost()?.content,
                "siteApiRoot" to siteApiRoot,
                "authHeader" to authHeader,
                "siteApiNamespace" to siteApiNamespace
            )

            return GutenbergEditorFragment.newInstance(
                getContext(),
                isNewPost,
                gutenbergWebViewAuthorizationData,
                gutenbergPropsBuilder,
                jetpackFeatureRemovalPhaseHelper.shouldShowJetpackPoweredEditorFeatures(),
                isNewGutenbergEditor,
                settings
            )
        }

        override fun instantiateItem(container: ViewGroup, position: Int): Any {
            val fragment: Fragment = super.instantiateItem(container, position) as Fragment
            when (position) {
                PAGE_CONTENT -> {
                    editorFragment = fragment as EditorFragmentAbstract
                    editorFragment?.setImageLoader(imageLoader)
                    if (isNewGutenbergEditor) {
                        editorFragment?.onEditorContentChanged(object : GutenbergView.ContentChangeListener {
                            override fun onContentChanged(title: String, content: String) {
                                storePostViewModel.savePostWithDelay()
                            }
                        })
                    } else {
                        editorFragment?.titleOrContentChanged?.observe(this@EditPostActivity) { _: Editable? ->
                            storePostViewModel.savePostWithDelay()
                        }
                    }
                    if (editorFragment is EditorMediaUploadListener) {
                        editorMediaUploadListener = editorFragment as EditorMediaUploadListener?

                        // Set up custom headers for the visual editor's internal WebView
                        editorFragment?.setCustomHttpHeader("User-Agent", userAgent.toString())
                        reattachUploadingMediaForAztec()
                    }
                }
                PAGE_SETTINGS -> editPostSettingsFragment = fragment as EditPostSettingsFragment
            }
            return fragment
        }

        override fun getCount(): Int {
            return numPagesInEditor
        }

        private val numPagesInEditor: Int = 4
    }

    private fun onXpostsSettingsCapability(isXpostsCapable: Boolean) {
        isXPostsCapable = isXpostsCapable
        if (editorFragment is GutenbergEditorFragment) {
            (editorFragment as GutenbergEditorFragment).updateCapabilities(gutenbergPropsBuilder)
        }
    }

    private val gutenbergPropsBuilder: GutenbergPropsBuilder
         get() {
            val postType = if (isPage) "page" else "post"
            val featuredImageId = editPostRepository.featuredImageId.toInt()
            val languageString = LocaleManager.getLanguage(this@EditPostActivity)
            val wpcomLocaleSlug = languageString.replace("_", "-").lowercase()

            // this.mIsXPostsCapable may return true for non-WP.com sites, but the app only supports xPosts for P2-based
            // WP.com sites so, gate with `isUsingWpComRestApi()`
            // If this.mIsXPostsCapable has not been set, default to allowing xPosts.
            val enableXPosts = siteModel.isUsingWpComRestApi && (isXPostsCapable == null || isXPostsCapable == true)
            val editorTheme = editorThemeStore.getEditorThemeForSite((siteModel))
            val themeBundle = if ((editorTheme != null)) editorTheme.themeSupport.toBundle((siteModel)) else null
            val isUnsupportedBlockEditorEnabled = siteModel.isWPCom || isJetpackSsoEnabled
            val unsupportedBlockEditorSwitch = siteModel.isJetpackConnected && !isJetpackSsoEnabled
            val isFreeWPCom = siteModel.isWPCom && SiteUtils.onFreePlan((siteModel))
            val isWPComSite = siteModel.isWPCom || siteModel.isWPComAtomic
            val shouldUseFastImage = !siteModel.isPrivate && !siteModel.isPrivateWPComAtomic
            val hostAppNamespace = if (buildConfigWrapper.isJetpackApp) "Jetpack" else "WordPress"

            // Disable Jetpack-powered editor features in WordPress app based on Jetpack Features Removal Phase helper
            val jetpackFeaturesRemoved = !jetpackFeatureRemovalPhaseHelper.shouldShowJetpackPoweredEditorFeatures()
            if (jetpackFeaturesRemoved) {
                return GutenbergPropsBuilder(
                    enableContactInfoBlock = false,
                    enableLayoutGridBlock = false,
                    enableTiledGalleryBlock = false,
                    enableVideoPressBlock = false,
                    enableVideoPressV5Support = false,
                    enableFacebookEmbed = false,
                    enableInstagramEmbed = false,
                    enableLoomEmbed = false,
                    enableSmartframeEmbed = false,
                    enableMentions = false,
                    enableXPosts = false,
                    enableUnsupportedBlockEditor = false,
                    enableSupportSection = false,
                    enableOnlyCoreBlocks = true,
                    unsupportedBlockEditorSwitch = false,
                    !isFreeWPCom,
                    shouldUseFastImage,
                    enableReusableBlock = false,
                    wpcomLocaleSlug,
                    postType,
                    hostAppNamespace,
                    featuredImageId,
                    themeBundle
                )
            }
            return GutenbergPropsBuilder(
                SiteUtils.supportsContactInfoFeature(siteModel),
                SiteUtils.supportsLayoutGridFeature(siteModel),
                SiteUtils.supportsTiledGalleryFeature(siteModel),
                SiteUtils.supportsVideoPressFeature(siteModel),
                SiteUtils.supportsVideoPressV5Feature(siteModel, SiteUtils.WP_VIDEOPRESS_V5_JETPACK_VERSION),
                SiteUtils.supportsEmbedVariationFeature(siteModel, SiteUtils.WP_FACEBOOK_EMBED_JETPACK_VERSION),
                SiteUtils.supportsEmbedVariationFeature(siteModel, SiteUtils.WP_INSTAGRAM_EMBED_JETPACK_VERSION),
                SiteUtils.supportsEmbedVariationFeature(siteModel, SiteUtils.WP_LOOM_EMBED_JETPACK_VERSION),
                SiteUtils.supportsEmbedVariationFeature(siteModel, SiteUtils.WP_SMARTFRAME_EMBED_JETPACK_VERSION),
                siteModel.isUsingWpComRestApi,
                enableXPosts,
                isUnsupportedBlockEditorEnabled,
                enableSupportSection = true,
                enableOnlyCoreBlocks = false,
                unsupportedBlockEditorSwitch,
                !isFreeWPCom,
                shouldUseFastImage,
                isWPComSite,
                wpcomLocaleSlug,
                postType,
                hostAppNamespace,
                featuredImageId,
                themeBundle
            )
        }

    private var mediaCapturePath: String? = ""
    private fun getUploadErrorHtml(mediaId: String, path: String): String {
        return String.format(
            Locale.US,
            ("<span id=\"img_container_%s\" class=\"img_container failed\" data-failed=\"%s\">"
                    + "<progress id=\"progress_%s\" value=\"0\" class=\"wp_media_indicator failed\" "
                    + "contenteditable=\"false\"></progress>"
                    + "<img data-wpid=\"%s\" src=\"%s\" alt=\"\" class=\"failed\"></span>"),
            mediaId, getString(R.string.tap_to_try_again), mediaId, mediaId, path
        )
    }

    private fun migrateLegacyDraft(inputContent: String): String {
        var content = inputContent
        if (content.contains("<img src=\"null\" android-uri=\"")) {
            // We must replace image tags specific to the legacy editor local drafts:
            // <img src="null" android-uri="file:///..." />
            // And trigger an upload action for the specific image / video
            val pattern: Pattern = Pattern.compile("<img src=\"null\" android-uri=\"([^\"]*)\".*>")
            val matcher: Matcher = pattern.matcher(content)
            val stringBuffer = StringBuffer()
            while (matcher.find()) {
                val stringUri = matcher.group(1)
                val uri = Uri.parse(stringUri)
                val mediaFile = FluxCUtils.mediaFileFromMediaModel(
                    editorMedia
                        .updateMediaUploadStateBlocking(uri, MediaUploadState.FAILED)
                ) ?: continue
                val replacement = getUploadErrorHtml(mediaFile.id.toString(), mediaFile.filePath)
                matcher.appendReplacement(stringBuffer, replacement)
            }
            matcher.appendTail(stringBuffer)
            content = stringBuffer.toString()
        }
        if (content.contains("[caption")) {
            // Convert old legacy post caption formatting to new format, to avoid being stripped by the visual editor
            val pattern = Pattern.compile("(\\[caption[^]]*caption=\"([^\"]*)\"[^]]*].+?)(\\[/caption])")
            val matcher = pattern.matcher(content)
            val stringBuffer = StringBuffer()
            while (matcher.find()) {
                val group1 = matcher.group(GROUP_ONE)
                val group2 = matcher.group(GROUP_TWO)
                val group3 = matcher.group(GROUP_THREE)
                if (group1 != null && group2 != null && group3 != null) {
                    val replacement = group1 + group2 + group3
                    matcher.appendReplacement(stringBuffer, replacement)
                }
            }
            matcher.appendTail(stringBuffer)
            content = stringBuffer.toString()
        }
        return content
    }

    private fun migrateToGutenbergEditor(content: String): String {
        return "<!-- wp:paragraph --><p>$content</p><!-- /wp:paragraph -->"
    }

    private fun fillContentEditorFields() {
        // Needed blog settings needed by the editor
        editorFragment?.setFeaturedImageSupported(siteModel.isFeaturedImageSupported)

        // Special actions - these only make sense for empty posts that are going to be populated now
        if (editPostRepository.hasPost() && TextUtils.isEmpty(editPostRepository.content)) {
            val action = intent.action
            if ((Intent.ACTION_SEND_MULTIPLE == action)) {
                setPostContentFromShareAction()
            } else if ((NEW_MEDIA_POST == action)) {
                intent.getLongArrayExtra(NEW_MEDIA_POST_EXTRA_IDS)?.let {
                    editorMedia.addExistingMediaToEditorAsync(AddExistingMediaSource.WP_MEDIA_LIBRARY, it)
                }
            }
        }
        if (isPage) {
            setPageContent()
        }

        // Set post title and content
        if (editPostRepository.hasPost()) {
            // don't avoid calling setContent() for GutenbergEditorFragment so RN gets initialized
            if (((!TextUtils.isEmpty(editPostRepository.content)
                        || editorFragment is GutenbergEditorFragment)
                        && !hasSetPostContent)
            ) {
                hasSetPostContent = true
                // NOTE: Might be able to drop .replaceAll() when legacy editor is removed
                var content = editPostRepository.content.replace("\uFFFC".toRegex(), "")
                // Prepare eventual legacy editor local draft for the new editor
                content = migrateLegacyDraft(content)
                editorFragment?.setContent(content)
            }
            if (!TextUtils.isEmpty(editPostRepository.title)) {
                editorFragment?.setTitle(editPostRepository.title)
            } else if (editorFragment is GutenbergEditorFragment) {
                // don't avoid calling setTitle() for GutenbergEditorFragment so RN gets initialized
                val title: String? = intent.getStringExtra(EditPostActivityConstants.EXTRA_PAGE_TITLE)
                if (title != null) {
                    editorFragment?.setTitle(title)
                } else {
                    editorFragment?.setTitle("")
                }
            }

            // TBD: postSettingsButton.setText(post.isPage() ? R.string.page_settings : R.string.post_settings);
            editorFragment?.setFeaturedImageId(editPostRepository.featuredImageId)
        }
    }

    private fun launchCamera() {
        WPMediaUtils.launchCamera(
            this,
            BuildConfig.APPLICATION_ID,
            object : WPMediaUtils.LaunchCameraCallback {
                override fun onMediaCapturePathReady(mediaCapturePath: String?) {
                  this@EditPostActivity.mediaCapturePath = mediaCapturePath
                }

                override fun onCameraError(errorMessage: String?) {
                    ToastUtils.showToast(
                        this@EditPostActivity,
                        errorMessage,
                        ToastUtils.Duration.SHORT
                    )
                }
            }
        )
    }

    private fun setPostContentFromShareAction() {
        val intent: Intent = intent

        // Check for shared text
        val text = intent.getStringExtra(Intent.EXTRA_TEXT)
        val title = intent.getStringExtra(Intent.EXTRA_SUBJECT)
        if (text != null) {
            hasSetPostContent = true
            editPostRepository.updateAsync({ postModel: PostModel ->
                if (title != null) {
                    postModel.setTitle(title)
                }
                // Create an <a href> element around links
                var updatedContent: String = AutolinkUtils.autoCreateLinks(text)

                // If editor is Gutenberg, add Gutenberg block around content
                if (showGutenbergEditor) {
                    updatedContent = migrateToGutenbergEditor(updatedContent)
                }

                // update PostModel
                postModel.setContent(updatedContent)
                editPostRepository.updatePublishDateIfShouldBePublishedImmediately(postModel)
                true
            }) { postModel: PostImmutableModel, result: UpdatePostResult ->
                if (result === Updated) {
                    editorFragment?.setTitle(postModel.title)
                    editorFragment?.setContent(postModel.content)
                }
            }
        }
        setPostMediaFromShareAction()
    }
    private fun setPostMediaFromShareAction() {
        // Short-circuit the method if Intent.EXTRA_STREAM is not found
        if (!intent.hasExtra(Intent.EXTRA_STREAM)) {
            return
        }

        // Check for shared media
        val action = intent.action
        val sharedUris: ArrayList<Uri> = ArrayList()

        if ((Intent.ACTION_SEND_MULTIPLE == action)) {
            val potentialUris: ArrayList<Uri>? = intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM)
            potentialUris?.forEach { uri ->
                if (isMediaTypeIntent(intent, uri)) {
                    sharedUris.add(uri)
                }
            }
        } else {
            // For a single media share, we only allow images and video types
            if (isMediaTypeIntent(intent, null)) {
                intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)?.let {
                    sharedUris.add(it)
                }
            }
        }

        if (sharedUris.isNotEmpty()) {
            // removing this from the intent so it doesn't insert the media items again on each Activity re-creation
            intent.removeExtra(Intent.EXTRA_STREAM)
            editorMedia.addNewMediaItemsToEditorAsync(sharedUris, false)
        }
    }

    private fun isMediaTypeIntent(intent: Intent, uri: Uri?): Boolean {
        var type: String? = null
        if (uri != null) {
            val extension = MimeTypeMap.getFileExtensionFromUrl(uri.toString())
            if (extension != null) {
                type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
            }
        } else {
            type = intent.type
        }
        return type != null && (type.startsWith("image") || type.startsWith("video"))
    }

    private fun setFeaturedImageId(mediaId: Long, imagePicked: Boolean, isGutenbergEditor: Boolean) {
        if (isGutenbergEditor) {
            val postRepository: EditPostRepository = editPostRepository
            val postId = editPostRepository.id
            if (mediaId == GutenbergEditorFragment.MEDIA_ID_NO_FEATURED_IMAGE_SET.toLong()) {
                featuredImageHelper.trackFeaturedImageEvent(
                    FeaturedImageHelper.TrackableEvent.IMAGE_REMOVED_GUTENBERG_EDITOR,
                    postId
                )
            } else {
                featuredImageHelper.trackFeaturedImageEvent(
                    FeaturedImageHelper.TrackableEvent.IMAGE_PICKED_GUTENBERG_EDITOR,
                    postId
                )
            }
            updateFeaturedImageUseCase.updateFeaturedImage(
                mediaId, postRepository
            ) { _: PostImmutableModel? ->
             }
        } else if (editPostSettingsFragment != null) {
            editPostSettingsFragment?.updateFeaturedImage(mediaId, imagePicked)
        }
        if (editorFragment is GutenbergEditorFragment) {
            (editorFragment as GutenbergEditorFragment).sendToJSFeaturedImageId(mediaId.toInt())
        }
    }

    /**
     * Sets the page content
     */
    private fun setPageContent() {
        val intent: Intent = intent
        val content: String? = intent.getStringExtra(EditPostActivityConstants.EXTRA_PAGE_CONTENT)
        if (!content.isNullOrEmpty()) {
            hasSetPostContent = true
            editPostRepository.updateAsync({ postModel: PostModel ->
                postModel.setContent(content)
                editPostRepository.updatePublishDateIfShouldBePublishedImmediately(postModel)
                true
            }) { postModel: PostImmutableModel, result: UpdatePostResult ->
                if (result === Updated) {
                    editorFragment?.setContent(postModel.content)
                }
            }
        }
    }

    @Deprecated("Deprecated in Java")
    public override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        // In case of Remote Preview we need to change state even if (resultCode != Activity.RESULT_OK)
        // so placing this here before the check
        if (requestCode == RequestCodes.REMOTE_PREVIEW_POST) {
            updatePostLoadingAndDialogState(PostLoadingState.NONE)
            return
        }

        if (resultCode != RESULT_OK) {
            return handleNotOKRequest(resultCode)
        }

        val shouldHandleRequest = (requestCode == RequestCodes.TAKE_PHOTO) ||
                (requestCode == RequestCodes.TAKE_VIDEO) ||
                (requestCode == RequestCodes.PHOTO_PICKER)

        if (data != null || shouldHandleRequest)
            handleRequest(requestCode, data)

        if (requestCode == JetpackSecuritySettingsActivity.JETPACK_SECURITY_SETTINGS_REQUEST_CODE) {
            fetchSiteSettings()
        }
    }

    private fun handleNotOKRequest(requestCode: Int) {
        // for all media related intents, let editor fragment know about cancellation
        when (requestCode) {
            RequestCodes.MULTI_SELECT_MEDIA_PICKER,
            RequestCodes.SINGLE_SELECT_MEDIA_PICKER,
            RequestCodes.PHOTO_PICKER,
            RequestCodes.STORIES_PHOTO_PICKER,
            RequestCodes.STOCK_MEDIA_PICKER_SINGLE_SELECT,
            RequestCodes.MEDIA_LIBRARY,
            RequestCodes.PICTURE_LIBRARY,
            RequestCodes.TAKE_PHOTO,
            RequestCodes.VIDEO_LIBRARY,
            RequestCodes.TAKE_VIDEO,
            RequestCodes.STOCK_MEDIA_PICKER_MULTI_SELECT,
            RequestCodes.STOCK_MEDIA_PICKER_SINGLE_SELECT_FOR_GUTENBERG_BLOCK -> {
                editorFragment?.mediaSelectionCancelled()
                return
            }
            else ->                     // noop
                return
        }
    }
    private fun handleRequest(requestCode: Int, data: Intent?) {
        when (requestCode) {
            RequestCodes.MULTI_SELECT_MEDIA_PICKER,
            RequestCodes.SINGLE_SELECT_MEDIA_PICKER -> handleMediaPickerResult(data)
            RequestCodes.PHOTO_PICKER,
            RequestCodes.STOCK_MEDIA_PICKER_SINGLE_SELECT -> handlePhotoPickerResult(data)
            RequestCodes.STOCK_MEDIA_PICKER_SINGLE_SELECT_FOR_GUTENBERG_BLOCK ->
                handleStockMediaPickerSingleSelect(data)
            RequestCodes.MEDIA_LIBRARY,
            RequestCodes.PICTURE_LIBRARY,
            RequestCodes.VIDEO_LIBRARY -> handleLibraries(data)
            RequestCodes.TAKE_PHOTO -> addLastTakenPicture()
            RequestCodes.TAKE_VIDEO -> handleTakeVideo(data)
            RequestCodes.MEDIA_SETTINGS -> handleMediaSettings(data)
            RequestCodes.STOCK_MEDIA_PICKER_MULTI_SELECT -> handleStockMediaPickerMultiSelect(data)
            RequestCodes.GIF_PICKER_SINGLE_SELECT,
            RequestCodes.GIF_PICKER_MULTI_SELECT -> handleGifPicker(data)
            RequestCodes.HISTORY_DETAIL -> handleHistoryDetail()
            RequestCodes.IMAGE_EDITOR_EDIT_IMAGE -> handleImageEditor(data)
            RequestCodes.SELECTED_USER_MENTION -> handleUserMention(data)
            RequestCodes.FILE_LIBRARY,
            RequestCodes.AUDIO_LIBRARY -> handleFileOrAudioLibrary(data)
        }
    }

    private fun handleStockMediaPickerSingleSelect(data: Intent?) {
        if (data?.hasExtra(MediaPickerConstants.EXTRA_MEDIA_ID) == true
        ) {
            // pass array with single item
            val mediaIds = longArrayOf(data.getLongExtra(MediaPickerConstants.EXTRA_MEDIA_ID, 0))
            editorMedia
                .addExistingMediaToEditorAsync(AddExistingMediaSource.STOCK_PHOTO_LIBRARY, mediaIds)
        }
    }

    private fun handleLibraries(data: Intent?) {
        editorMedia.addNewMediaItemsToEditorAsync(
            WPMediaUtils.retrieveMediaUris(data),
            false
        )
    }

    private fun handleTakeVideo(data: Intent?){
        data?.data?.let {
            editorMedia.addNewMediaToEditorAsync(it, true)
        }
    }

    private fun handleMediaSettings(data: Intent?) {
        if (editorFragment is AztecEditorFragment) {
            editorFragment?.onActivityResult(
                AztecEditorFragment.EDITOR_MEDIA_SETTINGS,
                RESULT_OK, data
            )
        }
    }

    private fun handleStockMediaPickerMultiSelect(data: Intent?) {
        val key = MediaBrowserActivity.RESULT_IDS
        if (data?.hasExtra(key) == true) {
            val mediaIds: LongArray? = data.getLongArrayExtra(key)
            mediaIds?.let {
                editorMedia.addExistingMediaToEditorAsync(
                    AddExistingMediaSource.STOCK_PHOTO_LIBRARY,
                    it
                )
            }
        }
    }

    private fun handleGifPicker(data: Intent?) {
        val localIds = data?.getIntArrayExtra(MediaPickerConstants.EXTRA_SAVED_MEDIA_MODEL_LOCAL_IDS)
        if (localIds != null && localIds.isNotEmpty()) {
            editorMedia.addGifMediaToPostAsync(localIds)
        }
    }

    private fun handleHistoryDetail() {
        if (dB?.hasParcel(KEY_REVISION) == true) {
            viewPager?.currentItem = PAGE_CONTENT
            revision = dB?.getParcel(KEY_REVISION, parcelableCreator())
            Handler().postDelayed(
                { loadRevision() },
                resources.getInteger(R.integer.full_screen_dialog_animation_duration).toLong()
            )
        }
    }

    private fun handleImageEditor(data: Intent?) {
        val uris: List<Uri> = WPMediaUtils.retrieveImageEditorResult(data)
        imageEditorTracker.trackAddPhoto(uris)
        uris.forEach { item ->
            item.let { editorMedia.addNewMediaToEditorAsync(it, false) }
        }
    }

    private fun handleUserMention(data: Intent?) {
        if (onGetSuggestionResult != null) {
            val selectedMention: String? = data?.getStringExtra(SuggestionActivity.SELECTED_VALUE)
            onGetSuggestionResult?.accept(selectedMention)
            // Clear the callback once we have gotten a result
            onGetSuggestionResult = null
        }
    }

    private fun handleFileOrAudioLibrary(data: Intent?) {
        if (data?.hasExtra(MediaPickerConstants.EXTRA_MEDIA_URIS) == true) {
            val uriResults: List<Uri> = data.getStringArrayExtra(MediaPickerConstants.EXTRA_MEDIA_URIS)
                ?.let { convertStringArrayIntoUrisList(it) }
                ?: emptyList()

            uriResults.forEach { uri ->
                uri.let { editorMedia.addNewMediaToEditorAsync(it, false) }
            }
        }
    }

    private fun convertStringArrayIntoUrisList(stringArray: Array<String>?): List<Uri> {
        val uris: MutableList<Uri> = ArrayList(stringArray?.size ?: 0)
        stringArray?.forEach { stringUri ->
            uris.add(Uri.parse(stringUri))
        }
        return uris
    }

    @Suppress("TooGenericExceptionCaught")
    private fun addLastTakenPicture() {
        try {
            mediaCapturePath?.let { path ->
                WPMediaUtils.scanMediaFile(this, path)
                val f = File(path)
                val capturedImageUri: Uri? = Uri.fromFile(f)
                if (capturedImageUri != null) {
                    editorMedia.addNewMediaToEditorAsync(capturedImageUri, true)
                } else {
                    ToastUtils.showToast(this, R.string.gallery_error, ToastUtils.Duration.SHORT)
                }
            }
        } catch (e: RuntimeException) {
            AppLog.e(AppLog.T.EDITOR, e)
        } catch (e: OutOfMemoryError) {
            AppLog.e(AppLog.T.EDITOR, e)
        } finally {
            mediaCapturePath = null
        }
    }

    private fun handlePhotoPickerResult(data: Intent?) {
        // user chose a featured image
        if (data?.hasExtra(MediaPickerConstants.EXTRA_MEDIA_ID) == true) {
            val mediaId = data.getLongExtra(MediaPickerConstants.EXTRA_MEDIA_ID, 0L)
            setFeaturedImageId(mediaId, true, isGutenbergEditor = false)
        } else if (data?.hasExtra(MediaPickerConstants.EXTRA_MEDIA_QUEUED_URIS) == true) {
            val uris: List<Uri> = convertStringArrayIntoUrisList(
                data.getStringArrayExtra(MediaPickerConstants.EXTRA_MEDIA_QUEUED_URIS)
            )
            val postId: Int = getImmutablePost().id
            featuredImageHelper.trackFeaturedImageEvent(
                FeaturedImageHelper.TrackableEvent.IMAGE_PICKED_POST_SETTINGS,
                postId
            )
            for (mediaUri: Uri in uris) {
                val mimeType: String? = contentResolver.getType(mediaUri)
                val queueImageResult: EnqueueFeaturedImageResult = featuredImageHelper
                    .queueFeaturedImageForUpload(
                        postId, site, mediaUri,
                        mimeType
                    )
                if (queueImageResult === EnqueueFeaturedImageResult.FILE_NOT_FOUND) {
                    Toast.makeText(
                        this,
                        R.string.file_not_found, Toast.LENGTH_SHORT
                    ).show()
                } else if (queueImageResult === EnqueueFeaturedImageResult.INVALID_POST_ID) {
                    Toast.makeText(
                        this,
                        R.string.error_generic, Toast.LENGTH_SHORT
                    ).show()
                }
            }
            editPostSettingsFragment?.refreshViews()
        } else if (data?.hasExtra(MediaPickerConstants.EXTRA_MEDIA_URIS) == true) {
            val uris: List<Uri> = convertStringArrayIntoUrisList(
                data.getStringArrayExtra(MediaPickerConstants.EXTRA_MEDIA_URIS)
            )
            editorMedia.addNewMediaItemsToEditorAsync(uris, false)
        } else if (data?.hasExtra(MediaPickerConstants.EXTRA_SAVED_MEDIA_MODEL_LOCAL_IDS) == true) {
            val localIds: IntArray? = data.getIntArrayExtra(MediaPickerConstants.EXTRA_SAVED_MEDIA_MODEL_LOCAL_IDS)
            val postId: Int = getImmutablePost().id

            localIds?.forEach { localId ->
                val media: MediaModel? = mediaStore.getMediaWithLocalId(localId)
                media?.let {
                    featuredImageHelper.queueFeaturedImageForUpload(postId, it)
                }
            }
            editPostSettingsFragment?.refreshViews()
        }
    }

    private fun handleMediaPickerResult(data: Intent?) {
        // TODO move this to EditorMedia
        var ids = data?.getLongArrayExtra(MediaBrowserActivity.RESULT_IDS)?.toList()?.map { it }?.toMutableList()
        if (ids.isNullOrEmpty()) {
            val mediaId = data?.getLongExtra(MediaPickerConstants.EXTRA_MEDIA_ID, 0)
            if (mediaId != null && mediaId != 0L) {
                ids = mutableListOf(mediaId)
            } else {
                return
            }
        }

        var allAreImages = true
        ids.forEach { id ->
            val media = mediaStore.getSiteMediaWithId(siteModel, id)
            if (media != null && !MediaUtils.isValidImage(media.url)) {
                allAreImages = false
                return@forEach
            }
        }

        // if the user selected multiple items and they're all images, show the insert media
        // dialog so the user can choose whether to insert them individually or as a gallery
        if ((ids.size > 1) && allAreImages && !showGutenbergEditor) {
            showInsertMediaDialog(ArrayList(ids))
        } else {
            // if allowMultipleSelection and gutenberg editor, pass all ids to addExistingMediaToEditor at once
            editorMedia.addExistingMediaToEditorAsync(AddExistingMediaSource.WP_MEDIA_LIBRARY, ids)
            if (showGutenbergEditor && editorPhotoPicker?.allowMultipleSelection == true) {
                editorPhotoPicker?.allowMultipleSelection = false
            }
        }
    }

    /*
     * called after user selects multiple photos from WP media library
     */
    private fun showInsertMediaDialog(mediaIds: ArrayList<Long>) {
        val callback = InsertMediaCallback {dialog: InsertMediaDialog ->
                when (dialog.insertType) {
                    InsertType.GALLERY -> {
                        val gallery = MediaGallery().apply {
                            type = dialog.galleryType.toString()
                            numColumns = dialog.numColumns
                            ids = mediaIds
                        }
                        editorFragment?.appendGallery(gallery)
                    }
                    InsertType.INDIVIDUALLY -> {
                        editorMedia.addExistingMediaToEditorAsync(AddExistingMediaSource.WP_MEDIA_LIBRARY, mediaIds)
                    }
                    null -> {
                        // Handle the case where dialog.insertType is null if needed
                    }
                }
            }

        val dialog = InsertMediaDialog.newInstance(callback, siteModel)
        val ft = supportFragmentManager.beginTransaction()
        ft.add(dialog, "insert_media")
        ft.commitAllowingStateLoss()
    }

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onAccountChanged(event: OnAccountChanged) {
        if (event.causeOfChange == AccountAction.SEND_VERIFICATION_EMAIL) {
            if (!event.isError) {
                ToastUtils.showToast(this, getString(R.string.toast_verification_email_sent))
            } else {
                ToastUtils.showToast(this, getString(R.string.toast_verification_email_send_error))
            }
        }
    }

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onMediaChanged(event: OnMediaChanged) {
        if (event.isError) {
            val errorMessage: String
            when (event.error.type) {
                MediaErrorType.FS_READ_PERMISSION_DENIED -> errorMessage =
                    getString(R.string.error_media_insufficient_fs_permissions)

                MediaErrorType.NOT_FOUND -> errorMessage = getString(R.string.error_media_not_found)
                MediaErrorType.AUTHORIZATION_REQUIRED -> errorMessage = getString(R.string.error_media_unauthorized)
                MediaErrorType.PARSE_ERROR -> errorMessage = getString(R.string.error_media_parse_error)
                MediaErrorType.MALFORMED_MEDIA_ARG, MediaErrorType.NULL_MEDIA_ARG, MediaErrorType.GENERIC_ERROR ->
                    errorMessage = getString(R.string.error_refresh_media)
                        else -> errorMessage = getString(R.string.error_refresh_media)
            }
            if (!TextUtils.isEmpty(errorMessage)) {
                ToastUtils.showToast(this@EditPostActivity, errorMessage, ToastUtils.Duration.SHORT)
            }
        } else {
            if (pendingVideoPressInfoRequests?.isNotEmpty() == true) {
                // If there are pending requests for video URLs from VideoPress ids, query the DB for
                // them again and notify the editor
                pendingVideoPressInfoRequests?.forEach { videoId ->
                    val videoUrl = mediaStore.getUrlForSiteVideoWithVideoPressGuid(
                        siteModel,
                        videoId
                    )
                    val posterUrl = WPMediaUtils.getVideoPressVideoPosterFromURL(videoUrl)
                    editorFragment?.setUrlForVideoPressId(videoId, videoUrl, posterUrl)
                }
                pendingVideoPressInfoRequests?.clear()
            }
        }
    }

    override fun onEditPostPublishedSettingsClick() {
        viewPager?.currentItem = PAGE_PUBLISH_SETTINGS
    }

    /**
     * EditorFragmentListener methods
     */
    override fun clearFeaturedImage() {
        if (editorFragment is GutenbergEditorFragment) {
            (editorFragment as GutenbergEditorFragment).sendToJSFeaturedImageId(0)
        }
    }

    override fun updateFeaturedImage(mediaId: Long, imagePicked: Boolean) {
        setFeaturedImageId(mediaId, imagePicked, true)
    }

    override fun onAddMediaClicked() {
        if (editorPhotoPicker?.isPhotoPickerShowing() == true) {
            editorPhotoPicker?.hidePhotoPicker()
        } else if (WPMediaUtils.currentUserCanUploadMedia(siteModel)) {
            editorPhotoPicker?.showPhotoPicker(siteModel)
        } else {
            // show the WP media library instead of the photo picker if the user doesn't have upload permission
            mediaPickerLauncher.viewWPMediaLibraryPickerForResult(this, siteModel, MediaBrowserType.EDITOR_PICKER)
        }
    }

    override fun onAddMediaImageClicked(allowMultipleSelection: Boolean) {
        editorPhotoPicker?.allowMultipleSelection = allowMultipleSelection
        mediaPickerLauncher.viewWPMediaLibraryPickerForResult(
            this,
            siteModel,
            MediaBrowserType.GUTENBERG_IMAGE_PICKER
        )
    }

    override fun onAddMediaVideoClicked(allowMultipleSelection: Boolean) {
        editorPhotoPicker?.allowMultipleSelection = allowMultipleSelection
        mediaPickerLauncher.viewWPMediaLibraryPickerForResult(
            this,
            siteModel,
            MediaBrowserType.GUTENBERG_VIDEO_PICKER
        )
    }

    override fun onAddLibraryMediaClicked(allowMultipleSelection: Boolean) {
        editorPhotoPicker?.allowMultipleSelection = allowMultipleSelection
        if (allowMultipleSelection) {
            mediaPickerLauncher.viewWPMediaLibraryPickerForResult(this, siteModel, MediaBrowserType.EDITOR_PICKER)
        } else {
            mediaPickerLauncher
                .viewWPMediaLibraryPickerForResult(this, siteModel, MediaBrowserType.GUTENBERG_SINGLE_MEDIA_PICKER)
        }
    }

    override fun onAddLibraryFileClicked(allowMultipleSelection: Boolean) {
        editorPhotoPicker?.allowMultipleSelection = allowMultipleSelection
        mediaPickerLauncher
            .viewWPMediaLibraryPickerForResult(this, siteModel, MediaBrowserType.GUTENBERG_SINGLE_FILE_PICKER)
    }

    override fun onAddLibraryAudioFileClicked(allowMultipleSelection: Boolean) {
        mediaPickerLauncher
            .viewWPMediaLibraryPickerForResult(this, siteModel, MediaBrowserType.GUTENBERG_SINGLE_AUDIO_FILE_PICKER)
    }

    override fun onAddPhotoClicked(allowMultipleSelection: Boolean) {
        if (allowMultipleSelection) {
            mediaPickerLauncher.showPhotoPickerForResult(
                this, MediaBrowserType.GUTENBERG_IMAGE_PICKER, siteModel,
                editPostRepository.id
            )
        } else {
            mediaPickerLauncher.showPhotoPickerForResult(
                this, MediaBrowserType.GUTENBERG_SINGLE_IMAGE_PICKER, siteModel,
                editPostRepository.id
            )
        }
    }

    override fun onCapturePhotoClicked() {
        onPhotoPickerIconClicked(PhotoPickerIcon.ANDROID_CAPTURE_PHOTO, false)
    }

    override fun onAddVideoClicked(allowMultipleSelection: Boolean) {
        if (allowMultipleSelection) {
            mediaPickerLauncher.showPhotoPickerForResult(
                this, MediaBrowserType.GUTENBERG_VIDEO_PICKER, siteModel,
                editPostRepository.id
            )
        } else {
            mediaPickerLauncher.showPhotoPickerForResult(
                this, MediaBrowserType.GUTENBERG_SINGLE_VIDEO_PICKER, siteModel,
                editPostRepository.id
            )
        }
    }

    override fun onAddDeviceMediaClicked(allowMultipleSelection: Boolean) {
        if (allowMultipleSelection) {
            mediaPickerLauncher.showPhotoPickerForResult(
                this, MediaBrowserType.GUTENBERG_MEDIA_PICKER, siteModel,
                editPostRepository.id
            )
        } else {
            mediaPickerLauncher.showPhotoPickerForResult(
                this, MediaBrowserType.GUTENBERG_SINGLE_MEDIA_PICKER, siteModel,
                editPostRepository.id
            )
        }
    }

    override fun onAddStockMediaClicked(allowMultipleSelection: Boolean) {
        onPhotoPickerIconClicked(PhotoPickerIcon.STOCK_MEDIA, allowMultipleSelection)
    }

    override fun onAddGifClicked(allowMultipleSelection: Boolean) {
        onPhotoPickerIconClicked(PhotoPickerIcon.GIF, allowMultipleSelection)
    }

    override fun onAddFileClicked(allowMultipleSelection: Boolean) {
        mediaPickerLauncher.showFilePicker(this, allowMultipleSelection, site)
    }

    override fun onAddAudioFileClicked(allowMultipleSelection: Boolean) {
        mediaPickerLauncher.showAudioFilePicker(this, allowMultipleSelection, site)
    }

    override fun onPerformFetch(
        path: String,
        enableCaching: Boolean,
        onResult: Consumer<String>,
        onError: Consumer<Bundle>
    ) {
       reactNativeRequestHandler.performGetRequest(path, siteModel, enableCaching, onResult, onError)
    }

    override fun onPerformPost(
        path: String,
        body: Map<String, Any>,
        onResult: Consumer<String>,
        onError: Consumer<Bundle>
    ) {
       reactNativeRequestHandler.performPostRequest(path, body, siteModel, onResult, onError)
    }

    override fun onCaptureVideoClicked() {
        onPhotoPickerIconClicked(PhotoPickerIcon.ANDROID_CAPTURE_VIDEO, false)
    }

    override fun onMediaDropped(mediaUris: ArrayList<Uri>) {
        editorMedia.droppedMediaUris = mediaUris
        val media: ArrayList<Uri> = ArrayList(mediaUris)
        editorMedia.addNewMediaItemsToEditorAsync(media, false)
        editorMedia.droppedMediaUris.clear()
    }

    override fun onRequestDragAndDropPermissions(dragEvent: DragEvent) {
        requestDragAndDropPermissions(dragEvent)
    }

    override fun onMediaRetryAll(failedMediaIds: Set<String>) {
        UploadService.cancelFinalNotification(this, editPostRepository.getPost())
        UploadService.cancelFinalNotificationForMedia(this, siteModel)
        val localMediaIds: ArrayList<Int> = ArrayList()
        for (idString: String? in failedMediaIds) {
            idString?.toIntOrNull()?.let {
                localMediaIds.add(it)
            }
        }
        editorMedia.retryFailedMediaAsync(localMediaIds)
    }

    @Suppress("ReturnCount")
    override fun onMediaRetryClicked(mediaId: String): Boolean {
        if (TextUtils.isEmpty(mediaId)) {
            AppLog.e(AppLog.T.MEDIA, "Invalid media id passed to onMediaRetryClicked")
            return false
        }
        val media: MediaModel? = mediaStore.getMediaWithLocalId(StringUtils.stringToInt(mediaId))
        if (media == null) {
            AppLog.e(
                AppLog.T.MEDIA,
                "Can't find media with local id: $mediaId"
            )
            val builder: AlertDialog.Builder = MaterialAlertDialogBuilder(this)
            builder.setTitle(getString(R.string.cannot_retry_deleted_media_item))
            builder.setPositiveButton(R.string.yes) { dialog, _ ->
                runOnUiThread { editorFragment?.removeMedia(mediaId) }
                dialog.dismiss()
            }
            builder.setNegativeButton(getString(R.string.no)
            ) { dialog: DialogInterface, _: Int -> dialog.dismiss() }
            val dialog: AlertDialog = builder.create()
            dialog.show()
            return false
        }
        if (!TextUtils.isEmpty(media.url) && (media.uploadState == MediaUploadState.UPLOADED.toString())) {
            // Note: we should actually do this when the editor fragment starts instead of waiting for user input.
            // Notify the editor fragment upload was successful and it should replace the local url by the remote url.
                editorMediaUploadListener?.onMediaUploadSucceeded(
                    media.id.toString(),
                    FluxCUtils.mediaFileFromMediaModel(media)
                )
        } else {
            UploadService.cancelFinalNotification(this, editPostRepository.getPost())
            UploadService.cancelFinalNotificationForMedia(this, siteModel)
            editorMedia.retryFailedMediaAsync(listOf(media.id))
        }
        AnalyticsUtils.trackWithSiteDetails(Stat.EDITOR_UPLOAD_MEDIA_RETRIED, siteModel)
        return true
    }

    override fun onMediaUploadCancelClicked(localMediaId: String) {
        if (!TextUtils.isEmpty(localMediaId)) {
            editorMedia.cancelMediaUploadAsync(StringUtils.stringToInt(localMediaId), true)
        } else {
            // Passed mediaId is incorrect: cancel all uploads for this post
            ToastUtils.showToast(this, getString(R.string.error_all_media_upload_canceled))
            EventBus.getDefault().post(PostMediaCanceled(editPostRepository.getEditablePost()))
        }
    }

    override fun onMediaDeleted(localMediaId: String) {
        if (!TextUtils.isEmpty(localMediaId)) {
            editorMedia.onMediaDeleted(showAztecEditor, showGutenbergEditor, localMediaId)
        }
    }

    override fun onUndoMediaCheck(undoedContent: String) {
        // here we check which elements tagged UPLOADING are there in undoedContent,
        // and check for the ones that ARE NOT being uploaded or queued in the UploadService.
        // These are the CANCELED ONES, so mark them FAILED now to retry.
        val currentlyUploadingMedia: List<MediaModel> = UploadService.getPendingOrInProgressMediaUploadsForPost(
            editPostRepository.getPost()
        )

        val mediaMarkedUploading: List<String?> =
            AztecEditorFragment.getMediaMarkedUploadingInPostContent(this@EditPostActivity, undoedContent)

        // go through the list of items marked UPLOADING within the Post content, and look in the UploadService
        // to see whether they're really being uploaded or not. If an item is not really being uploaded,
        // mark that item failed
        mediaMarkedUploading.forEach { mediaId ->
            if (mediaId != null &&
                currentlyUploadingMedia.none { media -> StringUtils.stringToInt(mediaId) == media.id }
            ) {
                if (editorFragment is AztecEditorFragment) {
                    editorMedia.updateDeletedMediaItemIds(mediaId)
                    (editorFragment as AztecEditorFragment).setMediaToFailed(mediaId)
                }
            }
        }
    }

    override fun onVideoPressInfoRequested(videoId: String) {
        val videoUrl = mediaStore.getUrlForSiteVideoWithVideoPressGuid((siteModel), videoId)
        if (videoUrl == null) {
            AppLog.w(
                AppLog.T.EDITOR, ("The editor wants more info about the following VideoPress code: " + videoId
                        + " but it's not available in the current site " + siteModel.url
                        + " Maybe it's from another site?")
            )
            return
        }
        if (videoUrl.isEmpty()) {
            if (PermissionUtils.checkAndRequestCameraAndStoragePermissions(
                    this, WPPermissionUtils.EDITOR_MEDIA_PERMISSION_REQUEST_CODE
                )
            ) {
                runOnUiThread {
                    pendingVideoPressInfoRequests?.add(videoId) ?: run {
                        pendingVideoPressInfoRequests = mutableListOf(videoId)
                    }
                    editorMedia.refreshBlogMedia()
                }
            }
        }
        val posterUrl: String = WPMediaUtils.getVideoPressVideoPosterFromURL(videoUrl)
        editorFragment?.setUrlForVideoPressId(videoId, videoUrl, posterUrl)
    }

    override fun onAuthHeaderRequested(url: String): Map<String, String> {
        val authHeaders: MutableMap<String, String> = HashMap()
        val token = accountStore.accessToken
        if ((siteModel.isPrivate && WPUrlUtils.safeToAddWordPressComAuthToken(url)
                    && !TextUtils.isEmpty(token))
        ) {
            authHeaders[AuthenticationUtils.AUTHORIZATION_HEADER_NAME] = "Bearer $token"
        }
        if (siteModel.isPrivateWPComAtomic && privateAtomicCookie.exists() && WPUrlUtils
                .safeToAddPrivateAtCookie(url, privateAtomicCookie.getDomain())
        ) {
            authHeaders[AuthenticationUtils.COOKIE_HEADER_NAME] = privateAtomicCookie.getCookieContent()
        }
        return authHeaders
    }

    override fun onEditorFragmentInitialized() {
        // now that we have the Post object initialized,
        // check whether we have media items to insert from the WRITE POST with media functionality
        if (intent.hasExtra(EditPostActivityConstants.EXTRA_INSERT_MEDIA)) {
            // Bump analytics
            AnalyticsTracker.track(Stat.NOTIFICATION_UPLOAD_MEDIA_SUCCESS_WRITE_POST)
            val serializableExtra = intent.getSerializableExtra(EditPostActivityConstants.EXTRA_INSERT_MEDIA)

            val mediaList = if (serializableExtra is List<*> && serializableExtra.all { it is MediaModel } ) {
                @Suppress("UNCHECKED_CAST")
                serializableExtra as List<MediaModel>
            } else {
                null
            }
            // removing this from the intent so it doesn't insert the media items again on each Activity re-creation
            intent.removeExtra(EditPostActivityConstants.EXTRA_INSERT_MEDIA)
            if (!mediaList.isNullOrEmpty()) {
                editorMedia.addExistingMediaToEditorAsync(mediaList, AddExistingMediaSource.WP_MEDIA_LIBRARY)
            }
        }
        onEditorFinalTouchesBeforeShowing()
    }

    private fun onEditorFinalTouchesBeforeShowing() {
        refreshEditorContent()

        onEditorFinalTouchesBeforeShowingForGutenbergIfNeeded()
        onEditorFinalTouchesBeforeShowingForAztecIfNeeded()
    }
    private fun onEditorFinalTouchesBeforeShowingForGutenbergIfNeeded() {
        // probably here is best for Gutenberg to start interacting with
        if (!(showGutenbergEditor && editorFragment is GutenbergEditorFragment))
            return

        refreshEditorTheme()

        editPostRepository.getPost()?.let {  post ->
            val failedMedia = mediaStore.getMediaForPostWithState(post, MediaUploadState.FAILED)
            if (failedMedia.isEmpty()) return@let
            val mediaIds: HashSet<Int> = HashSet()
            failedMedia.forEach { media ->
                // featured image isn't in the editor but in the Post Settings fragment, so we want to skip it
                if (!media.markedLocallyAsFeatured) {
                    mediaIds.add(media.id)
                }
            }
            (editorFragment as GutenbergEditorFragment).resetUploadingMediaToFailed(mediaIds)
        }
    }
    private fun onEditorFinalTouchesBeforeShowingForAztecIfNeeded() {
        if (showAztecEditor && editorFragment is AztecEditorFragment) {
            val entryPoint =
                intent.getSerializableExtra(EditPostActivityConstants.EXTRA_ENTRY_POINT) as PostUtils.EntryPoint?
            postEditorAnalyticsSession?.start(null, entryPoint)
        }
    }

    override fun onEditorFragmentContentReady(
        unsupportedBlocksList: ArrayList<Any>,
        replaceBlockActionWaiting: Boolean
    ) {
        val entryPoint: PostUtils.EntryPoint? =
            intent.getSerializableExtra(EditPostActivityConstants.EXTRA_ENTRY_POINT) as PostUtils.EntryPoint?

        // Note that this method is also used to track startup performance
        // It assumes this is being called when the editor has finished loading
        // If you need to refactor this, please ensure that the startup_time_ms property
        // is still reflecting the actual startup time of the editor
        postEditorAnalyticsSession?.start(unsupportedBlocksList, entryPoint)
        presentNewPageNoticeIfNeeded()

        // Start VM, load prompt and populate Editor with content after edit IS ready.
        val promptId: Int = intent.getIntExtra(EditPostActivityConstants.EXTRA_PROMPT_ID, -1)
        editorBloggingPromptsViewModel.start(siteModel, promptId)

        updateVoiceContentIfNeeded()
    }

    private fun updateVoiceContentIfNeeded() {
        if (isNewGutenbergEditor) {
            return
        }
        // Check if voice content exists and this is a new post for a Gutenberg editor fragment
        val content = intent.getStringExtra(EditPostActivityConstants.EXTRA_VOICE_CONTENT)
        if (isNewPost && content != null && !isVoiceContentSet) {
            val gutenbergFragment = editorFragment as? GutenbergEditorFragment
            gutenbergFragment?.let {
                isVoiceContentSet = true
                it.updateContent(content)
            }
        }
    }

    private fun logTemplateSelection() {
        val template = intent.getStringExtra(EditPostActivityConstants.EXTRA_PAGE_TEMPLATE) ?: return
        postEditorAnalyticsSession?.applyTemplate(template)
    }

    override fun showUserSuggestions(onResult: Consumer<String?>?) {
        showSuggestions(SuggestionType.Users, onResult)
    }

    override fun showXpostSuggestions(onResult: Consumer<String?>?) {
        showSuggestions(SuggestionType.XPosts, onResult)
    }

    private fun showSuggestions(type: SuggestionType, onResult: Consumer<String?>?) {
        onGetSuggestionResult = onResult
        ActivityLauncher.viewSuggestionsForResult(this, siteModel, type)
    }

    override fun onGutenbergEditorSetFocalPointPickerTooltipShown(tooltipShown: Boolean) {
        AppPrefs.setGutenbergFocalPointPickerTooltipShown(tooltipShown)
    }

    override fun onGutenbergEditorRequestFocalPointPickerTooltipShown(): Boolean {
        return AppPrefs.getGutenbergFocalPointPickerTooltipShown()
    }

    override fun onHtmlModeToggledInToolbar() {
        toggleHtmlModeOnMenu()
    }

    @Throws(IllegalArgumentException::class)
    override fun onTrackableEvent(event: EditorFragmentAbstract.TrackableEvent) {
        editorFragment?.let {
            editorTracker.trackEditorEvent(event, it.editorName)
        }

        when (event) {
            EditorFragmentAbstract.TrackableEvent.ELLIPSIS_COLLAPSE_BUTTON_TAPPED -> {
                AppPrefs.setAztecEditorToolbarExpanded(false)
            }
            EditorFragmentAbstract.TrackableEvent.ELLIPSIS_EXPAND_BUTTON_TAPPED -> {
                AppPrefs.setAztecEditorToolbarExpanded(true)
            }
            EditorFragmentAbstract.TrackableEvent.HTML_BUTTON_TAPPED,
            EditorFragmentAbstract.TrackableEvent.LINK_ADDED_BUTTON_TAPPED -> {
                editorPhotoPicker?.hidePhotoPicker()
            }
            else -> { /* no-op */ }
        }
    }

    override fun onTrackableEvent(event: EditorFragmentAbstract.TrackableEvent, properties: Map<String, String>) {
        editorFragment?.let {
            editorTracker.trackEditorEvent(event, it.editorName, properties)
        }
    }

    override fun showPreview(): Boolean {
        val post = editPostRepository.getPost() ?: return false

        val opResult: PreviewLogicOperationResult = remotePreviewLogicHelper.runPostPreviewLogic(
            this,
            siteModel,
            post,
            editPostActivityStrategyFunctions
        )

        return when (opResult) {
            PreviewLogicOperationResult.MEDIA_UPLOAD_IN_PROGRESS,
            PreviewLogicOperationResult.CANNOT_SAVE_EMPTY_DRAFT,
            PreviewLogicOperationResult.CANNOT_REMOTE_AUTO_SAVE_EMPTY_POST -> {
                false
            }
            PreviewLogicOperationResult.OPENING_PREVIEW -> {
                updatePostLoadingAndDialogState(PostLoadingState.PREVIEWING, post)
                true
            }
            else -> true
        }
    }

    override fun onRequestBlockTypeImpressions(): Map<String, Double> {
        return AppPrefs.getGutenbergBlockTypeImpressions()
    }

    override fun onSetBlockTypeImpressions(impressions: Map<String, Double>) {
        AppPrefs.setGutenbergBlockTypeImpressions(impressions)
    }

    override fun onContactCustomerSupport() {
        onContactCustomerSupport(
            (zendeskHelper),
            this,
            site,
            contactSupportFeatureConfig.isEnabled()
        )
    }

    override fun onGotoCustomerSupportOptions() {
        onGotoCustomerSupportOptions(this, site)
    }

    override fun onSendEventToHost(eventName: String, properties: Map<String, Any>) {
        AnalyticsUtils.trackBlockEditorEvent(eventName, siteModel, properties)
    }

    override fun onToggleUndo(isDisabled: Boolean) {
        if (menuHasUndo == !isDisabled) return
        menuHasUndo = !isDisabled
        Handler(Looper.getMainLooper()).post { invalidateOptionsMenu() }
    }

    override fun onToggleRedo(isDisabled: Boolean) {
        if (menuHasRedo == !isDisabled) return
        menuHasRedo = !isDisabled
        Handler(Looper.getMainLooper()).post { invalidateOptionsMenu() }
    }

    override fun onBackHandlerButton() {
        handleBackPressed()
    }

    // FluxC events
    @Suppress("unused", "CyclomaticComplexMethod")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onMediaUploaded(event: OnMediaUploaded) {
        if (isFinishing) {
            return
        }

        if (event.isError && !NetworkUtils.isNetworkAvailable(this)) {
            editorMediaUploadListener?.let { listener ->
                event.media?.let { media ->
                    editorMedia.onMediaUploadPaused(listener, media, event.error)
                }
            }
            return
        }

        event.media?.let {
            if (event.isError) {
                handleOnMediaUploadedError(event)
            } else if (event.completed) {
                handleOnMediaUploadedCompleted(event)
            } else {
                onUploadProgress(event.media, event.progress)
            }
        } ?: run {
            // event for unknown media, ignoring
            AppLog.w(AppLog.T.MEDIA, "Media event carries null media object, not recognized")
        }
    }
    private fun handleOnMediaUploadedError(event: OnMediaUploaded) {
        val view: View? = editorFragment?.view
        if (view != null) {
            uploadUtilsWrapper.showSnackbarError(
                view,
                String.format(
                    getString(R.string.error_media_upload_failed_for_reason),
                    UploadUtils.getErrorMessageFromMedia(this, event.media as MediaModel)
                )
            )
        }
        editorMediaUploadListener?.let { listener ->
            event.media?.let { media ->
                editorMedia.onMediaUploadError(listener, media, event.error)
            }
        }
    }

    private fun handleOnMediaUploadedCompleted(event: OnMediaUploaded){
        // if the remote url on completed is null, we consider this upload wasn't successful
        val media = event.media ?: return

        editorMediaUploadListener?.let { listener ->
            if (TextUtils.isEmpty(media.url)) {
                val error = MediaError(MediaErrorType.GENERIC_ERROR)
                if (!NetworkUtils.isNetworkAvailable(this)) {
                    editorMedia.onMediaUploadPaused(listener, media, error)
                } else {
                    editorMedia.onMediaUploadError(listener, media, error)
                }
            } else {
                onUploadSuccess(media)
            }
        }
    }

    // FluxC events
    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onMediaListFetched(event: OnMediaListFetched?) {
        if (event != null) {
            networkErrorOnLastMediaFetchAttempt = event.isError
        }
    }

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onPostChanged(event: OnPostChanged) {
        if (event.causeOfChange is CauseOfOnPostChanged.UpdatePost) {
            if (!event.isError) {
                // here update the menu if it's not a draft anymore
                invalidateOptionsMenu()
            } else {
                updatePostLoadingAndDialogState(PostLoadingState.NONE)
                AppLog.e(AppLog.T.POSTS, "UPDATE_POST failed: " + event.error.type + " - " + event.error.message)
            }
        } else if (event.causeOfChange is CauseOfOnPostChanged.RemoteAutoSavePost) {
            if (!editPostRepository.hasPost() || ((editPostRepository.id
                        != (event.causeOfChange as CauseOfOnPostChanged.RemoteAutoSavePost).localPostId))
            ) {
                AppLog.e(
                    AppLog.T.POSTS, (
                          "Ignoring REMOTE_AUTO_SAVE_POST in EditPostActivity as mPost is null or id of the opened post"
                                    + " doesn't match the event.")
                )
                return
            }
            if (event.isError) {
                AppLog.e(
                    AppLog.T.POSTS,
                    "REMOTE_AUTO_SAVE_POST failed: " + event.error.type + " - " + event.error.message
                )
            }
            editPostRepository.loadPostByLocalPostId(editPostRepository.id)
            if (isRemotePreviewingFromEditor) {
                handleRemotePreviewUploadResult(
                    event.isError,
                    RemotePreviewType.REMOTE_PREVIEW_WITH_REMOTE_AUTO_SAVE
                )
            }
        }
    }

    private val isRemotePreviewingFromEditor: Boolean
        get() {
            return (postLoadingState === PostLoadingState.UPLOADING_FOR_PREVIEW
                    ) || (postLoadingState === PostLoadingState.REMOTE_AUTO_SAVING_FOR_PREVIEW
                    ) || (postLoadingState === PostLoadingState.PREVIEWING
                    ) || (postLoadingState === PostLoadingState.REMOTE_AUTO_SAVE_PREVIEW_ERROR)
        }
    private val isUploadingPostForPreview: Boolean
        get() {
            return (postLoadingState === PostLoadingState.UPLOADING_FOR_PREVIEW
                    || postLoadingState === PostLoadingState.REMOTE_AUTO_SAVING_FOR_PREVIEW)
        }

    private fun updateOnSuccessfulUpload() {
        isNewPost = false
        invalidateOptionsMenu()
    }

    private val isRemoteAutoSaveError: Boolean
        get() {
            return postLoadingState === PostLoadingState.REMOTE_AUTO_SAVE_PREVIEW_ERROR
        }

    private fun handleRemotePreviewUploadResult(isError: Boolean, param: RemotePreviewType) {
        // We are in the process of remote previewing a post from the editor
        if (!isError && isUploadingPostForPreview) {
            // We were uploading post for preview and we got no error:
            // update post status and preview it in the internal browser
            updateOnSuccessfulUpload()
            ActivityLauncher.previewPostOrPageForResult(
                this@EditPostActivity,
                siteModel,
                editPostRepository.getPost(),
                param
            )
            updatePostLoadingAndDialogState(PostLoadingState.PREVIEWING, editPostRepository.getPost())
        } else if (isError || isRemoteAutoSaveError) {
            // We got an error from the uploading or from the remote auto save of a post: show snackbar error
            updatePostLoadingAndDialogState(PostLoadingState.NONE)
            uploadUtilsWrapper.showSnackbarError(
                findViewById(R.id.editor_activity),
                getString(R.string.remote_preview_operation_error)
            )
        }
    }

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onPostUploaded(event: OnPostUploaded) {
        val post: PostModel? = event.post

        // Check if editPostRepository is initialized
        val editPostRepositoryInitialized = this::editPostRepository.isInitialized
        val editPostId = if (editPostRepositoryInitialized) editPostRepository.getPost()?.id else null

        if (post != null && post.id == editPostId) {
            if (!isRemotePreviewingFromEditor) {
                // We are not remote previewing a post: show snackbar and update post status if needed
                val snackbarAttachView = findViewById<View>(R.id.editor_activity)
                uploadUtilsWrapper.onPostUploadedSnackbarHandler(
                    this, snackbarAttachView, event.isError,
                    event.isFirstTimePublish, post, if (event.isError) event.error.message else null, site
                )
                if (!event.isError) {
                    editPostRepository.set {
                        updateOnSuccessfulUpload()
                        post
                    }
                }
            } else {
                editPostRepository.set { post }
                handleRemotePreviewUploadResult(event.isError, RemotePreviewType.REMOTE_PREVIEW)
            }
        }
    }

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEventMainThread(event: ProgressEvent) {
        if (!isFinishing) {
            // use upload progress rather than optimizer progress since the former includes upload+optimization
            val progress: Float = UploadService.getUploadProgressForMedia(event.media)
            onUploadProgress(event.media, progress)
        }
    }

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEventMainThread(event: UploadMediaRetryEvent) {
        if ((!isFinishing
                    && (event.mediaModelList != null
                    ) && (editorMediaUploadListener != null))
        ) {
            for (media: MediaModel in event.mediaModelList) {
                val localMediaId = media.id.toString()
                val mediaType: EditorFragmentAbstract.MediaType =
                    if (media.isVideo)
                        EditorFragmentAbstract.MediaType.VIDEO
                    else EditorFragmentAbstract.MediaType.IMAGE
                editorMediaUploadListener?.onMediaUploadRetry(localMediaId, mediaType)
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEventMainThread(event: ConnectionChangeEvent) {
        if (editorFragment !is GutenbergNetworkConnectionListener) return
        (editorFragment as GutenbergEditorFragment).onConnectionStatusChange(event.isConnected)
    }

    private fun refreshEditorTheme() {
        val payload = FetchEditorThemePayload(siteModel, globalStyleSupportFeatureConfig.isEnabled())
        dispatcher.dispatch(EditorThemeActionBuilder.newFetchEditorThemeAction(payload))
    }

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN_ORDERED)
    fun onEditorThemeChanged(event: OnEditorThemeChanged) {
        if (editorFragment !is EditorThemeUpdateListener || (siteModel.id != event.siteId)) return
        val editorTheme: EditorTheme = event.editorTheme ?: return
        val editorThemeSupport: EditorThemeSupport = editorTheme.themeSupport
        (editorFragment as EditorThemeUpdateListener)
            .onEditorThemeUpdated(editorThemeSupport.toBundle(siteModel))
        postEditorAnalyticsSession?.editorSettingsFetched(editorThemeSupport.isBlockBasedTheme, event.endpoint.value)
    }

    // EditPostActivityHook methods
    override fun getEditPostRepository() = editPostRepository
    override fun getSite() = siteModel

    override fun onMenuOpened(featureId: Int, menu: Menu): Boolean {
        // This is a workaround for bag discovered on Chromebooks, where Enter key will not work in the toolbar menu
        // Editor fragments are messing with window focus, which causes keyboard events to get ignored

        // this fixes issue with GB editor
        val editorFragmentView: View? = editorFragment?.view
        editorFragmentView?.requestFocus()

        // this fixes issue with Aztec editor
        if (editorFragment is AztecEditorFragment) {
            (editorFragment as AztecEditorFragment).requestContentAreaFocus()
        }
        return super.onMenuOpened(featureId, menu)
    }

    // EditorMediaListener
    override fun appendMediaFiles(mediaFiles: Map<String, MediaFile>) {
        editorFragment?.appendMediaFiles(mediaFiles)
    }

    override fun getImmutablePost(): PostImmutableModel {
        // Before conversion to Kotlin, this was wrapped by Objects.requireNonNull, which would crash the app if null
        // The double bang serves the same purpose in Kotlin. Eventually we should revisit if the activity should
        // just finish.
        return editPostRepository.getPost()!!
    }

    override fun syncPostObjectWithUiAndSaveIt(listener: OnPostUpdatedFromUIListener?) {
        updateAndSavePostAsync(listener)
    }

    override fun onMediaModelsCreatedFromOptimizedUris(oldUriToMediaFiles: Map<Uri, MediaModel>) {
        // no op - we're not doing any special handling on MediaModels in EditPostActivity
    }

    override fun showVideoDurationLimitWarning(fileName: String) {
        val message: String = getString(R.string.error_media_video_duration_exceeds_limit)
        make(
            findViewById(R.id.editor_activity),
            message,
            Snackbar.LENGTH_LONG
        ).show()
    }

    override fun getExceptionLogger(): Consumer<Exception> {
        return Consumer { e: Exception? ->
            AppLog.e(
                AppLog.T.EDITOR,
                e
            )
        }
    }

    override fun getBreadcrumbLogger(): Consumer<String> {
        return Consumer { s: String? ->
            AppLog.e(
                AppLog.T.EDITOR,
                s
            )
        }
    }

    private fun updateAddingMediaToEditorProgressDialogState(uiState: ProgressDialogUiState) {
        addingMediaToEditorProgressDialog = progressDialogHelper
            .updateProgressDialogState(this, addingMediaToEditorProgressDialog, uiState, (uiHelpers))
    }

    override fun getErrorMessageFromMedia(mediaId: Int): String {
        val media: MediaModel? = mediaStore.getMediaWithLocalId(mediaId)
        if (media != null) {
            return UploadUtils.getErrorMessageFromMedia(this, media)
        }
        return ""
    }

    override fun showJetpackSettings() {
        ActivityLauncher.viewJetpackSecuritySettingsForResult(this, siteModel)
    }

    override val savingInProgressDialogVisibility: LiveData<DialogVisibility>
        get() {
            return storePostViewModel.savingInProgressDialogVisibility
        }
    private val dB: SavedInstanceDatabase?
        get() {
            return getDatabase(getContext())
        }

    override fun onLogJsException(exception: JsException, onExceptionSend: JsExceptionCallback) {
        crashLogging.sendJavaScriptReport(exception, onExceptionSend)
    }

    companion object {
        private const val PAGE_CONTENT: Int = 0
        private const val PAGE_SETTINGS: Int = 1
        private const val PAGE_PUBLISH_SETTINGS: Int = 2
        private const val PAGE_HISTORY: Int = 3
        private const val MIN_UPDATING_POST_DISPLAY_TIME: Long = 2000L // Minimum display time in milliseconds
        private const val OFFSCREEN_PAGE_LIMIT = 4
        private const val PREPUBLISHING_NUDGE_BOTTOM_SHEET_DELAY = 100L
        private const val SNACKBAR_DURATION = 4000

        @JvmStatic fun checkToRestart(data: Intent): Boolean {
            val extraRestartEditor = data.getStringExtra(EditPostActivityConstants.EXTRA_RESTART_EDITOR)
            return extraRestartEditor != null &&
                    RestartEditorOptions.valueOf(extraRestartEditor) != RestartEditorOptions.NO_RESTART
        }

        // Moved from EditPostContentFragment
        const val NEW_MEDIA_POST: String = "NEW_MEDIA_POST"
        const val NEW_MEDIA_POST_EXTRA_IDS: String = "NEW_MEDIA_POST_EXTRA_IDS"

        const val GROUP_ONE = 1
        const val GROUP_TWO = 2
        const val GROUP_THREE = 3
    }
}
