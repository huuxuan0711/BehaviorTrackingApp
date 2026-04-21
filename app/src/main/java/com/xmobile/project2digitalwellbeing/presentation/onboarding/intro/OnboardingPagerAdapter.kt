package com.xmobile.project2digitalwellbeing.presentation.onboarding.intro

import android.content.res.ColorStateList
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.widget.ImageViewCompat
import androidx.recyclerview.widget.RecyclerView
import com.xmobile.project2digitalwellbeing.databinding.ItemOnboardingBinding

class OnboardingPagerAdapter(
    private val items: List<OnboardingItem>
) : RecyclerView.Adapter<OnboardingPagerAdapter.OnboardingViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OnboardingViewHolder {
        val binding = ItemOnboardingBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return OnboardingViewHolder(binding)
    }

    override fun onBindViewHolder(holder: OnboardingViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    class OnboardingViewHolder(
        private val binding: ItemOnboardingBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: OnboardingItem) = with(binding) {
            imgIcon.setImageResource(item.iconResId)
            ImageViewCompat.setImageTintList(imgIcon, ColorStateList.valueOf(item.iconTintRes))

            val background = (imgIcon.background.mutate() as? GradientDrawable) ?: GradientDrawable().apply {
                shape = GradientDrawable.OVAL
            }
            background.setColor(item.iconBackgroundRes)
            imgIcon.background = background

            tvTitle.text = item.title
            tvDescription.text = item.description
        }
    }
}
