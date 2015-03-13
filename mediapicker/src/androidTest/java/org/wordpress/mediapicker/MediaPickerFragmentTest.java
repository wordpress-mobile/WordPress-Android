package org.wordpress.mediapicker;

import android.app.Activity;
import android.os.Parcel;
import android.support.v4.app.FragmentActivity;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.android.volley.toolbox.ImageLoader;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.util.FragmentTestUtil;
import org.wordpress.mediapicker.source.MediaSource;

import java.util.ArrayList;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Config(emulateSdk = 18)
@RunWith(RobolectricTestRunner.class)
public class MediaPickerFragmentTest {
    private boolean tSelectionStarted;
    private boolean tSelectionCancelled;
    private boolean tSelectionConfirmed;

    @Before
    public void setUp() {
        tSelectionStarted = false;
        tSelectionCancelled = false;
        tSelectionConfirmed = false;
    }

    /**
     * Verifies that the listener is notified when media selection begins.
     */
    @Test
    public void testMediaSelectionStart() {
        final MediaPickerFragment testFragment = new MediaPickerFragment();
        final ActionMode mockActionMode = mock(ActionMode.class);
        final Menu mockMenu = mock(Menu.class);
        final MediaActivity testListener = new MediaActivity();

        FragmentTestUtil.startFragment(testFragment);
        testFragment.setListener(testListener);
        testFragment.onPrepareActionMode(mockActionMode, mockMenu);

        Assert.assertTrue(tSelectionStarted);
    }

    /**
     * Verifies that the listener is notified when media selection is cancelled.
     */
    @Test
    public void testMediaSelectionCancelled() {
        final MediaPickerFragment testFragment = new MediaPickerFragment();
        final ActionMode mockActionMode = mock(ActionMode.class);
        final MediaActivity testListener = new MediaActivity();

        FragmentTestUtil.startFragment(testFragment);
        testFragment.setListener(testListener);
        testFragment.onDestroyActionMode(mockActionMode);

        Assert.assertTrue(tSelectionCancelled);
    }

    /**
     * Verifies that a single item being clicked confirms selection.
     */
    @Test
    public void testMediaSelectionConfirmedFromSingleClick() {
        final MediaPickerFragment testFragment = new MediaPickerFragment();
        final MediaActivity testListener = new MediaActivity();
        final ArrayList<MediaSource> testMediaSources = new ArrayList<>();
        final MediaSourceAdapter mockAdapter = mock(MediaSourceAdapter.class);

        when(mockAdapter.getItem(0)).thenReturn(new MediaItem());
        when(mockAdapter.getCount()).thenReturn(1);
        when(mockAdapter.getViewTypeCount()).thenReturn(1);
        testFragment.setAdapter(mockAdapter);

        testMediaSources.add(mMediaSourceOnMediaSelectedFalse);
        testFragment.setListener(testListener);
        testFragment.setMediaSources(testMediaSources);

        FragmentTestUtil.startFragment(testFragment);
        testFragment.onItemClick(null, null, 0, 0);

        Assert.assertTrue(tSelectionConfirmed);
    }

    /**
     * Verifies that selecting the confirm option in Action Mode confirms selection.
     */
    @Test
    public void testMediaSelectionConfirmedFromActionModeMenu() {
        final MediaPickerFragment testFragment = new MediaPickerFragment();
        final ActionMode mockActionMode = mock(ActionMode.class);
        final MenuItem mockMenuItem = mock(MenuItem.class);
        final MediaActivity testListener = new MediaActivity();

        when(mockMenuItem.getItemId()).thenReturn(R.id.menu_media_selection_confirmed);

        FragmentTestUtil.startFragment(testFragment);
        testFragment.setListener(testListener);
        testFragment.onItemCheckedStateChanged(mockActionMode, 0, 0, true);
        testFragment.onActionItemClicked(mockActionMode, mockMenuItem);

        Assert.assertTrue(tSelectionConfirmed);
    }

    private MediaSource mMediaSourceOnMediaSelectedFalse = new MediaSource() {
        @Override
        public void gather(OnMediaLoaded callback) {
        }

        @Override
        public void cleanup() {
        }

        @Override
        public void setListener(OnMediaChange listener) {
        }

        @Override
        public int getCount() {
            return 0;
        }

        @Override
        public MediaItem getMedia(int position) {
            return null;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent, LayoutInflater inflater, ImageLoader.ImageCache cache) {
            return null;
        }

        @Override
        public boolean onMediaItemSelected(MediaItem mediaItem, boolean selected) {
            return false;
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
        }
    };

    /**
     * Dummy class for testing various media selection interface calls.
     */
    public class MediaActivity extends Activity implements MediaPickerFragment.OnMediaSelected {
        @Override
        public void onMediaSelectionStarted() {
            tSelectionStarted = true;
        }

        @Override
        public void onMediaSelected(MediaItem mediaContent, boolean selected) {
        }

        @Override
        public void onMediaSelectionConfirmed(ArrayList<MediaItem> mediaContent) {
            tSelectionConfirmed = true;
        }

        @Override
        public void onMediaSelectionCancelled() {
            tSelectionCancelled = true;
        }

        @Override
        public boolean onMenuItemSelected(MenuItem menuItem) {
            return false;
        }

        @Override
        public ImageLoader.ImageCache getImageCache() {
            return null;
        }
    }
}
