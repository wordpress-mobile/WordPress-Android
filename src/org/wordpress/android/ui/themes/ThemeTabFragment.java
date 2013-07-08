package org.wordpress.android.ui.themes;

import java.util.ArrayList;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridView;

import com.android.volley.VolleyError;
import com.wordpress.rest.RestRequest.ErrorListener;
import com.wordpress.rest.RestRequest.Listener;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.models.Theme;

public class ThemeTabFragment extends Fragment {

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
    
    private static final String ARGS_THEME = "ARGS_THEME";
    
    public static Fragment newInstance(ThemeSortType theme) {
        
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

        mAdapter = new ThemeTabAdapter(getActivity(), new ArrayList<Theme>());
        mGridView.setAdapter(mAdapter);
        
        fetchThemes(getThemeSortType());
        
    }

    private ThemeSortType getThemeSortType() {
        int sortType = ThemeSortType.TRENDING.ordinal();
        if (getArguments().containsKey(ARGS_THEME))  {
            sortType = getArguments().getInt(ARGS_THEME);
        }
        
        return ThemeSortType.getTheme(sortType);
    }
    
    private void fetchThemes(ThemeSortType themeSortType) {
        
        String sort = "trending";
        
        switch(themeSortType) {
            case A_Z:
            case FRIENDS_OF_WP:
            case POPULAR:
                sort = "popular";
                break;
            case NEWEST:
                sort = "newest";
                break;
            case TRENDING:
            default:
                
        }
         
        String siteId = String.valueOf(WordPress.getCurrentBlog().getBlogId());
        
        WordPress.restClient.getThemes(siteId, sort, 0, 0, new Listener() {
            
            @Override
            public void onResponse(JSONObject response) {
                new FetchThemesTask().execute(response);
            }
        }, new ErrorListener() {
            
            @Override
            public void onErrorResponse(VolleyError response) {
                Log.d("WordPress", "Failed to download themes: " + response.getMessage());
            }
        });
        
    }
    
    public class FetchThemesTask extends AsyncTask<JSONObject, Void, ArrayList<Theme>> {

        @Override
        protected ArrayList<Theme> doInBackground(JSONObject... args) {
            JSONObject response = args[0];
            
            final ArrayList<Theme> themes = new ArrayList<Theme>();
            
            if (response != null) {
                JSONArray array = null;
                try {
                    array = response.getJSONArray("themes");
                    System.out.println("Themes: " + themes.size());

                    if (array != null) {

                        for (int i = 0; i < array.length(); i++) {
                            JSONObject object = array.getJSONObject(i);

                            themes.add(Theme.fromJSON(object));
                        }
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            if (themes != null && themes.size() > 0) {
                return themes;
            }
            return null;
        }
        
        @Override
        protected void onPostExecute(ArrayList<Theme> result) {
            mAdapter.setThemes(result);
        }
       
    }
    
}
