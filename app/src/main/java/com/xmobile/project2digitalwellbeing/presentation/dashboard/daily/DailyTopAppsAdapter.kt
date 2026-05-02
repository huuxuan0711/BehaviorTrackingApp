package com.xmobile.project2digitalwellbeing.presentation.dashboard.daily

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.recyclerview.widget.RecyclerView
import com.xmobile.project2digitalwellbeing.databinding.ItemTopAppDayBinding
import com.xmobile.project2digitalwellbeing.presentation.dashboard.home.TopAppUiModel

class DailyTopAppsAdapter : RecyclerView.Adapter<DailyTopAppsAdapter.TopAppViewHolder>() {

    private val items = mutableListOf<TopAppUiModel>()

    fun submitList(apps: List<TopAppUiModel>) {
        items.clear()
        items.addAll(apps)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TopAppViewHolder {
        val binding = ItemTopAppDayBinding.inflate(
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
        private val binding: ItemTopAppDayBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(app: TopAppUiModel) {
            binding.tvName.text = app.name
            binding.tvTime.text = app.durationText
            binding.imgIcon.setImageDrawable(app.icon)

            binding.root.post {
                val progressContainer = binding.viewProgress.parent as FrameLayout
                val containerWidth = (progressContainer.width - progressContainer.paddingStart - progressContainer.paddingEnd)
                    .coerceAtLeast(0)
                val width = (containerWidth * app.progressRatio.coerceIn(0f, 1f)).toInt().coerceAtLeast(0)
                binding.viewProgress.layoutParams = FrameLayout.LayoutParams(
                    width,
                    FrameLayout.LayoutParams.MATCH_PARENT
                )
            }
        }
    }
}
