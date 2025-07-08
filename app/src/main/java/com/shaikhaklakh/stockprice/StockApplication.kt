package com.shaikhaklakh.stockprice

import android.app.Application
import android.content.pm.ActivityInfo
import androidx.appcompat.app.AppCompatDelegate
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class StockApplication : Application(){
    override fun onCreate() {
        super.onCreate()
        // Force Light Mode globally
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)

    }
}