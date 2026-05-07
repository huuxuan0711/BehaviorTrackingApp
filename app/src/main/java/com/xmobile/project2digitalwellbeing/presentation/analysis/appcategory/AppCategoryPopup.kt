package com.xmobile.project2digitalwellbeing.presentation.analysis.appcategory

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.graphics.Rect
import android.widget.ImageView
import android.widget.PopupWindow
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.xmobile.project2digitalwellbeing.R
import com.xmobile.project2digitalwellbeing.domain.apps.model.AppCategory
import com.xmobile.project2digitalwellbeing.domain.apps.model.toFocusGroup

class AppCategoryPopup(
    private val context: Context,
    private val anchorView: View,
    private val imgArrow: ImageView,
    private val selectedCategory: AppCategory,
    private val onCategorySelected: (AppCategory) -> Unit
) {
    fun show() {
        imgArrow.setImageResource(R.drawable.chevron_up)

        val popupView = LayoutInflater.from(context).inflate(R.layout.layout_popup_category, null)
        val popupHeight = 200.dp(context)
        val popupWindow = PopupWindow(
            popupView,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            popupHeight,
            true
        )
        popupWindow.setBackgroundDrawable(ContextCompat.getDrawable(context, R.drawable.bg_popup_rounded))
        popupWindow.elevation = 5f
        popupWindow.setOnDismissListener {
            imgArrow.setImageResource(R.drawable.chevron_down)
        }

        val categories = AppCategory.entries.toList()
        val recyclerView = popupView.findViewById<RecyclerView>(R.id.rvCategoryOptions)
        recyclerView.layoutManager = LinearLayoutManager(context)
        val desiredHeight = popupHeight
        val existingLayoutParams = recyclerView.layoutParams
        recyclerView.layoutParams = if (existingLayoutParams != null) {
            existingLayoutParams.apply { height = desiredHeight }
        } else {
            ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, desiredHeight)
        }
        recyclerView.adapter = AppCategoryOptionAdapter(
            items = categories,
            selectedCategory = selectedCategory,
            onClick = { category ->
                onCategorySelected(category)
                popupWindow.dismiss()
            }
        )

        popupView.measure(
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        )
        val xOff = anchorView.width - popupView.measuredWidth
        val margin = (10 * context.resources.displayMetrics.density).toInt()

        val anchorLocation = IntArray(2)
        anchorView.getLocationOnScreen(anchorLocation)
        val anchorTopOnScreen = anchorLocation[1]
        val anchorBottomOnScreen = anchorTopOnScreen + anchorView.height

        val windowRect = Rect()
        anchorView.getWindowVisibleDisplayFrame(windowRect)
        val spaceBelow = windowRect.bottom - anchorBottomOnScreen
        val spaceAbove = anchorTopOnScreen - windowRect.top

        val showAbove = spaceBelow < popupHeight + margin && spaceAbove > spaceBelow
        val yOff = if (showAbove) {
            -(anchorView.height + popupHeight + margin)
        } else {
            margin
        }

        popupWindow.showAsDropDown(anchorView, xOff, yOff)
    }
}

private fun Int.dp(context: Context): Int {
    return (this * context.resources.displayMetrics.density).toInt()
}

private class AppCategoryOptionAdapter(
    private val items: List<AppCategory>,
    private val selectedCategory: AppCategory,
    private val onClick: (AppCategory) -> Unit
) : RecyclerView.Adapter<AppCategoryOptionAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_popup_category_option, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position], items[position] == selectedCategory, onClick)
    }

    override fun getItemCount(): Int = items.size

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val dot: View = itemView.findViewById(R.id.viewDot)
        private val name: TextView = itemView.findViewById(R.id.tvCategoryName)

        fun bind(item: AppCategory, selected: Boolean, onClick: (AppCategory) -> Unit) {
            val color = item.toFocusGroup().toBadgeColor()
            name.text = item.toDisplayName()
            name.setTextColor(color)

            val dotBg = dot.background?.mutate()
            if (dotBg is GradientDrawable) {
                dotBg.setColor(color)
            } else {
                dot.setBackgroundColor(color)
            }

            itemView.alpha = if (selected) 1f else 0.88f
            itemView.setOnClickListener { onClick(item) }
        }
    }
}
