package com.xmobile.project2digitalwellbeing.presentation.analysis.usagepattern

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.xmobile.project2digitalwellbeing.databinding.ItemMostFrequentlyAppBinding

class UsagePatternTopAppsAdapter : RecyclerView.Adapter<UsagePatternTopAppsAdapter.ViewHolder>() {

    private val items = mutableListOf<UsagePatternTopAppUiModel>()

    fun submitList(data: List<UsagePatternTopAppUiModel>) {
        items.clear()
        items.addAll(data)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemMostFrequentlyAppBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    class ViewHolder(
        private val binding: ItemMostFrequentlyAppBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: UsagePatternTopAppUiModel) {
            binding.txtName.text = item.appName
            binding.txtSessions.text = item.sessionCountText
            val icon = try {
                binding.root.context.packageManager.getApplicationIcon(item.packageName)
            } catch (_: Throwable) {
                null
            }
            if (icon != null) {
                binding.imgIcon.setImageDrawable(icon)
            } else {
                binding.imgIcon.setImageResource(android.R.drawable.sym_def_app_icon)
            }
        }
    }
}
