package com.xmobile.project2digitalwellbeing.presentation.dashboard.session

import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.xmobile.project2digitalwellbeing.R
import com.xmobile.project2digitalwellbeing.databinding.ItemPeriodHeaderBinding
import com.xmobile.project2digitalwellbeing.databinding.ItemSessionBinding
import com.xmobile.project2digitalwellbeing.databinding.ItemShortSessionsGroupBinding
import com.xmobile.project2digitalwellbeing.domain.apps.model.AppCategory

class SessionTimelineAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val items = mutableListOf<TimelineRow>()
    private var sessions = emptyList<SessionTimelineItemUiModel>()
    private val expandedShortSessionPeriods = mutableSetOf<String>()

    fun submitList(sessions: List<SessionTimelineItemUiModel>) {
        this.sessions = sessions
        expandedShortSessionPeriods.clear()
        rebuildRows()
        notifyDataSetChanged()
    }

    private fun rebuildRows() {
        items.clear()
        val visibleMeaningfulSessions = sessions
            .filter { it.durationMillis >= MEANINGFUL_SESSION_THRESHOLD_MILLIS }
            .take(MAX_VISIBLE_MEANINGFUL_SESSIONS)

        sessions
            .groupBy { it.periodLabel }
            .forEach { (period, periodSessions) ->
                val visiblePeriodSessions = periodSessions.filter { it in visibleMeaningfulSessions }
                val shortSessions = periodSessions.filter { it.durationMillis < MEANINGFUL_SESSION_THRESHOLD_MILLIS }
                val isExpanded = period in expandedShortSessionPeriods

                if (visiblePeriodSessions.isEmpty() && shortSessions.isEmpty()) {
                    return@forEach
                }

                items += TimelineRow.Header(period)
                items += visiblePeriodSessions.map(TimelineRow::Session)

                if (shortSessions.isNotEmpty()) {
                    items += TimelineRow.ShortSessionsGroup(
                        period = period,
                        appCount = shortSessions.map { it.packageName }.distinct().size,
                        isExpanded = isExpanded,
                        shortSessions = shortSessions
                    )
                    if (isExpanded) {
                        items += shortSessions.map(TimelineRow::Session)
                    }
                }
            }
    }

    override fun getItemViewType(position: Int): Int {
        return when (items[position]) {
            is TimelineRow.Header -> VIEW_TYPE_HEADER
            is TimelineRow.Session -> VIEW_TYPE_SESSION
            is TimelineRow.ShortSessionsGroup -> VIEW_TYPE_SHORT_SESSIONS_GROUP
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            VIEW_TYPE_HEADER -> PeriodHeaderViewHolder(
                ItemPeriodHeaderBinding.inflate(inflater, parent, false)
            )
            VIEW_TYPE_SHORT_SESSIONS_GROUP -> ShortSessionsGroupViewHolder(
                ItemShortSessionsGroupBinding.inflate(inflater, parent, false),
                onToggle = ::toggleShortSessions
            )
            else -> SessionTimelineViewHolder(
                ItemSessionBinding.inflate(inflater, parent, false)
            )
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val row = items[position]) {
            is TimelineRow.Header -> (holder as PeriodHeaderViewHolder).bind(row.period)
            is TimelineRow.ShortSessionsGroup -> (holder as ShortSessionsGroupViewHolder).bind(row)
            is TimelineRow.Session -> {
                val maxRatio = items
                    .filterIsInstance<TimelineRow.Session>()
                    .maxOfOrNull { it.item.progressRatio }
                    ?: 0f
                val hasVisiblePreviousSession = items.getOrNull(position - 1) is TimelineRow.Session
                (holder as SessionTimelineViewHolder).bind(
                    item = row.item,
                    maxRatio = maxRatio,
                    showTransition = hasVisiblePreviousSession
                )
            }
        }
    }

    override fun getItemCount(): Int = items.size

    private fun toggleShortSessions(position: Int) {
        val row = items.getOrNull(position) as? TimelineRow.ShortSessionsGroup ?: return

        if (row.isExpanded) {
            expandedShortSessionPeriods -= row.period
            items[position] = row.copy(isExpanded = false)
            repeat(row.shortSessions.size) {
                if (items.getOrNull(position + 1) is TimelineRow.Session) {
                    items.removeAt(position + 1)
                }
            }
            notifyItemChanged(position)
            notifyItemRangeRemoved(position + 1, row.shortSessions.size)
        } else {
            expandedShortSessionPeriods += row.period
            items[position] = row.copy(isExpanded = true)
            val insertedRows = row.shortSessions.map(TimelineRow::Session)
            items.addAll(position + 1, insertedRows)
            notifyItemChanged(position)
            notifyItemRangeInserted(position + 1, insertedRows.size)
        }
    }

    class PeriodHeaderViewHolder(
        private val binding: ItemPeriodHeaderBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(period: String) {
            binding.tvPeriod.text = period
        }
    }

    private class ShortSessionsGroupViewHolder(
        private val binding: ItemShortSessionsGroupBinding,
        private val onToggle: (Int) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: TimelineRow.ShortSessionsGroup) {
            val appLabel = if (item.appCount == 1) "app" else "apps"
            binding.tvShortSessionsSummary.text = "Other short sessions (${item.appCount} $appLabel)"
            binding.imgExpand.setImageResource(
                if (item.isExpanded) R.drawable.chevron_up else R.drawable.chevron_down
            )
            binding.root.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onToggle(position)
                }
            }
        }
    }

    class SessionTimelineViewHolder(
        private val binding: ItemSessionBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: SessionTimelineItemUiModel, maxRatio: Float, showTransition: Boolean) {
            binding.tvAppName.text = item.appName
            binding.tvTime.text = item.timeRangeText
            binding.tvDuration.text = item.durationText
            binding.imgIcon.setImageDrawable(
                binding.root.context.packageManager.getApplicationIconOrNull(item.packageName)
                    ?: ContextCompat.getDrawable(binding.root.context, R.drawable.smartphone)
            )
            binding.viewLeftBorder.setBackgroundColor(item.category.toColor())

            val transitionLabel = item.transitionLabel
            binding.layoutAppTransition.visibility =
                if (showTransition && !transitionLabel.isNullOrBlank()) View.VISIBLE else View.GONE
            binding.txtAppTransition.text = transitionLabel.orEmpty()

            binding.root.post {
                val progressContainer = binding.viewProgress.parent as FrameLayout
                val containerWidth = (progressContainer.width - progressContainer.paddingStart - progressContainer.paddingEnd)
                    .coerceAtLeast(0)
                val normalizedRatio = if (maxRatio <= 0f) 0f else item.progressRatio / maxRatio
                val width = (containerWidth * normalizedRatio.coerceIn(0.08f, 1f)).toInt().coerceAtLeast(0)
                binding.viewProgress.layoutParams = FrameLayout.LayoutParams(
                    width,
                    FrameLayout.LayoutParams.MATCH_PARENT
                )
            }
        }

        private fun AppCategory.toColor(): Int {
            return when (this) {
                AppCategory.PRODUCTIVITY, AppCategory.EDUCATION, AppCategory.TOOLS -> Color.parseColor("#4CAF50")
                AppCategory.SOCIAL, AppCategory.VIDEO, AppCategory.GAME, AppCategory.MUSIC -> Color.parseColor("#5C6BC0")
                AppCategory.COMMUNICATION, AppCategory.BROWSER -> Color.parseColor("#FF9800")
                AppCategory.SYSTEM, AppCategory.OTHER, AppCategory.UNKNOWN -> Color.parseColor("#9E9E9E")
            }
        }

        private fun PackageManager.getApplicationIconOrNull(packageName: String): Drawable? =
            try {
                getApplicationIcon(packageName)
            } catch (_: PackageManager.NameNotFoundException) {
                null
            }
    }

    private sealed interface TimelineRow {
        data class Header(val period: String) : TimelineRow
        data class Session(val item: SessionTimelineItemUiModel) : TimelineRow
        data class ShortSessionsGroup(
            val period: String,
            val appCount: Int,
            val isExpanded: Boolean,
            val shortSessions: List<SessionTimelineItemUiModel>
        ) : TimelineRow
    }

    companion object {
        private const val VIEW_TYPE_HEADER = 0
        private const val VIEW_TYPE_SESSION = 1
        private const val VIEW_TYPE_SHORT_SESSIONS_GROUP = 2
        private const val MEANINGFUL_SESSION_THRESHOLD_MILLIS = 60L * 1000L
        private const val MAX_VISIBLE_MEANINGFUL_SESSIONS = 15
    }
}
