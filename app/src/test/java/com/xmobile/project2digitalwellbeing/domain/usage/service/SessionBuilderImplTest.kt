package com.xmobile.project2digitalwellbeing.domain.usage.service

import com.xmobile.project2digitalwellbeing.domain.usage.model.AppSession
import com.xmobile.project2digitalwellbeing.domain.usage.model.AppUsageEvent
import com.xmobile.project2digitalwellbeing.domain.usage.model.UsageEventType
import org.junit.Assert.assertEquals
import org.junit.Test

class SessionBuilderImplTest {

    private val sessionBuilder = SessionBuilderImpl()

    @Test
    fun `builds a session from foreground to matching background`() {
        val sessions = sessionBuilder.buildSessions(
            events = listOf(
                event("app.a", 1_000L, UsageEventType.FOREGROUND),
                event("app.a", 5_000L, UsageEventType.BACKGROUND)
            ),
            rangeStartMillis = 0L,
            rangeEndMillis = 10_000L,
            nowMillis = 10_000L
        )

        assertEquals(
            listOf(session("app.a", 1_000L, 5_000L)),
            sessions
        )
    }

    @Test
    fun `ignores repeated foreground for the same app`() {
        val sessions = sessionBuilder.buildSessions(
            events = listOf(
                event("app.a", 1_000L, UsageEventType.FOREGROUND),
                event("app.a", 1_500L, UsageEventType.FOREGROUND),
                event("app.a", 5_000L, UsageEventType.BACKGROUND)
            ),
            rangeStartMillis = 0L,
            rangeEndMillis = 10_000L,
            nowMillis = 10_000L
        )

        assertEquals(
            listOf(session("app.a", 1_000L, 5_000L)),
            sessions
        )
    }

    @Test
    fun `closes current session when another app moves to foreground`() {
        val sessions = sessionBuilder.buildSessions(
            events = listOf(
                event("app.a", 1_000L, UsageEventType.FOREGROUND),
                event("app.b", 3_000L, UsageEventType.FOREGROUND),
                event("app.b", 7_000L, UsageEventType.BACKGROUND)
            ),
            rangeStartMillis = 0L,
            rangeEndMillis = 10_000L,
            nowMillis = 10_000L
        )

        assertEquals(
            listOf(
                session("app.a", 1_000L, 3_000L),
                session("app.b", 3_000L, 7_000L)
            ),
            sessions
        )
    }

    @Test
    fun `ignores orphan background and background of inactive app`() {
        val sessions = sessionBuilder.buildSessions(
            events = listOf(
                event("app.z", 500L, UsageEventType.BACKGROUND),
                event("app.a", 1_000L, UsageEventType.FOREGROUND),
                event("app.b", 2_000L, UsageEventType.BACKGROUND),
                event("app.a", 5_000L, UsageEventType.BACKGROUND)
            ),
            rangeStartMillis = 0L,
            rangeEndMillis = 10_000L,
            nowMillis = 10_000L
        )

        assertEquals(
            listOf(session("app.a", 1_000L, 5_000L)),
            sessions
        )
    }

    @Test
    fun `closes open session at min of now and range end`() {
        val sessions = sessionBuilder.buildSessions(
            events = listOf(
                event("app.a", 1_000L, UsageEventType.FOREGROUND)
            ),
            rangeStartMillis = 0L,
            rangeEndMillis = 5_000L,
            nowMillis = 8_000L
        )

        assertEquals(
            listOf(session("app.a", 1_000L, 5_000L)),
            sessions
        )
    }

    @Test
    fun `dedupes exact duplicate events and sorts unsorted input`() {
        val sessions = sessionBuilder.buildSessions(
            events = listOf(
                event("app.a", 5_000L, UsageEventType.BACKGROUND),
                event("app.a", 1_000L, UsageEventType.FOREGROUND),
                event("app.a", 1_000L, UsageEventType.FOREGROUND)
            ),
            rangeStartMillis = 0L,
            rangeEndMillis = 10_000L,
            nowMillis = 10_000L
        )

        assertEquals(
            listOf(session("app.a", 1_000L, 5_000L)),
            sessions
        )
    }

    @Test
    fun `drops zero or negative duration sessions`() {
        val sessions = sessionBuilder.buildSessions(
            events = listOf(
                event("app.a", 1_000L, UsageEventType.FOREGROUND),
                event("app.a", 1_000L, UsageEventType.BACKGROUND),
                event("app.b", 2_000L, UsageEventType.FOREGROUND)
            ),
            rangeStartMillis = 0L,
            rangeEndMillis = 3_000L,
            nowMillis = 2_000L
        )

        assertEquals(emptyList<AppSession>(), sessions)
    }

    @Test
    fun `ignores events outside the requested range`() {
        val sessions = sessionBuilder.buildSessions(
            events = listOf(
                event("app.a", 500L, UsageEventType.FOREGROUND),
                event("app.a", 1_500L, UsageEventType.FOREGROUND),
                event("app.a", 4_000L, UsageEventType.BACKGROUND),
                event("app.a", 8_000L, UsageEventType.FOREGROUND)
            ),
            rangeStartMillis = 1_000L,
            rangeEndMillis = 5_000L,
            nowMillis = 6_000L
        )

        assertEquals(
            listOf(session("app.a", 1_500L, 4_000L)),
            sessions
        )
    }

    private fun event(
        packageName: String,
        timestampMillis: Long,
        type: UsageEventType
    ) = AppUsageEvent(
        packageName = packageName,
        timestampMillis = timestampMillis,
        type = type
    )

    private fun session(
        packageName: String,
        startTimeMillis: Long,
        endTimeMillis: Long
    ) = AppSession(
        packageName = packageName,
        startTimeMillis = startTimeMillis,
        endTimeMillis = endTimeMillis,
        durationMillis = endTimeMillis - startTimeMillis
    )
}
