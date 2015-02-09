package org.wordpress.android.util.helpers;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.text.Layout;
import android.text.style.QuoteSpan;

/**
 * Customzed QuoteSpan for use in SpannableString's
 */
public class WPQuoteSpan extends QuoteSpan {
    public static final int STRIPE_COLOR = 0xFF21759B;
    private static final int STRIPE_WIDTH = 5;
    private static final int GAP_WIDTH = 20;

    public WPQuoteSpan(){
        super(STRIPE_COLOR);
    }

    @Override
    public int getLeadingMargin(boolean first) {
        int margin = GAP_WIDTH * 2 + STRIPE_WIDTH;
        return margin;
    }

    /**
     * Draw a nice thick gray bar if Ice Cream Sandwhich or newer. There's a
     * bug on older devices that does not respect the increased margin.
     */
    @Override
    public void drawLeadingMargin(Canvas c, Paint p, int x, int dir, int top, int baseline, int bottom,
                                  CharSequence text, int start, int end, boolean first, Layout layout) {
        Paint.Style style = p.getStyle();
        int color = p.getColor();

        p.setStyle(Paint.Style.FILL);
        p.setColor(STRIPE_COLOR);

        c.drawRect(GAP_WIDTH + x, top, x + dir * STRIPE_WIDTH, bottom, p);

        p.setStyle(style);
        p.setColor(color);
    }
}
