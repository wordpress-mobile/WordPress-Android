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
import android.database.Cursor;
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
import org.wordpress.android.WordPressDB;
import org.wordpress.android.models.Blog;
import org.wordpress.android.ui.reader.ReaderActivityLauncher;
import org.wordpress.android.ui.reader.ReaderActivityLauncher.PhotoViewerOption;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.DisplayUtils;
import org.wordpress.android.util.ImageUtils.BitmapWorkerCallback;
import org.wordpress.android.util.ImageUtils.BitmapWorkerTask;
import org.wordpress.android.util.MediaUtils;
import org.wordpress.android.util.SqlUtils;
import org.wordpress.android.util.StringUtils;
import org.wordpress.android.util.ToastUtils;
import org.wordpress.android.util.UrlUtils;
import org.wordpress.android.widgets.WPNetworkImageView;

import java.util.ArrayList;
import java.util.EnumSet;

/**
 * A fragment display a media item's details.
 */
public class MediaItemFragment extends Fragment {
    private static final String ARGS_MEDIA_ID = "media_id";

    public static final String TAG = MediaItemFragment.class.getName();

    private WPNetworkImageView mImageView;
    private TextView mCaptionView;
    private TextView mDescriptionView;
    private TextView mDateView;
    private TextView mFileNameView;
    private TextView mFileTypeView;
    private MediaItemFragmentCallback mCallback;

    private boolean mIsLocal;
    private String mImageUri;

    public interface MediaItemFragmentCallback {
        void onResume(Fragment fragment);
        void onPause(Fragment fragment);
    }

    public static MediaItemFragment newInstance(String mediaId) {
        MediaItemFragment fragment = new MediaItemFragment();

        Bundle args = new Bundle();
        args.putString(ARGS_MEDIA_ID, mediaId);
        fragment.setArguments(args);

        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
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
        loadMedia(getMediaId());
    }

    @Override
    public void onPause() {
        super.onPause();
        mCallback.onPause(this);
    }

    public String getMediaId() {
        if (getArguments() != null) {
            return getArguments().getString(ARGS_MEDIA_ID);
        } else {
            return null;
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
        loadMedia(null);
    }

    public void loadMedia(String mediaId) {
        Blog blog = WordPress.getCurrentBlog();

        if (blog != null) {
            String blogId = String.valueOf(blog.getLocalTableBlogId());

            Cursor cursor = null;
            try {
                // if the id is null, get the first media item in the database
                if (mediaId == null) {
                    cursor = WordPress.wpDB.getFirstMediaFileForBlog(blogId);
                } else {
                    cursor = WordPress.wpDB.getMediaFile(blogId, mediaId);
                }
                refreshViews(cursor);
            } finally {
                SqlUtils.closeCursor(cursor);
            }
        }
    }

    private void refreshViews(Cursor cursor) {
        if (!isAdded() || !cursor.moveToFirst()) {
            return;
        }

        // check whether or not to show the edit button
        String state = cursor.getString(cursor.getColumnIndex(WordPressDB.COLUMN_NAME_UPLOAD_STATE));
        mIsLocal = MediaUtils.isLocalFile(state);
        if (mIsLocal && getActivity() != null) {
            getActivity().invalidateOptionsMenu();
        }

        String caption = cursor.getString(cursor.getColumnIndex(WordPressDB.COLUMN_NAME_CAPTION));
        if (TextUtils.isEmpty(caption)) {
            mCaptionView.setVisibility(View.GONE);
        } else {
            mCaptionView.setText(caption);
            mCaptionView.setVisibility(View.VISIBLE);
        }

        String desc = cursor.getString(cursor.getColumnIndex(WordPressDB.COLUMN_NAME_DESCRIPTION));
        if (TextUtils.isEmpty(desc)) {
            mDescriptionView.setVisibility(View.GONE);
        } else {
            mDescriptionView.setText(desc);
            mDescriptionView.setVisibility(View.VISIBLE);
        }

        String date = MediaUtils.getDate(cursor.getLong(cursor.getColumnIndex(WordPressDB.COLUMN_NAME_DATE_CREATED_GMT)));
        mDateView.setText(date);
        TextView txtDateLabel = (TextView) getView().findViewById(R.id.media_listitem_details_date_label);
        txtDateLabel.setText(
                mIsLocal ? R.string.media_details_label_date_added : R.string.media_details_label_date_uploaded);

        String fileURL = cursor.getString(cursor.getColumnIndex(WordPressDB.COLUMN_NAME_FILE_URL));
        String fileName = cursor.getString(cursor.getColumnIndex(WordPressDB.COLUMN_NAME_FILE_NAME));
        mImageUri = TextUtils.isEmpty(fileURL)
                ? cursor.getString(cursor.getColumnIndex(WordPressDB.COLUMN_NAME_FILE_PATH))
                : fileURL;
        boolean isValidImage = MediaUtils.isValidImage(mImageUri);

        mFileNameView.setText(fileName);

        float mediaWidth = cursor.getInt(cursor.getColumnIndex(WordPressDB.COLUMN_NAME_WIDTH));
        float mediaHeight = cursor.getInt(cursor.getColumnIndex(WordPressDB.COLUMN_NAME_HEIGHT));

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
                final String filePath = cursor.getString(cursor.getColumnIndex(WordPressDB.COLUMN_NAME_FILE_PATH));
                loadLocalImage(mImageView, filePath, imageWidth, imageHeight);
            } else {
                // Allow non-private wp.com and Jetpack blogs to use photon to get a higher res thumbnail
                String thumbnailURL;
                if (WordPress.getCurrentBlog() != null && WordPress.getCurrentBlog().isPhotonCapable()){
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

        // enable fullscreen photo for non-local
        if (!mIsLocal && isValidImage) {
            mImageView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Blog blog = WordPress.getCurrentBlog();
                    boolean isPrivate = blog != null && blog.isPrivate();
                    EnumSet<PhotoViewerOption> imageOptions = EnumSet.noneOf(PhotoViewerOption.class);
                    if (isPrivate) {
                        imageOptions.add(PhotoViewerOption.IS_PRIVATE_IMAGE);
                    }
                    ReaderActivityLauncher.showReaderPhotoViewer(
                            v.getContext(), mImageUri, imageOptions);
                }
            });
        }
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

        menu.findItem(R.id.menu_edit_media).setVisible(
                !mIsLocal && WordPressMediaUtils.isWordPressVersionWithMediaEditingCapabilities());

        menu.findItem(R.id.menu_copy_media_url).setVisible(!mIsLocal && !TextUtils.isEmpty(mImageUri));
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();

        if (itemId == R.id.menu_delete) {
            String blogId = String.valueOf(WordPress.getCurrentBlog().getLocalTableBlogId());
            boolean canDeleteMedia = WordPressMediaUtils.canDeleteMedia(blogId, getMediaId());
            if (!canDeleteMedia) {
                Toast.makeText(getActivity(), R.string.wait_until_upload_completes, Toast.LENGTH_LONG).show();
                return true;
            }

            Builder builder = new AlertDialog.Builder(getActivity()).setMessage(R.string.confirm_delete_media)
                    .setCancelable(true).setPositiveButton(
                            R.string.delete, new OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    ArrayList<String> ids = new ArrayList<>(1);
                                    ids.add(getMediaId());
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
