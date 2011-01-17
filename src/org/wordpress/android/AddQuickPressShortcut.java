package org.wordpress.android;

import java.util.HashMap;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;

import com.commonsware.cwac.cache.SimpleWebImageCache;
import com.commonsware.cwac.thumbnail.ThumbnailAdapter;
import com.commonsware.cwac.thumbnail.ThumbnailBus;
import com.commonsware.cwac.thumbnail.ThumbnailMessage;

public class AddQuickPressShortcut extends wpAndroid {
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.add_quickpress_shortcut);
		setTitle(getResources().getText(R.string.quickpress_window_title));
		
		WordPressDB settingsDB = new WordPressDB(this);
		accounts = settingsDB.getAccounts(this);
		
		ListView listView = (ListView) findViewById(android.R.id.list);
		
		ImageView iv = new ImageView(this);
		iv.setBackgroundDrawable(getResources().getDrawable(R.drawable.list_divider));
		listView.addFooterView(iv);
		listView.setVerticalFadingEdgeEnabled(false);
		listView.setVerticalScrollBarEnabled(true);

		if (accounts.size() > 0) {
			ScrollView sv = new ScrollView(this);
			sv.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT));
			LinearLayout layout = new LinearLayout(this);
			layout.setPadding(10, 10, 10, 0);
			layout.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT));

			layout.setOrientation(LinearLayout.VERTICAL);

			blogNames = new String[accounts.size()];
			accountIDs = new String[accounts.size()];
			accountUsers = new String[accounts.size()];
			blavatars = new String[accounts.size()];
			int validBlogCtr = 0;
			for (int i = 0; i < accounts.size(); i++) {

				HashMap<?, ?> curHash = (HashMap<?, ?>) accounts.get(i);					
				blogNames[validBlogCtr] = curHash.get("blogName").toString();
				accountUsers[validBlogCtr] = curHash.get("username").toString();
				accountIDs[validBlogCtr] = curHash.get("id").toString();
				String url = curHash.get("url").toString();
				url = url.replace("http://", "");
				url = url.replace("https://", "");
				String[] urlSplit = url.split("/");
				url = urlSplit[0];
				url = "http://gravatar.com/blavatar/"
						+ moderateCommentsTab.getMd5Hash(url.trim())
						+ "?s=60&d=404";
				blavatars[validBlogCtr] = url;
				accountNames.add(validBlogCtr, blogNames[i]);
				validBlogCtr++;
			}
			
			if (validBlogCtr < accounts.size()){
				accounts = settingsDB.getAccounts(this);
			}

			ThumbnailBus bus = new ThumbnailBus();
			thumbs = new ThumbnailAdapter(this, new HomeListAdapter(this),
					new SimpleWebImageCache<ThumbnailBus, ThumbnailMessage>(
							null, null, 101, bus), IMAGE_IDS);

			setListAdapter(thumbs);
			
			listView.setOnItemClickListener(new OnItemClickListener() {
				public void onItemClick(AdapterView<?> arg0, View row, int positionParam, long id) {
					final int position = positionParam;
					
					AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(AddQuickPressShortcut.this);
					dialogBuilder.setTitle("Set shortcut name");
					
					final EditText quickPressShortcutName = new EditText(AddQuickPressShortcut.this);
					quickPressShortcutName.setText("QP "+ escapeUtils.unescapeHtml(accountNames.get(position)));
					dialogBuilder.setView(quickPressShortcutName);
					
					dialogBuilder.setPositiveButton(R.string.add, new DialogInterface.OnClickListener() {
						
						public void onClick(DialogInterface dialog, int which) {
							if (TextUtils.isEmpty(quickPressShortcutName.getText())) {
							    Toast t = Toast.makeText(AddQuickPressShortcut.this, R.string.quickpress_add_error, Toast.LENGTH_LONG);
							    t.show();
							} else {
								Intent shortcutIntent = new Intent();
				        		shortcutIntent.setClassName(editPost.class.getPackage().getName(), editPost.class.getName());
				        		shortcutIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				        		shortcutIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
				        		shortcutIntent.putExtra("id", accountIDs[position]);
				        		shortcutIntent.putExtra("accountName", escapeUtils.unescapeHtml(accountNames.get(position)));
				        		shortcutIntent.putExtra("isNew", true);
				        		
				        		Intent addIntent = new Intent();
				        		addIntent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent);
				        		addIntent.putExtra(Intent.EXTRA_SHORTCUT_NAME, quickPressShortcutName.getText().toString());
				        		addIntent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, Intent.ShortcutIconResource.fromContext(AddQuickPressShortcut.this, R.drawable.app_icon));
				        		
				        		WordPressDB wpDB = new WordPressDB(AddQuickPressShortcut.this);
				        		wpDB.addQuickPressShortcut(AddQuickPressShortcut.this.getApplicationContext(), accountIDs[position], quickPressShortcutName.getText().toString());
				        		
				        		addIntent.setAction("com.android.launcher.action.INSTALL_SHORTCUT");
				        		AddQuickPressShortcut.this.sendBroadcast(addIntent);
							    finish();
							}
						}
					});
					dialogBuilder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
						// just let the dialog close
						public void onClick(DialogInterface dialog, int which) {}
					});
					
					dialogBuilder.setCancelable(false);
					dialogBuilder.create().show();
				}
			});
			
		} else {
			// no account, load new account view
			Intent i = new Intent(AddQuickPressShortcut.this, newAccount.class);

			startActivityForResult(i, 0);
			
			finish();

		}
	}
	
	protected void onPause() {
		super.onPause();
		finish();
	}
}