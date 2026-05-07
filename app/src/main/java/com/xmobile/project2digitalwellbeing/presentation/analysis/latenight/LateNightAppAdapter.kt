package com.xmobile.project2digitalwellbeing.presentation.analysis.latenight

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.xmobile.project2digitalwellbeing.databinding.ItemMostUsedAppBinding

class LateNightAppAdapter : ListAdapter<LateNightAppUiModel, LateNightAppAdapter.ViewHolder>(DiffCallback) {

    class ViewHolder(private val binding: ItemMostUsedAppBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: LateNightAppUiModel) {
            binding.txtName.text = item.appName
            binding.txtDuration.text = item.durationText
            
            try {
                val icon = itemView.context.packageManager.getApplicationIcon(item.packageName)
                binding.imgIcon.setImageDrawable(icon)
            } catch (_: Exception) {
                binding.imgIcon.setImageResource(android.R.drawable.sym_def_app_icon)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(ItemMostUsedAppBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    object DiffCallback : DiffUtil.ItemCallback<LateNightAppUiModel>() {
        override fun areItemsTheSame(oldItem: LateNightAppUiModel, newItem: LateNightAppUiModel) = oldItem.packageName == newItem.packageName
        override fun areContentsTheSame(oldItem: LateNightAppUiModel, newItem: LateNightAppUiModel) = oldItem == newItem
    }
}
