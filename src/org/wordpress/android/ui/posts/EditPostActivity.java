package org.wordpress.android.ui.posts;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.lang.reflect.Type;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Vector;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.app.TimePickerDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.text.Editable;
import android.text.Html;
import android.text.Layout;
import android.text.Selection;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextWatcher;
import android.text.format.DateUtils;
import android.text.method.ArrowKeyMovementMethod;
import android.text.style.AlignmentSpan;
import android.text.style.CharacterStyle;
import android.text.style.QuoteSpan;
import android.text.style.StrikethroughSpan;
import android.text.style.StyleSpan;
import android.text.style.URLSpan;
import android.view.ContextMenu;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.AlphaAnimation;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.MenuInflater;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageLoader.ImageContainer;
import com.android.volley.toolbox.ImageLoader.ImageListener;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.json.JSONArray;
import org.xmlrpc.android.ApiHelper;

import org.wordpress.android.Constants;
import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.passcodelock.AppLockManager;
import org.wordpress.android.models.Blog;
import org.wordpress.android.models.MediaFile;
import org.wordpress.android.models.MediaGallery;
import org.wordpress.android.models.Post;
import org.wordpress.android.ui.media.MediaGalleryActivity;
import org.wordpress.android.ui.media.MediaGalleryPickerActivity;
import org.wordpress.android.ui.media.MediaUtils;
import org.wordpress.android.ui.media.MediaUtils.LaunchCameraCallback;
import org.wordpress.android.ui.media.MediaUtils.RequestCode;
import org.wordpress.android.util.DeviceUtils;
import org.wordpress.android.util.ImageHelper;
import org.wordpress.android.util.JSONUtil;
import org.wordpress.android.util.LocationHelper;
import org.wordpress.android.util.LocationHelper.LocationResult;
import org.wordpress.android.util.MediaGalleryImageSpan;
import org.wordpress.android.util.PostUploadService;
import org.wordpress.android.util.StringUtils;
import org.wordpress.android.util.WPEditText;
import org.wordpress.android.util.WPHtml;
import org.wordpress.android.util.WPImageSpan;
import org.wordpress.android.util.WPUnderlineSpan;

public class EditPostActivity extends SherlockFragmentActivity implements OnClickListener, OnTouchListener, TextWatcher,
        WPEditText.OnSelectionChangedListener, OnFocusChangeListener, WPEditText.EditTextImeBackListener {

    private static final int AUTOSAVE_DELAY_MILLIS = 60000;

    // Handled by MediaUtils
//    private static final int ACTIVITY_REQUEST_CODE_PICTURE_LIBRARY = 0;
//    private static final int ACTIVITY_REQUEST_CODE_TAKE_PHOTO = 1;
//    private static final int ACTIVITY_REQUEST_CODE_VIDEO_LIBRARY = 2;
//    private static final int ACTIVITY_REQUEST_CODE_TAKE_VIDEO = 3;
    private static final int ACTIVITY_REQUEST_CODE_CREATE_LINK = 4;
    private static final int ACTIVITY_REQUEST_CODE_SELECT_CATEGORIES = 5;

    private static final int ID_DIALOG_DATE = 0;
    private static final int ID_DIALOG_TIME = 1;
    private static final int ID_DIALOG_LOADING = 2;
    private static final int ID_DIALOG_DOWNLOAD = 3;

    private static final String CATEGORY_PREFIX_TAG = "category-";

    public static final String NEW_MEDIA_GALLERY = "NEW_MEDIA_GALLERY";
    public static final String NEW_MEDIA_GALLERY_EXTRA_IDS = "NEW_MEDIA_GALLERY_EXTRA_IDS";
    
    public static final String NEW_MEDIA_POST = "NEW_MEDIA_POST";
    public static final String NEW_MEDIA_POST_EXTRA = "NEW_MEDIA_POST_ID";

    private Blog mBlog;
    private Post mPost;
    // Used to restore post content if 'Discard' is chosen when leaving the editor.
    private Post mOriginalPost;

    private WPEditText mContentEditText;
    private ImageButton mAddPictureButton;
    private Spinner mStatusSpinner;
    private EditText mTitleEditText, mPasswordEditText, mTagsEditText, mExcerptEditText;
    private TextView mLocationText, mPubDateText;
    private ToggleButton mBoldToggleButton, mEmToggleButton, mBquoteToggleButton;
    private ToggleButton mUnderlineToggleButton, mStrikeToggleButton;
    private Button mPubDateButton, mLinkButton, mMoreButton;
    private RelativeLayout mFormatBar;

    private Location mCurrentLocation;
    private LocationHelper mLocationHelper;
    private Handler mAutoSaveHandler;
    private ArrayList<String> mCategories;

    private boolean mIsPage = false;
    private boolean mIsNew = false;
    private boolean mLocalDraft = false;
    private boolean mIsCustomPubDate = false;
    private boolean mIsBackspace = false;
    private boolean mScrollDetected = false;

    private String mAccountName = "";
    private int mQuickMediaType = -1;
    private String mMediaCapturePath = "";

    private String[] mPostFormats = null;
    private String[] mPostFormatTitles = null;

    private int mBlogID = -1;
    private long mPostID = -1;
    private long mCustomPubDate = 0;

    private int mYear, mMonth, mDay, mHour, mMinute;
    private int mStyleStart, mSelectionStart, mSelectionEnd;
    private int mLastPosition = -1;
    private int mCurrentActivityRequest = -1;

    private float mLastYPos = 0;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle extras = getIntent().getExtras();

        ActionBar actionBar = getSupportActionBar();
        actionBar.setHomeButtonEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);

        Calendar c = Calendar.getInstance();
        mYear = c.get(Calendar.YEAR);
        mMonth = c.get(Calendar.MONTH);
        mDay = c.get(Calendar.DAY_OF_MONTH);
        mHour = c.get(Calendar.HOUR_OF_DAY);
        mMinute = c.get(Calendar.MINUTE);
        mCategories = new ArrayList<String>();
        mAutoSaveHandler = new Handler();

        String action = getIntent().getAction();
        if (Intent.ACTION_SEND.equals(action) || Intent.ACTION_SEND_MULTIPLE.equals(action)) {
            // We arrived here from a share action
            setupTitleForShareAction();
        } else {
            initBlog();
            if (extras != null) {
                mAccountName = StringUtils.unescapeHTML(extras.getString("accountName"));
                mPostID = extras.getLong("postID");
                mLocalDraft = extras.getBoolean("localDraft", false);
                mIsPage = extras.getBoolean("isPage", false);
                mIsNew = extras.getBoolean("isNew", false);

                if (savedInstanceState != null) {
                    mCurrentActivityRequest = savedInstanceState.getInt("currentActivityRequest");
                    if (savedInstanceState.getString("mediaCapturePath") != null)
                        mMediaCapturePath = savedInstanceState.getString("mediaCapturePath");
                } else {
                    mQuickMediaType = extras.getInt("quick-media", -1);
                }

                if (extras.getBoolean("isQuickPress")) {
                    mBlogID = extras.getInt("id");
                } else {
                    mBlogID = WordPress.currentBlog.getId();
                }

                try {
                    mBlog = new Blog(mBlogID);
                    WordPress.currentBlog = mBlog;
                } catch (Exception e) {
                    showBlogErrorAndFinish();
                    return;
                }

                if (!mIsNew) {
                    try {
                        mPost = new Post(mBlogID, mPostID, mIsPage);
                        if (mPost == null) {
                            // big oopsie
                            Toast.makeText(this, getResources().getText(R.string.post_not_found), Toast.LENGTH_LONG).show();
                            finish();
                            return;
                        } else {
                            WordPress.currentPost = mPost;
                            mOriginalPost = new Post(mBlogID, mPostID, mIsPage);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        finish();
                    }
                }
            }

            if (mIsNew) {
                mLocalDraft = true;
                setTitle(StringUtils.unescapeHTML(WordPress.getCurrentBlog().getBlogName()) + " - "
                        + getString((mIsPage) ? R.string.new_page : R.string.new_post));
            } else {
                setTitle(StringUtils.unescapeHTML(WordPress.getCurrentBlog().getBlogName()) + " - "
                        + getString((mIsPage) ? R.string.edit_page : R.string.edit_post));
            }
        }

        setContentView(R.layout.edit);
        mContentEditText = (WPEditText) findViewById(R.id.postContent);
        mTitleEditText = (EditText) findViewById(R.id.title);
        mExcerptEditText = (EditText) findViewById(R.id.postExcerpt);
        mPasswordEditText = (EditText) findViewById(R.id.post_password);
        mLocationText = (TextView) findViewById(R.id.locationText);
        mBoldToggleButton = (ToggleButton) findViewById(R.id.bold);
        mEmToggleButton = (ToggleButton) findViewById(R.id.em);
        mBquoteToggleButton = (ToggleButton) findViewById(R.id.bquote);
        mUnderlineToggleButton = (ToggleButton) findViewById(R.id.underline);
        mStrikeToggleButton = (ToggleButton) findViewById(R.id.strike);
        mAddPictureButton = (ImageButton) findViewById(R.id.addPictureButton);
        mPubDateButton = (Button) findViewById(R.id.pubDateButton);
        mPubDateText = (TextView) findViewById(R.id.pubDate);
        mLinkButton = (Button) findViewById(R.id.link);
        mMoreButton = (Button) findViewById(R.id.more);
        mStatusSpinner = (Spinner) findViewById(R.id.status);
        mTagsEditText = (EditText) findViewById(R.id.tags);
        mFormatBar = (RelativeLayout) findViewById(R.id.formatBar);

        // Set header labels to upper case
        ((TextView) findViewById(R.id.categoryLabel)).setText(getResources().getString(R.string.categories).toUpperCase());
        ((TextView) findViewById(R.id.statusLabel)).setText(getResources().getString(R.string.status).toUpperCase());
        ((TextView) findViewById(R.id.postFormatLabel)).setText(getResources().getString(R.string.post_format).toUpperCase());
        ((TextView) findViewById(R.id.pubDateLabel)).setText(getResources().getString(R.string.publish_date).toUpperCase());

        if (mIsPage) { // remove post specific views
            mExcerptEditText.setVisibility(View.GONE);
            (findViewById(R.id.sectionTags)).setVisibility(View.GONE);
            (findViewById(R.id.sectionCategories)).setVisibility(View.GONE);
            (findViewById(R.id.sectionLocation)).setVisibility(View.GONE);
            (findViewById(R.id.postFormatLabel)).setVisibility(View.GONE);
            (findViewById(R.id.postFormat)).setVisibility(View.GONE);
        } else {
            if (mBlog.getPostFormats().equals("")) {
                List<Object> args = new Vector<Object>();
                args.add(mBlog);
                args.add(this);
                new ApiHelper.getPostFormatsTask().execute(args);
                mPostFormatTitles = getResources().getStringArray(R.array.post_formats_array);
                String defaultPostFormatTitles[] = {"aside", "audio", "chat", "gallery", "image", "link", "quote", "standard", "status",
                        "video"};
                mPostFormats = defaultPostFormatTitles;
            } else {
                try {
                    Gson gson = new Gson();
                    Type type = new TypeToken<Map<String, String>>() {
                    }.getType();
                    Map<String, String> jsonPostFormats = gson.fromJson(mBlog.getPostFormats(), type);
                    mPostFormats = new String[jsonPostFormats.size()];
                    mPostFormatTitles = new String[jsonPostFormats.size()];
                    int i = 0;
                    for (Map.Entry<String, String> entry : jsonPostFormats.entrySet()) {
                        String key = entry.getKey();
                        String val = entry.getValue();
                        mPostFormats[i] = key;
                        mPostFormatTitles[i] = val;
                        i++;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            Spinner pfSpinner = (Spinner) findViewById(R.id.postFormat);
            ArrayAdapter<String> pfAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, mPostFormatTitles);
            pfAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            pfSpinner.setAdapter(pfAdapter);
            String activePostFormat = "standard";
            if (!mIsNew) {
                try {
                    if (!mPost.getWP_post_format().equals(""))
                        activePostFormat = mPost.getWP_post_format();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            for (int i = 0; i < mPostFormats.length; i++) {
                if (mPostFormats[i].equals(activePostFormat))
                    pfSpinner.setSelection(i);
            }

            if (Intent.ACTION_SEND.equals(action) || Intent.ACTION_SEND_MULTIPLE.equals(action))
                setContent();
            else if (NEW_MEDIA_GALLERY.equals(action))
                prepareMediaGallery();
            else if (NEW_MEDIA_POST.equals(action))
                prepareMediaPost();
            
        }

        String[] items = new String[]{getResources().getString(R.string.publish_post), getResources().getString(R.string.draft),
                getResources().getString(R.string.pending_review), getResources().getString(R.string.post_private)};

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, items);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mStatusSpinner.setAdapter(adapter);

        getLocationProvider();

        if (mIsNew) {
            if (mQuickMediaType >= 0) {
                // User selected a 'Quick (media type)' option in the menu drawer
                if (mQuickMediaType == Constants.QUICK_POST_PHOTO_CAMERA)
                    launchCamera();
                else if (mQuickMediaType == Constants.QUICK_POST_PHOTO_LIBRARY)
                    launchPictureLibrary();
                else if (mQuickMediaType == Constants.QUICK_POST_VIDEO_CAMERA)
                    launchVideoCamera();
                else if (mQuickMediaType == Constants.QUICK_POST_VIDEO_LIBRARY)
                    launchVideoLibrary();
                mLocalDraft = extras.getBoolean("localDraft");
            }
        } else {
            mTitleEditText.setText(mPost.getTitle());
            mExcerptEditText.setText(mPost.getMt_excerpt());

            if (mPost.isUploaded()) {
                items = new String[]{
                        getResources().getString(R.string.publish_post),
                        getResources().getString(R.string.draft),
                        getResources().getString(R.string.pending_review),
                        getResources().getString(R.string.post_private)
                };
                adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item,
                        items);
                mStatusSpinner.setAdapter(adapter);
            }

            String contentHTML;

            if (!mPost.getMt_text_more().equals("")) {
                if (mPost.isLocalDraft())
                    contentHTML = mPost.getDescription() + "\n&lt;!--more--&gt;\n"
                            + mPost.getMt_text_more();
                else
                    contentHTML = mPost.getDescription() + "\n<!--more-->\n"
                            + mPost.getMt_text_more();
            } else
                contentHTML = mPost.getDescription();

            try {
                if (mPost.isLocalDraft())
                    mContentEditText.setText(WPHtml.fromHtml(contentHTML.replaceAll("\uFFFC", ""),
                            EditPostActivity.this, mPost));
                else
                    mContentEditText.setText(contentHTML.replaceAll("\uFFFC", ""));
            } catch (Exception e) {
                e.printStackTrace();
            }

            long pubDate = mPost.getDate_created_gmt();
            if (pubDate != 0) {
                try {
                    int flags = 0;
                    flags |= android.text.format.DateUtils.FORMAT_SHOW_DATE;
                    flags |= android.text.format.DateUtils.FORMAT_ABBREV_MONTH;
                    flags |= android.text.format.DateUtils.FORMAT_SHOW_YEAR;
                    flags |= android.text.format.DateUtils.FORMAT_SHOW_TIME;
                    String formattedDate = DateUtils.formatDateTime(EditPostActivity.this, pubDate,
                            flags);
                    mPubDateText.setText(formattedDate);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            if (mPost.getWP_password() != null)
                mPasswordEditText.setText(mPost.getWP_password());

            if (mPost.getPost_status() != null) {
                String status = mPost.getPost_status();

                if (status.equals("publish")) {
                    mStatusSpinner.setSelection(0, true);
                } else if (status.equals("draft")) {
                    mStatusSpinner.setSelection(1, true);
                } else if (status.equals("pending")) {
                    mStatusSpinner.setSelection(2, true);
                } else if (status.equals("private")) {
                    mStatusSpinner.setSelection(3, true);
                } else if (status.equals("localdraft")) {
                    mStatusSpinner.setSelection(0, true);
                }
            }

            if (!mIsPage) {
                if (mPost.getJSONCategories() != null) {
                    mCategories = JSONUtil.fromJSONArrayToStringList(mPost.getJSONCategories());
                }

                Double latitude = mPost.getLatitude();
                Double longitude = mPost.getLongitude();

                if (latitude != 0.0) {
                    new getAddressTask().execute(latitude, longitude);
                }
            }
            String tags = mPost.getMt_keywords();
            if (!tags.equals("")) {
                mTagsEditText.setText(tags);
            }
        }

        populateSelectedCategories();

        registerForContextMenu(mAddPictureButton);
        mContentEditText.setOnSelectionChangedListener(this);
        mContentEditText.setOnEditTextImeBackListener(this);
        mContentEditText.setOnTouchListener(this);
        mContentEditText.addTextChangedListener(this);
        mContentEditText.setOnFocusChangeListener(this);
        mAddPictureButton.setOnClickListener(this);
        mPubDateButton.setOnClickListener(this);
        mBoldToggleButton.setOnClickListener(this);
        mLinkButton.setOnClickListener(this);
        mEmToggleButton.setOnClickListener(this);
        mUnderlineToggleButton.setOnClickListener(this);
        mStrikeToggleButton.setOnClickListener(this);
        mBquoteToggleButton.setOnClickListener(this);
        mMoreButton.setOnClickListener(this);
    }

    private void prepareMediaGallery() {
        MediaGallery mediaGallery = new MediaGallery();
        mediaGallery.setIds(getIntent().getStringArrayListExtra("NEW_MEDIA_GALLERY_EXTRA_IDS"));
        
        startMediaGalleryActivity(mediaGallery);
    }

    private void prepareMediaPost() {
        String mediaId = getIntent().getStringExtra(NEW_MEDIA_POST_EXTRA);
        addExistingMediaToEditor(mediaId);
    }
    
    private void startMediaGalleryActivity(MediaGallery mediaGallery) {
        Intent intent = new Intent(EditPostActivity.this, MediaGalleryActivity.class);
        intent.putExtra(MediaGalleryActivity.PARAMS_MEDIA_GALLERY, mediaGallery);
        startActivityForResult(intent, MediaGalleryActivity.REQUEST_CODE);
    }
    
    private void startMediaGalleryAddActivity() {
        Intent intent = new Intent(EditPostActivity.this, MediaGalleryPickerActivity.class);
        intent.putExtra(MediaGalleryPickerActivity.PARAM_SELECT_ONE_ITEM, true);
        startActivityForResult(intent, MediaGalleryPickerActivity.REQUEST_CODE);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mAutoSaveHandler != null)
            mAutoSaveHandler.postDelayed(autoSaveRunnable, 60000);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mLocationHelper != null)
            mLocationHelper.cancelTimer();

        if (mAutoSaveHandler != null)
            mAutoSaveHandler.removeCallbacks(autoSaveRunnable);
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        savedInstanceState.putInt("currentActivityRequest", mCurrentActivityRequest);
        if (!mMediaCapturePath.equals(""))
            savedInstanceState.putString("mediaCapturePath", mMediaCapturePath);
    }

    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        menu.add(0, 0, 0, getResources().getText(R.string.select_photo));
        if (DeviceUtils.getInstance().hasCamera(getApplicationContext())) {
            menu.add(0, 1, 0, getResources().getText(R.string.media_add_popup_capture_photo));
        }
        menu.add(0, 2, 0, getResources().getText(R.string.select_video));
        if (DeviceUtils.getInstance().hasCamera(getApplicationContext())) {
            menu.add(0, 3, 0, getResources().getText(R.string.media_add_popup_capture_video));
        }
        
        menu.add(0, 4, 0, getResources().getText(R.string.media_add_new_media_gallery));
        menu.add(0, 5, 0, getResources().getText(R.string.select_from_media_library));
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

    @Override
    public boolean onCreateOptionsMenu(com.actionbarsherlock.view.Menu menu) {
        super.onCreateOptionsMenu(menu);
        MenuInflater inflater = getSupportMenuInflater();
        inflater.inflate(R.menu.edit, menu);
        return true;
    }

    // Menu actions
    @Override
    public boolean onOptionsItemSelected(com.actionbarsherlock.view.MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.menu_edit_post) {
            if (mAutoSaveHandler != null)
                mAutoSaveHandler.removeCallbacks(autoSaveRunnable);
            if (savePost(false, false)) {
                if (mQuickMediaType >= 0) {
                    if (mQuickMediaType == Constants.QUICK_POST_PHOTO_CAMERA || mQuickMediaType == Constants.QUICK_POST_PHOTO_LIBRARY)
                        mPost.setQuickPostType("QuickPhoto");
                    else if (mQuickMediaType == Constants.QUICK_POST_VIDEO_CAMERA || mQuickMediaType == Constants.QUICK_POST_VIDEO_LIBRARY)
                        mPost.setQuickPostType("QuickVideo");
                }
                WordPress.currentPost = mPost;
                PostUploadService.addPostToUpload(mPost);
                startService(new Intent(this, PostUploadService.class));
                Intent i = new Intent();
                i.putExtra("shouldRefresh", true);
                setResult(RESULT_OK, i);
                finish();
            }
            return true;
        } else if (itemId == android.R.id.home) {
            showCancelAlert(true);
            return true;
        }
        return false;
    }

    @Override
    public void onFocusChange(View v, boolean hasFocus) {
        if (hasFocus && mFormatBar.getVisibility() != View.VISIBLE)
            showFormatBar();
        else if (!hasFocus && mFormatBar.getVisibility() == View.VISIBLE)
            hideFormatBar();
    }

    @Override
    public void onImeBack(WPEditText ctrl, String text) {
        if (mFormatBar.getVisibility() == View.VISIBLE)
            hideFormatBar();
    }

    private void showFormatBar() {
        mFormatBar.setVisibility(View.VISIBLE);
        AlphaAnimation fadeInAnimation = new AlphaAnimation(0.0f, 1.0f);
        fadeInAnimation.setDuration(500);
        mFormatBar.startAnimation(fadeInAnimation);
    }

    private void hideFormatBar() {
        AlphaAnimation fadeOutAnimation = new AlphaAnimation(1.0f, 0.0f);
        fadeOutAnimation.setDuration(500);
        mFormatBar.startAnimation(fadeOutAnimation);
        mFormatBar.setVisibility(View.GONE);
    }

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
            Intent i = new Intent(EditPostActivity.this, EditLinkActivity.class);
            if (mSelectionEnd > mSelectionStart) {
                String selectedText = mContentEditText.getText().subSequence(mSelectionStart, mSelectionEnd).toString();
                i.putExtra("selectedText", selectedText);
            }
            startActivityForResult(i, ACTIVITY_REQUEST_CODE_CREATE_LINK);
        } else if (id == R.id.addPictureButton) {
            mAddPictureButton.performLongClick();
        } else if (id == R.id.pubDateButton) {
            showDialog(ID_DIALOG_DATE);
        } else if (id == R.id.selectCategories) {
            Bundle bundle = new Bundle();
            bundle.putInt("id", mBlogID);
            if (mCategories.size() > 0) {
                bundle.putSerializable("categories", new HashSet<String>(mCategories));
            }
            Intent categoriesIntent = new Intent(EditPostActivity.this, SelectCategoriesActivity.class);
            categoriesIntent.putExtras(bundle);
            startActivityForResult(categoriesIntent, ACTIVITY_REQUEST_CODE_SELECT_CATEGORIES);
        } else if (id == R.id.categoryButton) {
            onCategoryButtonClick(v);
        } else if (id == R.id.post) {
            if (mAutoSaveHandler != null)
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
                finish();
            }
        } else if (id == R.id.viewMap) {
            Double latitude = 0.0;
            try {
                latitude = mCurrentLocation.getLatitude();
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (latitude != 0.0) {
                String uri = "geo:" + latitude + "," + mCurrentLocation.getLongitude();
                startActivity(new Intent(android.content.Intent.ACTION_VIEW, Uri.parse(uri)));
            } else {
                Toast.makeText(EditPostActivity.this, getResources().getText(R.string.location_toast), Toast.LENGTH_SHORT).show();
            }
        } else if (id == R.id.updateLocation) {
            mLocationHelper.getLocation(EditPostActivity.this, locationResult);
        } else if (id == R.id.removeLocation) {
            if (mCurrentLocation != null) {
                mCurrentLocation.setLatitude(0.0);
                mCurrentLocation.setLongitude(0.0);
            }
            if (mPost != null) {
                mPost.setLatitude(0.0);
                mPost.setLongitude(0.0);
            }
            mLocationText.setText("");
        }
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {

        if (mFormatBar.getVisibility() != View.VISIBLE)
            showFormatBar();

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
                        LayoutInflater factory = LayoutInflater.from(EditPostActivity.this);
                        final View alertView = factory.inflate(R.layout.alert_image_options, null);
                        final TextView imageWidthText = (TextView) alertView.findViewById(R.id.imageWidthText);
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

                        featuredCheckBox.setOnCheckedChangeListener(new OnCheckedChangeListener() {
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
                        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(EditPostActivity.this, R.array.alignment_array,
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

                        seekBar.setMax(span.getWidth() / 10);
                        if (span.getWidth() != 0)
                            seekBar.setProgress(span.getWidth() / 10);
                        seekBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {

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

                        AlertDialog ad = new AlertDialog.Builder(EditPostActivity.this).setTitle(getString(R.string.image_settings))
                                .setView(alertView).setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int whichButton) {

                                        span.setTitle(titleText.getText().toString());
                                        // span.setDescription(descText.getText().toString());
                                        span.setHorizontalAlignment(alignmentSpinner.getSelectedItemPosition());
                                        span.setWidth(seekBar.getProgress() * 10);
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
                    mContentEditText.setSelection(mContentEditText.getSelectionStart());
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
    public void onBackPressed() {
        showCancelAlert(false);
    }

    private void showCancelAlert(final boolean isUpPress) {

        // Empty post? Let's not prompt then.
        if (mIsNew && mContentEditText.getText().toString().equals("") && mTitleEditText.getText().toString().equals("")) {
            finish();
            return;
        }

        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(EditPostActivity.this);
        dialogBuilder.setTitle(getString((mIsPage) ? R.string.edit_page : R.string.edit_post));
        dialogBuilder.setMessage(getString(R.string.prompt_save_changes));
        dialogBuilder.setPositiveButton(getResources().getText(R.string.save), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                savePost(false, true);
                Intent i = new Intent();
                i.putExtra("shouldRefresh", true);
                setResult(RESULT_OK, i);
                finish();
            }
        });
        dialogBuilder.setNeutralButton(getString(R.string.discard), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                // When discard options is chosen, restore existing post or delete new post if it was autosaved.
                if (mOriginalPost != null) {
                    mOriginalPost.update();
                    WordPress.currentPost = mOriginalPost;
                } else if (mPost != null && mIsNew) {
                    mPost.delete();
                    WordPress.currentPost = null;
                }
                finish();
            }
        });
        dialogBuilder.setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                dialog.dismiss();
            }
        });
        dialogBuilder.setCancelable(true);
        dialogBuilder.create().show();
    }

    private void initBlog() {
        if (WordPress.getCurrentBlog() == null)
            showBlogErrorAndFinish();
    }

    private void getLocationProvider() {
        boolean hasLocationProvider = false;
        LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        List<String> providers = locationManager.getProviders(true);
        for (String providerName : providers) {
            if (providerName.equals(LocationManager.GPS_PROVIDER) || providerName.equals(LocationManager.NETWORK_PROVIDER)) {
                hasLocationProvider = true;
            }
        }
        if (hasLocationProvider && mBlog.isLocation() && !mIsPage) {
            enableLBSButtons();
        }
    }

    private void setupTitleForShareAction() {
        mIsNew = true;
        mLocalDraft = true;
        
        mBlog = WordPress.getCurrentBlog();
        mBlogID = mBlog.getId();
        mAccountName = mBlog.getBlogName();
        WordPress.wpDB.updateLastBlogId(mBlogID);

        setTitle(StringUtils.unescapeHTML(mAccountName) + " - "
                + getResources().getText((mIsPage) ? R.string.new_page : R.string.new_post));
    }

    private void showBlogErrorAndFinish() {
        Toast.makeText(this, getResources().getText(R.string.blog_not_found), Toast.LENGTH_SHORT).show();
        finish();
    }

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

            if (mLocalDraft) {
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

    private void launchPictureLibrary() {
        mCurrentActivityRequest = RequestCode.ACTIVITY_REQUEST_CODE_PICTURE_LIBRARY;
        MediaUtils.launchPictureLibrary(this);
        AppLockManager.getInstance().setExtendedTimeout();
    }

    private void launchCamera() {
        MediaUtils.launchCamera(this, new LaunchCameraCallback() {
            
            @Override
            public void onMediaCapturePathReady(String mediaCapturePath) {
                mMediaCapturePath = mediaCapturePath;
                mCurrentActivityRequest = RequestCode.ACTIVITY_REQUEST_CODE_TAKE_PHOTO;
                AppLockManager.getInstance().setExtendedTimeout();
            }
        });
    }

    private void launchVideoLibrary() {
        mCurrentActivityRequest = RequestCode.ACTIVITY_REQUEST_CODE_VIDEO_LIBRARY;
        MediaUtils.launchVideoLibrary(this);
        AppLockManager.getInstance().setExtendedTimeout();
    }

    private void launchVideoCamera() {
        mCurrentActivityRequest = RequestCode.ACTIVITY_REQUEST_CODE_TAKE_VIDEO;
        MediaUtils.launchVideoCamera(this);
        AppLockManager.getInstance().setExtendedTimeout();
    }

    private LocationResult locationResult = new LocationResult() {
        @Override
        public void gotLocation(Location location) {
            if (location != null) {
                mCurrentLocation = location;
                new getAddressTask().execute(mCurrentLocation.getLatitude(), mCurrentLocation.getLongitude());
            } else {
                runOnUiThread(new Runnable() {
                    public void run() {
                        mLocationText.setText(getString(R.string.location_not_found));
                    }
                });
            }
        }
    };

    private void enableLBSButtons() {
        mLocationHelper = new LocationHelper();
        ((RelativeLayout) findViewById(R.id.sectionLocation)).setVisibility(View.VISIBLE);
        Button viewMap = (Button) findViewById(R.id.viewMap);
        Button updateLocation = (Button) findViewById(R.id.updateLocation);
        Button removeLocation = (Button) findViewById(R.id.removeLocation);
        updateLocation.setOnClickListener(this);
        removeLocation.setOnClickListener(this);
        viewMap.setOnClickListener(this);
        if (mIsNew)
            mLocationHelper.getLocation(EditPostActivity.this, locationResult);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_CANCELED) {
            if (mQuickMediaType >= 0) {
                setResult(Activity.RESULT_CANCELED, new Intent());
                finish();
            }
            if (mFormatBar.getVisibility() == View.VISIBLE)
                hideFormatBar();
            return;
        }

        if (data != null || ((requestCode == RequestCode.ACTIVITY_REQUEST_CODE_TAKE_PHOTO || requestCode == RequestCode.ACTIVITY_REQUEST_CODE_TAKE_VIDEO))) {
            Bundle extras;

            switch (requestCode) {
                case MediaGalleryActivity.REQUEST_CODE:
                    if (resultCode == RESULT_OK) {
                        handleMediaGalleryResult(data);
                    }
                    break;
                case MediaGalleryPickerActivity.REQUEST_CODE:
                    if (resultCode == RESULT_OK) {
                        handleMediaGalleryPickerResult(data);
                    }
                    break;
                case RequestCode.ACTIVITY_REQUEST_CODE_PICTURE_LIBRARY:
                    Uri imageUri = data.getData();
                    verifyImage(imageUri);
                    break;
                case RequestCode.ACTIVITY_REQUEST_CODE_TAKE_PHOTO:
                    if (resultCode == Activity.RESULT_OK) {
                        try {
                            File f = new File(mMediaCapturePath);
                            Uri capturedImageUri = Uri.fromFile(f);
                            f = null;
                            if (!addMedia(capturedImageUri, null))
                                Toast.makeText(EditPostActivity.this, getResources().getText(R.string.gallery_error), Toast.LENGTH_SHORT).show();
                            sendBroadcast(new Intent(Intent.ACTION_MEDIA_MOUNTED, Uri.parse("file://"
                                    + Environment.getExternalStorageDirectory())));
                        } catch (Exception e) {
                            e.printStackTrace();
                        } catch (OutOfMemoryError e) {
                            e.printStackTrace();
                        }
                    }
                    break;
                case RequestCode.ACTIVITY_REQUEST_CODE_VIDEO_LIBRARY:
                    Uri videoUri = data.getData();
                    if (!addMedia(videoUri, null))
                        Toast.makeText(EditPostActivity.this, getResources().getText(R.string.gallery_error), Toast.LENGTH_SHORT).show();
                    break;
                case RequestCode.ACTIVITY_REQUEST_CODE_TAKE_VIDEO:
                    if (resultCode == Activity.RESULT_OK) {
                        Uri capturedVideoUri = MediaUtils.getLastRecordedVideoUri(this);
                        if (!addMedia(capturedVideoUri, null))
                            Toast.makeText(EditPostActivity.this, getResources().getText(R.string.gallery_error), Toast.LENGTH_SHORT).show();
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
                            if (mLocalDraft) {
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
                case ACTIVITY_REQUEST_CODE_SELECT_CATEGORIES:
                    extras = data.getExtras();
                    mCategories = (ArrayList<String>) extras.getSerializable("selectedCategories");
                    populateSelectedCategories();
                    break;
            }
        }// end null check
    }
    
    private void handleMediaGalleryPickerResult(Intent data) {
        ArrayList<String> ids = data.getStringArrayListExtra(MediaGalleryPickerActivity.RESULT_IDS);
        if (ids == null || ids.size() == 0)
            return;
        
        String mediaId = ids.get(0);
        addExistingMediaToEditor(mediaId);
    }
    
    private void addExistingMediaToEditor(String mediaId) {
        if (WordPress.getCurrentBlog() == null)
            return;
        
        String blogId = String.valueOf(WordPress.getCurrentBlog().getBlogId());
        
        WPImageSpan imageSpan = prepareWPImageSpan(blogId, mediaId);
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

    private WPImageSpan prepareWPImageSpan(String blogId, final String mediaId) {
        Cursor cursor = WordPress.wpDB.getMediaFile(blogId, mediaId);
        if (cursor == null || !cursor.moveToFirst())
            return null; 

        String url = cursor.getString(cursor.getColumnIndex("fileURL"));
        if (url == null)
            return null;
        
        Uri uri = Uri.parse(url);
        WPImageSpan imageSpan = new WPImageSpan(EditPostActivity.this, R.drawable.remote_image, uri);
        imageSpan.setMediaId(mediaId);
        imageSpan.setCaption(cursor.getString(cursor.getColumnIndex("caption")));
        imageSpan.setDescription(cursor.getString(cursor.getColumnIndex("description")));
        imageSpan.setTitle(cursor.getString(cursor.getColumnIndex("title")));
        imageSpan.setWidth(cursor.getInt(cursor.getColumnIndex("width")));
        imageSpan.setHeight(cursor.getInt(cursor.getColumnIndex("height")));
        imageSpan.setMimeType(cursor.getString(cursor.getColumnIndex("mimeType")));
        imageSpan.setFileName(cursor.getString(cursor.getColumnIndex("fileName")));
        imageSpan.setThumbnailURL(cursor.getString(cursor.getColumnIndex("thumbnailURL")));
        imageSpan.setDateCreatedGMT(cursor.getLong(cursor.getColumnIndex("date_created_gmt")));
        
        boolean isVideo = false;
        String mimeType = cursor.getString(cursor.getColumnIndex("mimeType"));
        if (mimeType != null && mimeType.contains("video"))
            isVideo = true;
        imageSpan.setVideo(isVideo);
        cursor.close();
        
        return imageSpan;
    }

    /** Loads the thumbnail url in the imagespan from a server **/
    private void loadWPImageSpanThumbnail(WPImageSpan imageSpan) {
        final String mediaId = imageSpan.getMediaId();
        final String thumbnailUrl = imageSpan.getThumbnailURL();
        
        if (thumbnailUrl == null || mediaId == null)
            return;

        WordPress.imageLoader.get(thumbnailUrl, new ImageListener() {
            
            @Override
            public void onErrorResponse(VolleyError arg0) {
                
            }
            
            @Override
            public void onResponse(ImageContainer container, boolean arg1) {
                if (container.getBitmap() != null) {

                    Bitmap bitmap = container.getBitmap();

                    Editable s = mContentEditText.getText();
                    WPImageSpan[] spans = s.getSpans(0, s.length(), WPImageSpan.class);
                    if (spans.length != 0) {
                        for (WPImageSpan is : spans) {
                            if (mediaId != null && mediaId.equals(is.getMediaId()) && !is.isNetworkImageLoaded()) {
                                    
                                // replace the existing span with a new one with the correct image, re-add it to the same position.
                                int spanStart = s.getSpanStart(is);
                                int spanEnd = s.getSpanEnd(is);
                                WPImageSpan imageSpan = new WPImageSpan(EditPostActivity.this, bitmap, is.getImageSource());
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
        MediaGalleryImageSpan is = new MediaGalleryImageSpan(EditPostActivity.this, gallery);
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

    private void verifyImage(Uri imageUri) {
        if (isPicasaImage(imageUri)) {
            // Create an AsyncTask to download the file
            new DownloadImageTask().execute(imageUri);
        } else {
            // It is a regular local image file
            if (!addMedia(imageUri, null))
            Toast.makeText(EditPostActivity.this, getResources().getText(R.string.gallery_error), Toast.LENGTH_SHORT).show();
        }
    }

    private boolean isPicasaImage(Uri imageUri) {
        // Check if the imageUri returned is of picasa or not
        if (imageUri.toString().startsWith("content://com.android.gallery3d.provider")) {
            // Use the com.google provider for devices prior to 3.0
            imageUri = Uri.parse(imageUri.toString().replace("com.android.gallery3d", "com.google.android.gallery3d"));
        }

        if (imageUri.toString().startsWith("content://com.google.android.gallery3d"))
            return true;
        else
            return false;
    }

    private class DownloadImageTask extends AsyncTask<Uri, Integer, Uri> {

        @Override
        protected Uri doInBackground(Uri... uris) {
            Uri imageUri = uris[0];
            return downloadExternalImage(imageUri);
        }

        @Override
        protected void onPreExecute() {
            showDialog(ID_DIALOG_DOWNLOAD);
        }

        protected void onPostExecute(Uri newUri) {
            dismissDialog(ID_DIALOG_DOWNLOAD);
            if (newUri != null)
                addMedia(newUri, null);
            else
                Toast.makeText(getApplicationContext(), getString(R.string.error_downloading_image), Toast.LENGTH_SHORT).show();
        }
    }

    private Uri downloadExternalImage(Uri imageUri) {
        File cacheDir;

        // If the device has an SD card
        if (android.os.Environment.getExternalStorageState().equals(android.os.Environment.MEDIA_MOUNTED))
            cacheDir = new File(android.os.Environment.getExternalStorageDirectory() + "/WordPress/images");
        else {
            // If no SD card
            cacheDir = getApplicationContext().getCacheDir();
        }

        if (!cacheDir.exists())
            cacheDir.mkdirs();
        Random r = new Random();
        final String path = "wp-" + r.nextInt(400) + r.nextInt(400) + ".jpg";

        File f = new File(cacheDir, path);

        try {
            InputStream input;
            // Download the file
            if (imageUri.toString().startsWith("content://com.google.android.gallery3d")) {
                input = getContentResolver().openInputStream(imageUri);
            } else {
                input = new URL(imageUri.toString()).openStream();
            }
            OutputStream output = new FileOutputStream(f);

            byte data[] = new byte[1024];
            int count;
            while ((count = input.read(data)) != -1) {
                output.write(data, 0, count);
            }

            output.flush();
            output.close();
            input.close();

            Uri newUri = Uri.fromFile(f);
            return newUri;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    private void onCategoryButtonClick(View v) {
        // Get category name by removing prefix from the tag
        boolean listChanged = false;
        String categoryName = (String) v.getTag();
        categoryName = categoryName.replaceFirst(CATEGORY_PREFIX_TAG, "");

        // Remove clicked category from list
        for (int i = 0; i < mCategories.size(); i++) {
            if (mCategories.get(i).equals(categoryName)) {
                mCategories.remove(i);
                listChanged = true;
                break;
            }
        }

        // Recreate category views
        if (listChanged) {
            populateSelectedCategories();
        }
    }

    private void populateSelectedCategories() {
        ViewGroup sectionCategories = ((ViewGroup) findViewById(R.id.sectionCategories));

        // Remove previous category buttons if any + select category button
        List<View> viewsToRemove = new ArrayList<View>();
        for (int i = 0; i < sectionCategories.getChildCount(); i++) {
            View v = sectionCategories.getChildAt(i);
            Object tag = v.getTag();
            if (tag != null && tag.getClass() == String.class &&
                    (((String) tag).startsWith(CATEGORY_PREFIX_TAG) || tag.equals("select-category"))) {
                viewsToRemove.add(v);
            }
        }
        for (int i = 0; i < viewsToRemove.size(); i++) {
            sectionCategories.removeView(viewsToRemove.get(i));
        }
        viewsToRemove.clear();

        // New category buttons
        LayoutInflater layoutInflater = getLayoutInflater();
        for (int i = 0; i < mCategories.size(); i++) {
            String categoryName = mCategories.get(i);
            Button buttonCategory = (Button) layoutInflater.inflate(R.layout.category_button, null);
            buttonCategory.setText(Html.fromHtml(categoryName));
            buttonCategory.setTag(CATEGORY_PREFIX_TAG + categoryName);
            buttonCategory.setOnClickListener(this);
            sectionCategories.addView(buttonCategory);
        }

        // Add select category button
        Button selectCategory = (Button) layoutInflater.inflate(R.layout.category_select_button, null);
        selectCategory.setOnClickListener(this);
        sectionCategories.addView(selectCategory);
    }

    @Override
    protected Dialog onCreateDialog(int id) {

        switch (id) {
            case ID_DIALOG_DATE:
                DatePickerDialog dpd = new DatePickerDialog(this, mDateSetListener, mYear, mMonth, mDay);
                dpd.setTitle("");
                return dpd;
            case ID_DIALOG_TIME:
                TimePickerDialog tpd = new TimePickerDialog(this, mTimeSetListener, mHour, mMinute, false);
                tpd.setTitle("");
                return tpd;
            case ID_DIALOG_LOADING:
                ProgressDialog loadingDialog = new ProgressDialog(this);
                loadingDialog.setMessage(getResources().getText(R.string.loading));
                loadingDialog.setIndeterminate(true);
                loadingDialog.setCancelable(false);
                return loadingDialog;
            case ID_DIALOG_DOWNLOAD:
                ProgressDialog downloadDialog = new ProgressDialog(this);
                downloadDialog.setMessage(getResources().getText(R.string.download));
                downloadDialog.setIndeterminate(true);
                downloadDialog.setCancelable(false);
                return downloadDialog;
        }
        return super.onCreateDialog(id);
    }

    private boolean savePost(boolean isAutoSave, boolean isDraftSave) {

        String title = mTitleEditText.getText().toString();
        String password = mPasswordEditText.getText().toString();
        String pubDate = mPubDateText.getText().toString();
        String excerpt = mExcerptEditText.getText().toString();
        String content = "";

        if (mLocalDraft || mIsNew && !isAutoSave) {
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

        long pubDateTimestamp = 0;
        if (!pubDate.equals(getResources().getText(R.string.immediately))) {
            if (mIsCustomPubDate)
                pubDateTimestamp = mCustomPubDate;
            else if (!mIsNew)
                pubDateTimestamp = mPost.getDate_created_gmt();
        }

        String tags = "", postFormat = "";
        if (!mIsPage) {
            tags = mTagsEditText.getText().toString();
            // post format
            Spinner postFormatSpinner = (Spinner) findViewById(R.id.postFormat);
            postFormat = mPostFormats[postFormatSpinner.getSelectedItemPosition()];
        }

        String images = "";
        boolean success = false;

        if (content.equals("") && !isAutoSave && !isDraftSave) {
            AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(EditPostActivity.this);
            dialogBuilder.setTitle(getResources().getText(R.string.empty_fields));
            dialogBuilder.setMessage(getResources().getText(R.string.title_post_required));
            dialogBuilder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    dialog.dismiss();
                }
            });
            dialogBuilder.setCancelable(true);
            dialogBuilder.create().show();
        } else {

            
            if (!mIsNew) {
                // update the images
                mPost.deleteMediaFiles();

                Editable s = mContentEditText.getText();
                WPImageSpan[] click_spans = s.getSpans(0, s.length(), WPImageSpan.class);
                if (click_spans.length != 0) {

                    for (int i = 0; i < click_spans.length; i++) {
                        WPImageSpan wpIS = click_spans[i];
                        images += wpIS.getImageSource().toString() + ",";

                        if (wpIS.getMediaId() != null) {
                            updateMediaFileOnServer(wpIS);
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
                            if (mLocalDraft)
                                content = WPHtml.toHtml(s);
                            else
                                content = s.toString();
                        }
                    }
                }
            } else {

                Editable s = mContentEditText.getText();
                WPImageSpan[] click_spans = s.getSpans(0, s.length(), WPImageSpan.class);
                
                // update the description, caption and title of media files on the server
                if (click_spans.length != 0) {

                    for (int i = 0; i < click_spans.length; i++) {
                        WPImageSpan wpIS = click_spans[i];
                        images += wpIS.getImageSource().toString() + ",";

                        if (wpIS.getMediaId() != null) {
                            updateMediaFileOnServer(wpIS);
                        }
                        
                    }
                }
            }
            

            final String moreTag = "<!--more-->";
            int selectedStatus = mStatusSpinner.getSelectedItemPosition();
            String status = "";

            switch (selectedStatus) {
                case 0:
                    status = "publish";
                    break;
                case 1:
                    status = "draft";
                    break;
                case 2:
                    status = "pending";
                    break;
                case 3:
                    status = "private";
                    break;
            }

            Double latitude = 0.0;
            Double longitude = 0.0;
            if (mBlog.isLocation()) {

                // attempt to get the device's location
                try {
                    latitude = mCurrentLocation.getLatitude();
                    longitude = mCurrentLocation.getLongitude();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            if (mIsNew) {
                
                JSONArray categorisList = JSONUtil.fromStringListToJSONArray(mCategories);
                mPost = new Post(mBlogID, title, content, excerpt, images, pubDateTimestamp, categorisList.toString(), tags, status, password,
                        latitude, longitude, mIsPage, postFormat, true, false);
                mPost.setLocalDraft(true);

                // split up the post content if there's a more tag
                if (content.indexOf(moreTag) >= 0) {
                    mPost.setDescription(content.substring(0, content.indexOf(moreTag)));
                    mPost.setMt_text_more(content.substring(content.indexOf(moreTag) + moreTag.length(), content.length()));
                }

                success = mPost.save();

                if (success) {
                    mIsNew = false;
                }

                mPost.deleteMediaFiles();

                Spannable s = mContentEditText.getText();
                WPImageSpan[] image_spans = s.getSpans(0, s.length(), WPImageSpan.class);

                if (image_spans.length != 0) {

                    for (int i = 0; i < image_spans.length; i++) {
                        WPImageSpan wpIS = image_spans[i];
                        images += wpIS.getImageSource().toString() + ",";

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
                }

                WordPress.currentPost = mPost;

            } else {

                if (mCurrentLocation == null) {
                    latitude = mPost.getLatitude();
                    longitude = mPost.getLongitude();
                }

                mPost.setTitle(title);
                mPost.setMt_excerpt(excerpt);
                // split up the post content if there's a more tag
                if (mLocalDraft && content.indexOf(moreTag) >= 0) {
                    mPost.setDescription(content.substring(0, content.indexOf(moreTag)));
                    mPost.setMt_text_more(content.substring(content.indexOf(moreTag) + moreTag.length(), content.length()));
                } else {
                    mPost.setDescription(content);
                    mPost.setMt_text_more("");
                }
                mPost.setMediaPaths(images);
                mPost.setDate_created_gmt(pubDateTimestamp);
                mPost.setJSONCategories(new JSONArray(mCategories));
                mPost.setMt_keywords(tags);
                mPost.setPost_status(status);
                mPost.setWP_password(password);
                mPost.setLatitude(latitude);
                mPost.setLongitude(longitude);
                mPost.setWP_post_form(postFormat);
                if (!mPost.isLocalDraft())
                    mPost.setLocalChange(true);
                success = mPost.update();
            }
        }
        return success;
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
                        Toast.makeText(EditPostActivity.this, R.string.media_edit_failure, Toast.LENGTH_LONG).show();
                    }
                });

        List<Object> apiArgs = new ArrayList<Object>();
        apiArgs.add(currentBlog);
        task.execute(apiArgs);
    }

    private class getAddressTask extends AsyncTask<Double, Void, String> {

        @Override
        protected String doInBackground(Double... args) {
            Geocoder gcd = new Geocoder(EditPostActivity.this, Locale.getDefault());
            String finalText = "";
            List<Address> addresses;
            try {
                addresses = gcd.getFromLocation(args[0], args[1], 1);
                String locality = "", adminArea = "", country = "";
                if (addresses.get(0).getLocality() != null)
                    locality = addresses.get(0).getLocality();
                if (addresses.get(0).getAdminArea() != null)
                    adminArea = addresses.get(0).getAdminArea();
                if (addresses.get(0).getCountryName() != null)
                    country = addresses.get(0).getCountryName();

                if (addresses.size() > 0) {
                    finalText = ((locality.equals("")) ? locality : locality + ", ")
                            + ((adminArea.equals("")) ? adminArea : adminArea + " ") + country;
                    if (finalText.equals(""))
                        finalText = getString(R.string.location_not_found);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            return finalText;
        }

        protected void onPostExecute(String result) {
            mLocationText.setText(result);
        }
    }

    protected void setContent() {
        Intent intent = getIntent();
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
                mContentEditText.setText(WPHtml.fromHtml(StringUtils.addPTags(text), EditPostActivity.this, mPost));
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

    private class processAttachmentsTask extends AsyncTask<List<?>, Void, SpannableStringBuilder> {

        protected void onPreExecute() {
            showDialog(ID_DIALOG_LOADING);
        }

        @Override
        protected SpannableStringBuilder doInBackground(List<?>... args) {
            ArrayList<?> multi_stream = (ArrayList<?>) args[0].get(0);
            String type = (String) args[0].get(1);
            SpannableStringBuilder ssb = new SpannableStringBuilder();
            for (int i = 0; i < multi_stream.size(); i++) {
                Uri imageUri = (Uri) multi_stream.get(i);
                if (imageUri != null && type != null) {
                    addMedia(imageUri, ssb);
                }
            }
            return ssb;
        }

        protected void onPostExecute(SpannableStringBuilder ssb) {
            dismissDialog(ID_DIALOG_LOADING);
            if (ssb != null) {
                if (ssb.length() > 0) {
                    mContentEditText.setText(ssb);
                }
            } else {
                Toast.makeText(EditPostActivity.this, getResources().getText(R.string.gallery_error), Toast.LENGTH_SHORT).show();
            }
        }
    }

    //Calculate the minimun width between the blog setting and picture real width
    private void setWPImageSpanWidth(Uri curStream, WPImageSpan is) {
        String imageWidth = WordPress.getCurrentBlog().getMaxImageWidth();
        int imageWidthBlogSetting = Integer.MAX_VALUE;

        if (!imageWidth.equals("Original Size")) {
            try {
                imageWidthBlogSetting = Integer.valueOf(imageWidth);
            } catch (NumberFormatException e) {
                e.printStackTrace();
            }
        }

        int[] dimensions = ImageHelper.getImageSize(curStream, EditPostActivity.this);
        int imageWidthPictureSetting = dimensions[0] == 0 ? Integer.MAX_VALUE : dimensions[0];

        if (Math.min(imageWidthPictureSetting, imageWidthBlogSetting) == Integer.MAX_VALUE) {
            is.setWidth(1024); //Default value in case of errors reading the picture size and the blog settings is set to Original size
        } else {
            is.setWidth(Math.min(imageWidthPictureSetting, imageWidthBlogSetting));
        }
    }

    private boolean addMedia(Uri imageUri, SpannableStringBuilder ssb) {

        //if (mFormatBar.getVisibility() == View.VISIBLE)
        //    hideFormatBar();

        if (ssb != null && isPicasaImage(imageUri))
            imageUri = downloadExternalImage(imageUri);

        if (imageUri == null) {
            return false;
        }

        Bitmap resizedBitmap = null;
        ImageHelper ih = new ImageHelper();
        Display display = getWindowManager().getDefaultDisplay();
        int width = display.getWidth();
        int height = display.getHeight();
        if (width > height)
            width = height;

        Map<String, Object> mediaData = ih.getImageBytesForPath(imageUri.getEncodedPath(), EditPostActivity.this);

        if (mediaData == null) {
            // data stream not returned
            return false;
        }

        BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inJustDecodeBounds = true;
        byte[] bytes = (byte[]) mediaData.get("bytes");
        BitmapFactory.decodeByteArray(bytes, 0, bytes.length, opts);

        float conversionFactor = 0.25f;

        if (opts.outWidth > opts.outHeight)
            conversionFactor = 0.40f;

        byte[] finalBytes = ih.createThumbnail(bytes, String.valueOf((int) (width * conversionFactor)),
                (String) mediaData.get("orientation"), true);

        if (finalBytes == null) {
            //Toast.makeText(EditPostActivity.this, getResources().getText(R.string.out_of_memory), Toast.LENGTH_SHORT).show();
            return false;
        }

        resizedBitmap = BitmapFactory.decodeByteArray(finalBytes, 0, finalBytes.length);

        if (ssb != null) {
            WPImageSpan is = new WPImageSpan(EditPostActivity.this, resizedBitmap, imageUri);

            setWPImageSpanWidth(imageUri, is);

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
            WPImageSpan is = new WPImageSpan(EditPostActivity.this, resizedBitmap, imageUri);

            setWPImageSpanWidth(imageUri, is);

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

    private DatePickerDialog.OnDateSetListener mDateSetListener = new DatePickerDialog.OnDateSetListener() {
        public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
            mYear = year;
            mMonth = monthOfYear;
            mDay = dayOfMonth;
            showDialog(ID_DIALOG_TIME);
        }
    };

    private TimePickerDialog.OnTimeSetListener mTimeSetListener = new TimePickerDialog.OnTimeSetListener() {

        public void onTimeSet(TimePicker view, int hour, int minute) {
            mHour = hour;
            mMinute = minute;

            Date d = new Date(mYear - 1900, mMonth, mDay, mHour, mMinute);
            long timestamp = d.getTime();

            try {
                int flags = 0;
                flags |= android.text.format.DateUtils.FORMAT_SHOW_DATE;
                flags |= android.text.format.DateUtils.FORMAT_ABBREV_MONTH;
                flags |= android.text.format.DateUtils.FORMAT_SHOW_YEAR;
                flags |= android.text.format.DateUtils.FORMAT_SHOW_TIME;
                String formattedDate = DateUtils.formatDateTime(EditPostActivity.this, timestamp, flags);
                mCustomPubDate = timestamp;
                mPubDateText.setText(formattedDate);
                mIsCustomPubDate = true;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    };

    private Runnable autoSaveRunnable = new Runnable() {
        @Override
        public void run() {
            savePost(true, false);
            mAutoSaveHandler.postDelayed(this, AUTOSAVE_DELAY_MILLIS);
        }
    };

    @Override
    public void afterTextChanged(Editable s) {

        try {
            int position = Selection.getSelectionStart(mContentEditText.getText());
            if ((mIsBackspace && position != 1) || mLastPosition == position || !mLocalDraft)
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
        if (!mLocalDraft)
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
}