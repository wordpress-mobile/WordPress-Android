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
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.ui.ActivityLauncher
import org.wordpress.android.ui.JetpackConnectionSource
import org.wordpress.android.ui.JetpackConnectionWebViewActivity
import org.wordpress.android.ui.jetpackrestconnection.JetpackRestConnectionActivity
import org.wordpress.android.ui.jetpackrestconnection.JetpackRestConnectionViewModel
import javax.inject.Inject

@AndroidEntryPoint
class XmlRpcDisabledBottomSheetFragment : BottomSheetDialogFragment() {
    @Inject
    lateinit var siteStore: SiteStore

    @Inject
    lateinit var accountStore: AccountStore

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
        val siteLocalId = arguments?.getInt(ARG_SITE_LOCAL_ID) ?: run {
            dismiss()
            return
        }
        val site = siteStore.getSiteByLocalId(siteLocalId) ?: run {
            dismiss()
            return
        }

        binding.connectJetpackButton.setOnClickListener {
            if (!startJetpackRestConnectionFlow(site)) {
                JetpackConnectionWebViewActivity
                    .startJetpackConnectionFlow(
                        requireActivity(),
                        JetpackConnectionSource.XMLRPC_DISABLED,
                        site,
                        accountStore.hasAccessToken()
                    )
            }
            dismiss()
        }

        binding.learnMoreButton.setOnClickListener {
            ActivityLauncher.openUrlExternal(
                requireActivity(),
                LEARN_MORE_URL
            )
        }
    }

    private fun startJetpackRestConnectionFlow(
        site: SiteModel
    ): Boolean {
        if (JetpackRestConnectionViewModel
                .canInitiateJetpackRestConnection(site)
        ) {
            JetpackRestConnectionActivity
                .startJetpackRestConnectionFlow(
                    requireActivity(),
                    JetpackRestConnectionViewModel
                        .ConnectionSource.XMLRPC_DISABLED
                )
            return true
        }
        return false
    }

    companion object {
        const val TAG = "XmlRpcDisabledBottomSheetFragment"
        private const val ARG_SITE_LOCAL_ID = "arg_site_local_id"
        private const val LEARN_MORE_URL =
            "https://apps.wordpress.com/support/mobile/" +
                "login-signup/" +
                "inaccessible-xml-rpc-connection-error/"

        fun newInstance(siteLocalId: Int): XmlRpcDisabledBottomSheetFragment {
            return XmlRpcDisabledBottomSheetFragment().apply {
                arguments = Bundle().apply {
                    putInt(ARG_SITE_LOCAL_ID, siteLocalId)
                }
            }
        }
    }
}
