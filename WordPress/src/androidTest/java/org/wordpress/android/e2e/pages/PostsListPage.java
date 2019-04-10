package org.wordpress.android.e2e.pages;

import android.support.v7.widget.RecyclerView;

import org.wordpress.android.R;

import static org.wordpress.android.support.WPSupportUtils.getCurrentActivity;
import static org.wordpress.android.support.WPSupportUtils.scrollToAndClickOnTextInRecyclerView;


public class PostsListPage {
    public PostsListPage() {}

    public static void tapPostWithName(String name) {
        scrollToAndClickOnTextInRecyclerView(name, getRecyclerView());
    }

    private static RecyclerView getRecyclerView() {
        return (RecyclerView) getCurrentActivity().findViewById(R.id.recycler_view);
    }
}
