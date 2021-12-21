package org.wordpress.android.ui.mysite.cards.dashboard

import androidx.viewbinding.ViewBinding
import org.wordpress.android.ui.mysite.MySiteCardAndItemViewHolder

open class CardViewHolder<T : ViewBinding>(override val binding: T) : MySiteCardAndItemViewHolder<T>(binding)
