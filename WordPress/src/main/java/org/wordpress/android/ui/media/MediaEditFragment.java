package org.wordpress.android.ui.media;

import android.app.Fragment;
import android.os.Bundle;
import android.support.annotation.NonNull;
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
    private static final String ARGS_LOCAL_MEDIA_ID = "media_id";
    static final String TAG = "MediaEditFragment";

    @Inject Dispatcher mDispatcher;
    @Inject MediaStore mMediaStore;

    private EditText mTitleView;
    private EditText mCaptionView;
    private EditText mDescriptionView;

    private int mLocalMediaId;
    private SiteModel mSite;

    public static MediaEditFragment newInstance(@NonNull SiteModel site, int localMediaId) {
        MediaEditFragment fragment = new MediaEditFragment();
        Bundle args = new Bundle();
        args.putInt(ARGS_LOCAL_MEDIA_ID, localMediaId);
        args.putSerializable(WordPress.SITE, site);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ((WordPress) getActivity().getApplication()).component().inject(this);

        setHasOptionsMenu(true);
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

        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // force the keyboard to appear on the title when activity is created (but not in landscape)
        if (savedInstanceState == null && !DisplayUtils.isLandscape(getActivity())) {
            EditTextUtils.showSoftInput(mTitleView);
        }

        // getArguments() should never be null but let's check anyway
        if (getArguments() != null) {
            mSite = (SiteModel) getArguments().getSerializable(WordPress.SITE);
            mLocalMediaId = getArguments().getInt(ARGS_LOCAL_MEDIA_ID);
        }

        loadMedia();
    }

    void loadMedia() {
        if (!isAdded()) return;

        MediaModel media = mMediaStore.getMediaWithLocalId(mLocalMediaId);
        if (media != null) {
            mTitleView.setText(media.getTitle());
            mCaptionView.setText(media.getCaption());
            mDescriptionView.setText(media.getDescription());

            mTitleView.requestFocus();
            mTitleView.setSelection(mTitleView.getText().length());
        } else {
            ToastUtils.showToast(getActivity(), R.string.error_media_load);
        }
    }

    public void saveChanges() {
        if (!isAdded()) return;

        MediaModel media = mMediaStore.getMediaWithLocalId(mLocalMediaId);
        if (media == null) {
            AppLog.w(AppLog.T.MEDIA, "MediaEditFragment > Cannot save null media");
            ToastUtils.showToast(getActivity(), R.string.media_edit_failure);
            return;
        }

        String thisTitle = EditTextUtils.getText(mTitleView);
        String thisCaption = EditTextUtils.getText(mCaptionView);
        String thisDescription = EditTextUtils.getText(mDescriptionView);

        boolean hasChanged = !StringUtils.equals(media.getTitle(), thisTitle)
                || !StringUtils.equals(media.getCaption(), thisCaption)
                || !StringUtils.equals(media.getDescription(), thisDescription);
        if (hasChanged) {
            AppLog.d(AppLog.T.MEDIA, "MediaEditFragment > Saving changes");
            media.setTitle(thisTitle);
            media.setCaption(thisCaption);
            media.setDescription(thisDescription);
            mDispatcher.dispatch(MediaActionBuilder.newPushMediaAction(new MediaPayload(mSite, media)));
        }
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMediaChanged(MediaStore.OnMediaChanged event) {
        if (isAdded() && event.isError()) {
            ToastUtils.showToast(getActivity(), R.string.media_edit_failure);
        }
    }
}
