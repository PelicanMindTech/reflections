package com.paulaslab.reflections

import android.app.Application

class ReflectionsApp() : Application() {
    public var journalStore : JournallingStore? = null

    override fun onCreate() {
        super.onCreate()
        journalStore = JournallingStore(baseContext)
    }
}