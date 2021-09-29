package org.wordpress.android.editor.gutenberg

import android.view.ViewGroup
import com.facebook.react.ReactPackage
import org.wordpress.mobile.WPAndroidGlue.RequestExecutor
import org.wordpress.mobile.WPAndroidGlue.ShowSuggestionsUtil
import org.wordpress.mobile.WPAndroidGlue.WPAndroidGlueCode

class WPAndroidGlueCodeWPCOM : WPAndroidGlueCode() {
    private var mGutenbergBridgeJS2ParentWPCOMListener: GutenbergBridgeJS2ParentWPCOMListener? = null

    override fun getPackages(): MutableList<ReactPackage> {
        val packages: ArrayList<ReactPackage> = ArrayList(super.getPackages())
        packages.add(RNExamplePackage(object: GutenbergBridgeJS2ParentWPCOM {
            override fun justToast(text: String?) {
                mGutenbergBridgeJS2ParentWPCOMListener?.justToast(text)
            }
        }))
        return packages
    }

    interface GutenbergBridgeJS2ParentWPCOMListener {
        fun justToast(text: String?)
    }

    fun attachToContainer(
        viewGroup: ViewGroup?,
        onMediaLibraryButtonListener: OnMediaLibraryButtonListener?,
        onReattachMediaUploadQueryListener: OnReattachMediaUploadQueryListener?,
        onReattachMediaSavingQueryListener: OnReattachMediaSavingQueryListener?,
        onSetFeaturedImageListener: OnSetFeaturedImageListener?,
        onEditorMountListener: OnEditorMountListener?,
        onEditorAutosaveListener: OnEditorAutosaveListener?,
        onAuthHeaderRequestedListener: OnAuthHeaderRequestedListener?,
        fetchExecutor: RequestExecutor?,
        onImageFullscreenPreviewListener: OnImageFullscreenPreviewListener?,
        onMediaEditorListener: OnMediaEditorListener?,
        onGutenbergDidRequestUnsupportedBlockFallbackListener: OnGutenbergDidRequestUnsupportedBlockFallbackListener?,
        onGutenbergDidSendButtonPressedActionListener: OnGutenbergDidSendButtonPressedActionListener?,
        showSuggestionsUtil: ShowSuggestionsUtil?,
        onMediaFilesCollectionBasedBlockEditorListener: OnMediaFilesCollectionBasedBlockEditorListener?,
        onFocalPointPickerTooltipListener: OnFocalPointPickerTooltipShownEventListener?,
        onGutenbergDidRequestPreviewListener: OnGutenbergDidRequestPreviewListener?,
        onBlockTypeImpressionsEventListener: OnBlockTypeImpressionsEventListener?,
        isDarkMode: Boolean,
        gutenbergBridgeJS2ParentWPCOMListener: GutenbergBridgeJS2ParentWPCOMListener
    ) {
        super.attachToContainer(
                viewGroup,
                onMediaLibraryButtonListener,
                onReattachMediaUploadQueryListener,
                onReattachMediaSavingQueryListener,
                onSetFeaturedImageListener,
                onEditorMountListener,
                onEditorAutosaveListener,
                onAuthHeaderRequestedListener,
                fetchExecutor,
                onImageFullscreenPreviewListener,
                onMediaEditorListener,
                onGutenbergDidRequestUnsupportedBlockFallbackListener,
                onGutenbergDidSendButtonPressedActionListener,
                showSuggestionsUtil,
                onMediaFilesCollectionBasedBlockEditorListener,
                onFocalPointPickerTooltipListener,
                onGutenbergDidRequestPreviewListener,
                onBlockTypeImpressionsEventListener,
                isDarkMode
        )
        mGutenbergBridgeJS2ParentWPCOMListener = gutenbergBridgeJS2ParentWPCOMListener
    }
}
