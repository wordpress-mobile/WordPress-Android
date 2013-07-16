package org.wordpress.android.ui.themes;

import android.app.Activity;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView.RecyclerListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.GridView;

import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageLoader.ImageContainer;
import com.android.volley.toolbox.ImageLoader.ImageListener;
import com.android.volley.toolbox.NetworkImageView;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.ui.themes.ThemeTabAdapter.ViewHolder;

public class ThemeTabFragment extends Fragment implements OnItemClickListener, RecyclerListener {

    public enum ThemeSortType {
        TRENDING("Trending"), 
        A_Z("A-Z"), 
        POPULAR("Popular"), 
        NEWEST("Newest"),
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

    public interface ThemeTabFragmentCallback {
        public void onThemeSelected(String themeId);
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
    private ThemeTabFragmentCallback mCallback;
    
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        
        try {
            mCallback = (ThemeTabFragmentCallback) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement ThemeTabFragmentCallback");
        }
    }
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        
        View view = inflater.inflate(R.layout.theme_tab_fragment, container, false);
        
        mGridView = (GridView) view.findViewById(R.id.theme_gridview);
        mGridView.setRecyclerListener(this);
        
        return view;
    }
    
    @Override
    public void onResume() {
        super.onResume();
        
        Cursor cursor = fetchThemes(getThemeSortType());
        mAdapter = new ThemeTabAdapter(getActivity(), cursor, false);
        mGridView.setAdapter(mAdapter);
        mGridView.setOnItemClickListener(this);
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
            case TRENDING:
            default:
                return WordPress.wpDB.getThemesTrending(blogId);
                
        }
        
    }

    public void refresh() {
        Cursor cursor = fetchThemes(getThemeSortType());
        mAdapter.swapCursor(cursor);
    }
    
    public void search(String searchTerm) {
        
        String blogId = getBlogId();
        Cursor cursor =  WordPress.wpDB.getThemes(blogId, searchTerm);
        if (mAdapter == null) {
            return;
        } else {   
            mAdapter.swapCursor(cursor);   
            mGridView.invalidateViews();
        }             
    }
    
    private String getBlogId() {
        return String.valueOf(WordPress.getCurrentBlog().getBlogId());
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Cursor cursor = ((ThemeTabAdapter) parent.getAdapter()).getCursor();
        String themeId = cursor.getString(cursor.getColumnIndex("themeId"));
        mCallback.onThemeSelected(themeId);
    }

    @Override
    public void onMovedToScrapHeap(View view) {
        // cancel image fetch requests if the view has been moved to recycler.
        
        NetworkImageView niv = (NetworkImageView) view.findViewById(R.id.theme_grid_item_image);
        if (niv != null) {
            // this tag is set in the ThemeTabAdapter class
            ThemeTabAdapter.ViewHolder tag =  (ViewHolder) niv.getTag();
            if (tag != null && tag.requestURL != null) {
                // need a listener to cancel request, even if the listener does nothing
                ImageContainer container = WordPress.imageLoader.get(tag.requestURL, new ImageListener() {

                    @Override
                    public void onErrorResponse(VolleyError error) { }

                    @Override
                    public void onResponse(ImageContainer response, boolean isImmediate) { }
                    
                });
                container.cancelRequest();
            }
        }        
    }
}
