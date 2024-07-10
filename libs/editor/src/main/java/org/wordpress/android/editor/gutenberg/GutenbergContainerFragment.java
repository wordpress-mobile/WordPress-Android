package org.wordpress.android.editor.gutenberg;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.view.ViewGroup;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.Consumer;
import androidx.core.util.Pair;
import androidx.fragment.app.Fragment;

import com.facebook.react.bridge.WritableNativeMap;

import org.wordpress.android.editor.BuildConfig;
import org.wordpress.android.editor.ExceptionLogger;
import org.wordpress.android.editor.R;
import org.wordpress.android.editor.savedinstance.SavedInstanceDatabase;
import org.wordpress.mobile.WPAndroidGlue.ShowSuggestionsUtil;
import org.wordpress.mobile.WPAndroidGlue.GutenbergProps;
import org.wordpress.mobile.WPAndroidGlue.RequestExecutor;
import org.wordpress.mobile.WPAndroidGlue.Media;
import org.wordpress.mobile.WPAndroidGlue.WPAndroidGlueCode;
import org.wordpress.mobile.WPAndroidGlue.WPAndroidGlueCode.OnAuthHeaderRequestedListener;
import org.wordpress.mobile.WPAndroidGlue.WPAndroidGlueCode.OnBlockTypeImpressionsEventListener;
import org.wordpress.mobile.WPAndroidGlue.WPAndroidGlueCode.OnBackHandlerEventListener;
import org.wordpress.mobile.WPAndroidGlue.WPAndroidGlueCode.OnConnectionStatusEventListener;
import org.wordpress.mobile.WPAndroidGlue.WPAndroidGlueCode.OnContentInfoReceivedListener;
import org.wordpress.mobile.WPAndroidGlue.WPAndroidGlueCode.OnCustomerSupportOptionsListener;
import org.wordpress.mobile.WPAndroidGlue.WPAndroidGlueCode.OnEditorAutosaveListener;
import org.wordpress.mobile.WPAndroidGlue.WPAndroidGlueCode.OnEditorMountListener;
import org.wordpress.mobile.WPAndroidGlue.WPAndroidGlueCode.OnGetContentInterrupted;
import org.wordpress.mobile.WPAndroidGlue.WPAndroidGlueCode.OnGutenbergDidRequestEmbedFullscreenPreviewListener;
import org.wordpress.mobile.WPAndroidGlue.WPAndroidGlueCode.OnGutenbergDidRequestUnsupportedBlockFallbackListener;
import org.wordpress.mobile.WPAndroidGlue.WPAndroidGlueCode.OnGutenbergDidSendButtonPressedActionListener;
import org.wordpress.mobile.WPAndroidGlue.WPAndroidGlueCode.OnImageFullscreenPreviewListener;
import org.wordpress.mobile.WPAndroidGlue.WPAndroidGlueCode.OnLogExceptionListener;
import org.wordpress.mobile.WPAndroidGlue.WPAndroidGlueCode.OnReattachMediaUploadQueryListener;
import org.wordpress.mobile.WPAndroidGlue.WPAndroidGlueCode.OnFocalPointPickerTooltipShownEventListener;
import org.wordpress.mobile.WPAndroidGlue.WPAndroidGlueCode.OnMediaEditorListener;
import org.wordpress.mobile.WPAndroidGlue.WPAndroidGlueCode.OnMediaLibraryButtonListener;
import org.wordpress.mobile.WPAndroidGlue.WPAndroidGlueCode.OnSendEventToHostListener;
import org.wordpress.mobile.WPAndroidGlue.WPAndroidGlueCode.OnToggleUndoButtonListener;
import org.wordpress.mobile.WPAndroidGlue.WPAndroidGlueCode.OnToggleRedoButtonListener;
import org.wordpress.mobile.WPAndroidGlue.WPAndroidGlueCode.OnSetFeaturedImageListener;
import org.wordpress.mobile.WPAndroidGlue.WPAndroidGlueCode.OnGutenbergDidRequestPreviewListener;

import java.util.ArrayList;

public class GutenbergContainerFragment extends Fragment {
    public static final String TAG = "gutenberg_container_fragment_tag";

    private static final String ARG_GUTENBERG_PROPS_BUILDER = "param_gutenberg_props_builder";

    private boolean mHtmlModeEnabled;
    private boolean mHasReceivedAnyContent;

    private WPAndroidGlueCode mWPAndroidGlueCode;
    public static GutenbergContainerFragment newInstance(Context context, GutenbergPropsBuilder gutenbergPropsBuilder) {
        GutenbergContainerFragment fragment = new GutenbergContainerFragment();
        SavedInstanceDatabase db = SavedInstanceDatabase.Companion.getDatabase(context);
        if (db != null) {
            db.addParcel(ARG_GUTENBERG_PROPS_BUILDER, gutenbergPropsBuilder);
        }
        return fragment;
    }

    public boolean hasReceivedAnyContent() {
        return mHasReceivedAnyContent;
    }

    public void attachToContainer(ViewGroup viewGroup, OnMediaLibraryButtonListener onMediaLibraryButtonListener,
                                  OnReattachMediaUploadQueryListener onReattachQueryListener,
                                  OnSetFeaturedImageListener onSetFeaturedImageListener,
                                  OnEditorMountListener onEditorMountListener,
                                  OnEditorAutosaveListener onEditorAutosaveListener,
                                  OnAuthHeaderRequestedListener onAuthHeaderRequestedListener,
                                  RequestExecutor fetchExecutor,
                                  OnImageFullscreenPreviewListener onImageFullscreenPreviewListener,
                                  OnMediaEditorListener onMediaEditorListener,
                                  OnGutenbergDidRequestUnsupportedBlockFallbackListener
                                          onGutenbergDidRequestUnsupportedBlockFallbackListener,
                                  OnGutenbergDidRequestEmbedFullscreenPreviewListener
                                          onGutenbergDidRequestEmbedFullscreenPreviewListener,
                                  OnGutenbergDidSendButtonPressedActionListener
                                          onGutenbergDidSendButtonPressedActionListener,
                                  ShowSuggestionsUtil showSuggestionsUtil,
                                  OnFocalPointPickerTooltipShownEventListener onFPPTooltipShownEventListener,
                                  OnGutenbergDidRequestPreviewListener
                                          onGutenbergDidRequestPreviewListener,
                                  OnBlockTypeImpressionsEventListener onBlockTypeImpressionsListener,
                                  OnCustomerSupportOptionsListener onCustomerSupportOptionsListener,
                                  OnSendEventToHostListener onSendEventToHostListener,
                                  OnToggleUndoButtonListener onToggleUndoButtonListener,
                                  OnToggleRedoButtonListener onToggleRedoButtonListener,
                                  OnConnectionStatusEventListener onConnectionStatusEventListener,
                                  OnBackHandlerEventListener onBackHandlerEventListener,
                                  OnLogExceptionListener onLogExceptionListener,
                                  boolean isDarkMode) {
            mWPAndroidGlueCode.attachToContainer(
                    viewGroup,
                    onMediaLibraryButtonListener,
                    onReattachQueryListener,
                    onSetFeaturedImageListener,
                    onEditorMountListener,
                    onEditorAutosaveListener,
                    onAuthHeaderRequestedListener,
                    fetchExecutor,
                    onImageFullscreenPreviewListener,
                    onMediaEditorListener,
                    onGutenbergDidRequestUnsupportedBlockFallbackListener,
                    onGutenbergDidRequestEmbedFullscreenPreviewListener,
                    onGutenbergDidSendButtonPressedActionListener,
                    showSuggestionsUtil,
                    onFPPTooltipShownEventListener,
                    onGutenbergDidRequestPreviewListener,
                    onBlockTypeImpressionsListener,
                    onCustomerSupportOptionsListener,
                    onSendEventToHostListener,
                    onToggleUndoButtonListener,
                    onToggleRedoButtonListener,
                    onConnectionStatusEventListener,
                    onBackHandlerEventListener,
                    onLogExceptionListener,
                    isDarkMode);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        GutenbergPropsBuilder gutenbergPropsBuilder = null;
        SavedInstanceDatabase db = SavedInstanceDatabase.Companion.getDatabase(getContext());
        if (db != null) {
            gutenbergPropsBuilder = db.getParcel(ARG_GUTENBERG_PROPS_BUILDER, GutenbergPropsBuilder.CREATOR);
        }

        Consumer<Exception> exceptionLogger = null;
        Consumer<String> breadcrumbLogger = null;
        if (getActivity() instanceof ExceptionLogger) {
            ExceptionLogger exceptionLoggingActivity = ((ExceptionLogger) getActivity());
            exceptionLogger = exceptionLoggingActivity.getExceptionLogger();
            breadcrumbLogger = exceptionLoggingActivity.getBreadcrumbLogger();
        }

        mWPAndroidGlueCode = new WPAndroidGlueCode();
        mWPAndroidGlueCode.onCreate(getContext());
        mWPAndroidGlueCode.onCreateView(
                getContext(),
                getActivity().getApplication(),
                BuildConfig.DEBUG,
                getContext().getResources().getColor(R.color.background_color),
                exceptionLogger,
                breadcrumbLogger,
                gutenbergPropsBuilder.build(getActivity(), mHtmlModeEnabled));

        // clear the content initialization flag since a new ReactRootView has been created;
        mHasReceivedAnyContent = false;
    }

    @Override
    public void onPause() {
        super.onPause();

        mWPAndroidGlueCode.onPause(getActivity());
    }

    @Override
    public void onResume() {
        super.onResume();

        OnBackPressedCallback callback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (mWPAndroidGlueCode.shouldHandleBackPress()) {
                    mWPAndroidGlueCode.onBackPressed();
                } else {
                    if (isEnabled()) {
                        setEnabled(false); // Disable this callback
                        requireActivity().onBackPressed(); // Bubble up the onBackPressed event
                        setEnabled(true); // Re-enable this callback
                    }
                }
            }
        };

        getActivity().getOnBackPressedDispatcher().addCallback(this, callback);
        mWPAndroidGlueCode.onResume(this, getActivity());
    }

    @Override
    public void onDetach() {
        super.onDetach();

        mWPAndroidGlueCode.onDetach(getActivity());
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        mWPAndroidGlueCode.onDestroy(getActivity());
    }

    public void setTitle(String title) {
        mWPAndroidGlueCode.setTitle(title);
        mHasReceivedAnyContent = mWPAndroidGlueCode.hasReceivedInitialTitleAndContent();
    }

    public void setContent(String postContent) {
        mWPAndroidGlueCode.setContent(postContent);
        mHasReceivedAnyContent = mWPAndroidGlueCode.hasReceivedInitialTitleAndContent();
    }

    public void toggleHtmlMode() {
        mHtmlModeEnabled = !mHtmlModeEnabled;

        mWPAndroidGlueCode.toggleEditorMode(mHtmlModeEnabled);
    }

    public void sendToJSPostSaveEvent() {
        // Check that the activity isn't null, there is a possibility it can cause the following crash
        // https://github.com/wordpress-mobile/WordPress-Android/issues/20665
        final Activity activity = getActivity();
        if (activity != null) {
            mWPAndroidGlueCode.sendToJSPostSaveEvent();
        }
    }

    /**
     * Returns the contents of the content field from the JavaScript editor. Should be called from a background thread
     * where possible.
     */
    public CharSequence getContent(CharSequence originalContent, OnGetContentInterrupted onGetContentInterrupted) {
        return mWPAndroidGlueCode.getContent(originalContent, onGetContentInterrupted);
    }

    public Pair<CharSequence, CharSequence> getTitleAndContent(CharSequence originalContent,
                                                               OnGetContentInterrupted onGetContentInterrupted) {
        return mWPAndroidGlueCode.getTitleAndContent(originalContent, onGetContentInterrupted);
    }


    public void triggerGetContentInfo(OnContentInfoReceivedListener onContentInfoReceivedListener) {
        mWPAndroidGlueCode.triggerGetContentInfo(onContentInfoReceivedListener);
    }

    public void showDevOptionsDialog() {
        mWPAndroidGlueCode.showDevOptionsDialog();
    }

    public void appendMediaFiles(ArrayList<Media> mediaList) {
        mWPAndroidGlueCode.appendMediaFiles(mediaList);
    }

    public void mediaFileUploadProgress(final int mediaId, final float progress) {
        mWPAndroidGlueCode.mediaFileUploadProgress(mediaId, progress);
    }

    public void mediaFileUploadFailed(final int mediaId) {
        mWPAndroidGlueCode.mediaFileUploadFailed(mediaId);
    }

    public void mediaFileUploadPaused(final int mediaId) {
        mWPAndroidGlueCode.mediaFileUploadPaused(mediaId);
    }

    public void mediaFileUploadSucceeded(final int mediaId, final String mediaUrl, final int serverMediaId) {
        mWPAndroidGlueCode.mediaFileUploadSucceeded(mediaId, mediaUrl, serverMediaId, new WritableNativeMap());
    }

    public void mediaFileUploadSucceeded(final int mediaId, final String mediaUrl, final int serverMediaId, final
                                         WritableNativeMap metadata) {
        mWPAndroidGlueCode.mediaFileUploadSucceeded(mediaId, mediaUrl, serverMediaId, metadata);
    }

    public void clearMediaFileURL(final int mediaId) {
        mWPAndroidGlueCode.clearMediaFileURL(mediaId);
    }

    public void mediaSelectionCancelled() {
        mWPAndroidGlueCode.mediaSelectionCancelled();
    }

    public void replaceUnsupportedBlock(String content, String blockId) {
        mWPAndroidGlueCode.replaceUnsupportedBlock(content, blockId);
    }

    public void replaceStoryEditedBlock(String mediaFiles, String blockId) {
        mWPAndroidGlueCode.replaceMediaFilesEditedBlock(mediaFiles, blockId);
    }

    public void updateTheme(Bundle editorTheme) {
        mWPAndroidGlueCode.updateTheme(editorTheme);
    }

    public void showNotice(String message) {
        mWPAndroidGlueCode.showNotice(message);
    }

    public void showEditorHelp() {
        mWPAndroidGlueCode.showEditorHelp();
    }

    public void onUndoPressed() {
        mWPAndroidGlueCode.onUndoPressed();
    }

    public void onRedoPressed() {
        mWPAndroidGlueCode.onRedoPressed();
    }

    public void onContentUpdate(@NonNull String content) {
        mWPAndroidGlueCode.onContentUpdate(content);
    }

    public void updateCapabilities(GutenbergPropsBuilder gutenbergPropsBuilder) {
        // We want to make sure that activity isn't null
        // as it can make this crash to happen: https://github.com/wordpress-mobile/WordPress-Android/issues/13248
        final Activity activity = getActivity();
        if (activity != null) {
            GutenbergProps gutenbergProps = gutenbergPropsBuilder.build(activity, mHtmlModeEnabled);
            mWPAndroidGlueCode.updateCapabilities(gutenbergProps);
        }
    }

    public void clearFileSaveStatus(final String mediaId) {
        mWPAndroidGlueCode.clearFileSaveStatus(mediaId);
    }

    public void mediaFileSaveProgress(final String mediaId, final float progress) {
        mWPAndroidGlueCode.mediaFileSaveProgress(mediaId, progress);
    }

    public void mediaFileSaveFailed(final String mediaId) {
        mWPAndroidGlueCode.mediaFileSaveFailed(mediaId);
    }

    public void mediaFileSaveSucceeded(final String mediaId, final String mediaUrl) {
        mWPAndroidGlueCode.mediaFileSaveSucceeded(mediaId, mediaUrl);
    }

    public void onStorySaveResult(final String storyFirstMediaId, final boolean success) {
        mWPAndroidGlueCode.mediaCollectionFinalSaveResult(storyFirstMediaId, success);
    }

    public void onMediaModelCreatedForFile(String oldId, String newId, String oldUrl) {
        mWPAndroidGlueCode.mediaIdChanged(oldId, newId, oldUrl);
    }

    public void sendToJSFeaturedImageId(int mediaId) {
        mWPAndroidGlueCode.sendToJSFeaturedImageId(mediaId);
    }

    public void onConnectionStatusChange(boolean isConnected) {
        mWPAndroidGlueCode.connectionStatusChange(isConnected);
    }
}
