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
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Address;
import android.location.Criteria;
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
import android.util.Log;
import android.view.ContextMenu;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
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

public class EditPost extends Activity {
	/** Called when the activity is first created. */
	public ProgressDialog pd;
	Vector<String> selectedCategories = new Vector<String>();
	public String categoryErrorMsg = "", accountName = "", option, provider,
			SD_CARD_TEMP_DIR = "";
	private JSONArray categories;
	private int id;
	long postID, customPubDate = 0;
	private int ID_DIALOG_DATE = 0, ID_DIALOG_TIME = 1, ID_DIALOG_LOADING = 2;
	public Boolean localDraft = false, isPage = false, isNew = false,
			isAction = false, isUrl = false, isLargeScreen = false,
			isCustomPubDate = false, isFullScreenEditing = false,
			isBackspace = false, imeBackPressed = false,
			scrollDetected = false, isNewDraft = false;
	Criteria criteria;
	Location curLocation;
	ProgressDialog postingDialog;
	int cursorLoc = 0, screenDensity = 0;
	// date holders
	private int mYear, mMonth, mDay, mHour, mMinute, styleStart,
			selectionStart, selectionEnd, lastPosition = -1;
	private Blog blog;
	private Post post;
	// post formats
	String[] postFormats;
	String[] postFormatTitles = null;
	LocationHelper locationHelper;
	float lastYPos = 0;
	private Handler autoSaveHandler = new Handler();

	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		Bundle extras = getIntent().getExtras();

		// need to make sure we have db and currentBlog on views that don't use
		// the Action Bar
		if (WordPress.wpDB == null)
			WordPress.wpDB = new WordPressDB(this);
		if (WordPress.currentBlog == null) {
			try {
				WordPress.currentBlog = new Blog(
						WordPress.wpDB.getLastBlogID(this), this);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		Display display = ((WindowManager) getSystemService(Context.WINDOW_SERVICE))
				.getDefaultDisplay();
		int width = display.getWidth();
		int height = display.getHeight();
		if (height > width) {
			width = height;
		}
		if (width > 480) {
			isLargeScreen = true;
		}

		categories = new JSONArray();
		String action = getIntent().getAction();

		if (Intent.ACTION_SEND.equals(action)
				|| Intent.ACTION_SEND_MULTIPLE.equals(action)) {
			// we arrived here from a share action
			isAction = true;
			isNew = true;
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
						blog = new Blog(accountIDs[i], EditPost.this);
					} catch (Exception e) {
						Toast.makeText(
								this,
								getResources().getText(R.string.blog_not_found),
								Toast.LENGTH_SHORT).show();
						finish();
					}

				}

				// Don't prompt if they have one blog only
				if (accounts.size() != 1) {
					AlertDialog.Builder builder = new AlertDialog.Builder(
							EditPost.this);
					builder.setCancelable(false);
					builder.setTitle(getResources().getText(
							R.string.select_a_blog));
					builder.setItems(blogNames,
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int item) {
									id = accountIDs[item];
									try {
										blog = new Blog(id, EditPost.this);
									} catch (Exception e) {
										Toast.makeText(
												EditPost.this,
												getResources()
														.getText(
																R.string.blog_not_found),
												Toast.LENGTH_SHORT).show();
										finish();
									}
									WordPress.currentBlog = blog;
									WordPress.wpDB
											.updateLastBlogID(WordPress.currentBlog
													.getId());
									accountName = blogNames[item];
									setTitle(EscapeUtils
											.unescapeHtml(accountName)
											+ " - "
											+ getResources()
													.getText(
															(isPage) ? R.string.new_page
																	: R.string.new_post));
								}
							});
					AlertDialog alert = builder.create();
					alert.show();
				} else {
					id = accountIDs[0];
					try {
						blog = new Blog(id, EditPost.this);
					} catch (Exception e) {
						Toast.makeText(
								this,
								getResources().getText(R.string.blog_not_found),
								Toast.LENGTH_SHORT).show();
						finish();
					}
					WordPress.currentBlog = blog;
					WordPress.wpDB.updateLastBlogID(WordPress.currentBlog
							.getId());
					accountName = blogNames[0];
					setTitle(EscapeUtils.unescapeHtml(accountName)
							+ " - "
							+ getResources().getText(
									(isPage) ? R.string.new_page
											: R.string.new_post));
				}
			} else {
				// no account, load main view to load new account view
				Intent i = new Intent(this, Dashboard.class);
				Toast.makeText(getApplicationContext(),
						getResources().getText(R.string.no_account),
						Toast.LENGTH_LONG).show();
				startActivity(i);
				finish();
				return;
			}

		} else {

			if (extras != null) {
				id = WordPress.currentBlog.getId();
				try {
					blog = new Blog(id, this);
				} catch (Exception e) {
					Toast.makeText(this,
							getResources().getText(R.string.blog_not_found),
							Toast.LENGTH_SHORT).show();
					finish();
				}
				accountName = EscapeUtils.unescapeHtml(extras
						.getString("accountName"));
				postID = extras.getLong("postID");
				localDraft = extras.getBoolean("localDraft", false);
				isPage = extras.getBoolean("isPage", false);
				isNew = extras.getBoolean("isNew", false);
				option = extras.getString("option");

				if (extras.getBoolean("isQuickPress")) {
					id = extras.getInt("id");
					try {
						blog = new Blog(id, this);
						WordPress.currentBlog = blog;
					} catch (Exception e) {
						Toast.makeText(
								this,
								getResources().getText(R.string.blog_not_found),
								Toast.LENGTH_LONG).show();
						finish();
						return;
					}
				}

				if (!isNew) {
					try {
						post = new Post(id, postID, isPage, this);
						if (post == null) {
							// big oopsie
							Toast.makeText(
									this,
									getResources().getText(
											R.string.post_not_found),
									Toast.LENGTH_LONG).show();
							finish();
							return;
						} else {
							WordPress.currentPost = post;
						}
					} catch (Exception e) {
						finish();
					}
				}
			}

			if (isNew) {
				localDraft = true;
				setTitle(EscapeUtils.unescapeHtml(WordPress.currentBlog
						.getBlogName())
						+ " - "
						+ getResources().getText(
								(isPage) ? R.string.new_page
										: R.string.new_post));
			} else {
				setTitle(EscapeUtils.unescapeHtml(WordPress.currentBlog
						.getBlogName())
						+ " - "
						+ getResources().getText(
								(isPage) ? R.string.edit_page
										: R.string.edit_post));
			}
		}

		setContentView(R.layout.edit);
		if (isPage) {
			// remove post specific views
			RelativeLayout section3 = (RelativeLayout) findViewById(R.id.section3);
			section3.setVisibility(View.GONE);
			RelativeLayout locationWrapper = (RelativeLayout) findViewById(R.id.location_wrapper);
			locationWrapper.setVisibility(View.GONE);
			TextView postFormatLabel = (TextView) findViewById(R.id.postFormatLabel);
			postFormatLabel.setVisibility(View.GONE);
			Spinner postFormatSpinner = (Spinner) findViewById(R.id.postFormat);
			postFormatSpinner.setVisibility(View.GONE);
		} else {
			if (blog.getPostFormats().equals("")) {
				Vector<Object> args = new Vector<Object>();
				args.add(blog);
				args.add(this);
				new ApiHelper.getPostFormatsTask().execute(args);
				postFormatTitles = getResources().getStringArray(
						R.array.post_formats_array);
				String defaultPostFormatTitles[] = { "aside", "audio", "chat",
						"gallery", "image", "link", "quote", "standard",
						"status", "video" };
				postFormats = defaultPostFormatTitles;
			} else {
				try {
					JSONObject jsonPostFormats = new JSONObject(
							blog.getPostFormats());
					postFormats = new String[jsonPostFormats.length()];
					postFormatTitles = new String[jsonPostFormats.length()];
					Iterator<?> it = jsonPostFormats.keys();
					int i = 0;
					while (it.hasNext()) {
						String key = (String) it.next();
						String val = (String) jsonPostFormats.get(key);
						postFormats[i] = key;
						postFormatTitles[i] = val;
						i++;
					}

				} catch (JSONException e) {
					e.printStackTrace();
				}
			}
			Spinner pfSpinner = (Spinner) findViewById(R.id.postFormat);
			ArrayAdapter<String> pfAdapter = new ArrayAdapter<String>(this,
					android.R.layout.simple_spinner_item, postFormatTitles);
			pfAdapter
					.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
			pfSpinner.setAdapter(pfAdapter);
			String activePostFormat = "standard";
			if (!isNew) {
				try {
					if (!post.getWP_post_format().equals(""))
						activePostFormat = post.getWP_post_format();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			for (int i = 0; i < postFormats.length; i++) {
				if (postFormats[i].equals(activePostFormat))
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
		Spinner spinner = (Spinner) findViewById(R.id.status);
		ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
				android.R.layout.simple_spinner_item, items);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spinner.setAdapter(adapter);

		spinner.setOnItemSelectedListener(new OnItemSelectedListener() {

			@Override
			public void onItemSelected(AdapterView<?> arg0, View arg1,
					int arg2, long arg3) {
				evaluateSaveButtonText();

			}

			@Override
			public void onNothingSelected(AdapterView<?> arg0) {
			}

		});

		boolean hasLocationProvider = false;
		LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
		List<String> providers = locationManager.getProviders(true);
		for (String providerName : providers) {
			if (providerName.equals(LocationManager.GPS_PROVIDER)
					|| providerName.equals(LocationManager.NETWORK_PROVIDER)) {
				hasLocationProvider = true;
			}
		}

		if (hasLocationProvider && blog.isLocation() && !isPage) {
			enableLBSButtons();
		}

		if (isNew) {

			// handles selections from the quick action bar
			if (option != null) {
				if (option.equals("newphoto"))
					launchCamera();
				else if (option.equals("photolibrary"))
					launchPictureLibrary();
				else if (option.equals("newvideo"))
					launchVideoCamera();
				else if (option.equals("videolibrary"))
					launchVideoLibrary();

				localDraft = extras.getBoolean("localDraft");
			}

		} else {
			EditText titleET = (EditText) findViewById(R.id.title);
			WPEditText contentET = (WPEditText) findViewById(R.id.postContent);
			EditText passwordET = (EditText) findViewById(R.id.post_password);

			titleET.setText(post.getTitle());

			if (post.isUploaded()) {
				items = new String[] {
						getResources().getString(R.string.publish_post),
						getResources().getString(R.string.draft),
						getResources().getString(R.string.pending_review),
						getResources().getString(R.string.post_private) };
				adapter = new ArrayAdapter<String>(this,
						android.R.layout.simple_spinner_item, items);
				spinner.setAdapter(adapter);
			}

			String contentHTML;

			if (!post.getMt_text_more().equals("")) {
				if (post.isLocalDraft())
					contentHTML = post.getDescription()
							+ "\n&lt;!--more--&gt;\n" + post.getMt_text_more();
				else
					contentHTML = post.getDescription() + "\n<!--more-->\n"
							+ post.getMt_text_more();
			} else {
				contentHTML = post.getDescription();
			}

			try {
				if (post.isLocalDraft()) {
					contentET.setText(WPHtml.fromHtml(
							contentHTML.replaceAll("\uFFFC", ""),
							EditPost.this, post));
				} else {
					contentET.setText(contentHTML.replaceAll("\uFFFC", ""));
				}

			} catch (Exception e1) {
				e1.printStackTrace();
			}

			long pubDate = post.getDate_created_gmt();
			if (pubDate != 0) {
				try {
					int flags = 0;
					flags |= android.text.format.DateUtils.FORMAT_SHOW_DATE;
					flags |= android.text.format.DateUtils.FORMAT_ABBREV_MONTH;
					flags |= android.text.format.DateUtils.FORMAT_SHOW_YEAR;
					flags |= android.text.format.DateUtils.FORMAT_SHOW_TIME;
					String formattedDate = DateUtils.formatDateTime(
							EditPost.this, pubDate, flags);
					TextView tvPubDate = (TextView) findViewById(R.id.pubDate);
					tvPubDate.setText(formattedDate);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}

			if (post.getWP_password() != null)
				passwordET.setText(post.getWP_password());

			if (post.getPost_status() != null) {
				String status = post.getPost_status();

				if (status.equals("publish")) {
					spinner.setSelection(0, true);
				} else if (status.equals("draft")) {
					spinner.setSelection(1, true);
				} else if (status.equals("pending")) {
					spinner.setSelection(2, true);
				} else if (status.equals("private")) {
					spinner.setSelection(3, true);
				} else if (status.equals("localdraft")) {
					spinner.setSelection(4, true);
				}

				evaluateSaveButtonText();
			}

			if (!isPage) {
				if (post.getCategories() != null) {
					categories = post.getCategories();
					if (!categories.equals("")) {

						for (int i = 0; i < categories.length(); i++) {
							try {
								selectedCategories.add(categories.getString(i));
							} catch (JSONException e) {
								e.printStackTrace();
							}
						}

						TextView tvCategories = (TextView) findViewById(R.id.selectedCategories);
						tvCategories.setText(getResources().getText(
								R.string.selected_categories)
								+ " " + getCategoriesCSV());

					}
				}

				Double latitude = post.getLatitude();
				Double longitude = post.getLongitude();

				if (latitude != 0.0) {
					new getAddressTask().execute(latitude, longitude);
				}

			}

			String tags = post.getMt_keywords();
			if (!tags.equals("")) {
				EditText tagsET = (EditText) findViewById(R.id.tags);
				tagsET.setText(tags);
			}
		}

		if (!isPage) {
			Button selectCategories = (Button) findViewById(R.id.selectCategories);

			selectCategories.setOnClickListener(new Button.OnClickListener() {
				public void onClick(View v) {

					Bundle bundle = new Bundle();
					bundle.putInt("id", id);
					if (categories.length() > 0) {
						bundle.putString("categoriesCSV", getCategoriesCSV());
					}
					Intent i = new Intent(EditPost.this, SelectCategories.class);
					i.putExtras(bundle);
					startActivityForResult(i, 5);
				}
			});
		}

		final WPEditText content = (WPEditText) findViewById(R.id.postContent);

		content.setOnTouchListener(new View.OnTouchListener() {

			@Override
			public boolean onTouch(View v, MotionEvent event) {

				float pos = event.getY();

				if (event.getAction() == 0)
					lastYPos = pos;

				if (event.getAction() > 1) {
					if (((lastYPos - pos) > 2.0f) || ((pos - lastYPos) > 2.0f))
						scrollDetected = true;
				}

				lastYPos = pos;

				if (!isFullScreenEditing && event.getAction() == 1) {
					isFullScreenEditing = true;
					content.setFocusableInTouchMode(true);
					try {
						LinearLayout smallEditorWrap = (LinearLayout) findViewById(R.id.postContentEditorSmallWrapper);
						smallEditorWrap.removeView(content);
						ScrollView scrollView = (ScrollView) findViewById(R.id.scrollView);
						scrollView.setVisibility(View.GONE);
						LinearLayout contentEditorWrap = (LinearLayout) findViewById(R.id.postContentEditorWrapper);
						contentEditorWrap.addView(content);
						contentEditorWrap.setVisibility(View.VISIBLE);
						RelativeLayout formatBar = (RelativeLayout) findViewById(R.id.formatBar);
						formatBar.setVisibility(View.VISIBLE);
					} catch (Exception e) {
						e.printStackTrace();
					}
					content.requestFocus();
					return false;
				}

				if (event.getAction() == 1 && !scrollDetected
						&& isFullScreenEditing) {
					Layout layout = ((TextView) v).getLayout();
					int x = (int) event.getX();
					int y = (int) event.getY();

					x += v.getScrollX();
					y += v.getScrollY();
					if (layout != null) {
						int line = layout.getLineForVertical(y);
						int charPosition = layout.getOffsetForHorizontal(line,
								x);

						final Spannable s = content.getText();
						// check if image span was tapped
						WPImageSpan[] click_spans = s.getSpans(charPosition,
								charPosition, WPImageSpan.class);

						if (click_spans.length != 0) {
							final WPImageSpan span = click_spans[0];
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
								// alertView
								// .findViewById(R.id.description);
								final EditText caption = (EditText) alertView
										.findViewById(R.id.caption);
								// final CheckBox featured = (CheckBox)
								// alertView
								// .findViewById(R.id.featuredImage);
								final SeekBar seekBar = (SeekBar) alertView
										.findViewById(R.id.imageWidth);
								final Spinner alignmentSpinner = (Spinner) alertView
										.findViewById(R.id.alignment_spinner);
								ArrayAdapter<CharSequence> adapter = ArrayAdapter
										.createFromResource(
												EditPost.this,
												R.array.alignment_array,
												android.R.layout.simple_spinner_item);
								adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
								alignmentSpinner.setAdapter(adapter);

								imageWidthText.setText(String.valueOf(span
										.getWidth()) + "px");
								seekBar.setProgress(span.getWidth());
								titleText.setText(span.getTitle());
								// descText.setText(span.getDescription());
								caption.setText(span.getCaption());
								// featured.setChecked(span.isFeatured());

								alignmentSpinner.setSelection(
										span.getHorizontalAlignment(), true);

								seekBar.setMax(100);
								if (span.getWidth() != 0)
									seekBar.setProgress(span.getWidth() / 10);
								seekBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {

									@Override
									public void onStopTrackingTouch(
											SeekBar seekBar) {
									}

									@Override
									public void onStartTrackingTouch(
											SeekBar seekBar) {
									}

									@Override
									public void onProgressChanged(
											SeekBar seekBar, int progress,
											boolean fromUser) {
										if (progress == 0)
											progress = 1;
										imageWidthText.setText(progress * 10
												+ "px");
									}
								});

								AlertDialog ad = new AlertDialog.Builder(
										EditPost.this)
										.setTitle("Image Settings")
										.setView(alertView)
										.setPositiveButton(
												"OK",
												new DialogInterface.OnClickListener() {
													public void onClick(
															DialogInterface dialog,
															int whichButton) {

														span.setTitle(titleText
																.getText()
																.toString());
														// span.setDescription(descText
														// .getText().toString());

														span.setHorizontalAlignment(alignmentSpinner
																.getSelectedItemPosition());
														span.setWidth(seekBar
																.getProgress() * 10);
														span.setCaption(caption
																.getText()
																.toString());
														// span.setFeatured(featured
														// .isChecked());

													}
												})
										.setNegativeButton(
												"Cancel",
												new DialogInterface.OnClickListener() {
													public void onClick(
															DialogInterface dialog,
															int whichButton) {

													}
												}).create();
								ad.show();
								scrollDetected = false;
								return true;
							}

						} else {
							content.setMovementMethod(ArrowKeyMovementMethod
									.getInstance());
							content.setSelection(content.getSelectionStart());
						}
					}
				} else if (event.getAction() == 1) {
					scrollDetected = false;
				}
				return false;
			}
		});

		content.setOnSelectionChangedListener(new WPEditText.OnSelectionChangedListener() {

			@Override
			public void onSelectionChanged() {
				if (!localDraft)
					return;

				final Spannable s = content.getText();
				// set toggle buttons if cursor is inside of a matching
				// span
				styleStart = content.getSelectionStart();
				Object[] spans = s.getSpans(content.getSelectionStart(),
						content.getSelectionStart(), Object.class);
				ToggleButton boldButton = (ToggleButton) findViewById(R.id.bold);
				ToggleButton emButton = (ToggleButton) findViewById(R.id.em);
				ToggleButton bquoteButton = (ToggleButton) findViewById(R.id.bquote);
				ToggleButton underlineButton = (ToggleButton) findViewById(R.id.underline);
				ToggleButton strikeButton = (ToggleButton) findViewById(R.id.strike);
				boldButton.setChecked(false);
				emButton.setChecked(false);
				bquoteButton.setChecked(false);
				underlineButton.setChecked(false);
				strikeButton.setChecked(false);
				for (Object span : spans) {
					if (span instanceof StyleSpan) {
						StyleSpan ss = (StyleSpan) span;
						if (ss.getStyle() == android.graphics.Typeface.BOLD) {
							boldButton.setChecked(true);
						}
						if (ss.getStyle() == android.graphics.Typeface.ITALIC) {
							emButton.setChecked(true);
						}
					}
					if (span instanceof QuoteSpan) {
						bquoteButton.setChecked(true);
					}
					if (span instanceof WPUnderlineSpan) {
						underlineButton.setChecked(true);
					}
					if (span instanceof StrikethroughSpan) {
						strikeButton.setChecked(true);
					}
				}
			}
		});

		content.setOnEditTextImeBackListener(new WPEditText.EditTextImeBackListener() {

			@Override
			public void onImeBack(WPEditText view, String text) {
				finishEditing();
				imeBackPressed = true;
			}

		});

		content.addTextChangedListener(new TextWatcher() {
			public void afterTextChanged(Editable s) {

				try {
					int position = Selection.getSelectionStart(content
							.getText());
					if ((isBackspace && position != 1)
							|| lastPosition == position || !localDraft)
						return;

					// add style as the user types if a toggle button is enabled
					ToggleButton boldButton = (ToggleButton) findViewById(R.id.bold);
					ToggleButton emButton = (ToggleButton) findViewById(R.id.em);
					ToggleButton bquoteButton = (ToggleButton) findViewById(R.id.bquote);
					ToggleButton underlineButton = (ToggleButton) findViewById(R.id.underline);
					ToggleButton strikeButton = (ToggleButton) findViewById(R.id.strike);

					if (position < 0) {
						position = 0;
					}
					lastPosition = position;
					if (position > 0) {

						if (styleStart > position) {
							styleStart = position - 1;
						}
						boolean exists = false;
						if (boldButton.isChecked()) {
							StyleSpan[] ss = s.getSpans(styleStart, position,
									StyleSpan.class);
							exists = false;
							for (int i = 0; i < ss.length; i++) {
								if (ss[i].getStyle() == android.graphics.Typeface.BOLD) {
									exists = true;
								}
							}
							if (!exists)
								s.setSpan(new StyleSpan(
										android.graphics.Typeface.BOLD),
										styleStart, position,
										Spannable.SPAN_INCLUSIVE_INCLUSIVE);
						}
						if (emButton.isChecked()) {
							StyleSpan[] ss = s.getSpans(styleStart, position,
									StyleSpan.class);
							exists = false;
							for (int i = 0; i < ss.length; i++) {
								if (ss[i].getStyle() == android.graphics.Typeface.ITALIC) {
									exists = true;
								}
							}
							if (!exists)
								s.setSpan(new StyleSpan(
										android.graphics.Typeface.ITALIC),
										styleStart, position,
										Spannable.SPAN_INCLUSIVE_INCLUSIVE);
						}
						if (emButton.isChecked()) {
							StyleSpan[] ss = s.getSpans(styleStart, position,
									StyleSpan.class);
							exists = false;
							for (int i = 0; i < ss.length; i++) {
								if (ss[i].getStyle() == android.graphics.Typeface.ITALIC) {
									exists = true;
								}
							}
							if (!exists)
								s.setSpan(new StyleSpan(
										android.graphics.Typeface.ITALIC),
										styleStart, position,
										Spannable.SPAN_INCLUSIVE_INCLUSIVE);
						}
						if (underlineButton.isChecked()) {
							WPUnderlineSpan[] ss = s.getSpans(styleStart,
									position, WPUnderlineSpan.class);
							exists = false;
							for (int i = 0; i < ss.length; i++) {
								exists = true;
							}
							if (!exists)
								s.setSpan(new WPUnderlineSpan(), styleStart,
										position,
										Spannable.SPAN_INCLUSIVE_INCLUSIVE);
						}
						if (strikeButton.isChecked()) {
							StrikethroughSpan[] ss = s.getSpans(styleStart,
									position, StrikethroughSpan.class);
							exists = false;
							for (int i = 0; i < ss.length; i++) {
								exists = true;
							}
							if (!exists)
								s.setSpan(new StrikethroughSpan(), styleStart,
										position,
										Spannable.SPAN_INCLUSIVE_INCLUSIVE);
						}
						if (bquoteButton.isChecked()) {

							QuoteSpan[] ss = s.getSpans(styleStart, position,
									QuoteSpan.class);
							exists = false;
							for (int i = 0; i < ss.length; i++) {
								exists = true;
							}
							if (!exists)
								s.setSpan(new QuoteSpan(), styleStart,
										position,
										Spannable.SPAN_INCLUSIVE_INCLUSIVE);
						}
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}

			public void beforeTextChanged(CharSequence s, int start, int count,
					int after) {

				if ((count - after == 1) || (s.length() == 0))
					isBackspace = true;
				else
					isBackspace = false;
			}

			public void onTextChanged(CharSequence s, int start, int before,
					int count) {
			}

		});

		final ImageButton addPictureButton = (ImageButton) findViewById(R.id.addPictureButton);
		registerForContextMenu(addPictureButton);
		addPictureButton.setOnClickListener(new Button.OnClickListener() {
			public void onClick(View v) {

				addPictureButton.performLongClick();

			}
		});

		final Button saveButton = (Button) findViewById(R.id.post);

		saveButton.setOnClickListener(new Button.OnClickListener() {
			public void onClick(View v) {
				if (autoSaveHandler != null)
					autoSaveHandler.removeCallbacks(autoSaveRunnable);
				
				boolean result = savePost(false);
				if (result) {
					if (post.isUploaded() || !post.getPost_status().equals("localdraft")) {
						if (option != null) {
							if (option.equals("newphoto") || option.equals("photolibrary"))
								post.setQuickPostType("QuickPhoto");
							else if (option.equals("newvideo") || option.equals("videolibrary"))
								post.setQuickPostType("QuickVideo");
						}
						post.upload();
					}
					finish();
				}
			}
		});

		Button pubDate = (Button) findViewById(R.id.pubDateButton);
		pubDate.setOnClickListener(new TextView.OnClickListener() {
			public void onClick(View v) {

				// get the current date
				Calendar c = Calendar.getInstance();
				mYear = c.get(Calendar.YEAR);
				mMonth = c.get(Calendar.MONTH);
				mDay = c.get(Calendar.DAY_OF_MONTH);
				mHour = c.get(Calendar.HOUR_OF_DAY);
				mMinute = c.get(Calendar.MINUTE);

				showDialog(ID_DIALOG_DATE);

			}
		});

		final ToggleButton boldButton = (ToggleButton) findViewById(R.id.bold);
		boldButton.setOnClickListener(new Button.OnClickListener() {
			public void onClick(View v) {

				formatBtnClick(boldButton, "strong");
			}
		});

		final Button linkButton = (Button) findViewById(R.id.link);

		linkButton.setOnClickListener(new Button.OnClickListener() {
			public void onClick(View v) {

				WPEditText contentText = (WPEditText) findViewById(R.id.postContent);

				selectionStart = contentText.getSelectionStart();

				styleStart = selectionStart;

				selectionEnd = contentText.getSelectionEnd();

				if (selectionStart > selectionEnd) {
					int temp = selectionEnd;
					selectionEnd = selectionStart;
					selectionStart = temp;
				}

				Intent i = new Intent(EditPost.this, Link.class);
				if (selectionEnd > selectionStart) {
					String selectedText = contentText.getText()
							.subSequence(selectionStart, selectionEnd)
							.toString();
					i.putExtra("selectedText", selectedText);
				}
				startActivityForResult(i, 4);
			}
		});

		final ToggleButton emButton = (ToggleButton) findViewById(R.id.em);
		emButton.setOnClickListener(new Button.OnClickListener() {
			public void onClick(View v) {

				formatBtnClick(emButton, "em");
			}
		});

		final ToggleButton underlineButton = (ToggleButton) findViewById(R.id.underline);
		underlineButton.setOnClickListener(new Button.OnClickListener() {
			public void onClick(View v) {

				formatBtnClick(underlineButton, "u");
			}
		});

		final ToggleButton strikeButton = (ToggleButton) findViewById(R.id.strike);
		strikeButton.setOnClickListener(new Button.OnClickListener() {
			public void onClick(View v) {

				formatBtnClick(strikeButton, "strike");
			}
		});

		final ToggleButton bquoteButton = (ToggleButton) findViewById(R.id.bquote);
		bquoteButton.setOnClickListener(new Button.OnClickListener() {
			public void onClick(View v) {

				formatBtnClick(bquoteButton, "blockquote");

			}
		});

		final Button moreButton = (Button) findViewById(R.id.more);
		moreButton.setOnClickListener(new Button.OnClickListener() {
			public void onClick(View v) {
				WPEditText contentText = (WPEditText) findViewById(R.id.postContent);
				selectionEnd = contentText.getSelectionEnd();

				Editable str = contentText.getText();
				str.insert(selectionEnd, "\n\n<!--more-->\n\n");
			}
		});
	}

	protected void formatBtnClick(ToggleButton toggleButton, String tag) {
		try {
			WPEditText contentText = (WPEditText) findViewById(R.id.postContent);
			Spannable s = contentText.getText();

			int selectionStart = contentText.getSelectionStart();

			styleStart = selectionStart;

			int selectionEnd = contentText.getSelectionEnd();

			if (selectionStart > selectionEnd) {
				int temp = selectionEnd;
				selectionEnd = selectionStart;
				selectionStart = temp;
			}

			if (localDraft) {
				if (selectionEnd > selectionStart) {
					Spannable str = contentText.getText();
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

						StyleSpan[] ss = s.getSpans(styleStart - 1, styleStart,
								StyleSpan.class);

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
						WPUnderlineSpan[] us = s.getSpans(styleStart - 1,
								styleStart, WPUnderlineSpan.class);
						for (int i = 0; i < us.length; i++) {
							int tagStart = s.getSpanStart(us[i]);
							int tagEnd = s.getSpanEnd(us[i]);
							s.removeSpan(us[i]);
							s.setSpan(new WPUnderlineSpan(), tagStart, tagEnd,
									Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
						}
					} else if (tag.equals("strike")) {
						StrikethroughSpan[] ss = s.getSpans(styleStart - 1,
								styleStart, StrikethroughSpan.class);
						for (int i = 0; i < ss.length; i++) {
							int tagStart = s.getSpanStart(ss[i]);
							int tagEnd = s.getSpanEnd(ss[i]);
							s.removeSpan(ss[i]);
							s.setSpan(new StrikethroughSpan(), tagStart,
									tagEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
						}
					} else if (tag.equals("blockquote")) {
						QuoteSpan[] ss = s.getSpans(styleStart - 1, styleStart,
								QuoteSpan.class);
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
				Editable content = contentText.getText();
				if (selectionEnd > selectionStart) {
					content.insert(selectionStart, startTag);
					content.insert(selectionEnd + startTag.length(), endTag);
					toggleButton.setChecked(false);
					contentText.setSelection(selectionEnd + startTag.length()
							+ endTag.length());
				} else if (toggleButton.isChecked()) {
					content.insert(selectionStart, startTag);
					contentText.setSelection(selectionEnd + startTag.length());
				} else if (!toggleButton.isChecked()) {
					content.insert(selectionEnd, endTag);
					contentText.setSelection(selectionEnd + endTag.length());
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	protected void finishEditing() {
		WPEditText content = (WPEditText) findViewById(R.id.postContent);

		if (isFullScreenEditing) {
			isFullScreenEditing = false;
			try {
				RelativeLayout formatBar = (RelativeLayout) findViewById(R.id.formatBar);
				formatBar.setVisibility(View.GONE);
				LinearLayout contentEditorWrap = (LinearLayout) findViewById(R.id.postContentEditorWrapper);
				contentEditorWrap.removeView(content);
				contentEditorWrap.setVisibility(View.GONE);
				LinearLayout smallEditorWrap = (LinearLayout) findViewById(R.id.postContentEditorSmallWrapper);
				smallEditorWrap.addView(content);
				smallEditorWrap.setVisibility(View.VISIBLE);
				ScrollView scrollView = (ScrollView) findViewById(R.id.scrollView);
				scrollView.setVisibility(View.VISIBLE);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

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
		}
		return false;
	}

	private void launchPictureLibrary() {
		Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
		photoPickerIntent.setType("image/*");
		startActivityForResult(photoPickerIntent, 0);
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
			dialogBuilder.setPositiveButton("OK",
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog,
								int whichButton) {
							// just close the dialog

						}
					});
			dialogBuilder.setCancelable(true);
			dialogBuilder.create().show();
		} else {
			SD_CARD_TEMP_DIR = Environment.getExternalStorageDirectory()
					+ File.separator + "wordpress" + File.separator + "wp-"
					+ System.currentTimeMillis() + ".jpg";
			Intent takePictureFromCameraIntent = new Intent(
					MediaStore.ACTION_IMAGE_CAPTURE);
			takePictureFromCameraIntent.putExtra(
					android.provider.MediaStore.EXTRA_OUTPUT,
					Uri.fromFile(new File(SD_CARD_TEMP_DIR)));

			// make sure the directory we plan to store the recording in exists
			File directory = new File(SD_CARD_TEMP_DIR).getParentFile();
			if (!directory.exists() && !directory.mkdirs()) {
				try {
					throw new IOException("Path to file could not be created.");
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			startActivityForResult(takePictureFromCameraIntent, 1);
		}
	}

	private void launchVideoLibrary() {
		Intent videoPickerIntent = new Intent(Intent.ACTION_PICK);
		videoPickerIntent.setType("video/*");
		startActivityForResult(videoPickerIntent, 2);
	}

	private void launchVideoCamera() {
		Intent takeVideoIntent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
		startActivityForResult(takeVideoIntent, 3);
	}

	private void evaluateSaveButtonText() {

		Spinner spinner = (Spinner) findViewById(R.id.status);
		Button saveButton = (Button) findViewById(R.id.post);
		if (spinner.getSelectedItemPosition() == 0)
			saveButton.setText(getResources().getText(R.string.publish_post));
		else
			saveButton.setText(getResources().getText(R.string.save));

	}

	public LocationResult locationResult = new LocationResult() {
		@Override
		public void gotLocation(Location location) {
			if (location != null) {
				curLocation = location;
				new getAddressTask().execute(curLocation.getLatitude(),
						curLocation.getLongitude());
			} else {
				runOnUiThread(new Runnable() {
					public void run() {
						TextView locationText = (TextView) findViewById(R.id.locationText);
						locationText.setText(getResources().getText(
								R.string.location_not_found));
					}
				});
			}
		}
	};

	private void enableLBSButtons() {
		locationHelper = new LocationHelper();

		RelativeLayout section4 = (RelativeLayout) findViewById(R.id.section4);
		section4.setVisibility(View.VISIBLE);

		final Button viewMap = (Button) findViewById(R.id.viewMap);
		viewMap.setOnClickListener(new TextView.OnClickListener() {
			public void onClick(View v) {

				Double latitude = 0.0;
				try {
					latitude = curLocation.getLatitude();
				} catch (Exception e) {
					e.printStackTrace();
				}
				if (latitude != 0.0) {
					String uri = "geo:" + latitude + ","
							+ curLocation.getLongitude();
					startActivity(new Intent(
							android.content.Intent.ACTION_VIEW, Uri.parse(uri)));
				} else {
					Toast.makeText(EditPost.this,
							getResources().getText(R.string.location_toast),
							Toast.LENGTH_SHORT).show();
				}

			}
		});

		Button updateLocation = (Button) findViewById(R.id.updateLocation);

		updateLocation.setOnClickListener(new Button.OnClickListener() {
			public void onClick(View v) {

				locationHelper.getLocation(EditPost.this, locationResult);
			}
		});

		Button removeLocation = (Button) findViewById(R.id.removeLocation);

		removeLocation.setOnClickListener(new Button.OnClickListener() {
			public void onClick(View v) {

				if (curLocation != null) {
					curLocation.setLatitude(0.0);
					curLocation.setLongitude(0.0);
				}
				if (post != null) {
					post.setLatitude(0.0);
					post.setLongitude(0.0);
				}

				TextView locationText = (TextView) findViewById(R.id.locationText);
				locationText.setText("");
			}
		});

		if (isNew) {
			locationHelper.getLocation(EditPost.this, locationResult);
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (resultCode == RESULT_CANCELED) {
			if (option != null) {
				Intent intent = new Intent();
				setResult(Activity.RESULT_CANCELED, intent);
				finish();
			}
			return;
		}
		if (data != null || ((requestCode == 1 || requestCode == 3))) {
			Bundle extras;
			switch (requestCode) {
			case 0:

				Uri imageUri = data.getData();
				String imgPath = imageUri.toString();

				addMedia(imgPath, imageUri);
				break;
			case 1:
				if (resultCode == Activity.RESULT_OK) {

					File f = new File(SD_CARD_TEMP_DIR);
					try {
						Uri capturedImage = Uri
								.parse(android.provider.MediaStore.Images.Media
										.insertImage(getContentResolver(),
												f.getAbsolutePath(), null, null));

						Log.i("camera",
								"Selected image: " + capturedImage.toString());

						f.delete();

						addMedia(capturedImage.toString(), capturedImage);

					} catch (FileNotFoundException e) {
					} catch (Exception e) {
					}

				}

				break;
			case 2:

				Uri videoUri = data.getData();
				String videoPath = videoUri.toString();

				addMedia(videoPath, videoUri);

				break;
			case 3:
				if (resultCode == Activity.RESULT_OK) {
					Uri capturedVideo = data.getData();

					addMedia(capturedVideo.toString(), capturedVideo);
				}

				break;
			case 4:
				try {
					extras = data.getExtras();
					String linkURL = extras.getString("linkURL");
					if (!linkURL.equals("http://") && !linkURL.equals("")) {
						WPEditText contentText = (WPEditText) findViewById(R.id.postContent);

						if (selectionStart > selectionEnd) {
							int temp = selectionEnd;
							selectionEnd = selectionStart;
							selectionStart = temp;
						}
						Editable str = contentText.getText();
						if (localDraft) {
							if (extras.getString("linkText") == null) {
								if (selectionStart < selectionEnd)
									str.delete(selectionStart, selectionEnd);
								str.insert(selectionStart, linkURL);
								str.setSpan(new URLSpan(linkURL),
										selectionStart, selectionStart
												+ linkURL.length(),
										Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

								contentText.setSelection(selectionStart
										+ linkURL.length());
							} else {
								String linkText = extras.getString("linkText");
								if (selectionStart < selectionEnd)
									str.delete(selectionStart, selectionEnd);
								str.insert(selectionStart, linkText);
								str.setSpan(new URLSpan(linkURL),
										selectionStart, selectionStart
												+ linkText.length(),
										Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
								contentText.setSelection(selectionStart
										+ linkText.length());
							}
						} else {
							if (extras.getString("linkText") == null) {
								if (selectionStart < selectionEnd)
									str.delete(selectionStart, selectionEnd);
								String urlHTML = "<a href=\"" + linkURL + "\">"
										+ linkURL + "</a>";
								str.insert(selectionStart, urlHTML);
								contentText.setSelection(selectionStart
										+ urlHTML.length());
							} else {
								String linkText = extras.getString("linkText");
								if (selectionStart < selectionEnd)
									str.delete(selectionStart, selectionEnd);
								String urlHTML = "<a href=\"" + linkURL + "\">"
										+ linkText + "</a>";
								str.insert(selectionStart, urlHTML);
								contentText.setSelection(selectionStart
										+ urlHTML.length());
							}
						}
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
				break;
			case 5:
				extras = data.getExtras();
				String cats = extras.getString("selectedCategories");
				String[] splitCats = cats.split(",");
				categories = new JSONArray();
				for (int i = 0; i < splitCats.length; i++) {
					categories.put(splitCats[i]);
				}
				TextView selectedCategoriesTV = (TextView) findViewById(R.id.selectedCategories);
				selectedCategoriesTV.setText(getResources().getText(
						R.string.selected_categories)
						+ " " + getCategoriesCSV());

				break;
			}

		}// end null check
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		if (id == ID_DIALOG_DATE) {
			DatePickerDialog dpd = new DatePickerDialog(this, mDateSetListener,
					mYear, mMonth, mDay);
			dpd.setTitle("");
			return dpd;
		} else if (id == ID_DIALOG_TIME) {
			TimePickerDialog tpd = new TimePickerDialog(this, mTimeSetListener,
					mHour, mMinute, false);
			tpd.setTitle("");
			return tpd;
		} else if (id == ID_DIALOG_LOADING) {
			ProgressDialog loadingDialog = new ProgressDialog(this);
			loadingDialog.setMessage(getResources().getText(R.string.loading));
			loadingDialog.setIndeterminate(true);
			loadingDialog.setCancelable(true);
			return loadingDialog;
		}

		return super.onCreateDialog(id);
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {

		super.onConfigurationChanged(newConfig);
	}

	public boolean savePost(boolean autoSave) {

		// grab the form data
		EditText titleET = (EditText) findViewById(R.id.title);
		String title = titleET.getText().toString();
		WPEditText contentET = (WPEditText) findViewById(R.id.postContent);

		String content = "";

		EditText passwordET = (EditText) findViewById(R.id.post_password);
		String password = passwordET.getText().toString();
		if (localDraft || isNew && !autoSave) {
			Editable e = contentET.getText();
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
			content = contentET.getText().toString();
		}

		TextView tvPubDate = (TextView) findViewById(R.id.pubDate);
		String pubDate = tvPubDate.getText().toString();

		long pubDateTimestamp = 0;
		if (!pubDate.equals(getResources().getText(R.string.immediately))) {
			if (isCustomPubDate)
				pubDateTimestamp = customPubDate;
			else if (!isNew)
				pubDateTimestamp = post.getDate_created_gmt();
		}

		String tags = "", postFormat = "";
		if (!isPage) {
			EditText tagsET = (EditText) findViewById(R.id.tags);
			tags = tagsET.getText().toString();
			// post format
			Spinner postFormatSpinner = (Spinner) findViewById(R.id.postFormat);
			postFormat = postFormats[postFormatSpinner
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
			dialogBuilder.setPositiveButton("OK",
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog,
								int whichButton) {
							// Just close the window
						}
					});
			dialogBuilder.setCancelable(true);
			dialogBuilder.create().show();
		} else {

			if (!isNew) {
				// update the images
				post.deleteMediaFiles();
				Editable s = contentET.getText();
				WPImageSpan[] click_spans = s.getSpans(0, s.length(),
						WPImageSpan.class);

				if (click_spans.length != 0) {

					for (int i = 0; i < click_spans.length; i++) {
						WPImageSpan wpIS = click_spans[i];
						images += wpIS.getImageSource().toString() + ",";

						MediaFile mf = new MediaFile();
						mf.setPostID(post.getId());
						mf.setTitle(wpIS.getTitle());
						mf.setCaption(wpIS.getCaption());
						mf.setDescription(wpIS.getDescription());
						mf.setFeatured(wpIS.isFeatured());
						mf.setFileName(wpIS.getImageSource().toString());
						mf.setHorizontalAlignment(wpIS.getHorizontalAlignment());
						mf.setWidth(wpIS.getWidth());
						mf.save(EditPost.this);

						int tagStart = s.getSpanStart(wpIS);
						if (!autoSave) {
							s.removeSpan(wpIS);
							s.insert(tagStart, "<img android-uri=\""
								+ wpIS.getImageSource().toString() + "\" />");
							if (localDraft)
								content = EscapeUtils
									.unescapeHtml(WPHtml.toHtml(s));
							else
								content = s.toString();
						}
					}
				}
			}

			Spinner spinner = (Spinner) findViewById(R.id.status);
			int selectedStatus = spinner.getSelectedItemPosition();
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
			if (blog.isLocation()) {

				// attempt to get the device's location
				try {
					latitude = curLocation.getLatitude();
					longitude = curLocation.getLongitude();
				} catch (Exception e) {
					e.printStackTrace();
				}

			}
			String needle = "<!--more-->";
			
			if (isNew) {
				post = new Post(id, title, content, images, pubDateTimestamp,
						categories.toString(), tags, status, password,
						latitude, longitude, isPage, postFormat, EditPost.this,
						true, false);
				post.setLocalDraft(true);

				// split up the post content if there's a more tag
				if (content.indexOf(needle) >= 0) {
					post.setDescription(content.substring(0,
							content.indexOf(needle)));
					post.setMt_text_more(content.substring(
							content.indexOf(needle) + needle.length(),
							content.length()));
				}

				success = post.save();
				
				if (success) {
					isNew = false;
					isNewDraft = true;
				}
				
				post.deleteMediaFiles();

				Spannable s = contentET.getText();
				WPImageSpan[] image_spans = s.getSpans(0, s.length(),
						WPImageSpan.class);

				if (image_spans.length != 0) {

					for (int i = 0; i < image_spans.length; i++) {
						WPImageSpan wpIS = image_spans[i];
						images += wpIS.getImageSource().toString() + ",";

						MediaFile mf = new MediaFile();
						mf.setPostID(post.getId());
						mf.setTitle(wpIS.getTitle());
						mf.setCaption(wpIS.getCaption());
						// mf.setDescription(wpIS.getDescription());
						// mf.setFeatured(wpIS.isFeatured());
						mf.setFileName(wpIS.getImageSource().toString());
						mf.setFilePath(wpIS.getImageSource().toString());
						mf.setHorizontalAlignment(wpIS.getHorizontalAlignment());
						mf.setWidth(wpIS.getWidth());
						mf.setVideo(wpIS.isVideo());
						mf.save(EditPost.this);
					}
				}

				WordPress.currentPost = post;

			} else {

				if (curLocation == null) {
					latitude = post.getLatitude();
					longitude = post.getLongitude();
				}

				post.setTitle(title);
				// split up the post content if there's a more tag
				if (localDraft && content.indexOf(needle) >= 0) {
					post.setDescription(content.substring(0,
							content.indexOf(needle)));
					post.setMt_text_more(content.substring(
							content.indexOf(needle) + needle.length(),
							content.length()));
				} else {
					post.setDescription(content);
					post.setMt_text_more("");
				}
				post.setMediaPaths(images);
				post.setDate_created_gmt(pubDateTimestamp);
				post.setCategories(categories);
				post.setMt_keywords(tags);
				post.setPost_status(status);
				post.setWP_password(password);
				post.setLatitude(latitude);
				post.setLongitude(longitude);
				post.setWP_post_form(postFormat);
				if (!post.isLocalDraft())
					post.setLocalChange(true);
				success = post.update();
			}

		}

		return success;
	}

	@Override
	public void onBackPressed() {
		if (!isFullScreenEditing && !imeBackPressed) {
			AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(
					EditPost.this);
			dialogBuilder
					.setTitle(getResources().getText(R.string.cancel_edit));
			dialogBuilder.setMessage(getResources().getText(
					(isPage) ? R.string.sure_to_cancel_edit_page
							: R.string.sure_to_cancel_edit));
			dialogBuilder.setPositiveButton(getResources()
					.getText(R.string.yes),
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog,
								int whichButton) {
							if (isNewDraft)
								post.delete();
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
							// just close the dialog window
						}
					});
			dialogBuilder.setCancelable(true);
			dialogBuilder.create().show();
		} else {
			finishEditing();
		}

		if (imeBackPressed)
			imeBackPressed = false;

		return;
	}

	/** Register for the updates when Activity is in foreground */
	@Override
	protected void onResume() {
		super.onResume();
		autoSaveHandler.postDelayed(autoSaveRunnable, 60000);
	}

	/** Stop the updates when Activity is paused */
	@Override
	protected void onPause() {
		super.onPause();
		if (locationHelper != null) {
			locationHelper.cancelTimer();
		}
		autoSaveHandler.removeCallbacks(autoSaveRunnable);
		}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		if (locationHelper != null) {
			locationHelper.cancelTimer();
		}
	}

	private class getAddressTask extends AsyncTask<Double, Void, String> {

		protected void onPostExecute(String result) {
			TextView map = (TextView) findViewById(R.id.locationText);
			map.setText(result);
		}

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
						finalText = getResources().getText(
								R.string.location_not_found).toString();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
			return finalText;
		}
	}

	protected void setContent() {
		Intent intent = getIntent();
		String text = intent.getStringExtra(Intent.EXTRA_TEXT);
		String title = intent.getStringExtra(Intent.EXTRA_SUBJECT);
		if (text != null) {
			EditText titleET = (EditText) findViewById(R.id.title);

			if (title != null) {
				titleET.setText(title);
			}

			WPEditText contentET = (WPEditText) findViewById(R.id.postContent);
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
				contentET.setText(text);
			} else {
				// add link tag around URLs, trac #64
				text = text.replaceAll("((http|https|ftp|mailto):\\S+)",
						"<a href=\"$1\">$1</a>");
				contentET.setText(WPHtml.fromHtml(StringHelper.addPTags(text),
						EditPost.this, post));
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

		protected void onPostExecute(SpannableStringBuilder result) {
			dismissDialog(ID_DIALOG_LOADING);
			if (result != null) {
				if (result.length() > 0) {
					WPEditText postContent = (WPEditText) findViewById(R.id.postContent);
					postContent.setText(result);
				}
			}
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

		WPEditText content = (WPEditText) findViewById(R.id.postContent);
		int selectionStart = content.getSelectionStart();

		styleStart = selectionStart;

		int selectionEnd = content.getSelectionEnd();

		if (selectionStart > selectionEnd) {
			int temp = selectionEnd;
			selectionEnd = selectionStart;
			selectionStart = temp;
		}

		if (content.getText().length() == 0) {
			content.setText(" ");
		}

		Editable s = content.getText();

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

		// insert a few line breaks if the cursor is already on an image
		WPImageSpan[] click_spans = s.getSpans(selectionStart, selectionEnd,
				WPImageSpan.class);
		if (click_spans.length != 0) {
			s.insert(selectionEnd, "\n\n");
			selectionStart = selectionStart + 2;
			selectionEnd = selectionEnd + 2;
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
			content.setSelection(s.length());
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	public SpannableStringBuilder addMediaFromShareAction(String imgPath,
			Uri curStream, SpannableStringBuilder ssb) {
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
		if (categories.length() > 0) {
			for (int i = 0; i < categories.length(); i++) {
				try {
					csv += EscapeUtils.unescapeHtml(categories.getString(i))
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
				customPubDate = timestamp;
				TextView tvPubDate = (TextView) findViewById(R.id.pubDate);
				tvPubDate.setText(formattedDate);
				isCustomPubDate = true;
			} catch (Exception e) {
				e.printStackTrace();
			}

		}

	};
	
	/*AUTOSAVE*/
	private Runnable autoSaveRunnable = new Runnable() {
		@Override
		public void run() {
			savePost(true);
			autoSaveHandler.postDelayed(this, 60000);
		}
	};

}
