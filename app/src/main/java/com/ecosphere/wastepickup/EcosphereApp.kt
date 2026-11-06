package com.ecosphere.wastepickup

import android.app.Application
import com.ecosphere.wastepickup.core.di.AppContainer

class EcosphereApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()

        container = AppContainer(this)
    }
}