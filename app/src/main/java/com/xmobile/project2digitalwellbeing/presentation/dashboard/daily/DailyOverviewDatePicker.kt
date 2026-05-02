package com.xmobile.project2digitalwellbeing.presentation.dashboard.daily

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupWindow
import androidx.recyclerview.widget.GridLayoutManager
import com.xmobile.project2digitalwellbeing.databinding.PopupDatePickerBinding
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

internal class DailyOverviewDatePicker(
    private val context: Context
) {

    private var popupWindow: PopupWindow? = null

    fun show(
        anchor: View,
        selectedDate: LocalDate,
        onDateSelected: (LocalDate) -> Unit
    ) {
        popupWindow?.dismiss()

        val popupBinding = PopupDatePickerBinding.inflate(LayoutInflater.from(context))
        val calendarAdapter = CalendarDayAdapter { day ->
            val resolvedDate = day.date ?: return@CalendarDayAdapter
            onDateSelected(resolvedDate)
            popupWindow?.dismiss()
        }
        var displayedMonth = YearMonth.from(selectedDate)

        popupBinding.rvCalendarDays.apply {
            layoutManager = GridLayoutManager(context, 7)
            adapter = calendarAdapter
        }

        val popup = PopupWindow(
            popupBinding.root,
            anchor.width.coerceAtLeast(context.resources.displayMetrics.widthPixels - 64),
            ViewGroup.LayoutParams.WRAP_CONTENT,
            true
        ).apply {
            elevation = 12f
            isOutsideTouchable = true
        }

        fun renderMonth(month: YearMonth) {
            popupBinding.txtMonthYear.text = month.format(
                DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault())
            )
            calendarAdapter.submitList(buildCalendarDays(month, selectedDate))
        }

        popupBinding.btnPrevMonth.setOnClickListener {
            displayedMonth = displayedMonth.minusMonths(1)
            renderMonth(displayedMonth)
        }
        popupBinding.btnNextMonth.setOnClickListener {
            displayedMonth = displayedMonth.plusMonths(1)
            renderMonth(displayedMonth)
        }

        renderMonth(displayedMonth)
        popup.showAsDropDown(anchor, 0, 16)
        popupWindow = popup
    }

    fun dismiss() {
        popupWindow?.dismiss()
        popupWindow = null
    }

    private fun buildCalendarDays(
        month: YearMonth,
        selectedDate: LocalDate
    ): List<CalendarDayUiModel> {
        val firstDay = month.atDay(1)
        val leadingEmptyDays = firstDay.dayOfWeek.toCalendarColumnIndex()
        val today = LocalDate.now()

        return buildList {
            repeat(leadingEmptyDays) {
                add(
                    CalendarDayUiModel(
                        date = null,
                        label = "",
                        isSelected = false,
                        isToday = false,
                        isEnabled = false
                    )
                )
            }

            for (day in 1..month.lengthOfMonth()) {
                val date = month.atDay(day)
                add(
                    CalendarDayUiModel(
                        date = date,
                        label = day.toString(),
                        isSelected = date == selectedDate,
                        isToday = date == today,
                        isEnabled = true
                    )
                )
            }
        }
    }

    private fun DayOfWeek.toCalendarColumnIndex(): Int = value % 7
}
