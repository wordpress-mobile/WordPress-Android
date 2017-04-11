package org.wordpress.android.ui.media;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Fragment;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.fluxc.model.MediaModel;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.store.MediaStore;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.DateTimeUtils;
import org.wordpress.android.util.DisplayUtils;
import org.wordpress.android.util.ImageUtils.BitmapWorkerCallback;
import org.wordpress.android.util.ImageUtils.BitmapWorkerTask;
import org.wordpress.android.util.MediaUtils;
import org.wordpress.android.util.SiteUtils;
import org.wordpress.android.util.StringUtils;
import org.wordpress.android.util.ToastUtils;
import org.wordpress.android.util.UrlUtils;
import org.wordpress.android.widgets.WPNetworkImageView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.inject.Inject;

/**
 * A fragment display a media item's details.
 */
public class MediaItemFragment extends Fragment {
    private static final String ARGS_MEDIA_ID = "media_id";
    private static final int MISSING_MEDIA_ID = -1;

    public static final String TAG = MediaItemFragment.class.getName();

    @Inject MediaStore mMediaStore;

    private WPNetworkImageView mImageView;
    private TextView mCaptionView;
    private TextView mDescriptionView;
    private TextView mDateView;
    private TextView mFileNameView;
    private TextView mFileTypeView;
    private MediaItemFragmentCallback mCallback;

    private boolean mIsLocal;
    private String mImageUri;

    private SiteModel mSite;

    public interface MediaItemFragmentCallback {
        void onResume(Fragment fragment);
        void onPause(Fragment fragment);
    }

    public static MediaItemFragment newInstance(SiteModel site, int localMediaId) {
        MediaItemFragment fragment = new MediaItemFragment();
        Bundle args = new Bundle();
        args.putInt(ARGS_MEDIA_ID, localMediaId);
        args.putSerializable(WordPress.SITE, site);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
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
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putSerializable(WordPress.SITE, mSite);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        try {
            mCallback = (MediaItemFragmentCallback) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement MediaItemFragmentCallback");
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        mCallback.onResume(this);
        loadMedia(getLocalMediaId());
    }

    @Override
    public void onPause() {
        super.onPause();
        mCallback.onPause(this);
    }

    public int getLocalMediaId() {
        if (getArguments() != null) {
            return getArguments().getInt(ARGS_MEDIA_ID);
        } else {
            return MISSING_MEDIA_ID;
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.media_listitem_details, container, false);

        mCaptionView = (TextView) view.findViewById(R.id.media_listitem_details_caption);
        mDescriptionView = (TextView) view.findViewById(R.id.media_listitem_details_description);
        mDateView = (TextView) view.findViewById(R.id.media_listitem_details_date);
        mFileNameView = (TextView) view.findViewById(R.id.media_listitem_details_file_name);
        mFileTypeView = (TextView) view.findViewById(R.id.media_listitem_details_file_type);
        mImageView = (WPNetworkImageView) view.findViewById(R.id.media_listitem_details_image);

        return view;
    }

    /** Loads the first media item for the current blog from the database **/
    public void loadDefaultMedia() {
        loadMedia(MISSING_MEDIA_ID);
    }

    public void loadMedia(int localMediaId) {
        if (mSite == null)
            return;

        MediaModel mediaModel = null;
        if (localMediaId != MISSING_MEDIA_ID) {
            mediaModel = mMediaStore.getMediaWithLocalId(localMediaId);
        }

        // if the id is null, get the first media item in the database
        if (mediaModel == null) {
            List<MediaModel> list = mMediaStore.getAllSiteMedia(mSite);
            if (list != null && list.size() > 0) {
                mediaModel = list.get(0);
            }
        }
        refreshViews(mediaModel);
    }

    private void refreshViews(final MediaModel mediaModel) {
        if (!isAdded() || mediaModel == null) {
            return;
        }

        // check whether or not to show the edit button
        mIsLocal = MediaUtils.isLocalFile(mediaModel.getUploadState());
        if (mIsLocal && getActivity() != null) {
            getActivity().invalidateOptionsMenu();
        }

        String caption = mediaModel.getCaption();
        if (TextUtils.isEmpty(caption)) {
            mCaptionView.setVisibility(View.GONE);
        } else {
            mCaptionView.setText(caption);
            mCaptionView.setVisibility(View.VISIBLE);
        }

        String desc = mediaModel.getDescription();
        if (TextUtils.isEmpty(desc)) {
            mDescriptionView.setVisibility(View.GONE);
        } else {
            mDescriptionView.setText(desc);
            mDescriptionView.setVisibility(View.VISIBLE);
        }

        mDateView.setText(getDisplayDate(mediaModel.getUploadDate()));

        if (getView() != null) {
            TextView txtDateLabel = (TextView) getView().findViewById(R.id.media_listitem_details_date_label);
            txtDateLabel.setText(
                    mIsLocal ? R.string.media_details_label_date_added : R.string.media_details_label_date_uploaded);
        }

        String fileURL = mediaModel.getUrl();
        String fileName = mediaModel.getFileName();
        mImageUri = TextUtils.isEmpty(fileURL)
                ? mediaModel.getFilePath()
                : fileURL;
        boolean isValidImage = MediaUtils.isValidImage(mImageUri);

        mFileNameView.setText(fileName);

        float mediaWidth = mediaModel.getWidth();
        float mediaHeight = mediaModel.getHeight();

        // image and dimensions
        if (isValidImage) {
            int screenWidth = DisplayUtils.getDisplayPixelWidth(getActivity());
            int screenHeight = DisplayUtils.getDisplayPixelHeight(getActivity());

            // determine size for display
            int imageWidth;
            int imageHeight;
            boolean isFullWidth;
            if (mediaWidth == 0 || mediaHeight == 0) {
                imageWidth = screenWidth;
                imageHeight = screenHeight / 2;
                isFullWidth = true;
            } else if (mediaWidth > mediaHeight) {
                float ratio = mediaHeight / mediaWidth;
                imageWidth = Math.min(screenWidth, (int) mediaWidth);
                imageHeight = (int) (imageWidth * ratio);
                isFullWidth = (imageWidth == screenWidth);
            } else {
                float ratio = mediaWidth / mediaHeight;
                imageHeight = Math.min(screenHeight / 2, (int) mediaHeight);
                imageWidth = (int) (imageHeight * ratio);
                isFullWidth = false;
            }

            // set the imageView's parent height to match the image so it takes up space while
            // the image is loading
            FrameLayout frameView = (FrameLayout) getView().findViewById(R.id.layout_image_frame);
            frameView.setLayoutParams(
                    new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, imageHeight));

            // add padding to the frame if the image isn't full-width
            if (!isFullWidth) {
                int hpadding = getResources().getDimensionPixelSize(R.dimen.content_margin);
                int vpadding = getResources().getDimensionPixelSize(R.dimen.margin_extra_large);
                frameView.setPadding(hpadding, vpadding, hpadding, vpadding);
            }

            if (mIsLocal) {
                final String filePath = mediaModel.getFilePath();
                loadLocalImage(mImageView, filePath, imageWidth, imageHeight);
            } else {
                // Allow non-private wp.com and Jetpack blogs to use photon to get a higher res thumbnail
                String thumbnailURL;
                if (SiteUtils.isPhotonCapable(mSite)) {
                    thumbnailURL = StringUtils.getPhotonUrl(mImageUri, imageWidth);
                } else {
                    thumbnailURL = UrlUtils.removeQuery(mImageUri) + "?w=" + imageWidth;
                }
                mImageView.setImageUrl(thumbnailURL, WPNetworkImageView.ImageType.PHOTO);
            }
        } else {
            // not an image so show placeholder icon
            int placeholderResId = WordPressMediaUtils.getPlaceholder(mImageUri);
            mImageView.setDefaultImageResId(placeholderResId);
            mImageView.showDefaultImage();
        }

        // show dimens & file ext together
        String dimens =
                (mediaWidth > 0 && mediaHeight > 0) ? (int) mediaWidth + " x " + (int) mediaHeight : null;
        String fileExt =
                TextUtils.isEmpty(fileURL) ? null : fileURL.replaceAll(".*\\.(\\w+)$", "$1").toUpperCase();
        boolean hasDimens = !TextUtils.isEmpty(dimens);
        boolean hasExt = !TextUtils.isEmpty(fileExt);
        if (hasDimens & hasExt) {
            mFileTypeView.setText(fileExt + ", " + dimens);
            mFileTypeView.setVisibility(View.VISIBLE);
        } else if (hasExt) {
            mFileTypeView.setText(fileExt);
            mFileTypeView.setVisibility(View.VISIBLE);
        } else {
            mFileTypeView.setVisibility(View.GONE);
        }

        // enable fullscreen preview
        if (isValidImage) {
            mImageView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    MediaPreviewActivity.showPreview(
                            getActivity(),
                            v,
                            mediaModel.getId(),
                            mediaModel.isVideo());
                }
            });
        }
    }

    /*
     * returns the passed string formatted as a short date if it's valid ISO 8601 date,
     * otherwise returns the passed string
     */
    private String getDisplayDate(String dateString) {
        if (dateString != null) {
            Date date = DateTimeUtils.dateFromIso8601(dateString);
            if (date != null) {
                return SimpleDateFormat.getDateInstance().format(date);
            }
        }
        return dateString;
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
                        imageView.setImageBitmap(bitmap);
                        WordPress.getBitmapCache().put(path, bitmap);
                    }
                });
                task.execute(filePath);
            }
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.media_details, menu);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        menu.findItem(R.id.menu_new_media).setVisible(false);
        menu.findItem(R.id.menu_search).setVisible(false);
        menu.findItem(R.id.menu_edit_media).setVisible(!mIsLocal);
        menu.findItem(R.id.menu_copy_media_url).setVisible(!mIsLocal && !TextUtils.isEmpty(mImageUri));
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();

        if (itemId == R.id.menu_delete) {
            MediaModel mediaModel = mMediaStore.getMediaWithLocalId(getLocalMediaId());
            if (mediaModel == null) {
                return true;
            }
            boolean canDeleteMedia = WordPressMediaUtils.canDeleteMedia(mediaModel);
            if (!canDeleteMedia) {
                Toast.makeText(getActivity(), R.string.wait_until_upload_completes, Toast.LENGTH_LONG).show();
                return true;
            }

            Builder builder = new AlertDialog.Builder(getActivity()).setMessage(R.string.confirm_delete_media)
                    .setCancelable(true).setPositiveButton(
                            R.string.delete, new OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    ArrayList<Integer> ids = new ArrayList<>(1);
                                    ids.add(getLocalMediaId());
                                    if (getActivity() instanceof MediaBrowserActivity) {
                                        ((MediaBrowserActivity) getActivity()).deleteMedia(ids);
                                    }
                                }
                            }).setNegativeButton(R.string.cancel, null);
            AlertDialog dialog = builder.create();
            dialog.show();
            return true;
        } else if (itemId == R.id.menu_copy_media_url) {
            copyUrlToClipboard();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void copyUrlToClipboard() {
        try {
            ClipboardManager clipboard = (ClipboardManager) getActivity().getSystemService(Context.CLIPBOARD_SERVICE);
            clipboard.setPrimaryClip(ClipData.newPlainText(mImageUri, mImageUri));
            ToastUtils.showToast(getActivity(), R.string.media_details_copy_url_toast);
        } catch (Exception e) {
            AppLog.e(AppLog.T.UTILS, e);
            ToastUtils.showToast(getActivity(), R.string.error_copy_to_clipboard);
        }
    }
}
