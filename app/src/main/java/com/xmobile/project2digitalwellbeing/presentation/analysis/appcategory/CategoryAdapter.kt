package com.xmobile.project2digitalwellbeing.presentation.analysis.appcategory

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.xmobile.project2digitalwellbeing.R
import com.xmobile.project2digitalwellbeing.databinding.ItemCategoryBinding
class CategoryAdapter(
    private val onCategoryHeaderClick: (CategoryGroupUiModel) -> Unit,
    private val onAppCategoryClick: (AppItemUiModel, View, android.widget.ImageView) -> Unit,
    private val onAppItemClick: (AppItemUiModel) -> Unit
) : ListAdapter<CategoryGroupUiModel, CategoryAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemCategoryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(private val binding: ItemCategoryBinding) : RecyclerView.ViewHolder(binding.root) {
        
        private val appAdapter = AppItemAdapter(onAppCategoryClick, onAppItemClick)

        init {
            binding.rvApps.apply {
                layoutManager = LinearLayoutManager(context)
                adapter = appAdapter
            }
        }

        fun bind(item: CategoryGroupUiModel) {
            binding.tvCategory.text = item.category.toDisplayName()
            binding.tvCount.text = "${item.apps.size} apps"
            
            val color = item.category.toBadgeColor()
            binding.tvCategory.setTextColor(color)
            val bg = binding.tvCategory.background?.mutate()
            if (bg is GradientDrawable) {
                // Set faint background color matching the stroke slightly
                val alphaColor = Color.argb(20, Color.red(color), Color.green(color), Color.blue(color))
                bg.setColor(alphaColor)
            } else {
                val alphaColor = Color.argb(20, Color.red(color), Color.green(color), Color.blue(color))
                binding.tvCategory.backgroundTintList = android.content.res.ColorStateList.valueOf(alphaColor)
            }

            binding.ivArrow.setImageResource(
                if (item.isExpanded) R.drawable.chevron_up else R.drawable.chevron_down
            )
            binding.rvApps.visibility = if (item.isExpanded) View.VISIBLE else View.GONE
            
            appAdapter.submitList(item.apps)

            binding.layoutHeader.setOnClickListener {
                onCategoryHeaderClick(item)
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<CategoryGroupUiModel>() {
        override fun areItemsTheSame(oldItem: CategoryGroupUiModel, newItem: CategoryGroupUiModel): Boolean =
            oldItem.category == newItem.category

        override fun areContentsTheSame(oldItem: CategoryGroupUiModel, newItem: CategoryGroupUiModel): Boolean =
            oldItem == newItem
    }
}
