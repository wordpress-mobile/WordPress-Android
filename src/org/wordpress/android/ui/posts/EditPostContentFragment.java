package org.wordpress.android.ui.posts;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.text.Editable;
import android.text.Layout;
import android.text.Selection;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.method.ArrowKeyMovementMethod;
import android.text.style.AlignmentSpan;
import android.text.style.CharacterStyle;
import android.text.style.QuoteSpan;
import android.text.style.StrikethroughSpan;
import android.text.style.StyleSpan;
import android.text.style.URLSpan;
import android.view.ContextMenu;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.DecelerateInterpolator;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockFragment;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageLoader;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.models.Blog;
import org.wordpress.android.models.MediaFile;
import org.wordpress.android.models.MediaGallery;
import org.wordpress.android.models.Post;
import org.wordpress.android.ui.media.MediaGalleryActivity;
import org.wordpress.android.ui.media.MediaGalleryPickerActivity;
import org.wordpress.android.ui.media.MediaUtils;
import org.wordpress.android.util.DeviceUtils;
import org.wordpress.android.util.ImageHelper;
import org.wordpress.android.util.MediaGalleryImageSpan;
import org.wordpress.android.util.StringUtils;
import org.wordpress.android.util.WPEditText;
import org.wordpress.android.util.WPHtml;
import org.wordpress.android.util.WPImageSpan;
import org.wordpress.android.util.WPUnderlineSpan;
import org.wordpress.passcodelock.AppLockManager;
import org.xmlrpc.android.ApiHelper;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by dan on 11/20/13.
 */
public class EditPostContentFragment extends SherlockFragment implements TextWatcher,
        WPEditText.OnSelectionChangedListener, View.OnTouchListener {

    NewEditPostActivity mActivity;

    private static final int ACTIVITY_REQUEST_CODE_CREATE_LINK = 4;
    public static final String NEW_MEDIA_POST_EXTRA = "NEW_MEDIA_POST_ID";

    private Post mPost;

    private WPEditText mContentEditText;
    private ImageButton mAddPictureButton;
    private EditText mTitleEditText;
    private ToggleButton mBoldToggleButton, mEmToggleButton, mBquoteToggleButton;
    private ToggleButton mUnderlineToggleButton, mStrikeToggleButton;
    private Button mLinkButton, mMoreButton;
    private RelativeLayout mFormatBar;
    private boolean mIsBackspace;
    private boolean mScrollDetected;

    private String mMediaCapturePath = "";

    private int mStyleStart, mSelectionStart, mSelectionEnd;
    private int mLastPosition = -1;
    private int mCurrentActivityRequest = -1;

    private float mLastYPos = 0;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mActivity = (NewEditPostActivity)getActivity();

        ViewGroup rootView = (ViewGroup) inflater
                .inflate(R.layout.fragment_edit_post_content, container, false);

        mFormatBar = (RelativeLayout) rootView.findViewById(R.id.format_bar);
        mTitleEditText = (EditText)rootView.findViewById(R.id.post_title);
        mContentEditText = (WPEditText)rootView.findViewById(R.id.post_content);
        mBoldToggleButton = (ToggleButton) rootView.findViewById(R.id.bold);
        mEmToggleButton = (ToggleButton) rootView.findViewById(R.id.em);
        mBquoteToggleButton = (ToggleButton) rootView.findViewById(R.id.bquote);
        mUnderlineToggleButton = (ToggleButton) rootView.findViewById(R.id.underline);
        mStrikeToggleButton = (ToggleButton) rootView.findViewById(R.id.strike);
        mAddPictureButton = (ImageButton) rootView.findViewById(R.id.addPictureButton);
        mLinkButton = (Button) rootView.findViewById(R.id.link);
        mMoreButton = (Button) rootView.findViewById(R.id.more);

        registerForContextMenu(mAddPictureButton);
        mContentEditText.setOnSelectionChangedListener(this);
        mContentEditText.setOnTouchListener(this);
        mContentEditText.addTextChangedListener(this);
        mAddPictureButton.setOnClickListener(mFormatBarButtonClickListener);
        mBoldToggleButton.setOnClickListener(mFormatBarButtonClickListener);
        mLinkButton.setOnClickListener(mFormatBarButtonClickListener);
        mEmToggleButton.setOnClickListener(mFormatBarButtonClickListener);
        mUnderlineToggleButton.setOnClickListener(mFormatBarButtonClickListener);
        mStrikeToggleButton.setOnClickListener(mFormatBarButtonClickListener);
        mBquoteToggleButton.setOnClickListener(mFormatBarButtonClickListener);
        mMoreButton.setOnClickListener(mFormatBarButtonClickListener);

        mPost = mActivity.getPost();
        if (mPost != null) {
            if (!TextUtils.isEmpty(mPost.getContent())) {
                if (mPost.isLocalDraft())
                    mContentEditText.setText(WPHtml.fromHtml(mPost.getContent().replaceAll("\uFFFC", ""), mActivity, mPost));
                else
                    mContentEditText.setText(mPost.getContent().replaceAll("\uFFFC", ""));
            }
            if (!TextUtils.isEmpty(mPost.getTitle())) {
                mTitleEditText.setText(mPost.getTitle());
            }
        }

        mContentEditText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (mPost != null && hasFocus) {
                    setContentEditingModeVisible(true);
                }
            }
        });

        setContentEditTextFocusable(true);

        return rootView;
    }

    public void setContentEditTextFocusable(boolean canFocus) {
        if (mContentEditText != null) {
            mContentEditText.setFocusableInTouchMode(canFocus);
            mContentEditText.setFocusable(canFocus);
        }
    }

    private void setContentEditingModeVisible(boolean isVisible) {
        if (mActivity == null)
            return;
        ActionBar actionBar = mActivity.getSupportActionBar();
        if (isVisible) {

            //actionBar.hide();

            actionBar.setDisplayShowHomeEnabled(false);
            actionBar.setDisplayShowTitleEnabled(false);
            actionBar.setDisplayShowCustomEnabled(true);
            ViewGroup actionBarLayout = (ViewGroup) mActivity.getLayoutInflater().inflate(R.layout.edit_post_content_action_bar, null);
            actionBar.setCustomView(actionBarLayout);
            Button doneButton = (Button) mActivity.findViewById(R.id.action_bar_done);
            doneButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    setContentEditingModeVisible(false);
                }
            });

            actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
            mTitleEditText.setVisibility(View.GONE);

            mActivity.supportInvalidateOptionsMenu();
            //actionBar.show();

            Animation fadeIn = new AlphaAnimation(0, 1);
            fadeIn.setInterpolator(new DecelerateInterpolator()); //add this
            fadeIn.setDuration(300);
            fadeIn.setStartOffset(300);
            mFormatBar.setVisibility(View.VISIBLE);
            mFormatBar.startAnimation(fadeIn);

        } else {
            //actionBar.hide();

            actionBar.setDisplayShowHomeEnabled(true);
            actionBar.setDisplayShowTitleEnabled(true);
            actionBar.setDisplayShowCustomEnabled(false);
            actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
            mTitleEditText.setVisibility(View.VISIBLE);
            mFormatBar.setVisibility(View.GONE);
            mActivity.supportInvalidateOptionsMenu();

            //actionBar.show();
        }
    }

    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        menu.add(0, 0, 0, getResources().getText(R.string.select_photo));
        if (DeviceUtils.getInstance().hasCamera(getActivity())) {
            menu.add(0, 1, 0, getResources().getText(R.string.media_add_popup_capture_photo));
        }
        menu.add(0, 2, 0, getResources().getText(R.string.select_video));
        if (DeviceUtils.getInstance().hasCamera(getActivity())) {
            menu.add(0, 3, 0, getResources().getText(R.string.media_add_popup_capture_video));
        }

        menu.add(0, 4, 0, getResources().getText(R.string.media_add_new_media_gallery));
        menu.add(0, 5, 0, getResources().getText(R.string.select_from_media_library));
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (data != null || ((requestCode == MediaUtils.RequestCode.ACTIVITY_REQUEST_CODE_TAKE_PHOTO || requestCode == MediaUtils.RequestCode.ACTIVITY_REQUEST_CODE_TAKE_VIDEO))) {
            Bundle extras;

            switch (requestCode) {
                case MediaGalleryActivity.REQUEST_CODE:
                    if (resultCode == Activity.RESULT_OK) {
                        handleMediaGalleryResult(data);
                    }
                    break;
                case MediaGalleryPickerActivity.REQUEST_CODE:
                    if (resultCode == Activity.RESULT_OK) {
                        handleMediaGalleryPickerResult(data);
                    }
                    break;
                case MediaUtils.RequestCode.ACTIVITY_REQUEST_CODE_PICTURE_LIBRARY:
                    Uri imageUri = data.getData();
                    verifyImage(imageUri);
                    break;
                case MediaUtils.RequestCode.ACTIVITY_REQUEST_CODE_TAKE_PHOTO:
                    if (resultCode == Activity.RESULT_OK) {
                        try {
                            File f = new File(mMediaCapturePath);
                            Uri capturedImageUri = Uri.fromFile(f);
                            f = null;
                            if (!addMedia(capturedImageUri, null))
                                Toast.makeText(getActivity(), getResources().getText(R.string.gallery_error), Toast.LENGTH_SHORT).show();
                            getActivity().sendBroadcast(new Intent(Intent.ACTION_MEDIA_MOUNTED, Uri.parse("file://"
                                    + Environment.getExternalStorageDirectory())));
                        } catch (Exception e) {
                            e.printStackTrace();
                        } catch (OutOfMemoryError e) {
                            e.printStackTrace();
                        }
                    }
                    break;
                case MediaUtils.RequestCode.ACTIVITY_REQUEST_CODE_VIDEO_LIBRARY:
                    Uri videoUri = data.getData();
                    if (!addMedia(videoUri, null))
                        Toast.makeText(getActivity(), getResources().getText(R.string.gallery_error), Toast.LENGTH_SHORT).show();
                    break;
                case MediaUtils.RequestCode.ACTIVITY_REQUEST_CODE_TAKE_VIDEO:
                    if (resultCode == Activity.RESULT_OK) {
                        Uri capturedVideoUri = MediaUtils.getLastRecordedVideoUri(getActivity());
                        if (!addMedia(capturedVideoUri, null))
                            Toast.makeText(getActivity(), getResources().getText(R.string.gallery_error), Toast.LENGTH_SHORT).show();
                    }
                    break;
                case ACTIVITY_REQUEST_CODE_CREATE_LINK:
                    try {
                        extras = data.getExtras();
                        String linkURL = extras.getString("linkURL");
                        if (!linkURL.equals("http://") && !linkURL.equals("")) {

                            if (mSelectionStart > mSelectionEnd) {
                                int temp = mSelectionEnd;
                                mSelectionEnd = mSelectionStart;
                                mSelectionStart = temp;
                            }
                            Editable str = mContentEditText.getText();
                            if (mPost.isLocalDraft()) {
                                if (extras.getString("linkText") == null) {
                                    if (mSelectionStart < mSelectionEnd)
                                        str.delete(mSelectionStart, mSelectionEnd);
                                    str.insert(mSelectionStart, linkURL);
                                    str.setSpan(new URLSpan(linkURL), mSelectionStart, mSelectionStart + linkURL.length(),
                                            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                                    mContentEditText.setSelection(mSelectionStart + linkURL.length());
                                } else {
                                    String linkText = extras.getString("linkText");
                                    if (mSelectionStart < mSelectionEnd)
                                        str.delete(mSelectionStart, mSelectionEnd);
                                    str.insert(mSelectionStart, linkText);
                                    str.setSpan(new URLSpan(linkURL), mSelectionStart, mSelectionStart + linkText.length(),
                                            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                                    mContentEditText.setSelection(mSelectionStart + linkText.length());
                                }
                            } else {
                                if (extras.getString("linkText") == null) {
                                    if (mSelectionStart < mSelectionEnd)
                                        str.delete(mSelectionStart, mSelectionEnd);
                                    String urlHTML = "<a href=\"" + linkURL + "\">" + linkURL + "</a>";
                                    str.insert(mSelectionStart, urlHTML);
                                    mContentEditText.setSelection(mSelectionStart + urlHTML.length());
                                } else {
                                    String linkText = extras.getString("linkText");
                                    if (mSelectionStart < mSelectionEnd)
                                        str.delete(mSelectionStart, mSelectionEnd);
                                    String urlHTML = "<a href=\"" + linkURL + "\">" + linkText + "</a>";
                                    str.insert(mSelectionStart, urlHTML);
                                    mContentEditText.setSelection(mSelectionStart + urlHTML.length());
                                }
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    break;
            }
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case 0:
                launchPictureLibrary();
                return true;
            case 1:
                launchCamera();
                return true;
            case 2:
                launchVideoLibrary();
                return true;
            case 3:
                launchVideoCamera();
                return true;
            case 4:
                startMediaGalleryActivity(null);
                return true;
            case 5:
                startMediaGalleryAddActivity();
                return true;
        }
        return false;
    }

    /**
     * Media
     */

    private void launchPictureLibrary() {
        mCurrentActivityRequest = MediaUtils.RequestCode.ACTIVITY_REQUEST_CODE_PICTURE_LIBRARY;
        MediaUtils.launchPictureLibrary(this);
        AppLockManager.getInstance().setExtendedTimeout();
    }

    private void launchCamera() {
        MediaUtils.launchCamera(this, new MediaUtils.LaunchCameraCallback() {

            @Override
            public void onMediaCapturePathReady(String mediaCapturePath) {
                mMediaCapturePath = mediaCapturePath;
                mCurrentActivityRequest = MediaUtils.RequestCode.ACTIVITY_REQUEST_CODE_TAKE_PHOTO;
                AppLockManager.getInstance().setExtendedTimeout();
            }
        });
    }

    private void launchVideoLibrary() {
        mCurrentActivityRequest = MediaUtils.RequestCode.ACTIVITY_REQUEST_CODE_VIDEO_LIBRARY;
        MediaUtils.launchVideoLibrary(this);
        AppLockManager.getInstance().setExtendedTimeout();
    }

    private void launchVideoCamera() {
        mCurrentActivityRequest = MediaUtils.RequestCode.ACTIVITY_REQUEST_CODE_TAKE_VIDEO;
        MediaUtils.launchVideoCamera(this);
        AppLockManager.getInstance().setExtendedTimeout();
    }

    private void verifyImage(Uri imageUri) {
        if (MediaUtils.isPicasaImage(imageUri)) {
            // Create an AsyncTask to download the file
            new DownloadImageTask().execute(imageUri);
        } else {
            // It is a regular local image file
            if (!addMedia(imageUri, null))
                Toast.makeText(getActivity(), getResources().getText(R.string.gallery_error), Toast.LENGTH_SHORT).show();
        }
    }

    public void savePostContent(boolean isAutoSave) {

        if (mPost == null)
            return;

        String title = mTitleEditText.getText().toString();
        String content;

        if (mPost.isLocalDraft() || !isAutoSave) {
            Editable e = mContentEditText.getText();
            if (android.os.Build.VERSION.SDK_INT >= 14) {
                // remove suggestion spans, they cause craziness in
                // WPHtml.toHTML().
                CharacterStyle[] style = e.getSpans(0, e.length(), CharacterStyle.class);
                for (int i = 0; i < style.length; i++) {
                    if (style[i].getClass().getName().equals("android.text.style.SuggestionSpan"))
                        e.removeSpan(style[i]);
                }
            }
            content = WPHtml.toHtml(e);
            // replace duplicate <p> tags so there's not duplicates, trac #86
            content = content.replace("<p><p>", "<p>");
            content = content.replace("</p></p>", "</p>");
            content = content.replace("<br><br>", "<br>");
            // sometimes the editor creates extra tags
            content = content.replace("</strong><strong>", "").replace("</em><em>", "").replace("</u><u>", "")
                    .replace("</strike><strike>", "").replace("</blockquote><blockquote>", "");
        } else {
            content = mContentEditText.getText().toString();
        }

        String images = "";
        // update the images
        mPost.deleteMediaFiles();

        Editable s = mContentEditText.getText();

        // Add gallery shortcode
        MediaGalleryImageSpan[] gallerySpans = s.getSpans(0, s.length(), MediaGalleryImageSpan.class);
        for (MediaGalleryImageSpan gallerySpan : gallerySpans) {
            int start = s.getSpanStart(gallerySpan);
            s.removeSpan(gallerySpan);
            s.insert(start, WPHtml.getGalleryShortcode(gallerySpan));
        }

        WPImageSpan[] imageSpans = s.getSpans(0, s.length(), WPImageSpan.class);
        if (imageSpans.length != 0) {

            for (int i = 0; i < imageSpans.length; i++) {
                WPImageSpan wpIS = imageSpans[i];
                images += wpIS.getImageSource().toString() + ",";

                if (wpIS.getMediaId() != null) {
                    updateMediaFileOnServer(wpIS);
                } else {
                    MediaFile mf = new MediaFile();
                    mf.setBlogId(WordPress.getCurrentBlog().getBlogId() + "");
                    mf.setPostID(mPost.getId());
                    mf.setTitle(wpIS.getTitle());
                    mf.setCaption(wpIS.getCaption());
                    // mf.setDescription(wpIS.getDescription());
                    mf.setFeatured(wpIS.isFeatured());
                    mf.setFeaturedInPost(wpIS.isFeaturedInPost());
                    mf.setFileName(wpIS.getImageSource().toString());
                    mf.setFilePath(wpIS.getImageSource().toString());
                    mf.setHorizontalAlignment(wpIS.getHorizontalAlignment());
                    mf.setWidth(wpIS.getWidth());
                    mf.setVideo(wpIS.isVideo());
                    mf.save();
                }

                int tagStart = s.getSpanStart(wpIS);
                if (!isAutoSave) {
                    s.removeSpan(wpIS);

                    // network image has a mediaId
                    if (wpIS.getMediaId() != null && wpIS.getMediaId().length() > 0) {
                        s.insert(tagStart, WPHtml.getContent(wpIS));

                    } else { // local image for upload
                        s.insert(tagStart, "<img android-uri=\"" + wpIS.getImageSource().toString() + "\" />");
                    }
                }
            }
        }

        String moreTag = "<!--more-->";

        mPost.setTitle(title);
        // split up the post content if there's a more tag
        if (mPost.isLocalDraft() && content.indexOf(moreTag) >= 0) {
            mPost.setDescription(content.substring(0, content.indexOf(moreTag)));
            mPost.setMt_text_more(content.substring(content.indexOf(moreTag) + moreTag.length(), content.length()));
        } else {
            mPost.setDescription(content);
            mPost.setMt_text_more("");
        }
        mPost.setMediaPaths(images);
        if (!mPost.isLocalDraft())
            mPost.setLocalChange(true);
        mPost.update();
    }

    public boolean hasEmptyContentFields() {
        return TextUtils.isEmpty(mTitleEditText.getText()) && TextUtils.isEmpty(mContentEditText.getText());
    }

    private class DownloadImageTask extends AsyncTask<Uri, Integer, Uri> {

        @Override
        protected Uri doInBackground(Uri... uris) {
            Uri imageUri = uris[0];
            return MediaUtils.downloadExternalImage(getActivity(), imageUri);
        }

        @Override
        protected void onPreExecute() {
            //TODO: Show a toast instead?
            //showDialog(ID_DIALOG_DOWNLOAD);
        }

        protected void onPostExecute(Uri newUri) {
            //dismissDialog(ID_DIALOG_DOWNLOAD);
            if (newUri != null)
                addMedia(newUri, null);
            else
                Toast.makeText(getActivity(), getString(R.string.error_downloading_image), Toast.LENGTH_SHORT).show();
        }
    }

    private void prepareMediaGallery() {
        MediaGallery mediaGallery = new MediaGallery();
        mediaGallery.setIds(getActivity().getIntent().getStringArrayListExtra("NEW_MEDIA_GALLERY_EXTRA_IDS"));

        startMediaGalleryActivity(mediaGallery);
    }

    private void prepareMediaPost() {
        String mediaId = getActivity().getIntent().getStringExtra(NEW_MEDIA_POST_EXTRA);
        addExistingMediaToEditor(mediaId);
    }

    private void addExistingMediaToEditor(String mediaId) {
        if (WordPress.getCurrentBlog() == null)
            return;

        String blogId = String.valueOf(WordPress.getCurrentBlog().getBlogId());

        WPImageSpan imageSpan = MediaUtils.prepareWPImageSpan(getActivity(), blogId, mediaId);
        if (imageSpan == null)
            return;

        // based on addMedia()

        int selectionStart = mContentEditText.getSelectionStart();
        int selectionEnd = mContentEditText.getSelectionEnd();

        if (selectionStart > selectionEnd) {
            int temp = selectionEnd;
            selectionEnd = selectionStart;
            selectionStart = temp;
        }

        int line = 0, column = 0;
        try {
            line = mContentEditText.getLayout().getLineForOffset(selectionStart);
            column = mContentEditText.getSelectionStart() - mContentEditText.getLayout().getLineStart(line);
        } catch (Exception ex) {
        }

        Editable s = mContentEditText.getText();
        WPImageSpan[] gallerySpans = s.getSpans(selectionStart, selectionEnd, WPImageSpan.class);
        if (gallerySpans.length != 0) {
            // insert a few line breaks if the cursor is already on an image
            s.insert(selectionEnd, "\n\n");
            selectionStart = selectionStart + 2;
            selectionEnd = selectionEnd + 2;
        } else if (column != 0) {
            // insert one line break if the cursor is not at the first column
            s.insert(selectionEnd, "\n");
            selectionStart = selectionStart + 1;
            selectionEnd = selectionEnd + 1;
        }

        s.insert(selectionStart, " ");
        s.setSpan(imageSpan, selectionStart, selectionEnd + 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        AlignmentSpan.Standard as = new AlignmentSpan.Standard(Layout.Alignment.ALIGN_CENTER);
        s.setSpan(as, selectionStart, selectionEnd + 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        s.insert(selectionEnd + 1, "\n\n");
        try {
            mContentEditText.setSelection(s.length());
        } catch (Exception e) {
            e.printStackTrace();
        }

        // load image from server
        loadWPImageSpanThumbnail(imageSpan);
    }

    private void updateMediaFileOnServer(WPImageSpan wpIS) {

        Blog currentBlog = WordPress.getCurrentBlog();
        if (currentBlog == null || wpIS == null)
            return;

        MediaFile mf = getMediaFileFromWPImageSpan(wpIS);

        final String mediaId = mf.getMediaId();
        final String title = mf.getTitle();
        final String description = mf.getDescription();
        final String caption = mf.getCaption();

        ApiHelper.EditMediaItemTask task = new ApiHelper.EditMediaItemTask(mf.getMediaId(), mf.getTitle(),
                mf.getDescription(), mf.getCaption(),
                new ApiHelper.EditMediaItemTask.Callback() {

                    @Override
                    public void onSuccess() {
                        String blogId = String.valueOf(WordPress.getCurrentBlog().getBlogId());
                        WordPress.wpDB.updateMediaFile(blogId, mediaId, title, description, caption);
                    }

                    @Override
                    public void onFailure() {
                        Toast.makeText(getActivity(), R.string.media_edit_failure, Toast.LENGTH_LONG).show();
                    }
                });

        List<Object> apiArgs = new ArrayList<Object>();
        apiArgs.add(currentBlog);
        task.execute(apiArgs);
    }

    private MediaFile getMediaFileFromWPImageSpan(WPImageSpan wpIS) {
        MediaFile mf = new MediaFile();
        mf.setMediaId(wpIS.getMediaId());
        if (mPost != null)
            mf.setPostID(mPost.getId());
        mf.setMIMEType(wpIS.getMimeType());
        mf.setHeight(wpIS.getHeight());
        mf.setFileName(wpIS.getFileName());
        mf.setTitle(wpIS.getTitle());
        mf.setCaption(wpIS.getCaption());
        mf.setDescription(wpIS.getDescription());
        mf.setFeatured(wpIS.isFeatured());
        mf.setFeaturedInPost(wpIS.isFeaturedInPost());
        mf.setHorizontalAlignment(wpIS.getHorizontalAlignment());
        mf.setWidth(wpIS.getWidth());
        mf.setBlogId(WordPress.getCurrentBlog().getBlogId() + "");
        mf.setDateCreatedGMT(wpIS.getDateCreatedGMT());
        mf.save();
        return mf;
    }

    /** Loads the thumbnail url in the imagespan from a server **/
    private void loadWPImageSpanThumbnail(WPImageSpan imageSpan) {
        final String mediaId = imageSpan.getMediaId();
        String imageUrl = imageSpan.getThumbnailURL();
        if (imageUrl == null || mediaId == null)
            return;

        if (WordPress.getCurrentBlog() != null && WordPress.getCurrentBlog().isPhotonCapable()) {
            String photonUrl = imageSpan.getImageSource().toString();
            imageUrl = StringUtils.getPhotonUrl(photonUrl, 400);
        }

        WordPress.imageLoader.get(imageUrl, new ImageLoader.ImageListener() {

            @Override
            public void onErrorResponse(VolleyError arg0) {

            }

            @Override
            public void onResponse(ImageLoader.ImageContainer container, boolean arg1) {
                if (container.getBitmap() != null) {

                    Bitmap bitmap = container.getBitmap();

                    ImageHelper ih = new ImageHelper();
                    ByteArrayOutputStream stream = new ByteArrayOutputStream();
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
                    byte[] bitmapByteArray = stream.toByteArray();
                    Bitmap resizedBitmap = ih.getResizedImageThumbnail(getActivity(), bitmapByteArray, null);
                    if (resizedBitmap == null)
                        return;
                    Editable s = mContentEditText.getText();
                    WPImageSpan[] spans = s.getSpans(0, s.length(), WPImageSpan.class);
                    if (spans.length != 0) {
                        for (WPImageSpan is : spans) {
                            if (mediaId != null && mediaId.equals(is.getMediaId()) && !is.isNetworkImageLoaded()) {

                                // replace the existing span with a new one with the correct image, re-add it to the same position.
                                int spanStart = s.getSpanStart(is);
                                int spanEnd = s.getSpanEnd(is);
                                WPImageSpan imageSpan = new WPImageSpan(getActivity(), resizedBitmap, is.getImageSource());
                                imageSpan.setCaption(is.getCaption());
                                imageSpan.setDescription(is.getDescription());
                                imageSpan.setFeatured(is.isFeatured());
                                imageSpan.setFeaturedInPost(is.isFeaturedInPost());
                                imageSpan.setHeight(is.getHeight());
                                imageSpan.setHorizontalAlignment(is.getHorizontalAlignment());
                                imageSpan.setMediaId(is.getMediaId());
                                imageSpan.setMimeType(is.getMimeType());
                                imageSpan.setTitle(is.getTitle());
                                imageSpan.setVideo(is.isVideo());
                                imageSpan.setWidth(is.getWidth());
                                imageSpan.setFileName(is.getFileName());
                                imageSpan.setThumbnailURL(is.getThumbnailURL());
                                imageSpan.setDateCreatedGMT(is.getDateCreatedGMT());
                                imageSpan.setNetworkImageLoaded(true);
                                s.removeSpan(is);
                                s.setSpan(imageSpan, spanStart, spanEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                                break;
                            }
                        }
                    }
                }
            }
        }, 0, 0);
    }

    private void startMediaGalleryActivity(MediaGallery mediaGallery) {
        Intent intent = new Intent(getActivity(), MediaGalleryActivity.class);
        intent.putExtra(MediaGalleryActivity.PARAMS_MEDIA_GALLERY, mediaGallery);
        startActivityForResult(intent, MediaGalleryActivity.REQUEST_CODE);
    }

    private void startMediaGalleryAddActivity() {
        Intent intent = new Intent(getActivity(), MediaGalleryPickerActivity.class);
        intent.putExtra(MediaGalleryPickerActivity.PARAM_SELECT_ONE_ITEM, true);
        startActivityForResult(intent, MediaGalleryPickerActivity.REQUEST_CODE);
    }

    private void handleMediaGalleryPickerResult(Intent data) {
        ArrayList<String> ids = data.getStringArrayListExtra(MediaGalleryPickerActivity.RESULT_IDS);
        if (ids == null || ids.size() == 0)
            return;

        String mediaId = ids.get(0);
        addExistingMediaToEditor(mediaId);
    }

    private void handleMediaGalleryResult(Intent data) {
        MediaGallery gallery = (MediaGallery) data.getSerializableExtra(MediaGalleryActivity.RESULT_MEDIA_GALLERY);

        // if blank gallery returned, don't add to span
        if (gallery == null || gallery.getIds().size() == 0)
            return;


        int selectionStart = mContentEditText.getSelectionStart();
        int selectionEnd = mContentEditText.getSelectionEnd();

        if (selectionStart > selectionEnd) {
            int temp = selectionEnd;
            selectionEnd = selectionStart;
            selectionStart = temp;
        }

        int line = 0, column = 0;
        try {
            line = mContentEditText.getLayout().getLineForOffset(selectionStart);
            column = mContentEditText.getSelectionStart() - mContentEditText.getLayout().getLineStart(line);
        } catch (Exception ex) {
        }

        Editable s = mContentEditText.getText();
        MediaGalleryImageSpan[] gallerySpans = s.getSpans(selectionStart, selectionEnd, MediaGalleryImageSpan.class);
        if (gallerySpans.length != 0) {
            for (int i = 0; i < gallerySpans.length; i++) {
                if (gallerySpans[i].getMediaGallery().getUniqueId() == gallery.getUniqueId()) {

                    // replace the existing span with a new gallery, re-add it to the same position.
                    gallerySpans[i].setMediaGallery(gallery);
                    int spanStart = s.getSpanStart(gallerySpans[i]);
                    int spanEnd = s.getSpanEnd(gallerySpans[i]);
                    s.setSpan(gallerySpans[i], spanStart, spanEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
            }
            return;
        } else if (column != 0) {
            // insert one line break if the cursor is not at the first column
            s.insert(selectionEnd, "\n");
            selectionStart = selectionStart + 1;
            selectionEnd = selectionEnd + 1;
        }

        s.insert(selectionStart, " ");
        MediaGalleryImageSpan is = new MediaGalleryImageSpan(getActivity(), gallery);
        s.setSpan(is, selectionStart, selectionEnd + 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        AlignmentSpan.Standard as = new AlignmentSpan.Standard(Layout.Alignment.ALIGN_CENTER);
        s.setSpan(as, selectionStart, selectionEnd + 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        s.insert(selectionEnd + 1, "\n\n");
        try {
            mContentEditText.setSelection(s.length());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private boolean addMedia(Uri imageUri, SpannableStringBuilder ssb) {

        //if (mFormatBar.getVisibility() == View.VISIBLE)
        //    hideFormatBar();

        if (ssb != null && MediaUtils.isPicasaImage(imageUri))
            imageUri = MediaUtils.downloadExternalImage(getActivity(), imageUri);

        if (imageUri == null) {
            return false;
        }

        ImageHelper ih = new ImageHelper();
        Map<String, Object> mediaData = ih.getImageBytesForPath(imageUri.getEncodedPath(), getActivity());

        if (mediaData == null) {
            // data stream not returned
            return false;
        }

        Bitmap resizedBitmap = ih.getResizedImageThumbnail(getActivity(), (byte[]) mediaData.get("bytes"), (String) mediaData.get("orientation"));
        if (resizedBitmap == null)
            return false;

        if (ssb != null) {
            WPImageSpan is = new WPImageSpan(getActivity(), resizedBitmap, imageUri);

            MediaUtils.setWPImageSpanWidth(getActivity(), imageUri, is);

            is.setTitle((String) mediaData.get("title"));
            is.setImageSource(imageUri);
            is.setVideo(imageUri.getEncodedPath().contains("video"));
            ssb.append(" ");
            ssb.setSpan(is, ssb.length() - 1, ssb.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            AlignmentSpan.Standard as = new AlignmentSpan.Standard(Layout.Alignment.ALIGN_CENTER);
            ssb.setSpan(as, ssb.length() - 1, ssb.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            ssb.append("\n");
        } else {
            int selectionStart = mContentEditText.getSelectionStart();
            mStyleStart = selectionStart;
            int selectionEnd = mContentEditText.getSelectionEnd();

            if (selectionStart > selectionEnd) {
                int temp = selectionEnd;
                selectionEnd = selectionStart;
                selectionStart = temp;
            }

            Editable s = mContentEditText.getText();
            WPImageSpan is = new WPImageSpan(getActivity(), resizedBitmap, imageUri);

            MediaUtils.setWPImageSpanWidth(getActivity(), imageUri, is);

            is.setTitle((String) mediaData.get("title"));
            is.setImageSource(imageUri);
            if (imageUri.getEncodedPath().contains("video")) {
                is.setVideo(true);
            }

            int line = 0, column = 0;
            try {
                line = mContentEditText.getLayout().getLineForOffset(selectionStart);
                column = mContentEditText.getSelectionStart() - mContentEditText.getLayout().getLineStart(line);
            } catch (Exception ex) {
            }

            WPImageSpan[] image_spans = s.getSpans(selectionStart, selectionEnd, WPImageSpan.class);
            if (image_spans.length != 0) {
                // insert a few line breaks if the cursor is already on an image
                s.insert(selectionEnd, "\n\n");
                selectionStart = selectionStart + 2;
                selectionEnd = selectionEnd + 2;
            } else if (column != 0) {
                // insert one line break if the cursor is not at the first column
                s.insert(selectionEnd, "\n");
                selectionStart = selectionStart + 1;
                selectionEnd = selectionEnd + 1;
            }

            s.insert(selectionStart, " ");
            s.setSpan(is, selectionStart, selectionEnd + 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            AlignmentSpan.Standard as = new AlignmentSpan.Standard(Layout.Alignment.ALIGN_CENTER);
            s.setSpan(as, selectionStart, selectionEnd + 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            s.insert(selectionEnd + 1, "\n\n");
            try {
                mContentEditText.setSelection(s.length());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return true;
    }

    /**
     * Formatting bar
     */

    private View.OnClickListener mFormatBarButtonClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            int id = v.getId();
            if (id == R.id.bold) {
                formatBtnClick(mBoldToggleButton, "strong");
            } else if (id == R.id.em) {
                formatBtnClick(mEmToggleButton, "em");
            } else if (id == R.id.underline) {
                formatBtnClick(mUnderlineToggleButton, "u");
            } else if (id == R.id.strike) {
                formatBtnClick(mStrikeToggleButton, "strike");
            } else if (id == R.id.bquote) {
                formatBtnClick(mBquoteToggleButton, "blockquote");
            } else if (id == R.id.more) {
                mSelectionEnd = mContentEditText.getSelectionEnd();
                Editable str = mContentEditText.getText();
                str.insert(mSelectionEnd, "\n<!--more-->\n");
            } else if (id == R.id.link) {
                mSelectionStart = mContentEditText.getSelectionStart();
                mStyleStart = mSelectionStart;
                mSelectionEnd = mContentEditText.getSelectionEnd();
                if (mSelectionStart > mSelectionEnd) {
                    int temp = mSelectionEnd;
                    mSelectionEnd = mSelectionStart;
                    mSelectionStart = temp;
                }
                Intent i = new Intent(getActivity(), EditLinkActivity.class);
                if (mSelectionEnd > mSelectionStart) {
                    String selectedText = mContentEditText.getText().subSequence(mSelectionStart, mSelectionEnd).toString();
                    i.putExtra("selectedText", selectedText);
                }
                startActivityForResult(i, ACTIVITY_REQUEST_CODE_CREATE_LINK);
            } else if (id == R.id.addPictureButton) {
                mAddPictureButton.performLongClick();
            } else if (id == R.id.post) {
                // TODO: uploading happens from new activity
                /*if (mAutoSaveHandler != null)
                    mAutoSaveHandler.removeCallbacks(autoSaveRunnable);
                if (savePost(false, false)) {
                    if (mPost.isUploaded() || !mPost.getPost_status().equals("localdraft")) {
                        if (mQuickMediaType >= 0) {
                            if (mQuickMediaType == Constants.QUICK_POST_PHOTO_CAMERA || mQuickMediaType == Constants.QUICK_POST_PHOTO_LIBRARY)
                                mPost.setQuickPostType("QuickPhoto");
                            else if (mQuickMediaType == Constants.QUICK_POST_VIDEO_CAMERA || mQuickMediaType == Constants.QUICK_POST_VIDEO_LIBRARY)
                                mPost.setQuickPostType("QuickVideo");
                        }

                        WordPress.currentPost = mPost;
                        PostUploadService.addPostToUpload(mPost);
                        startService(new Intent(this, PostUploadService.class));
                    }
                    getActivity().finish();*/
                }
            }
    };

    private void formatBtnClick(ToggleButton toggleButton, String tag) {
        try {
            Spannable s = mContentEditText.getText();
            int selectionStart = mContentEditText.getSelectionStart();
            mStyleStart = selectionStart;
            int selectionEnd = mContentEditText.getSelectionEnd();

            if (selectionStart > selectionEnd) {
                int temp = selectionEnd;
                selectionEnd = selectionStart;
                selectionStart = temp;
            }

            if (mPost.isLocalDraft()) {
                if (selectionEnd > selectionStart) {
                    Spannable str = mContentEditText.getText();
                    if (tag.equals("strong")) {
                        StyleSpan[] ss = str.getSpans(selectionStart, selectionEnd, StyleSpan.class);

                        boolean exists = false;
                        for (int i = 0; i < ss.length; i++) {
                            int style = ((StyleSpan) ss[i]).getStyle();
                            if (style == android.graphics.Typeface.BOLD) {
                                str.removeSpan(ss[i]);
                                exists = true;
                            }
                        }

                        if (!exists) {
                            str.setSpan(new StyleSpan(android.graphics.Typeface.BOLD), selectionStart, selectionEnd,
                                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                        }
                        toggleButton.setChecked(false);
                    } else if (tag.equals("em")) {
                        StyleSpan[] ss = str.getSpans(selectionStart, selectionEnd, StyleSpan.class);

                        boolean exists = false;
                        for (int i = 0; i < ss.length; i++) {
                            int style = ((StyleSpan) ss[i]).getStyle();
                            if (style == android.graphics.Typeface.ITALIC) {
                                str.removeSpan(ss[i]);
                                exists = true;
                            }
                        }

                        if (!exists) {
                            str.setSpan(new StyleSpan(android.graphics.Typeface.ITALIC), selectionStart, selectionEnd,
                                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                        }
                        toggleButton.setChecked(false);
                    } else if (tag.equals("u")) {

                        WPUnderlineSpan[] ss = str.getSpans(selectionStart, selectionEnd, WPUnderlineSpan.class);

                        boolean exists = false;
                        for (int i = 0; i < ss.length; i++) {
                            str.removeSpan(ss[i]);
                            exists = true;
                        }

                        if (!exists) {
                            str.setSpan(new WPUnderlineSpan(), selectionStart, selectionEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                        }

                        toggleButton.setChecked(false);
                    } else if (tag.equals("strike")) {

                        StrikethroughSpan[] ss = str.getSpans(selectionStart, selectionEnd, StrikethroughSpan.class);

                        boolean exists = false;
                        for (int i = 0; i < ss.length; i++) {
                            str.removeSpan(ss[i]);
                            exists = true;
                        }

                        if (!exists) {
                            str.setSpan(new StrikethroughSpan(), selectionStart, selectionEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                        }

                        toggleButton.setChecked(false);
                    } else if (tag.equals("blockquote")) {

                        QuoteSpan[] ss = str.getSpans(selectionStart, selectionEnd, QuoteSpan.class);

                        boolean exists = false;
                        for (int i = 0; i < ss.length; i++) {
                            str.removeSpan(ss[i]);
                            exists = true;
                        }

                        if (!exists) {
                            str.setSpan(new QuoteSpan(), selectionStart, selectionEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                        }

                        toggleButton.setChecked(false);
                    }
                } else if (!toggleButton.isChecked()) {

                    if (tag.equals("strong") || tag.equals("em")) {

                        StyleSpan[] ss = s.getSpans(mStyleStart - 1, mStyleStart, StyleSpan.class);

                        for (int i = 0; i < ss.length; i++) {
                            int tagStart = s.getSpanStart(ss[i]);
                            int tagEnd = s.getSpanEnd(ss[i]);
                            if (ss[i].getStyle() == android.graphics.Typeface.BOLD && tag.equals("strong")) {
                                tagStart = s.getSpanStart(ss[i]);
                                tagEnd = s.getSpanEnd(ss[i]);
                                s.removeSpan(ss[i]);
                                s.setSpan(new StyleSpan(android.graphics.Typeface.BOLD), tagStart, tagEnd,
                                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                            }
                            if (ss[i].getStyle() == android.graphics.Typeface.ITALIC && tag.equals("em")) {
                                tagStart = s.getSpanStart(ss[i]);
                                tagEnd = s.getSpanEnd(ss[i]);
                                s.removeSpan(ss[i]);
                                s.setSpan(new StyleSpan(android.graphics.Typeface.ITALIC), tagStart, tagEnd,
                                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                            }
                        }
                    } else if (tag.equals("u")) {
                        WPUnderlineSpan[] us = s.getSpans(mStyleStart - 1, mStyleStart, WPUnderlineSpan.class);
                        for (int i = 0; i < us.length; i++) {
                            int tagStart = s.getSpanStart(us[i]);
                            int tagEnd = s.getSpanEnd(us[i]);
                            s.removeSpan(us[i]);
                            s.setSpan(new WPUnderlineSpan(), tagStart, tagEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                        }
                    } else if (tag.equals("strike")) {
                        StrikethroughSpan[] ss = s.getSpans(mStyleStart - 1, mStyleStart, StrikethroughSpan.class);
                        for (int i = 0; i < ss.length; i++) {
                            int tagStart = s.getSpanStart(ss[i]);
                            int tagEnd = s.getSpanEnd(ss[i]);
                            s.removeSpan(ss[i]);
                            s.setSpan(new StrikethroughSpan(), tagStart, tagEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                        }
                    } else if (tag.equals("blockquote")) {
                        QuoteSpan[] ss = s.getSpans(mStyleStart - 1, mStyleStart, QuoteSpan.class);
                        for (int i = 0; i < ss.length; i++) {
                            int tagStart = s.getSpanStart(ss[i]);
                            int tagEnd = s.getSpanEnd(ss[i]);
                            s.removeSpan(ss[i]);
                            s.setSpan(new QuoteSpan(), tagStart, tagEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                        }
                    }
                }
            } else {
                String startTag = "<" + tag + ">";
                String endTag = "</" + tag + ">";
                Editable content = mContentEditText.getText();
                if (selectionEnd > selectionStart) {
                    content.insert(selectionStart, startTag);
                    content.insert(selectionEnd + startTag.length(), endTag);
                    toggleButton.setChecked(false);
                    mContentEditText.setSelection(selectionEnd + startTag.length() + endTag.length());
                } else if (toggleButton.isChecked()) {
                    content.insert(selectionStart, startTag);
                    mContentEditText.setSelection(selectionEnd + startTag.length());
                } else if (!toggleButton.isChecked()) {
                    content.insert(selectionEnd, endTag);
                    mContentEditText.setSelection(selectionEnd + endTag.length());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Rich Text Editor
     */

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        float pos = event.getY();

        if (event.getAction() == 0)
            mLastYPos = pos;

        if (event.getAction() > 1) {
            if (((mLastYPos - pos) > 2.0f) || ((pos - mLastYPos) > 2.0f))
                mScrollDetected = true;
        }

        mLastYPos = pos;

        if (event.getAction() == 1 && !mScrollDetected) {
            Layout layout = ((TextView) v).getLayout();
            int x = (int) event.getX();
            int y = (int) event.getY();

            x += v.getScrollX();
            y += v.getScrollY();
            if (layout != null) {
                int line = layout.getLineForVertical(y);
                int charPosition = layout.getOffsetForHorizontal(line, x);

                final Spannable s = mContentEditText.getText();
                // check if image span was tapped
                WPImageSpan[] image_spans = s.getSpans(charPosition, charPosition, WPImageSpan.class);

                if (image_spans.length != 0) {
                    final WPImageSpan span = image_spans[0];
                    if (!span.isVideo()) {
                        LayoutInflater factory = LayoutInflater.from(getActivity());
                        final View alertView = factory.inflate(R.layout.alert_image_options, null);
                        final EditText imageWidthText = (EditText) alertView.findViewById(R.id.imageWidthText);
                        final EditText titleText = (EditText) alertView.findViewById(R.id.title);
                        // final EditText descText = (EditText)
                        // alertView.findViewById(R.id.description);
                        final EditText caption = (EditText) alertView.findViewById(R.id.caption);
                        final CheckBox featuredCheckBox = (CheckBox) alertView.findViewById(R.id.featuredImage);
                        final CheckBox featuredInPostCheckBox = (CheckBox) alertView.findViewById(R.id.featuredInPost);

                        // show featured image checkboxes if theme support it
                        if (WordPress.getCurrentBlog().isFeaturedImageCapable()) {
                            featuredCheckBox.setVisibility(View.VISIBLE);
                            featuredInPostCheckBox.setVisibility(View.VISIBLE);
                        }

                        featuredCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                            @Override
                            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                                if (isChecked) {
                                    featuredInPostCheckBox.setVisibility(View.VISIBLE);
                                } else {
                                    featuredInPostCheckBox.setVisibility(View.GONE);
                                }

                            }
                        });

                        final SeekBar seekBar = (SeekBar) alertView.findViewById(R.id.imageWidth);
                        final Spinner alignmentSpinner = (Spinner) alertView.findViewById(R.id.alignment_spinner);
                        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(getActivity(), R.array.alignment_array,
                                android.R.layout.simple_spinner_item);
                        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                        alignmentSpinner.setAdapter(adapter);

                        imageWidthText.setText(String.valueOf(span.getWidth()) + "px");
                        seekBar.setProgress(span.getWidth());
                        titleText.setText(span.getTitle());
                        // descText.setText(span.getDescription());
                        caption.setText(span.getCaption());
                        featuredCheckBox.setChecked(span.isFeatured());

                        if (span.isFeatured())
                            featuredInPostCheckBox.setVisibility(View.VISIBLE);
                        else
                            featuredInPostCheckBox.setVisibility(View.GONE);

                        featuredInPostCheckBox.setChecked(span.isFeaturedInPost());

                        alignmentSpinner.setSelection(span.getHorizontalAlignment(), true);

                        final int maxWidth = MediaUtils.getMinimumImageWitdh(getActivity(), span.getImageSource());
                        seekBar.setMax(maxWidth / 10);
                        if (span.getWidth() != 0)
                            seekBar.setProgress(span.getWidth() / 10);
                        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

                            @Override
                            public void onStopTrackingTouch(SeekBar seekBar) {
                            }

                            @Override
                            public void onStartTrackingTouch(SeekBar seekBar) {
                            }

                            @Override
                            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                                if (progress == 0)
                                    progress = 1;
                                imageWidthText.setText(progress * 10 + "px");
                            }
                        });

                        imageWidthText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                            @Override
                            public void onFocusChange(View v, boolean hasFocus) {
                                if (hasFocus) {
                                    imageWidthText.setText("");
                                }
                            }
                        });

                        imageWidthText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
                            @Override
                            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                                int width = getEditTextIntegerClamped(imageWidthText, 10, maxWidth);
                                seekBar.setProgress(width / 10);
                                imageWidthText.setSelection((String.valueOf(width).length()));

                                InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                                imm.hideSoftInputFromWindow(imageWidthText.getWindowToken(),
                                        InputMethodManager.RESULT_UNCHANGED_SHOWN);

                                return true;
                            }
                        });

                        AlertDialog ad = new AlertDialog.Builder(getActivity()).setTitle(getString(R.string.image_settings))
                                .setView(alertView).setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int whichButton) {

                                        span.setTitle(titleText.getText().toString());
                                        // span.setDescription(descText.getText().toString());
                                        span.setHorizontalAlignment(alignmentSpinner.getSelectedItemPosition());
                                        span.setWidth(getEditTextIntegerClamped(imageWidthText, 10, maxWidth));
                                        span.setCaption(caption.getText().toString());
                                        span.setFeatured(featuredCheckBox.isChecked());
                                        if (featuredCheckBox.isChecked()) {
                                            // remove featured flag from all
                                            // other images
                                            WPImageSpan[] click_spans = s.getSpans(0, s.length(), WPImageSpan.class);
                                            if (click_spans.length > 1) {
                                                for (int i = 0; i < click_spans.length; i++) {
                                                    WPImageSpan verifySpan = click_spans[i];
                                                    if (verifySpan != span) {
                                                        verifySpan.setFeatured(false);
                                                        verifySpan.setFeaturedInPost(false);
                                                    }
                                                }
                                            }
                                        }
                                        span.setFeaturedInPost(featuredInPostCheckBox.isChecked());
                                    }
                                }).setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int whichButton) {
                                        dialog.dismiss();
                                    }
                                }).create();
                        ad.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
                        ad.show();
                        mScrollDetected = false;
                        return true;
                    }

                } else {
                    mContentEditText.setMovementMethod(ArrowKeyMovementMethod.getInstance());
                    int selectionStart = mContentEditText.getSelectionStart();
                    if (selectionStart >= 0 && mContentEditText.getSelectionEnd() >= selectionStart);
                    mContentEditText.setSelection(selectionStart, mContentEditText.getSelectionEnd());
                }

                // get media gallery spans

                MediaGalleryImageSpan[] gallerySpans = s.getSpans(charPosition, charPosition, MediaGalleryImageSpan.class);
                if (gallerySpans.length > 0) {
                    final MediaGalleryImageSpan gallerySpan = gallerySpans[0];
                    startMediaGalleryActivity(gallerySpan.getMediaGallery());
                }

            }
        } else if (event.getAction() == 1) {
            mScrollDetected = false;
        }
        return false;
    }

    @Override
    public void afterTextChanged(Editable s) {

        try {
            int position = Selection.getSelectionStart(mContentEditText.getText());
            if ((mIsBackspace && position != 1) || mLastPosition == position || !mPost.isLocalDraft())
                return;

            if (position < 0) {
                position = 0;
            }
            mLastPosition = position;
            if (position > 0) {

                if (mStyleStart > position) {
                    mStyleStart = position - 1;
                }
                boolean exists = false;
                if (mBoldToggleButton.isChecked()) {
                    StyleSpan[] ss = s.getSpans(mStyleStart, position, StyleSpan.class);
                    exists = false;
                    for (int i = 0; i < ss.length; i++) {
                        if (ss[i].getStyle() == android.graphics.Typeface.BOLD) {
                            exists = true;
                        }
                    }
                    if (!exists)
                        s.setSpan(new StyleSpan(android.graphics.Typeface.BOLD), mStyleStart, position, Spannable.SPAN_INCLUSIVE_INCLUSIVE);
                }
                if (mEmToggleButton.isChecked()) {
                    StyleSpan[] ss = s.getSpans(mStyleStart, position, StyleSpan.class);
                    exists = false;
                    for (int i = 0; i < ss.length; i++) {
                        if (ss[i].getStyle() == android.graphics.Typeface.ITALIC) {
                            exists = true;
                        }
                    }
                    if (!exists)
                        s.setSpan(new StyleSpan(android.graphics.Typeface.ITALIC), mStyleStart, position,
                                Spannable.SPAN_INCLUSIVE_INCLUSIVE);
                }
                if (mEmToggleButton.isChecked()) {
                    StyleSpan[] ss = s.getSpans(mStyleStart, position, StyleSpan.class);
                    exists = false;
                    for (int i = 0; i < ss.length; i++) {
                        if (ss[i].getStyle() == android.graphics.Typeface.ITALIC) {
                            exists = true;
                        }
                    }
                    if (!exists)
                        s.setSpan(new StyleSpan(android.graphics.Typeface.ITALIC), mStyleStart, position,
                                Spannable.SPAN_INCLUSIVE_INCLUSIVE);
                }
                if (mUnderlineToggleButton.isChecked()) {
                    WPUnderlineSpan[] ss = s.getSpans(mStyleStart, position, WPUnderlineSpan.class);
                    exists = false;
                    for (int i = 0; i < ss.length; i++) {
                        exists = true;
                    }
                    if (!exists)
                        s.setSpan(new WPUnderlineSpan(), mStyleStart, position, Spannable.SPAN_INCLUSIVE_INCLUSIVE);
                }
                if (mStrikeToggleButton.isChecked()) {
                    StrikethroughSpan[] ss = s.getSpans(mStyleStart, position, StrikethroughSpan.class);
                    exists = false;
                    for (int i = 0; i < ss.length; i++) {
                        exists = true;
                    }
                    if (!exists)
                        s.setSpan(new StrikethroughSpan(), mStyleStart, position, Spannable.SPAN_INCLUSIVE_INCLUSIVE);
                }
                if (mBquoteToggleButton.isChecked()) {

                    QuoteSpan[] ss = s.getSpans(mStyleStart, position, QuoteSpan.class);
                    exists = false;
                    for (int i = 0; i < ss.length; i++) {
                        exists = true;
                    }
                    if (!exists)
                        s.setSpan(new QuoteSpan(), mStyleStart, position, Spannable.SPAN_INCLUSIVE_INCLUSIVE);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        if ((count - after == 1) || (s.length() == 0))
            mIsBackspace = true;
        else
            mIsBackspace = false;
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
    }

    @Override
    public void onSelectionChanged() {
        if (!mPost.isLocalDraft())
            return;

        final Spannable s = mContentEditText.getText();
        // set toggle buttons if cursor is inside of a matching span
        mStyleStart = mContentEditText.getSelectionStart();
        Object[] spans = s.getSpans(mContentEditText.getSelectionStart(), mContentEditText.getSelectionStart(), Object.class);

        mBoldToggleButton.setChecked(false);
        mEmToggleButton.setChecked(false);
        mBquoteToggleButton.setChecked(false);
        mUnderlineToggleButton.setChecked(false);
        mStrikeToggleButton.setChecked(false);
        for (Object span : spans) {
            if (span instanceof StyleSpan) {
                StyleSpan ss = (StyleSpan) span;
                if (ss.getStyle() == android.graphics.Typeface.BOLD) {
                    mBoldToggleButton.setChecked(true);
                }
                if (ss.getStyle() == android.graphics.Typeface.ITALIC) {
                    mEmToggleButton.setChecked(true);
                }
            }
            if (span instanceof QuoteSpan) {
                mBquoteToggleButton.setChecked(true);
            }
            if (span instanceof WPUnderlineSpan) {
                mUnderlineToggleButton.setChecked(true);
            }
            if (span instanceof StrikethroughSpan) {
                mStrikeToggleButton.setChecked(true);
            }
        }
    }

    private int getEditTextIntegerClamped(EditText editText, int min, int max) {
        int width = 10;
        try {
            width = Integer.parseInt(editText.getText().toString().replace("px", ""));
        } catch (NumberFormatException e) {
        }
        width = Math.min(max, Math.max(width, min));
        return width;
    }
}
