package org.wordpress.android.widgets;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.v7.content.res.AppCompatResources;
import android.support.v7.widget.AppCompatTextView;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.util.AttributeSet;

import org.wordpress.android.R;

/**
 * Custom TextView - with an option to auto fix widow words.
 */
public class WPTextView extends AppCompatTextView {
    protected boolean mFixWidowWordEnabled;

    public WPTextView(Context context) {
        super(context, null);
    }

    public WPTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        readCustomAttrs(context, attrs);
    }

    public WPTextView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        readCustomAttrs(context, attrs);
    }

    public void setFixWidowWord(boolean enabled) {
        mFixWidowWordEnabled = enabled;
    }

    @Override
    public void setText(CharSequence text, BufferType type) {
        if (!mFixWidowWordEnabled) {
            super.setText(text, type);
            return;
        }
        Spannable out;
        int lastSpace = text.toString().lastIndexOf(' ');
        if (lastSpace != -1 && lastSpace < text.length() - 1) {
            // Replace last space character by a non breaking space.
            CharSequence tmpText = replaceCharacter(text, lastSpace, "\u00A0");
            out = new SpannableString(tmpText);
            // Restore spans if text is an instance of Spanned
            if (text instanceof Spanned) {
                TextUtils.copySpansFrom((Spanned) text, 0, text.length(), null, out, 0);
            }
        } else {
            out = new SpannableString(text);
        }
        super.setText(out, type);
    }

    private void readCustomAttrs(Context context, AttributeSet attrs) {
        TypedArray array = context.getTheme().obtainStyledAttributes(attrs, R.styleable.WPTextView, 0, 0);
        if (array != null) {
            setFixWidowWord(array.getBoolean(R.styleable.WPTextView_fixWidowWords, false));
            if (mFixWidowWordEnabled) {
                // Force text update
                setText(getText());
            }

            // support vector drawables for API < 5.0
            Drawable drawableLeft = null;
            Drawable drawableRight = null;
            Drawable drawableBottom = null;
            Drawable drawableTop = null;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                drawableLeft = array.getDrawable(R.styleable.WPTextView_drawableLeftCompat);
                drawableRight = array.getDrawable(R.styleable.WPTextView_drawableRightCompat);
                drawableBottom = array.getDrawable(R.styleable.WPTextView_drawableBottomCompat);
                drawableTop = array.getDrawable(R.styleable.WPTextView_drawableTopCompat);
            } else {
                final int drawableLeftId = array.getResourceId(R.styleable.WPTextView_drawableLeftCompat, -1);
                final int drawableRightId = array.getResourceId(R.styleable.WPTextView_drawableRightCompat, -1);
                final int drawableBottomId = array.getResourceId(R.styleable.WPTextView_drawableBottomCompat, -1);
                final int drawableTopId = array.getResourceId(R.styleable.WPTextView_drawableTopCompat, -1);

                if (drawableLeftId != -1) drawableLeft = AppCompatResources.getDrawable(context, drawableLeftId);
                if (drawableRightId != -1) drawableRight = AppCompatResources.getDrawable(context, drawableRightId);
                if (drawableBottomId != -1) drawableBottom = AppCompatResources.getDrawable(context, drawableBottomId);
                if (drawableTopId != -1) drawableTop = AppCompatResources.getDrawable(context, drawableTopId);
            }
            setCompoundDrawablesWithIntrinsicBounds(drawableLeft, drawableTop, drawableRight, drawableBottom);
            array.recycle();
        }
    }

    private CharSequence replaceCharacter(CharSequence source, int charIndex, CharSequence replacement) {
        if (charIndex != -1 && charIndex < source.length() - 1) {
            return TextUtils.concat(source.subSequence(0, charIndex), replacement, source.subSequence(charIndex + 1,
                    source.length()));
        }
        return source;
    }
}
