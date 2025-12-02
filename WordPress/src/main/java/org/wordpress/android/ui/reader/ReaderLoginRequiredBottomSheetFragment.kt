package org.wordpress.android.ui.reader

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import org.wordpress.android.R
import org.wordpress.android.ui.ActivityLauncher
import org.wordpress.android.widgets.WPTextView

/**
 * A bottom sheet that prompts the user to log in to WordPress.com.
 * Used when a logged-out user tries to perform actions that require authentication,
 * such as subscribing to a blog.
 *
 * This fragment reuses the same layout as [SubfilterPageFragment] to maintain visual consistency
 * with the empty state shown in the subfilter bottom sheet.
 */
class ReaderLoginRequiredBottomSheetFragment : BottomSheetDialogFragment() {
    companion object {
        const val TAG = "ReaderLoginRequiredBottomSheetFragment"

        fun newInstance(): ReaderLoginRequiredBottomSheetFragment {
            return ReaderLoginRequiredBottomSheetFragment()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.subfilter_page_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Hide the recycler view since we only want to show the empty state
        view.findViewById<RecyclerView>(R.id.content_recycler_view).visibility = View.GONE

        // Show and configure the empty state container
        val emptyStateContainer = view.findViewById<LinearLayout>(R.id.empty_state_container)
        emptyStateContainer.visibility = View.VISIBLE

        // Hide title (matching the logged-out empty state behavior)
        view.findViewById<WPTextView>(R.id.title).visibility = View.GONE

        // Set the text for logged-out users
        view.findViewById<WPTextView>(R.id.text).setText(
            R.string.reader_filter_self_hosted_empty_blogs_list
        )

        // Configure primary button for login
        val primaryButton = view.findViewById<Button>(R.id.action_button_primary)
        primaryButton.visibility = View.VISIBLE
        primaryButton.setText(R.string.reader_filter_self_hosted_empty_sites_tags_action)
        primaryButton.setOnClickListener {
            dismiss()
            ActivityLauncher.showMainActivityAndMeScreen(requireContext())
        }

        // Hide secondary button (not needed for login-only scenario)
        view.findViewById<Button>(R.id.action_button_secondary).visibility = View.GONE
    }
}
