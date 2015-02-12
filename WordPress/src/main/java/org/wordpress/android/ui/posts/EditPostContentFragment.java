package org.wordpress.android.ui.posts;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.text.Editable;
import android.text.Layout;
import android.text.Selection;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.method.ArrowKeyMovementMethod;
import android.text.style.AlignmentSpan;
import android.text.style.ImageSpan;
import android.text.style.QuoteSpan;
import android.text.style.StrikethroughSpan;
import android.text.style.StyleSpan;
import android.text.style.URLSpan;
import android.view.ContextMenu;
import android.view.KeyEvent;
import android.view.LayoutInflater;
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
import android.widget.ToggleButton;

import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageLoader;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.analytics.AnalyticsTracker;
import org.wordpress.android.analytics.AnalyticsTracker.Stat;
import org.wordpress.android.editor.EditorFragmentAbstract;
import org.wordpress.android.editor.legacy.WPEditImageSpan;
import org.wordpress.android.ui.media.MediaUtils;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.CrashlyticsUtils;
import org.wordpress.android.util.CrashlyticsUtils.ExceptionType;
import org.wordpress.android.util.CrashlyticsUtils.ExtraKey;
import org.wordpress.android.util.DeviceUtils;
import org.wordpress.android.util.DisplayUtils;
import org.wordpress.android.util.ImageUtils;
import org.wordpress.android.util.helpers.MediaFile;
import org.wordpress.android.util.helpers.WPImageSpan;
import org.wordpress.android.util.widgets.WPEditText;
import org.wordpress.android.widgets.MediaGalleryImageSpan;
import org.wordpress.android.widgets.WPUnderlineSpan;

public class EditPostContentFragment extends EditorFragmentAbstract implements TextWatcher,
        WPEditText.OnSelectionChangedListener, View.OnTouchListener {
    private static final int MIN_THUMBNAIL_WIDTH = 200;
    private static final int CONTENT_ANIMATION_DURATION = 250;
    private static final String TAG_FORMAT_BAR_BUTTON_STRONG = "strong";
    private static final String TAG_FORMAT_BAR_BUTTON_EM = "em";
    private static final String TAG_FORMAT_BAR_BUTTON_UNDERLINE = "u";
    private static final String TAG_FORMAT_BAR_BUTTON_STRIKE = "strike";
    private static final String TAG_FORMAT_BAR_BUTTON_QUOTE = "blockquote";

    private ActionBarActivity mActivity;
    private View mRootView;
    private WPEditText mContentEditText;
    private Button mAddPictureButton;
    private EditText mTitleEditText;
    private ToggleButton mBoldToggleButton, mEmToggleButton, mBquoteToggleButton;
    private ToggleButton mUnderlineToggleButton, mStrikeToggleButton;
    private LinearLayout mFormatBar, mPostContentLinearLayout, mPostSettingsLinearLayout;
    private boolean mIsBackspace;
    private boolean mScrollDetected;
    private boolean mIsLocalDraft;

    private int mStyleStart, mSelectionStart, mSelectionEnd, mFullViewBottom;
    private int mLastPosition = -1;
    private CharSequence mTitle;
    private CharSequence mContent;

    private float mLastYPos = 0;

    @Override
    public boolean onBackPressed() {
        // leave full screen mode back button is pressed
        if (getActivity().getActionBar() != null && !getActivity().getActionBar().isShowing()) {
            setContentEditingModeVisible(false);
            return true;
        }
        return false;
    }

    @Override
    public CharSequence getTitle() {
        if (mTitleEditText != null) {
            return mTitleEditText.getText().toString();
        }
        return mTitle;
    }

    @Override
    public CharSequence getContent() {
        if (mContentEditText != null) {
            return mContentEditText.getText().toString();
        }
        return mContent;
    }

    @Override
    public void setTitle(CharSequence text) {
        mTitle = text;
        if (mTitleEditText != null) {
            mTitleEditText.setText(text);
        } else {
            // TODO
        }
    }

    @Override
    public void setContent(CharSequence text) {
        mContent = text;
        if (mContentEditText != null) {
            mContentEditText.setText(text);
        } else {
            // TODO
        }
    }

    public void setLocalDraft(boolean isLocalDraft) {
        mIsLocalDraft = isLocalDraft;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mActivity = (ActionBarActivity) getActivity();

        final ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.fragment_edit_post_content, container, false);

        mFormatBar = (LinearLayout) rootView.findViewById(R.id.format_bar);
        mTitleEditText = (EditText) rootView.findViewById(R.id.post_title);
        mTitleEditText.setText(mTitle);
        mTitleEditText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                // Go to full screen editor when 'next' button is tapped on soft keyboard
                if (actionId == EditorInfo.IME_ACTION_NEXT && isAdded() && mActivity.getSupportActionBar() != null &&
                        mActivity.getSupportActionBar().isShowing()) {
                    setContentEditingModeVisible(true);
                }
                return false;
            }
        });
        mContentEditText = (WPEditText) rootView.findViewById(R.id.post_content);
        mContentEditText.setText(mContent);
        mPostContentLinearLayout = (LinearLayout) rootView.findViewById(R.id.post_content_wrapper);
        mPostSettingsLinearLayout = (LinearLayout) rootView.findViewById(R.id.post_settings_wrapper);
        Button postSettingsButton = (Button) rootView.findViewById(R.id.post_settings_button);
        postSettingsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mEditorFragmentListener.onSettingsClicked();
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
                if (mRootView.getBottom() < mFullViewBottom && isAdded() && mActivity.getSupportActionBar() != null
                        && !mActivity.getSupportActionBar().isShowing()) {
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
        mEditorFragmentListener.onEditorFragmentInitialized();
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
        if (!isAdded()) {
            return;
        }
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
            if (actionBar != null) {
                actionBar.hide();
            }
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
            mActivity.invalidateOptionsMenu();
            if (actionBar != null) {
                actionBar.show();
            }
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



    // TODO: call MediaPicker-Android instead (see EditPostActivity.onAddMediaButtonClicked)
    /*
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
    */

    public boolean hasEmptyContentFields() {
        return TextUtils.isEmpty(mTitleEditText.getText()) && TextUtils.isEmpty(mContentEditText.getText());
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mFullViewBottom = mRootView.getBottom();
    }

    public void createLinkFromSelection(String linkURL, String linkText) {
        try {
            if (linkURL != null && !linkURL.equals("http://") && !linkURL.equals("")) {
                if (mSelectionStart > mSelectionEnd) {
                    int temp = mSelectionEnd;
                    mSelectionEnd = mSelectionStart;
                    mSelectionStart = temp;
                }
                Editable str = mContentEditText.getText();
                if (str == null)
                    return;
                if (mIsLocalDraft) {
                    if (linkText == null) {
                        if (mSelectionStart < mSelectionEnd) {
                            str.delete(mSelectionStart, mSelectionEnd);
                        }
                        str.insert(mSelectionStart, linkURL);
                        str.setSpan(new URLSpan(linkURL), mSelectionStart, mSelectionStart + linkURL.length(),
                                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                        mContentEditText.setSelection(mSelectionStart + linkURL.length());
                    } else {
                        if (mSelectionStart < mSelectionEnd) {
                            str.delete(mSelectionStart, mSelectionEnd);
                        }
                        str.insert(mSelectionStart, linkText);
                        str.setSpan(new URLSpan(linkURL), mSelectionStart, mSelectionStart + linkText.length(),
                                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                        mContentEditText.setSelection(mSelectionStart + linkText.length());
                    }
                } else {
                    if (linkText == null) {
                        if (mSelectionStart < mSelectionEnd)
                            str.delete(mSelectionStart, mSelectionEnd);
                        String urlHTML = "<a href=\"" + linkURL + "\">" + linkURL + "</a>";
                        str.insert(mSelectionStart, urlHTML);
                        mContentEditText.setSelection(mSelectionStart + urlHTML.length());
                    } else {
                        if (mSelectionStart < mSelectionEnd) {
                            str.delete(mSelectionStart, mSelectionEnd);
                        }
                        String urlHTML = "<a href=\"" + linkURL + "\">" + linkText + "</a>";
                        str.insert(mSelectionStart, urlHTML);
                        mContentEditText.setSelection(mSelectionStart + urlHTML.length());
                    }
                }
            }
        } catch (RuntimeException e) {
            AppLog.e(T.POSTS, e);
        }
    }

    /**
     * Formatting bar
     */
    private View.OnClickListener mFormatBarButtonClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            int id = v.getId();
            if (id == R.id.bold) {
                AnalyticsTracker.track(Stat.EDITOR_TAPPED_BOLD);
                onFormatButtonClick(mBoldToggleButton, TAG_FORMAT_BAR_BUTTON_STRONG);
            } else if (id == R.id.em) {
                AnalyticsTracker.track(Stat.EDITOR_TAPPED_ITALIC);
                onFormatButtonClick(mEmToggleButton, TAG_FORMAT_BAR_BUTTON_EM);
            } else if (id == R.id.underline) {
                AnalyticsTracker.track(Stat.EDITOR_TAPPED_UNDERLINE);
                onFormatButtonClick(mUnderlineToggleButton, TAG_FORMAT_BAR_BUTTON_UNDERLINE);
            } else if (id == R.id.strike) {
                AnalyticsTracker.track(Stat.EDITOR_TAPPED_STRIKETHROUGH);
                onFormatButtonClick(mStrikeToggleButton, TAG_FORMAT_BAR_BUTTON_STRIKE);
            } else if (id == R.id.bquote) {
                AnalyticsTracker.track(Stat.EDITOR_TAPPED_BLOCKQUOTE);
                onFormatButtonClick(mBquoteToggleButton, TAG_FORMAT_BAR_BUTTON_QUOTE);
            } else if (id == R.id.more) {
                AnalyticsTracker.track(Stat.EDITOR_TAPPED_MORE);
                mSelectionEnd = mContentEditText.getSelectionEnd();
                Editable str = mContentEditText.getText();
                if (str != null) {
                    if (mSelectionEnd > str.length())
                        mSelectionEnd = str.length();
                    str.insert(mSelectionEnd, "\n<!--more-->\n");
                }
            } else if (id == R.id.link) {
                AnalyticsTracker.track(Stat.EDITOR_TAPPED_LINK);
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
                startActivityForResult(i, EditPostActivity.ACTIVITY_REQUEST_CODE_CREATE_LINK);
            } else if (id == R.id.addPictureButton) {
                AnalyticsTracker.track(Stat.EDITOR_TAPPED_IMAGE);
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
        if (mIsLocalDraft) {
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
                        AppLog.e(T.POSTS, e);
                    } catch (IllegalAccessException e) {
                        AppLog.e(T.POSTS, e);
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
            if (isAdded() && mActivity.getSupportActionBar() != null && mActivity.getSupportActionBar().isShowing()) {
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
                        // TODO: we should move that to EditPostActivity
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
                                                        // TODO: we should move that to EditPostActivity.savePost()
                                                        WordPress.wpDB.saveMediaFile(postMediaFile);
                                                    }
                                                }
                                            }
                                        }
                                        mediaFile.setFeaturedInPost(featuredInPostCheckBox.isChecked());
                                        // TODO: we should move that to EditPostActivity.savePost()
                                        WordPress.wpDB.saveMediaFile(mediaFile);
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
                    // TODO: send action to create media gallery
                    // startMediaGalleryActivity(gallerySpan.getMediaGallery());
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
        if ((mIsBackspace && position != 1) || mLastPosition == position || !mIsLocalDraft)
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
        if (!mIsLocalDraft) {
            return;
        }

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
            AppLog.e(T.POSTS, e);
        }
        width = Math.min(max, Math.max(width, min));
        return width;
    }

    private void loadWPImageSpanThumbnail(MediaFile mediaFile, String imageURL, ImageLoader imageLoader) {
        if (mediaFile == null || imageURL == null) {
            return;
        }
        final String mediaId = mediaFile.getMediaId();
        if (mediaId == null) {
            return;
        }

        final int maxThumbWidth = ImageUtils.getMaximumThumbnailWidthForEditor(getActivity());

        imageLoader.get(imageURL, new ImageLoader.ImageListener() {
            @Override
            public void onErrorResponse(VolleyError arg0) {
            }

            @Override
            public void onResponse(ImageLoader.ImageContainer container, boolean arg1) {
                Bitmap downloadedBitmap = container.getBitmap();
                if (downloadedBitmap == null) {
                    // no bitmap downloaded from the server.
                    return;
                }

                if (downloadedBitmap.getWidth() < MIN_THUMBNAIL_WIDTH) {
                    // Picture is too small. Show the placeholder in this case.
                    return;
                }

                Bitmap resizedBitmap;
                // resize the downloaded bitmap
                try {
                    resizedBitmap = ImageUtils.getScaledBitmapAtLongestSide(downloadedBitmap, maxThumbWidth);
                } catch (OutOfMemoryError er) {
                    CrashlyticsUtils.setInt(ExtraKey.IMAGE_WIDTH, downloadedBitmap.getWidth());
                    CrashlyticsUtils.setInt(ExtraKey.IMAGE_HEIGHT, downloadedBitmap.getHeight());
                    CrashlyticsUtils.setFloat(ExtraKey.IMAGE_RESIZE_SCALE,
                            ((float) maxThumbWidth) / downloadedBitmap.getWidth());
                    CrashlyticsUtils.logException(er, ExceptionType.SPECIFIC, T.POSTS);
                    return;
                }

                if (resizedBitmap == null) {
                    return;
                }

                final EditText editText = mContentEditText;
                Editable s = editText.getText();
                if (s == null) {
                    return;
                }
                WPImageSpan[] spans = s.getSpans(0, s.length(), WPImageSpan.class);
                if (spans.length != 0) {
                    for (WPImageSpan is : spans) {
                        MediaFile mediaFile = is.getMediaFile();
                        if (mediaFile == null) {
                            continue;
                        }
                        if (mediaId.equals(mediaFile.getMediaId()) && !is.isNetworkImageLoaded()) {
                            // replace the existing span with a new one with the correct image, re-add
                            // it to the same position.
                            int spanStart = s.getSpanStart(is);
                            int spanEnd = s.getSpanEnd(is);
                            WPEditImageSpan imageSpan = new WPEditImageSpan(getActivity(), resizedBitmap,
                                    is.getImageSource());
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

    @Override
    public void appendMediaFile(MediaFile mediaFile, String imageUrl, ImageLoader imageLoader) {
        WPEditImageSpan imageSpan = MediaUtils.createWPEditImageSpan(getActivity(), mediaFile);

        // Insert the WPImageSpan in the content field
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
        if (s == null) {
            return;
        }

        WPImageSpan[] imageSpans = s.getSpans(selectionStart, selectionEnd, WPImageSpan.class);
        if (imageSpans.length != 0) {
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

        // Fetch and replace the WPImageSpan if it's a remote media
        if (mediaFile.getFileURL() != null) {
            loadWPImageSpanThumbnail(mediaFile, imageUrl, imageLoader);
        }
    }
}
