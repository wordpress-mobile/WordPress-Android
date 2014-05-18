package org.wordpress.android.ui.reader;

import android.app.Fragment;
import android.app.FragmentManager;
import android.os.Bundle;
import android.support.v13.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.MenuItem;

import org.wordpress.android.R;
import org.wordpress.android.ui.WPActionBarActivity;
import org.wordpress.android.ui.reader.models.ReaderBlogIdPostId;
import org.wordpress.android.ui.reader.models.ReaderBlogIdPostIdList;

import java.util.ArrayList;

public class ReaderPostPagerActivity extends WPActionBarActivity {
    protected static final String ARG_POSITION = "position";
    protected static final String ARG_BLOG_POST_ID_LIST = "blog_post_id_list";

    private ViewPager mViewPager;
    private PostPagerAdapter mPageAdapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        createMenuDrawer(R.layout.reader_activity_post_pager);
        mViewPager = (ViewPager) findViewById(R.id.viewpager);

        final ReaderBlogIdPostIdList idList;
        final int position;
        if (getIntent().hasExtra(ARG_BLOG_POST_ID_LIST)) {
            position = getIntent().getIntExtra(ARG_POSITION, 0);
            ArrayList<ReaderBlogIdPostId> list =
                    (ArrayList<ReaderBlogIdPostId>) getIntent().getSerializableExtra(ARG_BLOG_POST_ID_LIST);
            idList = new ReaderBlogIdPostIdList(list);
        } else {
            position = 0;
            idList = new ReaderBlogIdPostIdList();
        }

        mPageAdapter = new PostPagerAdapter(getFragmentManager(), idList);
        mViewPager.setAdapter(mPageAdapter);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return super.onOptionsItemSelected(item);
    }

    class PostPagerAdapter extends FragmentStatePagerAdapter {
        private ReaderBlogIdPostIdList mIdList;

        PostPagerAdapter(FragmentManager fm, ReaderBlogIdPostIdList idList) {
            super(fm);
            mIdList = (ReaderBlogIdPostIdList)idList.clone();
        }

        @Override
        public int getCount() {
            return mIdList.size();
        }

        @Override
        public Fragment getItem(int position) {
            long blogId = mIdList.get(position).blogId;
            long postId = mIdList.get(position).postId;
            return ReaderPostDetailFragment.newInstance(blogId, postId);
        }
    }
}
