package org.wordpress.android.ui;

import java.util.ArrayList;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.wordpress.android.R;
import org.wordpress.android.util.Utils;

/**
 * A view that mimics the action bar tabs. It can be placed anywhere and appears 
 * under the sliding menu, unlike the action bar tabs 
 */

public class HorizontalTabView extends HorizontalScrollView implements OnClickListener {

    private static final String TAG_PREFIX = "tab:";
    private ArrayList<Tab> mTabs;
    private ArrayList<TextView> mTextViews;
    private LinearLayout mTabContainer;
    private float mMaxTabWidth = 0f;
    private TabListener mTabListener;
    private boolean mEnableScroll = true;
    private LinearLayout mSelectedLayout;
    
    public HorizontalTabView(Context context) {
        super(context);
        init();
    }
    public HorizontalTabView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }
    public HorizontalTabView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }
    
    private void init() {
        mTabs = new ArrayList<Tab>();
        mTextViews = new ArrayList<TextView>();
        
        setupBackground();
        setupTabContainer();
    }
    
    private void setupBackground() {
        setBackgroundColor(getResources().getColor(R.color.tab_background));
    }
    
    private void setupTabContainer() {
        mTabContainer = new LinearLayout(getContext());
        HorizontalScrollView.LayoutParams linearLayoutParams =
                new HorizontalScrollView.LayoutParams(HorizontalScrollView.LayoutParams.WRAP_CONTENT, HorizontalScrollView.LayoutParams.WRAP_CONTENT);
        mTabContainer.setLayoutParams(linearLayoutParams);
        mTabContainer.setOrientation(LinearLayout.HORIZONTAL);
        
        addView(mTabContainer);
        
    }
    public Tab newTab() {
        return new Tab();
    }
    
    public void addTab(Tab tab) {
        tab.setPosition(mTabs.size());
        mTabs.add(tab);
        
        int divWidth = (int) Utils.dpToPx(1);
        int divTopMargin = (int) Utils.dpToPx(12);
        int divHeight = (int) Utils.dpToPx(24);
        
        int tabPad = (int) Utils.dpToPx(16);
        
        int fontSizeSp = 12;
        
        // add dividers in the middle - not using divider property as it is API 11
        if (mTextViews.size() > 0) {
            View divider = new View(getContext());
            LinearLayout.LayoutParams separatorParams = new LinearLayout.LayoutParams(divWidth, divHeight);
            separatorParams.topMargin = divTopMargin;
            divider.setLayoutParams(separatorParams);
            divider.setBackgroundColor(getResources().getColor(R.color.tab_divider));
            mTabContainer.addView(divider);
        }
        
        TextView textView = new TextView(getContext());
        LinearLayout.LayoutParams textViewParams =
                new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        textView.setLayoutParams(textViewParams);
        textView.setGravity(Gravity.CENTER);
        textView.setText(tab.getText());
        textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, fontSizeSp);
        textView.setTextColor(getResources().getColor(R.color.tab_text));
        textView.setTypeface(null, Typeface.BOLD);
        
        mTextViews.add(textView);

        LinearLayout linearLayout = new LinearLayout(getContext());
        LinearLayout.LayoutParams linearLayoutParams =
                new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1);
        linearLayout.setLayoutParams(linearLayoutParams);
        linearLayout.addView(textView);
        linearLayout.setTag(TAG_PREFIX + (mTabs.size()-1));
        linearLayout.setOnClickListener(this);
        linearLayout.setBackgroundResource(R.drawable.tab_indicator_ab_wordpress);
        linearLayout.setPadding(tabPad, tabPad, tabPad, tabPad);
        
        mTabContainer.addView(linearLayout);
        
        recomputeTabWidths();
    }

    /** Make the tabs have the same widths, where this width is based on the longest tab title **/ 
    private void recomputeTabWidths() {
        
        // Determine the max width
        for(TextView textView : mTextViews) {
            Paint paint = textView.getPaint();
            float width = paint.measureText(textView.getText().toString());
            if (mMaxTabWidth < width)
                mMaxTabWidth = width;
        }
        
        // Set the tabs to use the max width
        for(TextView textView : mTextViews) {
            LinearLayout.LayoutParams textViewParams =
                    new LinearLayout.LayoutParams((int) mMaxTabWidth, LinearLayout.LayoutParams.WRAP_CONTENT, Gravity.CENTER);
            textView.setLayoutParams(textViewParams);
        }
        
    }

    public class Tab {
        
        private String mText;
        private int mPosition;
        
        @SuppressLint("DefaultLocale")
		public CharSequence getText() {
            return mText.toUpperCase();
        }

        public Tab setText(CharSequence pageTitle) {
            mText = pageTitle.toString();
            return this;
        }

        public int getPosition() {
            return mPosition;
        }

        public void setPosition(int position) {
            this.mPosition = position;
        }
    }
    
    public interface TabListener {
        public void onTabSelected(Tab tab);
    }
    
    public TabListener getTabListener() {
        return mTabListener;
    }
    
    public void setTabListener(TabListener tabListener) {
        this.mTabListener = tabListener;
    }
    
    @Override
    public void onClick(View v) {
        if (v instanceof LinearLayout) {
            LinearLayout layout = (LinearLayout) v;
            
            String tag = (String) layout.getTag();
            int position = Integer.valueOf(tag.substring(TAG_PREFIX.length()));

            // It is necessary to disable scrolling upon click before informing the listener
            // because I've found that if setSelectedTab() was called in the listener implementation,
            // and that setSelectedTab() is called again after onTabSelected(), it does not smooth scroll.
            
            // The call to setSelectedTab() in this method is necessary because if there was no listener or if 
            // it did not call setSelectedTab(), then it would appear as if nothing happened.
            
            mEnableScroll = false;
            
            if (mTabListener != null)
                mTabListener.onTabSelected(mTabs.get(position));
            
            mEnableScroll = true;

            setSelectedTab(position);
        }
    }

    public void setSelectedTab(int position) {
        if (position >= mTextViews.size())
            return;

        if (mEnableScroll) {
            scrollToTab(position);
            setSelectedLayout(getTabParent(position));
        }
    }
    
    public void setTabText(int position, String text) {
        mTabs.get(position).mText = text;
        mTextViews.get(position).setText(text);
    }
    
    private void scrollToTab(int position) {
        int tabWidth = getTabParent(position).getWidth();
        int parentWidth = ((View) this.getParent()).getWidth();
        
        int offset = parentWidth / 2 - tabWidth / 2;
        
        smoothScrollTo(tabWidth * position - offset, 0);
    }
    
    private LinearLayout getTabParent(int position) {
        View tab = mTextViews.get(position);
        return (LinearLayout) tab.getParent();
    }
    
    private void setSelectedLayout(LinearLayout layout) {
        if (mSelectedLayout != null) {
            mSelectedLayout.setSelected(false);
        }

        mSelectedLayout = (LinearLayout)layout;
        mSelectedLayout.setSelected(true);
    }
    
}