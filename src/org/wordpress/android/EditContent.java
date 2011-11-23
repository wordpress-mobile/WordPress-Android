package org.wordpress.android;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;

import org.wordpress.android.util.ImageHelper;
import org.wordpress.android.util.WPEditText;
import org.wordpress.android.util.WPHtml;
import org.wordpress.android.util.WPImageSpan;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.Layout;
import android.text.Selection;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextWatcher;
import android.text.method.ArrowKeyMovementMethod;
import android.text.style.AlignmentSpan;
import android.text.style.BulletSpan;
import android.text.style.QuoteSpan;
import android.text.style.StrikethroughSpan;
import android.text.style.StyleSpan;
import android.text.style.URLSpan;
import android.text.style.UnderlineSpan;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Display;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.ToggleButton;

public class EditContent extends Activity {
	/** Called when the activity is first created. */
	int styleStart, cursorLoc, selectionStart, selectionEnd;
	String SD_CARD_TEMP_DIR = "";
	boolean localDraft = true, isBackspace = false;
	boolean isNew = true;
	public EditText contentET;
	private String option;

	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);

		setContentView(R.layout.edit_content);

		final ImageButton addPictureButton = (ImageButton) findViewById(R.id.addPictureButton);

		registerForContextMenu(addPictureButton);

		addPictureButton.setOnClickListener(new Button.OnClickListener() {
			public void onClick(View v) {

				addPictureButton.performLongClick();

			}
		});

		Bundle extras = getIntent().getExtras();

		if (extras != null) {
			option = extras.getString("option");
			if (option != null) {
				if (option.equals("newphoto")) {
					launchCamera();
				} else if (option.equals("photolibrary")) {
					launchPictureLibrary();
				} else if (option.equals("newvideo")) {
					launchVideoCamera();
				} else if (option.equals("videolibrary")) {
					launchVideoLibrary();
				}
			}
		}

		final WPEditText contentEditor = (WPEditText) findViewById(R.id.postContent);
		Spannable contentText = WordPress.richPostContent;
		if (contentText != null) {
			contentEditor.setText(contentText);
		}
		
		contentEditor.setOnSelectionChangedListener(new WPEditText.OnSelectionChangedListener() {

			@Override
			public void onSelectionChanged() {
				final Spannable s = contentEditor.getText();
				//set toggle buttons if cursor is inside of a matching span
				Object[] spans = s.getSpans(
						contentEditor.getSelectionStart(),
						contentEditor.getSelectionStart(), Object.class);
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
					if (span instanceof UnderlineSpan) {
						underlineButton.setChecked(true);
					}
					if (span instanceof StrikethroughSpan) {
						strikeButton.setChecked(true);
					}
				}	
			}
		});
		
		
		contentEditor
				.setOnFocusChangeListener(new View.OnFocusChangeListener() {
					@Override
					public void onFocusChange(View view, boolean hasFocus) {
						RelativeLayout formatBar = (RelativeLayout) findViewById(R.id.formatBar);
						if (hasFocus) {
							formatBar.setVisibility(View.VISIBLE);
							Animation fadeInAnimation = AnimationUtils
									.loadAnimation(EditContent.this,
											R.anim.show);
							formatBar.startAnimation(fadeInAnimation);
						} else {
							Animation fadeOutAnimation = AnimationUtils
									.loadAnimation(EditContent.this,
											R.anim.disappear);
							formatBar.startAnimation(fadeOutAnimation);
							formatBar.setVisibility(View.GONE);
						}
					}
				});

		contentEditor
				.setOnEditTextImeBackListener(new WPEditText.EditTextImeBackListener() {

					@Override
					public void onImeBack(WPEditText view, String text) {
						/*
						 * RelativeLayout formatBar = (RelativeLayout)
						 * findViewById(R.id.formatBar); Animation
						 * fadeOutAnimation = AnimationUtils
						 * .loadAnimation(EditContent.this, R.anim.disappear);
						 * formatBar.startAnimation(fadeOutAnimation);
						 * formatBar.setVisibility(View.GONE);
						 */
						finishEditing();
					}

				});

		contentEditor.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				RelativeLayout formatBar = (RelativeLayout) findViewById(R.id.formatBar);
				if (!(formatBar.getVisibility() == View.VISIBLE)) {

					formatBar.setVisibility(View.VISIBLE);
					Animation fadeInAnimation = AnimationUtils.loadAnimation(
							EditContent.this, R.anim.show);
					formatBar.startAnimation(fadeInAnimation);

				}

				
				final Spannable s = contentEditor.getText();
				// check if image span was tapped
				WPImageSpan[] click_spans = s.getSpans(
						contentEditor.getSelectionStart(),
						contentEditor.getSelectionStart(), WPImageSpan.class);

				if (click_spans.length != 0) {
					final WPImageSpan span = click_spans[0];
					if (!span.isVideo()) {
						LayoutInflater factory = LayoutInflater
								.from(EditContent.this);
						final View alertView = factory.inflate(
								R.layout.alert_image_options, null);

						final TextView imageWidthText = (TextView) alertView
								.findViewById(R.id.imageWidthText);
						final EditText titleText = (EditText) alertView
								.findViewById(R.id.title);
						// final EditText descText = (EditText) alertView
						// .findViewById(R.id.description);
						final EditText caption = (EditText) alertView
								.findViewById(R.id.caption);
						// final CheckBox featured = (CheckBox) alertView
						// .findViewById(R.id.featuredImage);
						final SeekBar seekBar = (SeekBar) alertView
								.findViewById(R.id.imageWidth);
						final Spinner alignmentSpinner = (Spinner) alertView
								.findViewById(R.id.alignment_spinner);
						ArrayAdapter<CharSequence> adapter = ArrayAdapter
								.createFromResource(EditContent.this,
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
						// featured.setChecked(span.isFeatured());

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

						AlertDialog ad = new AlertDialog.Builder(
								EditContent.this)
								.setTitle("Image Settings")
								.setView(alertView)
								.setPositiveButton("OK",
										new DialogInterface.OnClickListener() {
											public void onClick(
													DialogInterface dialog,
													int whichButton) {

												span.setTitle(titleText
														.getText().toString());
												// span.setDescription(descText
												// .getText().toString());

												span.setHorizontalAlignment(alignmentSpinner
														.getSelectedItemPosition());
												span.setWidth(seekBar
														.getProgress() * 10);
												span.setCaption(caption
														.getText().toString());
												// span.setFeatured(featured
												// .isChecked());

											}
										})
								.setNegativeButton("Cancel",
										new DialogInterface.OnClickListener() {
											public void onClick(
													DialogInterface dialog,
													int whichButton) {

											}
										}).create();
						ad.show();
					}

				} else {
					contentEditor.setMovementMethod(ArrowKeyMovementMethod
							.getInstance());
					contentEditor.setSelection(contentEditor
							.getSelectionStart());
				}

			}
		});

		final WPEditText contentEdit = (WPEditText) findViewById(R.id.postContent);
		contentEdit.addTextChangedListener(new TextWatcher() {
			public void afterTextChanged(Editable s) {
				
				if (isBackspace)
					return;
				
				// add style as the user types if a toggle button is enabled
				ToggleButton boldButton = (ToggleButton) findViewById(R.id.bold);
				ToggleButton emButton = (ToggleButton) findViewById(R.id.em);
				ToggleButton bquoteButton = (ToggleButton) findViewById(R.id.bquote);
				ToggleButton underlineButton = (ToggleButton) findViewById(R.id.underline);
				ToggleButton strikeButton = (ToggleButton) findViewById(R.id.strike);
				int position = Selection.getSelectionStart(contentEdit
						.getText());
				if (position < 0) {
					position = 0;
				}

				if (position > 0) {

					/*if (styleStart > position || position > (cursorLoc + 1)) {
						// user changed cursor location, reset
						if (position - cursorLoc > 1) {
							// user pasted text
							styleStart = cursorLoc;
						} else {
							styleStart = position - 1;
						}
					}*/

					if (boldButton.isChecked()) {
						StyleSpan[] ss = s.getSpans(styleStart, position,
								StyleSpan.class);

						for (int i = 0; i < ss.length; i++) {
							if (ss[i].getStyle() == android.graphics.Typeface.BOLD) {
								s.removeSpan(ss[i]);
							}
						}
						s.setSpan(
								new StyleSpan(android.graphics.Typeface.BOLD),
								styleStart, position,
								Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
					}
					if (emButton.isChecked()) {
						StyleSpan[] ss = s.getSpans(styleStart, position,
								StyleSpan.class);

						for (int i = 0; i < ss.length; i++) {
							if (ss[i].getStyle() == android.graphics.Typeface.ITALIC) {
								s.removeSpan(ss[i]);
							}
						}
						s.setSpan(new StyleSpan(
								android.graphics.Typeface.ITALIC), styleStart,
								position, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
					}
					if (bquoteButton.isChecked()) {

						QuoteSpan[] ss = s.getSpans(styleStart, position,
								QuoteSpan.class);

						for (int i = 0; i < ss.length; i++) {
							s.removeSpan(ss[i]);
						}
						s.setSpan(new QuoteSpan(), styleStart, position,
								Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
					}
					if (underlineButton.isChecked()) {
						UnderlineSpan[] ss = s.getSpans(styleStart, position,
								UnderlineSpan.class);

						for (int i = 0; i < ss.length; i++) {
							s.removeSpan(ss[i]);
						}
						s.setSpan(new UnderlineSpan(), styleStart, position,
								Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
					}
					if (strikeButton.isChecked()) {
						StrikethroughSpan[] ss = s.getSpans(styleStart,
								position, StrikethroughSpan.class);

						for (int i = 0; i < ss.length; i++) {
							s.removeSpan(ss[i]);
						}
						s.setSpan(new StrikethroughSpan(), styleStart,
								position, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
					}
				}

				cursorLoc = Selection.getSelectionStart(contentEdit.getText());
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

				Intent i = new Intent(EditContent.this, Link.class);
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

				SpannableStringBuilder ssb = new SpannableStringBuilder();
				ssb.append(contentText.getText().subSequence(0, selectionEnd));

				Spannable more = (Spannable) WPHtml
						.fromHtml(
								"<br><div style=\"display:block;\" id=\"wp-android-more\"><font color=\"#777777\">........"
										+ getResources().getText(
												R.string.more_tag)
										+ "</font></div>", EditContent.this,
								WordPress.currentPost);
				ssb.append(more);
				ssb.append(contentText.getText().subSequence(selectionEnd,
						contentText.getText().length()));

				contentText.setText(ssb);
				contentText.setSelection(selectionEnd + more.length());

			}
		});
	}

	protected void formatBtnClick(ToggleButton toggleButton, String tag) {
		WPEditText contentText = (WPEditText) findViewById(R.id.postContent);

		int selectionStart = contentText.getSelectionStart();

		styleStart = selectionStart;

		int selectionEnd = contentText.getSelectionEnd();

		if (selectionStart > selectionEnd) {
			int temp = selectionEnd;
			selectionEnd = selectionStart;
			selectionStart = temp;
		}

		if (selectionEnd > selectionStart) {
			Spannable str = contentText.getText();
			if (tag.equals("blockquote")) {

				QuoteSpan[] ss = str.getSpans(selectionStart, selectionEnd,
						QuoteSpan.class);

				boolean exists = false;
				for (int i = 0; i < ss.length; i++) {
					str.removeSpan(ss[i]);
					exists = true;
				}

				if (!exists) {
					str.setSpan(new QuoteSpan(), selectionStart, selectionEnd,
							Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
				}

				toggleButton.setChecked(false);
			} else if (tag.equals("strong")) {
				StyleSpan[] ss = str.getSpans(selectionStart, selectionEnd,
						StyleSpan.class);

				boolean exists = false;
				for (int i = 0; i < ss.length; i++) {
					int style = ((StyleSpan) ss[i]).getStyle();
					if (style == android.graphics.Typeface.BOLD) {
						str.removeSpan(ss[i]);
						exists = true;
					}
				}

				if (!exists) {
					str.setSpan(new StyleSpan(android.graphics.Typeface.BOLD),
							selectionStart, selectionEnd,
							Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
				}
				toggleButton.setChecked(false);
			} else if (tag.equals("em")) {
				StyleSpan[] ss = str.getSpans(selectionStart, selectionEnd,
						StyleSpan.class);

				boolean exists = false;
				for (int i = 0; i < ss.length; i++) {
					int style = ((StyleSpan) ss[i]).getStyle();
					if (style == android.graphics.Typeface.ITALIC) {
						str.removeSpan(ss[i]);
						exists = true;
					}
				}

				if (!exists) {
					str.setSpan(
							new StyleSpan(android.graphics.Typeface.ITALIC),
							selectionStart, selectionEnd,
							Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
				}
				toggleButton.setChecked(false);
			} else if (tag.equals("u")) {

				UnderlineSpan[] ss = str.getSpans(selectionStart, selectionEnd,
						UnderlineSpan.class);

				boolean exists = false;
				for (int i = 0; i < ss.length; i++) {
					str.removeSpan(ss[i]);
					exists = true;
				}

				if (!exists) {
					str.setSpan(new UnderlineSpan(), selectionStart,
							selectionEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
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
					str.setSpan(new StrikethroughSpan(), selectionStart,
							selectionEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
				}

				toggleButton.setChecked(false);
			} else if (tag.equals("ul")) {

				BulletSpan[] ss = str.getSpans(selectionStart, selectionEnd,
						BulletSpan.class);

				boolean exists = false;
				for (int i = 0; i < ss.length; i++) {
					str.removeSpan(ss[i]);
					exists = true;
				}

				if (!exists) {
					str.setSpan(new BulletSpan(), selectionStart, selectionEnd,
							Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
				}

				toggleButton.setChecked(false);
			}
		}

	}

	private void addMedia(String imgPath, Uri curStream) {

		Bitmap resizedBitmap = null;
		ImageHelper ih = new ImageHelper();
		Display display = getWindowManager().getDefaultDisplay();
		int width = display.getWidth();

		HashMap<String, Object> mediaData = ih.getImageBytesForPath(imgPath,
				EditContent.this);

		if (mediaData == null) {
			return;
		}

		byte[] finalBytes = ih.createThumbnail((byte[]) mediaData.get("bytes"),
				String.valueOf(width / 2),
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

		CharSequence beforeText = content.getText().subSequence(0,
				selectionStart);
		CharSequence afterText = content.getText().subSequence(selectionStart,
				selectionEnd);

		SpannableStringBuilder builder = new SpannableStringBuilder();
		builder.append(beforeText);
		builder.append(" ");
		builder.append(afterText);
		WPImageSpan is = new WPImageSpan(EditContent.this, resizedBitmap,
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
		builder.setSpan(is, selectionStart, selectionEnd + 1,
				Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		AlignmentSpan.Standard as = new AlignmentSpan.Standard(
				Layout.Alignment.ALIGN_CENTER);
		builder.setSpan(as, selectionStart, selectionEnd + 1,
				Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		builder.append("\n\n");
		content.setText(builder);
		content.setSelection(content.getText().length());

	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (data != null
				&& (resultCode != RESULT_CANCELED || (requestCode == 1 || requestCode == 3))) {
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

					}

				} else {
					// user canceled capture
					if (option != null) {
						Intent intent = new Intent();
						intent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
						setResult(Activity.RESULT_CANCELED, intent);
						finish();
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
				} else {
					// user canceled capture
					if (option != null) {
						Intent intent = new Intent();
						intent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
						setResult(Activity.RESULT_CANCELED, intent);
						finish();
					}
				}

				break;
			case 4:
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
					if (extras.getString("linkText") == null) {
						if (selectionStart < selectionEnd)
							str.delete(selectionStart, selectionEnd);
						str.insert(selectionStart, linkURL);
						str.setSpan(new URLSpan(linkURL), selectionStart,
								selectionStart + linkURL.length(),
								Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

						contentText.setSelection(selectionStart
								+ linkURL.length());
					} else {
						String linkText = extras.getString("linkText");
						if (selectionStart < selectionEnd)
							str.delete(selectionStart, selectionEnd);
						str.insert(selectionStart, linkText);
						str.setSpan(new URLSpan(linkURL), selectionStart,
								selectionStart + linkText.length(),
								Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
						contentText.setSelection(selectionStart
								+ linkText.length());
					}
				}
				break;
			}

		}// end null check
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {

		super.onConfigurationChanged(newConfig);
	}

	@Override
	public boolean onKeyDown(int i, KeyEvent event) {

		// only intercept back button press
		if (i == KeyEvent.KEYCODE_BACK) {
			finishEditing();
		}

		return false;
	}

	private void finishEditing() {
		WPEditText contentET = (WPEditText) findViewById(R.id.postContent);
		Spannable content = contentET.getText();
		WordPress.richPostContent = content;
		Intent intent = new Intent();
		intent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
		setResult(RESULT_OK, intent);
		finish();
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
					EditContent.this);
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

	/** Register for the updates when Activity is in foreground */
	@Override
	protected void onResume() {
		super.onResume();

	}
}
