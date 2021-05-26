package org.wordpress.android.ui.bloggingreminders

import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.CompoundButton.OnCheckedChangeListener
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.databinding.BloggingRemindersFragmentBinding
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.viewmodel.observeEvent
import org.wordpress.android.workers.reminder.ReminderConfig.ReminderType
import org.wordpress.android.workers.reminder.ReminderConfig.ReminderType.DAILY
import org.wordpress.android.workers.reminder.ReminderConfig.ReminderType.WEEKLY
import java.time.DayOfWeek
import java.time.DayOfWeek.FRIDAY
import java.time.DayOfWeek.MONDAY
import java.time.DayOfWeek.SATURDAY
import java.time.DayOfWeek.SUNDAY
import java.time.DayOfWeek.THURSDAY
import java.time.DayOfWeek.TUESDAY
import java.time.DayOfWeek.WEDNESDAY
import javax.inject.Inject

class BloggingRemindersFragment : Fragment(R.layout.blogging_reminders_fragment) {
    @Inject lateinit var viewModelFactory: ViewModelProvider.Factory
    private lateinit var viewModel: BloggingRemindersViewModel

    override fun onAttach(context: Context) {
        super.onAttach(context)
        (requireActivity().applicationContext as WordPress).component().inject(this)
        viewModel = ViewModelProvider(this, viewModelFactory).get(BloggingRemindersViewModel::class.java)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val site = activity?.intent?.getSerializableExtra(WordPress.SITE) as? SiteModel

        val adapter = BloggingRemindersAdapter { viewModel.cancel(it.workInfo.id) }
        with(BloggingRemindersFragmentBinding.bind(view)) {
            recyclerView.layoutManager = LinearLayoutManager(context)
            recyclerView.adapter = adapter
            fab.setOnClickListener { site?.let { viewModel.add(it.siteId) } }
            dailyButton.setOnCheckedChangeListener(getTypeCheckedListener(DAILY))
            weeklyButton.setOnCheckedChangeListener(getTypeCheckedListener(WEEKLY))
            mondayCheckbox.setOnCheckedChangeListener(getDayOfWeekCheckedListener(MONDAY))
            tuesdayCheckbox.setOnCheckedChangeListener(getDayOfWeekCheckedListener(TUESDAY))
            wednesdayCheckbox.setOnCheckedChangeListener(getDayOfWeekCheckedListener(WEDNESDAY))
            thursdayCheckbox.setOnCheckedChangeListener(getDayOfWeekCheckedListener(THURSDAY))
            fridayCheckbox.setOnCheckedChangeListener(getDayOfWeekCheckedListener(FRIDAY))
            saturdayCheckbox.setOnCheckedChangeListener(getDayOfWeekCheckedListener(SATURDAY))
            sundayCheckbox.setOnCheckedChangeListener(getDayOfWeekCheckedListener(SUNDAY))
        }
        viewModel.items.observe(viewLifecycleOwner) { adapter.submitList(it) }
        viewModel.message.observeEvent(viewLifecycleOwner) { Toast.makeText(context, it, Toast.LENGTH_LONG).show() }
    }

    private fun getTypeCheckedListener(type: ReminderType) = OnCheckedChangeListener { _, isChecked ->
        if (isChecked) viewModel.type = type
    }

    private fun getDayOfWeekCheckedListener(day: DayOfWeek) = OnCheckedChangeListener { _, isChecked ->
        viewModel.days = if (isChecked) viewModel.days + day else viewModel.days - day
    }
}
