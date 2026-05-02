package com.xmobile.project2digitalwellbeing.presentation.dashboard.weekly

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.xmobile.project2digitalwellbeing.databinding.ItemTopAppWeekBinding
import com.xmobile.project2digitalwellbeing.presentation.dashboard.home.TopAppUiModel

class WeeklyTopAppsAdapter : RecyclerView.Adapter<WeeklyTopAppsAdapter.TopAppViewHolder>() {

    private val items = mutableListOf<TopAppUiModel>()

    fun submitList(apps: List<TopAppUiModel>) {
        items.clear()
        items.addAll(apps)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TopAppViewHolder {
        val binding = ItemTopAppWeekBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return TopAppViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TopAppViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    class TopAppViewHolder(
        private val binding: ItemTopAppWeekBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(app: TopAppUiModel) {
            binding.tvName.text = app.name
            binding.tvTime.text = app.durationText
            binding.imgIcon.setImageDrawable(app.icon)
        }
    }
}
