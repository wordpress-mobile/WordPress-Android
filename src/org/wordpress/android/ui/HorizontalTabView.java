package org.wordpress.android.ui;

import java.util.ArrayList;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.wordpress.android.R;

public class HorizontalTabView extends HorizontalScrollView implements OnClickListener {

    private static final String TAG_PREFIX = "tab:";
    private ArrayList<Tab> mTabs;
    private ArrayList<TextView> mTextViews;
    private LinearLayout mTabContainer;
    private float mMaxTabWidth = 0f;
    private TabListener mTabListener;
    
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
                new HorizontalScrollView.LayoutParams(HorizontalScrollView.LayoutParams.MATCH_PARENT, HorizontalScrollView.LayoutParams.WRAP_CONTENT);
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
        
        int divWidth = (int) dpToPx(1);
        int divTopMargin = (int) dpToPx(12);
        int divHeight = (int) dpToPx(24);
        
        int tabPad = (int) dpToPx(16);
        
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


    private void recomputeTabWidths() {
        
        int tabPad = (int) dpToPx(16);
        int divWidth = (int) dpToPx(1);
        
        for(TextView textView : mTextViews) {
            Paint paint = textView.getPaint();
            float width = paint.measureText(textView.getText().toString());
            if (mMaxTabWidth < width)
                mMaxTabWidth = width;
        }
        
        int tabContainerWidth = 0;
        for(TextView textView : mTextViews) {
            LinearLayout.LayoutParams textViewParams =
                    new LinearLayout.LayoutParams((int) mMaxTabWidth, LinearLayout.LayoutParams.WRAP_CONTENT, Gravity.CENTER);
            textView.setLayoutParams(textViewParams);
            
            tabContainerWidth += mMaxTabWidth; // add text width
            tabContainerWidth += 2*tabPad; // add 2*tabPad for left and right padding
            tabContainerWidth += divWidth; // add divider width
        }
        
        mTabContainer.setLayoutParams(new HorizontalScrollView.LayoutParams(tabContainerWidth, HorizontalScrollView.LayoutParams.WRAP_CONTENT));
    }

    public class Tab {
        
        String mText;
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
            mTabListener.onTabSelected(mTabs.get(position));

            scrollToTab(layout, position);
            setSelectedLayout(layout);
        }
    }

    public void setSelectedTab(int position) {
        if (position >= mTextViews.size())
            return;
        
        View tab = mTextViews.get(position);
        LinearLayout parentView = (LinearLayout) tab.getParent();
        scrollToTab(parentView, position);
        setSelectedLayout(parentView);

    }
    
    private void scrollToTab(LinearLayout view, int position) {
        int tabWidth = view.getWidth();
        int parentWidth = ((View) this.getParent()).getWidth();
        int offset = parentWidth / 2 - tabWidth / 2;
        
        smoothScrollTo(tabWidth * position - offset, 0);
    }
    
    private void setSelectedLayout(LinearLayout layout) {
        if (mSelectedLayout != null) {
            mSelectedLayout.setSelected(false);
        }

        mSelectedLayout = (LinearLayout)layout;
        mSelectedLayout.setSelected(true);
    }
    
    public float dpToPx(int dp) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, getContext().getResources().getDisplayMetrics());
   }
    
    public float pxToDp(float px) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_PX, px, getContext().getResources().getDisplayMetrics());
    }
    
}
