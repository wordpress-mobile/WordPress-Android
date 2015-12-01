package org.wordpress.android.util;

import android.content.Context;
import android.graphics.Typeface;
import android.util.TypedValue;
import android.widget.TextView;

import org.wordpress.android.widgets.TypefaceCache;

import org.wordpress.android.R;

/**
 * Design guidelines for Calypso-styled Site Settings (and likely
 */

public class WPPrefUtils {
    /**
     * Font      : Open Sans
     * Style     : Normal
     * Variation : Normal
     */
    public static Typeface getNormalTypeface(Context context) {
        return TypefaceCache.getTypeface(context,
                TypefaceCache.FAMILY_OPEN_SANS, Typeface.NORMAL, TypefaceCache.VARIATION_NORMAL);
    }

    /**
     * Font      : Open Sans
     * Style     : Bold
     * Variation : Light
     */
    public static Typeface getSemiboldTypeface(Context context) {
        return TypefaceCache.getTypeface(context,
                TypefaceCache.FAMILY_OPEN_SANS, Typeface.BOLD, TypefaceCache.VARIATION_LIGHT);
    }

    /**
     * Styles a {@link TextView} to display a large title against a dark background.
     */
    public static void layoutAsLightTitle(TextView view) {
        int size = view.getResources().getDimensionPixelSize(R.dimen.text_sz_extra_large);
        setTextViewAttributes(view, size, R.color.white, getSemiboldTypeface(view.getContext()));
    }

    /**
     * Styles a {@link TextView} to display a large title against a light background.
     */
    public static void layoutAsDarkTitle(TextView view) {
        int size = view.getResources().getDimensionPixelSize(R.dimen.text_sz_extra_large);
        setTextViewAttributes(view, size, R.color.grey_dark, getSemiboldTypeface(view.getContext()));
    }

    /**
     * Styles a {@link TextView} to display medium sized text as a header with sub-elements.
     */
    public static void layoutAsSubhead(TextView view) {
        int color = view.isEnabled() ? R.color.grey_dark : R.color.grey_lighten_10;
        int size = view.getResources().getDimensionPixelSize(R.dimen.text_sz_large);
        setTextViewAttributes(view, size, color, getNormalTypeface(view.getContext()));
    }

    /**
     * Styles a {@link TextView} to display smaller text.
     */
    public static void layoutAsBody1(TextView view) {
        int color = view.isEnabled() ? R.color.grey_darken_10 : R.color.grey_lighten_10;
        int size = view.getResources().getDimensionPixelSize(R.dimen.text_sz_medium);
        setTextViewAttributes(view, size, color, getNormalTypeface(view.getContext()));
    }

    /**
     * Styles a {@link TextView} to display smaller text with the orange accent color.
     */
    public static void layoutAsBody2(TextView view) {
        int size = view.getResources().getDimensionPixelSize(R.dimen.text_sz_medium);
        setTextViewAttributes(view, size, R.color.orange_jazzy, getSemiboldTypeface(view.getContext()));
    }

    /**
     * Styles a {@link TextView} to display very small helper text.
     */
    public static void layoutAsCaption(TextView view) {
        int size = view.getResources().getDimensionPixelSize(R.dimen.text_sz_small);
        setTextViewAttributes(view, size, R.color.grey_darken_10, getNormalTypeface(view.getContext()));
    }

    /**
     * Styles a {@link TextView} to display text in a button.
     */
    public static void layoutAsFlatButton(TextView view) {
        int size = view.getResources().getDimensionPixelSize(R.dimen.text_sz_medium);
        setTextViewAttributes(view, size, R.color.blue_medium, getSemiboldTypeface(view.getContext()));
    }

    /**
     * Styles a {@link TextView} to display text in a button.
     */
    public static void layoutAsRaisedButton(TextView view) {
        int size = view.getResources().getDimensionPixelSize(R.dimen.text_sz_medium);
        setTextViewAttributes(view, size, R.color.white, getSemiboldTypeface(view.getContext()));
    }

    /**
     * Styles a {@link TextView} to display text in an editable text field.
     */
    public static void layoutAsInput(TextView view) {
        int size = view.getResources().getDimensionPixelSize(R.dimen.text_sz_large);
        setTextViewAttributes(view, size, R.color.grey_dark, getNormalTypeface(view.getContext()));
    }

    /**
     * Styles a {@link TextView} to display selected numbers in a {@link android.widget.NumberPicker}.
     */
    public static void layoutAsNumberPickerSelected(TextView view) {
        int size = view.getResources().getDimensionPixelSize(R.dimen.text_sz_triple_extra_large);
        setTextViewAttributes(view, size, R.color.blue_medium, getSemiboldTypeface(view.getContext()));
    }

    /**
     * Styles a {@link TextView} to display non-selected numbers in a {@link android.widget.NumberPicker}.
     */
    public static void layoutAsNumberPickerPeek(TextView view) {
        int size = view.getResources().getDimensionPixelSize(R.dimen.text_sz_large);
        setTextViewAttributes(view, size, R.color.grey_dark, getNormalTypeface(view.getContext()));
    }

    public static void setTextViewAttributes(TextView textView, int size, int colorRes, int family, int style, int variation) {
        if (textView == null) return;
        Context context = textView.getContext();
        if (context == null) return;

        Typeface typeface = TypefaceCache.getTypeface(context, family, style, variation);
        setTextViewAttributes(textView, size, colorRes, typeface);
    }

    public static void setTextViewAttributes(TextView textView, int size, int colorRes, Typeface typeface) {
        textView.setTypeface(typeface);
        textView.setTextSize(TypedValue.COMPLEX_UNIT_PX, size);
        textView.setTextColor(textView.getResources().getColor(colorRes));
    }
}
