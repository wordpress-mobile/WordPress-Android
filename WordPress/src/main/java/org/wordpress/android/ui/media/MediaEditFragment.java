package org.wordpress.android.ui.media;

import android.app.Activity;
import android.app.Fragment;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.Toast;

import com.android.volley.toolbox.NetworkImageView;

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
import org.wordpress.android.fluxc.store.MediaStore.OnMediaChanged;
import org.wordpress.android.fluxc.tools.FluxCImageLoader;
import org.wordpress.android.util.ActivityUtils;
import org.wordpress.android.util.ImageUtils.BitmapWorkerCallback;
import org.wordpress.android.util.ImageUtils.BitmapWorkerTask;
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
    public static final int MISSING_MEDIA_ID = -1;

    @Inject Dispatcher mDispatcher;
    @Inject MediaStore mMediaStore;
    @Inject FluxCImageLoader mImageLoader;

    private NetworkImageView mNetworkImageView;
    private ImageView mLocalImageView;
    private EditText mTitleView;
    private EditText mCaptionView;
    private EditText mDescriptionView;

    private String mTitleOriginal;
    private String mDescriptionOriginal;
    private String mCaptionOriginal;

    private MediaEditFragmentCallback mCallback;

    private int mLocalMediaId = MISSING_MEDIA_ID;
    private ScrollView mScrollView;
    private View mLinearLayout;

    private SiteModel mSite;
    private MediaModel mMediaModel;

    public interface MediaEditFragmentCallback {
        void onResume(Fragment fragment);
        void setLookClosable();
        void onPause(Fragment fragment);
        void onSavedEdit(int localMediaId, boolean result);
    }

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
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        try {
            mCallback = (MediaEditFragmentCallback) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement "
                                         + MediaEditFragmentCallback.class.getSimpleName());
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
    public void onDetach() {
        super.onDetach();
        // set callback to null so we don't accidentally leak the activity instance
        mCallback = null;
    }

    private boolean hasCallback() {
        return (mCallback != null);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (hasCallback()) {
            mCallback.onResume(this);
            mCallback.setLookClosable();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (hasCallback()) {
            mCallback.onPause(this);
        }
    }

    public int getLocalMediaId() {
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
        mScrollView = (ScrollView) inflater.inflate(R.layout.media_edit_fragment, container, false);

        mLinearLayout = mScrollView.findViewById(R.id.media_edit_linear_layout);
        mTitleView = (EditText) mScrollView.findViewById(R.id.media_edit_fragment_title);
        mCaptionView = (EditText) mScrollView.findViewById(R.id.media_edit_fragment_caption);
        mDescriptionView = (EditText) mScrollView.findViewById(R.id.media_edit_fragment_description);
        mLocalImageView = (ImageView) mScrollView.findViewById(R.id.media_edit_fragment_image_local);
        mNetworkImageView = (NetworkImageView) mScrollView.findViewById(R.id.media_edit_fragment_image_network);

        loadMedia(getLocalMediaId());

        return mScrollView;
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

    void saveMedia() {
        ActivityUtils.hideKeyboard(getActivity());
        mMediaModel.setTitle(mTitleView.getText().toString());
        mMediaModel.setDescription(mDescriptionView.getText().toString());
        mMediaModel.setCaption(mCaptionView.getText().toString());
        mDispatcher.dispatch(MediaActionBuilder.newPushMediaAction(new MediaPayload(mSite, mMediaModel)));
    }

    private void refreshImageView(MediaModel mediaModel, boolean isLocal) {
        final String imageUri;
        if (isLocal) {
            imageUri = mediaModel.getFilePath();
        } else {
            imageUri = mediaModel.getUrl();
        }
        if (MediaUtils.isValidImage(imageUri)) {
            int width = mediaModel.getWidth();
            int height = mediaModel.getHeight();

            // differentiating between tablet and phone
            float screenWidth;
            if (this.isInLayout()) {
                screenWidth = mLinearLayout.getMeasuredWidth();
            } else {
                screenWidth = getActivity().getResources().getDisplayMetrics().widthPixels;
            }
            float screenHeight = getActivity().getResources().getDisplayMetrics().heightPixels;

            if (width > screenWidth) {
                height = (int) (height / (width / screenWidth));
            } else if (height > screenHeight) {
                width = (int) (width / (height / screenHeight));
            }

            if (isLocal) {
                loadLocalImage(mLocalImageView, imageUri, width, height);
                mLocalImageView.setLayoutParams(new FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT, height));
            } else {
                mNetworkImageView.setImageUrl(imageUri + "?w=" + screenWidth, mImageLoader);
                mNetworkImageView.setLayoutParams(new FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT, height));
            }
        } else {
            mNetworkImageView.setVisibility(View.GONE);
            mLocalImageView.setVisibility(View.GONE);
        }
    }

    private void refreshViews(MediaModel mediaModel) {
        if (mediaModel == null) {
            mLinearLayout.setVisibility(View.GONE);
            return;
        }

        mLinearLayout.setVisibility(View.VISIBLE);

        mScrollView.scrollTo(0, 0);

        boolean isLocal = MediaUtils.isLocalFile(mediaModel.getUploadState());
        if (isLocal) {
            mNetworkImageView.setVisibility(View.GONE);
            mLocalImageView.setVisibility(View.VISIBLE);
        } else {
            mNetworkImageView.setVisibility(View.VISIBLE);
            mLocalImageView.setVisibility(View.GONE);
        }

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

        refreshImageView(mediaModel, isLocal);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        if (!isInLayout()) {
            inflater.inflate(R.menu.media_edit, menu);
        }
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        if (!isInLayout()) {
            menu.findItem(R.id.menu_new_media).setVisible(false);
            menu.findItem(R.id.menu_search).setVisible(false);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.menu_save_media) {
            item.setActionView(R.layout.progressbar);
            saveMedia();
        }
        return super.onOptionsItemSelected(item);
    }

    private synchronized void loadLocalImage(ImageView imageView, String filePath, int width, int height) {
        if (MediaUtils.isValidImage(filePath)) {
            imageView.setTag(filePath);

            Bitmap bitmap = WordPress.getBitmapCache().get(filePath);
            if (bitmap != null) {
                imageView.setImageBitmap(bitmap);
            } else {
                BitmapWorkerTask task = new BitmapWorkerTask(imageView, width, height, new BitmapWorkerCallback() {
                    @Override
                    public void onBitmapReady(String path, ImageView imageView, Bitmap bitmap) {
                        if (imageView != null) {
                            imageView.setImageBitmap(bitmap);
                        }
                        WordPress.getBitmapCache().put(path, bitmap);
                    }
                });
                task.execute(filePath);
            }
        }
    }

    public boolean isDirty() {
        return mLocalMediaId != MISSING_MEDIA_ID &&
                (!StringUtils.equals(mTitleOriginal, mTitleView.getText().toString())
                || !StringUtils.equals(mCaptionOriginal, mCaptionView.getText().toString())
                || !StringUtils.equals(mDescriptionOriginal, mDescriptionView.getText().toString()));
    }

    // FluxC events

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMediaChanged(OnMediaChanged event) {
        if (getActivity() != null) {
            getActivity().invalidateOptionsMenu();
            Toast.makeText(getActivity(), event.isError() ? R.string.media_edit_failure : R.string.media_edit_success,
                    Toast.LENGTH_LONG).show();
        }
        if (hasCallback()) {
            mCallback.onSavedEdit(mLocalMediaId, !event.isError());
        }
    }
}
