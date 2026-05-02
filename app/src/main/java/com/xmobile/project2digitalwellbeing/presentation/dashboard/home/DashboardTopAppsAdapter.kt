package com.xmobile.project2digitalwellbeing.presentation.dashboard.home

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.recyclerview.widget.RecyclerView
import com.xmobile.project2digitalwellbeing.databinding.ItemAppUsageBinding

class DashboardTopAppsAdapter : RecyclerView.Adapter<DashboardTopAppsAdapter.TopAppViewHolder>() {

    private val items = mutableListOf<TopAppUiModel>()

    fun submitList(apps: List<TopAppUiModel>) {
        items.clear()
        items.addAll(apps)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TopAppViewHolder {
        val binding = ItemAppUsageBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return TopAppViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TopAppViewHolder, position: Int) {
        holder.bind(app = items[position])
    }

    override fun getItemCount(): Int = items.size

    class TopAppViewHolder(
        private val binding: ItemAppUsageBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(app: TopAppUiModel) {
            binding.tvName.text = app.name
            binding.tvDuration.text = app.durationText
            binding.imgIcon.setImageDrawable(app.icon)

            binding.root.post {
                val containerWidth = (binding.root.width - binding.root.paddingStart - binding.root.paddingEnd)
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
