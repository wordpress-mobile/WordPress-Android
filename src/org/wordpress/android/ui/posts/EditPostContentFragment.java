package org.wordpress.android.ui.posts;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Build.VERSION_CODES;
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
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
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
import org.wordpress.android.util.DisplayUtils;
import org.wordpress.android.util.ImageHelper;
import org.wordpress.android.util.MediaGalleryImageSpan;
import org.wordpress.android.util.StringUtils;
import org.wordpress.android.util.WPEditText;
import org.wordpress.android.util.WPHtml;
import org.wordpress.android.util.WPImageSpan;
import org.wordpress.android.util.WPMobileStatsUtil;
import org.wordpress.android.util.WPUnderlineSpan;
import org.wordpress.passcodelock.AppLockManager;
import org.xmlrpc.android.ApiHelper;

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

    EditPostActivity mActivity;

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

    private static final int CONTENT_ANIMATION_DURATION = 250;

    private View mRootView;
    private WPEditText mContentEditText;
    private Button mAddPictureButton;
    private EditText mTitleEditText;
    private ToggleButton mBoldToggleButton, mEmToggleButton, mBquoteToggleButton;
    private ToggleButton mUnderlineToggleButton, mStrikeToggleButton;
    private LinearLayout mFormatBar, mPostContentLinearLayout, mPostSettingsLinearLayout;
    private boolean mIsBackspace;
    private boolean mScrollDetected;

    private String mMediaCapturePath = "";

    private int mStyleStart, mSelectionStart, mSelectionEnd, mFullViewBottom;
    private int mLastPosition = -1, mQuickMediaType = -1;

    private float mLastYPos = 0;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mActivity = (EditPostActivity) getActivity();

        final ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.fragment_edit_post_content, container, false);

        mFormatBar = (LinearLayout) rootView.findViewById(R.id.format_bar);
        mTitleEditText = (EditText) rootView.findViewById(R.id.post_title);
        mTitleEditText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                // Go to full screen editor when 'next' button is tapped on soft keyboard
                if (actionId == EditorInfo.IME_ACTION_NEXT && mActivity.getSupportActionBar().isShowing()) {
                    setContentEditingModeVisible(true);
                }
                return false;
            }
        });
        mContentEditText = (WPEditText) rootView.findViewById(R.id.post_content);
        if (Build.VERSION.SDK_INT <= VERSION_CODES.GINGERBREAD_MR1) {
            mContentEditText.setBackgroundResource(android.R.drawable.editbox_background_normal);
            LinearLayout.LayoutParams layoutParams = (LinearLayout.LayoutParams) mContentEditText.getLayoutParams();
            int sideMargin = mActivity.getResources().getDimensionPixelSize(
                    R.dimen.post_editor_content_side_margin_gingerbread);
            layoutParams.setMargins(sideMargin, layoutParams.topMargin, sideMargin, layoutParams.bottomMargin);
            mContentEditText.setLayoutParams(layoutParams);
        }
        mPostContentLinearLayout = (LinearLayout) rootView.findViewById(R.id.post_content_wrapper);
        mPostSettingsLinearLayout = (LinearLayout) rootView.findViewById(R.id.post_settings_wrapper);
        Button postSettingsButton = (Button) rootView.findViewById(R.id.post_settings_button);
        postSettingsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mActivity.showPostSettings();
            }
        });
        mBoldToggleButton = (ToggleButton) rootView.findViewById(R.id.bold);
        mEmToggleButton = (ToggleButton) rootView.findViewById(R.id.em);
        mBquoteToggleButton = (ToggleButton) rootView.findViewById(R.id.bquote);
        mUnderlineToggleButton = (ToggleButton) rootView.findViewById(R.id.underline);
        mStrikeToggleButton = (ToggleButton) rootView.findViewById(R.id.strike);
        mAddPictureButton = (Button) rootView.findViewById(R.id.addPictureButton);
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
                // Bottom comparison is there to ensure that the keyboard is actually showing
                if (mRootView.getBottom() < mFullViewBottom && !mActivity.getSupportActionBar().isShowing()) {
                    setContentEditingModeVisible(false);
                }
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

            postSettingsButton.setText(post.isPage() ? R.string.page_settings : R.string.post_settings);
        }

        // Check for Android share action
        String action = mActivity.getIntent().getAction();
        if (mActivity.getIntent().getExtras() != null)
            mQuickMediaType = mActivity.getIntent().getExtras().getInt("quick-media", -1);
        if (Intent.ACTION_SEND.equals(action) || Intent.ACTION_SEND_MULTIPLE.equals(action))
            setPostContentFromShareAction();
        else if (NEW_MEDIA_GALLERY.equals(action))
            prepareMediaGallery();
        else if (NEW_MEDIA_POST.equals(action))
            prepareMediaPost();
        else if (mQuickMediaType >= 0) {
            // User selected a 'Quick (media type)' option in the menu drawer
            if (mQuickMediaType == Constants.QUICK_POST_PHOTO_CAMERA)
                launchCamera();
            else if (mQuickMediaType == Constants.QUICK_POST_PHOTO_LIBRARY)
                launchPictureLibrary();
            else if (mQuickMediaType == Constants.QUICK_POST_VIDEO_CAMERA)
                launchVideoCamera();
            else if (mQuickMediaType == Constants.QUICK_POST_VIDEO_LIBRARY)
                launchVideoLibrary();

            if (post != null) {
                if (mQuickMediaType == Constants.QUICK_POST_PHOTO_CAMERA || mQuickMediaType == Constants.QUICK_POST_PHOTO_LIBRARY)
                    post.setQuickPostType(Post.QUICK_MEDIA_TYPE_PHOTO);
                else if (mQuickMediaType == Constants.QUICK_POST_VIDEO_CAMERA || mQuickMediaType == Constants.QUICK_POST_VIDEO_LIBRARY)
                    post.setQuickPostType(Post.QUICK_MEDIA_TYPE_VIDEO);
            }
        }

        return rootView;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mRootView = view;
        mRootView.getViewTreeObserver().addOnGlobalLayoutListener(mGlobalLayoutListener);
    }

    private ViewTreeObserver.OnGlobalLayoutListener mGlobalLayoutListener = new ViewTreeObserver.OnGlobalLayoutListener() {
        public void onGlobalLayout() {
            mRootView.getViewTreeObserver().removeGlobalOnLayoutListener(this);
            mFullViewBottom = mRootView.getBottom();
        }
    };

    public void setContentEditingModeVisible(boolean isVisible) {
        if (mActivity == null)
            return;
        ActionBar actionBar = mActivity.getSupportActionBar();
        if (isVisible) {
            Animation fadeAnimation = new AlphaAnimation(1, 0);
            fadeAnimation.setDuration(CONTENT_ANIMATION_DURATION);
            fadeAnimation.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {
                    mTitleEditText.setVisibility(View.GONE);
                }

                @Override
                public void onAnimationEnd(Animation animation) {
                    mPostSettingsLinearLayout.setVisibility(View.GONE);
                    mFormatBar.setVisibility(View.VISIBLE);
                }

                @Override
                public void onAnimationRepeat(Animation animation) {

                }
            });

            mPostContentLinearLayout.startAnimation(fadeAnimation);
            actionBar.hide();
        } else {
            mTitleEditText.setVisibility(View.VISIBLE);
            mFormatBar.setVisibility(View.GONE);
            Animation fadeAnimation = new AlphaAnimation(0, 1);
            fadeAnimation.setDuration(CONTENT_ANIMATION_DURATION);
            fadeAnimation.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {

                }

                @Override
                public void onAnimationEnd(Animation animation) {
                    mPostSettingsLinearLayout.setVisibility(View.VISIBLE);
                }

                @Override
                public void onAnimationRepeat(Animation animation) {

                }
            });
            mPostContentLinearLayout.startAnimation(fadeAnimation);
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
                    fetchMedia(imageUri);
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
                    } else if (mActivity != null && mQuickMediaType > -1 && TextUtils.isEmpty(mContentEditText.getText())) {
                        // Quick Photo was cancelled, delete post and finish activity
                        mActivity.getPost().delete();
                        mActivity.finish();
                    }
                    break;
                case MediaUtils.RequestCode.ACTIVITY_REQUEST_CODE_VIDEO_LIBRARY:
                    Uri videoUri = data.getData();
                    fetchMedia(videoUri);
                    break;
                case MediaUtils.RequestCode.ACTIVITY_REQUEST_CODE_TAKE_VIDEO:
                    if (resultCode == Activity.RESULT_OK) {
                        Uri capturedVideoUri = MediaUtils.getLastRecordedVideoUri(getActivity());
                        if (!addMedia(capturedVideoUri, null))
                            Toast.makeText(getActivity(), getResources().getText(R.string.gallery_error), Toast.LENGTH_SHORT).show();
                    } else if (mActivity != null && mQuickMediaType > -1 && TextUtils.isEmpty(mContentEditText.getText())) {
                        // Quick Photo was cancelled, delete post and finish activity
                        mActivity.getPost().delete();
                        mActivity.finish();
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

        // Check for shared text
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
        }

        // Check for shared media
        if (intent.hasExtra(Intent.EXTRA_STREAM)) {
            String action = intent.getAction();
            String type = intent.getType();
            ArrayList<Uri> sharedUris;

            if (Intent.ACTION_SEND_MULTIPLE.equals(action)) {
                sharedUris = intent.getParcelableArrayListExtra((Intent.EXTRA_STREAM));
            } else {
                // For a single media share, we only allow images and video types
                if (type != null && (type.startsWith("image") || type.startsWith("video"))) {
                    sharedUris = new ArrayList<Uri>();
                    sharedUris.add((Uri) intent.getParcelableExtra(Intent.EXTRA_STREAM));
                } else {
                    return;
                }
            }

            if (sharedUris != null) {
                List<Serializable> params = new Vector<Serializable>();
                params.add(sharedUris);
                params.add(type);
                new processAttachmentsTask().execute(params);
            }
        }
    }

    public void savePostContent(boolean isAutoSave) {

        Post post = mActivity.getPost();

        if (post == null || mContentEditText.getText() == null)
            return;

        String title = (mTitleEditText.getText() != null) ? mTitleEditText.getText().toString() : "";
        String content;

        Editable postContentEditable;
        try {
            postContentEditable = new SpannableStringBuilder(mContentEditText.getText());
        } catch (IndexOutOfBoundsException e) {
            // A core android bug might cause an out of bounds exception, if so we'll just use the current editable
            // See https://code.google.com/p/android/issues/detail?id=5164
            postContentEditable = mContentEditText.getText();
        }

        if (postContentEditable == null)
            return;

        if (post.isLocalDraft()) {
            if (android.os.Build.VERSION.SDK_INT >= 14) {
                // remove suggestion spans, they cause craziness in WPHtml.toHTML().
                CharacterStyle[] characterStyles = postContentEditable.getSpans(0, postContentEditable.length(), CharacterStyle.class);
                for (CharacterStyle characterStyle : characterStyles) {
                    if (characterStyle.getClass().getName().equals("android.text.style.SuggestionSpan"))
                        postContentEditable.removeSpan(characterStyle);
                }
            }
            content = WPHtml.toHtml(postContentEditable);
            // replace duplicate <p> tags so there's not duplicates, trac #86
            content = content.replace("<p><p>", "<p>");
            content = content.replace("</p></p>", "</p>");
            content = content.replace("<br><br>", "<br>");
            // sometimes the editor creates extra tags
            content = content.replace("</strong><strong>", "").replace("</em><em>", "").replace("</u><u>", "")
                    .replace("</strike><strike>", "").replace("</blockquote><blockquote>", "");
        } else {
            //content = (mContentEditText.getText() != null) ? mContentEditText.getText().toString() : "";
            WPImageSpan[] imageSpans = postContentEditable.getSpans(0, postContentEditable.length(), WPImageSpan.class);
            if (imageSpans.length != 0) {

                for (WPImageSpan wpIS : imageSpans) {
                    //images += wpIS.getImageSource().toString() + ",";
                    MediaFile mediaFile = wpIS.getMediaFile();
                    if (mediaFile == null)
                        continue;
                    if (mediaFile.getMediaId() != null) {
                        updateMediaFileOnServer(wpIS);
                    } else {
                        mediaFile.setFileName(wpIS.getImageSource().toString());
                        mediaFile.setFilePath(wpIS.getImageSource().toString());
                        mediaFile.save();
                    }

                    int tagStart = postContentEditable.getSpanStart(wpIS);
                    if (!isAutoSave) {
                        postContentEditable.removeSpan(wpIS);

                        // network image has a mediaId
                        if (mediaFile.getMediaId() != null && mediaFile.getMediaId().length() > 0) {
                            postContentEditable.insert(tagStart, WPHtml.getContent(wpIS));

                        } else { // local image for upload
                            postContentEditable.insert(tagStart, "<img android-uri=\"" + wpIS.getImageSource().toString() + "\" />");
                        }
                    }
                }
            }
            content = postContentEditable.toString();
        }

        if (!isAutoSave) {
            // Add gallery shortcode
            MediaGalleryImageSpan[] gallerySpans = postContentEditable.getSpans(0, postContentEditable.length(), MediaGalleryImageSpan.class);
            for (MediaGalleryImageSpan gallerySpan : gallerySpans) {
                int start = postContentEditable.getSpanStart(gallerySpan);
                postContentEditable.removeSpan(gallerySpan);
                postContentEditable.insert(start, WPHtml.getGalleryShortcode(gallerySpan));
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

        if (!post.isLocalDraft())
            post.setLocalChange(true);
        post.update();
    }

    public boolean hasEmptyContentFields() {
        return TextUtils.isEmpty(mTitleEditText.getText()) && TextUtils.isEmpty(mContentEditText.getText());
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        mFullViewBottom = mRootView.getBottom();
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
            if (ssb != null && ssb.length() > 0) {
                Editable postContentEditable = mContentEditText.getText();
                if (postContentEditable != null)
                    postContentEditable.insert(0, ssb);
            } else {
                Toast.makeText(getActivity(), getResources().getText(R.string.gallery_error), Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void launchPictureLibrary() {
        MediaUtils.launchPictureLibrary(this);
        AppLockManager.getInstance().setExtendedTimeout();
    }

    private void launchCamera() {
        MediaUtils.launchCamera(this, new MediaUtils.LaunchCameraCallback() {

            @Override
            public void onMediaCapturePathReady(String mediaCapturePath) {
                mMediaCapturePath = mediaCapturePath;
                AppLockManager.getInstance().setExtendedTimeout();
            }
        });
    }

    private void launchVideoLibrary() {
        MediaUtils.launchVideoLibrary(this);
        AppLockManager.getInstance().setExtendedTimeout();
    }

    private void launchVideoCamera() {
        MediaUtils.launchVideoCamera(this);
        AppLockManager.getInstance().setExtendedTimeout();
    }

    private void fetchMedia(Uri mediaUri) {
        if (!MediaUtils.isInMediaStore(mediaUri)) {
            // Create an AsyncTask to download the file
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
                new DownloadMediaTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, mediaUri);
            else
                new DownloadMediaTask().execute(mediaUri);
        } else {
            // It is a regular local image file
            if (!addMedia(mediaUri, null))
                Toast.makeText(getActivity(), getResources().getText(R.string.gallery_error), Toast.LENGTH_SHORT).show();
        }
    }

    private class DownloadMediaTask extends AsyncTask<Uri, Integer, Uri> {

        @Override
        protected Uri doInBackground(Uri... uris) {
            Uri imageUri = uris[0];
            return MediaUtils.downloadExternalMedia(getActivity(), imageUri);
        }

        @Override
        protected void onPreExecute() {
            Toast.makeText(getActivity(), R.string.download, Toast.LENGTH_SHORT).show();
        }

        protected void onPostExecute(Uri newUri) {
            if (getActivity() == null)
                return;

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

        String blogId = String.valueOf(WordPress.getCurrentBlog().getLocalTableBlogId());

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
        }

        // load image from server
        loadWPImageSpanThumbnail(imageSpan);
    }

    private void updateMediaFileOnServer(WPImageSpan wpIS) {

        Blog currentBlog = WordPress.getCurrentBlog();
        if (currentBlog == null || wpIS == null)
            return;

        MediaFile mf = wpIS.getMediaFile();

        final String mediaId = mf.getMediaId();
        final String title = mf.getTitle();
        final String description = mf.getDescription();
        final String caption = mf.getCaption();

        ApiHelper.EditMediaItemTask task = new ApiHelper.EditMediaItemTask(mf.getMediaId(), mf.getTitle(),
                mf.getDescription(), mf.getCaption(),
                new ApiHelper.GenericCallback() {

                    @Override
                    public void onSuccess() {
                        if (WordPress.getCurrentBlog() == null) {
                            return;
                        }
                        String localBlogTableIndex = String.valueOf(WordPress.getCurrentBlog().getLocalTableBlogId());
                        WordPress.wpDB.updateMediaFile(localBlogTableIndex, mediaId, title, description, caption);
                    }

                    @Override
                    public void onFailure(ApiHelper.ErrorType errorType, String errorMessage, Throwable throwable) {
                        if (getActivity() != null && !isRemoving()) {
                            Toast.makeText(getActivity(), R.string.media_edit_failure, Toast.LENGTH_LONG).show();
                        }
                    }
                });

        List<Object> apiArgs = new ArrayList<Object>();
        apiArgs.add(currentBlog);
        task.execute(apiArgs);
    }

    /** Loads the thumbnail url in the imagespan from a server **/
    private void loadWPImageSpanThumbnail(WPImageSpan imageSpan) {
        final int maxPictureWidthForContentEditor = 400;
        final int minPictureWidthForContentEditor = 200;

        MediaFile mediaFile = imageSpan.getMediaFile();
        if (mediaFile == null)
            return;

        final String mediaId = mediaFile.getMediaId();
        if (mediaId == null)
            return;

        String imageURL;
        if (WordPress.getCurrentBlog() != null && WordPress.getCurrentBlog().isPhotonCapable()) {
            String photonUrl = imageSpan.getImageSource().toString();
            imageURL = StringUtils.getPhotonUrl(photonUrl, maxPictureWidthForContentEditor);
        } else {
            //Not a Jetpack or wpcom blog
           //imageURL = mediaFile.getThumbnailURL(); //do not use fileURL here since downloading picture of big dimensions can result in OOM Exception
            imageURL = mediaFile.getFileURL() != null ?  mediaFile.getFileURL() : mediaFile.getThumbnailURL();
        }

        if (imageURL == null)
            return;

        WordPress.imageLoader.get(imageURL, new ImageLoader.ImageListener() {

            @Override
            public void onErrorResponse(VolleyError arg0) {

            }

            @Override
            public void onResponse(ImageLoader.ImageContainer container, boolean arg1) {
                Bitmap downloadedBitmap = container.getBitmap();
                if (downloadedBitmap == null) {
                    //no bitmap downloaded from the server.
                    return;
                }

                if (downloadedBitmap.getWidth() < minPictureWidthForContentEditor) {
                    //Picture is too small. Show the placeholder in this case.
                    return;
                }

                Bitmap resizedBitmap;
                if (downloadedBitmap.getWidth() <= maxPictureWidthForContentEditor) {
                    //bitmap is already small in size, do not resize.
                    resizedBitmap = downloadedBitmap;
                } else {
                    //resize the downloaded bitmap
                    try {
                        ImageHelper ih = new ImageHelper();
                        resizedBitmap = ih.getThumbnailForWPImageSpan(downloadedBitmap, 400);
                    } catch (OutOfMemoryError er) {
                        return;
                    }
                }

                if (resizedBitmap == null)
                    return;

                Editable s = mContentEditText.getText();
                if (s == null)
                    return;
                WPImageSpan[] spans = s.getSpans(0, s.length(), WPImageSpan.class);
                if (spans.length != 0) {
                    for (WPImageSpan is : spans) {
                        MediaFile mediaFile = is.getMediaFile();
                        if (mediaFile == null)
                            continue;
                        if (mediaId.equals(mediaFile.getMediaId()) && !is.isNetworkImageLoaded()) {

                            // replace the existing span with a new one with the correct image, re-add it to the same position.
                            int spanStart = s.getSpanStart(is);
                            int spanEnd = s.getSpanEnd(is);
                            WPImageSpan imageSpan = new WPImageSpan(getActivity(), resizedBitmap, is.getImageSource());
                            imageSpan.setMediaFile(is.getMediaFile());
                            imageSpan.setNetworkImageLoaded(true);
                            s.removeSpan(is);
                            s.setSpan(imageSpan, spanStart, spanEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                            break;
                        }
                    }
                }

            }
        }, 0, 0);
    }

    private void startMediaGalleryActivity(MediaGallery mediaGallery) {
        Intent intent = new Intent(getActivity(), MediaGalleryActivity.class);
        intent.putExtra(MediaGalleryActivity.PARAMS_MEDIA_GALLERY, mediaGallery);
        if (mediaGallery == null)
            intent.putExtra(MediaGalleryActivity.PARAMS_LAUNCH_PICKER, true);
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
    }

    private boolean addMedia(Uri imageUri, SpannableStringBuilder ssb) {

        if (ssb != null && !MediaUtils.isInMediaStore(imageUri))
            imageUri = MediaUtils.downloadExternalMedia(getActivity(), imageUri);

        if (imageUri == null) {
            return false;
        }

        Bitmap thumbnailBitmap;
        String mediaTitle;
        if (imageUri.toString().contains("video") && !MediaUtils.isInMediaStore(imageUri)) {
            thumbnailBitmap = BitmapFactory.decodeResource(getActivity().getResources(), R.drawable.media_movieclip);
            mediaTitle = getResources().getString(R.string.video);
        } else {
            ImageHelper ih = new ImageHelper();
            Map<String, Object> mediaData = ih.getImageBytesForPath(imageUri.getEncodedPath(), getActivity());

            if (mediaData == null) {
                // data stream not returned
                return false;
            }

            thumbnailBitmap = ih.getThumbnailForWPImageSpan(getActivity(), (byte[]) mediaData.get("bytes"), (String) mediaData.get("orientation"));
            if (thumbnailBitmap == null)
                return false;

            mediaTitle = (String) mediaData.get("title");
        }

        WPImageSpan is = new WPImageSpan(getActivity(), thumbnailBitmap, imageUri);
        MediaFile mediaFile = is.getMediaFile();
        mediaFile.setPostID(mActivity.getPost().getId());
        mediaFile.setTitle(mediaTitle);
        mediaFile.setFilePath(is.getImageSource().toString());
        MediaUtils.setWPImageSpanWidth(getActivity(), imageUri, is);
        if (imageUri.getEncodedPath() != null)
            mediaFile.setVideo(imageUri.getEncodedPath().contains("video"));
        mediaFile.save();

        if (ssb != null) {
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
        }
        // Show the soft keyboard after adding media
        if (mActivity != null && !mActivity.getSupportActionBar().isShowing())
            ((InputMethodManager) mActivity.getSystemService(Context.INPUT_METHOD_SERVICE)).toggleSoftInput(InputMethodManager.SHOW_FORCED, InputMethodManager.HIDE_IMPLICIT_ONLY);
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
                trackFormatButtonClick(WPMobileStatsUtil.StatsPropertyPostDetailClickedKeyboardToolbarBoldButton);
            } else if (id == R.id.em) {
                onFormatButtonClick(mEmToggleButton, TAG_FORMAT_BAR_BUTTON_EM);
                trackFormatButtonClick(WPMobileStatsUtil.StatsPropertyPostDetailClickedKeyboardToolbarItalicButton);
            } else if (id == R.id.underline) {
                onFormatButtonClick(mUnderlineToggleButton, TAG_FORMAT_BAR_BUTTON_UNDERLINE);
                trackFormatButtonClick(WPMobileStatsUtil.StatsPropertyPostDetailClickedKeyboardToolbarUnderlineButton);
            } else if (id == R.id.strike) {
                onFormatButtonClick(mStrikeToggleButton, TAG_FORMAT_BAR_BUTTON_STRIKE);
                trackFormatButtonClick(WPMobileStatsUtil.StatsPropertyPostDetailClickedKeyboardToolbarDelButton);
            } else if (id == R.id.bquote) {
                onFormatButtonClick(mBquoteToggleButton, TAG_FORMAT_BAR_BUTTON_QUOTE);
                trackFormatButtonClick(WPMobileStatsUtil.StatsPropertyPostDetailClickedKeyboardToolbarBlockquoteButton);
            } else if (id == R.id.more) {
                mSelectionEnd = mContentEditText.getSelectionEnd();
                Editable str = mContentEditText.getText();
                if (str != null) {
                    if (mSelectionEnd > str.length())
                        mSelectionEnd = str.length();
                    str.insert(mSelectionEnd, "\n<!--more-->\n");
                }
                trackFormatButtonClick(WPMobileStatsUtil.StatsPropertyPostDetailClickedKeyboardToolbarMoreButton);
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
                trackFormatButtonClick(WPMobileStatsUtil.StatsPropertyPostDetailClickedKeyboardToolbarLinkButton);
                startActivityForResult(i, ACTIVITY_REQUEST_CODE_CREATE_LINK);
            } else if (id == R.id.addPictureButton) {
                mAddPictureButton.performLongClick();
                trackFormatButtonClick(WPMobileStatsUtil.StatsPropertyPostDetailClickedKeyboardToolbarPictureButton);
            }
        }
    };

    public void trackFormatButtonClick(String statPropertyName) {
        WPMobileStatsUtil.flagProperty(mActivity.getStatEventEditorClosed(), statPropertyName);
    }

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
            int scrollThreshold = DisplayUtils.dpToPx(getActivity(), 2);
            if (((mLastYPos - pos) > scrollThreshold) || ((pos - mLastYPos) > scrollThreshold))
                mScrollDetected = true;
        }

        mLastYPos = pos;

        if (event.getAction() == MotionEvent.ACTION_UP) {
            if (mActivity != null && mActivity.getSupportActionBar().isShowing()) {
                setContentEditingModeVisible(true);
                return false;
            }
        }

        if (event.getAction() == MotionEvent.ACTION_UP && !mScrollDetected) {
            Layout layout = ((TextView) v).getLayout();
            int x = (int) event.getX();
            int y = (int) event.getY();

            x += v.getScrollX();
            y += v.getScrollY();
            if (layout != null) {
                int line = layout.getLineForVertical(y);
                int charPosition = layout.getOffsetForHorizontal(line, x);

                Spannable s = mContentEditText.getText();
                if (s == null)
                    return false;
                // check if image span was tapped
                WPImageSpan[] image_spans = s.getSpans(charPosition, charPosition, WPImageSpan.class);

                if (image_spans.length != 0) {
                    final WPImageSpan span = image_spans[0];
                    MediaFile mediaFile = span.getMediaFile();
                    if (mediaFile == null)
                        return false;
                    if (!mediaFile.isVideo()) {
                        LayoutInflater factory = LayoutInflater.from(getActivity());
                        final View alertView = factory.inflate(R.layout.alert_image_options, null);
                        if (alertView == null)
                            return false;
                        final EditText imageWidthText = (EditText) alertView.findViewById(R.id.imageWidthText);
                        final EditText titleText = (EditText) alertView.findViewById(R.id.title);
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

                        imageWidthText.setText(String.valueOf(mediaFile.getWidth()) + "px");
                        seekBar.setProgress(mediaFile.getWidth());
                        titleText.setText(mediaFile.getTitle());
                        caption.setText(mediaFile.getCaption());
                        featuredCheckBox.setChecked(mediaFile.isFeatured());

                        if (mediaFile.isFeatured())
                            featuredInPostCheckBox.setVisibility(View.VISIBLE);
                        else
                            featuredInPostCheckBox.setVisibility(View.GONE);

                        featuredInPostCheckBox.setChecked(mediaFile.isFeaturedInPost());

                        alignmentSpinner.setSelection(mediaFile.getHorizontalAlignment(), true);

                        final int maxWidth = MediaUtils.getMinimumImageWidth(getActivity(), span.getImageSource());
                        seekBar.setMax(maxWidth / 10);
                        if (mediaFile.getWidth() != 0)
                            seekBar.setProgress(mediaFile.getWidth() / 10);
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
                                        MediaFile mediaFile = span.getMediaFile();
                                        if (mediaFile == null)
                                            return;
                                        mediaFile.setTitle(title);
                                        mediaFile.setHorizontalAlignment(alignmentSpinner.getSelectedItemPosition());
                                        mediaFile.setWidth(getEditTextIntegerClamped(imageWidthText, 10, maxWidth));
                                        String captionText = (caption.getText() != null) ? caption.getText().toString() : "";
                                        mediaFile.setCaption(captionText);
                                        mediaFile.setFeatured(featuredCheckBox.isChecked());
                                        if (featuredCheckBox.isChecked()) {
                                            // remove featured flag from all other images
                                            Spannable contentSpannable = mContentEditText.getText();
                                            WPImageSpan[] postImageSpans = contentSpannable.getSpans(0, contentSpannable.length(), WPImageSpan.class);
                                            if (postImageSpans.length > 1) {
                                                for (WPImageSpan postImageSpan : postImageSpans) {
                                                    if (postImageSpan != span) {
                                                        MediaFile postMediaFile = postImageSpan.getMediaFile();
                                                        postMediaFile.setFeatured(false);
                                                        postMediaFile.setFeaturedInPost(false);
                                                        postMediaFile.save();
                                                    }
                                                }
                                            }
                                        }
                                        mediaFile.setFeaturedInPost(featuredInPostCheckBox.isChecked());
                                        mediaFile.save();
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
        if (mActivity.getPost() == null || !mActivity.getPost().isLocalDraft())
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