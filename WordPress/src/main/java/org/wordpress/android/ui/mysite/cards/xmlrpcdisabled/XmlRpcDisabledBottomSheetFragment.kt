package org.wordpress.android.ui.mysite.cards.xmlrpcdisabled

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dagger.hilt.android.AndroidEntryPoint
import com.google.android.material.R as MaterialR
import org.wordpress.android.databinding.XmlrpcDisabledBottomSheetBinding
import org.wordpress.android.ui.ActivityLauncher

@AndroidEntryPoint
class XmlRpcDisabledBottomSheetFragment : BottomSheetDialogFragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return XmlrpcDisabledBottomSheetBinding.inflate(
            inflater, container, false
        ).root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        dialog?.setOnShowListener { dialogInterface ->
            val bottomSheet =
                (dialogInterface as? BottomSheetDialog)
                    ?.findViewById<View>(
                        MaterialR.id.design_bottom_sheet
                    ) as? FrameLayout
            bottomSheet?.let {
                BottomSheetBehavior.from(it).state =
                    BottomSheetBehavior.STATE_EXPANDED
            }
        }
        val binding = XmlrpcDisabledBottomSheetBinding.bind(view)
        binding.learnMoreButton.setOnClickListener {
            ActivityLauncher.openUrlExternal(
                requireActivity(),
                LEARN_MORE_URL
            )
        }
    }

    companion object {
        const val TAG = "XmlRpcDisabledBottomSheetFragment"
        private const val LEARN_MORE_URL =
            "https://apps.wordpress.com/support/mobile/" +
                "login-signup/" +
                "inaccessible-xml-rpc-connection-error/"

        fun newInstance(): XmlRpcDisabledBottomSheetFragment {
            return XmlRpcDisabledBottomSheetFragment()
        }
    }
}
