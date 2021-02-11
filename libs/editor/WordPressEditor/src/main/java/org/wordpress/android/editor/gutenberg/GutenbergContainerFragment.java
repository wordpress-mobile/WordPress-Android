package org.wordpress.android.editor.gutenberg;

import android.app.Activity;
import android.os.Bundle;
import android.view.ViewGroup;

import androidx.core.util.Consumer;
import androidx.fragment.app.Fragment;

import org.wordpress.android.editor.BuildConfig;
import org.wordpress.android.editor.ExceptionLogger;
import org.wordpress.android.editor.R;
import org.wordpress.mobile.WPAndroidGlue.ShowSuggestionsUtil;
import org.wordpress.mobile.WPAndroidGlue.GutenbergProps;
import org.wordpress.mobile.WPAndroidGlue.RequestExecutor;
import org.wordpress.mobile.WPAndroidGlue.Media;
import org.wordpress.mobile.WPAndroidGlue.WPAndroidGlueCode;
import org.wordpress.mobile.WPAndroidGlue.WPAndroidGlueCode.OnAuthHeaderRequestedListener;
import org.wordpress.mobile.WPAndroidGlue.WPAndroidGlueCode.OnContentInfoReceivedListener;
import org.wordpress.mobile.WPAndroidGlue.WPAndroidGlueCode.OnEditorAutosaveListener;
import org.wordpress.mobile.WPAndroidGlue.WPAndroidGlueCode.OnEditorMountListener;
import org.wordpress.mobile.WPAndroidGlue.WPAndroidGlueCode.OnGetContentTimeout;
import org.wordpress.mobile.WPAndroidGlue.WPAndroidGlueCode.OnGutenbergDidRequestUnsupportedBlockFallbackListener;
import org.wordpress.mobile.WPAndroidGlue.WPAndroidGlueCode.OnGutenbergDidSendButtonPressedActionListener;
import org.wordpress.mobile.WPAndroidGlue.WPAndroidGlueCode.OnImageFullscreenPreviewListener;
import org.wordpress.mobile.WPAndroidGlue.WPAndroidGlueCode.OnLogGutenbergUserEventListener;
import org.wordpress.mobile.WPAndroidGlue.WPAndroidGlueCode.OnReattachMediaSavingQueryListener;
import org.wordpress.mobile.WPAndroidGlue.WPAndroidGlueCode.OnReattachMediaUploadQueryListener;
import org.wordpress.mobile.WPAndroidGlue.WPAndroidGlueCode.OnFocalPointPickerTooltipShownEventListener;
import org.wordpress.mobile.WPAndroidGlue.WPAndroidGlueCode.OnMediaEditorListener;
import org.wordpress.mobile.WPAndroidGlue.WPAndroidGlueCode.OnMediaLibraryButtonListener;
import org.wordpress.mobile.WPAndroidGlue.WPAndroidGlueCode.OnMediaFilesCollectionBasedBlockEditorListener;

import java.util.ArrayList;

public class GutenbergContainerFragment extends Fragment {
    public static final String TAG = "gutenberg_container_fragment_tag";

    private static final String ARG_GUTENBERG_PROPS_BUILDER = "param_gutenberg_props_builder";

    private boolean mHtmlModeEnabled;
    private boolean mHasReceivedAnyContent;

    private WPAndroidGlueCode mWPAndroidGlueCode;
    public static GutenbergContainerFragment newInstance(GutenbergPropsBuilder gutenbergPropsBuilder) {
        GutenbergContainerFragment fragment = new GutenbergContainerFragment();
        Bundle args = new Bundle();
        args.putParcelable(ARG_GUTENBERG_PROPS_BUILDER, gutenbergPropsBuilder);
        fragment.setArguments(args);
        return fragment;
    }

    public boolean hasReceivedAnyContent() {
        return mHasReceivedAnyContent;
    }

    public void attachToContainer(ViewGroup viewGroup, OnMediaLibraryButtonListener onMediaLibraryButtonListener,
                                  OnReattachMediaUploadQueryListener onReattachQueryListener,
                                  OnReattachMediaSavingQueryListener onStorySavingReattachQueryListener,
                                  OnEditorMountListener onEditorMountListener,
                                  OnEditorAutosaveListener onEditorAutosaveListener,
                                  OnAuthHeaderRequestedListener onAuthHeaderRequestedListener,
                                  RequestExecutor fetchExecutor,
                                  OnImageFullscreenPreviewListener onImageFullscreenPreviewListener,
                                  OnMediaEditorListener onMediaEditorListener,
                                  OnLogGutenbergUserEventListener onLogGutenbergUserEventListener,
                                  OnGutenbergDidRequestUnsupportedBlockFallbackListener
                                          onGutenbergDidRequestUnsupportedBlockFallbackListener,
                                  OnGutenbergDidSendButtonPressedActionListener
                                          onGutenbergDidSendButtonPressedActionListener,
                                  ShowSuggestionsUtil showSuggestionsUtil,
                                  OnMediaFilesCollectionBasedBlockEditorListener
                                          onMediaFilesCollectionBasedBlockEditorListener,
                                  OnFocalPointPickerTooltipShownEventListener onFPPTooltipShownEventListener,
                                  boolean isDarkMode) {
            mWPAndroidGlueCode.attachToContainer(
                    viewGroup,
                    onMediaLibraryButtonListener,
                    onReattachQueryListener,
                    onStorySavingReattachQueryListener,
                    onEditorMountListener,
                    onEditorAutosaveListener,
                    onAuthHeaderRequestedListener,
                    fetchExecutor,
                    onImageFullscreenPreviewListener,
                    onMediaEditorListener,
                    onLogGutenbergUserEventListener,
                    onGutenbergDidRequestUnsupportedBlockFallbackListener,
                    onGutenbergDidSendButtonPressedActionListener,
                    showSuggestionsUtil,
                    onMediaFilesCollectionBasedBlockEditorListener,
                    onFPPTooltipShownEventListener,
                    isDarkMode);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        GutenbergPropsBuilder gutenbergPropsBuilder = getArguments().getParcelable(ARG_GUTENBERG_PROPS_BUILDER);

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
                BuildConfig.BUILD_GUTENBERG_FROM_SOURCE,
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

    /**
     * Returns the contents of the content field from the JavaScript editor. Should be called from a background thread
     * where possible.
     */
    public CharSequence getContent(CharSequence originalContent, OnGetContentTimeout onGetContentTimeout) {
        return mWPAndroidGlueCode.getContent(originalContent, onGetContentTimeout);
    }

    public CharSequence getTitle(OnGetContentTimeout onGetContentTimeout) {
        return mWPAndroidGlueCode.getTitle(onGetContentTimeout);
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

    public void mediaFileUploadSucceeded(final int mediaId, final String mediaUrl, final int serverMediaId) {
        mWPAndroidGlueCode.mediaFileUploadSucceeded(mediaId, mediaUrl, serverMediaId);
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
}
