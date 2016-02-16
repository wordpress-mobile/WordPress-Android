package org.wordpress.android.ui.plans;

import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.TextView;

import org.wordpress.android.R;
import org.wordpress.android.widgets.WPViewPager;

/**
 * post-purchase "on-boarding" experience
 */
public class PlanPostPurchaseActivity extends AppCompatActivity {

    private ViewPager mViewPager;
    private TextView mTxtSkip;
    private TextView mTxtNext;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.plan_post_purchase_activity);

        mViewPager = (WPViewPager) findViewById(R.id.viewpager);
        mTxtSkip = (TextView) findViewById(R.id.text_skip);
        mTxtNext = (TextView) findViewById(R.id.text_next);

        mTxtSkip.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        mTxtNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                gotoNextPage();
            }
        });
    }

    private void gotoNextPage() {

    }

    private void gotoPreviousPage() {

    }
}
