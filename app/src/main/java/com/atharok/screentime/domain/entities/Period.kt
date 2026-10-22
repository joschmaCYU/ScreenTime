package com.atharok.screentime.domain.entities

import androidx.annotation.StringRes
import com.atharok.screentime.R

enum class Period(@param:StringRes val stringRes: Int) {
    DAY(R.string.day),
    WEEK(R.string.week),
    MONTH(R.string.month),
    YEAR(R.string.year),
    ALL(R.string.all)
}