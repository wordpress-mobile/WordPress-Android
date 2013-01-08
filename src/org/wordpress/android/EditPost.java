package org.wordpress.android;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Vector;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.wordpress.android.models.Blog;
import org.wordpress.android.models.MediaFile;
import org.wordpress.android.models.Post;
import org.wordpress.android.util.EscapeUtils;
import org.wordpress.android.util.ImageHelper;
import org.wordpress.android.util.LocationHelper;
import org.wordpress.android.util.LocationHelper.LocationResult;
import org.wordpress.android.util.DeviceUtils;
import org.wordpress.android.util.StringHelper;
import org.wordpress.android.util.WPEditText;
import org.wordpress.android.util.WPHtml;
import org.wordpress.android.util.WPImageSpan;
import org.wordpress.android.util.WPUnderlineSpan;
import org.xmlrpc.android.ApiHelper;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.app.TimePickerDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
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
import android.provider.MediaStore;
import android.text.Editable;
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
import android.view.View.OnTouchListener;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;
import android.widget.ToggleButton;

public class EditPost extends Activity implements OnClickListener,
		OnTouchListener, TextWatcher, WPEditText.OnSelectionChangedListener,
		WPEditText.EditTextImeBackListener {

	private static final int AUTOSAVE_DELAY_MILLIS = 60000;

	private static final int ACTIVITY_REQUEST_CODE_PICTURE_LIBRARY = 0;
	private static final int ACTIVITY_REQUEST_CODE_TAKE_PHOTO = 1;
	private static final int ACTIVITY_REQUEST_CODE_VIDEO_LIBRARY = 2;
	private static final int ACTIVITY_REQUEST_CODE_TAKE_VIDEO = 3;
	private static final int ACTIVITY_REQUEST_CODE_CREATE_LINK = 4;
	private static final int ACTIVITY_REQUEST_CODE_SELECT_CATEGORIES = 5;

	private static final int ID_DIALOG_DATE = 0;
	private static final int ID_DIALOG_TIME = 1;
	private static final int ID_DIALOG_LOADING = 2;

	private Blog mBlog;
	private Post mPost;

	private WPEditText mContentEditText;
	private ImageButton mAddPictureButton;
	private Spinner mStatusSpinner;
	private EditText mTitleEditText, mPasswordEditText, mTagsEditText;
	private TextView mLocationText, mCategoriesText, mPubDateText;
	private ToggleButton mBoldToggleButton, mEmToggleButton,
			mBquoteToggleButton;
	private ToggleButton mUnderlineToggleButton, mStrikeToggleButton;
	private Button mSaveButton, mPubDateButton, mLinkButton, mMoreButton;

	private Location mCurrentLocation;
	private LocationHelper mLocationHelper;
	private Handler mAutoSaveHandler;
	private JSONArray mCategories;

	private boolean mIsPage = false;
	private boolean mIsNew = false;
	private boolean mLocalDraft = false;
	private boolean mIsCustomPubDate = false;
	private boolean mIsFullScreenEditing = false;
	private boolean mIsBackspace = false;
	private boolean mImeBackPressed = false;
	private boolean mScrollDetected = false;
	private boolean mIsNewDraft = false;

	private Vector<String> mSelectedCategories;
	private String mAccountName = "";
	private String mOption = "";
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

		Calendar c = Calendar.getInstance();
		mYear = c.get(Calendar.YEAR);
		mMonth = c.get(Calendar.MONTH);
		mDay = c.get(Calendar.DAY_OF_MONTH);
		mHour = c.get(Calendar.HOUR_OF_DAY);
		mMinute = c.get(Calendar.MINUTE);
		mCategories = new JSONArray();
		mAutoSaveHandler = new Handler();
		mSelectedCategories = new Vector<String>();

		String action = getIntent().getAction();
		if (Intent.ACTION_SEND.equals(action) || Intent.ACTION_SEND_MULTIPLE.equals(action)) {
			// we arrived here from a share action
			if (!selectBlogForShareAction())
				return;
		} else {
			initBlog();
			if (extras != null) {
				mAccountName = EscapeUtils.unescapeHtml(extras
						.getString("accountName"));
				mPostID = extras.getLong("postID");
				mLocalDraft = extras.getBoolean("localDraft", false);
				mIsPage = extras.getBoolean("isPage", false);
				mIsNew = extras.getBoolean("isNew", false);

				if (savedInstanceState != null) {
					mCurrentActivityRequest = savedInstanceState
							.getInt("currentActivityRequest");
					if (savedInstanceState.getString("mediaCapturePath") != null)
						mMediaCapturePath = savedInstanceState
								.getString("mediaCapturePath");
				} else {
					mOption = extras.getString("option");
				}

				if (extras.getBoolean("isQuickPress")) {
					mBlogID = extras.getInt("id");
				} else {
					mBlogID = WordPress.currentBlog.getId();
				}

				try {
					mBlog = new Blog(mBlogID, this);
					WordPress.currentBlog = mBlog;
				} catch (Exception e) {
					showBlogErrorAndFinish();
					return;
				}

				if (!mIsNew) {
					try {
						mPost = new Post(mBlogID, mPostID, mIsPage, this);
						if (mPost == null) {
							// big oopsie
							Toast.makeText(
									this,
									getResources().getText(
											R.string.post_not_found),
									Toast.LENGTH_LONG).show();
							finish();
							return;
						} else {
							WordPress.currentPost = mPost;
						}
					} catch (Exception e) {
						e.printStackTrace();
						finish();
					}
				}
			}

			if (mIsNew) {
				mLocalDraft = true;
				setTitle(EscapeUtils.unescapeHtml(WordPress.currentBlog
						.getBlogName())
						+ " - "
						+ getString((mIsPage) ? R.string.new_page
								: R.string.new_post));
			} else {
				setTitle(EscapeUtils.unescapeHtml(WordPress.currentBlog
						.getBlogName())
						+ " - "
						+ getString((mIsPage) ? R.string.edit_page
								: R.string.edit_post));
			}
		}

		setContentView(R.layout.edit);
		mContentEditText = (WPEditText) findViewById(R.id.postContent);
		mTitleEditText = (EditText) findViewById(R.id.title);
		mPasswordEditText = (EditText) findViewById(R.id.post_password);
		mLocationText = (TextView) findViewById(R.id.locationText);
		mBoldToggleButton = (ToggleButton) findViewById(R.id.bold);
		mEmToggleButton = (ToggleButton) findViewById(R.id.em);
		mBquoteToggleButton = (ToggleButton) findViewById(R.id.bquote);
		mUnderlineToggleButton = (ToggleButton) findViewById(R.id.underline);
		mStrikeToggleButton = (ToggleButton) findViewById(R.id.strike);
		mCategoriesText = (TextView) findViewById(R.id.selectedCategories);
		mAddPictureButton = (ImageButton) findViewById(R.id.addPictureButton);
		mSaveButton = (Button) findViewById(R.id.post);
		mPubDateButton = (Button) findViewById(R.id.pubDateButton);
		mPubDateText = (TextView) findViewById(R.id.pubDate);
		mLinkButton = (Button) findViewById(R.id.link);
		mMoreButton = (Button) findViewById(R.id.more);
		mStatusSpinner = (Spinner) findViewById(R.id.status);
		mTagsEditText = (EditText) findViewById(R.id.tags);

		if (mIsPage) { // remove post specific views
			((RelativeLayout) findViewById(R.id.section3))
					.setVisibility(View.GONE);
			((RelativeLayout) findViewById(R.id.location_wrapper))
					.setVisibility(View.GONE);
			((TextView) findViewById(R.id.postFormatLabel))
					.setVisibility(View.GONE);
			((Spinner) findViewById(R.id.postFormat)).setVisibility(View.GONE);
		} else {
			if (mBlog.getPostFormats().equals("")) {
				Vector<Object> args = new Vector<Object>();
				args.add(mBlog);
				args.add(this);
				new ApiHelper.getPostFormatsTask().execute(args);
				mPostFormatTitles = getResources().getStringArray(
						R.array.post_formats_array);
				String defaultPostFormatTitles[] = { "aside", "audio", "chat",
						"gallery", "image", "link", "quote", "standard",
						"status", "video" };
				mPostFormats = defaultPostFormatTitles;
			} else {
				try {
					JSONObject jsonPostFormats = new JSONObject(
							mBlog.getPostFormats());
					mPostFormats = new String[jsonPostFormats.length()];
					mPostFormatTitles = new String[jsonPostFormats.length()];
					Iterator<?> it = jsonPostFormats.keys();
					int i = 0;
					while (it.hasNext()) {
						String key = (String) it.next();
						String val = (String) jsonPostFormats.get(key);
						mPostFormats[i] = key;
						mPostFormatTitles[i] = val;
						i++;
					}
				} catch (JSONException e) {
					e.printStackTrace();
				}
			}
			Spinner pfSpinner = (Spinner) findViewById(R.id.postFormat);
			ArrayAdapter<String> pfAdapter = new ArrayAdapter<String>(this,
					android.R.layout.simple_spinner_item, mPostFormatTitles);
			pfAdapter
					.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
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

			if (Intent.ACTION_SEND.equals(action)
					|| Intent.ACTION_SEND_MULTIPLE.equals(action))
				setContent();
		}

		String[] items = new String[] {
				getResources().getString(R.string.publish_post),
				getResources().getString(R.string.draft),
				getResources().getString(R.string.pending_review),
				getResources().getString(R.string.post_private),
				getResources().getString(R.string.local_draft) };

		ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
				android.R.layout.simple_spinner_item, items);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		mStatusSpinner.setAdapter(adapter);
		mStatusSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> arg0, View arg1,
					int arg2, long arg3) {
				evaluateSaveButtonText();
			}

			@Override
			public void onNothingSelected(AdapterView<?> arg0) {
			}
		});

		getLocationProvider();

		if (mIsNew) {
			// handles selections from the quick action bar
			if (mOption != null) {
				if (mOption.equals("newphoto"))
					launchCamera();
				else if (mOption.equals("photolibrary"))
					launchPictureLibrary();
				else if (mOption.equals("newvideo"))
					launchVideoCamera();
				else if (mOption.equals("videolibrary"))
					launchVideoLibrary();
				mLocalDraft = extras.getBoolean("localDraft");
			}
		} else {
			mTitleEditText.setText(mPost.getTitle());

			if (mPost.isUploaded()) {
				items = new String[] {
						getResources().getString(R.string.publish_post),
						getResources().getString(R.string.draft),
						getResources().getString(R.string.pending_review),
						getResources().getString(R.string.post_private) };
				adapter = new ArrayAdapter<String>(this,
						android.R.layout.simple_spinner_item, items);
				mStatusSpinner.setAdapter(adapter);
			}

			String contentHTML;

			if (!mPost.getMt_text_more().equals("")) {
				if (mPost.isLocalDraft())
					contentHTML = mPost.getDescription()
							+ "\n&lt;!--more--&gt;\n" + mPost.getMt_text_more();
				else
					contentHTML = mPost.getDescription() + "\n<!--more-->\n"
							+ mPost.getMt_text_more();
			} else
				contentHTML = mPost.getDescription();

			try {
				if (mPost.isLocalDraft())
					mContentEditText.setText(WPHtml.fromHtml(
							contentHTML.replaceAll("\uFFFC", ""),
							EditPost.this, mPost));
				else
					mContentEditText.setText(contentHTML.replaceAll("\uFFFC",
							""));
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
					String formattedDate = DateUtils.formatDateTime(
							EditPost.this, pubDate, flags);
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
					mStatusSpinner.setSelection(4, true);
				}

				evaluateSaveButtonText();
			}

			if (!mIsPage) {
				if (mPost.getCategories() != null) {
					mCategories = mPost.getCategories();
					if (!mCategories.equals("")) {

						for (int i = 0; i < mCategories.length(); i++) {
							try {
								mSelectedCategories.add(mCategories
										.getString(i));
							} catch (JSONException e) {
								e.printStackTrace();
							}
						}
						mCategoriesText
								.setText(getString(R.string.selected_categories)
										+ " " + getCategoriesCSV());
					}
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

		if (!mIsPage) {
			Button selectCategories = (Button) findViewById(R.id.selectCategories);
			selectCategories.setOnClickListener(this);
		}

		registerForContextMenu(mAddPictureButton);
		mContentEditText.setOnSelectionChangedListener(this);
		mContentEditText.setOnEditTextImeBackListener(this);
		mContentEditText.setOnTouchListener(this);
		mContentEditText.addTextChangedListener(this);
		mAddPictureButton.setOnClickListener(this);
		mSaveButton.setOnClickListener(this);
		mPubDateButton.setOnClickListener(this);
		mBoldToggleButton.setOnClickListener(this);
		mLinkButton.setOnClickListener(this);
		mEmToggleButton.setOnClickListener(this);
		mUnderlineToggleButton.setOnClickListener(this);
		mStrikeToggleButton.setOnClickListener(this);
		mBquoteToggleButton.setOnClickListener(this);
		mMoreButton.setOnClickListener(this);
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
		savedInstanceState.putInt("currentActivityRequest",
				mCurrentActivityRequest);
		if (!mMediaCapturePath.equals(""))
			savedInstanceState.putString("mediaCapturePath", mMediaCapturePath);
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		mImeBackPressed = true;
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
		}
		return false;
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.bold:
			formatBtnClick(mBoldToggleButton, "strong");
			break;
		case R.id.em:
			formatBtnClick(mEmToggleButton, "em");
			break;
		case R.id.underline:
			formatBtnClick(mUnderlineToggleButton, "u");
			break;
		case R.id.strike:
			formatBtnClick(mStrikeToggleButton, "strike");
			break;
		case R.id.bquote:
			formatBtnClick(mBquoteToggleButton, "blockquote");
			break;
		case R.id.more:
			mSelectionEnd = mContentEditText.getSelectionEnd();
			Editable str = mContentEditText.getText();
			str.insert(mSelectionEnd, "\n<!--more-->\n");
			break;
		case R.id.link:
			mSelectionStart = mContentEditText.getSelectionStart();
			mStyleStart = mSelectionStart;
			mSelectionEnd = mContentEditText.getSelectionEnd();

			if (mSelectionStart > mSelectionEnd) {
				int temp = mSelectionEnd;
				mSelectionEnd = mSelectionStart;
				mSelectionStart = temp;
			}

			Intent i = new Intent(EditPost.this, Link.class);
			if (mSelectionEnd > mSelectionStart) {
				String selectedText = mContentEditText.getText()
						.subSequence(mSelectionStart, mSelectionEnd).toString();
				i.putExtra("selectedText", selectedText);
			}
			startActivityForResult(i, ACTIVITY_REQUEST_CODE_CREATE_LINK);
			break;
		case R.id.addPictureButton:
			mAddPictureButton.performLongClick();
			break;
		case R.id.pubDateButton:
			showDialog(ID_DIALOG_DATE);
			break;
		case R.id.selectCategories:
			Bundle bundle = new Bundle();
			bundle.putInt("id", mBlogID);
			if (mCategories.length() > 0) {
				bundle.putString("categoriesCSV", getCategoriesCSV());
			}
			Intent i1 = new Intent(EditPost.this, SelectCategories.class);
			i1.putExtras(bundle);
			startActivityForResult(i1, ACTIVITY_REQUEST_CODE_SELECT_CATEGORIES);
			break;
		case R.id.post: // mSaveButton
			if (mAutoSaveHandler != null)
				mAutoSaveHandler.removeCallbacks(autoSaveRunnable);

			if (savePost(false)) {
				if (mPost.isUploaded()
						|| !mPost.getPost_status().equals("localdraft")) {
					if (mOption != null) {
						if (mOption.equals("newphoto")
								|| mOption.equals("photolibrary"))
							mPost.setQuickPostType("QuickPhoto");
						else if (mOption.equals("newvideo")
								|| mOption.equals("videolibrary"))
							mPost.setQuickPostType("QuickVideo");
					}
					mPost.upload();
				}
				finish();
			}
			break;
		case R.id.viewMap:
			Double latitude = 0.0;
			try {
				latitude = mCurrentLocation.getLatitude();
			} catch (Exception e) {
				e.printStackTrace();
			}
			if (latitude != 0.0) {
				String uri = "geo:" + latitude + ","
						+ mCurrentLocation.getLongitude();
				startActivity(new Intent(android.content.Intent.ACTION_VIEW,
						Uri.parse(uri)));
			} else {
				Toast.makeText(EditPost.this,
						getResources().getText(R.string.location_toast),
						Toast.LENGTH_SHORT).show();
			}
			break;
		case R.id.updateLocation:
			mLocationHelper.getLocation(EditPost.this, locationResult);
			break;
		case R.id.removeLocation:
			if (mCurrentLocation != null) {
				mCurrentLocation.setLatitude(0.0);
				mCurrentLocation.setLongitude(0.0);
			}
			if (mPost != null) {
				mPost.setLatitude(0.0);
				mPost.setLongitude(0.0);
			}
			mLocationText.setText("");
			break;
		}
	}

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

		if (!mIsFullScreenEditing && event.getAction() == 1) {
			mIsFullScreenEditing = true;
			mContentEditText.setFocusableInTouchMode(true);
			mContentEditText.setHint("");
			try {
				((LinearLayout) findViewById(R.id.postContentEditorSmallWrapper))
						.removeView(mContentEditText);
				((ScrollView) findViewById(R.id.scrollView))
						.setVisibility(View.GONE);
				LinearLayout contentEditorWrap = (LinearLayout) findViewById(R.id.postContentEditorWrapper);
				contentEditorWrap.addView(mContentEditText);
				contentEditorWrap.setVisibility(View.VISIBLE);
				((RelativeLayout) findViewById(R.id.formatBar))
						.setVisibility(View.VISIBLE);
			} catch (Exception e) {
				e.printStackTrace();
			}
			mContentEditText.requestFocus();
			return false;
		}

		if (event.getAction() == 1 && !mScrollDetected && mIsFullScreenEditing) {
			mImeBackPressed = false;
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
				WPImageSpan[] image_spans = s.getSpans(charPosition,
						charPosition, WPImageSpan.class);

				if (image_spans.length != 0) {
					final WPImageSpan span = image_spans[0];
					if (!span.isVideo()) {
						LayoutInflater factory = LayoutInflater
								.from(EditPost.this);
						final View alertView = factory.inflate(
								R.layout.alert_image_options, null);
						final TextView imageWidthText = (TextView) alertView
								.findViewById(R.id.imageWidthText);
						final EditText titleText = (EditText) alertView
								.findViewById(R.id.title);
						// final EditText descText = (EditText)
						// alertView.findViewById(R.id.description);
						final EditText caption = (EditText) alertView
								.findViewById(R.id.caption);
						final CheckBox featuredCheckBox = (CheckBox) alertView.findViewById(R.id.featuredImage);
						final CheckBox featuredInPostCheckBox = (CheckBox) alertView.findViewById(R.id.featuredInPost);
						
						//show featured image checkboxes if theme support it
						if (WordPress.currentBlog.isFeaturedImageCapable()) {
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
						
						final SeekBar seekBar = (SeekBar) alertView
								.findViewById(R.id.imageWidth);
						final Spinner alignmentSpinner = (Spinner) alertView
								.findViewById(R.id.alignment_spinner);
						ArrayAdapter<CharSequence> adapter = ArrayAdapter
								.createFromResource(EditPost.this,
										R.array.alignment_array,
										android.R.layout.simple_spinner_item);
						adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
						alignmentSpinner.setAdapter(adapter);

						imageWidthText.setText(String.valueOf(span.getWidth())
								+ "px");
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

						alignmentSpinner.setSelection(
								span.getHorizontalAlignment(), true);

						seekBar.setMax(100);
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
							public void onProgressChanged(SeekBar seekBar,
									int progress, boolean fromUser) {
								if (progress == 0)
									progress = 1;
								imageWidthText.setText(progress * 10 + "px");
							}
						});

						AlertDialog ad = new AlertDialog.Builder(EditPost.this)
								.setTitle(getString(R.string.image_settings))
								.setView(alertView)
								.setPositiveButton(getString(R.string.ok),
										new DialogInterface.OnClickListener() {
											public void onClick(
													DialogInterface dialog,
													int whichButton) {

												span.setTitle(titleText
														.getText().toString());
												// span.setDescription(descText.getText().toString());
												span.setHorizontalAlignment(alignmentSpinner
														.getSelectedItemPosition());
												span.setWidth(seekBar
														.getProgress() * 10);
												span.setCaption(caption
														.getText().toString());
												span.setFeatured(featuredCheckBox.isChecked());
												if (featuredCheckBox.isChecked()) {
													//remove featured flag from all other images
													WPImageSpan[] click_spans = s.getSpans(0, s.length(),
															WPImageSpan.class);
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
										})
								.setNegativeButton(getString(R.string.cancel),
										new DialogInterface.OnClickListener() {
											public void onClick(
													DialogInterface dialog,
													int whichButton) {
												dialog.dismiss();
											}
										}).create();
						ad.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
						ad.show();
						mImeBackPressed = true;
						mScrollDetected = false;
						return true;
					}

				} else {
					mContentEditText.setMovementMethod(ArrowKeyMovementMethod
							.getInstance());
					mContentEditText.setSelection(mContentEditText
							.getSelectionStart());
				}
			}
		} else if (event.getAction() == 1) {
			mScrollDetected = false;
		}
		return false;
	}

	@Override
	public void onBackPressed() {
		if (!mIsFullScreenEditing && !mImeBackPressed) {
			AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(
					EditPost.this);
			dialogBuilder
					.setTitle(getResources().getText(R.string.cancel_edit));
			dialogBuilder.setMessage(getResources().getText(
					(mIsPage) ? R.string.sure_to_cancel_edit_page
							: R.string.sure_to_cancel_edit));
			dialogBuilder.setPositiveButton(getResources()
					.getText(R.string.yes),
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog,
								int whichButton) {
							if (mIsNewDraft)
								mPost.delete();
							Bundle bundle = new Bundle();
							bundle.putString("returnStatus", "CANCEL");
							Intent mIntent = new Intent();
							mIntent.putExtras(bundle);
							setResult(RESULT_OK, mIntent);
							finish();
						}
					});
			dialogBuilder.setNegativeButton(
					getResources().getText(R.string.no),
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog,
								int whichButton) {
							dialog.dismiss();
						}
					});
			dialogBuilder.setCancelable(true);
			dialogBuilder.create().show();
		} else {
			finishEditing();
		}

		if (mImeBackPressed)
			mImeBackPressed = false;

		return;
	}

	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenu.ContextMenuInfo menuInfo) {
		menu.add(0, 0, 0, getResources().getText(R.string.select_photo));
		if (getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)) {
			menu.add(0, 1, 0, getResources().getText(R.string.take_photo));
		}
		menu.add(0, 2, 0, getResources().getText(R.string.select_video));
		if (getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)) {
			menu.add(0, 3, 0, getResources().getText(R.string.take_video));
		}
	}

	private void initBlog() {
		if (WordPress.currentBlog == null) {
			try {
				WordPress.currentBlog = new Blog(
						WordPress.wpDB.getLastBlogID(this), this);
			} catch (Exception e) {
				showBlogErrorAndFinish();
			}
		}
	}

	private void getLocationProvider() {
		boolean hasLocationProvider = false;
		LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
		List<String> providers = locationManager.getProviders(true);
		for (String providerName : providers) {
			if (providerName.equals(LocationManager.GPS_PROVIDER)
					|| providerName.equals(LocationManager.NETWORK_PROVIDER)) {
				hasLocationProvider = true;
			}
		}
		if (hasLocationProvider && mBlog.isLocation() && !mIsPage) {
			enableLBSButtons();
		}
	}

	private boolean selectBlogForShareAction() {

		mIsNew = true;

		Vector<?> accounts = WordPress.wpDB.getAccounts(this);

		if (accounts.size() > 0) {

			final String blogNames[] = new String[accounts.size()];
			final int accountIDs[] = new int[accounts.size()];

			for (int i = 0; i < accounts.size(); i++) {

				HashMap<?, ?> curHash = (HashMap<?, ?>) accounts.get(i);
				try {
					blogNames[i] = EscapeUtils.unescapeHtml(curHash.get(
							"blogName").toString());
				} catch (Exception e) {
					blogNames[i] = curHash.get("url").toString();
				}
				accountIDs[i] = (Integer) curHash.get("id");
				try {
					mBlog = new Blog(accountIDs[i], EditPost.this);
				} catch (Exception e) {
					showBlogErrorAndFinish();
					return false;
				}
			}

			// Don't prompt if they have one blog only
			if (accounts.size() != 1) {
				AlertDialog.Builder builder = new AlertDialog.Builder(
						EditPost.this);
				builder.setCancelable(false);
				builder.setTitle(getResources().getText(R.string.select_a_blog));
				builder.setItems(blogNames,
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int item) {
								mBlogID = accountIDs[item];
								try {
									mBlog = new Blog(mBlogID, EditPost.this);
								} catch (Exception e) {
									showBlogErrorAndFinish();
								}
								WordPress.currentBlog = mBlog;
								WordPress.wpDB
										.updateLastBlogID(WordPress.currentBlog
												.getId());
								mAccountName = blogNames[item];
								setTitle(EscapeUtils.unescapeHtml(mAccountName)
										+ " - "
										+ getResources().getText(
												(mIsPage) ? R.string.new_page
														: R.string.new_post));
							}
						});
				AlertDialog alert = builder.create();
				alert.show();
			} else {
				mBlogID = accountIDs[0];
				try {
					mBlog = new Blog(mBlogID, EditPost.this);
				} catch (Exception e) {
					showBlogErrorAndFinish();
					return false;
				}
				WordPress.currentBlog = mBlog;
				WordPress.wpDB.updateLastBlogID(WordPress.currentBlog.getId());
				mAccountName = blogNames[0];
				setTitle(EscapeUtils.unescapeHtml(mAccountName)
						+ " - "
						+ getResources().getText(
								(mIsPage) ? R.string.new_page
										: R.string.new_post));
			}
			return true;
		} else {
			// no account, load main view to load new account view
			Toast.makeText(getApplicationContext(),
					getResources().getText(R.string.no_account),
					Toast.LENGTH_LONG).show();
			startActivity(new Intent(this, Dashboard.class));
			finish();
			return false;
		}
	}
	
	private void showBlogErrorAndFinish() {
		Toast.makeText(this,
				getResources().getText(R.string.blog_not_found),
				Toast.LENGTH_SHORT).show();
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
						StyleSpan[] ss = str.getSpans(selectionStart,
								selectionEnd, StyleSpan.class);

						boolean exists = false;
						for (int i = 0; i < ss.length; i++) {
							int style = ((StyleSpan) ss[i]).getStyle();
							if (style == android.graphics.Typeface.BOLD) {
								str.removeSpan(ss[i]);
								exists = true;
							}
						}

						if (!exists) {
							str.setSpan(new StyleSpan(
									android.graphics.Typeface.BOLD),
									selectionStart, selectionEnd,
									Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
						}
						toggleButton.setChecked(false);
					} else if (tag.equals("em")) {
						StyleSpan[] ss = str.getSpans(selectionStart,
								selectionEnd, StyleSpan.class);

						boolean exists = false;
						for (int i = 0; i < ss.length; i++) {
							int style = ((StyleSpan) ss[i]).getStyle();
							if (style == android.graphics.Typeface.ITALIC) {
								str.removeSpan(ss[i]);
								exists = true;
							}
						}

						if (!exists) {
							str.setSpan(new StyleSpan(
									android.graphics.Typeface.ITALIC),
									selectionStart, selectionEnd,
									Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
						}
						toggleButton.setChecked(false);
					} else if (tag.equals("u")) {

						WPUnderlineSpan[] ss = str.getSpans(selectionStart,
								selectionEnd, WPUnderlineSpan.class);

						boolean exists = false;
						for (int i = 0; i < ss.length; i++) {
							str.removeSpan(ss[i]);
							exists = true;
						}

						if (!exists) {
							str.setSpan(new WPUnderlineSpan(), selectionStart,
									selectionEnd,
									Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
						}

						toggleButton.setChecked(false);
					} else if (tag.equals("strike")) {

						StrikethroughSpan[] ss = str.getSpans(selectionStart,
								selectionEnd, StrikethroughSpan.class);

						boolean exists = false;
						for (int i = 0; i < ss.length; i++) {
							str.removeSpan(ss[i]);
							exists = true;
						}

						if (!exists) {
							str.setSpan(new StrikethroughSpan(),
									selectionStart, selectionEnd,
									Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
						}

						toggleButton.setChecked(false);
					} else if (tag.equals("blockquote")) {

						QuoteSpan[] ss = str.getSpans(selectionStart,
								selectionEnd, QuoteSpan.class);

						boolean exists = false;
						for (int i = 0; i < ss.length; i++) {
							str.removeSpan(ss[i]);
							exists = true;
						}

						if (!exists) {
							str.setSpan(new QuoteSpan(), selectionStart,
									selectionEnd,
									Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
						}

						toggleButton.setChecked(false);
					}
				} else if (!toggleButton.isChecked()) {

					if (tag.equals("strong") || tag.equals("em")) {

						StyleSpan[] ss = s.getSpans(mStyleStart - 1,
								mStyleStart, StyleSpan.class);

						for (int i = 0; i < ss.length; i++) {
							int tagStart = s.getSpanStart(ss[i]);
							int tagEnd = s.getSpanEnd(ss[i]);
							if (ss[i].getStyle() == android.graphics.Typeface.BOLD
									&& tag.equals("strong")) {
								tagStart = s.getSpanStart(ss[i]);
								tagEnd = s.getSpanEnd(ss[i]);
								s.removeSpan(ss[i]);
								s.setSpan(new StyleSpan(
										android.graphics.Typeface.BOLD),
										tagStart, tagEnd,
										Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
							}
							if (ss[i].getStyle() == android.graphics.Typeface.ITALIC
									&& tag.equals("em")) {
								tagStart = s.getSpanStart(ss[i]);
								tagEnd = s.getSpanEnd(ss[i]);
								s.removeSpan(ss[i]);
								s.setSpan(new StyleSpan(
										android.graphics.Typeface.ITALIC),
										tagStart, tagEnd,
										Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
							}
						}
					} else if (tag.equals("u")) {
						WPUnderlineSpan[] us = s.getSpans(mStyleStart - 1,
								mStyleStart, WPUnderlineSpan.class);
						for (int i = 0; i < us.length; i++) {
							int tagStart = s.getSpanStart(us[i]);
							int tagEnd = s.getSpanEnd(us[i]);
							s.removeSpan(us[i]);
							s.setSpan(new WPUnderlineSpan(), tagStart, tagEnd,
									Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
						}
					} else if (tag.equals("strike")) {
						StrikethroughSpan[] ss = s.getSpans(mStyleStart - 1,
								mStyleStart, StrikethroughSpan.class);
						for (int i = 0; i < ss.length; i++) {
							int tagStart = s.getSpanStart(ss[i]);
							int tagEnd = s.getSpanEnd(ss[i]);
							s.removeSpan(ss[i]);
							s.setSpan(new StrikethroughSpan(), tagStart,
									tagEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
						}
					} else if (tag.equals("blockquote")) {
						QuoteSpan[] ss = s.getSpans(mStyleStart - 1,
								mStyleStart, QuoteSpan.class);
						for (int i = 0; i < ss.length; i++) {
							int tagStart = s.getSpanStart(ss[i]);
							int tagEnd = s.getSpanEnd(ss[i]);
							s.removeSpan(ss[i]);
							s.setSpan(new QuoteSpan(), tagStart, tagEnd,
									Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
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
					mContentEditText.setSelection(selectionEnd
							+ startTag.length() + endTag.length());
				} else if (toggleButton.isChecked()) {
					content.insert(selectionStart, startTag);
					mContentEditText.setSelection(selectionEnd
							+ startTag.length());
				} else if (!toggleButton.isChecked()) {
					content.insert(selectionEnd, endTag);
					mContentEditText.setSelection(selectionEnd
							+ endTag.length());
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void finishEditing() {
		mContentEditText.setHint(R.string.content);
		if (mIsFullScreenEditing) {
			mIsFullScreenEditing = false;
			try {
				((RelativeLayout) findViewById(R.id.formatBar))
						.setVisibility(View.GONE);
				LinearLayout contentEditorWrap = (LinearLayout) findViewById(R.id.postContentEditorWrapper);
				contentEditorWrap.removeView(mContentEditText);
				contentEditorWrap.setVisibility(View.GONE);
				LinearLayout smallEditorWrap = (LinearLayout) findViewById(R.id.postContentEditorSmallWrapper);
				smallEditorWrap.addView(mContentEditText);
				smallEditorWrap.setVisibility(View.VISIBLE);
				((ScrollView) findViewById(R.id.scrollView))
						.setVisibility(View.VISIBLE);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	private void launchPictureLibrary() {
		Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
		photoPickerIntent.setType("image/*");
		mCurrentActivityRequest = ACTIVITY_REQUEST_CODE_PICTURE_LIBRARY;
		startActivityForResult(photoPickerIntent,
				ACTIVITY_REQUEST_CODE_PICTURE_LIBRARY);
	}

	private void launchCamera() {
		String state = android.os.Environment.getExternalStorageState();
		if (!state.equals(android.os.Environment.MEDIA_MOUNTED)) {
			AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(
					EditPost.this);
			dialogBuilder.setTitle(getResources()
					.getText(R.string.sdcard_title));
			dialogBuilder.setMessage(getResources().getText(
					R.string.sdcard_message));
			dialogBuilder.setPositiveButton(getString(R.string.ok),
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog,
								int whichButton) {
							dialog.dismiss();
						}
					});
			dialogBuilder.setCancelable(true);
			dialogBuilder.create().show();
		} else {
			String dcimFolderName = Environment.DIRECTORY_DCIM;
			if (dcimFolderName == null)
				dcimFolderName = "DCIM";
			mMediaCapturePath = Environment.getExternalStorageDirectory()
					+ File.separator + dcimFolderName + File.separator + "Camera" + File.separator + "wp-"
					+ System.currentTimeMillis() + ".jpg";
			Intent takePictureFromCameraIntent = new Intent(
					MediaStore.ACTION_IMAGE_CAPTURE);
			takePictureFromCameraIntent.putExtra(
					android.provider.MediaStore.EXTRA_OUTPUT,
					Uri.fromFile(new File(mMediaCapturePath)));

			// make sure the directory we plan to store the recording in exists
			File directory = new File(mMediaCapturePath).getParentFile();
			if (!directory.exists() && !directory.mkdirs()) {
				try {
					throw new IOException("Path to file could not be created.");
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			mCurrentActivityRequest = ACTIVITY_REQUEST_CODE_TAKE_PHOTO;
			startActivityForResult(takePictureFromCameraIntent,
					ACTIVITY_REQUEST_CODE_TAKE_PHOTO);
		}
	}

	private void launchVideoLibrary() {
		Intent videoPickerIntent = new Intent(Intent.ACTION_PICK);
		videoPickerIntent.setType("video/*");
		mCurrentActivityRequest = ACTIVITY_REQUEST_CODE_VIDEO_LIBRARY;
		startActivityForResult(videoPickerIntent,
				ACTIVITY_REQUEST_CODE_VIDEO_LIBRARY);
	}

	private void launchVideoCamera() {
		mCurrentActivityRequest = ACTIVITY_REQUEST_CODE_TAKE_VIDEO;
		startActivityForResult(new Intent(MediaStore.ACTION_VIDEO_CAPTURE),
				ACTIVITY_REQUEST_CODE_TAKE_VIDEO);
	}

	private void evaluateSaveButtonText() {
		if (mStatusSpinner.getSelectedItemPosition() == 0)
			mSaveButton.setText(getString(R.string.publish_post));
		else
			mSaveButton.setText(getString(R.string.save));
	}

	private LocationResult locationResult = new LocationResult() {
		@Override
		public void gotLocation(Location location) {
			if (location != null) {
				mCurrentLocation = location;
				new getAddressTask().execute(mCurrentLocation.getLatitude(),
						mCurrentLocation.getLongitude());
			} else {
				runOnUiThread(new Runnable() {
					public void run() {
						mLocationText
								.setText(getString(R.string.location_not_found));
					}
				});
			}
		}
	};

	private void enableLBSButtons() {
		mLocationHelper = new LocationHelper();
		((RelativeLayout) findViewById(R.id.section4))
				.setVisibility(View.VISIBLE);
		Button viewMap = (Button) findViewById(R.id.viewMap);
		Button updateLocation = (Button) findViewById(R.id.updateLocation);
		Button removeLocation = (Button) findViewById(R.id.removeLocation);
		updateLocation.setOnClickListener(this);
		removeLocation.setOnClickListener(this);
		viewMap.setOnClickListener(this);
		if (mIsNew)
			mLocationHelper.getLocation(EditPost.this, locationResult);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		if (resultCode == RESULT_CANCELED) {
			if (mOption != null) {
				setResult(Activity.RESULT_CANCELED, new Intent());
				finish();
			}
			return;
		}

		if (data != null
				|| ((requestCode == ACTIVITY_REQUEST_CODE_TAKE_PHOTO || requestCode == ACTIVITY_REQUEST_CODE_TAKE_VIDEO))) {
			Bundle extras;

			switch (requestCode) {
			case ACTIVITY_REQUEST_CODE_PICTURE_LIBRARY:
				Uri imageUri = data.getData();
				String imgPath = imageUri.toString();
				addMedia(imgPath, imageUri);
				break;
			case ACTIVITY_REQUEST_CODE_TAKE_PHOTO:
				if (resultCode == Activity.RESULT_OK) {
					try {
						File f = new File(mMediaCapturePath);
						Uri capturedImageUri = Uri.fromFile(f);
						f = null;
						addMedia(capturedImageUri.toString(), capturedImageUri);
						sendBroadcast(new Intent(Intent.ACTION_MEDIA_MOUNTED, Uri.parse("file://"+ Environment.getExternalStorageDirectory())));
					} catch (Exception e) {
						e.printStackTrace();
					} catch (OutOfMemoryError e) {
						e.printStackTrace();
					}
				}
				break;
			case ACTIVITY_REQUEST_CODE_VIDEO_LIBRARY:
				Uri videoUri = data.getData();
				String videoPath = videoUri.toString();
				addMedia(videoPath, videoUri);
				break;
			case ACTIVITY_REQUEST_CODE_TAKE_VIDEO:
				if (resultCode == Activity.RESULT_OK) {
					Uri capturedVideo = data.getData();
					addMedia(capturedVideo.toString(), capturedVideo);
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
								str.setSpan(new URLSpan(linkURL),
										mSelectionStart, mSelectionStart
												+ linkURL.length(),
										Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
								mContentEditText.setSelection(mSelectionStart
										+ linkURL.length());
							} else {
								String linkText = extras.getString("linkText");
								if (mSelectionStart < mSelectionEnd)
									str.delete(mSelectionStart, mSelectionEnd);
								str.insert(mSelectionStart, linkText);
								str.setSpan(new URLSpan(linkURL),
										mSelectionStart, mSelectionStart
												+ linkText.length(),
										Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
								mContentEditText.setSelection(mSelectionStart
										+ linkText.length());
							}
						} else {
							if (extras.getString("linkText") == null) {
								if (mSelectionStart < mSelectionEnd)
									str.delete(mSelectionStart, mSelectionEnd);
								String urlHTML = "<a href=\"" + linkURL + "\">"
										+ linkURL + "</a>";
								str.insert(mSelectionStart, urlHTML);
								mContentEditText.setSelection(mSelectionStart
										+ urlHTML.length());
							} else {
								String linkText = extras.getString("linkText");
								if (mSelectionStart < mSelectionEnd)
									str.delete(mSelectionStart, mSelectionEnd);
								String urlHTML = "<a href=\"" + linkURL + "\">"
										+ linkText + "</a>";
								str.insert(mSelectionStart, urlHTML);
								mContentEditText.setSelection(mSelectionStart
										+ urlHTML.length());
							}
						}
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
				break;
			case ACTIVITY_REQUEST_CODE_SELECT_CATEGORIES:
				extras = data.getExtras();
				String cats = extras.getString("selectedCategories");
				String[] splitCats = cats.split(",");
				mCategories = new JSONArray();
				for (int i = 0; i < splitCats.length; i++) {
					mCategories.put(splitCats[i]);
				}
				mCategoriesText.setText(getString(R.string.selected_categories)
						+ " " + getCategoriesCSV());
				break;
			}
		}// end null check
	}

	@Override
	protected Dialog onCreateDialog(int id) {

		switch (id) {
		case ID_DIALOG_DATE:
			DatePickerDialog dpd = new DatePickerDialog(this, mDateSetListener,
					mYear, mMonth, mDay);
			dpd.setTitle("");
			return dpd;
		case ID_DIALOG_TIME:
			TimePickerDialog tpd = new TimePickerDialog(this, mTimeSetListener,
					mHour, mMinute, false);
			tpd.setTitle("");
			return tpd;
		case ID_DIALOG_LOADING:
			ProgressDialog loadingDialog = new ProgressDialog(this);
			loadingDialog.setMessage(getResources().getText(R.string.loading));
			loadingDialog.setIndeterminate(true);
			loadingDialog.setCancelable(true);
			return loadingDialog;
		}
		return super.onCreateDialog(id);
	}

	private boolean savePost(boolean autoSave) {

		String title = mTitleEditText.getText().toString();
		String password = mPasswordEditText.getText().toString();
		String pubDate = mPubDateText.getText().toString();
		String content = "";

		if (mLocalDraft || mIsNew && !autoSave) {
			Editable e = mContentEditText.getText();
			if (android.os.Build.VERSION.SDK_INT >= 14) {
				// remove suggestion spans, they cause craziness in
				// WPHtml.toHTML().
				CharacterStyle[] style = e.getSpans(0, e.length(),
						CharacterStyle.class);
				for (int i = 0; i < style.length; i++) {
					if (style[i].getClass().getName()
							.equals("android.text.style.SuggestionSpan"))
						e.removeSpan(style[i]);
				}
			}
			content = EscapeUtils.unescapeHtml(WPHtml.toHtml(e));
			// replace duplicate <p> tags so there's not duplicates, trac #86
			content = content.replace("<p><p>", "<p>");
			content = content.replace("</p></p>", "</p>");
			content = content.replace("<br><br>", "<br>");
			// sometimes the editor creates extra tags
			content = content.replace("</strong><strong>", "")
					.replace("</em><em>", "").replace("</u><u>", "")
					.replace("</strike><strike>", "")
					.replace("</blockquote><blockquote>", "");
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
			postFormat = mPostFormats[postFormatSpinner
					.getSelectedItemPosition()];
		}

		String images = "";
		boolean success = false;

		if (content.equals("") && !autoSave) {
			AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(
					EditPost.this);
			dialogBuilder.setTitle(getResources()
					.getText(R.string.empty_fields));
			dialogBuilder.setMessage(getResources().getText(
					R.string.title_post_required));
			dialogBuilder.setPositiveButton(getString(R.id.ok),
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog,
								int whichButton) {
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
				WPImageSpan[] click_spans = s.getSpans(0, s.length(),
						WPImageSpan.class);

				if (click_spans.length != 0) {

					for (int i = 0; i < click_spans.length; i++) {
						WPImageSpan wpIS = click_spans[i];
						images += wpIS.getImageSource().toString() + ",";

						MediaFile mf = new MediaFile();
						mf.setPostID(mPost.getId());
						mf.setTitle(wpIS.getTitle());
						mf.setCaption(wpIS.getCaption());
						mf.setDescription(wpIS.getDescription());
						mf.setFeatured(wpIS.isFeatured());
						mf.setFeaturedInPost(wpIS.isFeaturedInPost());
						mf.setFileName(wpIS.getImageSource().toString());
						mf.setHorizontalAlignment(wpIS.getHorizontalAlignment());
						mf.setWidth(wpIS.getWidth());
						mf.save(EditPost.this);

						int tagStart = s.getSpanStart(wpIS);
						if (!autoSave) {
							s.removeSpan(wpIS);
							s.insert(tagStart, "<img android-uri=\""
									+ wpIS.getImageSource().toString()
									+ "\" />");
							if (mLocalDraft)
								content = EscapeUtils.unescapeHtml(WPHtml
										.toHtml(s));
							else
								content = s.toString();
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
			case 4:
				status = "localdraft";
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
				mPost = new Post(mBlogID, title, content, images,
						pubDateTimestamp, mCategories.toString(), tags, status,
						password, latitude, longitude, mIsPage, postFormat,
						EditPost.this, true, false);
				mPost.setLocalDraft(true);

				// split up the post content if there's a more tag
				if (content.indexOf(moreTag) >= 0) {
					mPost.setDescription(content.substring(0,
							content.indexOf(moreTag)));
					mPost.setMt_text_more(content.substring(
							content.indexOf(moreTag) + moreTag.length(),
							content.length()));
				}

				success = mPost.save();

				if (success) {
					mIsNew = false;
					mIsNewDraft = true;
				}

				mPost.deleteMediaFiles();

				Spannable s = mContentEditText.getText();
				WPImageSpan[] image_spans = s.getSpans(0, s.length(),
						WPImageSpan.class);

				if (image_spans.length != 0) {

					for (int i = 0; i < image_spans.length; i++) {
						WPImageSpan wpIS = image_spans[i];
						images += wpIS.getImageSource().toString() + ",";

						MediaFile mf = new MediaFile();
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
						mf.save(EditPost.this);
					}
				}

				WordPress.currentPost = mPost;

			} else {

				if (mCurrentLocation == null) {
					latitude = mPost.getLatitude();
					longitude = mPost.getLongitude();
				}

				mPost.setTitle(title);
				// split up the post content if there's a more tag
				if (mLocalDraft && content.indexOf(moreTag) >= 0) {
					mPost.setDescription(content.substring(0,
							content.indexOf(moreTag)));
					mPost.setMt_text_more(content.substring(
							content.indexOf(moreTag) + moreTag.length(),
							content.length()));
				} else {
					mPost.setDescription(content);
					mPost.setMt_text_more("");
				}
				mPost.setMediaPaths(images);
				mPost.setDate_created_gmt(pubDateTimestamp);
				mPost.setCategories(mCategories);
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

	private class getAddressTask extends AsyncTask<Double, Void, String> {

		@Override
		protected String doInBackground(Double... args) {
			Geocoder gcd = new Geocoder(EditPost.this, Locale.getDefault());
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
					finalText = ((locality.equals("")) ? locality : locality
							+ ", ")
							+ ((adminArea.equals("")) ? adminArea : adminArea
									+ " ") + country;
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

			// It's a youtube video link! need to strip some parameters so the
			// embed will work
			if (text.contains("youtube_gdata")) {
				text = text.replace("feature=youtube_gdata", "");
				text = text.replace("&", "");
				text = text.replace("_player", "");
				text = text.replace("watch?v=", "v/");
				text = "<object width=\"480\" height=\"385\"><param name=\"movie\" value=\""
						+ text
						+ "\"></param><param name=\"allowFullScreen\" value=\"true\"></param><param name=\"allowscriptaccess\" value=\"always\"></param><embed src=\""
						+ text
						+ "\" type=\"application/x-shockwave-flash\" allowscriptaccess=\"always\" allowfullscreen=\"true\" width=\"480\" height=\"385\"></embed></object>";
				mContentEditText.setText(text);
			} else {
				// add link tag around URLs, trac #64
				text = text.replaceAll("((http|https|ftp|mailto):\\S+)",
						"<a href=\"$1\">$1</a>");
				mContentEditText.setText(WPHtml.fromHtml(
						StringHelper.addPTags(text), EditPost.this, mPost));
			}
		} else {
			String action = intent.getAction();
			final String type = intent.getType();
			final ArrayList<Uri> multi_stream;
			if (Intent.ACTION_SEND_MULTIPLE.equals(action)) {
				multi_stream = intent
						.getParcelableArrayListExtra((Intent.EXTRA_STREAM));
			} else {
				multi_stream = new ArrayList<Uri>();
				multi_stream.add((Uri) intent
						.getParcelableExtra(Intent.EXTRA_STREAM));
			}

			Vector<Serializable> params = new Vector<Serializable>();
			params.add(multi_stream);
			params.add(type);
			new processAttachmentsTask().execute(params);
		}
	}

	private class processAttachmentsTask extends
			AsyncTask<Vector<?>, Void, SpannableStringBuilder> {

		protected void onPreExecute() {
			showDialog(ID_DIALOG_LOADING);
		}

		@Override
		protected SpannableStringBuilder doInBackground(Vector<?>... args) {
			ArrayList<?> multi_stream = (ArrayList<?>) args[0].get(0);
			String type = (String) args[0].get(1);
			SpannableStringBuilder ssb = new SpannableStringBuilder();
			for (int i = 0; i < multi_stream.size(); i++) {
				Uri curStream = (Uri) multi_stream.get(i);
				if (curStream != null && type != null) {
					String imgPath = curStream.getEncodedPath();
					ssb = addMediaFromShareAction(imgPath, curStream, ssb);
				}
			}
			return ssb;
		}

		protected void onPostExecute(SpannableStringBuilder result) {
			dismissDialog(ID_DIALOG_LOADING);
			if (result != null) {
				if (result.length() > 0) {
					mContentEditText.setText(result);
				}
			}
		}
	}

	private void addMedia(String imgPath, Uri curStream) {

		Bitmap resizedBitmap = null;
		ImageHelper ih = new ImageHelper();
		Display display = getWindowManager().getDefaultDisplay();
		int width = display.getWidth();
		int height = display.getHeight();
		if (width > height)
			width = height;

		HashMap<String, Object> mediaData = ih.getImageBytesForPath(imgPath,
				EditPost.this);

		if (mediaData == null) {
			// data stream not returned
			Toast.makeText(EditPost.this,
					getResources().getText(R.string.gallery_error),
					Toast.LENGTH_SHORT).show();
			return;
		}

		BitmapFactory.Options opts = new BitmapFactory.Options();
		opts.inJustDecodeBounds = true;
		byte[] bytes = (byte[]) mediaData.get("bytes");
		BitmapFactory.decodeByteArray(bytes, 0, bytes.length, opts);

		float conversionFactor = 0.25f;

		if (opts.outWidth > opts.outHeight)
			conversionFactor = 0.40f;

		byte[] finalBytes = ih.createThumbnail(bytes,
				String.valueOf((int) (width * conversionFactor)),
				(String) mediaData.get("orientation"), true);

		resizedBitmap = BitmapFactory.decodeByteArray(finalBytes, 0,
				finalBytes.length);

		int selectionStart = mContentEditText.getSelectionStart();
		mStyleStart = selectionStart;
		int selectionEnd = mContentEditText.getSelectionEnd();

		if (selectionStart > selectionEnd) {
			int temp = selectionEnd;
			selectionEnd = selectionStart;
			selectionStart = temp;
		}

		Editable s = mContentEditText.getText();
		WPImageSpan is = new WPImageSpan(EditPost.this, resizedBitmap,
				curStream);

		String imageWidth = WordPress.currentBlog.getMaxImageWidth();
		if (!imageWidth.equals("Original Size")) {
			try {
				is.setWidth(Integer.valueOf(imageWidth));
			} catch (NumberFormatException e) {
				e.printStackTrace();
			}
		}

		is.setTitle((String) mediaData.get("title"));
		is.setImageSource(curStream);
		if (imgPath.contains("video")) {
			is.setVideo(true);
		}

		int line = 0, column = 0;
		try {
			line = mContentEditText.getLayout().getLineForOffset(selectionStart);
			column = mContentEditText.getSelectionStart() - mContentEditText.getLayout().getLineStart(line);
		} catch (Exception ex) {
		}
				
		
		WPImageSpan[] image_spans = s.getSpans(selectionStart, selectionEnd,
				WPImageSpan.class);
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
		s.setSpan(is, selectionStart, selectionEnd + 1,
				Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		AlignmentSpan.Standard as = new AlignmentSpan.Standard(
				Layout.Alignment.ALIGN_CENTER);
		s.setSpan(as, selectionStart, selectionEnd + 1,
				Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		s.insert(selectionEnd + 1, "\n\n");
		try {
			mContentEditText.setSelection(s.length());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public SpannableStringBuilder addMediaFromShareAction(String imgPath,
			Uri curStream, SpannableStringBuilder ssb) {
		initBlog();
		Bitmap resizedBitmap = null;
		String imageTitle = "";

		ImageHelper ih = new ImageHelper();
		Display display = getWindowManager().getDefaultDisplay();
		int width = display.getWidth();

		HashMap<?, ?> mediaData = ih.getImageBytesForPath(imgPath,
				EditPost.this);

		if (mediaData == null) {
			return null;
		}

		BitmapFactory.Options opts = new BitmapFactory.Options();
		opts.inJustDecodeBounds = true;
		byte[] bytes = (byte[]) mediaData.get("bytes");
		BitmapFactory.decodeByteArray(bytes, 0, bytes.length, opts);

		float conversionFactor = 0.25f;

		if (opts.outWidth > opts.outHeight)
			conversionFactor = 0.40f;

		byte[] finalBytes = ih.createThumbnail((byte[]) mediaData.get("bytes"),
				String.valueOf((int) (width * conversionFactor)),
				(String) mediaData.get("orientation"), true);

		resizedBitmap = BitmapFactory.decodeByteArray(finalBytes, 0,
				finalBytes.length);

		WPImageSpan is = new WPImageSpan(EditPost.this, resizedBitmap,
				curStream);

		String imageWidth = WordPress.currentBlog.getMaxImageWidth();
		if (!imageWidth.equals("Original Size")) {
			try {
				is.setWidth(Integer.valueOf(imageWidth));
			} catch (NumberFormatException e) {
				e.printStackTrace();
			}
		}

		is.setTitle(imageTitle);
		is.setImageSource(curStream);
		is.setVideo(imgPath.contains("video"));
		ssb.append(" ");
		ssb.setSpan(is, ssb.length() - 1, ssb.length(),
				Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		AlignmentSpan.Standard as = new AlignmentSpan.Standard(
				Layout.Alignment.ALIGN_CENTER);
		ssb.setSpan(as, ssb.length() - 1, ssb.length(),
				Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		ssb.append("\n");
		return ssb;
	}

	private String getCategoriesCSV() {
		String csv = "";
		if (mCategories.length() > 0) {
			for (int i = 0; i < mCategories.length(); i++) {
				try {
					csv += EscapeUtils.unescapeHtml(mCategories.getString(i))
							+ ",";
				} catch (JSONException e) {
					e.printStackTrace();
				}
			}
			csv = csv.substring(0, csv.length() - 1);
		}
		return csv;
	}

	private DatePickerDialog.OnDateSetListener mDateSetListener = new DatePickerDialog.OnDateSetListener() {
		public void onDateSet(DatePicker view, int year, int monthOfYear,
				int dayOfMonth) {
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
				String formattedDate = DateUtils.formatDateTime(EditPost.this,
						timestamp, flags);
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
			savePost(true);
			mAutoSaveHandler.postDelayed(this, AUTOSAVE_DELAY_MILLIS);
		}
	};

	@Override
	public void afterTextChanged(Editable s) {

		try {
			int position = Selection.getSelectionStart(mContentEditText
					.getText());
			if ((mIsBackspace && position != 1) || mLastPosition == position
					|| !mLocalDraft)
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
					StyleSpan[] ss = s.getSpans(mStyleStart, position,
							StyleSpan.class);
					exists = false;
					for (int i = 0; i < ss.length; i++) {
						if (ss[i].getStyle() == android.graphics.Typeface.BOLD) {
							exists = true;
						}
					}
					if (!exists)
						s.setSpan(
								new StyleSpan(android.graphics.Typeface.BOLD),
								mStyleStart, position,
								Spannable.SPAN_INCLUSIVE_INCLUSIVE);
				}
				if (mEmToggleButton.isChecked()) {
					StyleSpan[] ss = s.getSpans(mStyleStart, position,
							StyleSpan.class);
					exists = false;
					for (int i = 0; i < ss.length; i++) {
						if (ss[i].getStyle() == android.graphics.Typeface.ITALIC) {
							exists = true;
						}
					}
					if (!exists)
						s.setSpan(new StyleSpan(
								android.graphics.Typeface.ITALIC), mStyleStart,
								position, Spannable.SPAN_INCLUSIVE_INCLUSIVE);
				}
				if (mEmToggleButton.isChecked()) {
					StyleSpan[] ss = s.getSpans(mStyleStart, position,
							StyleSpan.class);
					exists = false;
					for (int i = 0; i < ss.length; i++) {
						if (ss[i].getStyle() == android.graphics.Typeface.ITALIC) {
							exists = true;
						}
					}
					if (!exists)
						s.setSpan(new StyleSpan(
								android.graphics.Typeface.ITALIC), mStyleStart,
								position, Spannable.SPAN_INCLUSIVE_INCLUSIVE);
				}
				if (mUnderlineToggleButton.isChecked()) {
					WPUnderlineSpan[] ss = s.getSpans(mStyleStart, position,
							WPUnderlineSpan.class);
					exists = false;
					for (int i = 0; i < ss.length; i++) {
						exists = true;
					}
					if (!exists)
						s.setSpan(new WPUnderlineSpan(), mStyleStart, position,
								Spannable.SPAN_INCLUSIVE_INCLUSIVE);
				}
				if (mStrikeToggleButton.isChecked()) {
					StrikethroughSpan[] ss = s.getSpans(mStyleStart, position,
							StrikethroughSpan.class);
					exists = false;
					for (int i = 0; i < ss.length; i++) {
						exists = true;
					}
					if (!exists)
						s.setSpan(new StrikethroughSpan(), mStyleStart,
								position, Spannable.SPAN_INCLUSIVE_INCLUSIVE);
				}
				if (mBquoteToggleButton.isChecked()) {

					QuoteSpan[] ss = s.getSpans(mStyleStart, position,
							QuoteSpan.class);
					exists = false;
					for (int i = 0; i < ss.length; i++) {
						exists = true;
					}
					if (!exists)
						s.setSpan(new QuoteSpan(), mStyleStart, position,
								Spannable.SPAN_INCLUSIVE_INCLUSIVE);
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
		Object[] spans = s.getSpans(mContentEditText.getSelectionStart(),
				mContentEditText.getSelectionStart(), Object.class);

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

	@Override
	public void onImeBack(WPEditText ctrl, String text) {
		if ((DeviceUtils.getInstance().isBlackBerry() || DeviceUtils.getInstance().isKindleFire()) && mIsFullScreenEditing) 
			mImeBackPressed = true;
		finishEditing();
	}
}
