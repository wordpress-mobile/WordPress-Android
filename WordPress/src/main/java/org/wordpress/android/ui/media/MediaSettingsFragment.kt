package org.wordpress.android.ui.media

import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
import android.app.Activity
import android.app.DownloadManager
import android.content.ActivityNotFoundException
import android.content.BroadcastReceiver
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.text.format.DateUtils
import android.view.MenuItem
import android.view.View
import android.widget.ArrayAdapter
import android.widget.ImageView.ScaleType
import android.widget.LinearLayout
import android.widget.SeekBar
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat.AccessibilityActionCompat
import androidx.core.view.doOnAttach
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.databinding.MediaSettingsFragmentBinding
import org.wordpress.android.databinding.MediaSettingsInfoRowBinding
import org.wordpress.android.databinding.MediaSettingsRowBinding
import org.wordpress.android.editor.EditorImageMetaData
import org.wordpress.android.editor.EditorImageMetaData.ARG_EDITOR_IMAGE_METADATA
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.action.MediaAction
import org.wordpress.android.fluxc.generated.MediaActionBuilder
import org.wordpress.android.fluxc.model.MediaModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.UserAgent
import org.wordpress.android.fluxc.store.MediaStore
import org.wordpress.android.fluxc.store.MediaStore.MediaPayload
import org.wordpress.android.fluxc.store.MediaStore.OnMediaChanged
import org.wordpress.android.ui.media.MediaFieldEditBottomSheet.Field
import org.wordpress.android.ui.utils.AuthenticationUtils
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T
import org.wordpress.android.util.ColorUtils
import org.wordpress.android.util.DateTimeUtils
import org.wordpress.android.util.DisplayUtils
import org.wordpress.android.util.EditTextUtils
import org.wordpress.android.util.FormatUtils
import org.wordpress.android.util.ImageUtils
import org.wordpress.android.util.MediaUtils
import org.wordpress.android.util.NetworkUtils
import org.wordpress.android.util.PhotonUtils
import org.wordpress.android.util.SiteUtils
import org.wordpress.android.util.ToastUtils
import org.wordpress.android.util.WPMediaUtils
import org.wordpress.android.util.WPPermissionUtils
import org.wordpress.android.util.extensions.getParcelableCompat
import org.wordpress.android.util.image.ImageManager
import org.wordpress.android.util.image.ImageType
import java.text.SimpleDateFormat
import java.util.EnumMap
import java.util.Locale
import javax.inject.Inject
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Details of one media item, either from the media library or an image in the editor. [MediaSettingsActivity]
 * pages through these. Edits are saved when the page pauses, so swiping to another item keeps them.
 */
@AndroidEntryPoint
@Suppress("LargeClass", "TooManyFunctions")
class MediaSettingsFragment : Fragment(R.layout.media_settings_fragment) {
    @Inject
    lateinit var userAgent: UserAgent

    @Inject
    lateinit var mediaStore: MediaStore

    @Inject
    lateinit var dispatcher: Dispatcher

    @Inject
    lateinit var imageManager: ImageManager

    @Inject
    lateinit var authenticationUtils: AuthenticationUtils

    private enum class MediaType(@StringRes val titleRes: Int) {
        IMAGE(R.string.media_title_image_details),
        VIDEO(R.string.media_title_video_details),
        AUDIO(R.string.media_title_audio_details),
        DOCUMENT(R.string.media_title_document_details)
    }

    private var binding: MediaSettingsFragmentBinding? = null
    private val detailRows = EnumMap<Field, MediaSettingsRowBinding>(Field::class.java)

    // in-memory values of the editable fields, read by saveChanges()
    private val fieldValues = EnumMap<Field, String>(Field::class.java)

    private lateinit var site: SiteModel

    // the last values loaded or pushed, which saveMediaChanges() compares the fields against
    private lateinit var media: MediaModel
    private val hasMedia get() = ::media.isInitialized
    private var editorImageMetaData: EditorImageMetaData? = null
    private val isMediaFromEditor get() = editorImageMetaData != null

    private var mediaType = MediaType.DOCUMENT
    private var imageSize = MediaSettingsImageSize.FULL
    private val alignmentKeys by lazy { resources.getStringArray(R.array.alignment_key_array) }
    private var downloadId = 0L
    private var rowToRefocus: View? = null
    private var deleteConfirmationDialog: AlertDialog? = null
    private var progressDialog: AlertDialog? = null

    private val downloadReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1) == downloadId) {
                onDownloadComplete(context)
            }
        }
    }

    private val storagePermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            val grantResult = if (isGranted) PackageManager.PERMISSION_GRANTED else PackageManager.PERMISSION_DENIED
            WPPermissionUtils.setPermissionListAsked(
                requireActivity(),
                WPPermissionUtils.MEDIA_PREVIEW_PERMISSION_REQUEST_CODE,
                arrayOf(WRITE_EXTERNAL_STORAGE),
                intArrayOf(grantResult),
                true
            )
            if (isGranted) {
                saveMediaToDevice()
            }
        }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        site = (context as MediaSettingsActivity).site
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // the editor's metadata is updated on save, so prefer the saved copy
        val state = savedInstanceState ?: requireArguments()
        editorImageMetaData = state.getParcelableCompat(ARG_EDITOR_IMAGE_METADATA)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val binding = MediaSettingsFragmentBinding.bind(view).also { binding = it }
        setupToolbar(binding)
        setupDetailRows(binding)

        // a missing item is reported in onResume, so a neighbouring page doesn't close the screen
        if (!loadMedia(binding)) {
            return
        }
        setupMenu(binding)

        savedInstanceState?.let { state ->
            Field.entries.forEach { field ->
                state.getString(STATE_FIELD_PREFIX + field.name)?.let { fieldValues[field] = it }
            }
            bindDetailRows()
            if (state.getBoolean(STATE_DELETE_DIALOG_VISIBLE)) {
                deleteMediaWithConfirmation()
            }
        }

        setupHeader(binding)
        setupInsets(binding)

        // the activity keeps the saved state of every page swiped past, so leave out the views' state, which would
        // grow it with each page; library edits are pushed when a page pauses, so only the scroll position is lost
        if (!isMediaFromEditor) {
            binding.appBarLayout.isSaveFromParentEnabled = false
            binding.scrollView.isSaveFromParentEnabled = false
        }
    }

    override fun onStart() {
        super.onStart()
        ContextCompat.registerReceiver(
            requireContext(),
            downloadReceiver,
            IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE),
            ContextCompat.RECEIVER_EXPORTED
        )
        dispatcher.register(this)
    }

    override fun onResume() {
        super.onResume()
        if (!hasMedia) {
            delayedFinishWithError()
        }
    }

    override fun onPause() {
        super.onPause()
        saveChanges()
    }

    override fun onStop() {
        requireContext().unregisterReceiver(downloadReceiver)
        dispatcher.unregister(this)
        super.onStop()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putParcelable(ARG_EDITOR_IMAGE_METADATA, editorImageMetaData)
        // only the edits that aren't saved yet: the activity keeps the state of every page swiped past
        if (hasMedia) {
            unsavedEdits().forEach { outState.putString(STATE_FIELD_PREFIX + it.name, getFieldValue(it)) }
        }
        outState.putBoolean(STATE_DELETE_DIALOG_VISIBLE, deleteConfirmationDialog?.isShowing == true)
    }

    override fun onDestroyView() {
        deleteConfirmationDialog?.dismiss()
        deleteConfirmationDialog = null
        progressDialog?.dismiss()
        progressDialog = null
        detailRows.clear()
        // stop the image load, which would otherwise run on, and hold this view, until the activity is destroyed.
        // Skipped when it is being destroyed, which clears the load anyway: the view's context is Hilt's wrapper,
        // so ImageManager can't tell the activity is gone and Glide throws.
        if (!requireActivity().isDestroyed) {
            binding?.let { imageManager.cancelRequestAndClearImageView(it.imagePreview) }
        }
        binding = null
        super.onDestroyView()
    }

    /**
     * Called by the activity. When the edit sheet closes after keyboard input, focus would otherwise land on the
     * scroll view, which then draws as a gray overlay across the screen; give it back to the row that opened the sheet
     */
    fun onWindowFocusChanged(hasFocus: Boolean) {
        if (hasFocus) {
            rowToRefocus?.requestFocus()
            rowToRefocus = null
        }
    }

    private fun setupToolbar(binding: MediaSettingsFragmentBinding) = with(binding.toolbar) {
        setNavigationIcon(R.drawable.ic_close_white_24dp)
        setNavigationContentDescription(R.string.close)
        setNavigationOnClickListener { requireActivity().onBackPressedDispatcher.onBackPressed() }
    }

    // only once the media has loaded, since every menu action needs it
    private fun setupMenu(binding: MediaSettingsFragmentBinding) = with(binding.toolbar) {
        inflateMenu(R.menu.media_settings)
        setOnMenuItemClickListener(::onMenuItemClick)
        updateMenu()
    }

    private fun setupDetailRows(binding: MediaSettingsFragmentBinding) {
        detailRows[Field.TITLE] = binding.rowTitle
        detailRows[Field.CAPTION] = binding.rowCaption
        detailRows[Field.DESCRIPTION] = binding.rowDescription
        detailRows[Field.ALT_TEXT] = binding.rowAltText
        detailRows.forEach { (field, row) ->
            row.rowLabel.setText(field.titleRes)
            row.root.setOnClickListener { showFieldEditor(field) }
            ViewCompat.replaceAccessibilityAction(
                row.root, AccessibilityActionCompat.ACTION_CLICK, getString(R.string.edit), null
            )
        }

        // clip row ripples to the rounded cards
        listOf(binding.mediaCustomisationOptions, binding.cardDetails, binding.cardFileInfo).forEach {
            it.clipToOutline = true
        }

        childFragmentManager.setFragmentResultListener(
            MediaFieldEditBottomSheet.REQUEST_KEY, viewLifecycleOwner
        ) { _, result -> onFieldEdited(result) }
    }

    private fun onFieldEdited(result: Bundle) {
        val fieldName = result.getString(MediaFieldEditBottomSheet.KEY_FIELD) ?: return
        fieldValues[Field.valueOf(fieldName)] = result.getString(MediaFieldEditBottomSheet.KEY_VALUE).orEmpty()
        bindDetailRows()
    }

    private fun setupHeader(binding: MediaSettingsFragmentBinding) {
        // only show the title when the toolbar is collapsed
        binding.appBarLayout.addOnOffsetChangedListener { appBarLayout, verticalOffset ->
            binding.collapsingToolbar.title = if (appBarLayout.totalScrollRange + verticalOffset == 0) {
                getString(mediaType.titleRes)
            } else {
                " " // space between double quotes is on purpose
            }
        }

        // make the image 40% of the screen height, with the scrim that appears atop it a few toolbars tall
        val context = requireContext()
        binding.imagePreview.layoutParams.height =
            (DisplayUtils.getWindowPixelHeight(context) * IMAGE_HEIGHT_RATIO).toInt()
        binding.imageGradientScrim.layoutParams.height =
            DisplayUtils.getActionBarHeight(context) * TOOLBAR_HEIGHT_MULTIPLIER

        // tap to show full screen view (not supported for documents)
        if (mediaType != MediaType.DOCUMENT) {
            listOf(binding.imagePreview, binding.imagePlay, binding.buttonFullscreen).forEach {
                it.setOnClickListener { showFullScreen() }
            }
        }
    }

    private fun setupInsets(binding: MediaSettingsFragmentBinding) {
        // keep the last rows clear of the navigation bar when drawing edge-to-edge
        val scrollContentPaddingBottom = binding.scrollContent.paddingBottom
        ViewCompat.setOnApplyWindowInsetsListener(binding.scrollContent) { v, insets ->
            val navBarHeight = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
            v.updatePadding(bottom = scrollContentPaddingBottom + navBarHeight)
            insets
        }
        // ViewPager2 doesn't dispatch insets to pages attached after the last dispatch, so ask for them again
        binding.root.doOnAttach { ViewCompat.requestApplyInsets(it) }
    }

    private fun delayedFinishWithError() {
        ToastUtils.showToast(requireContext(), R.string.error_media_not_found)
        viewLifecycleOwner.lifecycleScope.launch {
            delay(FINISH_DELAY_MS)
            activity?.finish()
        }
    }

    private fun loadMedia(binding: MediaSettingsFragmentBinding): Boolean {
        val model = editorImageMetaData?.let { mediaFromEditorImageMetaData(it) }
            ?: mediaStore.getMediaWithLocalId(requireArguments().getInt(ARG_MEDIA_LOCAL_ID))
            ?: return false
        showMedia(binding, model)
        return true
    }

    private fun reloadMedia() {
        val binding = binding ?: return
        // keep the edits made since the last push, which the stored values would otherwise replace
        val edits = unsavedEdits().associateWith { getFieldValue(it) }
        mediaStore.getMediaWithLocalId(media.id)?.let { showMedia(binding, it) }
        if (edits.isNotEmpty()) {
            fieldValues.putAll(edits)
            bindDetailRows()
        }
    }

    /** The fields whose values differ from [media], which holds the values last loaded or pushed */
    private fun unsavedEdits() = Field.entries.filter { getFieldValue(it) != media.valueOf(it) }

    private fun MediaModel.valueOf(field: Field): String = when (field) {
        Field.TITLE -> title
        Field.CAPTION -> caption
        Field.ALT_TEXT -> alt
        Field.DESCRIPTION -> description
    }.orEmpty()

    private fun showMedia(binding: MediaSettingsFragmentBinding, model: MediaModel) {
        media = model

        // try to get a file without parameters so we can more reliably determine media type
        val uriFilePath = model.url.takeIf { it.isNotEmpty() }?.let { Uri.parse(it).path }.orEmpty()

        // default to DOCUMENT if we can't detect it's an image, video, or audio file
        mediaType = when {
            MediaUtils.isValidImage(uriFilePath) -> MediaType.IMAGE
            model.isVideo -> MediaType.VIDEO
            MediaUtils.isAudio(uriFilePath) -> MediaType.AUDIO
            else -> MediaType.DOCUMENT
        }

        binding.imagePlay.isVisible = mediaType == MediaType.VIDEO || mediaType == MediaType.AUDIO
        binding.buttonFullscreen.isVisible = mediaType == MediaType.IMAGE
        binding.rowAltText.root.isVisible = mediaType == MediaType.IMAGE
        binding.dividerAltText.isVisible = mediaType == MediaType.IMAGE

        showMetaData(binding)

        // audio & documents show a placeholder on top of a gradient, otherwise we show a thumbnail
        if (mediaType == MediaType.AUDIO || mediaType == MediaType.DOCUMENT) {
            val padding = resources.getDimensionPixelSize(R.dimen.margin_extra_extra_large)
            ColorUtils.setImageResourceWithTint(
                binding.imagePreview,
                WPMediaUtils.getPlaceholder(uriFilePath, model.mimeType),
                R.color.neutral_30
            )
            binding.imagePreview.setPadding(padding, padding * 2, padding, padding)
        } else {
            loadImage(binding)
        }
    }

    private fun mediaFromEditorImageMetaData(metaData: EditorImageMetaData): MediaModel {
        val source = metaData.src.orEmpty()
        return MediaModel(
            source,
            org.wordpress.android.fluxc.utils.MediaUtils.getFileName(source),
            org.wordpress.android.fluxc.utils.MediaUtils.getExtension(source),
            metaData.title,
            metaData.caption.orEmpty(),
            metaData.alt.orEmpty(),
            metaData.widthInt,
            metaData.heightInt
        )
    }

    private fun updateMenu() {
        val menu = binding?.toolbar?.menu ?: return
        val canSaveOrShare = !site.isPrivate && !isMediaFromEditor
        menu.findItem(R.id.menu_save).apply {
            isVisible = canSaveOrShare
            isEnabled = downloadId == 0L
        }
        menu.findItem(R.id.menu_share).isVisible = canSaveOrShare
        menu.findItem(R.id.menu_trash).isVisible = !isMediaFromEditor
        menu.findItem(R.id.menu_remove_image).isVisible = isMediaFromEditor
    }

    private fun onMenuItemClick(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_save -> saveMediaToDevice()
            R.id.menu_share -> shareMedia()
            R.id.menu_trash, R.id.menu_remove_image -> deleteMediaWithConfirmation()
            else -> return false
        }
        return true
    }

    private fun showMetaData(binding: MediaSettingsFragmentBinding) {
        fieldValues[Field.TITLE] = media.title.orEmpty()
        fieldValues[Field.ALT_TEXT] = media.alt.orEmpty()
        fieldValues[Field.CAPTION] = media.caption.orEmpty()

        val metaData = editorImageMetaData
        if (metaData != null) {
            binding.editLink.setText(metaData.linkUrl)
            binding.editLinkTargetNewWidnowCheckbox.isChecked = metaData.isLinkTargetBlank
            binding.rowDescription.root.isVisible = false
            binding.dividerDescription.isVisible = false
            setupAlignmentSpinner(binding, metaData)
            setupImageSizeSeekBar(binding, metaData)
        } else {
            fieldValues[Field.DESCRIPTION] = media.description.orEmpty()
            binding.mediaCustomisationOptions.isVisible = false
        }

        bindDetailRows()
        showFileInfo(binding)
        showUrl(binding)
    }

    private fun showUrl(binding: MediaSettingsFragmentBinding) {
        val url = media.url.orEmpty()
        val urlRow = binding.rowUrl
        binding.textUrl.text = url
        urlRow.contentDescription =
            getString(R.string.media_settings_row_value, getString(R.string.media_edit_url_caption), url)
        if (url.isEmpty()) {
            urlRow.setOnClickListener(null)
            urlRow.isClickable = false
            ViewCompat.replaceAccessibilityAction(urlRow, AccessibilityActionCompat.ACTION_CLICK, null, null)
        } else {
            urlRow.setOnClickListener { copyMediaUrlToClipboard() }
            ViewCompat.replaceAccessibilityAction(
                urlRow, AccessibilityActionCompat.ACTION_CLICK, getString(R.string.media_settings_copy_url), null
            )
        }
    }

    private fun showFieldEditor(field: Field) {
        if (childFragmentManager.findFragmentByTag(MediaFieldEditBottomSheet.TAG) != null) {
            return
        }
        rowToRefocus = detailRows[field]?.root
        MediaFieldEditBottomSheet.newInstance(field, getFieldValue(field))
            .showNow(childFragmentManager, MediaFieldEditBottomSheet.TAG)
    }

    private fun getFieldValue(field: Field) = fieldValues[field].orEmpty()

    private fun bindDetailRows() {
        detailRows.forEach { (field, row) ->
            val label = getString(field.titleRes)
            val value = getFieldValue(field)
            row.rowValue.text = value
            row.root.contentDescription = if (value.isEmpty()) {
                getString(R.string.media_settings_row_not_set, label)
            } else {
                getString(R.string.media_settings_row_value, label, value)
            }
        }
    }

    /**
     * Shows the read-only file info rows below the URL, skipping any value that's unknown
     */
    private fun showFileInfo(binding: MediaSettingsFragmentBinding) {
        // dimensions aren't shown for images in a post since the size settings determine them
        val dimensions = if (!isMediaFromEditor && media.width > 0 && media.height > 0) {
            "${media.width} × ${media.height}"
        } else {
            null
        }
        val duration = media.length.takeIf { it > 0 }?.let { DateUtils.formatElapsedTime(it.toLong()) }

        val container = binding.fileInfoRows
        container.removeAllViews()
        listOf(
            R.string.media_settings_file_name to media.fileName,
            R.string.media_settings_file_type to media.fileExtension.orEmpty().uppercase(Locale.ROOT),
            R.string.media_settings_file_size to getFormattedFileSize(),
            R.string.media_settings_dimensions to dimensions,
            R.string.media_edit_duration_caption to duration,
            R.string.media_settings_uploaded to getFormattedUploadDate()
        ).forEach { (labelRes, value) -> addFileInfoRow(container, labelRes, value) }
    }

    private fun addFileInfoRow(container: LinearLayout, @StringRes labelRes: Int, value: String?) {
        if (value.isNullOrEmpty()) {
            return
        }
        layoutInflater.inflate(R.layout.media_settings_divider, container, true)
        val row = MediaSettingsInfoRowBinding.inflate(layoutInflater, container, false)
        val label = getString(labelRes)
        row.rowLabel.text = label
        row.rowValue.text = value
        row.root.contentDescription = getString(R.string.media_settings_row_value, label, value)
        container.addView(row.root)
    }

    private fun getFormattedFileSize(): String? {
        if (media.fileSize <= 0) {
            return media.fileSizeFormatted
        }
        val units = arrayOf(
            getString(R.string.file_size_in_bytes),
            getString(R.string.file_size_in_kilobytes),
            getString(R.string.file_size_in_megabytes),
            getString(R.string.file_size_in_gigabytes),
            getString(R.string.file_size_in_terabytes)
        )
        return FormatUtils.formatFileSize(media.fileSize, units)
    }

    private fun getFormattedUploadDate(): String? = media.uploadDate
        ?.let { DateTimeUtils.dateFromIso8601(it) }
        ?.let { SimpleDateFormat.getDateInstance().format(it) }

    private fun setupImageSizeSeekBar(binding: MediaSettingsFragmentBinding, metaData: EditorImageMetaData) {
        // image size is parsed from html, so we can get non standard values (anything that matches ^size-.*)
        // in this case we will default to the full size
        imageSize = MediaSettingsImageSize.fromCssClass(requireContext(), metaData.size)
        binding.imageSizeHint.setText(imageSize.label)
        with(binding.imageSizeSeekbar) {
            max = MediaSettingsImageSize.entries.size - 1
            progress = imageSize.ordinal
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                    imageSize = MediaSettingsImageSize.entries[progress]
                    binding.imageSizeHint.setText(imageSize.label)
                }

                override fun onStartTrackingTouch(seekBar: SeekBar) = Unit

                override fun onStopTrackingTouch(seekBar: SeekBar) = Unit
            })
        }
    }

    private fun setupAlignmentSpinner(binding: MediaSettingsFragmentBinding, metaData: EditorImageMetaData) {
        val adapter = ArrayAdapter.createFromResource(
            requireContext(), R.array.alignment_array, R.layout.media_settings_alignment_spinner_item
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.alignmentSpinner.adapter = adapter
        binding.alignmentSpinner.setSelection(alignmentKeys.indexOf(metaData.align).coerceAtLeast(0))
    }

    private fun loadImage(binding: MediaSettingsFragmentBinding) {
        val mediaUri = if (mediaType == MediaType.VIDEO) media.thumbnailUrl else media.url
        if (mediaUri.isNullOrEmpty()) {
            if (mediaType == MediaType.VIDEO) {
                downloadVideoThumbnail()
            } else {
                ToastUtils.showToast(requireContext(), R.string.error_media_load)
            }
            return
        }

        val context = requireContext()
        val size = max(DisplayUtils.getWindowPixelWidth(context), DisplayUtils.getWindowPixelHeight(context))
        val imageUrl = if (SiteUtils.isPhotonCapable(site)) {
            PhotonUtils.getPhotonImageUrl(mediaUri, size, 0, site.isPrivateWPComAtomic)
        } else {
            mediaUri
        }

        binding.progress.isVisible = true
        imageManager.loadWithResultListener(
            binding.imagePreview, ImageType.IMAGE, imageUrl, ScaleType.CENTER, null,
            object : ImageManager.RequestListener<Drawable> {
                override fun onResourceReady(resource: Drawable, model: Any?) {
                    this@MediaSettingsFragment.binding?.progress?.isVisible = false
                }

                override fun onLoadFailed(e: Exception?, model: Any?) {
                    e?.let { AppLog.e(T.MEDIA, it) }
                    this@MediaSettingsFragment.binding?.progress?.isVisible = false
                    // the details can still be edited, so keep the page; a neighbouring page loading in the
                    // background stays quiet rather than reporting an item the user isn't looking at
                    if (isResumed) {
                        val messageRes = if (mediaType == MediaType.VIDEO) {
                            R.string.error_media_thumbnail_not_loaded
                        } else {
                            R.string.error_media_load
                        }
                        ToastUtils.showToast(requireContext(), messageRes)
                    }
                }
            }
        )
    }

    /**
     * Downloads and displays the thumbnail for a video that doesn't already have a thumbnail assigned (seen most
     * often with .org and JP sites)
     */
    private fun downloadVideoThumbnail() {
        val url = media.url.takeIf { it.isNotEmpty() } ?: return
        WordPress.getBitmapCache()[url]?.let {
            binding?.imagePreview?.setImageBitmap(it)
            return
        }
        val width = DisplayUtils.getWindowPixelWidth(requireContext())
        viewLifecycleOwner.lifecycleScope.launch {
            val thumb = withContext(Dispatchers.IO) {
                ImageUtils.getVideoFrameFromVideo(url, width, authenticationUtils.getAuthHeaders(url))
            }
            if (thumb != null) {
                WordPress.getBitmapCache().put(url, thumb)
                binding?.imagePreview?.setImageBitmap(thumb)
            }
        }
    }

    private fun showFullScreen() {
        // no need to save first: opening the preview pauses this page, which saves it
        val metaData = editorImageMetaData
        if (metaData != null) {
            MediaPreviewActivity.showPreview(requireContext(), site, metaData.src)
        } else {
            val mediaIdList = (requireActivity() as MediaSettingsActivity).mediaIdList
            MediaPreviewActivity.showPreview(requireContext(), site, media, mediaIdList)
        }
    }

    private fun onDownloadComplete(context: Context) {
        val query = DownloadManager.Query().setFilterById(downloadId)
        context.getSystemService(DownloadManager::class.java).query(query)?.use { cursor ->
            if (cursor.moveToFirst()) {
                // meaning of `reason` depends on the value of COLUMN_STATUS
                val reason = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_REASON))
                val status = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
                if (status == DownloadManager.STATUS_FAILED) {
                    ToastUtils.showToast(context, R.string.error_media_save)
                    // If an HTTP error occurred, this will hold the HTTP status code as defined in RFC 2616.
                    // Otherwise, it will hold one of the ERROR_* constants
                    AppLog.e(T.MEDIA, "MediaSettingsFragment > save > STATUS_FAILED - reason: $reason")
                }
            }
        }
        downloadId = 0
        updateMenu()
    }

    /**
     * Saves the edits to the media library, or returns them to the editor as the activity's result. Called when
     * the page pauses and when the user leaves the screen.
     */
    fun saveChanges() {
        if (!hasMedia || requireActivity().isFinishing) {
            return
        }
        val metaData = editorImageMetaData
        if (metaData == null) {
            saveMediaChanges()
        } else {
            saveEditorChanges(metaData)
        }
    }

    private fun saveMediaChanges() {
        val title = getFieldValue(Field.TITLE)
        val caption = getFieldValue(Field.CAPTION)
        val altText = getFieldValue(Field.ALT_TEXT)
        val description = getFieldValue(Field.DESCRIPTION)

        // compare against the values last pushed rather than the store, which only changes once a push finishes,
        // so pausing again before then doesn't push the same edit twice
        if (unsavedEdits().isEmpty()) {
            return
        }

        val storedMedia = mediaStore.getMediaWithLocalId(media.id)
        if (storedMedia == null) {
            AppLog.w(T.MEDIA, "MediaSettingsFragment > Cannot save null media")
            ToastUtils.showToast(requireContext(), R.string.media_edit_failure)
            return
        }

        AppLog.d(T.MEDIA, "MediaSettingsFragment > Saving changes")
        listOf(storedMedia, media).forEach {
            it.title = title
            it.caption = caption
            it.alt = altText
            it.description = description
        }
        dispatcher.dispatch(MediaActionBuilder.newPushMediaAction(MediaPayload(site, storedMedia)))
    }

    private fun saveEditorChanges(metaData: EditorImageMetaData) {
        val binding = binding ?: return
        val title = getFieldValue(Field.TITLE)
        val caption = getFieldValue(Field.CAPTION)
        val altText = getFieldValue(Field.ALT_TEXT)
        val alignment = alignmentKeys.getOrNull(binding.alignmentSpinner.selectedItemPosition) ?: metaData.align
        val size = getString(imageSize.cssClass)
        val linkUrl = EditTextUtils.getText(binding.editLink)
        val linkTargetBlank = binding.editLinkTargetNewWidnowCheckbox.isChecked
        val hasSizeChanged = metaData.size.orEmpty() != size

        val hasChanged = hasSizeChanged ||
                metaData.title.orEmpty() != title ||
                metaData.alt.orEmpty() != altText ||
                metaData.caption.orEmpty() != caption ||
                metaData.align.orEmpty() != alignment.orEmpty() ||
                metaData.linkUrl.orEmpty() != linkUrl ||
                metaData.isLinkTargetBlank != linkTargetBlank
        // with no change the result stays as it was: RESULT_CANCELED, or RESULT_OK from an earlier save
        if (!hasChanged) {
            return
        }

        metaData.title = title
        metaData.size = size
        metaData.alt = altText
        metaData.align = alignment
        metaData.caption = caption
        metaData.linkUrl = linkUrl
        metaData.isLinkTargetBlank = linkTargetBlank

        // we only update width and height attributes on self hosted and Jetpack sites
        // because css image size classes wont have any effect there
        if (!site.isWPCom && hasSizeChanged) {
            updateImageSizeParameters(metaData)
        }

        requireActivity().setResult(Activity.RESULT_OK, Intent().putExtra(ARG_EDITOR_IMAGE_METADATA, metaData))
    }

    private fun updateImageSizeParameters(metaData: EditorImageMetaData) {
        // if caption is empty we can safely remove width and height attributes
        if (imageSize == MediaSettingsImageSize.FULL && metaData.caption.isNullOrEmpty()) {
            metaData.width = null
            metaData.height = null
            return
        }

        var imageWidth = metaData.widthInt
        var imageHeight = metaData.heightInt
        val newImageSize = resources.getInteger(imageSize.size)
        val aspectRatio = imageWidth.toFloat() / imageHeight.toFloat()

        when {
            imageWidth > imageHeight -> {
                imageHeight = (newImageSize / aspectRatio).roundToInt()
                imageWidth = newImageSize
            }
            imageWidth < imageHeight -> {
                imageWidth = (newImageSize * aspectRatio).roundToInt()
                imageHeight = newImageSize
            }
            else -> {
                imageHeight = newImageSize
                imageWidth = newImageSize
            }
        }

        metaData.width = imageWidth.toString()
        metaData.height = imageHeight.toString()
    }

    /**
     * Saves the media to the device using the Android DownloadManager
     */
    @Suppress("DEPRECATION") // allowScanningByMediaScanner is a no-op from Q but still needed below it
    private fun saveMediaToDevice() {
        val activity = requireActivity()
        // must request the permission even though it's already defined in the manifest
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R &&
            ContextCompat.checkSelfPermission(activity, WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
        ) {
            storagePermissionLauncher.launch(WRITE_EXTERNAL_STORAGE)
            return
        }
        if (!NetworkUtils.checkConnection(activity)) {
            return
        }

        val request = DownloadManager.Request(Uri.parse(media.url))
        try {
            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, media.fileName)
        } catch (error: IllegalStateException) {
            AppLog.e(T.MEDIA, error)
            ToastUtils.showToast(activity, R.string.error_media_save)
            return
        }
        request.allowScanningByMediaScanner()
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
        request.addRequestHeader("User-Agent", userAgent.apiUserAgent)

        downloadId = activity.getSystemService(DownloadManager::class.java).enqueue(request)
        updateMenu()
        ToastUtils.showToast(activity, R.string.media_downloading)
    }

    private fun shareMedia() {
        val intent = Intent(Intent.ACTION_SEND)
            .setType("text/plain")
            .putExtra(Intent.EXTRA_TEXT, media.url)
        val subject = media.title?.takeIf { it.isNotEmpty() } ?: media.description.takeIf { it.isNotEmpty() }
        subject?.let { intent.putExtra(Intent.EXTRA_SUBJECT, it) }
        try {
            startActivity(Intent.createChooser(intent, getString(R.string.share_link)))
        } catch (e: ActivityNotFoundException) {
            AppLog.e(T.MEDIA, e)
            ToastUtils.showToast(requireContext(), R.string.reader_toast_err_share_intent)
        }
    }

    /**
     * Depending on the media source it either removes it from the post or deletes it from the media library
     */
    private fun deleteMediaWithConfirmation() {
        deleteConfirmationDialog?.let {
            it.show()
            return
        }

        val messageRes = when {
            isMediaFromEditor -> R.string.confirm_remove_media_image
            mediaType == MediaType.VIDEO -> R.string.confirm_delete_media_video
            else -> R.string.confirm_delete_media_image
        }
        deleteConfirmationDialog = MaterialAlertDialogBuilder(requireContext())
            .setMessage(messageRes)
            .setCancelable(true)
            .setPositiveButton(if (isMediaFromEditor) R.string.remove else R.string.delete) { _, _ ->
                if (isMediaFromEditor) removeMediaFromPost() else deleteMedia()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun deleteMedia() {
        if (!NetworkUtils.checkConnection(requireContext())) {
            return
        }

        progressDialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.deleting_media_dlg)
            .setView(R.layout.media_picker_progress_dialog)
            .setCancelable(false)
            .show()

        AppLog.v(T.MEDIA, "Deleting ${media.title} (id=${media.mediaId})")
        dispatcher.dispatch(MediaActionBuilder.newDeleteMediaAction(MediaPayload(site, media)))
    }

    private fun removeMediaFromPost() {
        val metaData = editorImageMetaData ?: return
        metaData.markAsRemoved()
        with(requireActivity()) {
            setResult(Activity.RESULT_OK, Intent().putExtra(ARG_EDITOR_IMAGE_METADATA, metaData))
            finish()
        }
    }

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onMediaChanged(event: OnMediaChanged) {
        if (!hasMedia || isMediaFromEditor) {
            return
        }
        // every page hears every change, so only react to the ones for this page's item, or to the result of a
        // delete this page started
        val isThisMedia = event.mediaList.any { it?.id == media.id }
        if (event.cause == MediaAction.DELETE_MEDIA) {
            if (isThisMedia || progressDialog != null) {
                onMediaDeleted(event)
            }
        } else if (isThisMedia && !event.isError) {
            reloadMedia()
        }
    }

    private fun onMediaDeleted(event: OnMediaChanged) {
        progressDialog?.dismiss()
        progressDialog = null
        if (event.isError) {
            ToastUtils.showToast(requireContext(), R.string.error_generic)
        } else {
            with(requireActivity()) {
                setResult(MediaSettingsActivity.RESULT_MEDIA_DELETED)
                finish()
            }
        }
    }

    @Suppress("TooGenericExceptionCaught")
    private fun copyMediaUrlToClipboard() {
        val context = requireContext()
        try {
            context.getSystemService(ClipboardManager::class.java)
                .setPrimaryClip(ClipData.newPlainText(getString(R.string.app_name), media.url))
            ToastUtils.showToast(context, R.string.media_edit_copy_url_toast)
        } catch (e: Exception) {
            AppLog.e(T.UTILS, e)
            ToastUtils.showToast(context, R.string.error_copy_to_clipboard)
        }
    }

    companion object {
        private const val TOOLBAR_HEIGHT_MULTIPLIER = 3
        private const val IMAGE_HEIGHT_RATIO = 0.4f
        private const val FINISH_DELAY_MS = 1500L
        private const val ARG_MEDIA_LOCAL_ID = "media_local_id"
        private const val STATE_FIELD_PREFIX = "state_field_"
        private const val STATE_DELETE_DIALOG_VISIBLE = "delete_media_dialog_visible"

        fun newInstance(mediaId: Int) = MediaSettingsFragment().apply {
            arguments = bundleOf(ARG_MEDIA_LOCAL_ID to mediaId)
        }

        fun newInstance(editorImageMetaData: EditorImageMetaData) = MediaSettingsFragment().apply {
            arguments = bundleOf(ARG_EDITOR_IMAGE_METADATA to editorImageMetaData)
        }
    }
}
