package com.example.telebridge_android_app

import android.app.Application
import com.google.firebase.FirebaseApp
import com.google.firebase.database.FirebaseDatabase

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)
        // Enable offline persistence
        FirebaseDatabase.getInstance("https://telebridge-fc798-default-rtdb.firebaseio.com/")
            .setPersistenceEnabled(true)
    }
}
