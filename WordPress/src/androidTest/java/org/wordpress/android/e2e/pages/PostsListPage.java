package org.wordpress.android.e2e.pages;

import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager.widget.ViewPager;

import org.wordpress.android.R;

import static org.wordpress.android.support.WPSupportUtils.getCurrentActivity;
import static org.wordpress.android.support.WPSupportUtils.scrollToAndClickOnTextInRecyclerView;
import static org.wordpress.android.support.WPSupportUtils.scrollToTopOfRecyclerView;

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
        return (RecyclerView) pager.getChildAt(pager.getCurrentItem()).findViewById(R.id.recycler_view);
    }
}
