package org.wordpress.android.ui.mysite.quickstart

import android.view.ViewGroup
import org.wordpress.android.databinding.QuickStartBlockBinding
import org.wordpress.android.ui.mysite.MySiteItemViewHolder
import org.wordpress.android.util.viewBinding

class QuickStartBlockViewHolder(
    parent: ViewGroup
) : MySiteItemViewHolder<QuickStartBlockBinding>(parent.viewBinding(QuickStartBlockBinding::inflate))
