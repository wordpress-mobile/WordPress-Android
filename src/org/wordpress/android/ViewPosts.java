package org.wordpress.android;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.Vector;

import org.wordpress.android.R;
import org.wordpress.android.models.Blog;
import org.wordpress.android.models.Post;
import org.wordpress.android.util.EscapeUtils;
import org.wordpress.android.util.StringHelper;
import org.xmlpull.v1.XmlPullParser;
import org.xmlrpc.android.ApiHelper;
import org.xmlrpc.android.XMLRPCClient;
import org.xmlrpc.android.XMLRPCException;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Looper;
import android.text.format.DateUtils;
import android.util.Log;
import android.util.Xml;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnCreateContextMenuListener;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.LayoutAnimationController;
import android.view.animation.TranslateAnimation;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewSwitcher;

public class ViewPosts extends ListActivity {
    /** Called when the activity is first created. */
    private XMLRPCClient client;
    private String[] postIDs, titles, dateCreated, dateCreatedFormatted,
            draftIDs, draftTitles, publish;
    private Integer[] uploaded;
    private String id = "", accountName = "";
    int rowID = 0;
    long selectedID;
    private int ID_DIALOG_DELETING = 1, ID_DIALOG_POSTING = 2;
    public boolean inDrafts = false;
    public String imgHTML, sImagePlacement = "", sMaxImageWidth = "";
    public boolean centerThumbnail = false;
    public Vector<String> imageUrl = new Vector<String>();
    public String imageTitle = null;
    public boolean thumbnailOnly, secondPass, xmlrpcError = false;
    public String submitResult = "", mediaErrorMsg = "";
    public ProgressDialog loadingDialog;
    public int totalDrafts = 0;
    public boolean isPage = false, vpUpgrade = false;
    boolean largeScreen = false;
    public int numRecords = 20;
    public ViewSwitcher switcher;
    private PostListAdapter pla;
    private Blog blog;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setContentView(R.layout.viewposts);

        Bundle extras = getIntent().getExtras();
        String action = null;
        if (extras != null) {
            id = extras.getString("id");
            blog = new Blog(id, this);
            accountName = extras.getString("accountName");
            isPage = extras.getBoolean("viewPages");
            action = extras.getString("action");
        }

        createSwitcher();

        // user came from action intent
        if (action != null && !isPage) {
            if (action.equals("upload")) {
                selectedID = extras.getInt("uploadID");
                showDialog(ID_DIALOG_POSTING);

                try {
                    submitResult = submitPost();

                } catch (IOException e) {
                    e.printStackTrace();
                }

            } else {

                boolean loadedPosts = loadPosts(false);
                if (!loadedPosts) {
                    refreshPosts(false);
                }
            }
        } else {

            // query for posts and refresh view
            boolean loadedPosts = loadPosts(false);

            if (!loadedPosts) {
                refreshPosts(false);
            }
        }

        Display display = getWindowManager().getDefaultDisplay();
        int width = display.getWidth();
        int height = display.getHeight();
        if (width > 480 || height > 480) {
            largeScreen = true;
        }

        final ImageButton addNewPost = (ImageButton) findViewById(R.id.newPost);

        addNewPost.setOnClickListener(new ImageButton.OnClickListener() {
            public void onClick(View v) {

                Intent i = new Intent(ViewPosts.this, EditPost.class);
                i.putExtra("accountName", accountName);
                i.putExtra("id", id);
                i.putExtra("isNew", true);
                if (isPage) {
                    i.putExtra("isPage", true);
                }
                startActivityForResult(i, 0);

            }
        });

        final ImageButton refresh = (ImageButton) findViewById(R.id.refresh);

        refresh.setOnClickListener(new ImageButton.OnClickListener() {
            public void onClick(View v) {

                refreshPosts(false);

            }
        });

    }

    private void createSwitcher() {
        // add footer view
        if (!isPage) {
            // create the ViewSwitcher in the current context
            switcher = new ViewSwitcher(this);
            Button footer = (Button) View.inflate(this,
                    R.layout.list_footer_btn, null);
            footer.setText(getResources().getText(R.string.load_more) + " "
                    + getResources().getText(R.string.tab_posts));

            footer.setOnClickListener(new Button.OnClickListener() {
                public void onClick(View v) {
                    // first view is showing, show the second progress view
                    switcher.showNext();
                    // get 30 more posts
                    numRecords += 30;
                    refreshPosts(true);
                }
            });

            View progress = View.inflate(this, R.layout.list_footer_progress,
                    null);

            switcher.addView(footer);
            switcher.addView(progress);
        }

    }

    public void refreshPosts(final boolean loadMore) {

        if (!loadMore) {
            showProgressBar();
        }
        Vector<Object> apiArgs = new Vector<Object>();
        apiArgs.add(blog);
        apiArgs.add(isPage);
        apiArgs.add(ViewPosts.this);
        apiArgs.add(numRecords);
        apiArgs.add(loadMore);
        new ApiHelper.getRecentPostsTask().execute(apiArgs);
    }

    public Map<String, ?> createItem(String title, String caption) {
        Map<String, String> item = new HashMap<String, String>();
        item.put("title", title);
        item.put("caption", caption);
        return item;
    }

    public boolean loadPosts(boolean loadMore) { // loads posts from the db

        WordPressDB postStoreDB = new WordPressDB(this);
        Vector<?> loadedPosts;
        if (isPage) {
            loadedPosts = postStoreDB.loadUploadedPosts(ViewPosts.this, id,
                    true);
        } else {
            loadedPosts = postStoreDB.loadUploadedPosts(ViewPosts.this, id,
                    false);
        }

        if (loadedPosts != null) {
            titles = new String[loadedPosts.size()];
            postIDs = new String[loadedPosts.size()];
            dateCreated = new String[loadedPosts.size()];
            dateCreatedFormatted = new String[loadedPosts.size()];
        } else {
            titles = new String[0];
            postIDs = new String[0];
            dateCreated = new String[0];
            dateCreatedFormatted = new String[0];
            if (pla != null) {
                pla.notifyDataSetChanged();
            }
        }
        if (loadedPosts != null) {
            for (int i = 0; i < loadedPosts.size(); i++) {
                HashMap<?, ?> contentHash = (HashMap<?, ?>) loadedPosts.get(i);
                titles[i] = EscapeUtils.unescapeHtml(contentHash.get("title")
                        .toString());

                postIDs[i] = contentHash.get("id").toString();
                dateCreated[i] = contentHash.get("date_created_gmt").toString();
                // dateCreatedFormatted[i] =
                // contentHash.get("postDateFormatted").toString();
                int flags = 0;
                flags |= android.text.format.DateUtils.FORMAT_SHOW_DATE;
                flags |= android.text.format.DateUtils.FORMAT_ABBREV_MONTH;
                flags |= android.text.format.DateUtils.FORMAT_SHOW_YEAR;
                flags |= android.text.format.DateUtils.FORMAT_SHOW_TIME;
                long localTime = (Long) contentHash.get("date_created_gmt")
                        + TimeZone.getDefault().getOffset(
                                (Long) contentHash.get("date_created_gmt"));
                dateCreatedFormatted[i] = DateUtils.formatDateTime(this,
                        localTime, flags);
            }

            // add the header
            List<String> postIDList = Arrays.asList(postIDs);
            List<String> newPostIDList = new ArrayList<String>();
            newPostIDList.add("postsHeader");
            newPostIDList.addAll(postIDList);
            postIDs = (String[]) newPostIDList.toArray(new String[newPostIDList
                    .size()]);

            List<String> postTitleList = Arrays.asList(titles);
            List<CharSequence> newPostTitleList = new ArrayList<CharSequence>();
            newPostTitleList.add(getResources().getText(
                    (isPage) ? R.string.tab_pages : R.string.tab_posts));
            newPostTitleList.addAll(postTitleList);
            titles = (String[]) newPostTitleList
                    .toArray(new String[newPostTitleList.size()]);

            List<String> dateList = Arrays.asList(dateCreated);
            List<String> newDateList = new ArrayList<String>();
            newDateList.add("postsHeader");
            newDateList.addAll(dateList);
            dateCreated = (String[]) newDateList.toArray(new String[newDateList
                    .size()]);

            List<String> dateFormattedList = Arrays
                    .asList(dateCreatedFormatted);
            List<String> newDateFormattedList = new ArrayList<String>();
            newDateFormattedList.add("postsHeader");
            newDateFormattedList.addAll(dateFormattedList);
            dateCreatedFormatted = (String[]) newDateFormattedList
                    .toArray(new String[newDateFormattedList.size()]);
        }
        // load drafts
        boolean drafts = loadDrafts();

        if (drafts) {

            List<String> draftIDList = Arrays.asList(draftIDs);
            List<String> newDraftIDList = new ArrayList<String>();
            newDraftIDList.add("draftsHeader");
            newDraftIDList.addAll(draftIDList);
            draftIDs = (String[]) newDraftIDList
                    .toArray(new String[newDraftIDList.size()]);

            List<String> titleList = Arrays.asList(draftTitles);
            List<CharSequence> newTitleList = new ArrayList<CharSequence>();
            newTitleList.add(getResources().getText(R.string.local_drafts));
            newTitleList.addAll(titleList);
            draftTitles = (String[]) newTitleList
                    .toArray(new String[newTitleList.size()]);

            List<String> publishList = Arrays.asList(publish);
            List<String> newPublishList = new ArrayList<String>();
            newPublishList.add("draftsHeader");
            newPublishList.addAll(publishList);
            publish = (String[]) newPublishList
                    .toArray(new String[newPublishList.size()]);

            postIDs = StringHelper.mergeStringArrays(draftIDs, postIDs);
            titles = StringHelper.mergeStringArrays(draftTitles, titles);
            dateCreatedFormatted = StringHelper.mergeStringArrays(publish,
                    dateCreatedFormatted);
        } else {
            if (pla != null) {
                pla.notifyDataSetChanged();
            }
        }

        if (loadedPosts != null || drafts == true) {
            ListView listView = (ListView) findViewById(android.R.id.list);

            if (!isPage) {
                listView.removeFooterView(switcher);
                if (loadedPosts != null) {
                    if (loadedPosts.size() >= 20) {
                        listView.addFooterView(switcher);
                    }
                }
            }

            if (loadMore) {
                pla.notifyDataSetChanged();
            } else {
                pla = new PostListAdapter(ViewPosts.this);
                listView.setAdapter(pla);

                listView.setOnItemClickListener(new OnItemClickListener() {

                    public void onItemClick(AdapterView<?> arg0, View arg1,
                            int arg2, long arg3) {
                        if (arg1 != null) {
                            arg1.performLongClick();
                        }

                    }

                });

                listView
                        .setOnCreateContextMenuListener(new OnCreateContextMenuListener() {

                            public void onCreateContextMenu(ContextMenu menu,
                                    View v, ContextMenuInfo menuInfo) {
                                AdapterView.AdapterContextMenuInfo info;
                                try {
                                    info = (AdapterView.AdapterContextMenuInfo) menuInfo;
                                } catch (ClassCastException e) {
                                    // Log.e(TAG, "bad menuInfo", e);
                                    return;
                                }

                                Object[] args = { R.id.row_post_id };

                                try {
                                    Method m = android.view.View.class
                                            .getMethod("getTag");
                                    m.invoke(selectedID, args);
                                } catch (NoSuchMethodException e) {
                                    selectedID = info.targetView.getId();
                                } catch (IllegalArgumentException e) {
                                    selectedID = info.targetView.getId();
                                } catch (IllegalAccessException e) {
                                    selectedID = info.targetView.getId();
                                } catch (InvocationTargetException e) {
                                    selectedID = info.targetView.getId();
                                }
                                // selectedID = (String)
                                // info.targetView.getTag(R.id.row_post_id);
                                rowID = info.position;

                                if (totalDrafts > 0 && rowID <= totalDrafts
                                        && rowID != 0) {
                                    menu.clear();
                                    menu.setHeaderTitle(getResources().getText(
                                            R.string.draft_actions));
                                    menu.add(1, 0, 0, getResources().getText(
                                            R.string.edit_draft));
                                    menu.add(1, 1, 0, getResources().getText(
                                            R.string.upload));
                                    menu.add(1, 2, 0, getResources().getText(
                                            R.string.delete_draft));
                                } else if (rowID == 1
                                        || ((rowID != (totalDrafts + 1)) && rowID != 0)) {
                                    menu.clear();

                                    if (isPage) {
                                        menu
                                                .setHeaderTitle(getResources()
                                                        .getText(
                                                                R.string.page_actions));
                                        menu
                                                .add(
                                                        2,
                                                        0,
                                                        0,
                                                        getResources()
                                                                .getText(
                                                                        R.string.preview_page));
                                        menu
                                                .add(
                                                        2,
                                                        1,
                                                        0,
                                                        getResources()
                                                                .getText(
                                                                        R.string.view_comments));
                                        menu.add(2, 2, 0, getResources()
                                                .getText(R.string.edit_page));
                                        menu.add(2, 3, 0, getResources()
                                                .getText(R.string.delete_page));
                                        menu.add(2, 4, 0, getResources()
                                                .getText(R.string.share_url));
                                    } else {
                                        menu
                                                .setHeaderTitle(getResources()
                                                        .getText(
                                                                R.string.post_actions));
                                        menu
                                                .add(
                                                        0,
                                                        0,
                                                        0,
                                                        getResources()
                                                                .getText(
                                                                        R.string.preview_post));
                                        menu
                                                .add(
                                                        0,
                                                        1,
                                                        0,
                                                        getResources()
                                                                .getText(
                                                                        R.string.view_comments));
                                        menu.add(0, 2, 0, getResources()
                                                .getText(R.string.edit_post));
                                        menu.add(0, 3, 0, getResources()
                                                .getText(R.string.delete_post));
                                        menu.add(0, 4, 0, getResources()
                                                .getText(R.string.share_url));
                                    }
                                }
                            }
                        });
            }
            return true;
        } else {
            return false;
        }

    }

    class ViewWrapper {
        View base;
        TextView title = null;
        TextView date = null;

        ViewWrapper(View base) {
            this.base = base;
        }

        TextView getTitle() {
            if (title == null) {
                title = (TextView) base.findViewById(R.id.title);
            }
            return (title);
        }

        TextView getDate() {
            if (date == null) {
                date = (TextView) base.findViewById(R.id.date);
            }
            return (date);
        }
    }

    private boolean loadDrafts() { // loads drafts from the db

        WordPressDB lDraftsDB = new WordPressDB(this);
        Vector<?> loadedPosts;
        if (isPage) {
            loadedPosts = lDraftsDB.loadDrafts(ViewPosts.this, id, true);
        } else {
            loadedPosts = lDraftsDB.loadDrafts(ViewPosts.this, id, false);
        }
        if (loadedPosts != null) {
            draftIDs = new String[loadedPosts.size()];
            draftTitles = new String[loadedPosts.size()];
            publish = new String[loadedPosts.size()];
            uploaded = new Integer[loadedPosts.size()];
            totalDrafts = loadedPosts.size();

            for (int i = 0; i < loadedPosts.size(); i++) {
                HashMap<?, ?> contentHash = (HashMap<?, ?>) loadedPosts.get(i);
                draftIDs[i] = contentHash.get("id").toString();
                draftTitles[i] = EscapeUtils.unescapeHtml(contentHash.get(
                        "title").toString());
                if (contentHash.get("status") != null) {
                    publish[i] = contentHash.get("status").toString();
                } else {
                    publish[i] = "";
                }
                uploaded[i] = (Integer) contentHash.get("uploaded");
            }

            return true;
        } else {
            totalDrafts = 0;
            return false;
        }
    }

    private class PostListAdapter extends BaseAdapter {

        public PostListAdapter(Context context) {
        }

        public int getCount() {
            return postIDs.length;
        }

        public Object getItem(int position) {
            return position;
        }

        public long getItemId(int position) {
            return position;
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            View pv = convertView;
            ViewWrapper wrapper = null;
            if (pv == null) {
                LayoutInflater inflater = getLayoutInflater();
                pv = inflater.inflate(R.layout.row_post_page, parent, false);
                wrapper = new ViewWrapper(pv);
                if (position == 0) {
                    // dateHeight = wrapper.getDate().getHeight();
                }
                pv.setTag(wrapper);
                wrapper = new ViewWrapper(pv);
                pv.setTag(wrapper);
            } else {
                wrapper = (ViewWrapper) pv.getTag();
            }

            String date = dateCreatedFormatted[position];
            if (date.equals("postsHeader") || date.equals("draftsHeader")) {

                pv.setBackgroundDrawable(getResources().getDrawable(
                        R.drawable.list_header_bg));

                wrapper.getTitle().setTextColor(Color.parseColor("#EEEEEE"));
                wrapper.getTitle().setShadowLayer(1, 1, 1,
                        Color.parseColor("#444444"));
                if (largeScreen) {
                    wrapper.getTitle().setPadding(12, 0, 12, 3);
                } else {
                    wrapper.getTitle().setPadding(8, 0, 8, 2);
                }
                wrapper.getTitle().setTextScaleX(1.2f);
                wrapper.getTitle().setTextSize(17);
                wrapper.getDate().setHeight(0);

                if (date.equals("draftsHeader")) {
                    inDrafts = true;
                    date = "";
                } else if (date.equals("postsHeader")) {
                    inDrafts = false;
                    date = "";
                }
            } else {
                pv.setBackgroundDrawable(getResources().getDrawable(
                        R.drawable.list_bg_selector));
                if (largeScreen) {
                    wrapper.getTitle().setPadding(12, 12, 12, 0);
                } else {
                    wrapper.getTitle().setPadding(8, 8, 8, 0);
                }
                wrapper.getTitle().setTextColor(Color.parseColor("#444444"));
                wrapper.getTitle().setShadowLayer(0, 0, 0,
                        Color.parseColor("#444444"));
                wrapper.getTitle().setTextScaleX(1.0f);
                wrapper.getTitle().setTextSize(16);
                wrapper.getDate().setTextColor(Color.parseColor("#888888"));

                Object[] args = { R.id.row_post_id, postIDs[position] };

                try {
                    Method m = android.view.View.class.getMethod("setTag");
                    m.invoke(pv, args);
                } catch (NoSuchMethodException e) {
                    pv.setId(Integer.valueOf(postIDs[position]));
                } catch (IllegalArgumentException e) {
                    pv.setId(Integer.valueOf(postIDs[position]));
                } catch (IllegalAccessException e) {
                    pv.setId(Integer.valueOf(postIDs[position]));
                } catch (InvocationTargetException e) {
                    pv.setId(Integer.valueOf(postIDs[position]));
                }

                // pv.setId(Integer.valueOf(postIDs[position]));
                // pv.setTag(R.id.row_post_id, postIDs[position]);

                if (wrapper.getDate().getHeight() == 0) {
                    wrapper.getDate().setHeight(
                            (int) wrapper.getTitle().getTextSize()
                                    + wrapper.getDate().getPaddingBottom());
                }
                String customDate = date;

                if (customDate.equals("draft")) {
                    customDate = getResources().getText(R.string.draft)
                            .toString();
                } else if (customDate.equals("pending")) {
                    customDate = getResources()
                            .getText(R.string.pending_review).toString();
                } else if (customDate.equals("private")) {
                    customDate = getResources().getText(R.string.post_private)
                            .toString();
                } else if (customDate.equals("publish")) {
                    customDate = getResources().getText(R.string.publish_post)
                            .toString();
                    wrapper.getDate().setTextColor(Color.parseColor("#006505"));
                }
                date = customDate;

            }
            wrapper.getTitle().setText(titles[position]);
            wrapper.getDate().setText(date);

            return pv;

        }

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            Bundle extras = data.getExtras();
            String returnResult = extras.getString("returnStatus");

            if (returnResult != null) {
                switch (requestCode) {
                case 0:
                    if (returnResult.equals("OK")) {
                        boolean uploadNow = false;
                        uploadNow = extras.getBoolean("upload");
                        uploadNow = true;
                        if (uploadNow) {
                            selectedID = extras.getLong("newID");
                            showDialog(ID_DIALOG_POSTING);

                            try {
                                submitResult = submitPost();

                            } catch (IOException e) {
                                e.printStackTrace();
                            }

                        } else {
                            loadPosts(false);
                        }
                    }
                    break;
                case 1:
                    if (returnResult.equals("OK")) {
                        refreshPosts(false);
                    }
                    break;
                }
            }
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {

        /* Switch on the ID of the item, to get what the user selected. */
        if (item.getGroupId() == 0) {
            switch (item.getItemId()) {
            case 0:
                Intent i0 = new Intent(ViewPosts.this, ViewPost.class);
                i0.putExtra("postID", selectedID);
                i0.putExtra("id", id);
                i0.putExtra("accountName", accountName);
                startActivity(i0);
                return true;
            case 1:
                Intent i = new Intent(ViewPosts.this, ViewPostComments.class);
                i.putExtra("postID", selectedID);
                i.putExtra("id", id);
                i.putExtra("accountName", accountName);
                startActivity(i);
                return true;
            case 2:
                Intent i2 = new Intent(ViewPosts.this, EditPost.class);
                i2.putExtra("postID", selectedID);
                i2.putExtra("id", id);
                i2.putExtra("accountName", accountName);
                startActivityForResult(i2, 0);
                return true;
            case 3:
                AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(
                        ViewPosts.this);
                dialogBuilder.setTitle(getResources().getText(
                        R.string.delete_post));
                dialogBuilder.setMessage(getResources().getText(
                        R.string.delete_sure_post)
                        + " '" + titles[rowID] + "'?");
                dialogBuilder.setPositiveButton(getResources().getText(
                        R.string.yes), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        showDialog(ID_DIALOG_DELETING);
                        new Thread() {
                            public void run() {
                                deletePost();
                            }
                        }.start();

                    }
                });
                dialogBuilder.setNegativeButton(getResources().getText(
                        R.string.no), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        // Just close the window.

                    }
                });
                dialogBuilder.setCancelable(true);
                if (!isFinishing()) {
                    dialogBuilder.create().show();
                }

                return true;
            case 4:
                loadingDialog = ProgressDialog.show(this, getResources()
                        .getText(R.string.share_url), getResources().getText(
                        R.string.attempting_fetch_url), true, false);
                Thread action = new Thread() {
                    public void run() {
                        Looper.prepare();
                        shareURL(id, String.valueOf(selectedID), false);
                        Looper.loop();
                    }
                };
                action.start();
                return true;
            }

        } else if (item.getGroupId() == 2) {
            switch (item.getItemId()) {
            case 0:
                Intent i0 = new Intent(ViewPosts.this, ViewPost.class);
                i0.putExtra("postID", String.valueOf(selectedID));
                i0.putExtra("id", id);
                i0.putExtra("accountName", accountName);
                i0.putExtra("isPage", true);
                startActivity(i0);
                return true;
            case 1:
                Intent i = new Intent(ViewPosts.this, ViewPostComments.class);
                i.putExtra("postID", String.valueOf(selectedID));
                i.putExtra("id", id);
                i.putExtra("accountName", accountName);
                startActivity(i);
                return true;
            case 2:
                Intent i2 = new Intent(ViewPosts.this, EditPost.class);
                i2.putExtra("postID", selectedID);
                i2.putExtra("id", id);
                i2.putExtra("accountName", accountName);
                i2.putExtra("isPage", true);
                startActivityForResult(i2, 0);
                return true;
            case 3:
                AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(
                        ViewPosts.this);
                dialogBuilder.setTitle(getResources().getText(
                        R.string.delete_page));
                dialogBuilder.setMessage(getResources().getText(
                        R.string.delete_sure_page)
                        + " '" + titles[rowID] + "'?");
                dialogBuilder.setPositiveButton(getResources().getText(
                        R.string.yes), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        showDialog(ID_DIALOG_DELETING);
                        new Thread() {
                            public void run() {
                                deletePost();
                            }
                        }.start();
                    }
                });
                dialogBuilder.setNegativeButton(getResources().getText(
                        R.string.no), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        // Just close the window.

                    }
                });
                dialogBuilder.setCancelable(true);
                if (!isFinishing()) {
                    dialogBuilder.create().show();
                }
                return true;
            case 4:
                loadingDialog = ProgressDialog.show(this, getResources()
                        .getText(R.string.share_url), getResources().getText(
                        R.string.attempting_fetch_url), true, false);
                Thread action = new Thread() {
                    public void run() {
                        Looper.prepare();
                        shareURL(id, String.valueOf(selectedID), true);
                        Looper.loop();
                    }
                };
                action.start();
                return true;
            }

        } else {
            switch (item.getItemId()) {
            case 0:
                Intent i2 = new Intent(ViewPosts.this, EditPost.class);
                i2.putExtra("postID", selectedID);
                i2.putExtra("id", id);
                if (isPage) {
                    i2.putExtra("isPage", true);
                }
                i2.putExtra("accountName", accountName);
                i2.putExtra("localDraft", true);
                startActivityForResult(i2, 0);
                return true;
            case 1:
                showDialog(ID_DIALOG_POSTING);

                new Thread() {
                    public void run() {

                        try {
                            submitResult = submitPost();

                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                    }
                }.start();
                return true;
            case 2:
                AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(
                        ViewPosts.this);
                dialogBuilder.setTitle(getResources().getText(
                        R.string.delete_draft));
                dialogBuilder.setMessage(getResources().getText(
                        R.string.delete_sure)
                        + " '" + titles[rowID] + "'?");
                dialogBuilder.setPositiveButton(getResources().getText(
                        R.string.yes), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {

                        Post post = new Post(id, selectedID, isPage,
                                ViewPosts.this);
                        post.delete();

                        loadPosts(false);

                    }
                });
                dialogBuilder.setNegativeButton(getResources().getText(
                        R.string.no), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        // Just close the window.

                    }
                });
                dialogBuilder.setCancelable(true);
                if (!isFinishing()) {
                    dialogBuilder.create().show();
                }

            }
        }

        return false;
    }

    private void deletePost() {

        String selPostID = String.valueOf(selectedID);
        client = new XMLRPCClient(blog.getUrl(), blog.getHttpuser(), blog
                .getHttppassword());

        Object[] postParams = { "", selPostID, blog.getUsername(),
                blog.getPassword() };
        Object[] pageParams = { blog.getBlogId(), blog.getUsername(),
                blog.getPassword(), selPostID };

        try {
            client.call((isPage) ? "wp.deletePage" : "blogger.deletePost",
                    (isPage) ? pageParams : postParams);
            dismissDialog(ID_DIALOG_DELETING);
            Thread action = new Thread() {
                public void run() {
                    Toast.makeText(
                            ViewPosts.this,
                            getResources().getText(
                                    (isPage) ? R.string.page_deleted
                                            : R.string.post_deleted),
                            Toast.LENGTH_SHORT).show();
                }
            };
            this.runOnUiThread(action);
            Thread action2 = new Thread() {
                public void run() {
                    refreshPosts(false);
                }
            };
            this.runOnUiThread(action2);

        } catch (final XMLRPCException e) {
            dismissDialog(ID_DIALOG_DELETING);
            Thread action3 = new Thread() {
                public void run() {
                    AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(
                            ViewPosts.this);
                    dialogBuilder.setTitle(getResources().getText(
                            R.string.connection_error));
                    dialogBuilder.setMessage(e.getLocalizedMessage());
                    dialogBuilder.setPositiveButton("OK",
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog,
                                        int whichButton) {
                                    // Just close the window.

                                }
                            });
                    dialogBuilder.setCancelable(true);
                    if (!isFinishing()) {
                        dialogBuilder.create().show();
                    }
                }
            };
            this.runOnUiThread(action3);
        }

    }

    @Override
    protected Dialog onCreateDialog(int id) {
        if (id == ID_DIALOG_POSTING) {
            loadingDialog = new ProgressDialog(this);
            loadingDialog.setTitle(getResources().getText(
                    R.string.uploading_content));
            loadingDialog.setMessage(getResources().getText(
                    (isPage) ? R.string.page_attempt_upload
                            : R.string.post_attempt_upload));
            loadingDialog.setCancelable(false);
            return loadingDialog;
        } else if (id == ID_DIALOG_DELETING) {
            loadingDialog = new ProgressDialog(this);
            loadingDialog.setTitle(getResources().getText(
                    (isPage) ? R.string.delete_page : R.string.delete_post));
            loadingDialog.setMessage(getResources().getText(
                    (isPage) ? R.string.attempt_delete_page
                            : R.string.attempt_delete_post));
            loadingDialog.setCancelable(false);
            return loadingDialog;
        }

        return super.onCreateDialog(id);
    }

    public String submitPost() throws IOException {

        Post post = new Post(id, selectedID, isPage, ViewPosts.this);

        post.upload();

        return "";
    }

    public void showProgressBar() {
        AnimationSet set = new AnimationSet(true);

        Animation animation = new AlphaAnimation(0.0f, 1.0f);
        animation.setDuration(500);
        set.addAnimation(animation);

        animation = new TranslateAnimation(Animation.RELATIVE_TO_SELF, 0.0f,
                Animation.RELATIVE_TO_SELF, 0.0f, Animation.RELATIVE_TO_SELF,
                -1.0f, Animation.RELATIVE_TO_SELF, 0.0f);
        animation.setDuration(500);
        set.addAnimation(animation);

        LayoutAnimationController controller = new LayoutAnimationController(
                set, 0.5f);
        RelativeLayout loading = (RelativeLayout) findViewById(R.id.loading);
        loading.setVisibility(View.VISIBLE);
        loading.setLayoutAnimation(controller);
    }

    public void closeProgressBar() {

        AnimationSet set = new AnimationSet(true);

        Animation animation = new AlphaAnimation(0.0f, 1.0f);
        animation.setDuration(500);
        set.addAnimation(animation);

        animation = new TranslateAnimation(Animation.RELATIVE_TO_SELF, 0.0f,
                Animation.RELATIVE_TO_SELF, 0.0f, Animation.RELATIVE_TO_SELF,
                0.0f, Animation.RELATIVE_TO_SELF, -1.0f);
        animation.setDuration(500);
        set.addAnimation(animation);

        LayoutAnimationController controller = new LayoutAnimationController(
                set, 0.5f);
        RelativeLayout loading = (RelativeLayout) findViewById(R.id.loading);

        loading.setLayoutAnimation(controller);

        loading.setVisibility(View.INVISIBLE);
    }

    private void shareURL(String accountId, String postId, final boolean isPage) {

        String errorStr = null;

        client = new XMLRPCClient(blog.getUrl(), blog.getHttpuser(), blog
                .getHttppassword());

        Object versionResult = new Object();
        try {
            if (isPage) {
                Object[] vParams = { blog.getBlogId(), postId,
                        blog.getUsername(), blog.getPassword() };
                versionResult = (Object) client.call("wp.getPage", vParams);
            } else {
                Object[] vParams = { postId, blog.getUsername(),
                        blog.getPassword() };
                versionResult = (Object) client.call("metaWeblog.getPost",
                        vParams);
            }
        } catch (XMLRPCException e) {
            errorStr = e.getMessage();
            Log.d("WP", "Error", e);
        }

        if (errorStr == null && versionResult != null) {
            try {
                HashMap<?, ?> contentHash = (HashMap<?, ?>) versionResult;

                if ((isPage && !"publish".equals(contentHash.get("page_status")
                        .toString()))
                        || (!isPage && !"publish".equals(contentHash.get(
                                "post_status").toString()))) {
                    Thread prompt = new Thread() {
                        public void run() {
                            AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(
                                    ViewPosts.this);
                            dialogBuilder.setTitle(getResources().getText(
                                    R.string.share_url));
                            if (isPage) {
                                dialogBuilder.setMessage(ViewPosts.this
                                        .getResources().getText(
                                                R.string.page_not_published));
                            } else {
                                dialogBuilder.setMessage(ViewPosts.this
                                        .getResources().getText(
                                                R.string.post_not_published));
                            }
                            dialogBuilder.setPositiveButton("OK",
                                    new DialogInterface.OnClickListener() {
                                        public void onClick(
                                                DialogInterface dialog,
                                                int whichButton) {
                                        }
                                    });
                            dialogBuilder.setCancelable(true);
                            dialogBuilder.create().show();
                        }
                    };
                    this.runOnUiThread(prompt);
                } else {
                    String postURL = contentHash.get("permaLink").toString();
                    String shortlink = getShortlinkTagHref(postURL);
                    Intent share = new Intent(Intent.ACTION_SEND);
                    share.setType("text/plain");
                    if (shortlink == null) {
                        share.putExtra(Intent.EXTRA_TEXT, postURL);
                    } else {
                        share.putExtra(Intent.EXTRA_TEXT, shortlink);
                    }
                    share.putExtra(Intent.EXTRA_SUBJECT, contentHash.get(
                            "title").toString());
                    startActivity(Intent.createChooser(share, this
                            .getText(R.string.share_url)));
                }
            } catch (Exception e) {
                errorStr = e.getMessage();
                Log.d("WP", "Error", e);
            }
        }

        loadingDialog.dismiss();
        if (errorStr != null) {
            final String fErrorStr = errorStr;
            Thread prompt = new Thread() {
                public void run() {
                    AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(
                            ViewPosts.this);
                    dialogBuilder.setTitle(getResources().getText(
                            R.string.connection_error));
                    dialogBuilder.setMessage(fErrorStr);
                    dialogBuilder.setPositiveButton("OK",
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog,
                                        int whichButton) {
                                }
                            });
                    dialogBuilder.setCancelable(true);
                    dialogBuilder.create().show();
                }
            };
            this.runOnUiThread(prompt);
        }
    }

    private String getShortlinkTagHref(String urlString) {
        InputStream in = getResponse(urlString);

        if (in != null) {
            XmlPullParser parser = Xml.newPullParser();
            try {
                // auto-detect the encoding from the stream
                parser.setInput(in, null);
                int eventType = parser.getEventType();
                while (eventType != XmlPullParser.END_DOCUMENT) {
                    String name = null;
                    String rel = "";
                    String href = "";
                    switch (eventType) {
                    case XmlPullParser.START_TAG:
                        name = parser.getName();
                        if (name.equalsIgnoreCase("link")) {
                            for (int i = 0; i < parser.getAttributeCount(); i++) {
                                String attrName = parser.getAttributeName(i);
                                String attrValue = parser.getAttributeValue(i);
                                if (attrName.equals("rel")) {
                                    rel = attrValue;
                                } else if (attrName.equals("href")) {
                                    href = attrValue;
                                }
                            }

                            if (rel.equals("shortlink")) {
                                return href;
                            }
                        }
                        break;
                    }
                    eventType = parser.next();
                }
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }

        }
        return null; // never found the shortlink tag
    }

    private InputStream getResponse(String urlString) {
        InputStream in = null;
        int response = -1;

        URL url = null;
        try {
            url = new URL(urlString);
        } catch (MalformedURLException e1) {
            e1.printStackTrace();
            return null;
        }
        URLConnection conn = null;
        try {
            conn = url.openConnection();
        } catch (IOException e1) {
            e1.printStackTrace();
            return null;
        }

        try {
            HttpURLConnection httpConn = (HttpURLConnection) conn;
            httpConn.setAllowUserInteraction(false);
            httpConn.setInstanceFollowRedirects(true);
            httpConn.setRequestMethod("GET");
            httpConn.addRequestProperty("user-agent", "Mozilla/5.0");
            httpConn.connect();

            response = httpConn.getResponseCode();
            if (response == HttpURLConnection.HTTP_OK) {
                in = httpConn.getInputStream();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
        return in;
    }

    public void uploadCompleted() {
        this.dismissDialog(ID_DIALOG_POSTING);
        this.refreshPosts(false);
    }
    
    public void uploadFailed(String error) {
        this.dismissDialog(ID_DIALOG_POSTING);
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(
                ViewPosts.this);
        dialogBuilder.setTitle(getResources().getText(
                R.string.connection_error));
        dialogBuilder.setMessage(error);
        dialogBuilder.setPositiveButton("OK",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog,
                            int whichButton) {
                        loadPosts(false);
                    }
                });
        dialogBuilder.setCancelable(true);
        dialogBuilder.create().show();
    }

}
