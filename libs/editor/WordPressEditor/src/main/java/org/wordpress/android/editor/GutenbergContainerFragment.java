package org.wordpress.android.editor;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.ViewGroup;

import org.wordpress.mobile.WPAndroidGlue.WPAndroidGlueCode;
import org.wordpress.mobile.WPAndroidGlue.WPAndroidGlueCode.OnEditorMountListener;
import org.wordpress.mobile.WPAndroidGlue.WPAndroidGlueCode.OnGetContentTimeout;
import org.wordpress.mobile.WPAndroidGlue.WPAndroidGlueCode.OnMediaLibraryButtonListener;
import org.wordpress.mobile.WPAndroidGlue.WPAndroidGlueCode.OnReattachQueryListener;

public class GutenbergContainerFragment extends Fragment {
    public static final String TAG = "gutenberg_container_fragment_tag";

    private static final String ARG_IS_NEW_POST = "param_is_new_post";
    private static final String ARG_LOCALE = "param_locale";
    private static final String ARG_TRANSLATIONS = "param_translations";

    private boolean mHtmlModeEnabled;
    private boolean mHasReceivedAnyContent;

    private WPAndroidGlueCode mWPAndroidGlueCode;

    public static GutenbergContainerFragment newInstance(boolean isNewPost, String localeString, Bundle translations) {
        GutenbergContainerFragment fragment = new GutenbergContainerFragment();
        Bundle args = new Bundle();
        args.putBoolean(ARG_IS_NEW_POST, isNewPost);
        args.putString(ARG_LOCALE, localeString);
        args.putBundle(ARG_TRANSLATIONS, translations);
        fragment.setArguments(args);
        return fragment;
    }

    public boolean hasReceivedAnyContent() {
        return mHasReceivedAnyContent;
    }

    public void attachToContainer(ViewGroup viewGroup, OnMediaLibraryButtonListener onMediaLibraryButtonListener,
                                  OnReattachQueryListener onReattachQueryListener,
                                  OnEditorMountListener onEditorMountListener) {
        mWPAndroidGlueCode.attachToContainer(viewGroup, onMediaLibraryButtonListener, onReattachQueryListener,
                onEditorMountListener);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        boolean isNewPost = getArguments() != null && getArguments().getBoolean(ARG_IS_NEW_POST);
        String localeString = getArguments().getString(ARG_LOCALE);
        Bundle translations = getArguments().getBundle(ARG_TRANSLATIONS);

        mWPAndroidGlueCode = new WPAndroidGlueCode();
        mWPAndroidGlueCode.onCreate(getContext());
        mWPAndroidGlueCode.onCreateView(
                getContext(),
                mHtmlModeEnabled,
                getActivity().getApplication(),
                BuildConfig.DEBUG,
                BuildConfig.BUILD_GUTENBERG_FROM_SOURCE,
                isNewPost,
                localeString,
                translations);

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

        mWPAndroidGlueCode.toggleEditorMode();
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

    public void appendMediaFile(int mediaId, final String mediaUrl) {
        mWPAndroidGlueCode.appendMediaFile(mediaId, mediaUrl);
    }

    public void showDevOptionsDialog() {
        mWPAndroidGlueCode.showDevOptionsDialog();
    }

    public void appendUploadMediaFile(final int mediaId, final String mediaUri) {
        mWPAndroidGlueCode.appendUploadMediaFile(mediaId, mediaUri);
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
}
