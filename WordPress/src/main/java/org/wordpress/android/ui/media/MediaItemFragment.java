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
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.Html;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.ViewStub;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.NetworkImageView;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.WordPressDB;
import org.wordpress.android.models.Blog;
import org.wordpress.android.ui.reader.ReaderActivityLauncher;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.ImageUtils.BitmapWorkerCallback;
import org.wordpress.android.util.ImageUtils.BitmapWorkerTask;
import org.wordpress.android.util.MediaUtils;
import org.wordpress.android.util.StringUtils;
import org.wordpress.android.util.ToastUtils;

import java.util.ArrayList;

/**
 * A fragment display a media item's details.
 */
public class MediaItemFragment extends Fragment {
    private static final String ARGS_MEDIA_ID = "media_id";

    public static final String TAG = MediaItemFragment.class.getName();

    private View mView;

    private ImageView mImageView;
    private TextView mCaptionView;
    private TextView mDescriptionView;
    private TextView mDateView;
    private TextView mFileNameView;
    private TextView mFileTypeView;
    private ImageView mImageCopy;
    private MediaItemFragmentCallback mCallback;
    private ImageLoader mImageLoader;
    private ProgressBar mProgressView;

    private boolean mIsLocal;

    public interface MediaItemFragmentCallback {
        public void onResume(Fragment fragment);
        public void onPause(Fragment fragment);
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
        mImageLoader = MediaImageLoader.getInstance();
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
    }

    @Override
    public void onPause() {
        super.onPause();
        mCallback.onPause(this);
    }

    public String getMediaId() {
        if (getArguments() != null)
            return getArguments().getString(ARGS_MEDIA_ID);
        else
            return null;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mView = inflater.inflate(R.layout.media_listitem_details, container, false);

        mCaptionView = (TextView) mView.findViewById(R.id.media_listitem_details_caption);
        mDescriptionView = (TextView) mView.findViewById(R.id.media_listitem_details_description);
        mDateView = (TextView) mView.findViewById(R.id.media_listitem_details_date);
        mFileNameView = (TextView) mView.findViewById(R.id.media_listitem_details_file_name);
        mFileTypeView = (TextView) mView.findViewById(R.id.media_listitem_details_file_type);
        mProgressView = (ProgressBar) mView.findViewById(R.id.media_listitem_details_progress);
        mImageCopy = (ImageView) mView.findViewById(R.id.image_copy);

        loadMedia(getMediaId());

        return mView;
    }

    /** Loads the first media item for the current blog from the database **/
    public void loadDefaultMedia() {
        loadMedia(null);
    }

    public void loadMedia(String mediaId) {
        Blog blog = WordPress.getCurrentBlog();

        if (blog != null) {
            String blogId = String.valueOf(blog.getLocalTableBlogId());

            Cursor cursor;

            // if the id is null, get the first media item in the database
            if (mediaId == null) {
                cursor = WordPress.wpDB.getFirstMediaFileForBlog(blogId);
            } else {
                cursor = WordPress.wpDB.getMediaFile(blogId, mediaId);
            }

            refreshViews(cursor);
            cursor.close();
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
        if (mIsLocal) {
            mDateView.setText(getResources().getString(R.string.media_details_added_on) + " " + date);
        } else {
            mDateView.setText(getResources().getString(R.string.media_details_uploaded_on) + " " + date);
        }

        final String fileURL = cursor.getString(cursor.getColumnIndex(WordPressDB.COLUMN_NAME_FILE_URL));
        String fileName = cursor.getString(cursor.getColumnIndex(WordPressDB.COLUMN_NAME_FILE_NAME));
        final String imageUri = TextUtils.isEmpty(fileURL)
                ? cursor.getString(cursor.getColumnIndex(WordPressDB.COLUMN_NAME_FILE_PATH))
                : fileURL;

        // hyperlink to image url unless local
        if (mIsLocal || TextUtils.isEmpty(fileURL)) {
            mFileNameView.setText(fileName);
        } else {
            String link = String.format("<a href='%1$s'>%2$s</a>", fileURL, fileName);
            mFileNameView.setText(Html.fromHtml(link));
            mFileNameView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ReaderActivityLauncher.openUrl(v.getContext(), fileURL);
                }
            });
        }

        inflateImageView();

        // image and dimensions
        String dimensions = null;
        if (MediaUtils.isValidImage(imageUri)) {
            int mediaWidth = cursor.getInt(cursor.getColumnIndex(WordPressDB.COLUMN_NAME_WIDTH));
            int mediaHeight = cursor.getInt(cursor.getColumnIndex(WordPressDB.COLUMN_NAME_HEIGHT));
            if (mediaWidth > 0 && mediaHeight > 0) {
                dimensions = mediaWidth + " x " + mediaHeight;
            }

            float screenWidth;
            if (this.isInLayout()) {
                screenWidth =  ((View) mImageView.getParent()).getMeasuredWidth();
            } else {
                screenWidth = getActivity().getResources().getDisplayMetrics().widthPixels;
            }
            float screenHeight = getActivity().getResources().getDisplayMetrics().heightPixels;

            if (mediaWidth > screenWidth) {
                mediaHeight = (int) (mediaHeight / (mediaWidth/screenWidth));
                mediaWidth = (int) screenWidth;
            } else if (mediaHeight > screenHeight) {
                mediaWidth = (int) (mediaWidth / (mediaHeight/screenHeight));
                mediaHeight = (int) screenHeight;
            }

            if (mIsLocal) {
                final String filePath = cursor.getString(cursor.getColumnIndex(WordPressDB.COLUMN_NAME_FILE_PATH));
                loadLocalImage(mImageView, filePath, mediaWidth, mediaHeight);
            } else {
                // Allow non-private wp.com and Jetpack blogs to use photon to get a higher res thumbnail
                if (WordPress.getCurrentBlog() != null && WordPress.getCurrentBlog().isPhotonCapable()){
                    String thumbnailURL = StringUtils.getPhotonUrl(imageUri, (int) screenWidth);
                    ((NetworkImageView) mImageView).setImageUrl(thumbnailURL, mImageLoader);
                } else {
                    ((NetworkImageView) mImageView).setImageUrl(imageUri + "?w=" + screenWidth, mImageLoader);
                }
            }
            mImageView.setVisibility(View.VISIBLE);
            mImageView.setLayoutParams(new RelativeLayout.LayoutParams(LayoutParams.MATCH_PARENT, mediaHeight));
        } else {
            mImageView.setVisibility(View.GONE);
            mProgressView.setVisibility(View.GONE);
        }

        // show dimens & file ext together
        String fileExt = TextUtils.isEmpty(fileURL) ? null : fileURL.replaceAll(".*\\.(\\w+)$", "$1").toUpperCase();
        boolean hasDimens = !TextUtils.isEmpty(dimensions);
        boolean hasExt = !TextUtils.isEmpty(fileExt);
        if (hasDimens & hasExt) {
            mFileTypeView.setText(dimensions + " " + fileExt);
            mFileTypeView.setVisibility(View.VISIBLE);
        } else if (hasDimens) {
            mFileTypeView.setText(dimensions);
            mFileTypeView.setVisibility(View.VISIBLE);
        } else if (hasExt) {
            mFileTypeView.setText(fileExt);
            mFileTypeView.setVisibility(View.VISIBLE);
        } else {
            mFileTypeView.setVisibility(View.GONE);
        }

        // enable fullscreen photo and copy url to clipboard for non-local
        if (!mIsLocal && !TextUtils.isEmpty(imageUri)) {
            mImageView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ReaderActivityLauncher.showReaderPhotoViewer(
                            v.getContext(), imageUri, WordPress.getCurrentBlog().isPrivate());
                }
            });
            mImageCopy.setVisibility(View.VISIBLE);
            mImageCopy.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    copyUrlToClipboard(imageUri);
                }
            });
        } else {
            mImageCopy.setVisibility(View.GONE);
            mImageView.setOnClickListener(null);
        }
    }

    private void copyUrlToClipboard(String imageUri) {
        try {
            ClipboardManager clipboard = (ClipboardManager) getActivity().getSystemService(Context.CLIPBOARD_SERVICE);
            clipboard.setPrimaryClip(ClipData.newPlainText(imageUri, imageUri));
            ToastUtils.showToast(getActivity(), R.string.media_details_copy_url_toast);
        } catch (Exception e) {
            AppLog.e(AppLog.T.UTILS, e);
            ToastUtils.showToast(getActivity(), R.string.error_copy_to_clipboard);
        }
    }

    private void inflateImageView() {
        ViewStub viewStub = (ViewStub) mView.findViewById(R.id.media_listitem_details_stub);
        if (viewStub != null) {
            if (mIsLocal)
                viewStub.setLayoutResource(R.layout.media_grid_image_local);
            else
                viewStub.setLayoutResource(R.layout.media_grid_image_network);
            viewStub.inflate();
        }

        mImageView = (ImageView) mView.findViewById(R.id.media_listitem_details_image);

        // add a background color so something appears while image is downloaded - note that this
        // must be translucent so progress bar appears beneath it
        mProgressView.setVisibility(View.VISIBLE);
        mImageView.setImageDrawable(new ColorDrawable(getResources().getColor(R.color.translucent_grey_lighten_20)));
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

        if (mIsLocal || !WordPressMediaUtils.isWordPressVersionWithMediaEditingCapabilities()) {
            menu.findItem(R.id.menu_edit_media).setVisible(false);
        }
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
                                    ArrayList<String> ids = new ArrayList<String>(1);
                                    ids.add(getMediaId());
                                    if (getActivity() instanceof MediaBrowserActivity) {
                                        ((MediaBrowserActivity) getActivity()).deleteMedia(ids);
                                    }
                                }
                            }).setNegativeButton(R.string.cancel, null);
            AlertDialog dialog = builder.create();
            dialog.show();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
