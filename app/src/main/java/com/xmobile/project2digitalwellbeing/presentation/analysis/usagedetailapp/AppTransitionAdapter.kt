package com.xmobile.project2digitalwellbeing.presentation.analysis.usagedetailapp

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.xmobile.project2digitalwellbeing.databinding.ItemAppTransitionBinding

class AppTransitionAdapter : RecyclerView.Adapter<AppTransitionAdapter.ViewHolder>() {

    private val items = mutableListOf<AppTransitionUiModel>()

    fun submitList(newItems: List<AppTransitionUiModel>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemAppTransitionBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    class ViewHolder(private val binding: ItemAppTransitionBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: AppTransitionUiModel) {
            binding.tvFromAppName.text = item.fromAppName
            val fromIcon = try {
                binding.root.context.packageManager.getApplicationIcon(item.fromPackage)
            } catch (_: Throwable) {
                null
            }
            if (fromIcon != null) {
                binding.ivFromAppIcon.setImageDrawable(fromIcon)
            } else {
                binding.ivFromAppIcon.setImageResource(android.R.drawable.sym_def_app_icon)
            }
            binding.tvToAppName.text = item.toAppName
            val toIcon = try {
                binding.root.context.packageManager.getApplicationIcon(item.toPackage)
            } catch (_: Throwable) {
                null
            }
            if (toIcon != null) {
                binding.ivToAppIcon.setImageDrawable(toIcon)
            } else {
                binding.ivToAppIcon.setImageResource(android.R.drawable.sym_def_app_icon)
            }
            binding.tvTransitionCount.text = "${item.count}×"
        }
    }
}


