package com.atharok.screentime.presentation.glance

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.atharok.screentime.common.utils.GLANCE_PREFERENCES_LOADING

class TodayUsageActionCallback: ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {

        // Loading
        updateAppWidgetState(context, glanceId) { prefs ->
            prefs[booleanPreferencesKey(GLANCE_PREFERENCES_LOADING)] = true
        }

        TodayUsageWidget().update(context, glanceId)

        val work = OneTimeWorkRequestBuilder<TodayUsageComputeWorker>().build()

        WorkManager.getInstance(context).enqueue(work)
    }
}