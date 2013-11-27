package org.wordpress.android.ui.posts;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Typeface;
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

import org.wordpress.android.Constants;
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
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Vector;

/**
 * Created by dan on 11/20/13.
 */
public class EditPostContentFragment extends SherlockFragment implements TextWatcher,
        WPEditText.OnSelectionChangedListener, View.OnTouchListener {

    NewEditPostActivity mActivity;

    private static final int ACTIVITY_REQUEST_CODE_CREATE_LINK = 4;
    public static final String NEW_MEDIA_GALLERY = "NEW_MEDIA_GALLERY";
    public static final String NEW_MEDIA_GALLERY_EXTRA_IDS = "NEW_MEDIA_GALLERY_EXTRA_IDS";
    public static final String NEW_MEDIA_POST = "NEW_MEDIA_POST";
    public static final String NEW_MEDIA_POST_EXTRA = "NEW_MEDIA_POST_ID";

    private static final String TAG_FORMAT_BAR_BUTTON_STRONG = "strong";
    private static final String TAG_FORMAT_BAR_BUTTON_EM = "em";
    private static final String TAG_FORMAT_BAR_BUTTON_UNDERLINE = "u";
    private static final String TAG_FORMAT_BAR_BUTTON_STRIKE = "strike";
    private static final String TAG_FORMAT_BAR_BUTTON_QUOTE = "blockquote";

    private WPEditText mContentEditText;
    private ImageButton mAddPictureButton;
    private EditText mTitleEditText;
    private ToggleButton mBoldToggleButton, mEmToggleButton, mBquoteToggleButton;
    private ToggleButton mUnderlineToggleButton, mStrikeToggleButton;
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

        final ViewGroup rootView = (ViewGroup) inflater
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
        Button linkButton = (Button) rootView.findViewById(R.id.link);
        Button moreButton = (Button) rootView.findViewById(R.id.more);

        registerForContextMenu(mAddPictureButton);
        mContentEditText.setOnSelectionChangedListener(this);
        mContentEditText.setOnTouchListener(this);
        mContentEditText.addTextChangedListener(this);
        mContentEditText.setOnEditTextImeBackListener(new WPEditText.EditTextImeBackListener() {
            @Override
            public void onImeBack(WPEditText ctrl, String text) {
                // Go back to regular editor if IME keyboard is dismissed
                if (!mActivity.getSupportActionBar().isShowing())
                    setContentEditingModeVisible(false);
            }
        });
        mAddPictureButton.setOnClickListener(mFormatBarButtonClickListener);
        mBoldToggleButton.setOnClickListener(mFormatBarButtonClickListener);
        linkButton.setOnClickListener(mFormatBarButtonClickListener);
        mEmToggleButton.setOnClickListener(mFormatBarButtonClickListener);
        mUnderlineToggleButton.setOnClickListener(mFormatBarButtonClickListener);
        mStrikeToggleButton.setOnClickListener(mFormatBarButtonClickListener);
        mBquoteToggleButton.setOnClickListener(mFormatBarButtonClickListener);
        moreButton.setOnClickListener(mFormatBarButtonClickListener);

        Post post = mActivity.getPost();
        if (post != null) {
            if (!TextUtils.isEmpty(post.getContent())) {
                if (post.isLocalDraft())
                    mContentEditText.setText(WPHtml.fromHtml(post.getContent().replaceAll("\uFFFC", ""), mActivity, post));
                else
                    mContentEditText.setText(post.getContent().replaceAll("\uFFFC", ""));
            }
            if (!TextUtils.isEmpty(post.getTitle())) {
                mTitleEditText.setText(post.getTitle());
            }
        }

        mContentEditText.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_UP && mActivity != null && mActivity.getSupportActionBar().isShowing()) {
                    setContentEditingModeVisible(true);
                }

                return false;
            }
        });

        // Check for Android share action
        String action = mActivity.getIntent().getAction();
        int quickMediaType = -1;
        if (mActivity.getIntent().getExtras() != null)
            quickMediaType = mActivity.getIntent().getExtras().getInt("quick-media", -1);
        if (Intent.ACTION_SEND.equals(action) || Intent.ACTION_SEND_MULTIPLE.equals(action))
            setPostContentFromShareAction();
        else if (NEW_MEDIA_GALLERY.equals(action))
            prepareMediaGallery();
        else if (NEW_MEDIA_POST.equals(action))
            prepareMediaPost();
        else if (quickMediaType >= 0) {
            // User selected a 'Quick (media type)' option in the menu drawer
            if (quickMediaType == Constants.QUICK_POST_PHOTO_CAMERA)
                launchCamera();
            else if (quickMediaType == Constants.QUICK_POST_PHOTO_LIBRARY)
                launchPictureLibrary();
            else if (quickMediaType == Constants.QUICK_POST_VIDEO_CAMERA)
                launchVideoCamera();
            else if (quickMediaType == Constants.QUICK_POST_VIDEO_LIBRARY)
                launchVideoLibrary();

            if (post != null) {
                if (quickMediaType == Constants.QUICK_POST_PHOTO_CAMERA || quickMediaType == Constants.QUICK_POST_PHOTO_LIBRARY)
                    post.setQuickPostType(Post.QUICK_MEDIA_TYPE_PHOTO);
                else if (quickMediaType == Constants.QUICK_POST_VIDEO_CAMERA || quickMediaType == Constants.QUICK_POST_VIDEO_LIBRARY)
                    post.setQuickPostType(Post.QUICK_MEDIA_TYPE_VIDEO);
            }
        }

        return rootView;
    }

    public void setContentEditingModeVisible(boolean isVisible) {
        if (mActivity == null)
            return;
        ActionBar actionBar = mActivity.getSupportActionBar();
        if (isVisible) {
            actionBar.hide();
            mTitleEditText.setVisibility(View.GONE);
            mActivity.setViewPagerEnabled(false);
            mFormatBar.setVisibility(View.VISIBLE);

        } else {
            mActivity.setViewPagerEnabled(true);
            mTitleEditText.setVisibility(View.VISIBLE);
            mFormatBar.setVisibility(View.GONE);
            mActivity.supportInvalidateOptionsMenu();
            actionBar.show();
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
                        if (extras == null)
                            return;
                        String linkURL = extras.getString("linkURL");
                        if (linkURL != null && !linkURL.equals("http://") && !linkURL.equals("")) {

                            if (mSelectionStart > mSelectionEnd) {
                                int temp = mSelectionEnd;
                                mSelectionEnd = mSelectionStart;
                                mSelectionStart = temp;
                            }
                            Editable str = mContentEditText.getText();
                            if (str == null)
                                return;
                            if (mActivity.getPost().isLocalDraft()) {
                                if (extras.getString("linkText") == null) {
                                    if (mSelectionStart < mSelectionEnd)
                                        str.delete(mSelectionStart, mSelectionEnd);
                                    str.insert(mSelectionStart, linkURL);
                                    str.setSpan(new URLSpan(linkURL), mSelectionStart, mSelectionStart + linkURL.length(),
                                            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                                    mContentEditText.setSelection(mSelectionStart + linkURL.length());
                                } else {
                                    String linkText = extras.getString("linkText");
                                    if (linkText == null)
                                        return;
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

    protected void setPostContentFromShareAction() {
        Intent intent = mActivity.getIntent();
        String text = intent.getStringExtra(Intent.EXTRA_TEXT);
        String title = intent.getStringExtra(Intent.EXTRA_SUBJECT);
        if (text != null) {

            if (title != null) {
                mTitleEditText.setText(title);
            }

            if (text.contains("youtube_gdata")) {
                // Just use the URL for YouTube links for oEmbed support
                mContentEditText.setText(text);
            } else {
                // add link tag around URLs, trac #64
                text = text.replaceAll("((http|https|ftp|mailto):\\S+)", "<a href=\"$1\">$1</a>");
                mContentEditText.setText(WPHtml.fromHtml(StringUtils.addPTags(text), getActivity(), mActivity.getPost()));
            }
        } else {
            String action = intent.getAction();
            final String type = intent.getType();
            final ArrayList<Uri> multi_stream;
            if (Intent.ACTION_SEND_MULTIPLE.equals(action)) {
                multi_stream = intent.getParcelableArrayListExtra((Intent.EXTRA_STREAM));
            } else {
                multi_stream = new ArrayList<Uri>();
                multi_stream.add((Uri) intent.getParcelableExtra(Intent.EXTRA_STREAM));
            }

            List<Serializable> params = new Vector<Serializable>();
            params.add(multi_stream);
            params.add(type);
            new processAttachmentsTask().execute(params);
        }
    }

    public void savePostContent(boolean isAutoSave) {

        Post post = mActivity.getPost();

        if (post == null)
            return;

        String title = (mTitleEditText.getText() != null) ? mTitleEditText.getText().toString() : "";
        String content;

        if (post.isLocalDraft() || !isAutoSave) {
            Editable e = mContentEditText.getText();
            if (android.os.Build.VERSION.SDK_INT >= 14 && e != null) {
                // remove suggestion spans, they cause craziness in
                // WPHtml.toHTML().
                CharacterStyle[] characterStyles = e.getSpans(0, e.length(), CharacterStyle.class);
                for (CharacterStyle characterStyle : characterStyles) {
                    if (characterStyle.getClass().getName().equals("android.text.style.SuggestionSpan"))
                        e.removeSpan(characterStyle);
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
            content = (mContentEditText.getText() != null) ? mContentEditText.getText().toString() : "";
        }

        String images = "";
        // update the images
        post.deleteMediaFiles();

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

            for (WPImageSpan wpIS : imageSpans) {
                images += wpIS.getImageSource().toString() + ",";

                if (wpIS.getMediaId() != null) {
                    updateMediaFileOnServer(wpIS);
                } else {
                    MediaFile mf = new MediaFile();
                    mf.setBlogId(WordPress.getCurrentBlog().getBlogId() + "");
                    mf.setPostID(post.getId());
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

        post.setTitle(title);
        // split up the post content if there's a more tag
        if (post.isLocalDraft() && content.contains(moreTag)) {
            post.setDescription(content.substring(0, content.indexOf(moreTag)));
            post.setMt_text_more(content.substring(content.indexOf(moreTag) + moreTag.length(), content.length()));
        } else {
            post.setDescription(content);
            post.setMt_text_more("");
        }
        post.setMediaPaths(images);
        if (!post.isLocalDraft())
            post.setLocalChange(true);
        post.update();
    }

    public boolean hasEmptyContentFields() {
        return TextUtils.isEmpty(mTitleEditText.getText()) && TextUtils.isEmpty(mContentEditText.getText());
    }

    /**
     * Media
     */

    private class processAttachmentsTask extends AsyncTask<List<?>, Void, SpannableStringBuilder> {

        protected void onPreExecute() {
            Toast.makeText(getActivity(), R.string.loading, Toast.LENGTH_SHORT).show();
        }

        @Override
        protected SpannableStringBuilder doInBackground(List<?>... args) {
            ArrayList<?> multi_stream = (ArrayList<?>) args[0].get(0);
            String type = (String) args[0].get(1);
            SpannableStringBuilder ssb = new SpannableStringBuilder();
            for (Object streamUri : multi_stream) {
                if (streamUri instanceof Uri) {
                    Uri imageUri = (Uri) streamUri;
                    if (type != null) {
                        addMedia(imageUri, ssb);
                    }
                }
            }
            return ssb;
        }

        protected void onPostExecute(SpannableStringBuilder ssb) {
            if (ssb != null) {
                if (ssb.length() > 0) {
                    mContentEditText.setText(ssb);
                }
            } else {
                Toast.makeText(getActivity(), getResources().getText(R.string.gallery_error), Toast.LENGTH_SHORT).show();
            }
        }
    }

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
        mediaGallery.setIds(getActivity().getIntent().getStringArrayListExtra(NEW_MEDIA_GALLERY_EXTRA_IDS));

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

        int line, column = 0;
        if (mContentEditText.getLayout() != null) {
            line = mContentEditText.getLayout().getLineForOffset(selectionStart);
            column = mContentEditText.getSelectionStart() - mContentEditText.getLayout().getLineStart(line);
        }

        Editable s = mContentEditText.getText();
        if (s != null) {
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
        if (mActivity.getPost() != null)
            mf.setPostID(mActivity.getPost().getId());
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
                    if (s == null)
                        return;
                    WPImageSpan[] spans = s.getSpans(0, s.length(), WPImageSpan.class);
                    if (spans.length != 0) {
                        for (WPImageSpan is : spans) {
                            if (mediaId.equals(is.getMediaId()) && !is.isNetworkImageLoaded()) {

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

        int line, column = 0;
        if (mContentEditText.getLayout() != null) {
            line = mContentEditText.getLayout().getLineForOffset(selectionStart);
            column = mContentEditText.getSelectionStart() - mContentEditText.getLayout().getLineStart(line);
        }

        Editable s = mContentEditText.getText();
        if (s == null)
            return;
        MediaGalleryImageSpan[] gallerySpans = s.getSpans(selectionStart, selectionEnd, MediaGalleryImageSpan.class);
        if (gallerySpans.length != 0) {
            for (MediaGalleryImageSpan gallerySpan : gallerySpans) {
                if (gallerySpan.getMediaGallery().getUniqueId() == gallery.getUniqueId()) {
                    // replace the existing span with a new gallery, re-add it to the same position.
                    gallerySpan.setMediaGallery(gallery);
                    int spanStart = s.getSpanStart(gallerySpan);
                    int spanEnd = s.getSpanEnd(gallerySpan);
                    s.setSpan(gallerySpan, spanStart, spanEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
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
            if (imageUri.getEncodedPath() != null)
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
            if (s == null)
                return false;
            WPImageSpan is = new WPImageSpan(getActivity(), resizedBitmap, imageUri);

            MediaUtils.setWPImageSpanWidth(getActivity(), imageUri, is);

            is.setTitle((String) mediaData.get("title"));
            is.setImageSource(imageUri);
            if (imageUri.getEncodedPath() != null)
                is.setVideo(imageUri.getEncodedPath().contains("video"));

            int line, column = 0;
            if (mContentEditText.getLayout() != null) {
                line = mContentEditText.getLayout().getLineForOffset(selectionStart);
                column = mContentEditText.getSelectionStart() - mContentEditText.getLayout().getLineStart(line);
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
                onFormatButtonClick(mBoldToggleButton, TAG_FORMAT_BAR_BUTTON_STRONG);
            } else if (id == R.id.em) {
                onFormatButtonClick(mEmToggleButton, TAG_FORMAT_BAR_BUTTON_EM);
            } else if (id == R.id.underline) {
                onFormatButtonClick(mUnderlineToggleButton, TAG_FORMAT_BAR_BUTTON_UNDERLINE);
            } else if (id == R.id.strike) {
                onFormatButtonClick(mStrikeToggleButton, TAG_FORMAT_BAR_BUTTON_STRIKE);
            } else if (id == R.id.bquote) {
                onFormatButtonClick(mBquoteToggleButton, TAG_FORMAT_BAR_BUTTON_QUOTE);
            } else if (id == R.id.more) {
                mSelectionEnd = mContentEditText.getSelectionEnd();
                Editable str = mContentEditText.getText();
                if (str != null)
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
                    if (mContentEditText.getText() != null) {
                        String selectedText = mContentEditText.getText().subSequence(mSelectionStart, mSelectionEnd).toString();
                        i.putExtra("selectedText", selectedText);
                    }
                }
                startActivityForResult(i, ACTIVITY_REQUEST_CODE_CREATE_LINK);
            } else if (id == R.id.addPictureButton) {
                mAddPictureButton.performLongClick();
            }
        }
    };

    /**
     * Applies formatting to selected text, or marks the entry for a new text style
     * at the current cursor position
     * @param toggleButton button from formatting bar
     * @param tag HTML tag name for text style
     */
    private void onFormatButtonClick(ToggleButton toggleButton, String tag) {
            Spannable s = mContentEditText.getText();
            if (s == null)
                return;
            int selectionStart = mContentEditText.getSelectionStart();
            mStyleStart = selectionStart;
            int selectionEnd = mContentEditText.getSelectionEnd();

            if (selectionStart > selectionEnd) {
                int temp = selectionEnd;
                selectionEnd = selectionStart;
                selectionStart = temp;
            }

        Class styleClass = null;
        if (tag.equals(TAG_FORMAT_BAR_BUTTON_STRONG) || tag.equals(TAG_FORMAT_BAR_BUTTON_EM))
                styleClass = StyleSpan.class;
        else if (tag.equals(TAG_FORMAT_BAR_BUTTON_UNDERLINE))
                styleClass = WPUnderlineSpan.class;
        else if (tag.equals(TAG_FORMAT_BAR_BUTTON_STRIKE))
                styleClass = StrikethroughSpan.class;
        else if (tag.equals(TAG_FORMAT_BAR_BUTTON_QUOTE))
                styleClass = QuoteSpan.class;

        if (styleClass == null)
            return;

        Object[] allSpans = s.getSpans(selectionStart, selectionEnd, styleClass);
        boolean textIsSelected = selectionEnd > selectionStart;
        if (mActivity.getPost().isLocalDraft()) {
            // Local drafts can use the rich text editor. Yay!
            boolean shouldAddSpan = true;
            for (Object span : allSpans) {
                if (span instanceof StyleSpan) {
                    StyleSpan styleSpan = (StyleSpan)span;
                    if ((styleSpan.getStyle() == Typeface.BOLD && !tag.equals(TAG_FORMAT_BAR_BUTTON_STRONG))
                            || (styleSpan.getStyle() == Typeface.ITALIC && !tag.equals(TAG_FORMAT_BAR_BUTTON_EM))) {
                        continue;
                    }
                }
                if (!toggleButton.isChecked() && textIsSelected) {
                    // If span exists and text is selected, remove the span
                    s.removeSpan(span);
                    shouldAddSpan = false;
                    break;
                } else if (!toggleButton.isChecked()) {
                    // Remove span at cursor point if button isn't checked
                    Object[] spans = s.getSpans(mStyleStart - 1, mStyleStart, styleClass);
                    for (Object removeSpan : spans) {
                        selectionStart = s.getSpanStart(removeSpan);
                        selectionEnd = s.getSpanEnd(removeSpan);
                        s.removeSpan(removeSpan);
                    }
                }
            }

            if (shouldAddSpan) {
                if (tag.equals(TAG_FORMAT_BAR_BUTTON_STRONG)) {
                        s.setSpan(new StyleSpan(android.graphics.Typeface.BOLD), selectionStart, selectionEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                } else if (tag.equals(TAG_FORMAT_BAR_BUTTON_EM)) {
                        s.setSpan(new StyleSpan(android.graphics.Typeface.ITALIC), selectionStart, selectionEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                } else {
                    try {
                        s.setSpan(styleClass.newInstance(), selectionStart, selectionEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    } catch (java.lang.InstantiationException e) {
                        e.printStackTrace();
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    }
                }
            }
        } else {
            // Add HTML tags when editing an existing post
            String startTag = "<" + tag + ">";
            String endTag = "</" + tag + ">";
            Editable content = mContentEditText.getText();
            if (textIsSelected) {
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
                if (s == null)
                    return false;
                // check if image span was tapped
                WPImageSpan[] image_spans = s.getSpans(charPosition, charPosition, WPImageSpan.class);

                if (image_spans.length != 0) {
                    final WPImageSpan span = image_spans[0];
                    if (!span.isVideo()) {
                        LayoutInflater factory = LayoutInflater.from(getActivity());
                        final View alertView = factory.inflate(R.layout.alert_image_options, null);
                        if (alertView == null)
                            return false;
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

                                        String title = (titleText.getText() != null) ? titleText.getText().toString() : "";
                                        span.setTitle(title);
                                        // span.setDescription(descText.getText().toString());
                                        span.setHorizontalAlignment(alignmentSpinner.getSelectedItemPosition());
                                        span.setWidth(getEditTextIntegerClamped(imageWidthText, 10, maxWidth));
                                        String captionText = (caption.getText() != null) ? caption.getText().toString() : "";
                                        span.setCaption(captionText);
                                        span.setFeatured(featuredCheckBox.isChecked());
                                        if (featuredCheckBox.isChecked()) {
                                            // remove featured flag from all
                                            // other images
                                            WPImageSpan[] click_spans = s.getSpans(0, s.length(), WPImageSpan.class);
                                            if (click_spans.length > 1) {
                                                for (WPImageSpan verifySpan : click_spans) {
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
                    if (selectionStart >= 0 && mContentEditText.getSelectionEnd() >= selectionStart)
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

        int position = Selection.getSelectionStart(mContentEditText.getText());
        if ((mIsBackspace && position != 1) || mLastPosition == position || !mActivity.getPost().isLocalDraft())
            return;

        if (position < 0) {
            position = 0;
        }
        mLastPosition = position;
        if (position > 0) {

            if (mStyleStart > position) {
                mStyleStart = position - 1;
            }

            boolean shouldBold = mBoldToggleButton.isChecked();
            boolean shouldEm = mEmToggleButton.isChecked();
            boolean shouldUnderline = mUnderlineToggleButton.isChecked();
            boolean shouldStrike = mStrikeToggleButton.isChecked();
            boolean shouldQuote = mBquoteToggleButton.isChecked();

            Object[] allSpans = s.getSpans(mStyleStart, position, Object.class);
            for (Object span : allSpans) {
                if (span instanceof StyleSpan) {
                    StyleSpan styleSpan = (StyleSpan) span;
                    if (styleSpan.getStyle() == Typeface.BOLD)
                        shouldBold = false;
                    else if (styleSpan.getStyle() == Typeface.ITALIC)
                        shouldEm = false;
                } else if (span instanceof WPUnderlineSpan) {
                    shouldUnderline = false;
                } else if (span instanceof StrikethroughSpan) {
                    shouldStrike = false;
                } else if (span instanceof QuoteSpan) {
                    shouldQuote = false;
                }
            }

            if (shouldBold)
                s.setSpan(new StyleSpan(android.graphics.Typeface.BOLD), mStyleStart, position, Spannable.SPAN_INCLUSIVE_INCLUSIVE);
            if (shouldEm)
                s.setSpan(new StyleSpan(android.graphics.Typeface.ITALIC), mStyleStart, position, Spannable.SPAN_INCLUSIVE_INCLUSIVE);
            if (shouldUnderline)
                s.setSpan(new WPUnderlineSpan(), mStyleStart, position, Spannable.SPAN_INCLUSIVE_INCLUSIVE);
            if (shouldStrike)
                s.setSpan(new StrikethroughSpan(), mStyleStart, position, Spannable.SPAN_INCLUSIVE_INCLUSIVE);
            if (shouldQuote)
                s.setSpan(new QuoteSpan(), mStyleStart, position, Spannable.SPAN_INCLUSIVE_INCLUSIVE);
        }
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        mIsBackspace = (count - after == 1) || (s.length() == 0);
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
    }

    @Override
    public void onSelectionChanged() {
        if (!mActivity.getPost().isLocalDraft())
            return;

        final Spannable s = mContentEditText.getText();
        if (s == null)
            return;
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
            if (editText.getText() != null)
                width = Integer.parseInt(editText.getText().toString().replace("px", ""));
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }
        width = Math.min(max, Math.max(width, min));
        return width;
    }
}
