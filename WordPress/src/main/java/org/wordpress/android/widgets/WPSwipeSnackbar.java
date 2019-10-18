package org.wordpress.android.widgets;

import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.snackbar.BaseTransientBottomBar;
import com.google.android.material.snackbar.Snackbar;

import org.wordpress.android.R;

/*
 * Snackbar used with a ViewPager to indicate to the user they can swipe to see more pages
 */
public class WPSwipeSnackbar {
    public enum SwipeArrows {
        LEFT, RIGHT, BOTH, NONE
    }

    private WPSwipeSnackbar() {
        throw new AssertionError();
    }

    public static Snackbar show(@NonNull ViewPager viewPager) {
        SwipeArrows arrows;
        PagerAdapter adapter = viewPager.getAdapter();
        if (adapter == null || adapter.getCount() <= 1) {
            arrows = SwipeArrows.NONE;
        } else if (viewPager.getCurrentItem() == 0) {
            arrows = SwipeArrows.RIGHT;
        } else if (viewPager.getCurrentItem() == (adapter.getCount() - 1)) {
            arrows = SwipeArrows.LEFT;
        } else {
            arrows = SwipeArrows.BOTH;
        }
        return show(viewPager, arrows);
    }

    private static Snackbar show(@NonNull ViewPager viewPager, @NonNull SwipeArrows arrows) {
        Context context = viewPager.getContext();
        String swipeText = context.getResources().getString(R.string.swipe_for_more);
        String arrowLeft = context.getResources().getString(R.string.previous_button);
        String arrowRight = context.getResources().getString(R.string.next_button);

        String text;
        switch (arrows) {
            case LEFT:
                text = arrowLeft + " " + swipeText;
                break;
            case RIGHT:
                text = swipeText + " " + arrowRight;
                break;
            case BOTH:
                text = arrowLeft + " " + swipeText + " " + arrowRight;
                break;
            default:
                text = swipeText;
                break;
        }

        Snackbar snackbar = Snackbar.make(viewPager, text, BaseTransientBottomBar.LENGTH_LONG); // CHECKSTYLE IGNORE
        centerSnackbarText(snackbar);
        snackbar.show();

        return snackbar;
    }

    /*
     * horizontally center the snackbar's text
     */
    private static void centerSnackbarText(@NonNull Snackbar snackbar) {
        TextView textView = (TextView) snackbar.getView().findViewById(com.google.android.material.R.id.snackbar_text);
        if (textView != null) {
            textView.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
            textView.setGravity(Gravity.CENTER_HORIZONTAL);
        }
    }
}
