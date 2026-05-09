package com.xmobile.project2digitalwellbeing.presentation.settings.preferences

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.xmobile.project2digitalwellbeing.R
import com.xmobile.project2digitalwellbeing.databinding.ItemLanguageOptionBinding

class LanguageAdapter(
    private val languages: List<LanguageUiModel>,
    private val onLanguageSelected: (LanguageUiModel) -> Unit
) : RecyclerView.Adapter<LanguageAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemLanguageOptionBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(languages[position])
    }

    override fun getItemCount(): Int = languages.size

    inner class ViewHolder(private val binding: ItemLanguageOptionBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(language: LanguageUiModel) {
            binding.ivFlag.setImageResource(language.flagRes)
            binding.tvLanguageName.text = binding.root.context.getString(language.nameRes)
            binding.root.setOnClickListener { onLanguageSelected(language) }
        }
    }
}

data class LanguageUiModel(
    val code: String,
    val nameRes: Int,
    val flagRes: Int
)
