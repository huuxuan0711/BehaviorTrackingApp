package com.xmobile.project2digitalwellbeing.presentation.dashboard.weekly

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.xmobile.project2digitalwellbeing.databinding.ItemTopAppWeekBinding
import com.xmobile.project2digitalwellbeing.domain.apps.model.AppCategory
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

            val background = binding.viewDot.background?.mutate()
            if (background is GradientDrawable) {
                background.setColor(app.category.toColor())
            } else {
                binding.viewDot.backgroundTintList = android.content.res.ColorStateList.valueOf(app.category.toColor())
            }
        }

        private fun AppCategory.toColor(): Int {
            return when (this) {
                AppCategory.PRODUCTIVITY, AppCategory.EDUCATION, AppCategory.TOOLS -> Color.parseColor("#4CAF50")
                AppCategory.SOCIAL, AppCategory.VIDEO, AppCategory.GAME, AppCategory.MUSIC -> Color.parseColor("#5C6BC0")
                AppCategory.COMMUNICATION, AppCategory.BROWSER -> Color.parseColor("#FF9800")
                AppCategory.SYSTEM, AppCategory.OTHER, AppCategory.UNKNOWN -> Color.parseColor("#9E9E9E")
            }
        }
    }
}
