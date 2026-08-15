package com.tkno.gallery

import android.app.Application
import com.tkno.gallery.util.VideoEngineManager

open class App : Application() {

    override fun onCreate() {
        super.onCreate()
        instance = this
        VideoEngineManager.init(this)
    }

    companion object {
        lateinit var instance: App
            private set

        val context: App
            get() = instance
    }
}
