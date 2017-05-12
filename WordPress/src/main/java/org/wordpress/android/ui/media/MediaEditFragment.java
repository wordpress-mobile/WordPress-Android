package org.wordpress.android.ui.media;

import android.app.Fragment;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.fluxc.Dispatcher;
import org.wordpress.android.fluxc.generated.MediaActionBuilder;
import org.wordpress.android.fluxc.model.MediaModel;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.store.MediaStore;
import org.wordpress.android.fluxc.store.MediaStore.MediaPayload;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.DisplayUtils;
import org.wordpress.android.util.EditTextUtils;
import org.wordpress.android.util.StringUtils;
import org.wordpress.android.util.ToastUtils;

import javax.inject.Inject;

/**
 * A fragment for editing media on the Media tab
 */
public class MediaEditFragment extends Fragment {
    private static final String ARGS_MEDIA_ID = "media_id";
    // also appears in the layouts, from the strings.xml
    public static final String TAG = "MediaEditFragment";

    @Inject Dispatcher mDispatcher;
    @Inject MediaStore mMediaStore;

    private EditText mTitleView;
    private EditText mCaptionView;
    private EditText mDescriptionView;

    private int mLocalMediaId;
    private SiteModel mSite;

    public static MediaEditFragment newInstance(SiteModel site, int localMediaId) {
        MediaEditFragment fragment = new MediaEditFragment();
        Bundle args = new Bundle();
        args.putInt(ARGS_MEDIA_ID, localMediaId);
        args.putSerializable(WordPress.SITE, site);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ((WordPress) getActivity().getApplication()).component().inject(this);

        if (savedInstanceState != null) {
            mSite = (SiteModel) savedInstanceState.getSerializable(WordPress.SITE);
            mLocalMediaId = savedInstanceState.getInt(ARGS_MEDIA_ID);
        }

        if (mSite == null) {
            ToastUtils.showToast(getActivity(), R.string.blog_not_found, ToastUtils.Duration.SHORT);
            getActivity().finish();
        }

        setHasOptionsMenu(true);

        // retain this fragment across configuration changes
        setRetainInstance(true);
    }

    @Override
    public void setArguments(Bundle args) {
        super.setArguments(args);
        if (args != null) {
            mSite = (SiteModel) args.getSerializable(WordPress.SITE);
            mLocalMediaId = args.getInt(ARGS_MEDIA_ID);
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        mDispatcher.register(this);
    }

    @Override
    public void onStop() {
        mDispatcher.unregister(this);
        super.onStop();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view  = inflater.inflate(R.layout.media_edit_fragment, container, false);

        mTitleView = (EditText) view.findViewById(R.id.media_edit_fragment_title);
        mCaptionView = (EditText) view.findViewById(R.id.media_edit_fragment_caption);
        mDescriptionView = (EditText) view.findViewById(R.id.media_edit_fragment_description);

        loadMedia();

        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        // force the keyboard to appear on the title when activity is created (but not in landscape)
        if (savedInstanceState == null && !DisplayUtils.isLandscape(getActivity())) {
            EditTextUtils.showSoftInput(mTitleView);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putSerializable(WordPress.SITE, mSite);
        outState.putInt(ARGS_MEDIA_ID, mLocalMediaId);
    }

    public void loadMedia() {
        if (isAdded()) {
            MediaModel media = mMediaStore.getMediaWithLocalId(mLocalMediaId);
            refreshViews(media);
        }
    }

    public void saveChanges() {
        MediaModel media = mMediaStore.getMediaWithLocalId(mLocalMediaId);
        if (media != null && isAdded()) {
            boolean isDirty = !StringUtils.equals(media.getTitle(), mTitleView.getText().toString())
                    || !StringUtils.equals(media.getCaption(), mCaptionView.getText().toString())
                    || !StringUtils.equals(media.getDescription(), mDescriptionView.getText().toString());
            if (isDirty) {
                AppLog.d(AppLog.T.MEDIA, "MediaEditFragment > Saving changes");
                media.setTitle(mTitleView.getText().toString());
                media.setDescription(mDescriptionView.getText().toString());
                media.setCaption(mCaptionView.getText().toString());
                mDispatcher.dispatch(MediaActionBuilder.newPushMediaAction(new MediaPayload(mSite, media)));
            }
        }
    }

    private void refreshViews(MediaModel mediaModel) {
        if (mediaModel == null || !isAdded()) {
            return;
        }

        mTitleView.setText(mediaModel.getTitle());
        mTitleView.requestFocus();
        mTitleView.setSelection(mTitleView.getText().length());
        mCaptionView.setText(mediaModel.getCaption());
        mDescriptionView.setText(mediaModel.getDescription());
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMediaChanged(MediaStore.OnMediaChanged event) {
        if (isAdded() && event.isError()) {
            ToastUtils.showToast(getActivity(), R.string.media_edit_failure);
        }
    }
}
