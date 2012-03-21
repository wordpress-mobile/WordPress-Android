package org.wordpress.android;

import java.util.List;
import java.util.Vector;

import org.wordpress.android.WPCOMReaderBase.ChangeTopicListener;
import org.wordpress.android.WPCOMReaderBase.GetLastSelectedItemListener;
import org.wordpress.android.WPCOMReaderBase.GetLoadedItemsListener;
import org.wordpress.android.WPCOMReaderBase.GetPermalinkListener;
import org.wordpress.android.WPCOMReaderBase.UpdateButtonStatusListener;
import org.wordpress.android.WPCOMReaderBase.UpdateTopicIDListener;
import org.wordpress.android.WPCOMReaderBase.UpdateTopicTitleListener;
import org.wordpress.android.WPCOMReaderDetailPage.LoadExternalURLListener;
import org.wordpress.android.WPCOMReaderImpl.PostSelectedListener;
import org.wordpress.android.WPCOMReaderImpl.ShowTopicsListener;
import org.wordpress.android.util.WPViewPager;

import android.app.Dialog;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.PixelFormat;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.view.WindowManager;

public class WPCOMReaderPager extends FragmentActivity implements
		ChangeTopicListener, PostSelectedListener,
		UpdateTopicIDListener, UpdateTopicTitleListener,
		GetLoadedItemsListener, UpdateButtonStatusListener, ShowTopicsListener,
		LoadExternalURLListener, GetPermalinkListener, GetLastSelectedItemListener {

	private WPViewPager readerPager;
	private ReaderPagerAdapter readerAdapter;
	private Fragment readerPage;
	private Fragment detailPage;
	private Fragment topicPage;
	private Fragment webPage;
	private Dialog topicsDialog;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getWindow().setFormat(PixelFormat.RGBA_8888);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_DITHER);
		setContentView(R.layout.reader_wpcom_pager);

		if (WordPress.wpDB == null)
			WordPress.wpDB = new WordPressDB(this);

	}

	@Override
	protected void onResume() {
		super.onResume();

		readerPager = (WPViewPager) findViewById(R.id.pager);
		readerPager.setOffscreenPageLimit(3);
		readerPage = Fragment
				.instantiate(this, WPCOMReaderImpl.class.getName());
		topicPage = Fragment.instantiate(this,
				WPCOMReaderTopicsSelector.class.getName());
		detailPage = Fragment.instantiate(this,
				WPCOMReaderDetailPage.class.getName());
		webPage = Fragment
				.instantiate(this, WPCOMReaderWebPage.class.getName());

		List<Fragment> fragments = new Vector<Fragment>();

		fragments.add(topicPage);
		fragments.add(readerPage);
		fragments.add(detailPage);
		fragments.add(webPage);
		readerAdapter = new ReaderPagerAdapter(
				super.getSupportFragmentManager(), fragments);

		readerPager.setAdapter(readerAdapter);
		readerPager.setCurrentItem(1, true);

	}

	@Override
	protected void onPause() {
		super.onPause();
		WPCOMReaderImpl readerPageFragment = (WPCOMReaderImpl) readerPage;
		readerPageFragment.wv.stopLoading();
		// finish();
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
			if (readerPager.getCurrentItem() == 2) {
				final WPCOMReaderDetailPage readerPageDetailFragment = (WPCOMReaderDetailPage) detailPage;
				if (readerPageDetailFragment != null) {
					runOnUiThread(new Runnable() {
						public void run() {
							readerPageDetailFragment.wv.loadUrl("javascript:Reader2.get_article_permalink();");
						}
					});
				}
			}
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
	public void onChangeTopic(String topicID, final String topicName) {

		final WPCOMReaderImpl readerPageFragment = (WPCOMReaderImpl) readerPage;
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
	public void onPostSelected(String requestedURL) {
		readerPager.setCurrentItem(2, true);	
	}

	@Override
	public void onBackPressed() {
		if (readerPager.getCurrentItem() > 1)
			readerPager.setCurrentItem(readerPager.getCurrentItem() - 1, true);
		else
			super.onBackPressed();

	}

	@Override
	public void updateTopicTitle(final String topicTitle) {
		final WPCOMReaderImpl readerPageFragment = (WPCOMReaderImpl) readerPage;
		runOnUiThread(new Runnable() {
			public void run() {
				if (topicsDialog != null) {
					if (topicsDialog.isShowing())
						topicsDialog.cancel();
				}
				if (topicTitle != null) {
					readerPageFragment.topicTV.setText(topicTitle);
				}
				// readerPager.setCurrentItem(1, true);
			}
		});

	}

	@Override
	public void onUpdateTopicID(String topicID) {
		final WPCOMReaderTopicsSelector topicsFragment = (WPCOMReaderTopicsSelector) topicPage;
		final String methodCall = "document.setSelectedTopic('" + topicID
				+ "')";
		runOnUiThread(new Runnable() {
			public void run() {
				if (topicsFragment != null)
					topicsFragment.wv.loadUrl("javascript:" + methodCall);
			}
		});
	}

	@Override
	public void getLoadedItems(String items) {
		final WPCOMReaderDetailPage readerPageDetailFragment = (WPCOMReaderDetailPage) detailPage;
		readerPageDetailFragment.readerItems = items;
		final String method = "Reader2.set_loaded_items(" + readerPageDetailFragment.readerItems + ")";
		runOnUiThread(new Runnable() {
			public void run() {
				readerPageDetailFragment.wv.loadUrl("javascript:" + method);
			}
		});
	}

	@Override
	public void updateButtonStatus(final int button, final boolean enabled) {
		final WPCOMReaderDetailPage readerPageDetailFragment = (WPCOMReaderDetailPage) detailPage;
		runOnUiThread(new Runnable() {
			public void run() {
				readerPageDetailFragment.updateButtonStatus(button, enabled);
			}
		});
		
	}

	@Override
	public void showTopics() {
		WPCOMReaderTopicsSelector topicsFragment = (WPCOMReaderTopicsSelector) topicPage;
		((ViewGroup) topicsFragment.getView().getParent())
				.removeView(topicsFragment.getView());
		topicsDialog = new Dialog(this);
		topicsDialog.setContentView(topicsFragment.getView());
		topicsDialog.setTitle(getResources().getText(R.string.topics));
		topicsDialog.setCancelable(true);
		topicsDialog.show();
	}

	@Override
	public void loadExternalURL(String url) {
		WPCOMReaderWebPage readerWebPageFragment = (WPCOMReaderWebPage) webPage;
		readerWebPageFragment.wv.clearView();
		readerWebPageFragment.wv.loadUrl(url);
		readerPager.setCurrentItem(3, true);
	}

	@Override
	public void getPermalink(String permalink) {
		if (!permalink.equals("")) {
			Uri uri = Uri.parse(permalink);
			if (uri != null) {
				Intent i = new Intent(Intent.ACTION_VIEW);
				i.setData(Uri.parse(permalink));
				startActivity(i);
			}
		}

	}

	@Override
	public void getLastSelectedItem(final String lastSelectedItem) {
		final WPCOMReaderDetailPage readerPageDetailFragment = (WPCOMReaderDetailPage) detailPage;
		runOnUiThread(new Runnable() {
			public void run() {
				String methodCall = "Reader2.show_article_details(" + lastSelectedItem + ")";
				if (readerPageDetailFragment != null) {
					readerPageDetailFragment.wv.loadUrl("javascript:" + methodCall); 
					readerPageDetailFragment.wv.loadUrl("javascript:Reader2.is_next_item();");
					readerPageDetailFragment.wv.loadUrl("javascript:Reader2.is_prev_item();");
				}
			}
		});
		
		//readerPageDetailFragment.wv.loadDataWithBaseURL("https://en.wordpress.com", lastSelectedItem, "text/html", "utf-8", null);
		
	}
}