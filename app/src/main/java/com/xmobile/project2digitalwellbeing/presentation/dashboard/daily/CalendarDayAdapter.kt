package com.xmobile.project2digitalwellbeing.presentation.dashboard.daily

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.xmobile.project2digitalwellbeing.R
import com.xmobile.project2digitalwellbeing.databinding.ItemCalendarDayBinding

class CalendarDayAdapter(
    private val onDayClick: (CalendarDayUiModel) -> Unit
) : RecyclerView.Adapter<CalendarDayAdapter.CalendarDayViewHolder>() {

    private val items = mutableListOf<CalendarDayUiModel>()

    fun submitList(days: List<CalendarDayUiModel>) {
        items.clear()
        items.addAll(days)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CalendarDayViewHolder {
        val binding = ItemCalendarDayBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return CalendarDayViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CalendarDayViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class CalendarDayViewHolder(
        private val binding: ItemCalendarDayBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: CalendarDayUiModel) {
            binding.btnDay.text = item.label
            binding.btnDay.isEnabled = item.isEnabled
            binding.btnDay.visibility = if (item.isEnabled) View.VISIBLE else View.INVISIBLE
            binding.vwDayBackground.visibility = if (item.isSelected || item.isToday) View.VISIBLE else View.GONE
            binding.vwDayBackground.isSelected = item.isSelected
            binding.vwDayBackground.setBackgroundResource(
                when {
                    item.isSelected -> R.drawable.bg_calendar_day_selected
                    item.isToday -> R.drawable.bg_calendar_day_today
                    else -> R.drawable.bg_calendar_day
                }
            )
            binding.btnDay.setTextColor(
                when {
                    item.isSelected -> android.graphics.Color.WHITE
                    !item.isEnabled -> android.graphics.Color.TRANSPARENT
                    else -> android.graphics.Color.BLACK
                }
            )
            binding.btnDay.setOnClickListener {
                if (item.isEnabled) {
                    onDayClick(item)
                }
            }
        }
    }
}
