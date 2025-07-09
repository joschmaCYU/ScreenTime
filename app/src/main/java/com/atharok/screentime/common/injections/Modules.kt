package com.atharok.screentime.common.injections

import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import com.atharok.screentime.R
import com.atharok.screentime.common.utils.DurationFormater
import com.atharok.screentime.common.utils.KOIN_DATE_FORMAT
import com.atharok.screentime.common.utils.KOIN_DATE_TIME_FORMAT
import com.atharok.screentime.common.utils.KOIN_SIMPLE_DAY_FORMAT
import com.atharok.screentime.common.utils.KOIN_SIMPLE_HOUR_FORMAT
import com.atharok.screentime.data.dataStore.SettingsDataStore
import com.atharok.screentime.data.repositories.DeviceUsageRepositoryImpl
import com.atharok.screentime.data.repositories.SettingsRepositoryImpl
import com.atharok.screentime.data.statsTracker.UsageStatsTracker
import com.atharok.screentime.domain.repositories.DeviceUsageRepository
import com.atharok.screentime.domain.repositories.SettingsRepository
import com.atharok.screentime.domain.usecases.DeviceUsageUseCase
import com.atharok.screentime.domain.usecases.SettingsUseCase
import com.atharok.screentime.presentation.viewmodel.DeviceUsageViewModel
import com.atharok.screentime.presentation.viewmodel.InstalledPackagesViewModel
import com.atharok.screentime.presentation.viewmodel.SettingsViewModel
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.core.qualifier.named
import org.koin.dsl.module
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

val appModules by lazy {
    listOf<Module>(androidModule, viewModelModule, useCaseModule, repositoryModule, dataModule)
}

private val androidModule: Module = module {
    single<UsageStatsManager> {
        androidContext().getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
    }

    single<AppOpsManager> {
        androidContext().getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
    }

    single { TimeZone.getDefault() }

    single { Calendar.getInstance(get<TimeZone>()) }

    single<DateFormat>(named(KOIN_DATE_TIME_FORMAT)) {
        SimpleDateFormat.getDateTimeInstance(
            SimpleDateFormat.FULL, SimpleDateFormat.SHORT, Locale.getDefault()
        ).apply {
            timeZone = get<TimeZone>()
        }
    }

    single<DateFormat>(named(KOIN_DATE_FORMAT)) {
        SimpleDateFormat.getDateInstance(
            SimpleDateFormat.MEDIUM, Locale.getDefault()
        ).apply {
            timeZone = get<TimeZone>()
        }
    }

    single<SimpleDateFormat>(named(KOIN_SIMPLE_DAY_FORMAT)) {
        SimpleDateFormat("EEE", Locale.getDefault()).apply {
            timeZone = get<TimeZone>()
        }
    }

    single<SimpleDateFormat>(named(KOIN_SIMPLE_HOUR_FORMAT)) {
        val pattern = if(android.text.format.DateFormat.is24HourFormat(androidContext())) {
            "HH'${androidContext().getString(R.string.hour_unit, "")}'"
        } else "h a"
        SimpleDateFormat(pattern, Locale.getDefault()).apply {
            timeZone = get<TimeZone>()
        }
    }

    single { DurationFormater(context = androidContext()) }

    single { CartesianChartModelProducer() }
}

private val viewModelModule: Module = module {
    viewModel {
        DeviceUsageViewModel(
            usageUseCase = get<DeviceUsageUseCase>(),
            settingsUseCase = get<SettingsUseCase>()
        )
    }

    viewModel {
        SettingsViewModel(
            useCase = get<SettingsUseCase>()
        )
    }

    viewModel {
        InstalledPackagesViewModel()
    }
}

private val useCaseModule: Module = module {
    single {
        DeviceUsageUseCase(
            repository = get<DeviceUsageRepository>()
        )
    }

    single {
        SettingsUseCase(
            repository = get<SettingsRepository>()
        )
    }
}

private val repositoryModule: Module = module {
    single<DeviceUsageRepository> {
        DeviceUsageRepositoryImpl(
            context = androidContext(),
            tracker = get<UsageStatsTracker>()
        )
    }

    single<SettingsRepository> {
        SettingsRepositoryImpl(
            settingsDataStore = get<SettingsDataStore>()
        )
    }
}

private val dataModule: Module = module {
    single {
        UsageStatsTracker(
            context = androidContext(),
            usageStatsManager = get<UsageStatsManager>(),
            appOps = get<AppOpsManager>()
        )
    }

    single {
        SettingsDataStore(
            context = androidContext()
        )
    }
}