package org.wordpress.android.e2e.pages;

import android.view.ViewGroup;

import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager.widget.ViewPager;

import org.wordpress.android.R;

import static org.wordpress.android.support.WPSupportUtils.getCurrentActivity;
import static org.wordpress.android.support.WPSupportUtils.getTranslatedString;
import static org.wordpress.android.support.WPSupportUtils.scrollToAndClickOnTextInRecyclerView;
import static org.wordpress.android.support.WPSupportUtils.scrollToTopOfRecyclerView;
import static org.wordpress.android.support.WPSupportUtils.selectItemWithTitleInTabLayout;

public class PostsListPage {
    public PostsListPage() {}

    public static void tapPostWithName(String name) {
        scrollToAndClickOnTextInRecyclerView(name, getRecyclerView());
    }

    public static void scrollToTop() {
        scrollToTopOfRecyclerView(getRecyclerView());
    }

    private static RecyclerView getRecyclerView() {
        ViewPager pager = getCurrentActivity().findViewById(R.id.postPager);
        RecyclerView recyclerView = (RecyclerView) pager.getChildAt(pager.getCurrentItem())
                                                        .findViewById(R.id.recycler_view);
        if (recyclerView == null) {
            // Workaround for cases when recyclerview id missing
            recyclerView = (RecyclerView) ((ViewGroup) ((ViewGroup) (pager.getChildAt(pager.getCurrentItem()))
                    .findViewById(R.id.ptr_layout)).getChildAt(0)).getChildAt(0);
        }
        return recyclerView;
    }

    public static void goToDrafts() {
        selectItemWithTitleInTabLayout(getTranslatedString(R.string.post_list_tab_drafts), R.id.tabLayout);
    }
}
