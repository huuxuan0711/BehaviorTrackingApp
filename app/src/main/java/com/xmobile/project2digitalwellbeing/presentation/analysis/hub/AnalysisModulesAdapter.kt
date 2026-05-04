package com.xmobile.project2digitalwellbeing.presentation.analysis.hub

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.xmobile.project2digitalwellbeing.databinding.ItemAnalysisModuleBinding
import com.xmobile.project2digitalwellbeing.databinding.ItemAnalysisSectionHeaderBinding

sealed interface AnalysisHubItem {
    data class SectionHeader(val title: String) : AnalysisHubItem
    data class Module(
        val title: String,
        val description: String,
        val iconResId: Int,
        val destination: Class<*>
    ) : AnalysisHubItem
}

class AnalysisModulesAdapter(
    private val onModuleClick: (AnalysisHubItem.Module) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val items = mutableListOf<AnalysisHubItem>()

    fun submitList(modules: List<AnalysisHubItem>) {
        items.clear()
        items.addAll(modules)
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int {
        return when (items[position]) {
            is AnalysisHubItem.SectionHeader -> VIEW_TYPE_SECTION_HEADER
            is AnalysisHubItem.Module -> VIEW_TYPE_MODULE
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            VIEW_TYPE_SECTION_HEADER -> AnalysisSectionHeaderViewHolder(
                ItemAnalysisSectionHeaderBinding.inflate(inflater, parent, false)
            )
            else -> AnalysisModuleViewHolder(
                ItemAnalysisModuleBinding.inflate(inflater, parent, false),
                onModuleClick
            )
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = items[position]) {
            is AnalysisHubItem.SectionHeader -> (holder as AnalysisSectionHeaderViewHolder).bind(item)
            is AnalysisHubItem.Module -> (holder as AnalysisModuleViewHolder).bind(item)
        }
    }

    override fun getItemCount(): Int = items.size

    fun isSectionHeader(position: Int): Boolean {
        return items.getOrNull(position) is AnalysisHubItem.SectionHeader
    }

    class AnalysisSectionHeaderViewHolder(
        private val binding: ItemAnalysisSectionHeaderBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: AnalysisHubItem.SectionHeader) {
            binding.tvSectionTitle.text = item.title
        }
    }

    class AnalysisModuleViewHolder(
        private val binding: ItemAnalysisModuleBinding,
        private val onModuleClick: (AnalysisHubItem.Module) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(module: AnalysisHubItem.Module) {
            binding.tvTitle.text = module.title
            binding.tvDesc.text = module.description
            binding.imgIcon.setImageResource(module.iconResId)
            binding.root.setOnClickListener { onModuleClick(module) }
        }
    }

    companion object {
        private const val VIEW_TYPE_SECTION_HEADER = 0
        private const val VIEW_TYPE_MODULE = 1
    }
}
