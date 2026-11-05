package com.inkr8
import android.app.Application
import com.google.firebase.FirebaseApp
import com.inkr8.di.AppGraph
class Inkr8App : Application() {
    var graph: AppGraph? = null
        private set
    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.FIREBASE_CONFIGURED && FirebaseApp.initializeApp(this) != null) graph = AppGraph(this)
    }
}
