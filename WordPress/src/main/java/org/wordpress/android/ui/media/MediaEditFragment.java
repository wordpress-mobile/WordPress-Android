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
import org.wordpress.android.util.DisplayUtils;
import org.wordpress.android.util.EditTextUtils;
import org.wordpress.android.util.MediaUtils;
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
    private static final int MISSING_MEDIA_ID = -1;

    @Inject Dispatcher mDispatcher;
    @Inject MediaStore mMediaStore;

    private EditText mTitleView;
    private EditText mCaptionView;
    private EditText mDescriptionView;

    private String mTitleOriginal;
    private String mDescriptionOriginal;
    private String mCaptionOriginal;

    private int mLocalMediaId = MISSING_MEDIA_ID;

    private SiteModel mSite;
    private MediaModel mMediaModel;

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

        if (savedInstanceState == null) {
            if (getArguments() != null) {
                mSite = (SiteModel) getArguments().getSerializable(WordPress.SITE);
            } else {
                mSite = (SiteModel) getActivity().getIntent().getSerializableExtra(WordPress.SITE);
            }
        } else {
            mSite = (SiteModel) savedInstanceState.getSerializable(WordPress.SITE);
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
    public void onStart() {
        super.onStart();
        mDispatcher.register(this);
    }

    @Override
    public void onStop() {
        mDispatcher.unregister(this);
        super.onStop();
    }

    private int getLocalMediaId() {
        if (mLocalMediaId != MISSING_MEDIA_ID) {
            return mLocalMediaId;
        } else if (getArguments() != null) {
            mLocalMediaId = getArguments().getInt(ARGS_MEDIA_ID);
            return mLocalMediaId;
        } else {
            return MISSING_MEDIA_ID;
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view  = inflater.inflate(R.layout.media_edit_fragment, container, false);

        mTitleView = (EditText) view.findViewById(R.id.media_edit_fragment_title);
        mCaptionView = (EditText) view.findViewById(R.id.media_edit_fragment_caption);
        mDescriptionView = (EditText) view.findViewById(R.id.media_edit_fragment_description);

        loadMedia(getLocalMediaId());

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
    }

    public void loadMedia(int localMediaId) {
        mLocalMediaId = localMediaId;
        if (getActivity() != null && mLocalMediaId != MISSING_MEDIA_ID) {
            mMediaModel = mMediaStore.getMediaWithLocalId(mLocalMediaId);
            refreshViews(mMediaModel);
        } else {
            refreshViews(null);
        }
    }

    public void saveChanges() {
        if (isDirty()) {
            mMediaModel.setTitle(mTitleView.getText().toString());
            mMediaModel.setDescription(mDescriptionView.getText().toString());
            mMediaModel.setCaption(mCaptionView.getText().toString());
            mDispatcher.dispatch(MediaActionBuilder.newPushMediaAction(new MediaPayload(mSite, mMediaModel)));
        }
    }

    private void refreshViews(MediaModel mediaModel) {
        if (mediaModel == null) {
            return;
        }

        boolean isLocal = MediaUtils.isLocalFile(mediaModel.getUploadState());

        // user can't edit local files
        mTitleView.setEnabled(!isLocal);
        mCaptionView.setEnabled(!isLocal);
        mDescriptionView.setEnabled(!isLocal);

        mTitleOriginal = mediaModel.getTitle();
        mCaptionOriginal = mediaModel.getCaption();
        mDescriptionOriginal = mediaModel.getDescription();

        mTitleView.setText(mediaModel.getTitle());
        mTitleView.requestFocus();
        mTitleView.setSelection(mTitleView.getText().length());
        mCaptionView.setText(mediaModel.getCaption());
        mDescriptionView.setText(mediaModel.getDescription());
    }

    private boolean isDirty() {
        return mLocalMediaId != MISSING_MEDIA_ID &&
                (!StringUtils.equals(mTitleOriginal, mTitleView.getText().toString())
                || !StringUtils.equals(mCaptionOriginal, mCaptionView.getText().toString())
                || !StringUtils.equals(mDescriptionOriginal, mDescriptionView.getText().toString()));
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMediaChanged(MediaStore.OnMediaChanged event) {
        if (isAdded() && event.isError()) {
            ToastUtils.showToast(getActivity(), R.string.media_edit_failure);
        }
    }
}
