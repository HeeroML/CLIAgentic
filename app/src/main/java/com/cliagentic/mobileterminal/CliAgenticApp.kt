package com.cliagentic.mobileterminal

import android.app.Application
import com.cliagentic.mobileterminal.di.AppContainer

class CliAgenticApp : Application() {
    lateinit var appContainer: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        appContainer = AppContainer(this)
    }
}
