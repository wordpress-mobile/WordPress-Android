package org.wordpress.android;

import java.util.List;
import java.util.Vector;

import org.wordpress.android.WPCOMReaderImpl.ChangePageListener;
import org.wordpress.android.WPCOMReaderImpl.PostSelectedListener;
import org.wordpress.android.WPCOMReaderImpl.UpdateTopicListener;
import org.wordpress.android.WPCOMReaderTopicsSelector.ChangeTopicListener;
import org.wordpress.android.util.WPViewPager;

import android.content.res.Configuration;
import android.graphics.PixelFormat;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;

public class WPCOMReaderPager extends FragmentActivity implements
		ChangePageListener, ChangeTopicListener, PostSelectedListener, UpdateTopicListener {

	private WPViewPager readerPager;
	private ReaderPagerAdapter readerAdapter;
	private Fragment readerPage;
	private Fragment detailPage;
	private Fragment topicPage;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getWindow().setFormat(PixelFormat.RGBA_8888);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_DITHER);
		setContentView(R.layout.reader);
		setContentView(R.layout.reader_wpcom_pager);

		if (WordPress.wpDB == null)
			WordPress.wpDB = new WordPressDB(this);

	}

	@Override
	protected void onResume() {
		super.onResume();
		
		readerPager = (WPViewPager) findViewById(R.id.pager);
		readerPage = Fragment
				.instantiate(this, WPCOMReaderImpl.class.getName());
		detailPage = Fragment.instantiate(this,
				WPCOMReaderDetailPage.class.getName());
		topicPage = Fragment.instantiate(this,
				WPCOMReaderTopicsSelector.class.getName());

		List<Fragment> fragments = new Vector<Fragment>();

		fragments.add(topicPage);
		fragments.add(readerPage);
		fragments.add(detailPage);
		readerAdapter = new ReaderPagerAdapter(
				super.getSupportFragmentManager(), fragments);

		readerPager.setAdapter(readerAdapter);
		readerPager.setCurrentItem(1, true);

	}
	
	@Override
	protected void onPause() {
		super.onPause();
		finish();
	}

	private class ReaderPagerAdapter extends FragmentPagerAdapter {

		private List<Fragment> fragments;

		public ReaderPagerAdapter(FragmentManager fm, List<Fragment> fragments) {
			super(fm);
			this.fragments = fragments;
		}

		@Override
		public int getCount() {
			return fragments.size();
		}

		/**
		 * Remove a page for the given position. The adapter is responsible for
		 * removing the view from its container, although it only must ensure
		 * this is done by the time it returns from {@link #finishUpdate()}.
		 * 
		 * @param container
		 *            The containing View from which the page will be removed.
		 * @param position
		 *            The page position to be removed.
		 * @param object
		 *            The same object that was returned by
		 *            {@link #instantiateItem(View, int)}.
		 */
		/*
		 * @Override public void destroyItem(View collection, int position,
		 * Object view) { ((ViewPager) collection).removeView((TextView) view);
		 * }
		 * 
		 * 
		 * 
		 * @Override public boolean isViewFromObject(View view, Object object) {
		 * return view==((TextView)object); }
		 */

		/**
		 * Called when the a change in the shown pages has been completed. At
		 * this point you must ensure that all of the pages have actually been
		 * added or removed from the container as appropriate.
		 * 
		 * @param container
		 *            The containing View which is displaying this adapter's
		 *            page views.
		 */
		@Override
		public void finishUpdate(View arg0) {
		}

		@Override
		public void restoreState(Parcelable arg0, ClassLoader arg1) {
		}

		@Override
		public Parcelable saveState() {
			return null;
		}

		@Override
		public void startUpdate(View arg0) {
		}

		@Override
		public Fragment getItem(int location) {
			// TODO Auto-generated method stub
			return fragments.get(location);
		}

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);

		menu.add(0, 0, 0, getResources().getText(R.string.home));
		MenuItem menuItem = menu.findItem(0);
		menuItem.setIcon(R.drawable.ic_menu_home);

		menu.add(0, 1, 0, getResources().getText(R.string.view_in_browser));
		menuItem = menu.findItem(1);
		menuItem.setIcon(android.R.drawable.ic_menu_view);

		return true;
	}

	public boolean onOptionsItemSelected(final MenuItem item) {
		switch (item.getItemId()) {
		case 0:
			finish();
			break;
		case 1:
			/*
			 * if (!readerList.wv.getUrl().contains("wp-login.php")) { Intent i
			 * = new Intent(Intent.ACTION_VIEW);
			 * i.setData(Uri.parse(readerList.wv.getUrl())); startActivity(i); }
			 */
			break;
		default:
			break;
		}
		return false;
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		// ignore orientation change
		super.onConfigurationChanged(newConfig);
	}

	@Override
	public void onChangePage(int position) {
		readerPager.setCurrentItem(position);
	}

	@Override
	public void onChangeTopic(String topicID, final String topicName) {

		final WPCOMReaderImpl readerPageFragment = (WPCOMReaderImpl) readerPage;
		if (readerPageFragment.topicsID.equalsIgnoreCase(topicID))
			return;
		readerPageFragment.topicsID = topicID;
		String methodCall = "Reader2.load_topic('" + topicID + "')";
		readerPageFragment.wv.loadUrl("javascript:" + methodCall);
		runOnUiThread(new Runnable() {
			public void run() {
				if (topicName != null) {
					readerPageFragment.topicTV.setText(topicName);
				}
				readerPager.setCurrentItem(1, true);
			}
		});
	}

	@Override
	public void onPostSelected(String requestedURL, String cachedPage) {
		WPCOMReaderDetailPage readerPageDetailFragment = (WPCOMReaderDetailPage) detailPage;
		readerPageDetailFragment.wv.clearView();
		readerPageDetailFragment.wv.loadUrl(requestedURL);
		readerPager.setCurrentItem(2, true);
	}

	@Override
	public void onBackPressed() {
		if (readerPager.getCurrentItem() != 1)
			readerPager.setCurrentItem(1, true);
		else
			super.onBackPressed();

	}

	@Override
	public void onUpdateTopic(String topicID) {
		WPCOMReaderTopicsSelector topicsFragment = (WPCOMReaderTopicsSelector) topicPage;
		String methodCall = "document.setSelectedTopic('"+topicID+"')";
		topicsFragment.wv.loadUrl("javascript:"+methodCall);
	}
}