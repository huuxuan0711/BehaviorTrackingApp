package com.xmobile.project2digitalwellbeing.presentation.dashboard.session

import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.xmobile.project2digitalwellbeing.R
import com.xmobile.project2digitalwellbeing.databinding.ItemSessionBinding
import com.xmobile.project2digitalwellbeing.domain.apps.model.AppCategory

class SessionTimelineAdapter : RecyclerView.Adapter<SessionTimelineAdapter.SessionTimelineViewHolder>() {

    private val items = mutableListOf<SessionTimelineItemUiModel>()

    fun submitList(sessions: List<SessionTimelineItemUiModel>) {
        items.clear()
        items.addAll(sessions)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SessionTimelineViewHolder {
        val binding = ItemSessionBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return SessionTimelineViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SessionTimelineViewHolder, position: Int) {
        val maxRatio = items.maxOfOrNull { it.progressRatio } ?: 0f
        holder.bind(items[position], maxRatio)
    }

    override fun getItemCount(): Int = items.size

    class SessionTimelineViewHolder(
        private val binding: ItemSessionBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: SessionTimelineItemUiModel, maxRatio: Float) {
            binding.tvAppName.text = item.appName
            binding.tvTime.text = item.timeRangeText
            binding.tvDuration.text = item.durationText
            binding.imgIcon.setImageDrawable(
                binding.root.context.packageManager.getApplicationIconOrNull(item.packageName)
                    ?: ContextCompat.getDrawable(binding.root.context, R.drawable.smartphone)
            )
            binding.viewLeftBorder.setBackgroundColor(item.category.toColor())

            val transitionLabel = item.transitionLabel
            binding.layoutAppTransition.visibility = if (transitionLabel.isNullOrBlank()) View.GONE else View.VISIBLE
            binding.txtAppTransition.text = transitionLabel.orEmpty()

            binding.root.post {
                val progressContainer = binding.viewProgress.parent as FrameLayout
                val containerWidth = (progressContainer.width - progressContainer.paddingStart - progressContainer.paddingEnd)
                    .coerceAtLeast(0)
                val normalizedRatio = if (maxRatio <= 0f) 0f else item.progressRatio / maxRatio
                val width = (containerWidth * normalizedRatio.coerceIn(0.08f, 1f)).toInt().coerceAtLeast(0)
                binding.viewProgress.layoutParams = FrameLayout.LayoutParams(
                    width,
                    FrameLayout.LayoutParams.MATCH_PARENT
                )
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

        private fun PackageManager.getApplicationIconOrNull(packageName: String): Drawable? =
            try {
                getApplicationIcon(packageName)
            } catch (_: PackageManager.NameNotFoundException) {
                null
            }
    }
}
