package org.wordpress.android.ui.plans;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import org.wordpress.android.R;
import org.wordpress.android.util.ToastUtils;

/**
 * single page within the post-purchase activity's ViewPager
 */
public class PlanPostPurchaseFragment extends Fragment {

    static PlanPostPurchaseFragment newInstance() {
        PlanPostPurchaseFragment fragment = new PlanPostPurchaseFragment();
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.plan_post_purchase_fragment, container, false);

        ImageView image = (ImageView) rootView.findViewById(R.id.image);
        TextView txtTitle = (TextView) rootView.findViewById(R.id.text_title);
        TextView txtDescription = (TextView) rootView.findViewById(R.id.text_description);
        Button button = (Button) rootView.findViewById(R.id.button);

        // TODO: these are placeholders until actual copy exists
        image.setImageResource(R.drawable.plan_business);
        txtTitle.setText("This is the title");
        txtDescription.setText("This is the description. It is a nice description and deserves to be respected.");
        button.setText("Don\'t Press This");
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ToastUtils.showToast(v.getContext(), "You never listen");
            }
        });

        return rootView;
    }
}
