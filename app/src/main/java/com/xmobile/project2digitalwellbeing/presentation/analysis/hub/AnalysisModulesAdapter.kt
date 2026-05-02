package com.xmobile.project2digitalwellbeing.presentation.analysis.hub

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.xmobile.project2digitalwellbeing.databinding.ItemAnalysisModuleBinding

data class AnalysisModuleItem(
    val title: String,
    val description: String,
    val iconResId: Int,
    val destination: Class<*>
)

class AnalysisModulesAdapter(
    private val onModuleClick: (AnalysisModuleItem) -> Unit
) : RecyclerView.Adapter<AnalysisModulesAdapter.AnalysisModuleViewHolder>() {

    private val items = mutableListOf<AnalysisModuleItem>()

    fun submitList(modules: List<AnalysisModuleItem>) {
        items.clear()
        items.addAll(modules)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AnalysisModuleViewHolder {
        val binding = ItemAnalysisModuleBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return AnalysisModuleViewHolder(binding, onModuleClick)
    }

    override fun onBindViewHolder(holder: AnalysisModuleViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    class AnalysisModuleViewHolder(
        private val binding: ItemAnalysisModuleBinding,
        private val onModuleClick: (AnalysisModuleItem) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(module: AnalysisModuleItem) {
            binding.tvTitle.text = module.title
            binding.tvDesc.text = module.description
            binding.imgIcon.setImageResource(module.iconResId)
            binding.root.setOnClickListener { onModuleClick(module) }
        }
    }
}
