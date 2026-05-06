package com.xmobile.project2digitalwellbeing.presentation.analysis.appcategory

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.PopupWindow
import androidx.core.content.ContextCompat
import com.xmobile.project2digitalwellbeing.R
import com.xmobile.project2digitalwellbeing.domain.apps.model.AppFocusGroup

class AppCategoryPopup(
    private val context: Context,
    private val anchorView: View,
    private val imgArrow: ImageView,
    private val onCategorySelected: (AppFocusGroup) -> Unit
) {
    fun show() {
        val inflater = LayoutInflater.from(context)
        val popupView = inflater.inflate(R.layout.layout_popup_category, null)
        val popupWindow = PopupWindow(
            popupView,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            true
        )

        // Set elevation and background
        popupWindow.setBackgroundDrawable(ContextCompat.getDrawable(context, R.drawable.bg_popup_rounded))
        popupWindow.elevation = 5f

        // Change arrow to up when opened
        imgArrow.setImageResource(R.drawable.chevron_up)

        popupWindow.setOnDismissListener {
            // Revert arrow back to down when closed
            imgArrow.setImageResource(R.drawable.chevron_down)
        }

        popupView.findViewById<View>(R.id.llProductive).setOnClickListener {
            onCategorySelected(AppFocusGroup.PRODUCTIVE)
            popupWindow.dismiss()
        }
        popupView.findViewById<View>(R.id.llNeutral).setOnClickListener {
            onCategorySelected(AppFocusGroup.NEUTRAL)
            popupWindow.dismiss()
        }
        popupView.findViewById<View>(R.id.llDistracting).setOnClickListener {
            onCategorySelected(AppFocusGroup.DISTRACTING)
            popupWindow.dismiss()
        }

        // Measure popup to calculate offset for right alignment
        popupView.measure(
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        )

        // Calculate x offset to align right edges, and y offset for 10dp margin top
        val xOff = anchorView.width - popupView.measuredWidth
        val yOff = (10 * context.resources.displayMetrics.density).toInt()

        // Show popup below the anchor view
        popupWindow.showAsDropDown(anchorView, xOff, yOff)
    }
}
