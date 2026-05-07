package com.xmobile.project2digitalwellbeing.presentation.analysis.appcategory

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.xmobile.project2digitalwellbeing.databinding.ItemAppBinding
import android.view.View

class AppItemAdapter(
    private val onCategoryClick: (AppItemUiModel, View, android.widget.ImageView) -> Unit,
    private val onItemClick: (AppItemUiModel) -> Unit
) : ListAdapter<AppItemUiModel, AppItemAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemAppBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(private val binding: ItemAppBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: AppItemUiModel) {
            binding.tvName.text = item.appName
            binding.tvUsage.visibility = View.VISIBLE
            binding.tvUsage.text = item.usageText

            binding.tvCategory.text = item.category.toDisplayName()

            val color = item.category.toBadgeColor()
            binding.tvCategory.setTextColor(color)
            binding.imgArrow.setColorFilter(color)

            val bg = binding.layoutCategory.background?.mutate()
            if (bg is GradientDrawable) {
                // Set faint background color matching the stroke slightly
                val alphaColor = Color.argb(20, Color.red(color), Color.green(color), Color.blue(color))
                bg.setColor(alphaColor)
            } else {
                val alphaColor = Color.argb(20, Color.red(color), Color.green(color), Color.blue(color))
                binding.layoutCategory.backgroundTintList = android.content.res.ColorStateList.valueOf(alphaColor)
            }

            // Try to load icon
            try {
                val icon = binding.root.context.packageManager.getApplicationIcon(item.packageName)
                binding.imgIcon.setImageDrawable(icon)
            } catch (e: Exception) {
                // Fallback icon already set in XML or can set a default here
            }

            binding.layoutCategory.setOnClickListener { onCategoryClick(item, binding.layoutCategory, binding.imgArrow) }
            binding.root.setOnClickListener { onItemClick(item) }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<AppItemUiModel>() {
        override fun areItemsTheSame(oldItem: AppItemUiModel, newItem: AppItemUiModel): Boolean =
            oldItem.packageName == newItem.packageName

        override fun areContentsTheSame(oldItem: AppItemUiModel, newItem: AppItemUiModel): Boolean =
            oldItem == newItem
    }
}
