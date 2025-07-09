package com.atharok.screentime.presentation.viewmodel

import android.content.Context
import android.content.pm.PackageManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.atharok.screentime.common.extensions.getApplicationName
import com.atharok.screentime.domain.entities.AppInfo
import com.atharok.screentime.domain.resources.Resource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class InstalledPackagesViewModel(): ViewModel() {

    private val _installedApps = MutableStateFlow<Resource<List<AppInfo>>>(Resource.loading())
    val installedApps: StateFlow<Resource<List<AppInfo>>> = _installedApps

    fun loadInstalledPackages(context: Context) = viewModelScope.launch(Dispatchers.Default) {
        _installedApps.value = Resource.loading()

        val pm = context.packageManager
        val installedAppsRes = pm.getInstalledApplications(PackageManager.GET_META_DATA)
            /*.filter {
                pm.getLaunchIntentForPackage(it.packageName) != null // only launchable apps
            }*/.map {
                AppInfo(
                    name = pm.getApplicationName(it.packageName) ?: "",
                    packageName = it.packageName,
                    icon = pm.getApplicationIcon(it.packageName)
                )
            }.sortedBy {
                it.name
            }

        _installedApps.value = Resource.success(installedAppsRes)
    }
}