package org.wordpress.android.ui.themes;

import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridView;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;

public class ThemeTabFragment extends Fragment {

    public enum ThemeSortType {
        TRENDING("Trending"), 
        A_Z("A-Z"), 
        POPULAR("Popular"), 
        NEWEST("Newest"),
        PREMIUM("Premium"),
        FRIENDS_OF_WP("Friends of WP");
        
        private String mTitle;

        private ThemeSortType(String title) {
            mTitle = title;
        }
        
        public String getTitle() {
            return mTitle;
        }
        
        public static ThemeSortType getTheme(int position) {
            if (position < ThemeSortType.values().length)
                return ThemeSortType.values()[position];
            else
                return TRENDING;
        }
    }
    
    private static final String ARGS_THEME = "ARGS_THEME";
    
    public static ThemeTabFragment newInstance(ThemeSortType theme) {
        
        ThemeTabFragment fragment = new ThemeTabFragment();
        
        Bundle args = new Bundle();
        args.putInt(ARGS_THEME, theme.ordinal());
        fragment.setArguments(args);
        
        return fragment;
    }

    private GridView mGridView;
    private ThemeTabAdapter mAdapter;
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        
        View view = inflater.inflate(R.layout.theme_tab_fragment, container, false);
        
        mGridView = (GridView) view.findViewById(R.id.theme_gridview);
        
        return view;
    }
    
    @Override
    public void onResume() {
        super.onResume();
        
        Cursor cursor = fetchThemes(getThemeSortType());
        mAdapter = new ThemeTabAdapter(getActivity(), cursor, false);
        mGridView.setAdapter(mAdapter);
    }

    private ThemeSortType getThemeSortType() {
        int sortType = ThemeSortType.TRENDING.ordinal();
        if (getArguments().containsKey(ARGS_THEME))  {
            sortType = getArguments().getInt(ARGS_THEME);
        }
        
        return ThemeSortType.getTheme(sortType);
    }
    
    private Cursor fetchThemes(ThemeSortType themeSortType) {
        
        String blogId = getBlogId();
        
        switch(themeSortType) {
            case A_Z:
                return WordPress.wpDB.getThemesAtoZ(blogId);
            case FRIENDS_OF_WP:
                return WordPress.wpDB.getThemesFriendsOfWP(blogId);
            case POPULAR:
                return WordPress.wpDB.getThemesPopularity(blogId);
            case NEWEST:
                return WordPress.wpDB.getThemesNewest(blogId);
            case PREMIUM:
                return WordPress.wpDB.getThemesPremium(blogId);
            case TRENDING:
            default:
                return WordPress.wpDB.getThemesTrending(blogId);
                
        }
        
    }

    public void refresh() {
        Cursor cursor = fetchThemes(getThemeSortType());
        if (mAdapter == null) {
            mAdapter = new ThemeTabAdapter(getActivity(), cursor, false);
            mGridView.setAdapter(mAdapter);
        } else {
            mAdapter.swapCursor(cursor);
        }
    }
    
    private String getBlogId() {
        return String.valueOf(WordPress.getCurrentBlog().getBlogId());
    }
}
