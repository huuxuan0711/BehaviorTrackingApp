package com.xmobile.project2digitalwellbeing.data.usage.mapper

import com.xmobile.project2digitalwellbeing.data.usage.source.local.room.entity.SessionEntity
import com.xmobile.project2digitalwellbeing.domain.usage.model.AppSession

fun SessionEntity.toDomain(): AppSession {
    return AppSession(
        packageName = packageName,
        startTimeMillis = startTimeMillis,
        endTimeMillis = endTimeMillis,
        durationMillis = durationMillis
    )
}

fun AppSession.toEntity(): SessionEntity {
    return SessionEntity(
        packageName = packageName,
        startTimeMillis = startTimeMillis,
        endTimeMillis = endTimeMillis,
        durationMillis = durationMillis
    )
}
