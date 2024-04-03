package org.wordpress.android.widgets;

import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView.Adapter;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;
import androidx.viewpager2.widget.ViewPager2;

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

    /** {@link ViewPager2}-based clone of the original helper method: {@link #show(ViewPager)} */
    @NonNull
    public static Snackbar show(@NonNull ViewPager2 viewPager) {
        SwipeArrows arrows;
        Adapter<?> adapter = viewPager.getAdapter();
        if (adapter == null) {
            arrows = SwipeArrows.NONE;
        } else {
            arrows = getSwipeArrows(adapter.getItemCount(), viewPager.getCurrentItem());
        }
        return show(viewPager, arrows);
    }

    @NonNull public static Snackbar show(@NonNull ViewPager viewPager) {
        SwipeArrows arrows;
        PagerAdapter adapter = viewPager.getAdapter();
        if (adapter == null) {
            arrows = SwipeArrows.NONE;
        } else {
            arrows = getSwipeArrows(adapter.getCount(), viewPager.getCurrentItem());
        }
        return show(viewPager, arrows);
    }

    @NonNull private static SwipeArrows getSwipeArrows(int itemCount, int currentItem) {
        SwipeArrows arrows;
        if (itemCount <= 1) {
            arrows = SwipeArrows.NONE;
        } else if (currentItem == 0) {
            arrows = SwipeArrows.RIGHT;
        } else if (currentItem == (itemCount - 1)) {
            arrows = SwipeArrows.LEFT;
        } else {
            arrows = SwipeArrows.BOTH;
        }
        return arrows;
    }

    @NonNull private static Snackbar show(@NonNull View view, @NonNull SwipeArrows arrows) {
        String text = getSwipeText(view.getContext(), arrows);
        Snackbar snackbar = WPSnackbar.make(view, text, BaseTransientBottomBar.LENGTH_LONG);
        centerSnackbarText(snackbar);
        snackbar.show();
        return snackbar;
    }

    @NonNull private static String getSwipeText(@NonNull Context context, @NonNull SwipeArrows arrows) {
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
            case NONE:
            default:
                text = swipeText;
                break;
        }
        return text;
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
